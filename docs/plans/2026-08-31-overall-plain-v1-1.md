# 综合运势通俗版 v1.1 Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** 让综合命书的跨年差异来自年度依据，而不是只换标题；同时解释“加强/缓和”和四个方面之间的实际联动。

**Architecture:** 保留 v1 的四维评估和三年主次裁决，在结构化评估与文案之间新增“年度主导依据”解析层。v1.1 文案根据维度、状态、年度主导依据和相邻年度变化生成；每年新增一条跨维联动结论。内容版本升级为 `overall-narrative-v1.1`，历史 `overall-narrative-v1` 继续可读。

**Tech Stack:** Java 17、Spring Boot、Jackson、JUnit 5、React、TypeScript、Vitest。

---

## 边界

- 不引入 LLM，不生成具体事件，不增加用户问卷。
- 不用年份序号机械轮换同义句；年度差异必须能追溯到当年依据或相邻年变化。
- 保留每年四维完整输出、一个主焦点、一个次焦点和两项行动。
- v1.1 继续会员内测，不修改 `ReportProducts`，不开放单次购买。
- 当前工作区包含此前未提交改动，本计划不自动提交；待用户明确要求后再按范围提交。

### Task 1: 把 v1.1 内容门槛写成失败测试

**Files:**
- Modify: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePlannerTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePreflightTest.java`

**Steps:**
1. 增加 R03 固定命盘测试：相同维度三年的 `judgment` 不得出现完整句重复。
2. 增加 R03 测试：连续两年主焦点相同时，`priorityIssue` 与 `changeCondition` 必须解释年度差异，不能完全相同。
3. 增加 R03 测试：连续年度的“加强”必须说明具体变化，不能只返回关系标签。
4. 运行 `cd apps/server && ./mvnw -Dtest=OverallNarrativePlannerTest,OverallNarrativePreflightTest test`。
5. 预期测试因重复判断、重复优先问题或空泛转折失败，确认测试确实覆盖 v1 的问题。`linkage` 字段测试在 Task 4 新增，避免尚无字段时阻断 Task 2–3 的红绿循环。

### Task 2: 解析年度主导依据并生成有依据的四维判断

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallEvidenceAngle.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallEvidenceAngleResolver.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/overall/OverallPlainCopy.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/overall/OverallNarrativePlanner.java`
- Test: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePlannerTest.java`

**Steps:**
1. 先写解析器失败测试，覆盖责任、表达、钱财、协作、合、冲刑害破和基础承载等依据族。
2. 运行测试并确认类型或解析器尚不存在导致失败。
3. 实现确定性优先级：限制状态优先解释冲、刑、害、破；支持状态优先解释当年功能组与合；混合状态同时保留支持与限制角度；最后才使用基础承载。
4. 把 `judgment` 改为接收结构化角度，不再按年份序号轮换。相同状态但依据族不同，必须生成不同、自然且有对象的句子。
5. 重新运行 Task 1–2 测试，确认相同维度跨年完整句不重复，且禁用词仍为零。

### Task 3: 把“加强 / 缓和 / 延续”写进正文

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/overall/OverallNarrativePlanner.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/overall/OverallPlainCopy.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePlannerTest.java`

**Steps:**
1. 先写失败测试，构造同一主焦点连续两年分别“加强”和“缓和”的评估结果。
2. 要求 `transition` 说明下一年哪类压力增加或哪类条件缓和，不能只输出关系标签。
3. 让连续主焦点的 `priorityIssue` 和 `changeCondition` 使用当年主导依据，避免同句重复。
4. 运行测试，确认“加强”与“缓和”输出可由普通用户直接区分。

### Task 4: 增加跨维联动结论

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/overall/OverallNarrativePlan.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/overall/OverallNarrativePlanner.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/overall/OverallPlainCopy.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePlannerTest.java`

**Steps:**
1. 先写失败测试，要求每个年度输出 `linkage`。
2. `linkage` 必须同时解释主焦点如何影响次焦点，例如精力安排如何影响工作兑现、工作成果如何影响钱财余量。
3. 为 12 种主次维度组合提供日常中文，不出现术语堆叠或万能句。
4. 旧 v1 内容缺少该字段时允许反序列化为空，保证历史报告可读。
5. 运行综合模块测试。

### Task 5: 升级内容版本并更新前端阅读页

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/service/ReportService.java`
- Modify: `apps/server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java`
- Modify: `apps/web/src/services/reportApi.ts`
- Modify: `apps/web/src/components/report/OverallReportReader.tsx`
- Modify: `apps/web/src/pages/ReportReaderPage.test.tsx`
- Modify: `apps/web/src/styles/app.css`
- Modify: `contracts/openapi.yaml`

**Steps:**
1. 先写后端失败测试：新报告版本必须为 `overall-narrative-v1.1`，旧 v1 快照仍可读取。
2. 先写前端失败测试：v1.1 页面展示“为什么先看这件事”的联动结论，v1 旧报告不显示空区域。
3. 升级生成版本并让类型守卫同时识别 v1 与 v1.1。
4. 在年度总判断和四维卡片之间展示联动结论，保持现有纸张视觉，不增加仪表盘或评分。
5. 运行后端保存测试、前端阅读页测试、前端构建。

### Task 6: v1.1 全量预演与内测结论

**Files:**
- Modify: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePreflightTest.java`
- Create: `docs/samples/overall-v1-1-preflight-2026-08-31.md`

**Steps:**
1. 对 R01–R12 重新生成 36 个年度内容。
2. 除 v1 指标外，新增统计：同维度完整判断重复、主焦点优先问题重复、调整条件重复、空联动结论。
3. 人工复审 R03、R08、R11，确认年度差异可从依据解释，且不再像四个单主题并列。
4. 运行后端全量测试、前端全量测试、代码检查和生产构建。
5. 继续保持会员内测；只有用户明确验收后才另行讨论商业开放。
