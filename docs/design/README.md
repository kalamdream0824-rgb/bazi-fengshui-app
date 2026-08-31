# 设计文档索引

未来会话先读本文件，按需加载对应文档，避免整篇读取浪费上下文。

| 文档 | 内容 | 谁该读 |
|---|---|---|
| `frontend-design.md` | 前端技术设计（当前状态/架构/路由/领域逻辑/待办） | 前端开发会话 |
| `backend-design.md` | 后端技术设计（技术栈/模块/API/表结构/一致性策略） | 后端开发会话 |
| `database-design.md` | 数据库设计（表/字段/索引/迁移/约定） | 后端开发会话 |
| `commercial-design.md` | 商业化设计（免费/付费边界/定价/合规文案/落地路径） | 产品/前后端会话 |
| `mingshu-backend-content-generation-current.md` | 当前页面命书生成逻辑（2026-08-29）：公共计算、三主题差异、文案组织、快照保存及已知局限 | 产品/后端/外部智能体评审 |
| `mingshu-content-review-remediation-proposal.md` | 外部评审复核与整改方案（2026-08-29）：采纳/修正意见、财富优先路线、影响范围与验收标准；执行进度另见任务清单 | 产品/内容引擎整改评审 |
| `wealth-v3-expression-contract.md` | 财富 v3 字段与表达规则；任务 7–9 已接生成、快照、页面并完成真实 MySQL 三年样本系统验证，`TEXT` 容量风险仍保留 | 财富整改前后端/内容审校 |
| `wealth-v3-copy-license.md` | 财富任务 6–7：正文模板许可、负向事项词汇、生成前校验与历史快照读取边界 | 财富文案/生成保存开发 |
| `../samples/wealth-v2-remediation-baseline-2026-08-29.md` | 财富整改任务 1：23 组旧版计算和正文基线、来源区分、验收矩阵与覆盖边界 | 财富整改前后对照/验收 |
| `../samples/wealth-remediation-task3-red-report-2026-08-29.md` | 财富整改任务 3 历史结果：28 条独立验收规范，22 条复现缺陷、6 条已满足；输入边界及修复对应关系 | 财富整改开发/验收 |
| `../samples/wealth-remediation-task4-assessment-report-2026-08-29.md` | 财富整改任务 4 历史结果：独立判断及来源映射、72 项定向测试通过、当时剩 15 条年度/正文失败 | 财富整改开发/验收 |
| `../samples/wealth-remediation-task5-comparison-report-2026-08-29.md` | 财富整改任务 5 历史结果：真实依据年度比较、103 项定向测试通过；当时剩 12 条正文失败 | 财富整改开发/验收 |
| `../samples/wealth-remediation-task6-narrative-report-2026-08-29.md` | 财富整改任务 6：正文/许可检查及实际生成样本，159 项定向测试通过，独立规范 28/28；待用户审稿 | 财富整改开发/内容验收 |
| `../samples/wealth-remediation-task7-persistence-report-2026-08-29.md` | 财富整改任务 7：后端 v3 正式生成、失败不保存、v1/v2/v3 快照读取与 H2 验证边界 | 财富整改后端/接口验收 |
| `../samples/wealth-remediation-task8-frontend-report-2026-08-29.md` | 财富整改任务 8：前端 v3 独立阅读、版本分流、列表兼容与视觉适配；系统级验证仍待任务 9 | 财富整改前端/接口验收 |
| `../samples/wealth-remediation-task9-system-validation-2026-08-29.md` | 财富整改任务 9：全量回归、真实 MySQL、浏览器主链路、响应式与账号隔离；含 `TEXT` 容量风险 | 财富整改系统验收 |
| `../samples/wealth-v3-task6-birth-samples-2026-08-29.md` | 三份完整出生链路生成原文（R01/R05/R09），没有逐篇人工改写，页面未切换 | 用户正文审核 |
| `../samples/wealth-v3-task6-boundary-samples-2026-08-29.md` | 五份合成边界原文：弱支持、并列、无风险、延续、受限，不代表真实用户经历 | 内容边界审核 |
| `../plans/2026-08-29-wealth-content-remediation-checklist.md` | 财富第一批整改任务 1–10、范围与审核点 | 财富整改执行会话 |
| `../plans/2026-08-30-single-report-checkout.md` | 单份命书购买闭环：服务端定价、锁定报告、订单关联、幂等解锁、会员与非会员分流 | 产品/前后端/支付联调 |
| `mingshu-output-current-state-and-pain-points.md` | 命书当前内容引擎、输出流程、覆盖率数据、付费内容痛点与质量边界 | 命书产品/内容引擎会话 |
| `mingshu-backend-algorithm-review-brief.md` | 命书后端算法瓶颈、技术难点、产品约束与外部智能体评审问题清单 | 外部算法评审/内容引擎会话 |
| `report-reader-language-standard.md` | 命书通俗版“日常稳重”语言标准、禁用词、结构与跨主题验收要求 | 命书内容/综合、财富、感情主题开发会话 |
| `relationship-report-v1.md` | 感情通俗版的证据规则、状态边界、接口错误码与验收清单 | 感情命书算法/前后端会话 |
| `dev-checklist.md` | 开发自检清单（防复发：状态/交互/双模式/运行态/视觉/测试纪律） | **所有开发会话（提交前逐条自查）** |
| `handover-summary.md` | 项目交接总结（仓库/完成度/约定/环境坑/待办/下一步） | **新对话恢复上下文第一入口** |
| `../mockups/bazi-app-mockups.md` | 产品图、交互原型、设计系统规范 | 产品/UI 迭代会话 |
| `../mockups/design-philosophy.md` | 视觉哲学「朱墨星图」 | UI 相关会话 |
| `../../contracts/openapi.yaml` | 前后端共享 API 契约（**唯一真源**） | 前后端联调 |
| `../../contracts/fixtures/bazi-cases.json` | 排盘边界用例（一致性回归基准） | 前后端算法联调 |
| `../../apps/server/src/test/resources/report/wealth-v2-golden-cases.json` | 财富 v2 年限、五路径与证据边界黄金案例；人工复核前不锁定偶然精确分数 | 财富算法/契约回归 |
| `../../apps/server/src/test/resources/report-golden/relationship-v1-cases.json` | 感情 v1 男女命盘、三种状态、并列与无风险边界黄金案例 | 感情算法/契约回归 |

## 使用约定

1. **按需读取**：前端会话只读 `frontend-design.md`，后端会话只读 `backend-design.md`；跨端问题先查 `contracts/`。
2. **用 `rg` 定位**：找关键词（如"真太阳时""神煞"）用 `rg -n 关键词 docs/`，不要整篇通读。
3. **已交付历史看 git log**：设计文档只保留"当前状态 + 决策 + 待办"，不堆叠迭代过程。
4. **变更日志从简**：每个文档只留最近一条版本记录，历史由 git 负责。

## 前后端分离红线

- `apps/web` 与 `apps/server` **互不 import 源码**；共享信息只通过 `contracts/`（openapi + fixtures）。
- 接口字段以 `contracts/openapi.yaml` 为准；各端内部类型自行定义，但字段名与契约一致（camelCase）。
- 单份命书价格必须由服务端商品目录决定，前端不得提交金额；当前模拟支付属于联调工具、非真实扣款，不得作为正式支付能力对外描述。
- 排盘正确性以 `contracts/fixtures/bazi-cases.json` 为共同基准，前后端各跑一份一致性测试。
- 财富 v2 改动必须运行 `WealthGoldenCaseTest`；黄金案例锁定结构、路线与已审查证据，不使用整段文案快照代替内容验收。
- 财富整改另有 `WealthV2BaselineTest` 记录旧行为，仅作历史对照和兼容检查；不把旧缺陷作为 v3 正确答案。任务 7–8 已将 `contracts/drafts/wealth-v3.schema.json` 接入正式 `ReportInfo.content` 联合类型和前端独立阅读分支。
- 财富任务 3 新增的 `WealthRemediationSpec` 已在任务 9 纳入 Surefire 默认发现，28/28 随普通后端回归运行；不能删掉 `*Spec` 入口或只用旧测试判断整改通过。
- 财富任务 7 的正式生成使用 `WealthV3Analyzer.analyze`、`WealthNarrativeWriter.plan` 和 `validateDraft`；局部入口的待核实来源会被拒绝。校验仅用于新生成正文，不得用于重写历史快照。任务 8 已按 v1/v2/v3 分流前端读取，未知版本明确提示不支持。
- 感情 v1 改动必须运行 `RelationshipGoldenCaseTest`；关系状态不得进入事实提取、评分与裁决层。
- 前端第一版保留神煞/合婚/运势规则（`lib/`）；后端第一版不重复实现，只做排盘核心 + 记录存储。
