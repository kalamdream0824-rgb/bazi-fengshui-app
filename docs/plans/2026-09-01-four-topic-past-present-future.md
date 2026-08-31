# Four-Topic Past–Present–Future Narrative Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** 为事业、财富、感情和综合命书增加统一的“去年回看—当下判断—未来行动”时间线，同时保持现有产品预测年限、付费规则和历史命书读取能力不变。

**Architecture:** 后端新增一个四主题共用的 `NarrativeTimeline` 输出契约，并让 `AnnualContextFactory` 能单独计算当前年份的上一年。过去年度只用于“核对式回看”，当前和未来仍使用各主题现有评估器；五条实际生成链路（事业、财富、感情交往/已婚、感情单身、综合）分别把自己的证据映射到统一时间线。前端只实现一套时间线组件，各阅读器负责传入对应版本内容。

**Tech Stack:** Java 17、Spring Boot 3、Jackson、JUnit 5、React 19、TypeScript、Vitest、Testing Library、Vite。

---

## 产品与内容边界

- 生成日处于 2026 年时，过去回看固定计算 2025 年；不回看 2024 年。
- 过去内容必须使用“如果你去年曾经……”“回看去年是否……”等核对式表达，不得写成已经证实的个人经历。
- 过去回看不计入对外销售的预测年限，也不额外消耗会员次数。
- 事业仍为当年加下一年；财富、综合、交往中和已婚感情仍为当年起三年；单身感情仍为当年重点加下一年参考。
- 时间线只负责快速说明变化过程；现有逐年正文继续承担详细判断，禁止逐字重复。
- 不新增问卷。事业继续使用现有事业状态；感情继续使用关系状态；财富与综合使用系统计算。
- 不宣称过去回看“命中”或证明预测准确，只把它作为用户核对和后续反馈入口。

## 统一输出契约

新增 `NarrativeTimeline`：

```java
public record NarrativeTimeline(
    PastReview past,
    PresentReading present,
    List<FutureStep> future) {

  public record PastReview(
      int year,
      String headline,
      List<String> checkpoints,
      String bridge,
      List<String> evidenceKeys) {}

  public record PresentReading(
      int year,
      String headline,
      String judgment,
      String priority,
      List<String> evidenceKeys) {}

  public record FutureStep(
      int year,
      String headline,
      String action,
      List<String> evidenceKeys) {}
}
```

约束：

- `past.year == present.year - 1`。
- `past.checkpoints` 固定两条，内容不重复且必须为核对式表达。
- `future` 只包含当前年之后的产品年份，年份连续且每年一条行动。
- 所有判断都必须有 `evidenceKeys`；前端通俗版不展示键，但用于审计与测试。
- 事业和单身感情有 1 条未来行动；财富、综合、交往中和已婚感情有 2 条。

---

### Task 1: 固定统一时间线契约与语言边界

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/NarrativeTimeline.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/RetrospectiveLanguagePolicy.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/NarrativeTimelineTest.java`
- Modify: `docs/design/report-reader-language-standard.md`

**Steps:**

1. 在 `NarrativeTimelineTest` 写失败测试，覆盖上一年关系、两条核对点、未来年份连续、空证据拒绝和重复文案拒绝。
2. 增加过去文案否决词测试：`你去年已经`、`你去年一定`、`去年必然`、`事实证明`、`准确命中`。
3. 运行 `cd apps/server && ./mvnw -Dtest=NarrativeTimelineTest test`，确认测试因类型不存在而失败。
4. 实现最小不可变契约和 `RetrospectiveLanguagePolicy.validate(...)`，过去核对点必须包含 `如果`、`是否`、`可能` 或 `回看` 之一。
5. 再次运行测试，预期全部通过。
6. 在语言标准中写明：过去是核对，不是事实断言；当前是判断；未来是行动。
7. 提交：`git commit -m "feat(report): define past present future timeline contract"`。

### Task 2: 支持单独计算上一完整年度

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/AnnualContextFactory.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/AnnualContextFactoryTest.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportAnalysisWindow.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/ReportAnalysisWindowTest.java`

**Steps:**

1. 写失败测试：固定时钟为 2026-09-01 时，`createYear(..., 2025)` 返回 2025 年干支、对应大运和年度证据。
2. 写窗口测试：`previous.year == productYears[0].year - 1`，但 `productYears` 数量仍保持原产品年限。
3. 运行 `./mvnw -Dtest=AnnualContextFactoryTest,ReportAnalysisWindowTest test`，确认失败原因是缺少上一年 API。
4. 从现有循环抽取单年创建逻辑，新增 `AnnualContextFactory.createYear(request, chart, year)`；不要放宽 `ReportHorizon` 的 2–5 年产品限制。
5. 新增 `ReportAnalysisWindow(previous, productYears)`，负责年份关系校验，不承担主题判断。
6. 跑定向测试与 `AnnualPeriodAssessorTest`，预期全部通过。
7. 提交：`git commit -m "feat(report): calculate previous-year review context"`。

### Task 3: 建立通用时间线编排接口与重复检查

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/NarrativeTimelinePlanner.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/NarrativeTimelineValidator.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/NarrativeTimelineValidatorTest.java`

**Steps:**

1. 写失败测试，要求过去、当前和未来三个区段不能出现相同完整句，也不能共享四字以上的主题核心短语。
2. 定义编排接口输入：主题码、上一年已评估结果、当前及未来结果、可选用户现状；输出统一 `NarrativeTimeline`。
3. 保持具体主题文案在主题 planner 中，通用层只负责年份、证据和重复规则，不做“万能模板”。
4. 实现确定性校验，所有集合保持固定顺序；同输入必须生成相同 JSON。
5. 运行 `./mvnw -Dtest=NarrativeTimelineValidatorTest test`。
6. 提交：`git commit -m "feat(report): add timeline narrative validation"`。

### Task 4: 接入事业主题

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/AnnualPeriodAssessor.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/CareerTimelinePlanner.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/CareerNarrativePlan.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/CareerNarrativePlanner.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/CareerNarrativePlannerTest.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/CareerTimelinePlannerTest.java`

**Steps:**

1. 写失败测试，分别覆盖在职、自营、求职、学习四种状态；过去核对必须落到职责/成果、客户/回款、投递/面试、学习/作品，而不是统一写“工作变化”。
2. 为 `AnnualPeriodAssessor` 提供复用现有规则的单年评估入口，禁止另写一套过去评分规则。
3. 实现 `CareerTimelinePlanner`：上一年给两条核对点；当前结合三项事业问卷给一个优先问题；下一年给一条行动。
4. 给 `CareerNarrativePlan` 增加可空 `timeline` 字段和兼容旧 JSON 的构造方式。
5. 当前年度正文保留，但时间线文案不得复制 `verdict`、`obstacle` 或 `actions` 原句。
6. 跑 `./mvnw -Dtest=CareerTimelinePlannerTest,CareerNarrativePlannerTest test`。
7. 提交：`git commit -m "feat(report): add career retrospective timeline"`。

### Task 5: 接入财富主题

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthReportGenerator.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthNarrativeV3.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthTimelinePlanner.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthTimelinePlannerTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthNarrativeV3ContractTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthNarrativeSampleTest.java`

**Steps:**

1. 写失败测试，覆盖稳定收入、能力收入、项目收入、合作收入、结余五条路径。
2. 明确过去核对只描述收入来源变化、回款、支出压力和是否留下钱，不写具体金额、不把事业职责换词后复用。
3. 让 `WealthReportGenerator` 额外评估上一年 context，但仍把当前起三年交给既有 v3 writer。
4. 实现 `WealthTimelinePlanner`，从上一年 decision、当前 focus/retention、后两年 actions 中选取时间线内容。
5. 给财富内容增加 `timeline`，旧 `wealth-narrative-v3` 反序列化时允许为空。
6. 将既有 12 个财富固定样本加入时间线重复、禁语和证据归属检查。
7. 跑财富 v3 全套定向测试。
8. 提交：`git commit -m "feat(report): add wealth retrospective timeline"`。

### Task 6: 接入感情主题（交往中与已婚）

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipTimelinePlanner.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipNarrativePlan.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipNarrativePlanner.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipTimelinePlannerTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipNarrativePlannerTest.java`

**Steps:**

1. 写失败测试，分别覆盖交往中和已婚；过去核对点必须根据状态区分约会响应/关系确认与共同生活/责任分配。
2. 过去只描述互动、回应、日常配合、边界和稳定性，不直接断言分手、结婚、背叛或第三者。
3. 当前判断复用关系五维评估，选择一个主要维度和一个现实优先问题。
4. 未来两年各输出一条可执行行动，并保证两年对象和动词不重复。
5. 给 `RelationshipNarrativePlan` 增加兼容型 `timeline` 字段。
6. 跑关系 planner、golden case 和年度比较测试。
7. 提交：`git commit -m "feat(report): add relationship retrospective timeline"`。

### Task 7: 接入单身感情分支

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipSingleNarrativePlan.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipSingleNarrativePlanner.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipSingleNarrativePlannerTest.java`

**Steps:**

1. 写失败测试，确保单身分支也有上一年回看、当前判断和下一年行动，不能被“感情主题已完成”误漏。
2. 过去核对聚焦认识机会、回应持续性和个人边界；不得假定用户去年有具体对象。
3. 当前段继续以今年为详细重点，未来段只保留下一年一条行动，不擅自扩成三年。
4. 给 `RelationshipSingleNarrativePlan` 增加兼容型 `timeline` 字段，并保持 `evaluations` 审计信息完整。
5. 跑单身 planner 和内容兼容测试。
6. 提交：`git commit -m "feat(report): add single relationship retrospective timeline"`。

### Task 8: 接入综合主题

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallTimelinePlanner.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/overall/OverallNarrativePlan.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/overall/OverallNarrativePlanner.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/OverallTimelinePlannerTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePlannerTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePreflightTest.java`

**Steps:**

1. 写失败测试：过去回看必须明确一个主要方面和一个联动方面；当前必须说明先处理什么；未来两年必须给出不同重点。
2. 实现上一年度四维评估，复用 `OverallPeriodArbitrator` 的排序规则，不单独发明“过去权重”。
3. 过去核对用“事业—钱财—关系—生活节奏”的真实对象表达，禁止“整体情况”“综合层面”等空泛词。
4. 给 `OverallNarrativePlan` 增加兼容型 `timeline` 字段。
5. 在 R01–R12 预演中增加过去/当前/未来重复率、空内容和无证据统计，目标全部为 0。
6. 跑综合主题定向测试和预演。
7. 提交：`git commit -m "feat(report): add overall retrospective timeline"`。

### Task 9: 升级内容版本并保持历史命书可读

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/service/ReportService.java`
- Modify: `apps/server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/ReportContentCompatibilityTest.java`
- Modify: `contracts/openapi.yaml`

**New versions:**

- `career-narrative-v4`
- `wealth-narrative-v4`
- `relationship-narrative-v2`
- `relationship-single-v2`
- `overall-narrative-v2`

**Steps:**

1. 先写创建新版本和读取所有旧版本的集成失败测试。
2. 在 `ReportService` 一次计算 `ReportAnalysisWindow`，把 previous 和 product years 分发给对应主题链路。
3. 更新新生成报告的版本常量；保留 v1/v1.1/v3 等历史反序列化分支。
4. 断言旧报告 `timeline == null` 时仍返回 200，前端不得显示空时间线。
5. 断言生成失败不扣会员次数，上一年计算不得绕过既有 entitlement 事务边界。
6. 更新 OpenAPI 的版本枚举和 `NarrativeTimeline` schema。
7. 跑 `./mvnw -Dtest=SavedReportIntegrationTest,ReportContentCompatibilityTest test`。
8. 提交：`git commit -m "feat(report): version four-topic timeline content"`。

### Task 10: 增加前端统一时间线组件

**Files:**
- Modify: `apps/web/src/services/reportApi.ts`
- Create: `apps/web/src/components/report/NarrativeTimeline.tsx`
- Create: `apps/web/src/components/report/NarrativeTimeline.test.tsx`
- Modify: `apps/web/src/styles/app.css`

**Steps:**

1. 写失败测试，要求按“去年回看、当下判断、未来行动”顺序渲染，并显示具体年份。
2. 添加 TypeScript 类型，与 OpenAPI 字段一一对应；旧版本的 `timeline` 必须是可选字段。
3. 实现一个共享组件，过去区使用“回看核对”视觉语义，不能使用“已发生”标签；当前区突出优先问题；未来区按年份列行动。
4. 移动端保持单列，桌面端最多三列；不新增横向滚动，不使用过长卡片嵌套。
5. 没有 timeline 时组件返回 `null`，保证历史命书页面不变。
6. 跑 `npm test -- --run src/components/report/NarrativeTimeline.test.tsx`。
7. 提交：`git commit -m "feat(web): add shared report timeline"`。

### Task 11: 接入五个前端阅读器与版本守卫

**Files:**
- Modify: `apps/web/src/components/report/CareerReportReader.tsx`
- Modify: `apps/web/src/components/report/WealthV3ReportReader.tsx`
- Modify: `apps/web/src/components/report/RelationshipReportReader.tsx`
- Modify: `apps/web/src/components/report/RelationshipSingleReportReader.tsx`
- Modify: `apps/web/src/components/report/OverallReportReader.tsx`
- Modify: `apps/web/src/services/reportApi.ts`
- Modify: `apps/web/src/pages/ReportReaderPage.tsx`
- Modify: `apps/web/src/pages/ReportReaderPage.test.tsx`

**Steps:**

1. 写五个新版本 fixture，分别断言时间线出现且位于总览与逐年正文之间。
2. 写旧版本 fixture，断言页面仍可读取且不出现空白时间线标题。
3. 更新版本联合类型和守卫，避免依靠正文文本猜版本。
4. 五个阅读器统一调用 `NarrativeTimeline`，不得复制时间线 JSX。
5. 检查事业两年、单身两年和其他三年主题的未来步骤数量是否正确。
6. 跑 `npm test -- --run src/pages/ReportReaderPage.test.tsx src/components/report/NarrativeTimeline.test.tsx`。
7. 提交：`git commit -m "feat(web): show past present future in all reports"`。

### Task 12: 四主题固定样本、人工验收与全量回归

**Files:**
- Create: `docs/samples/four-topic-timeline-preflight-2026-09-01.md`
- Modify: `apps/server/src/test/java/com/bazi/app/report/ReportReaderLanguageTest.java`
- Modify: relevant fixed-sample/preflight tests under `apps/server/src/test/java/com/bazi/app/report/**`

**Steps:**

1. 至少生成：事业 4 种状态、财富 R01–R12、感情 3 种状态、综合 R01–R12。
2. 统计过去核对断言违规、空证据、跨段原句重复、未来行动重复、主题串味，目标全部为 0。
3. 人工审核每个主题至少 3 份，重点检查中文主语明确、动作对象明确、无“卡点”“抓手”“承接”等工作黑话。
4. 明确记录：固定样本只能证明结构和表达稳定，不能证明真实经历或未来判断准确。
5. 后端全量运行 `cd apps/server && ./mvnw test`。
6. 前端全量运行 `cd apps/web && npm test -- --run && npm run lint && npm run build`。
7. 运行 `git diff --check`。
8. 只在全部通过后提交：`git commit -m "test(report): verify four-topic narrative timelines"`。

---

## 验收标准

- 每份新报告都有且只有一个统一时间线。
- 过去固定回看上一完整年度，恰好两条可核对现象，不写成确定事实。
- 当下判断必须包含具体主题对象和一个优先处理事项。
- 未来行动按年度输出，跨年不能使用相同标题、核心短语或动作对象。
- 财富文案不能像事业换词；感情文案必须区分单身、交往中和已婚；综合必须体现四维联动。
- 历史命书全部可读，旧 JSON 缺少 `timeline` 不报错。
- 会员次数、单份付费、失败不扣费等现有规则不变。
- 后端、前端全量测试、lint、build 和 `git diff --check` 全部通过。

## 推荐执行批次

1. **批次 A（底座）**：Task 1–3。
2. **批次 B（主题生成）**：Task 4–8；每完成一个主题先输出样本验收，不一次性堆完。
3. **批次 C（版本与前端）**：Task 9–11。
4. **批次 D（总验收）**：Task 12。

由于当前工作区已有大量尚未提交的 v1.1 与其他改动，执行前应先建立当前代码检查点；不要在未区分改动归属的情况下直接按任务提交。
