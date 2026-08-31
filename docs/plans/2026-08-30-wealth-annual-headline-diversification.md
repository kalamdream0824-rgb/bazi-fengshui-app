# 财富命书年度标题去重复 Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** 让财富命书三年标题分别表达当年的不同重点，不再重复“在项目和额外收入上”这类主题和措辞；所有差异必须能追溯到当年事实、证据和判断，不能靠随机同义词制造。

**Architecture:** 保留现有事实提取、权重、五路径判断和年度比较；把标题选择从 `WealthNarrativeWriter.annualOverview(year)` 的单年局部决策提升为报告级规划。候选标题携带结构化语义键，规划器对三年候选做确定性组合选择。先在固定样本和经授权的脱敏样本上离线预演并测量可行率，再决定是否把“无法组成三条不同主题”设为线上失败条件。

**Tech Stack:** Java 17、Spring Boot、JUnit 5、Jackson、React/TypeScript、Vitest、JSON Schema、Playwright、MySQL。

---

## 一、修订后的验收口径

### 1. 硬规则采用结构化语义键，不用中文分词猜测

每条新标题必须携带以下元数据：

```java
record HeadlineMeta(
    String plannerVersion,
    String themeKey,
    String pathKey,
    String subjectKey,
    String angleKey,
    String objectKey,
    List<String> corePhraseKeys,
    List<String> selectionReasonCodes) {}
```

三年标题须满足：

1. `themeKey` 全部不同；三年产品必须有三个不同主题。
2. 同一个 `subjectKey` 最多出现一次；`pathKey` 可以在事实确实支持时重复，但后一年必须改用不同的具体对象、主题和表面主语。
3. `corePhraseKeys` 跨年不得相交；它来自封闭词表，不从标题文本临时切词。
4. 每条标题必须有 `objectKey`，不能只有“机会、限制、变化”等空泛角度。
5. 每条标题必须引用当前年的判断 ID；跨年比较可以参与选题，但不能替代当前年证据。
6. 三年共同主线只放在 `thesis`，不原样复制到每个年度标题。
7. 同一输入、同一版本反复运行必须得到字节级相同的标题和元数据。

`angle-only` 的含义需要纠正：它只表示不重复完整钱路名称，不表示可以没有对象。例如：

- 合格：`机会与限制同时出现，先用小额投入验证。`，对象是“小额投入”。
- 不合格：`机会与限制同时出现，适合小步验证。`，没有说明验证什么。

### 2. 文本重复检查只做辅助审计

不把“任意四个以上汉字 n-gram 重复”设为线上硬拦截。这种规则会误杀“有利条件”“收入条件”等正常中文，并迫使模板做无意义改写。

实现分为两层：

- 线上硬校验：比较 `themeKey`、`subjectKey`、`objectKey`、`corePhraseKeys` 和标准钱路标签。
- 离线审计告警：规范化标点后检查四字及以上重复片段，并用显式功能词表排除“有利条件”等允许项；告警进入预演报告，由人工判断，不直接阻止保存。

### 3. 失败关闭不是先验结论

原计划中的“只会极少数失败”没有数据支持，删除该断言。新版必须先完成全量预演：

- 固定完整出生输入：`R01–R12`、`H02`、`H05`、`Y27`；
- 合成边界样本：`S01–S08`；
- 若能取得并获得授权，再加入脱敏真实命盘；不得把姓名、出生时间、出生地点写进报告。

预演输出至少包括：

- 总样本数及可组成三个不同 `themeKey` 的数量、比例；
- `themeKey`、`pathKey`、`subjectKey`、`objectKey` 分布；
- 需要第二候选、第三候选才能完成规划的比例；
- 无具体对象候选数量；
- 文本审计告警数量；
- 每个失败样本的原因码和缺失候选维度。

发布闸门：15 个完整固定输入必须全部可形成三条合格主题；任何一个失败都先扩充有事实依据的候选目录，不能直接接入线上。脱敏真实样本的上线阈值须在知道样本量和分布后另行确认，不能现在虚构一个百分比。

### 4. 失败体验

如果预演通过且最终启用失败关闭，当前未接支付的链路顺序必须是：

```text
排盘与判断 → 标题规划与全文校验 → 保存报告
```

截至 2026-08-30，会员路径已接入“成功报告计次”：生成与质量校验全部完成后，系统锁定当前会员并检查当天已保存的 `ready` 报告数；只有报告最终保存成功才构成一次使用记录。生成失败或保存事务回滚都不会增加当天次数。同一会员的并发生成会串行检查，避免同时越过三份上限。

单次付费路径仍未接入报告订单，页面必须明确标注“联调中、当前不扣款”，不能因为按钮显示 `¥6.9/¥12.9` 就声称已完成收费。后续接入时按以下顺序处理：

- 先生成并通过质量校验，再创建或确认可支付的锁定内容；
- 渠道支持时先预授权，报告保存成功后捕获；渠道只支持实扣时，失败必须发起幂等退款；
- 订单号作为幂等键，且单次购买要有独立权益来源，不能误计入会员每日三份。

失败码和用户文案：

- 对外统一质量失败码：`REPORT_GENERATION_UNAVAILABLE`；具体标题规划原因仅留给内部审计，不能让付费用户承担技术细节。
- 页面提示：`本次命书暂未生成成功，未扣除费用或使用次数，请稍后再试`。
- 会员当天三份用完时返回 `MEMBER_DAILY_REPORT_LIMIT`，提示可单独购买且本次未扣费。
- 页面不得显示内部规则、英文异常、样本编号或网络异常原文；未知失败也使用安全的中文兜底文案。

---

## 二、数据契约 v3.3

原计划“结构不变”的说法不成立。v3.3 需要做向后兼容的附加字段：

```ts
type WealthHeadlineMeta = {
  plannerVersion: 'wealth-headline-v1'
  themeKey: string
  pathKey: string
  subjectKey: string
  angleKey: string
  objectKey: string
  corePhraseKeys: string[]
  selectionReasonCodes: string[]
}

type WealthYear = {
  // 原字段不变
  headlineMeta?: WealthHeadlineMeta
}

type WealthNarrativeV3 = {
  // 原字段不变
  headlinePlannerVersion?: 'wealth-headline-v1'
}
```

兼容规则：

- 历史 `wealth-plain-v3`、`v3.1`、`v3.2` 可以没有新字段并继续读取。
- 新生成的 `wealth-plain-v3.3` 必须有根级 `headlinePlannerVersion`，每个年度必须有 `headlineMeta`。
- JSON Schema 用 `if/then` 按 `copyVersion` 条件要求字段；不能因为 TypeScript 写成可选就放过不完整的新报告。
- `contentVersion` 仍为 `wealth-narrative-v3`，因为这是兼容性附加字段，不需要重写历史快照。
- 前端、契约测试和 E2E 直接断言 `themeKey`，禁止维护“标题文本 → themeKey”的反推表。

规划器是报告级的，但每个年度 `overview.decisionIds` 仍只记录支撑该年度标题的当前年判断；`headlineMeta.plannerVersion` 与 `selectionReasonCodes` 记录为什么在整份报告中选择此候选，补足审计链路。

---

## 三、确定性选题规则

### 1. 候选必须来自许可目录

新增 `WealthHeadlineVocabulary`，每个候选模板显式声明：

- `themeKey`
- `pathKey`
- `subjectKey`
- `angleKey`
- `objectKey`
- `corePhraseKeys`
- 适用的 stance、strength、年度支持/限制或比较变化条件
- 文案模板
- 稳定的 `catalogOrder`

禁止随机数、同义句数组、按“第一年/第二年/第三年”轮换措辞。

### 2. 不再使用未经校准的 +6/+4

原计划的加权魔法数删除，改用可解释的字典序比较。单年候选排序键依次为：

1. 是否满足全部事实和引用硬条件；不满足直接淘汰。
2. 是否有当年 annual 事实直接落根。
3. 是否包含真实的年度变化及变化绝对值。
4. 当年支持与限制证据的绝对总量。
5. 是否含具体对象；没有对象直接淘汰。
6. `catalogOrder`。
7. `themeKey`、`subjectKey`、证据签名的字典序。

报告级组合排序键依次为：

1. 不同 `themeKey` 数量，三年产品必须等于 3。
2. 不同 `subjectKey` 数量。
3. 不同 `objectKey` 数量。
4. 含直接 annual 事实的标题数量。
5. 总证据显著度。
6. 三年候选稳定签名的字典序。

实现必须用有序集合或显式排序，不能依赖 `HashMap` 遍历顺序。加入并列夹具和重复执行 100 次测试，保证 tie-breaker 可复现。

### 3. 示例目标

三年共同结论可以继续放在总览：

> 2026—2028 年，靠能力赚钱是共同主线。

年度标题示例必须有不同主题、不同对象：

1. 2026：先控制投入，别提前花掉尚未到账的钱。
2. 2027：机会与限制同时出现，先用小额投入验证。
3. 2028：合作进账出现机会，但暂时不够稳定。

示例只是文案方向，最终是否可用仍由对应年度的实际 evidence 和 decision 决定。

---

### Task 1: 锁定结构化验收语义

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthHeadlineVocabulary.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthHeadlineVocabularyTest.java`
- Create: `apps/server/src/test/resources/report/wealth-headline-planner-cases.json`
- Read only: `apps/server/src/test/resources/report/wealth-v2-baseline-inputs.json`

**Step 1: 先写失败测试**

覆盖：封闭 key 不为空、每个候选有具体 `objectKey`、`corePhraseKeys` 来自封闭枚举、模板包含许可对象、禁止词不出现。

`wealth-headline-planner-cases.json` 明确包含：

- 三年可形成不同主题的正常夹具；
- 两候选并列的确定性夹具；
- 只能形成两个主题的失败夹具；
- 同一 subject、不同 object 的合法与非法夹具。

不修改 `wealth-v2-baseline-inputs.json`，它只是已有测试语料。

**Step 2: 运行 RED**

```bash
cd apps/server
./mvnw -q -Dtest=WealthHeadlineVocabularyTest test
```

Expected: FAIL，原因是词表类尚不存在；不能接受 JSON 解析错误。

**Step 3: 实现最小许可目录并转绿**

```bash
./mvnw -q -Dtest=WealthHeadlineVocabularyTest test
```

Expected: PASS。

**Step 4: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthHeadlineVocabulary.java \
  apps/server/src/test/java/com/bazi/app/report/wealth/WealthHeadlineVocabularyTest.java \
  apps/server/src/test/resources/report/wealth-headline-planner-cases.json
git commit -m "test: define wealth headline semantic vocabulary"
```

---

### Task 2: 实现未接线的确定性规划器

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthAnnualHeadlinePlanner.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthHeadlinePlanningException.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthAnnualHeadlinePlannerTest.java`

**Step 1: 写规划器失败测试**

规划器一次接收 `WealthAnnualComparator.comparePeriod(...)` 的完整结果。断言：

- 年份数量与标题数量一致；
- 三年 `themeKey` 不同；
- 标题都有 `pathKey`、`objectKey` 和当前年 `decisionIds`；
- 完整 subject label 最多一次，而不是恰好一次；
- 输入相同连续执行 100 次，序列完全相同；
- 并列夹具得到固定的 key 顺序；
- 无合法组合抛 `WealthHeadlinePlanningException(INSUFFICIENT_DIVERSITY)`。

正确断言示例：

```java
assertTrue(headlines.stream()
    .filter(h -> h.text().contains("项目和额外收入"))
    .count() <= 1);
```

**Step 2: 运行 RED**

```bash
./mvnw -q -Dtest=WealthAnnualHeadlinePlannerTest test
```

Expected: FAIL，规划器和领域异常尚不存在。

**Step 3: 实现候选生成、组合枚举和字典序 tie-breaker**

规划器此时不接入 `WealthNarrativeWriter`，只供测试和预演使用。不得使用 `IllegalArgumentException` 表示业务失败。

**Step 4: 运行转绿并提交**

```bash
./mvnw -q -Dtest=WealthAnnualHeadlinePlannerTest test
git add apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthAnnualHeadlinePlanner.java \
  apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthHeadlinePlanningException.java \
  apps/server/src/test/java/com/bazi/app/report/wealth/WealthAnnualHeadlinePlannerTest.java
git commit -m "feat: add deterministic wealth headline planner"
```

---

### Task 3: 全量预演并形成发布闸门

**Files:**
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthHeadlinePlannerPreflightTest.java`
- Create: `docs/samples/wealth-headline-planner-preflight-2026-08-30.md`
- Read only: `apps/server/src/test/resources/report/wealth-v2-baseline-inputs.json`

**Step 1: 编写预演测试**

复用现有夹具加载器，对 15 个完整输入和 8 个合成边界逐一执行现有分析器、比较器和新规划器。测试日志只输出 case ID、结构化 key、候选数和原因码，不输出姓名、出生信息或正文。

若有经授权的脱敏真实样本，通过测试资源目录外的显式路径单独运行，不提交原数据，只把聚合统计写进报告。

**Step 2: 运行预演**

```bash
cd apps/server
./mvnw -Dtest=WealthHeadlinePlannerPreflightTest test
```

Expected: 命令完成并打印每个 case 的结构化结果；预演本身不得把失败样本伪装成 PASS。

**Step 3: 记录测量报告**

在 Markdown 中记录样本范围、可行率、key 分布、降序候选使用率、失败原因和文本告警。严禁写“极少数”而没有分子、分母。

**Step 4: 人工发布检查点**

- 15 个完整输入任一失败：停止接线，回到 Task 1 扩充有依据的对象/主题候选，再重跑。
- 全部通过：才允许进入 Task 4。
- 真实样本未取得：明确写“尚未测量”，不能用固定夹具结论外推线上失败率。

**Step 5: Commit**

```bash
git add apps/server/src/test/java/com/bazi/app/report/wealth/WealthHeadlinePlannerPreflightTest.java \
  docs/samples/wealth-headline-planner-preflight-2026-08-30.md
git commit -m "test: measure wealth headline planner coverage"
```

---

### Task 3A: 禁止为了去重降低标题质量层级

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthHeadlineVocabulary.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthAnnualHeadlinePlanner.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthAnnualHeadlinePlannerTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthHeadlinePlannerPreflightTest.java`
- Modify: `docs/samples/wealth-headline-planner-preflight-2026-08-30.md`

**验收规则：**

1. 候选按“当年直接依据 → 年度变化 → 表达强度 → 证据显著度”形成 `qualityRank`。
2. 报告级组合先最小化最差 `qualityRank` 和总质量降级，再比较多样性。
3. 同一路径、同一 stance 增加三个不同具体对象角度；它们引用同一判断，不改事实与权重。
4. 15 个完整固定输入的 `diversityDowngrade` 必须为 0。
5. 缺少对象和四至八字重复片段告警必须为 0。

**验证：**

```bash
cd apps/server
./mvnw -q -Dtest=WealthHeadlineVocabularyTest,WealthAnnualHeadlinePlannerTest,WealthHeadlinePlannerPreflightTest test
```

Expected: 15/15 完整固定输入可规划，所有已选标题 `qualityRank = 1`。

---

### Task 4: 扩展 v3.3 输出契约

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthNarrativeV3.java`
- Modify: `contracts/drafts/wealth-v3.ts`
- Modify: `contracts/drafts/wealth-v3.schema.json`
- Modify: `contracts/drafts/wealth-v3.examples.json`
- Modify: `contracts/tests/wealth-v3-contract.test.cjs`
- Modify: `apps/web/src/services/reportApi.ts`
- Modify: `apps/web/src/services/reportApi.test.ts`

**Step 1: 先写契约失败测试**

断言：

- v3.2 无元数据仍合法；
- v3.3 缺根级 planner version 时非法；
- v3.3 任一年度缺 `headlineMeta` 时非法；
- v3.3 完整元数据合法。

**Step 2: 增加兼容字段和条件 Schema**

Java 反序列化历史快照时允许字段为空；新写入逻辑在 Task 5 保证 v3.3 必填。

**Step 3: 运行契约测试**

```bash
cd contracts
node --test tests/*.test.cjs
cd ../apps/server
./mvnw -q -Dtest=SavedReportIntegrationTest test
cd ../web
npm test -- --run src/services/reportApi.test.ts
```

Expected: 历史和 v3.3 契约均通过。

**Step 4: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthNarrativeV3.java \
  contracts/drafts contracts/tests/wealth-v3-contract.test.cjs \
  apps/web/src/services/reportApi.ts apps/web/src/services/reportApi.test.ts
git commit -m "feat: expose wealth headline planning metadata"
```

---

### Task 5: 接入正文生成器并升级文案版本

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthNarrativeWriter.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthPlainCopyV3.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthNarrativeSampleTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthNarrativeV3Test.java`

**Step 1: 写接线失败测试**

覆盖：

- `COPY_VERSION` 为 `wealth-plain-v3.3`；
- 每个年度输出 `headlineMeta`；
- `themeKey` 三年不同；
- 每个 overview 的 `decisionIds` 属于当前年；
- 原 facts、evidence、decisions、focus、risk 与接线前固定结果一致；
- “项目和额外收入”计数 `<= 1`。

**Step 2: 在年度循环前规划**

一次生成完整 `headlinePlan`，年度循环只按年份取已选标题。删除或停止使用 `annualOverview(WealthAssessment)`、`annualSignal(...)` 和负责标题拼接的 `annualSignalText(...)`，避免保留两套选题逻辑。

**Step 3: 输出追溯元数据**

根级写 `wealth-headline-v1`；年度写结构化 keys 和 `selectionReasonCodes`。共同主线仍在 `thesis`，年度 overview 只引用当年判断。

**Step 4: 运行测试并提交**

```bash
cd apps/server
./mvnw -q -Dtest=WealthAnnualHeadlinePlannerTest,WealthNarrativeSampleTest,WealthNarrativeV3Test test
git add apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthNarrativeWriter.java \
  apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthPlainCopyV3.java \
  apps/server/src/test/java/com/bazi/app/report/wealth/WealthNarrativeSampleTest.java \
  apps/server/src/test/java/com/bazi/app/report/wealth/WealthNarrativeV3Test.java
git commit -m "refactor: compose wealth headlines at report scope"
```

---

### Task 6: 质量闸门与领域错误链路

**状态（2026-08-30）：会员权益安全链路已完成；单次付费订单链路待独立任务接入。**

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/service/ReportEntitlementService.java`
- Modify: `apps/server/src/main/java/com/bazi/app/mapper/UserMapper.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthNarrativeWriter.java`
- Modify: `apps/server/src/main/java/com/bazi/app/service/ReportService.java`
- Create: `apps/server/src/test/java/com/bazi/app/ReportEntitlementIntegrationTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/WealthContentFailureIntegrationTest.java`
- Modify: `apps/web/src/services/reportApi.ts`
- Modify: `apps/web/src/services/reportApi.test.ts`
- Modify: `apps/web/src/pages/ReportPage.tsx`
- Modify: `apps/web/src/pages/ReportPage.test.tsx`

**Step 1: 写会员次数和 API 失败测试**

断言会员当天前三份成功、第四份被拦截；模拟付费会员生成失败并断言：

- API 返回稳定的用户级错误码和中文文案；
- `bazi_report` 没有新增记录；
- 当天次数没有增加；
- 前端不显示内部规划异常或网络错误原文。

**Step 2: 在成功保存点核销会员次数**

以当天已经成功保存的 `ready` 报告作为会员使用记录。生成完成后、写入报告前锁定用户行并检查三份上限，报告写入与检查处于同一事务；任何异常都会整体回滚。

**Step 3: 隐藏技术错误**

后端把财富内容生成异常映射为 `REPORT_GENERATION_UNAVAILABLE`。前端保留业务错误码用于选择文案，但所有未知错误统一转换为安全中文提示。

**Step 4: 运行测试并提交**

```bash
cd apps/server
./mvnw -q -Dtest=WealthContentFailureIntegrationTest,WealthNarrativeV3Test test
cd ../web
npm test -- --run src/pages/ReportPage.test.tsx
git add apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthNarrativeWriter.java \
  apps/server/src/main/java/com/bazi/app/service/ReportService.java \
  apps/server/src/test/java/com/bazi/app/WealthContentFailureIntegrationTest.java \
  apps/web/src/pages/ReportPage.tsx apps/web/src/pages/ReportPage.test.tsx
git commit -m "feat: reject unqualified wealth headline plans"
```

---

### Task 7: 增加无隐私的可观测性

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthHeadlinePlanAudit.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthHeadlinePlanAuditTest.java`
- Create: `docs/design/wealth-headline-observability.md`

**Step 1: 定义结构化事件**

成功事件 `wealth_headline_plan_success` 只包含：planner version、theme/path/subject/object keys、候选数量、是否使用降序候选。

失败事件 `wealth_headline_plan_failure` 只包含：planner version、原因码、每年候选数量、冲突 key。禁止姓名、生日、地点、四柱、正文和 decision 文本。

**Step 2: 测试字段白名单**

断言事件序列化结果只含许可字段，失败也不会泄露用户输入。

**Step 3: 记录统计方法**

当前项目没有现成指标平台，不增加进程内假计数器。先使用结构化日志统计失败率；已保存 v3.3 报告可从 JSON 中统计 `themeKey` 分布。文档给出查询口径、分母定义和报警建议，等部署平台确定后再接 Micrometer/Prometheus。

**Step 4: 运行测试并提交**

```bash
cd apps/server
./mvnw -q -Dtest=WealthHeadlinePlanAuditTest test
git add apps/server/src/main/java/com/bazi/app/report/wealth/v3/WealthHeadlinePlanAudit.java \
  apps/server/src/test/java/com/bazi/app/report/wealth/WealthHeadlinePlanAuditTest.java \
  docs/design/wealth-headline-observability.md
git commit -m "feat: audit wealth headline planning outcomes"
```

---

### Task 8: 前端、契约与 E2E 验收

**Files:**
- Modify: `apps/web/src/pages/ReportReaderPage.test.tsx`
- Modify: `e2e/run_wealth_v3_e2e.py`
- Modify: `docs/design/wealth-v3-copy-license.md`
- Modify: `docs/design/wealth-v3-expression-contract.md`

**Step 1: 更新前端报告测试**

历史 v3.2 继续读取；v3.3 三年标题照常显示。前端不从标题文案推断主题。

**Step 2: E2E 直接断言元数据**

```python
years = report["content"]["years"]
theme_keys = [year["headlineMeta"]["themeKey"] for year in years]
check("R12-年度主题不同", len(set(theme_keys)) == len(theme_keys))
check("R12-项目和额外收入最多一次",
      sum("项目和额外收入" in year["overview"]["text"] for year in years) <= 1)
```

删除 `normalized_theme_keys(headlines)` 一类文本反推逻辑。

**Step 3: 完整回归**

```bash
cd apps/server && ./mvnw -q test
cd ../web && npm test -- --run && npm run lint && npm run build
cd ../../contracts && node --test tests/*.test.cjs
cd .. && python3 e2e/run_wealth_v3_e2e.py
```

Expected: 后端、前端、契约和 E2E 全部通过；浏览器控制台无错误。

**Step 4: 人工语言验收**

逐字阅读至少 `R04`、`R08`、`R12`，出现以下任一项即不交付：

- 三年只是同义词轮换；
- 标题没有对象；
- 标题与正文或证据不一致；
- 为了不同而突出证据很弱的路径；
- 中文语法不顺；
- 共同主线在三个年度重复出现。

**Step 5: 更新文档并提交**

```bash
git add apps/web/src/pages/ReportReaderPage.test.tsx e2e/run_wealth_v3_e2e.py \
  docs/design/wealth-v3-copy-license.md docs/design/wealth-v3-expression-contract.md
git commit -m "test: verify wealth headline semantics end to end"
```

---

## 四、最终影响评估

### 后端

中等改动。新增报告级规划器、结构化词表、领域异常和审计事件；修改正文生成顺序与快照模型。不改排盘、事实提取、证据权重、五路径判断或年度比较。

### 前端

小到中等改动。阅读布局基本不变，但类型契约、失败提示和测试要更新。新元数据用于验证和审计，不直接展示给用户。

### 数据与历史报告

不迁移、不重写历史报告。v3.3 是兼容性字段扩展；历史 v3.0–v3.2 继续读取，新报告必须满足 v3.3 条件契约。

### 业务风险

真实失败率目前未知，必须用 Task 3 测量。若固定完整样本不能 100% 通过，说明候选主题维度仍不足，不能把失败转嫁给付费用户。

### 不在本次范围

- 不引入 LLM 或随机文案；
- 不修改命理计算和权重；
- 不处理事业、感情主题；
- 不重写历史报告；
- 已具备会员每日三份的成功计次与并发保护；尚不具备单次报告订单、真实扣款和自动退款，必须作为独立业务闭环实施。

## 五、实施顺序和停止条件

```text
Task 1 语义词表
  → Task 2 未接线规划器
  → Task 3 全量预演
      ├─ 固定完整样本有失败：停止接线，扩充候选后重测
      └─ 固定完整样本全部通过：Task 3A 验证无质量降级
          ├─ diversityDowngrade > 0：继续扩充同质量对象角度
          └─ diversityDowngrade = 0：进入 Task 4–8
```

Task 3 是强制检查点，不允许为了赶进度跳过。计划完成的标准不是“测试不红”，而是：可行率有真实分子/分母、标题差异可追溯、确定性有测试保障、失败链路对用户和业务账务都没有模糊地带。
