"""Offline scoring for de-identified wealth-report blind reviews."""

import argparse
from collections import Counter
import hashlib
import json
from math import sqrt
from pathlib import Path
import re
import sys


class ValidationError(ValueError):
    pass


RATINGS = {"A", "B", "C", "D"}
REVIEW_STATUSES = {"none", "pending", "resolved"}
CURRENT_COPY_VERSION = "wealth-plain-v3.16"
SUPPORTED_SCORE_COPY_VERSIONS = {
    "wealth-plain-v3.4",
    "wealth-plain-v3.5",
    "wealth-plain-v3.6",
    "wealth-plain-v3.7",
    "wealth-plain-v3.8",
    "wealth-plain-v3.9",
    "wealth-plain-v3.10",
    "wealth-plain-v3.11",
    "wealth-plain-v3.12",
    "wealth-plain-v3.13",
    "wealth-plain-v3.14",
    "wealth-plain-v3.15",
    CURRENT_COPY_VERSION,
}
FAILURE_REASONS = {
    "RULE_DIRECTION_WRONG",
    "EVIDENCE_TOO_WEAK",
    "OBJECT_TOO_VAGUE",
    "UNSUPPORTED_ASSUMPTION",
    "COPY_UNNATURAL",
    "REPORTS_TOO_SIMILAR",
    "BIRTH_DATA_UNCERTAIN",
    "CANNOT_RECALL",
    "OTHER",
}
PERSONAL_FIELDS = {
    "name",
    "realName",
    "birthDate",
    "birthTime",
    "birthPlace",
    "solarDateTime",
    "phone",
    "mobile",
    "email",
    "idCard",
}
CASE_FIELDS = {
    "caseId",
    "reportId",
    "copyVersion",
    "contentHash",
    "pastMain",
    "pastSecondary",
    "pastHidden",
    "pastOverall",
    "currentRelevance",
    "readability",
    "similarity",
    "failureReasons",
    "reviewStatus",
}
BATCH_FIELDS = {"batchId", "commit", "cases"}


def initialize_batch(reports, batch_id, commit):
    if not isinstance(reports, list) or not reports:
        raise ValidationError("报告快照列表不能为空")
    if not isinstance(batch_id, str) or not batch_id.strip():
        raise ValidationError("batchId 不能为空")
    if not isinstance(commit, str) or not re.fullmatch(r"[0-9a-f]{7,40}", commit):
        raise ValidationError("commit 必须是 Git 提交哈希")

    cases = []
    for index, report in enumerate(reports, start=1):
        content = report.get("content") if isinstance(report, dict) else None
        if (
            not isinstance(content, dict)
            or report.get("topic") != "wealth"
            or report.get("edition") != "plain"
            or report.get("status") != "ready"
            or report.get("contentVersion") != "wealth-narrative-v4"
            or content.get("copyVersion") != CURRENT_COPY_VERSION
        ):
            raise ValidationError(
                f"只接受 wealth-narrative-v4 与 {CURRENT_COPY_VERSION} 的已完成财富通俗版报告"
            )
        report_id = report.get("id")
        if isinstance(report_id, bool) or not isinstance(report_id, int) or report_id <= 0:
            raise ValidationError("报告 id 必须是正整数")
        canonical = json.dumps(
            content, ensure_ascii=False, sort_keys=True, separators=(",", ":")
        ).encode("utf-8")
        cases.append({
            "caseId": f"W{index:03d}",
            "reportId": report_id,
            "copyVersion": CURRENT_COPY_VERSION,
            "contentHash": hashlib.sha256(canonical).hexdigest(),
            "pastMain": "",
            "pastSecondary": "",
            "pastHidden": "",
            "pastOverall": "",
            "currentRelevance": "",
            "readability": "",
            "similarity": "",
            "failureReasons": [],
            "reviewStatus": "none",
        })
    return {"batchId": batch_id, "commit": commit, "cases": cases}


def _rate(value, field, case_id):
    if value not in RATINGS:
        raise ValidationError(f"{case_id}.{field} 必须是 A/B/C/D")


def _score(value, field, case_id):
    if isinstance(value, bool) or not isinstance(value, int) or not 0 <= value <= 3:
        raise ValidationError(f"{case_id}.{field} 必须是 0–3 的整数")


def _wilson(successes, total):
    if total == 0:
        return {"lower": 0.0, "upper": 0.0}
    z = 1.959963984540054
    proportion = successes / total
    denominator = 1 + z * z / total
    center = (proportion + z * z / (2 * total)) / denominator
    margin = z * sqrt((proportion * (1 - proportion) + z * z / (4 * total)) / total) / denominator
    return {"lower": max(0.0, center - margin), "upper": min(1.0, center + margin)}


def _validate_case(case, seen_case_ids, seen_report_ids):
    if not isinstance(case, dict):
        raise ValidationError("cases 中每一项必须是对象")
    leaked = sorted(PERSONAL_FIELDS.intersection(case))
    if leaked:
        raise ValidationError(f"禁止个人信息字段：{', '.join(leaked)}")
    unknown = sorted(set(case) - CASE_FIELDS)
    missing = sorted(CASE_FIELDS - set(case))
    if unknown:
        raise ValidationError(f"不支持的字段：{', '.join(unknown)}")
    if missing:
        raise ValidationError(f"缺少字段：{', '.join(missing)}")

    case_id = case["caseId"]
    if not isinstance(case_id, str) or not re.fullmatch(r"W[0-9]{3,}", case_id):
        raise ValidationError("caseId 必须使用 W001 形式的随机编号")
    if case_id in seen_case_ids:
        raise ValidationError("caseId 不得重复")
    seen_case_ids.add(case_id)

    report_id = case["reportId"]
    if isinstance(report_id, bool) or not isinstance(report_id, int) or report_id <= 0:
        raise ValidationError(f"{case_id}.reportId 必须是正整数")
    if report_id in seen_report_ids:
        raise ValidationError("reportId 不得重复")
    seen_report_ids.add(report_id)

    if case["copyVersion"] not in SUPPORTED_SCORE_COPY_VERSIONS:
        supported = " / ".join(sorted(SUPPORTED_SCORE_COPY_VERSIONS))
        raise ValidationError(f"{case_id}.copyVersion 必须是 {supported}")
    if not isinstance(case["contentHash"], str) or not re.fullmatch(r"[0-9a-f]{64}", case["contentHash"]):
        raise ValidationError(f"{case_id}.contentHash 必须是小写 SHA-256")

    for field in ("pastMain", "pastSecondary", "pastHidden", "pastOverall"):
        _rate(case[field], field, case_id)
    for field in ("currentRelevance", "readability", "similarity"):
        _score(case[field], field, case_id)

    reasons = case["failureReasons"]
    if not isinstance(reasons, list) or any(reason not in FAILURE_REASONS for reason in reasons):
        raise ValidationError(f"{case_id}.failureReasons 含未知原因")
    if case["pastOverall"] != "A" and not reasons:
        raise ValidationError("B/C/D 必须填写 failureReasons")
    if case["pastOverall"] == "A" and reasons:
        raise ValidationError(f"{case_id} 评为 A 时 failureReasons 必须为空")
    if case["reviewStatus"] not in REVIEW_STATUSES:
        raise ValidationError(f"{case_id}.reviewStatus 无效")


def evaluate_batch(batch):
    if not isinstance(batch, dict):
        raise ValidationError("批次必须是对象")
    leaked = sorted(PERSONAL_FIELDS.intersection(batch))
    if leaked:
        raise ValidationError(f"禁止个人信息字段：{', '.join(leaked)}")
    unknown = sorted(set(batch) - BATCH_FIELDS)
    if unknown:
        raise ValidationError(f"不支持的批次字段：{', '.join(unknown)}")
    if not isinstance(batch.get("batchId"), str) or not batch["batchId"].strip():
        raise ValidationError("batchId 不能为空")
    if not isinstance(batch.get("commit"), str) or not re.fullmatch(r"[0-9a-f]{7,40}", batch["commit"]):
        raise ValidationError("commit 必须是 Git 提交哈希")
    cases = batch.get("cases")
    if not isinstance(cases, list) or not cases:
        raise ValidationError("cases 不能为空")

    seen_case_ids = set()
    seen_report_ids = set()
    for case in cases:
        _validate_case(case, seen_case_ids, seen_report_ids)
    copy_versions = {case["copyVersion"] for case in cases}
    if len(copy_versions) != 1:
        raise ValidationError("同一批次不得混用文案版本")
    copy_version = next(iter(copy_versions))

    total = len(cases)
    ratings = Counter(case["pastOverall"] for case in cases)
    for rating in RATINGS:
        ratings.setdefault(rating, 0)
    decidable = ratings["A"] + ratings["B"] + ratings["D"]
    raw_mismatch = ratings["D"] / total
    decidable_mismatch = ratings["D"] / decidable if decidable else 0.0
    coverage = decidable / total
    strong = ratings["A"] / total
    high_similarity = sum(case["similarity"] >= 2 for case in cases) / total
    disputes = sum(case["reviewStatus"] == "pending" for case in cases)
    reason_counts = Counter(reason for case in cases for reason in case["failureReasons"])
    interval = _wilson(ratings["D"], total)

    metrics = {
        "rawMismatchRate": raw_mismatch,
        "decidableMismatchRate": decidable_mismatch,
        "decidableCoverageRate": coverage,
        "strongRelevanceRate": strong,
        "highSimilarityRate": high_similarity,
        "disputeRate": disputes / total,
        "averageCurrentRelevance": sum(case["currentRelevance"] for case in cases) / total,
        "averageReadability": sum(case["readability"] for case in cases) / total,
    }

    failures = []
    if total < 30:
        stage = "dry-run"
        failures.append("至少需要30份")
    elif total < 100:
        stage = "pilot"
        if raw_mismatch > 0.05:
            failures.append("原始明显不符率高于5%")
        if coverage < 0.80:
            failures.append("可判断覆盖率低于80%")
        if metrics["averageReadability"] < 2.5:
            failures.append("中文可读性平均分低于2.5")
        if high_similarity > 0.15:
            failures.append("模板感偏高率高于15%")
    else:
        stage = "formal"
        if raw_mismatch > 0.05:
            failures.append("原始明显不符率高于5%")
        if decidable_mismatch > 0.05:
            failures.append("可判断样本明显不符率高于5%")
        if coverage < 0.85:
            failures.append("可判断覆盖率低于85%")
        if metrics["averageReadability"] < 2.7:
            failures.append("中文可读性平均分低于2.7")
        if high_similarity > 0.10:
            failures.append("模板感偏高率高于10%")

    if reason_counts["UNSUPPORTED_ASSUMPTION"]:
        failures.append("存在未经输入支持的事实推断")
    if disputes:
        failures.append("仍有争议项未复核")

    return {
        "batchId": batch["batchId"],
        "commit": batch["commit"],
        "copyVersion": copy_version,
        "counts": {
            "N": total,
            "ratings": {rating: ratings[rating] for rating in ("A", "B", "C", "D")},
            "pendingReviews": disputes,
            "failureReasons": dict(sorted(reason_counts.items())),
        },
        "metrics": metrics,
        "confidence": {
            "rawMismatchWilson95": interval,
            "supportsTrueRateAtMostFivePercent": interval["upper"] <= 0.05,
        },
        "gate": {
            "stage": stage,
            "passed": not failures,
            "failures": failures,
        },
    }


def _percent(value):
    return f"{value * 100:.1f}%"


def render_markdown(result):
    metrics = result["metrics"]
    interval = result["confidence"]["rawMismatchWilson95"]
    gate = result["gate"]
    failures = gate["failures"] or ["无"]
    lines = [
        f"# 财富真人盲测汇总：{result['batchId']}",
        "",
        f"代码提交：`{result['commit']}`  ",
        f"文案版本：`{result['copyVersion']}`  ",
        f"样本数：{result['counts']['N']}  ",
        f"阶段：`{gate['stage']}`  ",
        f"门槛结果：{'通过' if gate['passed'] else '未通过'}",
        "",
        "## 核心指标",
        "",
        f"- 原始明显不符率：{_percent(metrics['rawMismatchRate'])}",
        f"- 可判断样本明显不符率：{_percent(metrics['decidableMismatchRate'])}",
        f"- 可判断覆盖率：{_percent(metrics['decidableCoverageRate'])}",
        f"- 强相关率：{_percent(metrics['strongRelevanceRate'])}",
        f"- 模板感偏高率：{_percent(metrics['highSimilarityRate'])}",
        f"- 中文可读性平均分：{metrics['averageReadability']:.2f} / 3",
        f"- 明显不符率 Wilson 95% 区间：{_percent(interval['lower'])}–{_percent(interval['upper'])}",
        "",
        "## 未通过原因",
        "",
    ]
    lines.extend(f"- {failure}" for failure in failures)
    lines.extend([
        "",
        "结论：" + (
            "置信区间上界不高于 5%，本批数据支持总体明显不符率不高于 5%。"
            if result["confidence"]["supportsTrueRateAtMostFivePercent"]
            else "本批数据不足以证明总体明显不符率稳定不高于 5%。"
        ),
        "",
    ])
    return "\n".join(lines)


def main(argv=None):
    parser = argparse.ArgumentParser(description="统计脱敏财富真人盲测评分")
    parser.add_argument("input", help="私有评分或报告快照 JSON 文件路径")
    parser.add_argument("--format", choices=("markdown",), default="markdown")
    parser.add_argument("--init-reports", action="store_true", help="从财富报告快照初始化匿名评分骨架")
    parser.add_argument("--batch-id", help="初始化批次编号")
    parser.add_argument("--commit", help="初始化时锁定的 Git 提交")
    args = parser.parse_args(argv)
    try:
        payload = json.loads(Path(args.input).read_text(encoding="utf-8"))
        if args.init_reports:
            batch = initialize_batch(payload, args.batch_id, args.commit)
        else:
            result = evaluate_batch(payload)
    except (OSError, json.JSONDecodeError, ValidationError) as error:
        print(f"评分数据无效：{error}", file=sys.stderr)
        return 2
    if args.init_reports:
        print(json.dumps(batch, ensure_ascii=False, indent=2))
        return 0
    print(render_markdown(result), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
