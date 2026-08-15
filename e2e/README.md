# E2E 冒烟测试（Playwright）

覆盖真实用户主路径：注册登录 → 排盘 → 会员购买（模拟支付）→ 分享卡片 → 历史记录 → 刷新兜底。

## 前置条件

1. 后端 MySQL profile 运行中（`cd apps/server && SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run`，MySQL 容器 `docker compose up -d`）
2. 前端 http 模式 dev server 运行中（`cd apps/web && VITE_API_MODE=http npm run dev`）
3. Python Playwright：

```bash
python3 -m pip install playwright --user
python3 -m playwright install chromium
```

## 运行

```bash
python3 e2e/run_e2e.py
```

全部通过输出 `PASS`；任一失败输出 `FAIL` 并以非零码退出。每次运行使用随机用户名，不污染数据。

## 说明

- 脚本为 headless Chromium，依赖真实前后端链路（非 mock），排盘/会员/历史均走 `http://localhost:8080` API 与 MySQL。
- 曾在开发中借此发现「刷新后 /chart 立即跳回 /input」的 bug（历史兜底异步加载时页面过早导航），修复见 `useBaziWithFallback` 的 loading 状态。
