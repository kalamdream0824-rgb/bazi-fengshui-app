# 单身感情命书今年重点版实施计划

> 使用 Executing Plans 与 Test-Driven Development 技能按以下三项任务执行。

**Goal:** 将已审核样稿接入单身报告，形成今年详细、明年简短的生成与保存阅读闭环。

**Architecture:** 新建 `relationship-single-v1` 内容快照，单身仅取两年计算；年度算法、权重和技术五年上限不变。新文案从当前年五维结果选句，不使用跨年总论决定今年判断；旧 `relationship-narrative-v1` 保持原有读取与三年页面。

**Tech Stack:** Java 17 / Spring Boot、React / TypeScript、JUnit / Vitest / Playwright。

## 任务 1：内容范围和保存边界

- 修改 `apps/server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java`，先验证单身新版本、两年计算、今年正文与明年摘要、保存回读一致；交往中与已婚保持三年。
- 新增 `apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipSingleNarrativePlannerTest.java`，覆盖真实样本、当前与跨年主次不同、无风险不编风险、不同倾向、年度差异和旧快照。
- 运行 `cd apps/server && ./mvnw -Dtest=SavedReportIntegrationTest test`，确认版本断言先失败；新增领域测试按红绿循环执行。
- 新增 `RelationshipSingleNarrativePlan` 与 `RelationshipSingleNarrativePlanner`，在 `ReportService` 根据状态选择产品范围与内容版本；新文案维护在既有文案资源的新命名空间，不改旧文案。
- 验证单身、交往中和已婚共同年份的原始计算不因状态变化。

## 任务 2：生成入口与正文

- 在 `apps/web/src/pages/ReportPage.test.tsx` 先增加范围切换测试；在阅读页增加新版本分流与旧单身三年快照测试，确认未实现时失败。
- `apps/web/src/services/reportApi.ts` 增加独立内容类型和类型判断。
- `ReportPage.tsx` 随状态显示“今年重点＋明年参考”，未选状态不预先承诺三年。
- 新增 `RelationshipSingleReportReader.tsx` 与局部样式，复用纸张、书头、结论和页脚。使用一条阅读主线：结论、今年问题、明年摘要；不重复三年总论、五维卡片、逐年全章。
- `ReportReaderPage.tsx` 按版本分流，列表继续显示保存时的标题。
- 验证年份使用快照年份而非浏览时的系统年份，无额外问卷。

## 任务 3：回归与验收

- 运行后端完整测试、前端完整测试和构建；核对合成反例、男女真实样本及文案无未替换变量。
- 更新 `e2e/run_e2e.py` 兼容单身新结构，保留交往中、已婚的三年及年度差异检查。
- 用隔离本地服务验收生成、阅读、保存回看和手机端对齐，不重启原内存数据库，不覆盖旧报告。
- 更新设计文档和实际输出样本，记录检查结果。不自动提交或推送，保留既有未提交改动与 `work/`。

## 内容准则

- 五维仍完整计算，正文按问题合并表达，不机械凑足五张卡。
- 单身包含“尚无接触对象”和“正在了解某个人”，用条件句兼顾，不假设已经有对象。
- 跨年比较分别保留支持与限制变化；同权重换依据不直接推断变好；不人为安排认识、确定关系的年份。
- 相处例子是一般性建议，不是算出的特定人物行为；不承诺月份、人物、脱单概率。
- 没有负面依据时不额外生成风险段，不能将明年的限制倒灌到今年。

## 页面样式约束

沿用当前命书的墨色 `#142129`、纸色 `#fffaf0`、朱色 `#a63c32`、金色 `#b8904a` 与正文 `#514536`；沿用 `--font-display` 与正文继承字体。只调整信息层级，维持现有宽度与纸张边界。明年摘要以紧凑书页旁注呈现，不再添加第二条完整年度时间线。

## 执行结果

三项任务已完成。新版本、生成页周期和新阅读页先验证旧行为失败，再实现；领域测试先验证空实现失败，随后补齐分支。后端 258 项、前端 179 项、浏览器 35 项通过，生产构建、局部 ESLint、手机对齐检查通过。

为保留已有内存数据，本轮新增 5175 / 8082 隔离验收服务，不重启旧后端。不自动提交或推送。实际样本见 `docs/samples/relationship-single-current-year-implemented.md`。
