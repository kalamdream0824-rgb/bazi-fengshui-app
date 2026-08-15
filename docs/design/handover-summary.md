# 项目交接总结（2026-08-15）

> 一句话：八字排盘前后端分离 App；功能演示闭环已完成（8 模块 + 账号/云同步 + 会员模拟支付 + 新人解读版），**未上线、未接真实支付**。

## 0. 新对话恢复上下文（必读顺序）

1. `docs/design/README.md` —— 文档索引（按需读取，避免整篇加载）
2. 本文件 `handover-summary.md`
3. 按任务读：`frontend-design.md` / `backend-design.md` / `database-design.md` / `../mockups/bazi-app-mockups.md` / `../../contracts/openapi.yaml`
4. 提交前过一遍 `dev-checklist.md`（防复发自检清单）

## 1. 仓库与运行

| 项 | 内容 |
|---|---|
| 本地路径 | `/Users/lijialin/Documents/Codex/bazi-fengshui-app` |
| GitHub | `kalamdream0824-rgb/bazi-fengshui-app`（公开，main 分支） |
| 前端 | `apps/web`：Vite + React 19 + TS strict + zustand + TanStack Query + Vitest + lunar-javascript |
| 后端 | `apps/server`：Spring Boot 3.5.16 + Java 17 + MyBatis-Plus + lunar-java + H2（开发）/ MySQL（Docker 已配） |
| 契约 | `contracts/openapi.yaml` + `contracts/fixtures/bazi-cases.json`（前后端排盘一致性基准） |
| 质量 | 前端 95/95、后端 21/21、lint 0、构建绿、CI 双端全绿 |

本机验证：`cd apps/server && mvn spring-boot:run`（8080）+ `cd apps/web && VITE_API_MODE=http npm run dev`（5173，`/api` 已代理）。

## 2. 完成度

- **前端功能（约 85%）**：8 模块全走通（首页/排盘输入/基本排盘/专业细盘/合婚/每日运势/命书 PDF/我的）+ 择日 + 分享 + 本地历史与云同步 + 账号登录 + 会员中心（套餐/模拟支付/兑换码/续费）+ **新人解读版 v0.20**（`lib/explainer` 人话总览 + 六块解读 + 四柱表词库 15+ 条）。
- **后端（约 80%）**：排盘 API + fixtures 一致性、账号 JWT/BCrypt/登录限流、记录 CRUD/隔离/去重、会员权益/兑换码/订单/模拟支付（`/orders`、`/pay/mock-success/{orderId}`、`/pay/callback` 占位）、P0 安全加固、P1 MySQL + 请求日志；神煞/真太阳时/合婚/运势由前端实现（后端不重复）。
- **上线就绪（约 30-40%）**：未部署、无域名/备案/HTTPS、数据库仍是 H2 内存、真实支付待资质。

## 3. 关键决策与约定（必须遵守）

- **老流程**：文档标记进行中 → 实现 → 测试/构建 → 文档改已交付 → outputs 镜像同步 → git commit + push。
- **文档先行**：写代码前先更新设计文档；变更日志只留最近一条，历史靠 git。
- **前后端分离红线**：`apps/web` 与 `apps/server` 互不 import 源码；接口字段以 `contracts/openapi.yaml` 为唯一真源（camelCase）。
- **合规红线**：文案"温和参考"、不绝对预测、保留免责声明；解读/运势测试断言不含断言词（一定/必定/发财/离婚等）；不做医疗/投资/婚姻确定性建议。
- **不迎合用户**：用户明确要求不对的要反驳，不默认用户想法正确。
- **测试纪律**：每次改动有测试；修 bug 先补回归；重构/优化跑全量，不得引入次要缺陷。
- **防复发**：`dev-checklist.md` 收录历史 bug 教训（刷新丢数据/分享复制文字/页签不联动/双模式组合/后端不重启/伪需求），提交前逐条自查。
- **技术选型**：Java + MyBatis-Plus（用户确认）；解读/规则逻辑放前端 `lib/`，未来换后端走"数据源接口 + 契约结构"（BaziApi 模式）。

## 4. 环境与运行注意（踩过的坑）

- **H2 内存库重启即清空**：后端进程重启 → 用户/排盘/会员数据全丢。验证期间不要随意重启；持久化需切 MySQL（`docker compose up -d mysql` + `SPRING_PROFILES_ACTIVE=mysql`）。
- **改后端代码必须重启进程再验证**：JVM 运行中按需加载新类与容器旧 Bean 错位 → 接口 500（真实发生过）。
- **沙箱限制**：`mvn test`（Mockito 需 JVM attach）、`npm test/lint`（需写 node_modules/.vite-temp）、localhost curl 均需沙箱外（`require_escalated`）运行。
- **git push**：用 `git -c http.version=HTTP/1.1 -c http.proxy=http://127.0.0.1:7890 push origin main`（沙箱外）。
- **后端进程可能由 Codex 会话托管**：重启/查日志前先确认进程归属（`lsof -iTCP:8080`）。

## 5. 待办（按优先级）

1. **部署上线**：前端静态托管 + 后端上云 + 域名/备案/HTTPS（国内用户需备案）。
2. ~~**MySQL 切换并验证持久化**~~ **已实测完成（2026-08-16）**——docker compose mysql:8.4 + mysql profile，重启后数据保留；开发联调默认切 MySQL。
3. ~~**E2E 冒烟测试**~~ **已交付（2026-08-16）**——`e2e/run_e2e.py`（Playwright：登录→排盘→会员购买→分享→历史→刷新，6/6 通过）；顺带修复刷新兜底 bug（v0.31）。
4. **真实支付**：需企业资质 + 商户号 + 备案域名，`/pay/callback` 签名校验待实现。
5. **兑换码生成/管理接口**（P2，当前 SQL 预置）。
6. **合婚邀请分享**（P2，需后端配对机制）。
7. **定价与会员权益定义**——方向已定（盘面数据免费、内容类付费，见 `commercial-design.md`）；**会员权益（免费解读条数/云同步额度/会员卖点）用户再考虑中，暂缓实现（2026-08-16）**；未确认前不实现权益闸门。
8. 小项：关于与合规声明页、设置-流派、产品图"时辰边界提示"待办口径清理。
9. **新人解读内容深度扩展**（P0-P2）：解读从词典式升级为组合型（词库+规则两层，避免组合爆炸）；P0 旺衰粗判/副星自坐进解读/大运十神；P1 十神组合/神煞落宫；P2 胎元命宫身宫/流年冲合。维持前端 `lib/explainer`，详见 frontend-design.md 待办。
10. **忘记密码/重置密码**（上线前）：BCrypt 不可逆，只能重置不能找回；前置需 `bazi_user` 增加邮箱（或手机号）字段 + 注册/个人中心支持绑定，再接入邮件验证码发重置链接。当前临时方案：管理员数据库直改密码哈希。

## 6. 最近提交（新 → 旧）

| commit | 内容 |
|---|---|
| 02f908e | 四柱表新增自坐行（本柱天干太极点十二长生，契约/fixtures 同步） |
| 515c149 | 四柱表补齐问真口径：副星/空亡独立行 + 胎元命宫身宫 |
| c6eb254 | 项目交接总结（handover-summary） |
| a0b698a | 新人解读版（方案 B）：lib/explainer + 两页接入 + 词库扩展 |
| abc6705 | 解读版设计评审文档 |
| 1a49bdf | 底部导航在"我的"二级页保留 |
| 5cd1948 | 会员购买 401 统一处理（authFetch + 后端用户校验） |
| 040e1d9 | 开发自检清单（防复发） |
| 6420d38 | 会员中心独立页 /membership |
| 76257c6 | 会员状态卡登录状态同步修复 |
| f2b10a1 | 模拟支付打通全流程 |

## 7. 下一步结论（上次讨论）

优先做「部署 + MySQL 持久化」，把项目从开发态推向可用态；真实支付等资质就绪。用户最新方向是"让新人看得懂"（解读版已交付第一版），文案详略可按反馈继续迭代。
