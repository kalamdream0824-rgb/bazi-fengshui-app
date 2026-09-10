"""财富 v3 系统验收：真实 HTTP 购买链路 + 合成无风险页面边界。

前置：VITE_API_MODE=http 的前端（默认 5173）和后端（默认经 Vite 代理）。
运行：python3 e2e/run_wealth_v3_e2e.py
"""

import json
import os
import re
import sys
import uuid
from pathlib import Path

from playwright.sync_api import Page, sync_playwright


ROOT = Path(__file__).resolve().parents[1]
BASE = os.environ.get("E2E_BASE_URL", "http://localhost:5173").rstrip("/")
RESULT_PATH = Path(os.environ.get("TASK9_RESULT_PATH", "/tmp/wealth-v3-task9-result.json"))
SAMPLE_PATH = Path(os.environ.get(
    "V36_SAMPLE_PATH", "/tmp/wealth-v3.6-candidate-samples.md"
))
EXPECTED_CONTENT_VERSION = "wealth-narrative-v4"
EXPECTED_COPY_VERSION = "wealth-plain-v3.16"
PASSWORD = "pass123"
USER = f"task9_wealth_{uuid.uuid4().hex[:10]}"
OTHER_USER = f"task9_other_{uuid.uuid4().hex[:10]}"


def split_sentences(text: str) -> list[str]:
    return [part.strip() for part in re.findall(r"[^。！？]+[。！？]?", text) if part.strip()]


def group_annual_copy(content: dict) -> dict[str, list[dict]]:
    selectors = {
        "income": lambda year: [block["text"] for block in year["income"]],
        "retention": lambda year: [year["retention"]["text"]],
        "risk": lambda year: [year["risk"]["reading"]["text"]] if year.get("risk") else [],
        "observations": lambda year: [block["text"] for block in year["observations"]],
        "actions": lambda year: [block["text"] for block in year["actions"]],
    }
    result: dict[str, list[dict]] = {}
    for category, select in selectors.items():
        occurrences: dict[str, dict] = {}
        for year in content["years"]:
            for text in dict.fromkeys(
                sentence for value in select(year) for sentence in split_sentences(value)
            ):
                if text in occurrences:
                    occurrences[text]["years"].append(year["year"])
                else:
                    occurrences[text] = {
                        "firstYear": year["year"], "text": text, "years": [year["year"]]
                    }
        result[category] = list(occurrences.values())
    return result


def rendered_annual_copy(page: Page) -> dict[str, list[dict]]:
    selectors = {
        "income": ".wealth-year__income p",
        "retention": ".wealth-v3-year__retention p",
        "risk": ".wealth-v3-year__risk p",
        "observations": ".wealth-v3-year__observations li",
        "actions": ".wealth-v3-year__actions li",
    }
    result = {category: [] for category in selectors}
    for article in page.locator(".wealth-year").all():
        year = int(article.locator("header h2").inner_text().split("·")[0].strip())
        for category, selector in selectors.items():
            for item in article.locator(selector).all():
                text = item.locator(":scope > span").inner_text().strip()
                badge = item.locator(":scope > .wealth-year__applies")
                years = [year]
                if badge.count():
                    years = [int(value) for value in re.findall(r"\d{4}", badge.inner_text())]
                result[category].append({"firstYear": year, "text": text, "years": years})
    return result


def repeated_visible_sentences(page: Page) -> list[str]:
    selectors = [
        "#wealth-v3-thesis", ".wealth-reader__summary", ".wealth-path p",
        ".wealth-v3-risk-summary p", ".wealth-year__income p > span",
        ".wealth-v3-year__retention p > span", ".wealth-v3-year__risk p > span",
        ".wealth-v3-year__observations li > span", ".wealth-v3-year__actions li > span",
        ".wealth-v3-year__comparison p", ".wealth-v3-reader__route li",
        ".wealth-v3-reader__note",
    ]
    seen: set[str] = set()
    repeated: set[str] = set()
    for selector in selectors:
        for text in page.locator(selector).all_inner_texts():
            for sentence in split_sentences(text):
                if sentence in seen:
                    repeated.add(sentence)
                seen.add(sentence)
    return sorted(repeated)


def auth_token(page: Page) -> str:
    raw = page.evaluate("window.localStorage.getItem('bazi-auth')")
    return json.loads(raw)["token"]


def register(page: Page, username: str) -> str:
    page.goto(f"{BASE}/auth", wait_until="networkidle")
    page.locator(".opt", has_text="注册").click()
    page.get_by_placeholder("请输入用户名").fill(username)
    page.get_by_placeholder("请输入密码").fill(PASSWORD)
    with page.expect_response("**/api/v1/auth/register", timeout=15000) as response_info:
        page.locator("button", has_text="注册").last.click()
    response = response_info.value
    if response.status != 200:
        print(f"注册响应 {response.status}: {response.text()}")
    try:
        page.wait_for_url("**/profile", timeout=15000)
    except Exception:
        print(f"注册后地址: {page.url}")
        print(f"注册后页面: {page.inner_text('body')[:1200]}")
        page.screenshot(path=f"/tmp/{username}-register-failure.png", full_page=True)
        raise
    return auth_token(page)


def viewport_ok(page: Page, width: int, height: int) -> tuple[bool, bool]:
    page.set_viewport_size({"width": width, "height": height})
    page.wait_for_timeout(150)
    no_overflow = page.evaluate(
        "document.documentElement.scrollWidth <= document.documentElement.clientWidth"
    )
    paper_aligned = page.locator(".report-reader__paper").evaluate(
        "el => {"
        " const paper = el.getBoundingClientRect();"
        " const shell = document.querySelector('.app-shell').getBoundingClientRect();"
        " return Math.abs(paper.left - shell.left) < 1"
        "   && Math.abs(paper.right - shell.right) < 1;"
        "}"
    )
    return no_overflow, paper_aligned


def main() -> int:
    failures: list[str] = []
    total = 0
    console_errors: list[str] = []
    report_ids: list[int] = []
    api_content_bytes: list[int] = []
    candidate_samples: list[tuple[str, str]] = []
    visible_collisions: dict[str, list[str]] = {}

    def check(name: str, condition: bool) -> None:
        nonlocal total
        total += 1
        print(("PASS: " if condition else "FAIL: ") + name)
        if not condition:
            failures.append(name)

    print(f"测试账号一: {USER}")
    print(f"测试账号二: {OTHER_USER}")

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        first_context = browser.new_context(viewport={"width": 430, "height": 932})
        page = first_context.new_page()
        page.on("console", lambda message: console_errors.append(message.text) if message.type == "error" else None)
        page.on("pageerror", lambda error: console_errors.append(str(error)))

        token = register(page, USER)
        check("账号一注册登录", USER in page.inner_text("body"))

        page.goto(f"{BASE}/input", wait_until="networkidle")
        page.get_by_placeholder("请输入姓名").fill("任务九财富样本")
        page.get_by_role("button", name="开始排盘").click()
        page.wait_for_selector(".bazi-table", timeout=15000)
        check("真实排盘完成", "档案：" in page.inner_text("body"))

        page.goto(f"{BASE}/report", wait_until="networkidle")
        page.get_by_role("radio", name=re.compile("财富运势")).click()
        check("财富专业版保持禁用", page.get_by_role("radio", name=re.compile("专业版")).is_disabled())
        with page.expect_response(
            lambda response: response.url.endswith("/api/v1/reports/checkout")
            and response.request.method == "POST",
            timeout=30000,
        ) as checkout_info, page.expect_response(
            lambda response: re.search(r"/api/v1/reports/checkout/\d+/mock-pay$", response.url)
            is not None,
            timeout=30000,
        ) as payment_info:
            page.get_by_role("button", name="购买并生成通俗版命书 · ¥6.9").click()
        checkout_payload = checkout_info.value.json()
        payment_payload = payment_info.value.json()
        check(
            "非会员由服务端按690分创建待支付订单",
            checkout_info.value.status == 200
            and checkout_payload["amountCents"] == 690
            and checkout_payload["status"] == "pending",
        )
        check(
            "模拟支付解锁checkout生成的同一份命书",
            payment_info.value.status == 200
            and payment_payload["id"] == checkout_payload["reportId"],
        )
        page.wait_for_url("**/reports/*", timeout=30000)
        page.wait_for_selector(".wealth-v3-reader", timeout=30000)
        report_id = int(page.url.rstrip("/").split("/")[-1])
        report_ids.append(report_id)

        response = page.request.get(
            f"{BASE}/api/v1/reports/{report_id}",
            headers={"Authorization": f"Bearer {token}"},
        )
        check("真实创建响应可读取", response.status == 200)
        report = response.json()
        content = report["content"]
        api_content_bytes.append(len(json.dumps(content, ensure_ascii=False, separators=(",", ":")).encode("utf-8")))
        check("真实报告使用财富v4契约", report["contentVersion"] == EXPECTED_CONTENT_VERSION)
        check("新报告使用当前财富文案版本", content["copyVersion"] == EXPECTED_COPY_VERSION)
        check("真实报告固定三年", content["horizonYears"] == 3 and len(content["years"]) == 3)
        check("真实报告保留五条钱路", len(content["pathSummaries"]) == 5)
        check("页面显示五条钱路", page.locator(".wealth-path").count() == 5)
        check("页面显示三个年度", page.locator(".wealth-year").count() == 3)
        expected_annual = group_annual_copy(content)
        check("页面年度正文与保存内容分组一致", rendered_annual_copy(page) == expected_annual)
        check("页面显示合并后的留钱段", page.locator(".wealth-v3-year__retention").count()
              == len({item["firstYear"] for item in expected_annual["retention"]}))
        check("页面显示年度比较", page.locator(".wealth-v3-year__comparison").count() == 2)
        expected_risk_years = {item["firstYear"] for item in expected_annual["risk"]}
        check("风险卡数量与保存内容分组一致",
              page.locator(".wealth-v3-year__risk").count() == len(expected_risk_years))
        check(
            "三年风险摘要按内容决定",
            page.locator(".wealth-v3-risk-summary").count() == (1 if content["riskSummary"] is not None else 0),
        )
        annual_overviews = [year["overview"]["text"] for year in content["years"]]
        changed_pairs = [
            index
            for index, year in enumerate(content["years"])
            if index > 0 and year["comparison"] is not None and year["comparison"]["direction"] == "changed"
        ]
        check(
            "年度依据变化时标题同步变化",
            all(annual_overviews[index] != annual_overviews[index - 1] for index in changed_pairs),
        )
        rendered_overviews = page.locator(".wealth-year .report-year__verdict").all_inner_texts()
        check("页面年度标题与保存内容一致", rendered_overviews == annual_overviews)
        reader_text = page.locator(".wealth-v3-reader").inner_text()
        check("通俗页不暴露内部审计字段", all(term not in reader_text for term in ("wealth-path-v2", "净分", "专业依据")))
        check("真实页面生成正文没有重复句", not repeated_visible_sentences(page))

        saved_snapshot = reader_text
        page.reload(wait_until="networkidle")
        page.wait_for_selector(".wealth-v3-reader", timeout=20000)
        check("刷新后按快照保留正文", page.locator(".wealth-v3-reader").inner_text() == saved_snapshot)

        page.goto(f"{BASE}/reports", wait_until="networkidle")
        saved_link = page.locator(f'a[href="/reports/{report_id}"]')
        check("财富v3进入我的命书", saved_link.count() == 1)
        saved_link.click()
        page.wait_for_selector(".wealth-v3-reader", timeout=20000)
        check("从书架重新打开同一快照", page.locator(".wealth-v3-reader").inner_text() == saved_snapshot)

        for width, height in ((360, 800), (430, 932), (1280, 900)):
            no_overflow, paper_aligned = viewport_ok(page, width, height)
            check(f"真实财富命书{width}px无横向溢出", no_overflow)
            check(f"真实财富命书{width}px纸张对齐", paper_aligned)
            page.screenshot(path=f"/tmp/wealth-v3-reader-{width}.png", full_page=True)

        # 通过同一前端阅读组件加载经契约校验的合成无风险例子，只验证边界布局，
        # 不把它冒充为真实命盘生成或 MySQL 保存结果。
        examples = json.loads((ROOT / "contracts/drafts/wealth-v3.examples.json").read_text())["examples"]
        no_risk = next(example["report"] for example in examples if example["name"] == "no-risk")
        no_risk["id"] = 999999
        page.route(
            "**/api/v1/reports/999999",
            lambda route: route.fulfill(
                status=200,
                content_type="application/json",
                body=json.dumps(no_risk, ensure_ascii=False),
            ),
        )
        page.goto(f"{BASE}/reports/999999", wait_until="networkidle")
        page.wait_for_selector(".wealth-v3-reader", timeout=15000)
        check("合成无风险不渲染三年提醒卡", page.locator(".wealth-v3-risk-summary").count() == 0)
        check("合成无风险不渲染年度提醒卡", page.locator(".wealth-v3-year__risk").count() == 0)
        check("合成无风险不显示占位文案", "暂无风险" not in page.inner_text("body"))
        gap = page.evaluate(
            "() => document.querySelector('.wealth-reader__years').getBoundingClientRect().top"
            " - document.querySelector('.wealth-reader__paths').getBoundingClientRect().bottom"
        )
        check("合成无风险不保留空卡间距", gap <= 40)
        no_overflow, paper_aligned = viewport_ok(page, 360, 800)
        check("合成无风险360px无横向溢出", no_overflow)
        check("合成无风险360px纸张对齐", paper_aligned)
        page.screenshot(path="/tmp/wealth-v3-no-risk-360.png", full_page=True)
        page.unroute("**/api/v1/reports/999999")

        # 同一非会员用 12 组完整出生输入逐份购买，验证不允许绕过购买且不限制购买次数。
        cases = json.loads(
            (ROOT / "apps/server/src/test/resources/report/wealth-v2-baseline-inputs.json").read_text()
        )["fullBirthCases"]
        for case in cases:
            checkout_response = page.request.post(
                f"{BASE}/api/v1/reports/checkout",
                headers={"Authorization": f"Bearer {token}"},
                data={"request": case["request"], "topic": "wealth", "edition": "plain"},
                timeout=30000,
            )
            check(f"{case['id']}-创建单份购买订单", checkout_response.status == 200)
            if checkout_response.status != 200:
                continue
            checkout = checkout_response.json()
            locked_read = page.request.get(
                f"{BASE}/api/v1/reports/{checkout['reportId']}",
                headers={"Authorization": f"Bearer {token}"},
            )
            check(f"{case['id']}-支付前不可读取", locked_read.status == 404)
            paid = page.request.post(
                f"{BASE}/api/v1/reports/checkout/{checkout['orderId']}/mock-pay",
                headers={"Authorization": f"Bearer {token}"},
                timeout=30000,
            )
            check(f"{case['id']}-模拟支付解锁", paid.status == 200)
            if paid.status != 200:
                continue
            payload = paid.json()
            check(f"{case['id']}-支付后仍为同一报告", payload["id"] == checkout["reportId"])
            report_ids.append(payload["id"])
            api_content_bytes.append(
                len(json.dumps(payload["content"], ensure_ascii=False, separators=(",", ":")).encode("utf-8"))
            )
            check(f"{case['id']}-返回v4契约", payload["contentVersion"] == EXPECTED_CONTENT_VERSION)
            check(f"{case['id']}-返回当前财富文案版本", payload["content"]["copyVersion"] == EXPECTED_COPY_VERSION)
            if case["id"] == "R12":
                r12_overviews = [year["overview"]["text"] for year in payload["content"]["years"]]
                check("R12-共同主线不再重复占据年度标题", all(
                    "靠能力赚钱可以作为关注的一个方向。" not in text for text in r12_overviews
                ))
                check("R12-三年年度标题分别反映当年信号", len(set(r12_overviews)) == 3)
            reread = page.request.get(
                f"{BASE}/api/v1/reports/{payload['id']}",
                headers={"Authorization": f"Bearer {token}"},
            )
            check(f"{case['id']}-支付后回读", reread.status == 200 and reread.json()["content"] == payload["content"])
            page.goto(f"{BASE}/reports/{payload['id']}", wait_until="networkidle")
            page.wait_for_selector(".wealth-v3-reader", timeout=20000)
            check(f"{case['id']}-保存接口年度正文与页面一致",
                  rendered_annual_copy(page) == group_annual_copy(payload["content"]))
            repeated = repeated_visible_sentences(page)
            if repeated:
                visible_collisions[case["id"]] = repeated
                print(f"碰撞明细 {case['id']}: {repeated}")
            check(f"{case['id']}-真实页面生成正文无碰撞", not repeated)
            sample_text = page.locator(".wealth-v3-reader").inner_text()
            candidate_samples.append((case["id"], sample_text.replace(payload["subject"], f"样本 {case['id']}")))

        second_context = browser.new_context(viewport={"width": 430, "height": 932})
        second_page = second_context.new_page()
        second_token = register(second_page, OTHER_USER)
        second_page.goto(f"{BASE}/reports", wait_until="networkidle")
        check("账号二书架为空", "书架还是空的" in second_page.inner_text("body"))
        isolated = second_page.request.get(
            f"{BASE}/api/v1/reports/{report_id}",
            headers={"Authorization": f"Bearer {second_token}"},
        )
        check("账号二不能读取账号一命书", isolated.status == 404)
        second_page.goto(f"{BASE}/reports/{report_id}", wait_until="networkidle")
        check("账号二页面不泄露账号一正文", second_page.locator(".wealth-v3-reader").count() == 0)

        # 账号二开通会员后前三份直生成，第四份只提示单独购买，不自动创建订单。
        membership_order = second_page.request.post(
            f"{BASE}/api/v1/orders",
            headers={"Authorization": f"Bearer {second_token}"},
            data={"plan": "member_1m"},
        )
        check("会员订单创建", membership_order.status == 200)
        membership_paid = second_page.request.post(
            f"{BASE}/api/v1/pay/mock-success/{membership_order.json()['id']}",
            headers={"Authorization": f"Bearer {second_token}"},
        )
        check("会员模拟开通", membership_paid.status == 200 and membership_paid.json()["isMember"])
        for index in range(3):
            direct = second_page.request.post(
                f"{BASE}/api/v1/reports",
                headers={"Authorization": f"Bearer {second_token}"},
                data={"request": cases[index]["request"], "topic": "wealth", "edition": "plain"},
                timeout=30000,
            )
            check(f"会员第{index + 1}份额度内直生成", direct.status == 200)

        second_page.goto(f"{BASE}/input", wait_until="networkidle")
        second_page.get_by_placeholder("请输入姓名").fill("任务五会员样本")
        second_page.get_by_role("button", name="开始排盘").click()
        second_page.wait_for_selector(".bazi-table", timeout=15000)
        second_page.goto(f"{BASE}/report", wait_until="networkidle")
        second_page.get_by_role("radio", name=re.compile("财富运势")).click()
        checkout_requests: list[str] = []
        second_page.on(
            "request",
            lambda request: checkout_requests.append(request.url)
            if request.url.endswith("/api/v1/reports/checkout") else None,
        )
        with second_page.expect_response(
            lambda response: response.url.endswith("/api/v1/reports")
            and response.request.method == "POST",
            timeout=30000,
        ) as limit_info:
            second_page.get_by_role("button", name="使用会员权益生成通俗版命书").click()
        check("会员第四份返回额度提示", limit_info.value.status == 400)
        second_page.locator(".report-order__status").get_by_text(
            "今天的3份会员命书已全部生成；继续生成需单独购买，本次未扣费"
        ).wait_for()
        check("额度用完不自动创建购买订单", not checkout_requests)
        check(
            "额度用完明确显示单独购买入口",
            second_page.get_by_role("button", name="单独购买通俗版命书 · ¥6.9").is_visible(),
        )
        with second_page.expect_response(
            lambda response: response.url.endswith("/api/v1/reports/checkout"), timeout=30000
        ) as member_checkout_info, second_page.expect_response(
            lambda response: re.search(r"/api/v1/reports/checkout/\d+/mock-pay$", response.url)
            is not None,
            timeout=30000,
        ) as member_payment_info:
            second_page.get_by_role("button", name="单独购买通俗版命书 · ¥6.9").click()
        check(
            "会员超额单独购买后解锁同一份报告",
            member_payment_info.value.status == 200
            and member_payment_info.value.json()["id"] == member_checkout_info.value.json()["reportId"],
        )

        second_context.close()
        first_context.close()
        browser.close()

    check("浏览器控制台无错误", not console_errors)
    SAMPLE_PATH.parent.mkdir(parents=True, exist_ok=True)
    SAMPLE_PATH.write_text(
        "# 财富通俗版 v3.6 候选样本（R01–R12）\n\n"
        "> 由保存接口回读后，经真实前端页面提取；仅供人工审核，尚未代表发布通过。\n\n"
        + "\n\n---\n\n".join(
            f"## {fixture_id}\n\n```text\n{text}\n```" for fixture_id, text in candidate_samples
        )
        + "\n",
        encoding="utf-8",
    )
    RESULT_PATH.write_text(
        json.dumps(
            {
                "username": USER,
                "otherUsername": OTHER_USER,
                "reportIds": report_ids,
                "apiContentBytes": api_content_bytes,
                "maxApiContentBytes": max(api_content_bytes, default=0),
                "checks": total,
                "failures": failures,
                "consoleErrors": console_errors,
                "candidateSamplePath": str(SAMPLE_PATH),
                "visibleCollisions": visible_collisions,
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    passed = total - len(failures)
    print(f"\n结果: {passed}/{total} 通过" + (f"，失败: {failures}" if failures else ""))
    print(f"结果文件: {RESULT_PATH}")
    print(f"候选样本: {SAMPLE_PATH}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
