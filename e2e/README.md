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

## 财富真人盲测离线统计

真人评分不得写进仓库。先把示例复制到根目录已忽略的 `.private-evals/`，再填写随机编号与评分：

```bash
mkdir -p .private-evals
cp e2e/wealth-blind-review.example.json .private-evals/wealth-pilot-001.json
python3 e2e/wealth_blind_review.py .private-evals/wealth-pilot-001.json
```

如果已经取得 `/api/v1/reports` 返回的财富报告数组，先把完整响应保存在私有目录，例如 `.private-evals/private-reports.json`，再自动生成不含姓名和正文的评分骨架：

```bash
python3 e2e/wealth_blind_review.py .private-evals/private-reports.json --init-reports --batch-id wealth-v35-dry-run-001 --commit 1dc9abf > .private-evals/wealth-v35-dry-run-001.json
```

初始化只接受 `wealth-narrative-v4` + `wealth-plain-v3.18` 的已完成财富通俗版报告。统计器仍可复算历史 v3.4、v3.5、v3.6、v3.7、v3.8、v3.9、v3.10、v3.11、v3.12、v3.13、v3.14、v3.15、v3.16、v3.17 评分文件，但会拒绝在同一批次混用不同文案版本。填写骨架里的 A/B/C/D、0–3 分和失败原因后，再运行不带 `--init-reports` 的统计命令。

统计器会校验版本、重复报告、评分范围和失败原因，并拒绝姓名、生日、出生时间、出生地点、手机号、邮箱等个人字段。汇总输出只包含聚合指标，不回显报告正文或单份哈希。

运行统计器单元测试：

```bash
cd e2e
python3 -m unittest test_wealth_blind_review.py
```
