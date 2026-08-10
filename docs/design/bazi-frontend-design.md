# 八字排盘 App · 前端技术设计（v0.13 · 精简版）

| 项目 | 内容 |
|---|---|
| 版本 | v0.14（账号/云同步接入） |
| 日期 | 2026-08-09 |
| 索引 | 见 `docs/design/README.md`；后端见 `backend-design.md` |

## 1. 当前状态

- 8 模块全可走通；测试 60/60、lint 0 问题、构建绿、CI 已配置、已上 GitHub。
- 已完成里程碑：M0.5/M0.8（全模块）、表单组件（时间滚轮/省市下拉）、真太阳时、A2 神煞、A1 命书 PDF、A3 合婚规则、A4a 本地历史+备份、B0（共享组件/懒加载/CI）、C1（大运流年神煞/时辰边界提示）、C2（真实黄历/运势文案）、ESLint+Prettier。
- 详细历史见 git log，不在本文档堆叠。

## 2. 架构

- `apps/web` 独立可跑；排盘数据层 `BaziApi` 接口（Mock 本地真算 / Http 后端），切换仅改 `VITE_API_MODE`。
- 与后端唯一共享层是 `contracts/`（openapi + fixtures）；互不 import 源码。

## 3. 技术栈（实际）

Vite 8 + React 19 + TS 6.0.3（strict）+ react-router 7 + TanStack Query + zustand + Vitest + ESLint/Prettier + lunar-javascript。

## 4. 路由

`/` 首页 · `/input` 排盘输入 · `/history` 历史 · `/chart` 基本排盘 · `/chart/pro` 专业细盘 · `/comp` 合婚 · `/daily` 每日运势 · `/report` 命书 · `/profile` 我的；底部导航仅 首页/排盘/我的。

## 5. 领域逻辑（lib/，均有测试）

| 模块 | 职责 |
|---|---|
| `baziMapper` | lunar → `PaipanResult`（含真太阳时/大运/流年神煞） |
| `compRules` | 合婚参考规则（生肖六合/纳音/夫妻宫/年柱/缺补） |
| `shenSha` | 四柱 + 大运流年神煞（口径见文档历史，来源公示） |
| `trueSolarTime` | 真太阳时（356 城经度 + NOAA 均时差） |
| `almanac` / `dailyFortune` | 真实黄历 + 温和参考型运势文案 |
| `datePicker` / `wuxing` / `reportPdf` | 工具与命书 PDF |

## 6. 设计系统

令牌 `styles/tokens.css` + 全局 `app.css` + 共享组件（Button/Card/SegControl/Switch/TabBar/TopBar/BaziTable/WuxingBar/DaYunList/Sheet/Toast/FooterNote/EmptyState/RecordRow/RegionSelect/DateTimePicker）。偏差记录：样式为全局文件（CSS Modules 暂缓，需浏览器验证环境）。

## 7. 待办

- 占位功能：会员中心（**会员/支付待办：需企业资质，后续再做**）、关于与合规声明、设置-流派。
- **合婚邀请分享（P2，需后端配对机制）**：生成邀请卡片/链接，对方填入生辰后自动配对出合盘——这是命理 App 的真实社交分享场景。
- 优化项：CSS 作用域化、UI 测试补强、覆盖率统计。

## 8. 与后端边界

- `HttpBaziApi` 已占位，后端上线后切 `VITE_API_MODE=http`，页面零改动。
- 神煞/合婚/运势规则**前端保留**，后端第一版不重复实现；一致性靠 fixtures 回归。
- 账号：http 模式走 `/api/v1/auth`（登录/注册），登录后本地历史自动同步云端；Mock 模式模拟登录。

## 9. 合规红线

- 文案"温和参考"、不绝对预测、保留免责声明；无命盘时不生成个性化运势。

## 10. 变更日志

- v0.14（2026-08-09）：接入账号与云同步（登录/注册页、我的页登录态、历史云端读写），退出登录完成。
- v0.15（2026-08-10）：择日模块交付——事项匹配规则引擎（`lib/dayPicker`，真实黄历宜忌/值星/冲煞），页面 `/day-picker`（70/70 测试）。
- v0.16（2026-08-10）：分享（方案 A）交付——1080 分享卡片图（`lib/share`，html2canvas）+ Web Share API + 复制兜底，接入排盘结果页与每日运势页（72/72 测试）；链接分享页归后端 P2。
- v0.16.1（2026-08-10）：分享改为**图片预览面板**（ShareSheet）——点击分享先展示生成的卡片图，支持 保存图片 / 复制文案 / 系统分享图片文件，避免"只复制到文字"的困惑（73/73 测试）。
- v0.16.2（2026-08-10）：分享场景复盘——**移除每日运势分享**（伪需求）；排盘分享重新定位为"**保存命盘 / 求解读**"；合婚邀请分享列入 P2 待办。
- v0.16.3（2026-08-10）：修复"我的-我的命盘"刷新后无数据——改为"当前结果 → 最新历史记录"兜底（75/75 测试）。
- v0.13（2026-08-09）：精简整理（当前状态/决策/待办），历史归档至 git log。
