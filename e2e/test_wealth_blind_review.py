import contextlib
import hashlib
import io
import json
from pathlib import Path
import tempfile
import unittest

from wealth_blind_review import ValidationError, evaluate_batch, initialize_batch, main


def review_case(index: int, overall: str = "A", similarity: int = 0) -> dict:
    reasons = []
    if overall == "C":
        reasons = ["CANNOT_RECALL"]
    elif overall == "D":
        reasons = ["RULE_DIRECTION_WRONG"]
    return {
        "caseId": f"W{index:03d}",
        "reportId": index,
        "copyVersion": "wealth-plain-v3.18",
        "contentHash": f"{index:064x}",
        "pastMain": overall,
        "pastSecondary": overall,
        "pastHidden": overall,
        "pastOverall": overall,
        "currentRelevance": 3,
        "readability": 3,
        "similarity": similarity,
        "failureReasons": reasons,
        "reviewStatus": "none",
    }


class WealthBlindReviewTest(unittest.TestCase):
    def test_report_snapshot_initialization_outputs_only_anonymous_score_fields(self):
        content = {
            "copyVersion": "wealth-plain-v3.18",
            "timeline": {"past": {"year": 2025}},
            "years": [{"year": 2026}],
        }
        report = {
            "id": 27,
            "subject": "不应出现在输出中的姓名",
            "topic": "wealth",
            "edition": "plain",
            "status": "ready",
            "contentVersion": "wealth-narrative-v4",
            "content": content,
            "createdAt": "2026-09-05T08:00:00",
            "generatedAt": "2026-09-05T08:00:00",
        }

        batch = initialize_batch([report], "wealth-pilot-001", "994fa51")

        expected_hash = hashlib.sha256(json.dumps(
            content, ensure_ascii=False, sort_keys=True, separators=(",", ":")
        ).encode("utf-8")).hexdigest()
        self.assertEqual("W001", batch["cases"][0]["caseId"])
        self.assertEqual(27, batch["cases"][0]["reportId"])
        self.assertEqual("wealth-plain-v3.18", batch["cases"][0]["copyVersion"])
        self.assertEqual(expected_hash, batch["cases"][0]["contentHash"])
        self.assertEqual("", batch["cases"][0]["pastOverall"])
        serialized = json.dumps(batch, ensure_ascii=False)
        self.assertNotIn(report["subject"], serialized)
        self.assertNotIn("content", batch["cases"][0])

    def test_report_snapshot_initialization_rejects_old_wealth_copy(self):
        report = {
            "id": 1,
            "topic": "wealth",
            "edition": "plain",
            "status": "ready",
            "contentVersion": "wealth-narrative-v3",
            "content": {"copyVersion": "wealth-plain-v3.3"},
        }

        with self.assertRaisesRegex(ValidationError, "只接受 wealth-narrative-v4.*wealth-plain-v3.18"):
            initialize_batch([report], "wealth-pilot-001", "994fa51")

    def test_legacy_v34_batch_remains_evaluable(self):
        case = review_case(1)
        case["copyVersion"] = "wealth-plain-v3.4"

        result = evaluate_batch({
            "batchId": "wealth-v34-archive",
            "commit": "009ede1",
            "cases": [case],
        })

        self.assertEqual(1, result["counts"]["N"])
        self.assertEqual("wealth-plain-v3.4", result["copyVersion"])

    def test_mixed_copy_versions_are_rejected(self):
        legacy = review_case(1)
        legacy["copyVersion"] = "wealth-plain-v3.4"

        with self.assertRaisesRegex(ValidationError, "同一批次不得混用文案版本"):
            evaluate_batch({
                "batchId": "wealth-mixed-copy",
                "commit": "1dc9abf",
                "cases": [legacy, review_case(2)],
            })

    def test_pilot_metrics_keep_unknown_separate_from_clear_mismatch(self):
        cases = [review_case(index) for index in range(1, 31)]
        cases[-2] = review_case(29, "C")
        cases[-1] = review_case(30, "D")
        for index in range(4):
            cases[index]["similarity"] = 2

        result = evaluate_batch({
            "batchId": "wealth-pilot-001",
            "commit": "009ede1",
            "cases": cases,
        })

        self.assertEqual(30, result["counts"]["N"])
        self.assertEqual("wealth-plain-v3.18", result["copyVersion"])
        self.assertEqual({"A": 28, "B": 0, "C": 1, "D": 1}, result["counts"]["ratings"])
        self.assertAlmostEqual(1 / 30, result["metrics"]["rawMismatchRate"])
        self.assertAlmostEqual(1 / 29, result["metrics"]["decidableMismatchRate"])
        self.assertAlmostEqual(29 / 30, result["metrics"]["decidableCoverageRate"])
        self.assertAlmostEqual(4 / 30, result["metrics"]["highSimilarityRate"])
        self.assertEqual("pilot", result["gate"]["stage"])
        self.assertTrue(result["gate"]["passed"])
        self.assertGreater(result["confidence"]["rawMismatchWilson95"]["upper"], 0.05)
        self.assertFalse(result["confidence"]["supportsTrueRateAtMostFivePercent"])

    def test_fewer_than_thirty_cases_are_reported_as_incomplete(self):
        result = evaluate_batch({
            "batchId": "wealth-dry-run",
            "commit": "009ede1",
            "cases": [review_case(index) for index in range(1, 6)],
        })

        self.assertEqual("dry-run", result["gate"]["stage"])
        self.assertFalse(result["gate"]["passed"])
        self.assertIn("至少需要30份", result["gate"]["failures"])

    def test_personal_birth_fields_are_rejected(self):
        case = review_case(1)
        case["birthDate"] = "1990-01-01"

        with self.assertRaisesRegex(ValidationError, "禁止个人信息字段.*birthDate"):
            evaluate_batch({
                "batchId": "wealth-private-leak",
                "commit": "009ede1",
                "cases": [case],
            })

    def test_batch_level_personal_fields_are_rejected(self):
        with self.assertRaisesRegex(ValidationError, "禁止个人信息字段.*birthPlace"):
            evaluate_batch({
                "batchId": "wealth-private-leak",
                "commit": "009ede1",
                "birthPlace": "广东省 深圳市",
                "cases": [review_case(1)],
            })

    def test_duplicate_case_or_report_is_rejected(self):
        first = review_case(1)
        duplicate = review_case(2)
        duplicate["caseId"] = first["caseId"]

        with self.assertRaisesRegex(ValidationError, "caseId 不得重复"):
            evaluate_batch({
                "batchId": "wealth-duplicate",
                "commit": "009ede1",
                "cases": [first, duplicate],
            })

    def test_non_a_rating_requires_a_failure_reason(self):
        case = review_case(1, "B")
        case["failureReasons"] = []

        with self.assertRaisesRegex(ValidationError, "B/C/D 必须填写 failureReasons"):
            evaluate_batch({
                "batchId": "wealth-no-reason",
                "commit": "009ede1",
                "cases": [case],
            })

    def test_cli_prints_a_human_readable_report_without_personal_data(self):
        batch = {
            "batchId": "wealth-dry-run",
            "commit": "009ede1",
            "cases": [review_case(index) for index in range(1, 6)],
        }
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "private-ratings.json"
            path.write_text(json.dumps(batch), encoding="utf-8")
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                exit_code = main([str(path), "--format", "markdown"])

        self.assertEqual(0, exit_code)
        self.assertIn("# 财富真人盲测汇总：wealth-dry-run", output.getvalue())
        self.assertIn("文案版本：`wealth-plain-v3.18`", output.getvalue())
        self.assertIn("样本数：5", output.getvalue())
        self.assertIn("至少需要30份", output.getvalue())
        self.assertNotIn("contentHash", output.getvalue())

    def test_cli_can_initialize_an_anonymous_batch_from_report_snapshots(self):
        report = {
            "id": 27,
            "subject": "不应输出的姓名",
            "topic": "wealth",
            "edition": "plain",
            "status": "ready",
            "contentVersion": "wealth-narrative-v4",
            "content": {
                "copyVersion": "wealth-plain-v3.18",
                "timeline": {"past": {"year": 2025}},
            },
        }
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "private-reports.json"
            path.write_text(json.dumps([report], ensure_ascii=False), encoding="utf-8")
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                exit_code = main([
                    str(path),
                    "--init-reports",
                    "--batch-id", "wealth-pilot-001",
                    "--commit", "994fa51",
                ])

        self.assertEqual(0, exit_code)
        batch = json.loads(output.getvalue())
        self.assertEqual("W001", batch["cases"][0]["caseId"])
        self.assertEqual(27, batch["cases"][0]["reportId"])
        self.assertNotIn(report["subject"], output.getvalue())


if __name__ == "__main__":
    unittest.main()
