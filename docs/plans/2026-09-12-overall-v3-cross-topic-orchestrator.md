# Overall v3 Cross-Topic Orchestrator Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** 把综合运势改造成跨主题决策中枢，复用事业、财富、关系的真实计算结果，每年只给一个有依据、可验证、可调整的优先行动，同时彻底阻止此前三个主题出现过的重复、臆测、语病和付费后失败问题复发。

**Architecture:** 新增只供后端内部使用的 `OverallTopicSnapshot`，由事业基础规则、财富 v3 评估、关系五维评估和生活节奏评估分别提供确定性快照；`OverallDecisionArbitrator` 只负责风险优先、依赖关系和跨主题冲突裁决，不重新计算主题结论。新输出使用严格的 `OverallV3NarrativePlan`，旧 `OverallNarrativePlan` 继续只读取 v1/v1.1/v2 快照；前端 v3 每年展示一个行动闭环和压缩观察项，不再叠加四段正文与旧行动列表。

**Tech Stack:** Java 17、Spring Boot 3、Jackson、JUnit 5、React 19、TypeScript、Vitest、Testing Library、Playwright、OpenAPI YAML。

---

## 实施前提

- 当前工作区包含财富 v3.17、事业 v5、关系 v3 和共用行动闭环的未提交改动。开始本计划前必须先建立代码检查点，不能把综合 v3 与前一批改动混成一个不可回退的大提交。
- 不引入 LLM，不新增用户问卷，不读取用户历史报告来猜测职业或关系状态。
- 综合版继续固定销售三年、技术边界五年；本阶段继续会员内测，不开放单份购买。
- 旧 `overall-narrative-v1`、`overall-narrative-v1.1`、`overall-narrative-v2` 永久按快照读取，不回算、不改写。
- “不同命盘必须不同文案”不是正确规则。正确规则是：不同计算焦点不能共用同一决策；相同计算结论可以相同，但不得靠年份序号或随机数伪造差异。

## 防复发验收矩阵

| 已出现过的问题 | v3 强制门槛 |
|---|---|
| 不同命盘生成相同内容 | 记录快照指纹；不同 `focusKey` 不得共享行动、预期结果和有效信号三元组 |
| 三年标题或重点重复 | 允许真实主主题连续，但标题必须说明当年证据角度或相邻年变化；禁止按年份轮换模板 |
| 去年回顾千人一面 | 继续使用证据型核对句；回顾依据必须属于上一年，不能复制当前年或静态通用句 |
| 语义不完整、中文不顺 | 建立完整句、主语对象、搭配、长度和禁用词检查；进入前端前跑人工样本审核 |
| “卡点”“先观察”等空话 | 复用并扩充 `AnnualActionGuidePolicy`，禁止模糊片段和无验证条件的建议 |
| 默认用户是上班族或某种关系状态 | 综合行动只使用身份中立对象；不得出现老板、客户、配偶、对象等未经输入确认的称呼 |
| 为了多样性改变计算结论 | 裁决不接收年份序号，不使用随机数，不因上年主题相同而降级真实第一结论 |
| 付费或会员次数扣除后生成失败 | 所有生成与文案校验在 `requireSuccessfulReportSlot` 之前完成；内容多样性不足不得成为用户侧错误 |
| localhost 登录、白名单反复出错 | E2E 固定 `localhost`、`VITE_API_MODE=http`、显式 CORS，并检查注册登录和控制台错误 |

## v3 内部快照契约

```java
public record OverallTopicSnapshot(
    int year,
    Topic topic,
    String focusKey,
    Stance stance,
    Urgency urgency,
    ConfidenceLevel confidence,
    String opportunityKey,
    String riskKey,
    List<String> actionCandidateKeys,
    List<String> evidenceKeys) {

  public enum Topic { RHYTHM, CAREER, WEALTH, RELATIONSHIP }
  public enum Stance { SUPPORTIVE, BALANCED, MIXED, PRESSURED }
  public enum Urgency { LOW, MEDIUM, HIGH }
}
```

约束：

- `focusKey` 必须由计算对象组成，不能包含 `first-year`、`second-year` 等排版信息。
- `evidenceKeys` 至少包含两个独立依据；只有 `BALANCED + LOW confidence` 允许进入保守快照，但不能输出确定性事件。
- 快照只携带语义键，不携带可以直接显示的完整文案，避免适配器把各主题报告整段复制到综合版。
- 综合版不得修改主题快照，只能排序、建立联动和选择行动候选。

## 裁决规则

采用分层比较，不使用未经校准的加权魔法数：

1. `HIGH` 风险且有当年直接证据的主题优先。
2. 生活节奏受压时作为承载限制，阻止同时扩张两个目标。
3. 财富受压时作为资源限制，阻止需要提前投入较多资金的行动。
4. 没有高风险时，优先选择高可信、当年直接证据更完整的机会。
5. 主主题确定后，从能解释其限制或结果的主题中选择次主题。
6. 完全并列时使用固定主题顺序和 `focusKey` 字典序，保证同输入同输出。
7. 不读取上一年主主题来强行轮换；连续三年同一主主题是合法结果。

---

### Task 0: 建立现有成果检查点

**Files:**
- Inspect: all current tracked and untracked changes except `work/`

**Steps:**

1. 运行 `git status --short`，确认 `work/` 仍为用户资料且不纳入提交。
2. 运行后端全量测试：`cd apps/server && ./mvnw -q test`，预期 0 failures / 0 errors。
3. 运行前端全量测试与构建：`cd apps/web && npm test -- --run && npm run build`。
4. 运行 `git diff --check`。
5. 仅在用户明确确认提交范围后提交当前财富、事业、关系和共用行动闭环改动。
6. 记录检查点提交号，再开始 Overall v3。

### Task 1: 用失败测试锁死既有错误

**Files:**
- Modify: `apps/server/src/test/java/com/bazi/app/report/overall/OverallPeriodArbitratorTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePlannerTest.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/OverallV3RegressionGateTest.java`

**Steps:**

1. 写失败测试：连续年度最高主题相同时，裁决不得为了多样性选择三分以内的次主题。
2. 写失败测试：相同语义输入只改变 `yearIndex` 时，行动与标题语义不得变化。
3. 写失败测试：两个不同 `focusKey` 不得产生相同的行动、预期结果、有效信号三元组。
4. 写失败测试：输出不得包含 `老板`、`客户`、`配偶`、`对象`、`求职`、`创业` 等未经综合版输入确认的身份词。
5. 写失败测试：完整输出不得包含“卡点”“卡住”“先观察再判断”“可以作为关注”“现有证据不足”等禁用表达。
6. 运行：`cd apps/server && ./mvnw -Dtest=OverallPeriodArbitratorTest,OverallNarrativePlannerTest,OverallV3RegressionGateTest test`。
7. 预期至少因旧主主题轮换和按年份选文案失败；保留红灯证据。

### Task 2: 定义严格的主题快照与决策契约

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/OverallTopicSnapshot.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/OverallAnnualDecision.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/v3/OverallTopicSnapshotTest.java`

**Steps:**

1. 先写构造失败测试：空 `focusKey`、空证据、重复证据、空行动候选必须拒绝。
2. 为 `OverallAnnualDecision` 定义 `primary`、`secondary`、`conflictKey`、`decisionKey`、`selectedActionKey` 和合并证据。
3. 要求主次主题不同，决策证据必须是两个输入快照证据的子集。
4. 不在契约中加入完整中文文案或年份序号。
5. 运行：`./mvnw -Dtest=OverallTopicSnapshotTest test`，预期通过。

### Task 3: 从三个主题真实评估器生成只读快照

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/CareerOverallSnapshotAdapter.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/WealthOverallSnapshotAdapter.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/RelationshipOverallSnapshotAdapter.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/RhythmOverallSnapshotAdapter.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/OverallSnapshotFactory.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/v3/OverallSnapshotFactoryTest.java`

**Steps:**

1. 事业适配器调用 `AnnualPeriodAssessor.assessYear(context, ReportTopic.CAREER)` 的无问卷基础判断；只复用 `stage`、`confidence`、`ruleKeys` 和证据，不复制职业情境文案。
2. 财富适配器调用 `WealthV3Analyzer.analyze(chart, contexts)`；`focusKey` 必须来自真实 `focus.primaryCandidates` 和对应 `Decision`，风险来自 `WealthAssessment.Risk`。
3. 关系适配器调用 `RelationshipFactExtractor`、`RelationshipDimensionEvaluator`、`RelationshipPeriodArbitrator`；只复用状态无关的五维焦点、语气与风险，不假设单身、交往或已婚。
4. 生活节奏适配器暂时复用 `OverallDimensionEvaluator` 的 `RHYTHM` 维度；禁止它重新产生事业、财富、关系评分。
5. 写一致性测试：同一命盘同一年，综合快照的财富焦点必须等于财富 v3 评估焦点；关系证据必须属于关系五维评估；事业规则键必须属于事业基础评估。
6. 写身份测试：适配器输出的语义键不含任何职业或关系状态。
7. 运行：`./mvnw -Dtest=OverallSnapshotFactoryTest test`。

### Task 4: 实现不为多样性让路的跨主题裁决器

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/OverallDecisionArbitrator.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/OverallConflictCatalog.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/v3/OverallDecisionArbitratorTest.java`

**Steps:**

1. 写表格化失败测试，至少覆盖：节奏受压＋事业有利、财富受压＋事业有利、关系受压＋事业有利、无风险双机会、完全并列、连续三年同主题。
2. 用 `urgency → direct evidence → confidence → dependency → fixed tie-break` 的分层比较实现主主题选择。
3. 冲突目录只返回语义键，例如 `capacity_before_career_expansion`、`cash_buffer_before_growth`，不能直接返回整句文案。
4. 删除或停止调用 `OverallPeriodArbitrator.selectPrimary` 中“相近候选轮换”逻辑；旧 v1-v2 快照读取不受影响。
5. 加入确定性测试：相同快照顺序打乱后，仍得到相同决策。
6. 加入真实性测试：连续三年真实第一均为事业时，三年主主题都必须是事业。
7. 运行：`./mvnw -Dtest=OverallDecisionArbitratorTest,OverallPeriodArbitratorTest test`。

### Task 5: 先写行动闭环，再写综合正文

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/OverallActionGuideWriter.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/OverallV3CopyCatalog.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/OverallV3NarrativePolicy.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/v3/OverallActionGuideWriterTest.java`

**Steps:**

1. 写失败测试：每个冲突键必须输出问题、行动、预期变化、检查时间、有效信号、调整条件和备用行动。
2. `focusKey` 使用 `overall.<primary>.<secondary>.<conflictKey>`，禁止加入年份位置。
3. 行动使用身份中立对象，例如“当前最重要的责任”“实际到账与必要支出”“最受影响的一段重要关系”，不写未经确认的具体身份。
4. `expectedChange` 必须同时说明主主题收益和次主题保护，不能只写“会更顺利”。
5. 检查时间必须含明确的周、月或次数；成功信号必须是可观察事实。
6. 无效时的备用行动必须缩小范围、延后投入或重排顺序，不得返回“继续观察”。
7. 先调用共用 `AnnualActionGuidePolicy`，再调用综合专属策略检查身份臆测、句子完整性和跨主题一致性。
8. 运行：`./mvnw -Dtest=OverallActionGuideWriterTest,AnnualActionGuidePolicyTest test`。

### Task 6: 建立全新的 Overall v3 输出，不污染旧快照

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/OverallV3NarrativePlan.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/v3/OverallV3NarrativePlanner.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/v3/OverallV3NarrativePlannerTest.java`

**Steps:**

1. 定义 v3 年度字段：年度标题、主主题、次主题、联动说明、一个 `AnnualActionGuide`、两个压缩观察项、年度转折和证据。
2. 三年总论只描述真正的主题路径；同主题连续时说明阶段变化，不强行写成“转向”。
3. 复用现有 `OverallTimelinePlanner` 的年份和证据边界，但重写展示文案，防止过去、当下、未来与年度正文逐句重复。
4. 每年只允许一个行动闭环；旧 `priorityIssue`、`actions`、`changeCondition` 不进入 v3 契约。
5. 每年另外两个非主次主题只输出短观察项，不生成第二套行动。
6. 对所有三年行动闭环整体运行 `AnnualActionGuidePolicy.validate(...)`。
7. 写测试：完整三年、同输入同输出、不同 `focusKey` 不共享决策、同主题连续合法、时间线证据归属正确。
8. 运行：`./mvnw -Dtest=OverallV3NarrativePlannerTest,OverallV3RegressionGateTest test`。

### Task 7: 接入保存、版本和“失败不占次数”事务链路

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/service/ReportService.java`
- Modify: `apps/server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java`
- Modify: `contracts/openapi.yaml`

**Steps:**

1. 先写失败集成测试：新综合报告版本必须为 `overall-narrative-v3`，并包含三年行动闭环。
2. 新增历史读取测试：v1、v1.1、v2 仍反序列化为旧 `OverallNarrativePlan`，v3 反序列化为 `OverallV3NarrativePlan`。
3. 在 `ReportService` 里用 `OverallSnapshotFactory → OverallDecisionArbitrator → OverallV3NarrativePlanner` 替换新生成链路。
4. 保持内容计算与所有策略校验发生在 `requireSuccessfulReportSlot` 之前。
5. 写生成异常测试：适配器或策略抛错时，不新增 ready 报告、不占会员当日次数；随后正常生成仍可成功。
6. 对无法形成多样文案但计算有效的情况使用确定性保守文案，不允许抛“内容多样性不足”给用户。
7. 更新 OpenAPI 版本枚举与严格 v3 schema，解析 YAML 并运行集成测试。
8. 运行：`./mvnw -Dtest=SavedReportIntegrationTest test`。

### Task 8: 改造前端为“年度决策总览”

**Files:**
- Modify: `apps/web/src/services/reportApi.ts`
- Create: `apps/web/src/components/report/OverallV3ReportReader.tsx`
- Modify: `apps/web/src/pages/ReportReaderPage.tsx`
- Modify: `apps/web/src/pages/ReportReaderPage.test.tsx`
- Modify: `apps/web/src/styles/app.css`

**Steps:**

1. 先写失败测试：v3 每年恰好一张 `.annual-action-guide`，三年共三张。
2. 断言 v3 不渲染旧“四个方面完整卡片＋两条行动＋调整条件”的重复结构。
3. 每年展示主主题、次主题和联动说明；另外两个主题以两条短观察项展示。
4. 复用 `AnnualActionGuideCard`，不复制财富、事业或关系主题组件。
5. v1-v2 继续使用 `OverallReportReader`，v3 使用新阅读器。
6. 在 430px 与 360px 下断言无横向溢出、纸张与背景对齐；桌面端保持当前阅读宽度。
7. 运行：`cd apps/web && npm test -- --run src/pages/ReportReaderPage.test.tsx && npm run build`。

### Task 9: 建立跨命盘语义碰撞与中文质量预演

**Files:**
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/v3/OverallV3PreflightTest.java`
- Create: `apps/server/src/test/resources/report/overall-v3-engineering-corpus.json`
- Create: `docs/samples/overall-v3-preflight-2026-09-12.md`

**Steps:**

1. 纳入 R01–R12、既有经典兼容样本和至少 12 个边界样本；每个样本计算上一年加产品三年。
2. 输出主题快照指纹、主次主题路径、冲突键、行动三元组和证据键分布。
3. 统计不同 `focusKey` 的行动三元组碰撞率，目标必须为 0。
4. 统计完全重复标题、联动说明、行动句、预期结果和有效信号；同报告跨年完全重复必须为 0。
5. 检查句子是否缺少对象或谓语，单句建议不超过 48 个汉字；人工复核所有超限句。
6. 禁止身份臆测词、模糊词、未解析占位符和异常标点。
7. 人工抽查至少 6 份完整报告，重点判断：是否真的给出优先顺序、行动是否与计算焦点相符、不同命盘是否只是在换词。
8. 将结果写入预演文档；任何否决项未清零时不得接入前端入口。

### Task 10: 完整联调、上线门槛与回滚

**Files:**
- Modify: `e2e/run_e2e.py`
- Modify: `e2e/README.md`

**Steps:**

1. E2E 使用 `APP_CORS_ORIGINS=http://localhost:5173` 启动后端。
2. 前端使用 `VITE_API_MODE=http npm run dev -- --host localhost`，禁止改用 127.0.0.1 或 mock 模式代替验收。
3. 新注册用户、开通会员、生成综合 v3、刷新、进入书架再打开。
4. 断言三年三张行动卡、没有旧行动列表、没有横向溢出、控制台无错误。
5. 制造一次生成前异常，确认报告数与当日已用次数不变。
6. 后端全量：`cd apps/server && ./mvnw -q test`。
7. 前端全量与构建：`cd apps/web && npm test -- --run && npm run build`。
8. 契约和差异：解析 `contracts/openapi.yaml`，运行 `git diff --check`。
9. 保留 `OVERALL_CONTENT_VERSION` 的单点回滚能力；回滚生成版本不得影响已保存 v3 的读取。
10. 继续会员内测。只有跨主题一致性预演、中文人工复核和真实用户反馈都通过后，才讨论单独售卖。

## 最终验收定义

- 综合版回答的是“今年有限的时间和资源先放在哪里”，不是四份报告的摘要。
- 同一命盘同一年，综合版的财富、事业、关系基础判断与对应主题引擎不矛盾。
- 连续三年真实主主题相同时如实保留，不为标题多样性改计算。
- 三年每年只有一个行动闭环，且行动、结果、验证、调整完整。
- 不假设职业、收入结构和感情状态。
- 不出现其他三个主题已经暴露过的固定句、语病、空话、重复和扣次后失败问题。
- 后端、前端、契约、E2E、人工预演全部通过后才视为完成。
