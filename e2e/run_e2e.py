"""八字排盘 App E2E 冒烟：注册登录 → 排盘 → 感情命书 → 会员综合命书 → 分享 → 历史 → 退出登录 → 合规。

前置：localhost:5173 的 VITE_API_MODE=http 前端 + localhost:8080 的真实后端。
运行：python3 e2e/run_e2e.py
"""

import os
import re
import sys
import uuid
from urllib.parse import urlparse

from playwright.sync_api import sync_playwright

BASE = os.environ.get("E2E_BASE_URL", "http://localhost:5173").rstrip("/")
API_BASE = os.environ.get("E2E_API_BASE_URL", "http://localhost:8080").rstrip("/")
USER = f"e2e_{uuid.uuid4().hex[:10]}"
PASSWORD = "pass123"


def require_localhost(url: str, expected_port: int, label: str) -> None:
    parsed = urlparse(url)
    if parsed.scheme != "http" or parsed.hostname != "localhost" or parsed.port != expected_port:
        raise RuntimeError(
            f"{label} 必须使用 http://localhost:{expected_port}，"
            "不能以 127.0.0.1 或 mock 模式代替真实联调"
        )


def overall_payload(name: str, *, invalid_birth_place: bool = False) -> dict:
    request = {
        "name": name,
        "gender": "male",
        "solarDateTime": "1995-10-08T14:30:00",
        "trueSolarTime": invalid_birth_place,
    }
    if invalid_birth_place:
        request["birthPlace"] = "上海"
    return {"request": request, "topic": "overall", "edition": "plain"}


def main() -> int:
    require_localhost(BASE, 5173, "前端地址")
    require_localhost(API_BASE, 8080, "后端地址")
    failures: list[str] = []
    total = 0

    def check(name: str, cond: bool) -> None:
        nonlocal total
        total += 1
        print(("PASS: " if cond else "FAIL: ") + name)
        if not cond:
            failures.append(name)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        console_errors: list[str] = []
        page.on("console", lambda message: console_errors.append(message.text)
                if message.type == "error" else None)

        # 1. 注册登录
        page.goto(f"{BASE}/auth", wait_until="networkidle")
        page.locator(".opt", has_text="注册").click()
        page.get_by_placeholder("请输入用户名").fill(USER)
        page.get_by_placeholder("请输入密码").fill(PASSWORD)
        with page.expect_response("**/api/v1/auth/register", timeout=15000) as register_info:
            page.locator("button", has_text="注册").last.click()
        register_response = register_info.value
        token = register_response.json().get("token") if register_response.status == 200 else None
        page.wait_for_url("**/profile", timeout=15000)
        check("注册登录走真实 HTTP", register_response.status == 200 and bool(token)
              and USER in page.inner_text("body"))

        cors_probe = page.evaluate(
            """async ({ url, token }) => {
              try {
                const response = await fetch(url, {
                  headers: { Authorization: `Bearer ${token}` },
                });
                return { status: response.status, body: await response.json() };
              } catch (error) {
                return { status: 0, body: String(error) };
              }
            }""",
            {"url": f"{API_BASE}/api/v1/me", "token": token},
        )
        check("localhost:5173 跨域白名单可用",
              cors_probe["status"] == 200 and cors_probe["body"].get("username") == USER)

        # 2. 排盘（http 模式需登录）
        page.goto(f"{BASE}/input", wait_until="networkidle")
        page.get_by_role("button", name="开始排盘").click()
        page.wait_for_selector(".bazi-table", timeout=15000)
        body = page.inner_text("body")
        check("排盘四柱表", "乙" in body and "亥" in body)
        check("一句话档案", "档案：" in body)

        # 3. 感情命书：三种状态都能生成、保存并重新打开
        relationship_statuses = [
            ("单身或尚未确定关系", "今年有没有认识人的机会？"),
            ("已确认交往关系", "继续走下去"),
            ("已婚或长期共同生活", "共同生活"),
        ]
        single_url = ""
        single_snapshot = ""
        for status_label, required_text in relationship_statuses:
            page.goto(f"{BASE}/report", wait_until="networkidle")
            page.get_by_role("radio", name=re.compile("感情运势")).click()
            professional = page.get_by_role("radio", name=re.compile("专业版"))
            check(f"{status_label}-专业版禁用", professional.is_disabled())
            page.get_by_role("radio", name=status_label).click()
            page.get_by_role("button", name="生成通俗版命书 · ¥6.9").click()
            page.wait_for_url("**/reports/*", timeout=20000)
            page.wait_for_selector(".relationship-reader", timeout=20000)
            reader = page.inner_text("body")
            check(f"{status_label}-生成并打开", status_label in reader and required_text in reader)
            if status_label == "单身或尚未确定关系":
                single_url = page.url
                single_snapshot = page.locator(".relationship-single-reader").inner_text()
                check("单身-今年详细明年参考", "2026 年重点｜2027 年简短参考" in reader
                      and "2028" not in reader and "三年总断" not in reader)
                check("单身-不重复五维卡片与年度全章", "五个方面逐项看" not in reader
                      and page.locator(".relationship-single-section").count() == 3)
                check("单身-两种接触情况不增加问卷", "如果你目前没有正在了解的人" in reader
                      and "如果已经有聊得来的人" in reader)
                check("单身-明年单独摘要", page.locator(".relationship-single-reader__outlook").count() == 1)
                check("单身-只给当前年一个行动闭环", page.locator(".annual-action-guide").count() == 1)
                check("单身-行动闭环说明结果与调整", "这样做，可能看到什么变化" in reader
                      and "没有改善时怎么调整" in reader)
                page.reload(wait_until="networkidle")
                page.wait_for_selector(".relationship-single-reader")
                check("单身-刷新按快照保留正文", page.locator(".relationship-single-reader").inner_text() == single_snapshot)
                continue
            check(f"{status_label}-三年内容", all(year in reader for year in ("2026", "2027", "2028")))
            check(f"{status_label}-每年一个行动闭环", page.locator(".annual-action-guide").count() == 3)
            check(f"{status_label}-新版不再叠加旧行动清单",
                  page.locator(".relationship-year__actions").count() == 0)
            judgments = page.locator(".relationship-year__judgment p").all_text_contents()
            # This fixture has the same primary/tone in years 2–3 but different annual evidence.
            check(f"{status_label}-相同重点解释年度差异", len(judgments) == 3 and judgments[1] != judgments[2])
            check(f"{status_label}-逐年说明与上年比较", len(judgments) == 3 and all("上一年" in text for text in judgments[1:]))
            check(f"{status_label}-观察提示与最后一年边界", reader.count("现实中可以留意") == 3 and reader.count("阅读提醒：") == 1)
            if status_label == "已婚或长期共同生活":
                page.set_viewport_size({"width": 430, "height": 932})
                page.wait_for_timeout(200)
                check("已婚命书430px无横向溢出",
                      page.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth"))
                page.screenshot(path="/tmp/relationship-married-reader-430.png", full_page=True)
                page.set_viewport_size({"width": 1280, "height": 900})

        page.goto(f"{BASE}/reports", wait_until="networkidle")
        library = page.inner_text("body")
        check("感情命书进入书架", library.count("感情运势 · 通俗版") >= 3)
        first_relationship = page.locator('a[href^="/reports/"]', has_text="感情运势").first
        first_relationship.click()
        page.wait_for_url("**/reports/*", timeout=10000)
        page.wait_for_selector(".relationship-reader", timeout=20000)
        check("书架重新打开感情命书", "感情命书 · 通俗版" in page.inner_text("body"))

        page.goto(f"{BASE}/reports", wait_until="networkidle")
        page.locator(f'a[href="{single_url.removeprefix(BASE)}"]').click()
        page.wait_for_selector(".relationship-single-reader")
        check("书架重新打开单身新版", page.locator(".relationship-single-reader").inner_text() == single_snapshot)

        page.set_viewport_size({"width": 430, "height": 932})
        page.wait_for_timeout(200)
        no_overflow = page.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth")
        paper_aligned = page.locator(".report-reader__paper").evaluate(
            "el => Math.abs(el.getBoundingClientRect().left) < 1 && "
            "Math.abs(el.getBoundingClientRect().right - document.documentElement.clientWidth) < 1"
        )
        check("感情命书430px无横向溢出", no_overflow)
        check("感情命书纸张与背景对齐", paper_aligned)
        page.screenshot(path="/tmp/relationship-single-reader-430.png", full_page=True)
        page.set_viewport_size({"width": 360, "height": 800})
        check("单身命书360px无横向溢出", page.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth"))
        page.set_viewport_size({"width": 1280, "height": 900})
        page.wait_for_timeout(200)
        page.screenshot(path="/tmp/relationship-single-reader-desktop.png", full_page=True)

        # 4. 会员购买（模拟支付）
        page.goto(f"{BASE}/membership", wait_until="networkidle")
        page.get_by_role("button", name="立即开通").click()
        page.wait_for_selector("text=已开通", timeout=20000)
        mb = page.inner_text("body")
        check("会员购买开通", "已开通" in mb or "续费" in mb)

        # 5. 综合 v3：失败不占次数，真实页面生成、刷新、书架重开
        auth_headers = {"Authorization": f"Bearer {token}"}
        reports_before_failure = page.request.get(
            f"{API_BASE}/api/v1/reports", headers=auth_headers)
        check("生成前异常-读取失败前报告数", reports_before_failure.status == 200)
        before_count = len(reports_before_failure.json())

        failed_generation = page.request.post(
            f"{API_BASE}/api/v1/reports",
            headers=auth_headers,
            data=overall_payload("综合失败不扣次", invalid_birth_place=True),
        )
        failed_body = failed_generation.json()
        check("生成前异常返回明确错误", failed_generation.status == 400
              and failed_body.get("code") == "BIRTH_PLACE_UNRESOLVED")
        reports_after_failure = page.request.get(
            f"{API_BASE}/api/v1/reports", headers=auth_headers)
        check("生成前异常不保存报告", reports_after_failure.status == 200
              and len(reports_after_failure.json()) == before_count)

        page.goto(f"{BASE}/report?topic=overall", wait_until="networkidle")
        overall_button = page.get_by_role(
            "button", name="使用会员权益生成通俗版命书")
        overall_button.wait_for(state="visible", timeout=15000)
        check("综合版会员入口可点击", overall_button.is_enabled())
        with page.expect_response(
            lambda response: response.url.endswith("/api/v1/reports")
            and response.request.method == "POST",
            timeout=30000,
        ) as overall_response_info:
            overall_button.click()
        overall_response = overall_response_info.value
        overall_report = overall_response.json()
        check("综合 v3 真实生成", overall_response.status == 200
              and overall_report.get("contentVersion") == "overall-narrative-v3")
        overall_report_id = overall_report["id"]
        page.wait_for_url(f"**/reports/{overall_report_id}", timeout=30000)
        page.wait_for_selector(".overall-v3-reader", timeout=30000)
        overall_snapshot = page.locator(".overall-v3-reader").inner_text()
        check("综合 v3 恰好三年", page.locator(".overall-v3-year").count() == 3)
        check("综合 v3 每年一张行动卡", page.locator(".annual-action-guide").count() == 3)
        check("综合 v3 不叠加旧四维与行动列表",
              page.locator(".overall-year__dimensions").count() == 0
              and page.locator(".overall-year__priority").count() == 0
              and page.locator(".relationship-year__actions").count() == 0)

        page.reload(wait_until="networkidle")
        page.wait_for_selector(".overall-v3-reader", timeout=30000)
        check("综合 v3 刷新后按快照读取",
              page.locator(".overall-v3-reader").inner_text() == overall_snapshot)

        page.goto(f"{BASE}/reports", wait_until="networkidle")
        saved_overall = page.locator(f'a[href="/reports/{overall_report_id}"]')
        check("综合 v3 已进入我的命书", saved_overall.count() == 1)
        saved_overall.click()
        page.wait_for_selector(".overall-v3-reader", timeout=30000)
        check("书架重开综合 v3 保留正文",
              page.locator(".overall-v3-reader").inner_text() == overall_snapshot)

        for width, height in ((430, 932), (360, 800)):
            page.set_viewport_size({"width": width, "height": height})
            page.wait_for_timeout(200)
            check(f"综合 v3 {width}px 无横向溢出",
                  page.evaluate(
                      "document.documentElement.scrollWidth <= "
                      "document.documentElement.clientWidth"))
            check(f"综合 v3 {width}px 纸张与背景对齐",
                  page.locator(".report-reader__paper").evaluate(
                      "el => Math.abs(el.getBoundingClientRect().left) < 1 && "
                      "Math.abs(el.getBoundingClientRect().right - "
                      "document.documentElement.clientWidth) < 1"))
        page.screenshot(path="/tmp/overall-v3-e2e-360.png", full_page=True)
        page.set_viewport_size({"width": 1280, "height": 900})

        # 失败发生后仍可成功生成完整 3 份，证明失败没有占用会员当日次数。
        for index in (2, 3):
            additional = page.request.post(
                f"{API_BASE}/api/v1/reports",
                headers=auth_headers,
                data=overall_payload(f"综合额度验证{index}"),
            )
            check(f"失败后会员第{index}份仍可生成", additional.status == 200
                  and additional.json().get("contentVersion") == "overall-narrative-v3")
        reports_after_three = page.request.get(
            f"{API_BASE}/api/v1/reports", headers=auth_headers)
        check("失败未占当日次数", reports_after_three.status == 200
              and len(reports_after_three.json()) == before_count + 3)
        over_limit = page.request.post(
            f"{API_BASE}/api/v1/reports",
            headers=auth_headers,
            data=overall_payload("综合额度边界"),
        )
        check("三份成功后才触发会员上限", over_limit.status == 400
              and over_limit.json().get("code") == "MEMBER_DAILY_REPORT_LIMIT")

        # 6. 分享卡片
        page.goto(f"{BASE}/chart", wait_until="networkidle")
        page.wait_for_selector(".bazi-table", timeout=15000)
        page.locator('button[aria-label="保存命盘"]').click()
        page.wait_for_selector(".picker-mask", timeout=20000)
        sheet = page.inner_text("body")
        check("分享卡片弹出", "保存" in sheet or "复制" in sheet or "命盘" in sheet)

        # 7. 历史记录（云端同步）
        page.goto(f"{BASE}/history", wait_until="networkidle")
        page.wait_for_timeout(1500)
        hist = page.inner_text("body")
        check("历史记录存在", "乙" in hist or "1995" in hist or "命盘" in hist)

        # 8. 刷新后数据仍在（useBaziWithFallback 兜底）
        page.goto(f"{BASE}/chart", wait_until="networkidle")
        page.wait_for_selector(".bazi-table", timeout=15000)
        page.reload(wait_until="networkidle")
        page.wait_for_selector(".bazi-table", timeout=15000)
        check("刷新后排盘仍在", "档案：" in page.inner_text("body"))

        # 9. 退出登录：本地历史与当前命盘清空（云端记录属于账号，重登仍可见）
        page.goto(f"{BASE}/profile", wait_until="networkidle")
        page.get_by_text("退出登录").click()
        page.wait_for_selector("text=未登录", timeout=10000)
        check("退出登录", "未登录" in page.inner_text("body"))
        page.goto(f"{BASE}/history", wait_until="networkidle")
        page.wait_for_timeout(1500)
        hist_after = page.inner_text("body")
        check("登出后本地历史清空", "暂无排盘记录" in hist_after)

        # 10. 设置-关于与合规（首页齿轮入口）
        page.goto(f"{BASE}/", wait_until="networkidle")
        page.locator('button[aria-label="设置"]').first.click()
        page.wait_for_selector("text=关于与合规", timeout=10000)
        settings_body = page.inner_text("body")
        check("设置-关于与合规", "仅供娱乐与参考" in settings_body and "版本" in settings_body)
        check("页面控制台无错误", console_errors == [])

        browser.close()

    passed = total - len(failures)
    print(f"\n结果: {passed}/{total} 通过" + (f"，失败: {failures}" if failures else ""))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
