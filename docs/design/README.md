# 设计文档索引

未来会话先读本文件，按需加载对应文档，避免整篇读取浪费上下文。

| 文档 | 内容 | 谁该读 |
|---|---|---|
| `frontend-design.md` | 前端技术设计（当前状态/架构/路由/领域逻辑/待办） | 前端开发会话 |
| `backend-design.md` | 后端技术设计（技术栈/模块/API/表结构/一致性策略） | 后端开发会话 |
| `database-design.md` | 数据库设计（表/字段/索引/迁移/约定） | 后端开发会话 |
| `commercial-design.md` | 商业化设计（免费/付费边界/定价/合规文案/落地路径） | 产品/前后端会话 |
| `mingshu-output-current-state-and-pain-points.md` | 命书当前内容引擎、输出流程、覆盖率数据、付费内容痛点与质量边界 | 命书产品/内容引擎会话 |
| `mingshu-backend-algorithm-review-brief.md` | 命书后端算法瓶颈、技术难点、产品约束与外部智能体评审问题清单 | 外部算法评审/内容引擎会话 |
| `report-reader-language-standard.md` | 命书通俗版“日常稳重”语言标准、禁用词、结构与跨主题验收要求 | 命书内容/综合、财富、感情主题开发会话 |
| `dev-checklist.md` | 开发自检清单（防复发：状态/交互/双模式/运行态/视觉/测试纪律） | **所有开发会话（提交前逐条自查）** |
| `handover-summary.md` | 项目交接总结（仓库/完成度/约定/环境坑/待办/下一步） | **新对话恢复上下文第一入口** |
| `../mockups/bazi-app-mockups.md` | 产品图、交互原型、设计系统规范 | 产品/UI 迭代会话 |
| `../mockups/design-philosophy.md` | 视觉哲学「朱墨星图」 | UI 相关会话 |
| `../../contracts/openapi.yaml` | 前后端共享 API 契约（**唯一真源**） | 前后端联调 |
| `../../contracts/fixtures/bazi-cases.json` | 排盘边界用例（一致性回归基准） | 前后端算法联调 |
| `../../apps/server/src/test/resources/report/wealth-v2-golden-cases.json` | 财富 v2 年限、五路径与证据边界黄金案例；人工复核前不锁定偶然精确分数 | 财富算法/契约回归 |

## 使用约定

1. **按需读取**：前端会话只读 `frontend-design.md`，后端会话只读 `backend-design.md`；跨端问题先查 `contracts/`。
2. **用 `rg` 定位**：找关键词（如"真太阳时""神煞"）用 `rg -n 关键词 docs/`，不要整篇通读。
3. **已交付历史看 git log**：设计文档只保留"当前状态 + 决策 + 待办"，不堆叠迭代过程。
4. **变更日志从简**：每个文档只留最近一条版本记录，历史由 git 负责。

## 前后端分离红线

- `apps/web` 与 `apps/server` **互不 import 源码**；共享信息只通过 `contracts/`（openapi + fixtures）。
- 接口字段以 `contracts/openapi.yaml` 为准；各端内部类型自行定义，但字段名与契约一致（camelCase）。
- 排盘正确性以 `contracts/fixtures/bazi-cases.json` 为共同基准，前后端各跑一份一致性测试。
- 财富 v2 改动必须运行 `WealthGoldenCaseTest`；黄金案例锁定结构、路线与已审查证据，不使用整段文案快照代替内容验收。
- 前端第一版保留神煞/合婚/运势规则（`lib/`）；后端第一版不重复实现，只做排盘核心 + 记录存储。
