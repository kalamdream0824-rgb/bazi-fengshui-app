# E2E 冒烟测试（Playwright）

覆盖真实用户主路径：注册登录 → 排盘 → 感情命书 → 会员购买（模拟支付）→ 综合 v3 → 分享卡片 → 历史记录 → 刷新兜底。

## 前置条件

1. 后端固定允许 `localhost:5173`，使用本地 dev 数据库启动：

```bash
cd apps/server
APP_CORS_ORIGINS=http://localhost:5173 ./mvnw spring-boot:run
```

2. 前端固定使用真实 HTTP 模式和 `localhost`：

```bash
cd apps/web
VITE_API_MODE=http npm run dev -- --host localhost
```

验收脚本会主动拒绝 `127.0.0.1`，也会通过真实注册响应识别误用 mock 模式。不要用 `127.0.0.1` 临时绕过白名单问题。

3. Python Playwright：

```bash
python3 -m pip install playwright --user
python3 -m playwright install chromium
```

## 运行

```bash
python3 e2e/run_e2e.py
```

全部通过输出 `PASS`；任一失败输出 `FAIL` 并以非零码退出。每次运行使用随机用户名，避免账号冲突。

## 说明

- 脚本为 headless Chromium，依赖真实前后端链路（非 mock）。默认前端为 `http://localhost:5173`，后端为 `http://localhost:8080`。
- 综合 v3 验收包含：新注册、开通会员、真实生成、刷新、书架重开、三年三张行动卡、无旧行动列表、430px/360px 无横向溢出、控制台无错误。
- 脚本会先制造一次出生地解析异常，确认报告数不变；随后仍能生成满 3 份会员命书，证明失败没有占用当日次数，并确认第 4 份才触发会员上限。
- 当前综合通俗版继续只供会员内测，暂不进入单份售卖目录。只有跨主题预演、中文人工复核和真实用户反馈都通过后，才讨论单独售卖。
- 曾在开发中借此发现「刷新后 /chart 立即跳回 /input」的 bug（历史兜底异步加载时页面过早导航），修复见 `useBaziWithFallback` 的 loading 状态。

## 发布前完整门槛

```bash
cd apps/server
./mvnw -q test

cd ../web
npm test -- --run
npm run build

cd ../..
ruby -e 'require "yaml"; YAML.safe_load(File.read("contracts/openapi.yaml"), aliases: true)'
git diff --check
```

`APP_OVERALL_CONTENT_VERSION` 是新生成综合命书的单点版本开关，默认值来自 `ReportService.OVERALL_CONTENT_VERSION`。需要回滚时设为 `overall-narrative-v2`；它只改变新生成报告，不迁移或改写已保存快照，`overall-narrative-v3` 始终保留独立读取分支。

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
