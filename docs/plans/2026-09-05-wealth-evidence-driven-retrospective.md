# 财富去年回看证据驱动改造 Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** 将财富报告“去年回看”从“一条财富路径对应一整段固定文案”改为由主要对象、判断方向、主证据、次要对象和隐性影响共同决定的确定性内容，并消除不同有效证据被压缩成相同完整回顾的问题。

**Architecture:** 保留现有 `WealthAssessment` 计算结果，不改命盘、支付、会员和报告存储结构。在 `WealthTimelinePlanner` 前增加“信号提取 → 仲裁 → 结构化计划 → 文案生成”四步；文案仍使用封闭词库且不引入随机数、用户画像或 LLM。历史报告保持原快照，新报告将 `copyVersion` 升级为 `wealth-plain-v3.5`。

**Tech Stack:** Java 17、Spring Boot 3.5、JUnit 5、Jackson、React 19、TypeScript、Vitest。

---

## 不可妥协的验收原则

1. 不追求“每个人必须写得不一样”；只有相关证据签名不同，完整回顾才必须不同。
2. 相同输入重复生成 100 次，结构化计划与可见文案必须完全一致。
3. 不得通过随机同义词、姓名、用户 ID、生成时间制造差异。
4. 不得凭命盘推断工资、客户、项目、订单、职业或收入结算形式。
5. 主判断、次判断、隐性影响必须来自不同的主题或角度，并各自能追溯到证据。
6. R01–R12 中 `differentSignatureSameBlockCount` 必须为 0；若签名相同，允许文案相同。
7. 旧报告快照不重算、不迁移，前端继续正常展示 `wealth-plain-v3` 至 `v3.4`。

## 批次 A：先让旧架构在测试中明确失败

### Task 1：建立跨命盘回顾碰撞基线

**执行状态（2026-09-05）：已完成红灯基线。** 本轮仅执行 Task 1，保留预期失败，不进入生产代码修复。签名以当前 `PastReview.evidenceKeys` 的去重排序集合为准；`differentSignatureSameBlockCount` 统计文字相同但签名不同的无序样本对数，`largestDifferentSignatureCollisionGroup` 统计同一文字组内的最大不同签名数（无碰撞时为 1）。此签名仅描述当前回顾引用的有效证据集合，不代替 Task 2 的结构化计划签名。

本轮执行结果：

- 定向测试：5 项，3 项通过、2 项预期断言失败、0 错误；失败分别为全样本跨签名碰撞与 R03/R04/R05 固定回归组。
- 后端全量：541 项，539 项通过、仅上述 2 项预期失败、0 错误、0 跳过。业务代码未修改。
- JSON 基线：`apps/server/target/wealth-retrospective-collision-baseline.json`；12 份样本、11 种签名、4 种完整回顾、20 对跨签名同文碰撞，最大同文组包含 6 种不同签名。
- 碰撞组：R01/R02/R07/R09；R03/R04/R05/R08/R10/R11。R01 与 R02 签名相同，因此两者互相不计为碰撞。
- `ReportReaderLanguageTest` 的预演产物另记完整回顾种类与重复组，区分“一份报告内跨区段不重复”和“不同命盘之间不重复”。
- 基线中的 SHA-256、计数均经独立脚本复算；定向与全量测试两次生成的 JSON 文件一致。产物位于已忽略的 `target/`，测试失败前即写出，可重复生成。

**Files:**
- Modify: `apps/server/src/test/java/com/bazi/app/report/ReportReaderLanguageTest.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthRetrospectiveCollisionTest.java`

**Step 1: 写失败测试**

读取 `src/test/resources/report/wealth-v2-baseline-inputs.json` 的 R01–R12，分别生成上一年回顾。为每份样本记录：

```java
record ReviewCase(
    String fixtureId,
    String evidenceSignature,
    String visibleBlock) {}
```

先用与当前实现一致的有效证据集合构造稳定签名，并断言：

```java
assertEquals(0, differentSignatureSameBlockCount(cases));
```

同时固定已知回归组：R03、R04、R05 的证据签名不同，完整回顾不得全部相同。

**Step 2: 运行测试确认失败**

Run:

```bash
cd apps/server
./mvnw -Dtest=WealthRetrospectiveCollisionTest test
```

Expected: FAIL，报告至少一组“证据签名不同但完整回顾相同”。

**Step 3: 输出机器可读基线**

在测试运行时写入 `target/wealth-retrospective-collision-baseline.json`，至少包含：

```json
{
  "sampleCount": 12,
  "uniqueSignatureCount": 0,
  "uniqueVisibleBlockCount": 0,
  "differentSignatureSameBlockCount": 0,
  "largestDifferentSignatureCollisionGroup": 0,
  "cases": []
}
```

数字由测试实时计算，不在代码中写死。

**Step 4: Commit**

```bash
git add apps/server/src/test/java/com/bazi/app/report/ReportReaderLanguageTest.java apps/server/src/test/java/com/bazi/app/report/wealth/WealthRetrospectiveCollisionTest.java
git commit -m "test(report): expose retrospective copy collisions"
```

## 批次 B：建立结构化回顾计划

**执行状态（2026-09-05）：Task 2–4 已完成。** 新增 24 项测试全部通过；后端全量 565 项，563 项通过、2 项 Task 1 预期失败、0 错误、0 跳过。12 份固定出生样本均为 complete，12 种新计划证据签名，每份打乱 facts/evidence/decisions 顺序重复计算 100 次，计划和签名均一致。详情见 `docs/samples/wealth-retrospective-batch-b-preflight-2026-09-05.md`。

实现中的两项细化：

- 计划采用 `Observation primary/secondary/hidden` 嵌套结构，每个判断保留 subject、direction、strength、angle、完整 citations 与 dominantEvidenceIds。Citation 同时保存规则和解析后的 root fact 值，避免只比较 `annual.stem.ten_god` 等字段名而漏掉实际值变化。
- 对有证据的槽位严格校验字段和来源；当独立证据无法支撑全部槽位时，以 `null` 槽位、`missingSlots()` 和 `completeness()` 显式表示缺项。这是内部数据状态，不是新增用户错误码。批次 C 必须处理该状态，不能强制凑出三条判断。12 份固定出生样本本轮均为 complete。

本批次仅增加内部组件，不修改 `WealthTimelinePlanner` 的现有调用和 v3.4 文案。Task 1 的两项跨命盘文案断言预计继续失败，需 Task 5–6 接入后才能修复。

### Task 2：定义内部计划与稳定证据签名

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthRetrospectivePlan.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthRetrospectivePlanTest.java`

**Step 1: 写失败测试**

覆盖以下约束：

- `primarySubject`、`secondarySubject`、`hiddenSubject` 不得为空。
- `primarySubject` 与 `secondarySubject` 不得相同。
- `primaryAngle` 与 `secondaryAngle` 不得相同。
- 证据 ID 排序不同不能改变 `evidenceSignature`。
- 任意一个参与叙述的证据变化时，签名必须变化。

**Step 2: 运行测试确认失败**

```bash
cd apps/server
./mvnw -Dtest=WealthRetrospectivePlanTest test
```

Expected: FAIL，类型尚不存在。

**Step 3: 实现最小计划模型**

实际内部结构（原扁平草案已细化为逐判断可追溯结构）：

```java
public record WealthRetrospectivePlan(
    int year,
    Observation primary,
    Observation secondary,
    Observation hidden) {}
```

`evidenceSignature()` 由规划版本、年份、三个槽位的判断字段、引用证据的 rule/fact key、family、带符号权重和 root 的 kind/code/value 计算；包含主证据集合，集合排序、去重后再编码。引用 ID 用于追溯，不参与语义签名；不含姓名、用户 ID 或时间戳。签名用于审计，不能用作随机选句种子，也不能把签名不同当成“可见内容已经不同”的证明。

**Step 4: 运行测试确认通过并提交**

```bash
cd apps/server
./mvnw -Dtest=WealthRetrospectivePlanTest test
git add src/main/java/com/bazi/app/report/wealth/v3/WealthRetrospectivePlan.java src/test/java/com/bazi/app/report/wealth/WealthRetrospectivePlanTest.java
git commit -m "feat(report): define wealth retrospective plan"
```

### Task 3：提取所有与回顾有关的信号

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthRetrospectiveSignalExtractor.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthRetrospectiveSignalExtractorTest.java`

**Step 1: 写失败测试**

验证提取器保留而不是丢弃以下差异：

- 五条财富路径的支持权重、限制权重、净权重、stance、strength。
- 年干、大运、原局结构、冲合刑害对应的证据 family 与 rule/fact key。
- `retention` 和风险限制，即使它们没有进入 `focus.primaryCandidates`。
- 每个信号都能回溯到原始 evidence/fact。

**Step 2: 运行测试确认失败**

```bash
cd apps/server
./mvnw -Dtest=WealthRetrospectiveSignalExtractorTest test
```

**Step 3: 实现确定性提取器**

提取器只归一化数据，不决定文案。所有集合按显式顺序排序，禁止依赖 `HashMap` 遍历顺序。

**Step 4: 运行测试并提交**

```bash
cd apps/server
./mvnw -Dtest=WealthRetrospectiveSignalExtractorTest test
git add src/main/java/com/bazi/app/report/wealth/v3/WealthRetrospectiveSignalExtractor.java src/test/java/com/bazi/app/report/wealth/WealthRetrospectiveSignalExtractorTest.java
git commit -m "feat(report): extract retrospective wealth signals"
```

### Task 4：实现主要、次要与隐性影响仲裁

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthRetrospectiveArbitrator.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthRetrospectiveArbitratorTest.java`

**Step 1: 写失败测试**

至少覆盖：

- 主要对象从五条路径共同竞争，不直接复用 `focus.primaryCandidates().get(0)`。
- `retention` 可成为主对象，也可成为隐性影响，不再永久被收入路径排除。
- 次要对象必须与主要对象不同。
- 隐性影响优先选择留存、支出、责任或时点变化，但必须有证据。
- 并列时使用明确顺序：显著性 → 净权重绝对值 → 证据具体度 → 显式 subject 优先表。
- 将 `decisions`、`evidence` 顺序打乱后，计划完全相同。

**Step 2: 运行测试确认失败**

```bash
cd apps/server
./mvnw -Dtest=WealthRetrospectiveArbitratorTest test
```

**Step 3: 实现仲裁器**

禁止使用随机数。若不足以形成三个互异角度，允许隐性影响回到同一 subject，但 `angle` 必须不同并且有独立证据；不允许编造第三个结论。

**Step 4: 运行测试并提交**

```bash
cd apps/server
./mvnw -Dtest=WealthRetrospectiveArbitratorTest test
git add src/main/java/com/bazi/app/report/wealth/v3/WealthRetrospectiveArbitrator.java src/test/java/com/bazi/app/report/wealth/WealthRetrospectiveArbitratorTest.java
git commit -m "feat(report): arbitrate retrospective wealth angles"
```

## 批次 C：生成自然语言并接入现有报告

**执行状态（2026-09-05）：Task 5–6 已完成。** 新回顾写入器已接入 `DefaultWealthReportGenerator → WealthTimelinePlanner` 正式链路。跨命盘基线由 12 份样本、4 种完整回顾、20 对跨签名同文碰撞，变为 12 份样本、12 种完整回顾、0 对碰撞，最大碰撞组为 1。相关 99 项测试通过；后端全量 595 项全部通过。文案版本仍为 `wealth-plain-v3.4`，需 Task 7 升级并完成前端兼容后再作为 v3.5 对外验收。

人工复核后增加了三条语言约束：每份回顾只出现一次“命盘提示”；隐性影响直接描述其证据角度，不能重复次判断的问题；桥接句使用“核对清楚／分开看”，避免“责任是否有改善”等不自然搭配。详情见 `docs/samples/wealth-retrospective-batch-c-preflight-2026-09-05.md`。

### Task 5：把整段模板改成按结构字段组合

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthRetrospectiveWriter.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthRetrospectiveWriterTest.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthPlainCopyV3.java`

**Step 1: 写失败测试**

为五个 subject、四种 direction 和主要证据角度建立表驱动用例，断言：

- 主判断明确说“回看什么”。
- 次判断解释另一个可核对现象。
- 隐性影响说明收入、支出、到账或责任之间的实际影响。
- 每句话有明确主语或对象，避免“卡住了”“出现变化”等悬空表达。
- 禁止职业推断词和内部术语。
- 单句不超过现有阅读规范规定的长度。

**Step 2: 运行测试确认失败**

```bash
cd apps/server
./mvnw -Dtest=WealthRetrospectiveWriterTest test
```

**Step 3: 实现封闭词库组合器**

文案由 `subject + direction + angle` 选择短句片段；不得为同一结构维护多条随机同义句。桥接句应引用主对象和当前判断方向，不再只按 path 输出固定句。

**Step 4: 运行测试并提交**

```bash
cd apps/server
./mvnw -Dtest=WealthRetrospectiveWriterTest test
git add src/main/java/com/bazi/app/report/wealth/v3/WealthRetrospectiveWriter.java src/main/java/com/bazi/app/report/wealth/v3/WealthPlainCopyV3.java src/test/java/com/bazi/app/report/wealth/WealthRetrospectiveWriterTest.java
git commit -m "feat(report): write evidence-driven wealth reviews"
```

### Task 6：替换 `WealthTimelinePlanner` 的旧回顾分支

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthTimelinePlanner.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/DefaultWealthReportGenerator.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthTimelinePlannerTest.java`

**Step 1: 写失败测试**

增加同一主路径、不同 evidence/stance/secondary/retention 组合的用例，要求完整 `PastReview` 不同；相同 assessment 连续规划 100 次必须相同。

**Step 2: 运行测试确认失败**

```bash
cd apps/server
./mvnw -Dtest=WealthTimelinePlannerTest test
```

**Step 3: 接入新流程**

删除或停止调用：

- `reviewDecision`
- `reviewHeadline(int, String)`
- `pastCheckpoints(int, String)`
- `pastBridge(String)`

改为：

```text
previous assessment
→ signalExtractor.extract
→ arbitrator.plan
→ retrospectiveWriter.write
→ NarrativeTimeline.PastReview
```

今年和未来两年的现有逻辑本任务不改，控制影响范围。

**Step 4: 运行相关测试并提交**

```bash
cd apps/server
./mvnw -Dtest=WealthTimelinePlannerTest,WealthNarrativeV3Test,ReportReaderLanguageTest test
git add src/main/java/com/bazi/app/report/wealth/v3/WealthTimelinePlanner.java src/main/java/com/bazi/app/report/wealth/v3/DefaultWealthReportGenerator.java src/test/java/com/bazi/app/report/wealth/WealthTimelinePlannerTest.java
git commit -m "refactor(report): use retrospective wealth planner"
```

## 批次 D：版本、兼容与全量验收

### Task 7：升级文案版本并保持历史报告兼容

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthPlainCopyV3.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthNarrativeV3Test.java`
- Modify: `apps/server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java`
- Modify: `apps/web/src/services/reportApi.ts`
- Modify: `apps/web/src/services/reportApi.test.ts`
- Modify: `apps/web/src/components/report/WealthV3ReportReader.tsx`
- Modify: `apps/web/src/pages/ReportReaderPage.test.tsx`

**Step 1: 写失败测试**

后端新报告应输出 `copyVersion=wealth-plain-v3.5`；前端类型和阅读器应接受 v3.5，同时保留 v3.0–v3.4 的展示行为。

**Step 2: 运行测试确认失败**

```bash
cd apps/server
./mvnw -Dtest=WealthNarrativeV3Test,SavedReportIntegrationTest test
cd ../web
npm test -- --run src/services/reportApi.test.ts src/pages/ReportReaderPage.test.tsx
```

**Step 3: 最小版本改动**

只升级 copy version，不升级 `calculationVersion`，因为命理评分算法没有改变；不修改数据库 schema 和既有 JSON 快照。

**Step 4: 运行测试并提交**

```bash
cd apps/server
./mvnw -Dtest=WealthNarrativeV3Test,SavedReportIntegrationTest test
cd ../web
npm test -- --run src/services/reportApi.test.ts src/pages/ReportReaderPage.test.tsx
git add ../server/src/main/java/com/bazi/app/report/wealth/v3/WealthPlainCopyV3.java ../server/src/test/java/com/bazi/app/report/wealth/WealthNarrativeV3Test.java ../server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java src/services/reportApi.ts src/services/reportApi.test.ts src/components/report/WealthV3ReportReader.tsx src/pages/ReportReaderPage.test.tsx
git commit -m "feat(report): publish wealth retrospective copy v3.5"
```

### Task 8：运行跨命盘、语言和确定性验收

**Files:**
- Modify: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthRetrospectiveCollisionTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/ReportReaderLanguageTest.java`
- Create: `docs/samples/wealth-v3.5-retrospective-preflight-2026-09-05.md`

**Step 1: 让 Task 1 的红灯变绿**

```bash
cd apps/server
./mvnw -Dtest=WealthRetrospectiveCollisionTest test
```

Expected:

- `differentSignatureSameBlockCount=0`
- `largestDifferentSignatureCollisionGroup=1`
- 同输入 100 次结果一致

**Step 2: 运行财富报告完整测试**

```bash
cd apps/server
./mvnw -Dtest='*Wealth*Test,ReportReaderLanguageTest,SavedReportIntegrationTest' test
```

Expected: PASS。

**Step 3: 运行前端测试和构建**

```bash
cd apps/web
npm test -- --run
npm run build
```

Expected: PASS，TypeScript 无错误，Vite 构建成功。

**Step 4: 生成人工验收样本**

文档至少列出 R01–R12 的：命盘编号、证据签名、主判断、次判断、隐性影响；另列 R03/R04/R05 对照，说明它们为什么不再相同。人工否决项：职业臆测、悬空主语、同义改写式假差异、证据与文案不一致。

**Step 5: Commit**

```bash
git add src/test/java/com/bazi/app/report/wealth/WealthRetrospectiveCollisionTest.java src/test/java/com/bazi/app/report/ReportReaderLanguageTest.java ../../docs/samples/wealth-v3.5-retrospective-preflight-2026-09-05.md
git commit -m "test(report): verify evidence-driven retrospective diversity"
```

## 上线前人工门槛

- 固定 12 份样本全部通过只是技术验收，不代表“去年回看准确率”已经得到证明。
- 再选至少 5 份有真实反馈的命盘进行盲测，记录“明显不符 / 部分沾边 / 比较符合”。
- 若仍出现明显不符，应回到信号与仲裁规则，不允许仅修改句式掩盖问题。
- 只有跨命盘碰撞、自然语言、确定性和真实反馈四项同时达标，才将 v3.5 设为默认付费输出。

## 建议执行顺序

- 批次 A：Task 1，先固化失败证据。
- 批次 B：Task 2–4，完成结构化计划，不接入用户侧。
- 批次 C：Task 5–6，接入去年回看。
- 批次 D：Task 7–8，版本兼容与全量验收。

每个批次完成后先查看样本和测试指标，再进入下一批；禁止在指标未通过时用新增整段模板绕过失败。
