# 财富主题页面命书 v1 实施计划

**目标：** 在不引入 LLM、不改变命理规则命中的前提下，完成财富主题从三道现实问卷、后端两年判断、保存报告到前端阅读页的最小闭环。

**边界：** 财富问卷只决定生活场景用词、建议顺序和风险提醒，不增加、删除或修改命理证据；本版只开放事业、财富两个页面阅读主题，综合与感情仍保持不可生成。

## 任务 1：财富上下文与两年评估

- 新增 `WealthContext` 与 `WealthContextRequest`，覆盖收入来源、当前目标、近期收支状态。
- 先写无效选项和两年连续结果测试，再实现领域对象。
- 将财富主题的评估期收窄为今年和明年；事业行为保持不变，其余旧 PDF 主题保持三年。
- 运行：`cd apps/server && ./mvnw -Dtest=ThreeYearAssessmentTest,WealthContextTest test`

## 任务 2：财富生活化叙事

- 先新增 `WealthNarrativePlannerTest`，固定检查：每年一个重点、两个理由、两个动作；文本不出现空洞保留话术和生硬行业词；相同命盘在不同问卷下证据键完全一致。
- 新增 `WealthNarrativePlanner`，复用既有页面内容结构，按财富规则键解释收入机会、现金压力、合作分配、合同和合规风险。
- 所有结论使用可能性和条件表达，不承诺收益，不给具体投资品种建议。
- 运行：`cd apps/server && ./mvnw -Dtest=WealthNarrativePlannerTest test`

## 任务 3：保存报告接口

- 扩展 `ReportPreviewRequest` 支持 `wealthContext`。
- 在 `ReportService` 中按主题选择上下文、评估器、叙事器和内容版本；事业保持 `career-narrative-v3`，财富使用 `wealth-narrative-v1`。
- 先扩展 `SavedReportIntegrationTest`，覆盖财富报告创建、两年正文、缺失问卷错误和所有权读取。
- 更新 OpenAPI 契约。
- 运行：`cd apps/server && ./mvnw -Dtest=SavedReportIntegrationTest test`

## 任务 4：前端财富生成与阅读

- 先扩展页面、接口和阅读页测试。
- 新增财富三问：收入来源、当前目标、近期收支；问卷完成前禁止生成。
- 扩展请求类型，提交 `wealthContext`；事业请求保持兼容。
- 阅读页和“我的命书”按主题显示“财”印、财富标题和财富标签，正文结构继续使用已验收的自然中文格式。
- 运行：`cd apps/web && npm test -- --run src/services/reportApi.test.ts src/pages/ReportPage.test.tsx src/pages/ReportReaderPage.test.tsx src/pages/ReportLibraryPage.test.tsx`

## 任务 5：整体校验

- 运行后端完整测试：`cd apps/server && ./mvnw test`
- 运行前端完整测试与构建：`cd apps/web && npm test -- --run && npm run build`
- 使用本地 `localhost` 前后端实际生成一份财富通俗版，检查问卷、跳转、保存、阅读和列表展示。

> 当前工作区包含用户尚未提交的改动，本次不创建隔离提交，只修改财富闭环直接相关文件，并在完成时列出影响范围。
