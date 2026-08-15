"""八字排盘 App E2E 冒烟：注册登录 → 排盘 → 会员购买 → 分享 → 历史 → 刷新兜底 → 退出登录清空。

前置：前端 VITE_API_MODE=http dev server（5173）+ 后端 mysql profile（8080）。
运行：python3 e2e/run_e2e.py
"""

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

        # 3. 会员购买（模拟支付）
        page.goto(f"{BASE}/membership", wait_until="networkidle")
        page.get_by_role("button", name="立即开通").click()
        page.wait_for_selector("text=已开通", timeout=20000)
        mb = page.inner_text("body")
        check("会员购买开通", "已开通" in mb or "续费" in mb)

        # 4. 分享卡片
        page.goto(f"{BASE}/chart", wait_until="networkidle")
        page.wait_for_selector(".bazi-table", timeout=15000)
        page.locator('button[aria-label="保存命盘"]').click()
        page.wait_for_selector(".picker-mask", timeout=20000)
        sheet = page.inner_text("body")
        check("分享卡片弹出", "保存" in sheet or "复制" in sheet or "命盘" in sheet)

        # 5. 历史记录（云端同步）
        page.goto(f"{BASE}/history", wait_until="networkidle")
        page.wait_for_timeout(1500)
        hist = page.inner_text("body")
        check("历史记录存在", "乙" in hist or "1995" in hist or "命盘" in hist)

        # 6. 刷新后数据仍在（useBaziWithFallback 兜底）
        page.goto(f"{BASE}/chart", wait_until="networkidle")
        page.wait_for_selector(".bazi-table", timeout=15000)
        page.reload(wait_until="networkidle")
        page.wait_for_selector(".bazi-table", timeout=15000)
        check("刷新后排盘仍在", "档案：" in page.inner_text("body"))

        # 7. 退出登录：本地历史与当前命盘清空（云端记录属于账号，重登仍可见）
        page.goto(f"{BASE}/profile", wait_until="networkidle")
        page.get_by_text("退出登录").click()
        page.wait_for_selector("text=未登录", timeout=10000)
        check("退出登录", "未登录" in page.inner_text("body"))
        page.goto(f"{BASE}/history", wait_until="networkidle")
        page.wait_for_timeout(1500)
        hist_after = page.inner_text("body")
        check("登出后本地历史清空", "暂无排盘记录" in hist_after)

        browser.close()

    passed = 7 - len(failures)
    print(f"\n结果: {passed}/7 通过" + (f"，失败: {failures}" if failures else ""))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
