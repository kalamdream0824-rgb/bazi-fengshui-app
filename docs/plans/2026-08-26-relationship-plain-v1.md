# Relationship Plain v1 Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** Build a saved, plain-language relationship report for single, dating, and married users, covering the current and following two calendar years with the same chart evidence but genuinely different questions, judgments, and actions for each relationship status.

**Architecture:** Add a dedicated relationship pipeline beside wealth v2: chart facts → relationship evidence → five dimension evaluations → yearly/period arbitration → status-specific narrative → saved report reader. Relationship status enters only the narrative planner; it must never enter evidence extraction, scoring, ranking, risk selection, or annual trend calculation. The product remains fixed at three years while the calculation layer reuses the existing internal 2–5 year horizon.

**Tech Stack:** Java 17, Spring Boot 3.5, lunar-java, MyBatis-Plus, Jackson, JUnit 5/MockMvc, React 19, TypeScript 6, Vite 8, Vitest/Testing Library, OpenAPI YAML.

---

## Confirmed product decisions

- Ship only the relationship plain edition first. Keep the professional edition visible but disabled; do not silently fall back to plain content.
- Keep one required selector with exactly three values:
  - `single`: 单身或尚未确定关系
  - `dating`: 已确认交往关系
  - `married`: 已婚或长期共同生活
- Do not add more questionnaire fields. Relationship status is a reading context, not chart evidence.
- Evaluate all five dimensions every year:
  - `connection`: 关系连接——是否更容易注意、认识或靠近某个人
  - `response`: 回应与表达——话能不能说清，彼此能不能接住
  - `daily_cooperation`: 日常配合——时间、安排和生活节奏能不能配合
  - `boundaries`: 矛盾与边界——分歧是否容易升级，界限能不能守住
  - `stability`: 长期稳定——关系能否落到持续、具体的共同安排
- The three branches must answer different real questions:
  - single: 谁值得继续了解，什么时候不必急着确定关系
  - dating: 这段关系是否适合继续发展，分歧该怎么处理
  - married: 如何减少消耗，把共同生活和重要决定安排好
- Produce exactly three consecutive calendar years. Do not expose a year selector yet.
- Save generated reports and allow repeated reading from “我的命书”. Do not add PDF export.
- Do not add an LLM, payment changes, membership changes, exact months, fake probabilities, or deterministic event claims.

## Non-negotiable quality guardrails

1. The fact extractor and evaluator must not accept `RelationshipStatus` as an argument. This makes it structurally difficult for status to affect the result.
2. For one chart and period, changing only status must preserve dimension evidence, weights, rankings, tie flags, risk selection, and annual trend. Only narrative text and status-oriented actions may differ.
3. Do not force a negative result. `mainRisk` and each year's `mainLimit` are optional and appear only when a real negative evidence item exists.
4. Do not output “现有证据不足”“先观察再判断”“现在卡住了”“卡点”“正缘”“烂桃花”“第三者”“必定结婚”“一定分手”“不忠”.
5. Do not use a noun-replacement template. Each status must have independently authored thesis, judgment, signal, action, and transition copy.
6. A saved report must fail validation before persistence if it has no three years, no primary dimension evidence, repeated actions, unresolved template tokens, or a claim without an evidence key.
7. Scores express rule prominence and direction, not statistical probability and not guaranteed fate. Document that limitation in the design note and API description.

## Calculation model

### Evidence families

Use four evidence families so the reader can distinguish long-term structure from a single annual signal:

```java
public enum RelationshipEvidenceFamily {
  NATAL_STRUCTURE,
  SPOUSE_PALACE,
  ANNUAL_TRIGGER,
  DAYUN_CONTEXT
}
```

Every `RelationshipEvidence` contains a stable key, family, plain internal label, target dimension, signed weight, and source fact keys. A positive weight is supportive; a negative weight is limiting. Never render the numeric weight to the user.

### Initial v1 mapping

The initial mapping must be explicit and regression-tested. It is an auditable product rule set, not a scientific accuracy claim.

| Evidence | Dimension effect |
| --- | --- |
| natal visible spouse-star group | connection `+3`, stability `+1` |
| natal hidden spouse-star group | connection `+1`, stability `+1` |
| natal output group | response visible `+2`, hidden `+1` |
| natal resource group | response `+1`, daily cooperation `+1` |
| annual spouse-star group | connection `+4`, response `+1` |
| annual output group | response `+4`, connection `+1` |
| annual resource group | response `+2`, daily cooperation `+2` |
| annual peer group | boundaries prominence `+2`; wording is about equality and personal space, never conflict by default |
| annual non-spouse wealth/authority group | stability prominence `+2`; wording is about concrete arrangements and responsibility |
| annual branch harmony with day branch | daily cooperation `+4`, stability `+2`, response `+1` |
| annual branch harmony with another natal pillar | daily cooperation `+2`, connection `+1` |
| annual branch clash with day branch | boundaries `-4`, stability `-3`, daily cooperation `-1` |
| annual branch harm with day branch | boundaries `-3`, stability `-2` |
| annual branch punishment with day branch | boundaries `-3`, response `-1`, stability `-2` |
| disruptive relation with another natal pillar | boundaries `-2`, daily cooperation `-1` |
| active Dayun spouse-star group | connection `+2`, stability `+1` |
| active Dayun output/resource group | response or daily cooperation `+1` |
| annual disruptive relation with active Dayun | boundaries `-2`, stability `-2` |

For male charts the spouse-star group is wealth; for female charts it is authority. Treat spouse-star only as relationship attention/activation. It cannot support marriage, breakup, fidelity, or a particular person's identity.

### Dimension evaluation and arbitration

For every dimension calculate:

```text
supportWeight     = sum of positive evidence weights
limitationWeight  = sum of absolute values of negative evidence weights
prominenceWeight  = supportWeight + limitationWeight
netWeight         = supportWeight - limitationWeight
```

- Primary and secondary dimensions are ranked by `prominenceWeight`, not `netWeight`. This prevents a strong conflict signal from disappearing merely because it is negative.
- Ties use the stable enum order: connection, response, daily cooperation, boundaries, stability. Preserve a `tied=true` flag so copy does not pretend the lead is decisive.
- `mainRisk` is the dimension with the highest `limitationWeight`, but is absent when every limitation weight is zero.
- Tone is `SUPPORTIVE`, `MIXED`, `PRESSURED`, or `QUIET`, derived from support and limitation weights. `QUIET` must be phrased as “这一项今年不是重点”, not as missing evidence.
- Evidence with the same stable key may affect several dimensions, but the same key may only count once per dimension.
- Period arbitration uses the three annual evaluations and natal profile. It must not rescore narrative words or relationship status.

### Data flow boundary

```text
PaipanRequest + PaipanResultDto
             ↓
RelationshipFactExtractor(request, chart, horizon)
             ↓
RelationshipYearFacts[]
             ↓
RelationshipDimensionEvaluator
             ↓
RelationshipPeriodArbitrator
             ↓                         RelationshipStatus
RelationshipPeriodEvaluation ───────────────┬──────────────
                                             ↓
                              RelationshipNarrativePlanner
                                             ↓
                              RelationshipNarrativePlan
```

The arrow from `RelationshipStatus` must terminate at the narrative planner only.

---

### Task 1: Add the relationship status contract and validation

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipStatus.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/dto/RelationshipContextRequest.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipStatusTest.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/dto/ReportPreviewRequest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/SavedReportIntegrationTest.java`

**Step 1: Write failing enum and request tests**

Cover exact code parsing, blank status, unknown status, and Chinese labels:

```java
@ParameterizedTest
@CsvSource({
  "single,单身或尚未确定关系",
  "dating,已确认交往关系",
  "married,已婚或长期共同生活"
})
void parsesSupportedStatus(String code, String label) {
  RelationshipStatus status = RelationshipStatus.fromCode(code);
  assertEquals(label, status.label());
}

@Test
void rejectsUnknownStatus() {
  assertThrows(IllegalArgumentException.class,
      () -> RelationshipStatus.fromCode("ambiguous"));
}
```

Add integration requests proving relationship generation rejects absent and unknown status with stable application error codes. They will continue failing until Task 6 wires the service.

**Step 2: Run the focused tests and verify failure**

```bash
cd apps/server
./mvnw -Dtest=RelationshipStatusTest,SavedReportIntegrationTest test
```

Expected: FAIL because relationship context is not part of the request contract.

**Step 3: Implement the minimal domain and DTO**

```java
public enum RelationshipStatus {
  SINGLE("single", "单身或尚未确定关系"),
  DATING("dating", "已确认交往关系"),
  MARRIED("married", "已婚或长期共同生活");

  public static RelationshipStatus fromCode(String code) {
    return Arrays.stream(values())
        .filter(value -> value.code.equals(code))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unsupported relationship status"));
  }
}
```

`RelationshipContextRequest` contains only `@NotBlank String status` and exposes `toDomain()`. Add `relationshipContext` to `ReportPreviewRequest` without changing the deprecated wealth context field.

**Step 4: Run the domain test**

```bash
cd apps/server
./mvnw -Dtest=RelationshipStatusTest test
```

Expected: PASS. The integration tests may remain red until Task 6; record that dependency in the commit message rather than weakening them.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipStatus.java \
  apps/server/src/main/java/com/bazi/app/report/dto/RelationshipContextRequest.java \
  apps/server/src/main/java/com/bazi/app/report/dto/ReportPreviewRequest.java \
  apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipStatusTest.java \
  apps/server/src/test/java/com/bazi/app/report/SavedReportIntegrationTest.java
git commit -m "test: define relationship report context"
```

---

### Task 2: Extract dedicated relationship facts and evidence

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipDimension.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipEvidenceFamily.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipEvidence.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipNatalProfile.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipYearFacts.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipFactExtractor.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipFactExtractorTest.java`

**Step 1: Write fixed-chart extractor tests**

Use the same deterministic chart fixtures as wealth tests. Assert:

- male charts select wealth as the spouse-star group;
- female charts select authority as the spouse-star group;
- visible and hidden occurrences have different stable keys and weights;
- day-branch harmony/clash/harm/punishment becomes spouse-palace evidence;
- other-pillar relations remain distinguishable from day-pillar relations;
- exactly three consecutive years are extracted with `ReportHorizon.RELATIONSHIP_PRODUCT`;
- no extractor constructor or method accepts `RelationshipStatus`.

**Step 2: Run and verify failure**

```bash
cd apps/server
./mvnw -Dtest=RelationshipFactExtractorTest test
```

Expected: FAIL because the dedicated model does not exist.

**Step 3: Implement the evidence records and extractor**

Add `public static final ReportHorizon RELATIONSHIP_PRODUCT = ReportHorizon.of(3);` to `ReportHorizon` if it is not already present. Reuse `AnnualContextFactory` facts and chart Ten God occurrences; do not call legacy `RelationshipAnnualRules` as a black box.

```java
public record RelationshipEvidence(
    String key,
    RelationshipEvidenceFamily family,
    RelationshipDimension dimension,
    int weight,
    List<String> sourceFactKeys) {
  public RelationshipEvidence {
    if (weight == 0) throw new IllegalArgumentException("weight must not be zero");
    sourceFactKeys = List.copyOf(sourceFactKeys);
  }
}
```

Sort evidence by stable key before returning it. Keep internal labels plain and factual, such as “流年与日支相冲”; do not embed consumer conclusions in the extractor.

**Step 4: Run focused tests**

```bash
cd apps/server
./mvnw -Dtest=RelationshipFactExtractorTest,AnnualContextFactoryTest test
```

Expected: PASS with existing annual context behavior unchanged.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/ReportHorizon.java \
  apps/server/src/main/java/com/bazi/app/report/relationship \
  apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipFactExtractorTest.java
git commit -m "feat: extract relationship report evidence"
```

---

### Task 3: Evaluate all five relationship dimensions

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipTone.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipDimensionEvaluation.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipYearEvaluation.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipDimensionEvaluator.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipDimensionEvaluatorTest.java`

**Step 1: Write evaluator tests before implementation**

Test the table in “Initial v1 mapping” one row at a time. Also cover:

```java
@Test
void ranksByProminenceInsteadOfNetWeight() {
  // A strong -4 boundary signal is more prominent than a +2 response signal.
}

@Test
void deduplicatesTheSameEvidenceKeyWithinOneDimension() {
  // One source key must not be counted twice by two extraction paths.
}

@Test
void exposesAllFiveDimensionsEvenWhenSomeAreQuiet() {
  assertEquals(Set.copyOf(List.of(RelationshipDimension.values())), result.keySet());
}
```

**Step 2: Run and verify failure**

```bash
cd apps/server
./mvnw -Dtest=RelationshipDimensionEvaluatorTest test
```

Expected: FAIL because evaluation types do not exist.

**Step 3: Implement signed evaluation**

`RelationshipDimensionEvaluation` owns `supportWeight`, `limitationWeight`, `prominenceWeight`, `netWeight`, `tone`, and immutable evidence. Derive tone as follows:

```java
if (support == 0 && limitation == 0) return QUIET;
if (limitation == 0) return SUPPORTIVE;
if (support == 0 || limitation > support) return PRESSURED;
return MIXED;
```

Do not create text here. The evaluator returns calculation data only.

**Step 4: Run focused tests**

```bash
cd apps/server
./mvnw -Dtest=RelationshipDimensionEvaluatorTest,RelationshipFactExtractorTest test
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/relationship \
  apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipDimensionEvaluatorTest.java
git commit -m "feat: evaluate relationship dimensions"
```

---

### Task 4: Arbitrate annual and three-year relationship priorities

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipFocus.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipRisk.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipPeriodEvaluation.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipPeriodArbitrator.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipPeriodArbitratorTest.java`

**Step 1: Write arbitration tests**

Cover:

- primary and secondary use prominence weight;
- stable enum order resolves a tie and sets `tied=true`;
- the risk with highest limitation weight is selected;
- no risk is returned when all limitation weights are zero;
- three years produce two adjacent transitions;
- technical horizons of two through five are accepted;
- the arbitrator API has no relationship-status argument.

**Step 2: Run and verify failure**

```bash
cd apps/server
./mvnw -Dtest=RelationshipPeriodArbitratorTest test
```

Expected: FAIL because arbitration types do not exist.

**Step 3: Implement deterministic arbitration**

Return annual and period-level primary/secondary dimensions, optional risk, tie flags, evidence keys, and transition direction. A transition may be `EASING`, `STEADY`, or `TIGHTENING`, based on adjacent limitation and support totals. Do not infer a breakup or commitment event from the direction.

Add validation that primary and secondary differ and every selected result carries at least one evidence key.

**Step 4: Run focused tests**

```bash
cd apps/server
./mvnw -Dtest=RelationshipPeriodArbitratorTest,RelationshipDimensionEvaluatorTest test
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/relationship \
  apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipPeriodArbitratorTest.java
git commit -m "feat: arbitrate relationship report priorities"
```

---

### Task 5: Author three independent plain-language narratives

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipNarrativePlan.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipPlainCopy.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/relationship/RelationshipNarrativePlanner.java`
- Create: `apps/server/src/main/resources/report-copy/relationship-plain-v1.yml`
- Create: `apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipNarrativePlannerTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/ReportReaderLanguageTest.java`

**Step 1: Write narrative contract tests**

For the same `RelationshipPeriodEvaluation`, generate all three statuses and assert:

- calculation fields and evidence keys are equal;
- thesis, annual judgments, reality signals, and actions differ across statuses;
- each year has one judgment, exactly two reality signals, exactly two actions, and one transition;
- optional risk/limit text is absent when risk evidence is absent;
- sentences contain an explicit subject and object where needed;
- no banned phrase or unresolved `${token}` appears;
- no sentence exceeds the existing plain-language limit unless it is split at Chinese punctuation.

Add semantic fixture assertions, not just snapshots:

```java
assertThat(single.firstYear().actions()).anyMatch(text -> text.contains("继续了解"));
assertThat(dating.firstYear().actions()).anyMatch(text -> text.contains("两个人"));
assertThat(married.firstYear().actions()).anyMatch(text -> text.contains("共同生活"));
```

**Step 2: Run and verify failure**

```bash
cd apps/server
./mvnw -Dtest=RelationshipNarrativePlannerTest,ReportReaderLanguageTest test
```

Expected: FAIL because the planner and copy catalog do not exist.

**Step 3: Implement a structured narrative plan**

The persisted plan must contain at least:

```java
public record RelationshipNarrativePlan(
    String relationshipStatus,
    int horizonYears,
    String thesis,
    String summary,
    List<RelationshipDimensionSummary> dimensions,
    String primaryDimensionCode,
    String secondaryDimensionCode,
    RelationshipRiskSummary mainRisk,
    List<RelationshipYearNarrative> years,
    List<String> evidenceKeys) implements ReportContent {}
```

`mainRisk` and each annual `mainLimit` are nullable/optional by design. `RelationshipPlainCopy` resolves explicit keys beginning with `single.`, `dating.`, or `married.`. Do not construct one sentence by substituting “对象/伴侣/配偶”.

Follow the established “结论 → 你会看到什么 → 可以怎么做” style. Examples of acceptable distinctions:

- single: “先看对方是否愿意稳定回应，再决定要不要继续投入时间。”
- dating: “先把联系频率和见面安排说清楚，再判断两个人是否适合继续推进。”
- married: “先把家务、时间和重要开支重新分配，减少同一件事反复争执。”

**Step 4: Run focused tests**

```bash
cd apps/server
./mvnw -Dtest=RelationshipNarrativePlannerTest,ReportReaderLanguageTest test
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/relationship \
  apps/server/src/main/resources/report-copy/relationship-plain-v1.yml \
  apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipNarrativePlannerTest.java \
  apps/server/src/test/java/com/bazi/app/report/ReportReaderLanguageTest.java
git commit -m "feat: write relationship status narratives"
```

---

### Task 6: Wire generation, persistence, versioned reading, and server validation

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/ReportService.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/dto/ReportPreviewRequest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/SavedReportIntegrationTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/ReportContentCompatibilityTest.java`

**Step 1: Extend integration tests before service code**

Add tests for:

- create and read one `relationship-narrative-v1` report for each status;
- saved context contains only the selected status and no fabricated questionnaire answers;
- identical chart requests with three statuses preserve primary/secondary/risk/evidence and only change narrative text;
- missing and unknown status reject with `RELATIONSHIP_CONTEXT_REQUIRED` or `RELATIONSHIP_STATUS_INVALID` and create no database row;
- relationship professional rejects with `RELATIONSHIP_PROFESSIONAL_NOT_AVAILABLE` and creates no row;
- legacy career and wealth reports still deserialize;
- quality validation rejects missing evidence, duplicate actions, unresolved tokens, and wrong year count before save.

**Step 2: Run and verify failure**

```bash
cd apps/server
./mvnw -Dtest=SavedReportIntegrationTest,ReportContentCompatibilityTest test
```

Expected: relationship tests FAIL while existing career/wealth cases stay green.

**Step 3: Add the relationship generation branch**

In `ReportService`:

- add `RELATIONSHIP_CONTENT_VERSION = "relationship-narrative-v1"`;
- accept only plain relationship edition;
- require and parse `relationshipContext`;
- call paipan once, then extractor → evaluator → arbitrator → planner;
- validate the complete narrative plan;
- serialize plan and minimal context in the same transaction as the saved report;
- route `toDto` by `contentVersion` so relationship v1 never falls through to career deserialization.

Keep career and wealth branches unchanged except for small extraction of shared dispatch code if tests demand it. Do not revive PDF preview for relationship.

**Step 4: Run server tests**

```bash
cd apps/server
./mvnw -Dtest=SavedReportIntegrationTest,ReportContentCompatibilityTest,RelationshipNarrativePlannerTest test
./mvnw test
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/ReportService.java \
  apps/server/src/main/java/com/bazi/app/report/dto/ReportPreviewRequest.java \
  apps/server/src/test/java/com/bazi/app/report/SavedReportIntegrationTest.java \
  apps/server/src/test/java/com/bazi/app/report/ReportContentCompatibilityTest.java
git commit -m "feat: save relationship plain reports"
```

---

### Task 7: Add the minimal relationship-status selector and API types

**Files:**
- Modify: `apps/web/src/lib/reportApi.ts`
- Modify: `apps/web/src/pages/ReportPage.tsx`
- Modify: `apps/web/src/pages/ReportPage.css`
- Modify: `apps/web/src/pages/ReportPage.test.tsx`
- Modify: `apps/web/src/lib/reportApi.test.ts`

**Step 1: Write failing frontend tests**

Assert:

- selecting relationship shows only the three status choices and no career questionnaire;
- “生成命书” remains disabled until one status is selected;
- plain price remains ¥6.9;
- professional remains visible and disabled as “设计中 · 暂未开放”;
- changing topic clears stale relationship context;
- request body sends `relationshipContext: { status: "dating" }` and no career context;
- a relationship response navigates to the saved report reader.

**Step 2: Run and verify failure**

```bash
cd apps/web
npm test -- src/pages/ReportPage.test.tsx src/lib/reportApi.test.ts
```

Expected: FAIL because relationship is still shown as unsupported.

**Step 3: Implement typed request options**

Add:

```ts
export type RelationshipStatus = 'single' | 'dating' | 'married'

export interface RelationshipContextInput {
  status: RelationshipStatus
}

export interface CreateReportOptions {
  careerContext?: CareerContextInput
  relationshipContext?: RelationshipContextInput
}
```

Change `createReport` to accept `CreateReportOptions` rather than adding an ambiguous fifth positional argument. Update existing career call sites. In `ReportPage`, render three large radio-card choices with one-line labels; do not add an explanatory form or extra confirmation step.

**Step 4: Run focused frontend tests**

```bash
cd apps/web
npm test -- src/pages/ReportPage.test.tsx src/lib/reportApi.test.ts
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/web/src/lib/reportApi.ts apps/web/src/lib/reportApi.test.ts \
  apps/web/src/pages/ReportPage.tsx apps/web/src/pages/ReportPage.css \
  apps/web/src/pages/ReportPage.test.tsx
git commit -m "feat: add relationship report entry"
```

---

### Task 8: Build the saved relationship reader

**Files:**
- Create: `apps/web/src/components/report/RelationshipReportReader.tsx`
- Create: `apps/web/src/components/report/RelationshipReportReader.css`
- Create: `apps/web/src/components/report/RelationshipReportReader.test.tsx`
- Modify: `apps/web/src/lib/reportApi.ts`
- Modify: `apps/web/src/pages/ReportReaderPage.tsx`
- Modify: `apps/web/src/pages/ReportReaderPage.test.tsx`
- Modify: `apps/web/src/pages/ReportLibraryPage.test.tsx`

**Step 1: Define relationship v1 response types and failing routing tests**

Add a discriminated `RelationshipV1SavedReport` and `isRelationshipV1Report`. Test that `relationship-narrative-v1` routes to the new reader while legacy career/wealth routing remains unchanged.

Reader acceptance tests must cover:

- status label and three-year thesis;
- five dimension summaries;
- primary and secondary relationship cards;
- optional main-risk card hidden when absent;
- three annual sections with judgment, two signals, two actions, and transition;
- evidence details collapsed by default;
- 430 px viewport has no horizontal overflow;
- “我的命书” displays relationship metadata and opens the correct report.

**Step 2: Run and verify failure**

```bash
cd apps/web
npm test -- src/components/report/RelationshipReportReader.test.tsx \
  src/pages/ReportReaderPage.test.tsx src/pages/ReportLibraryPage.test.tsx
```

Expected: FAIL because the type guard and reader do not exist.

**Step 3: Implement the reader using the current visual system**

Reuse the wealth reader's page width, background alignment, spacing rhythm, and saved-report header. Do not copy wealth terminology or income-path card labels. Keep the hierarchy:

1. three-year thesis;
2. five-dimension overview;
3. main relationship focus, secondary focus, optional issue;
4. one section per year;
5. collapsed calculation basis.

Use ordinary Chinese labels such as “这一年最值得关注”“你可能会遇到”“可以怎么做”. Do not show internal score numbers in the plain edition.

**Step 4: Run focused tests**

```bash
cd apps/web
npm test -- src/components/report/RelationshipReportReader.test.tsx \
  src/pages/ReportReaderPage.test.tsx src/pages/ReportLibraryPage.test.tsx
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/web/src/components/report/RelationshipReportReader.tsx \
  apps/web/src/components/report/RelationshipReportReader.css \
  apps/web/src/components/report/RelationshipReportReader.test.tsx \
  apps/web/src/lib/reportApi.ts apps/web/src/pages/ReportReaderPage.tsx \
  apps/web/src/pages/ReportReaderPage.test.tsx \
  apps/web/src/pages/ReportLibraryPage.test.tsx
git commit -m "feat: add relationship report reader"
```

---

### Task 9: Publish the contract, algorithm note, and acceptance fixtures

**Files:**
- Modify: `contracts/openapi.yaml`
- Create: `docs/design/relationship-report-v1.md`
- Create: `apps/server/src/test/resources/report-golden/relationship-v1-cases.json`
- Create: `apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipGoldenCaseTest.java`
- Modify: `docs/design/README.md`
- Modify: `README.md`

**Step 1: Write contract and golden-case tests**

Create at least six fixed cases: male/female charts crossed with single/dating/married, including one harmony-heavy year, one day-branch disruptive year, one tie, and one no-negative-evidence case. Assert calculation invariance across status pairs and text differences across all three branches.

The golden file should store input, expected primary/secondary/risk codes, expected evidence keys, and required/forbidden text fragments. Do not snapshot the whole prose; that would make ordinary copy improvements unnecessarily expensive.

**Step 2: Run and verify failure**

```bash
cd apps/server
./mvnw -Dtest=RelationshipGoldenCaseTest test
```

Expected: FAIL until fixtures and any uncovered edge handling are complete.

**Step 3: Update OpenAPI and design documentation**

Add `RelationshipContextRequest`, `RelationshipNarrativePlan`, dimension/year/risk schemas, and include relationship v1 in the saved content `oneOf`. Document:

- the three status values;
- relationship plain-only availability;
- optional risk/limit behavior;
- scores are auditable rule weights, not probabilities;
- status changes presentation only;
- exact error codes;
- legacy report compatibility.

In `docs/design/relationship-report-v1.md`, include the evidence table, data-flow boundary, banned claims, and the rule-review checklist. Add it to `docs/design/README.md`.

**Step 4: Run tests and validate YAML**

```bash
cd apps/server
./mvnw -Dtest=RelationshipGoldenCaseTest,SavedReportIntegrationTest test
cd ../..
rg -n "relationship-narrative-v1|RelationshipContextRequest|RelationshipNarrativePlan" contracts/openapi.yaml docs/design
```

Expected: tests PASS and every public relationship type appears in OpenAPI.

**Step 5: Commit**

```bash
git add contracts/openapi.yaml docs/design/relationship-report-v1.md \
  docs/design/README.md README.md \
  apps/server/src/test/resources/report-golden/relationship-v1-cases.json \
  apps/server/src/test/java/com/bazi/app/report/relationship/RelationshipGoldenCaseTest.java
git commit -m "docs: define relationship report v1"
```

---

### Task 10: Run the complete regression and local acceptance flow

**Files:**
- Modify only if a real defect is found: `e2e/run_e2e.py`
- Modify only if a real defect is found: files already listed above

**Step 1: Run all automated tests**

```bash
cd apps/server
./mvnw test
cd ../web
npm test -- --run
npm run build
```

Expected: all server/frontend tests PASS and production build succeeds.

**Step 2: Exercise the real HTTP flow**

With MySQL, server, and Vite running, test:

```bash
python3 e2e/run_e2e.py
```

Extend the flow only if it does not yet cover: log in → enter birth data → choose relationship → choose each status → generate → open “我的命书” → reopen the saved report.

**Step 3: Perform manual content acceptance**

For the same chart, generate all three statuses and compare them side by side:

- primary/secondary/risk and evidence must match;
- single must discuss whether someone deserves more time;
- dating must discuss whether the relationship can keep developing;
- married must discuss reducing repeated friction in shared life;
- no branch may read like the wealth or career report with nouns changed;
- no unsupported risk block may appear;
- every action must name who does what and what observable result to look for.

Repeat at 430 px and desktop width. Confirm cards align with the page background and no fixed bottom navigation covers content.

**Step 4: Fix only observed defects and rerun the smallest failing test first**

Do not adjust weights merely to make one sample sound more exciting. If a judgment is wrong, trace it to fact extraction, mapping, arbitration, or copy and add a regression test at that layer before changing code.

**Step 5: Record final verification and commit**

```bash
git status --short
git log -10 --oneline
```

If Task 10 required code changes, commit them with a narrow message such as:

```bash
git add <only-the-files-changed-for-the-defect>
git commit -m "fix: complete relationship report acceptance"
```

Do not add or modify `work/`.

---

## Release gate

Relationship v1 is ready for user verification only when all of the following are true:

- backend and frontend full test suites pass;
- all three statuses generate, save, reopen, and display correctly;
- professional relationship edition is rejected by both UI and API;
- one chart produces identical calculation results across all three statuses;
- no-risk cases omit the risk block instead of inventing a warning;
- golden cases cover both genders, ties, disruptive relations, and quiet dimensions;
- Chinese-language checks and manual reading find no vague subject, broken grammar, or banned phrase;
- mobile and desktop readers align with the existing page background;
- `work/` remains untouched and outside commits.

