"""八字排盘 App E2E 冒烟：注册登录 → 排盘 → 感情命书三状态 → 会员购买 → 分享 → 历史 → 刷新兜底 → 退出登录清空 → 设置-关于合规。

前置：前端 VITE_API_MODE=http dev server（5173）+ 后端服务（8080）。
运行：python3 e2e/run_e2e.py
"""

import re
import sys
import uuid

from playwright.sync_api import sync_playwright

BASE = "http://localhost:5173"
USER = f"e2e_{uuid.uuid4().hex[:10]}"
PASSWORD = "pass123"


def main() -> int:
    failures: list[str] = []

    def check(name: str, cond: bool) -> None:
        print(("PASS: " if cond else "FAIL: ") + name)
        if not cond:
            failures.append(name)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # 1. 注册登录
        page.goto(f"{BASE}/auth", wait_until="networkidle")
        page.locator(".opt", has_text="注册").click()
        page.get_by_placeholder("请输入用户名").fill(USER)
        page.get_by_placeholder("请输入密码").fill(PASSWORD)
        page.locator("button", has_text="注册").last.click()
        page.wait_for_url("**/profile", timeout=15000)
        check("注册登录", USER in page.inner_text("body"))

        # 2. 排盘（http 模式需登录）
        page.goto(f"{BASE}/input", wait_until="networkidle")
        page.get_by_role("button", name="开始排盘").click()
        page.wait_for_selector(".bazi-table", timeout=15000)
        body = page.inner_text("body")
        check("排盘四柱表", "乙" in body and "亥" in body)
        check("一句话档案", "档案：" in body)

        # 3. 感情命书：三种状态都能生成、保存并重新打开
        relationship_statuses = [
            ("单身或尚未确定关系", "值得继续了解"),
            ("已确认交往关系", "继续走下去"),
            ("已婚或长期共同生活", "共同生活"),
        ]
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
            check(f"{status_label}-三年内容", all(year in reader for year in ("2026", "2027", "2028")))

        page.goto(f"{BASE}/reports", wait_until="networkidle")
        library = page.inner_text("body")
        check("感情命书进入书架", library.count("感情运势 · 通俗版") >= 3)
        first_relationship = page.locator('a[href^="/reports/"]', has_text="感情运势").first
        first_relationship.click()
        page.wait_for_url("**/reports/*", timeout=10000)
        page.wait_for_selector(".relationship-reader", timeout=20000)
        check("书架重新打开感情命书", "感情命书 · 通俗版" in page.inner_text("body"))

        page.set_viewport_size({"width": 430, "height": 932})
        page.wait_for_timeout(200)
        no_overflow = page.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth")
        paper_aligned = page.locator(".report-reader__paper").evaluate(
            "el => Math.abs(el.getBoundingClientRect().left) < 1 && "
            "Math.abs(el.getBoundingClientRect().right - document.documentElement.clientWidth) < 1"
        )
        check("感情命书430px无横向溢出", no_overflow)
        check("感情命书纸张与背景对齐", paper_aligned)
        page.screenshot(path="/tmp/relationship-reader-430.png", full_page=True)
        page.set_viewport_size({"width": 1280, "height": 900})
        page.wait_for_timeout(200)
        page.screenshot(path="/tmp/relationship-reader-desktop.png", full_page=True)

        # 4. 会员购买（模拟支付）
        page.goto(f"{BASE}/membership", wait_until="networkidle")
        page.get_by_role("button", name="立即开通").click()
        page.wait_for_selector("text=已开通", timeout=20000)
        mb = page.inner_text("body")
        check("会员购买开通", "已开通" in mb or "续费" in mb)

        # 5. 分享卡片
        page.goto(f"{BASE}/chart", wait_until="networkidle")
        page.wait_for_selector(".bazi-table", timeout=15000)
        page.locator('button[aria-label="保存命盘"]').click()
        page.wait_for_selector(".picker-mask", timeout=20000)
        sheet = page.inner_text("body")
        check("分享卡片弹出", "保存" in sheet or "复制" in sheet or "命盘" in sheet)

        # 6. 历史记录（云端同步）
        page.goto(f"{BASE}/history", wait_until="networkidle")
        page.wait_for_timeout(1500)
        hist = page.inner_text("body")
        check("历史记录存在", "乙" in hist or "1995" in hist or "命盘" in hist)

        # 7. 刷新后数据仍在（useBaziWithFallback 兜底）
        page.goto(f"{BASE}/chart", wait_until="networkidle")
        page.wait_for_selector(".bazi-table", timeout=15000)
        page.reload(wait_until="networkidle")
        page.wait_for_selector(".bazi-table", timeout=15000)
        check("刷新后排盘仍在", "档案：" in page.inner_text("body"))

        # 8. 退出登录：本地历史与当前命盘清空（云端记录属于账号，重登仍可见）
        page.goto(f"{BASE}/profile", wait_until="networkidle")
        page.get_by_text("退出登录").click()
        page.wait_for_selector("text=未登录", timeout=10000)
        check("退出登录", "未登录" in page.inner_text("body"))
        page.goto(f"{BASE}/history", wait_until="networkidle")
        page.wait_for_timeout(1500)
        hist_after = page.inner_text("body")
        check("登出后本地历史清空", "暂无排盘记录" in hist_after)

        # 9. 设置-关于与合规（首页齿轮入口）
        page.goto(f"{BASE}/", wait_until="networkidle")
        page.locator('button[aria-label="设置"]').first.click()
        page.wait_for_selector("text=关于与合规", timeout=10000)
        settings_body = page.inner_text("body")
        check("设置-关于与合规", "仅供娱乐与参考" in settings_body and "版本" in settings_body)

        browser.close()

    total = 21
    passed = total - len(failures)
    print(f"\n结果: {passed}/{total} 通过" + (f"，失败: {failures}" if failures else ""))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
