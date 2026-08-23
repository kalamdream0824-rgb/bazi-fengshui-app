# Wealth Plain v2 Three-Year Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** Build an independent plain-language wealth report that evaluates five wealth paths for the current and following two years, while keeping the calculation layer configurable up to five years and leaving the wealth professional edition unavailable.

**Architecture:** Replace wealth's reuse of `CareerNarrativePlan` with a dedicated evidence, path-ranking, and narrative pipeline: chart facts → five path evaluations → yearly arbitration → three-year plain-language report. Keep the public product fixed at three years and do not expose a year selector; the backend period abstraction accepts two through five years for future products. Preserve saved v1 reports by deserializing content according to `contentVersion`.

**Tech Stack:** Java 17, Spring Boot 3.5, lunar-java, MyBatis-Plus, Jackson, JUnit 5/MockMvc, React 19, TypeScript 6, Vite 8, Vitest/Testing Library, OpenAPI YAML.

---

## Scope and non-goals

- Wealth plain edition remains ¥6.9 and produces exactly three consecutive calendar years.
- The backend supports an internal horizon of 2–5 years; the request API does not expose this choice.
- Evaluate all five paths every year: stable income, skill income, project income, cooperation income, and retention.
- Automatically select a primary income path, secondary income path, and main risk. Do not ask a wealth questionnaire.
- Keep wealth professional visible but disabled as `设计中 · 暂未开放`; direct API requests must also be rejected.
- Existing `wealth-narrative-v1` and career report snapshots remain readable.
- Do not change career calculations, PDF preview behavior, pricing/payment rules, membership quotas, or add an LLM.
- Do not add exact amounts, exact months, investment products, guaranteed returns, or fake probability percentages.

## Pre-execution safety note

The current workspace contains many modified and untracked files, including the report v1 implementation. Before executing this plan, inspect `git status --short`, identify the v1 baseline changes, and place implementation in an isolated worktree only after the baseline is safely represented in Git. Do not delete, reset, overwrite, or include `work/` unless it is explicitly confirmed as project source.

---

### Task 1: Generalize the annual calculation period to 2–5 years

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportHorizon.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/ReportHorizonTest.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/AnnualContextFactory.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/AnnualContextFactoryTest.java`

**Step 1: Write the failing horizon tests**

Add tests that define the product period and technical boundary:

```java
@Test
void productWealthReportUsesThreeYears() {
  assertEquals(3, ReportHorizon.WEALTH_PRODUCT.years());
}

@ParameterizedTest
@ValueSource(ints = {2, 3, 4, 5})
void acceptsSupportedHorizons(int years) {
  assertEquals(years, ReportHorizon.of(years).years());
}

@ParameterizedTest
@ValueSource(ints = {0, 1, 6})
void rejectsUnsupportedHorizons(int years) {
  assertThrows(IllegalArgumentException.class, () -> ReportHorizon.of(years));
}
```

Extend `AnnualContextFactoryTest` with a five-year assertion for 2026–2030 and verify that every context has a Ganzhi, annual Ten God, natal analysis, and next-year fact.

**Step 2: Run the tests to verify failure**

Run:

```bash
cd apps/server
./mvnw -Dtest=ReportHorizonTest,AnnualContextFactoryTest test
```

Expected: FAIL because `ReportHorizon` and the horizon-aware factory overload do not exist.

**Step 3: Implement the bounded horizon**

Create an immutable value object:

```java
public record ReportHorizon(int years) {
  public static final int MIN_YEARS = 2;
  public static final int MAX_YEARS = 5;
  public static final ReportHorizon WEALTH_PRODUCT = new ReportHorizon(3);

  public ReportHorizon {
    if (years < MIN_YEARS || years > MAX_YEARS) {
      throw new IllegalArgumentException("report horizon must be between 2 and 5 years");
    }
  }

  public static ReportHorizon of(int years) {
    return new ReportHorizon(years);
  }
}
```

Change `AnnualContextFactory.create` to accept `ReportHorizon`. Keep the existing overload temporarily and delegate it to the current three-year default so unrelated PDF code does not change in this task:

```java
public List<AnnualContext> create(
    PaipanRequest request,
    PaipanResultDto chart,
    ReportHorizon horizon) {
  // existing validation
  for (int year = firstYear; year < firstYear + horizon.years(); year++) {
    // existing context creation
  }
}
```

The factory must calculate `next.annual.*` for the final displayed year as well, so a five-year report may inspect the sixth year's transition signal without displaying a sixth year.

**Step 4: Run focused tests**

Run the command from Step 2.

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/ReportHorizon.java \
  apps/server/src/main/java/com/bazi/app/report/AnnualContextFactory.java \
  apps/server/src/test/java/com/bazi/app/report/ReportHorizonTest.java \
  apps/server/src/test/java/com/bazi/app/report/AnnualContextFactoryTest.java
git commit -m "refactor: support bounded report horizons"
```

---

### Task 2: Replace the two/three-year assessment assumptions with a generic period

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualPeriodAssessment.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualPeriodAssessor.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/AnnualPeriodAssessmentTest.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/AnnualPeriodAssessorTest.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/ThreeYearAssessor.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/ThreeYearAssessment.java`

**Step 1: Write tests for three- and five-year sequences**

Cover these behaviors:

```java
@Test
void labelsEverySupportedPeriodInChinese() {
  assertEquals("两年", assessmentWith(2).periodLabel());
  assertEquals("三年", assessmentWith(3).periodLabel());
  assertEquals("四年", assessmentWith(4).periodLabel());
  assertEquals("五年", assessmentWith(5).periodLabel());
}

@Test
void rejectsMissingOrNonConsecutiveYears() {
  assertThrows(IllegalArgumentException.class, () -> assessmentForYears(2026, 2028, 2029));
}

@Test
void wealthProductAssessesExactlyThreeYears() {
  AnnualPeriodAssessment result = assessor.assess(
      request, chart, ReportTopic.WEALTH, ReportHorizon.WEALTH_PRODUCT);
  assertEquals(List.of(2026, 2027, 2028), result.years().stream().map(YearAssessment::year).toList());
}

@Test
void technicalFiveYearHorizonProducesFourAdjacentTransitions() {
  AnnualPeriodAssessment result = assessor.assess(
      request, chart, ReportTopic.WEALTH, ReportHorizon.of(5));
  assertEquals(5, result.years().size());
  assertEquals(4, result.transitions().size());
}
```

**Step 2: Run tests and verify failure**

```bash
cd apps/server
./mvnw -Dtest=AnnualPeriodAssessmentTest,AnnualPeriodAssessorTest test
```

Expected: FAIL because generic period types do not exist.

**Step 3: Implement generic period types**

`AnnualPeriodAssessment` owns:

```java
public record AnnualPeriodAssessment(
    ReportTopic topic,
    LocalDate generatedOn,
    ReportHorizon horizon,
    List<YearAssessment> years,
    List<AnnualTransition> transitions,
    List<String> priorities) {
  public String periodLabel() {
    return switch (horizon.years()) {
      case 2 -> "两年";
      case 3 -> "三年";
      case 4 -> "四年";
      case 5 -> "五年";
      default -> throw new IllegalStateException("unsupported report horizon");
    };
  }
}
```

Add `AnnualTransition(fromYear, toYear, relation)` and compare every adjacent pair. Reuse the existing momentum mapping, but do not create one global relation from only `years.get(0)` and `years.get(1)`.

Keep `ThreeYearAssessor` and `ThreeYearAssessment` as deprecated adapters until legacy PDF presenter tests are migrated. They may delegate to the new types; do not maintain two calculation implementations.

**Step 4: Run focused and report regression tests**

```bash
cd apps/server
./mvnw -Dtest=AnnualPeriodAssessmentTest,AnnualPeriodAssessorTest,ThreeYearAssessmentTest,ReportComposerTest test
```

Expected: PASS with career/PDF behavior unchanged.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/AnnualPeriodAssessment.java \
  apps/server/src/main/java/com/bazi/app/report/AnnualPeriodAssessor.java \
  apps/server/src/main/java/com/bazi/app/report/ThreeYearAssessment.java \
  apps/server/src/main/java/com/bazi/app/report/ThreeYearAssessor.java \
  apps/server/src/test/java/com/bazi/app/report/AnnualPeriodAssessmentTest.java \
  apps/server/src/test/java/com/bazi/app/report/AnnualPeriodAssessorTest.java
git commit -m "refactor: model reports as configurable annual periods"
```

---

### Task 3: Extract complete wealth facts from one unified system

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthPath.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/EvidenceFamily.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthEvidence.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthNatalProfile.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthYearFacts.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthFactExtractor.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthFactExtractorTest.java`

**Step 1: Write fixture-based extraction tests**

Use the fixed case `林先生 / 男 / 1995-10-08 14:30 / 上海`, whose pillars are `乙亥 · 乙酉 · 壬申 · 丁未`.

Assert stable facts, not prose:

- day master is 壬 and natal balance is 偏强;
- visible/hidden 正财 and 偏财 are counted separately and retain pillar position;
- output, peer, resource, wealth, and authority counts are available;
- 2026 annual stem is 偏财, 2027 is 正财, 2028 is 七杀;
- active Dayun Ten God and annual-to-natal/Dayun branch relations are preserved;
- each evidence item contains a unique key, family, label, value, effect, and weight.

Example assertion:

```java
assertTrue(profile.tenGodOccurrences().stream().anyMatch(item ->
    item.tenGod().equals("正财") && item.position().equals("time.branch.hidden")));
assertEquals("偏财", years.get(0).annualStemTenGod());
assertEquals("正财", years.get(1).annualStemTenGod());
assertEquals("七杀", years.get(2).annualStemTenGod());
```

**Step 2: Run the extractor test and verify failure**

```bash
cd apps/server
./mvnw -Dtest=WealthFactExtractorTest test
```

Expected: FAIL because the wealth fact model does not exist.

**Step 3: Implement extraction without interpretation copy**

Create these five path codes:

```java
STABLE_INCOME("stable_income", "稳定收入"),
SKILL_INCOME("skill_income", "靠能力赚钱"),
PROJECT_INCOME("project_income", "项目和额外收入"),
COOPERATION_INCOME("cooperation_income", "合作带来的收入"),
RETENTION("retention", "把钱留下")
```

Extract evidence into four families so independent support can be checked:

- `NATAL_STRUCTURE`: visible/hidden Ten Gods, position, count, balance.
- `NATAL_COMBINATION`: output-to-wealth, wealth plus adequate carrying ability, peer competition.
- `DAYUN_CONTEXT`: active Dayun Ten God and Dayun relations.
- `ANNUAL_TRIGGER`: annual Ten God and annual branch relations.

Do not collapse 正财/偏财 into a single wealth flag. Do not convert facts into user copy in the extractor.

Reject structurally invalid charts, duplicate evidence keys, and malformed pillar data. A missing optional Dayun relation contributes no evidence; it must not create an empty-answer sentence.

**Step 4: Run the extractor tests**

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/wealth \
  apps/server/src/test/java/com/bazi/app/report/wealth/WealthFactExtractorTest.java
git commit -m "feat: extract structured wealth evidence"
```

---

### Task 4: Evaluate all five wealth paths and arbitrate primary, secondary, and risk

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthPathEvaluation.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthYearEvaluation.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthPeriodEvaluation.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthPathEvaluator.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthArbitrator.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthPathEvaluatorTest.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthArbitratorTest.java`

**Step 1: Write failing tests for complete evaluation**

Required assertions:

```java
assertEquals(Set.allOf(WealthPath.class), evaluation.paths().keySet());
assertNotNull(evaluation.primaryIncomePath());
assertNotNull(evaluation.secondaryIncomePath());
assertNotEquals(evaluation.primaryIncomePath(), evaluation.secondaryIncomePath());
assertNotNull(evaluation.mainRisk());
assertEquals(3, period.years().size());
```

Also test:

- one evidence key is counted at most once within a path;
- a `重点` label requires support from at least two evidence families;
- ties are deterministic and may produce a documented dual-path conclusion;
- limitations are retained rather than discarded by positive evidence;
- all five paths are returned even when a path has no positive score;
- five-year internal evaluation works without changing the product constant.

**Step 2: Run the tests to verify failure**

```bash
cd apps/server
./mvnw -Dtest=WealthPathEvaluatorTest,WealthArbitratorTest test
```

Expected: FAIL because the evaluator and arbitrator do not exist.

**Step 3: Implement explicit deterministic weights**

Keep weights in code for v2 so they are reviewable and versioned; do not add a general rule DSL yet.

Initial support rules:

| Evidence | Stable | Skill | Project | Cooperation | Retention |
|---|---:|---:|---:|---:|---:|
| Visible 正财 | +3 | 0 | 0 | 0 | +1 |
| Hidden 正财 | +1 | 0 | 0 | 0 | +1 |
| Visible 偏财 | 0 | 0 | +3 | 0 | 0 |
| Hidden 偏财 | 0 | 0 | +1 | 0 | 0 |
| Visible 食神/伤官 | 0 | +2 | +1 | 0 | 0 |
| Hidden 食神/伤官 | 0 | +1 | 0 | 0 | 0 |
| 原局输出与财富同时存在 | 0 | +2 | +1 | 0 | 0 |
| 命局承载不弱且有财富 | +1 | 0 | +1 | 0 | +2 |
| 流年正财 | +4 | 0 | +1 | 0 | +1 |
| 流年偏财 | +1 | 0 | +4 | 0 | 0 |
| 流年食神/伤官 | 0 | +4 | +1 | 0 | 0 |
| 岁运/原局出现合 | 0 | 0 | +1 | +2 | 0 |
| 大运财富 | +2 or +1 | 0 | +2 or +1 | 0 | +1 |
| 大运同类 | 0 | 0 | 0 | +1 | -2 |

Initial limitation rules:

- weak balance plus wealth activation: retention −3;
- peer competition plus natal wealth: cooperation −2 and retention −2;
- clash/punishment/harm plus wealth activation: cooperation −2 and retention −2;
- annual authority without direct wealth/output support: project −1, while retention receives +1 for stronger rule/contract emphasis;
- duplicate evidence keys are ignored after the first occurrence in the same path.

Status labels:

- net ≥ 6 and at least two support families: `重点`;
- net ≥ 3: `可以作为补充`;
- net ≥ 0: `表现一般`;
- net < 0: `需要谨慎`.

Do not sum the five paths into an overall fortune number. Choose primary and secondary only from the four income paths; choose risk from the strongest limiting evidence across all five paths. Break exact ties using the enum order above and expose `tied=true` so the narrative does not invent a false difference.

**Step 4: Run focused tests and inspect the fixed case**

Expected: PASS. For the fixed case, verify the result follows the reviewed route rather than asserting a fragile exact numeric total: 2026 emphasizes extra/project or skill income, 2027 emphasizes stable income, and 2028 emphasizes retention/rules unless stronger extracted evidence legitimately changes the route.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/wealth \
  apps/server/src/test/java/com/bazi/app/report/wealth/WealthPathEvaluatorTest.java \
  apps/server/src/test/java/com/bazi/app/report/wealth/WealthArbitratorTest.java
git commit -m "feat: evaluate and rank five wealth paths"
```

---

### Task 5: Generate a dedicated three-year wealth content model in natural Chinese

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportContent.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthNarrativePlan.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/wealth/WealthNarrativePlanner.java`
- Create: `apps/server/src/main/resources/report/wealth-plain-v2.yml`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthNarrativePlannerTest.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/CareerNarrativePlan.java`
- Keep for legacy read only: `apps/server/src/main/java/com/bazi/app/report/WealthNarrativePlanner.java`

**Step 1: Write the failing narrative contract tests**

The new content shape must contain:

```java
record WealthNarrativePlan(
    int horizonYears,
    String thesis,
    String summary,
    List<PathSummary> paths,
    String primaryPathCode,
    String secondaryPathCode,
    RiskSummary mainRisk,
    List<YearNarrative> years,
    List<String> route) implements ReportContent {}
```

Every `YearNarrative` contains `year`, `ganZhi`, `focus`, `incomeSource`, `retention`, `mainLimit`, exactly two `realitySignals`, exactly two `actions`, `transition`, and evidence keys.

Tests must verify:

- exactly three years and all five path summaries;
- the three annual focuses are not identical;
- 2027 explains how 2026 results affect it, and 2028 explains how the first two years affect it;
- plain copy contains no raw rule keys or Ten God jargon;
- no questionnaire answer can affect evidence, ranking, or prose;
- every sentence line is at most 48 Chinese characters where the UI renders it as one line;
- forbidden wording is absent: `现有证据不足`, `先观察再判断`, `卡点`, `现在卡住了`, `变现`, `现金流`, `止损`, `资产配置`, `财富自由`, `抓住机会`, `发力`, `赋能`, `兑现`, `承接`;
- no guarantees, exact amounts, exact months, stock/fund/crypto recommendations, or medical/legal conclusions;
- removing the title still leaves money-specific words such as `收入`, `到账`, `支出`, `成本`, `分配`, `结余`, rather than career-specific words such as `晋升`, `职责`, `授权`.

**Step 2: Run the test and verify failure**

```bash
cd apps/server
./mvnw -Dtest=com.bazi.app.report.wealth.WealthNarrativePlannerTest test
```

Expected: FAIL because the independent content model and planner do not exist.

**Step 3: Implement evidence-keyed copy selection**

- Put reviewed sentence fragments in `wealth-plain-v2.yml`, keyed by path, status, annual role, and transition type.
- Select copy only from `WealthPeriodEvaluation`; never select core copy from a reality questionnaire.
- Use the approved three-year route for the fixed fixture as an acceptance case, not a universal hard-coded answer:
  - year 1: identify what people actually pay for;
  - year 2: retain the repeatable and reliable income source;
  - year 3: remove low-quality income and improve actual surplus.
- If paths tie, state that two routes are both worth observing and distinguish them by evidence; do not fabricate a winner.
- If structural extraction fails, throw before persistence. Never emit an empty-answer fallback.

`CareerNarrativePlan` and `WealthNarrativePlan` both implement the marker `ReportContent`. Keep the old root-package `WealthNarrativePlanner` only until v1 snapshot compatibility is tested; do not call it for new reports.

**Step 4: Run narrative and language tests**

```bash
cd apps/server
./mvnw -Dtest=com.bazi.app.report.wealth.WealthNarrativePlannerTest test
```

Expected: PASS.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/report/ReportContent.java \
  apps/server/src/main/java/com/bazi/app/report/CareerNarrativePlan.java \
  apps/server/src/main/java/com/bazi/app/report/wealth \
  apps/server/src/main/resources/report/wealth-plain-v2.yml \
  apps/server/src/test/java/com/bazi/app/report/wealth/WealthNarrativePlannerTest.java
git commit -m "feat: compose independent wealth plain reports"
```

---

### Task 6: Persist v2 reports while preserving old v1 snapshots

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/dto/ReportDto.java`
- Modify: `apps/server/src/main/java/com/bazi/app/dto/ReportPreviewRequest.java`
- Modify: `apps/server/src/main/java/com/bazi/app/service/ReportService.java`
- Modify: `apps/server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/ReportPreviewIntegrationTest.java`

**Step 1: Write failing integration tests**

Add these scenarios:

1. Creating wealth plain without `wealthContext` succeeds.
2. The response uses `wealth-narrative-v2`, contains five paths, and contains years 2026/2027/2028.
3. Sending a legacy `wealthContext` is accepted but has no effect on v2 content.
4. Creating wealth professional returns HTTP 400 with code `WEALTH_PROFESSIONAL_NOT_AVAILABLE` and inserts no report.
5. A manually inserted `wealth-narrative-v1` JSON snapshot still deserializes as the legacy `CareerNarrativePlan` shape.
6. Career report behavior remains unchanged.

Use `JdbcTemplate` or `BaziReportMapper` in the compatibility test to insert a stable v1 JSON fixture. Do not generate the fixture through the new planner.

**Step 2: Run integration tests and verify failure**

```bash
cd apps/server
./mvnw -Dtest=SavedReportIntegrationTest,ReportPreviewIntegrationTest test
```

Expected: FAIL because wealth still requires a questionnaire, professional is accepted, and the DTO only supports career content.

**Step 3: Switch new wealth generation to v2**

In `ReportService`:

- set `WEALTH_CONTENT_VERSION = "wealth-narrative-v2"`;
- reject wealth professional before chart calculation and persistence;
- stop requiring or converting `wealthContext` for v2;
- assess with `ReportHorizon.WEALTH_PRODUCT`;
- run fact extraction, path evaluation, arbitration, then the v2 planner;
- write `{}` or a versioned system context to `context_json`; do not store invented user answers;
- insert only after the complete content object is valid;
- mark `create` transactional so future payment integration cannot retain a partial report.

Change `ReportDto.content` to `ReportContent`. In `toDto`, deserialize by `contentVersion`:

```java
ReportContent content = switch (report.getContentVersion()) {
  case "wealth-narrative-v2" -> objectMapper.readValue(json, WealthNarrativePlan.class);
  default -> objectMapper.readValue(json, CareerNarrativePlan.class);
};
```

Keep `wealthContext` optional and deprecated in the request contract for one compatibility release. Ignore it for v2 and remove it in a later cleanup.

**Step 4: Run integration and backend regression tests**

```bash
cd apps/server
./mvnw test
```

Expected: all backend tests PASS; no existing career/PDF regression fails.

**Step 5: Commit**

```bash
git add apps/server/src/main/java/com/bazi/app/dto/ReportDto.java \
  apps/server/src/main/java/com/bazi/app/dto/ReportPreviewRequest.java \
  apps/server/src/main/java/com/bazi/app/service/ReportService.java \
  apps/server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java \
  apps/server/src/test/java/com/bazi/app/ReportPreviewIntegrationTest.java
git commit -m "feat: persist wealth v2 with snapshot compatibility"
```

---

### Task 7: Simplify wealth purchase UI and disable the professional edition

**Files:**
- Modify: `apps/web/src/services/reportApi.ts`
- Modify: `apps/web/src/services/reportApi.test.ts`
- Modify: `apps/web/src/pages/ReportPage.tsx`
- Modify: `apps/web/src/pages/ReportPage.test.tsx`
- Modify: `apps/web/src/styles/app.css`

**Step 1: Write failing frontend tests**

Test the wealth topic UI:

- no `补充财富现状` section or wealth choices are rendered;
- the cover and order summary say `未来三年`;
- plain edition is selected, enabled, and priced at ¥6.9;
- professional card remains visible, is disabled, contains `设计中 · 暂未开放`, and does not show ¥12.9;
- switching from career professional to wealth resets edition to plain;
- clicking generate calls `createReport(request, 'wealth', 'plain')` without context;
- career questions and both career editions remain unchanged.

**Step 2: Run focused tests and verify failure**

```bash
cd apps/web
npm test -- src/pages/ReportPage.test.tsx src/services/reportApi.test.ts
```

Expected: FAIL because the wealth questionnaire and professional purchase path still exist.

**Step 3: Implement topic-specific edition state**

- Remove wealth questionnaire state and completion gating from `ReportPage`.
- Keep the old API types only if needed to read historical fixtures; new wealth creation sends no context.
- Replace the global edition availability assumption with topic-aware metadata.
- A disabled professional plate must use a real `disabled` attribute, `aria-disabled="true"`, and visible unavailable copy.
- Do not render a price for unavailable wealth professional.
- Keep the price in one metadata source; do not duplicate ¥6.9 in button copy and conditionals.
- Update styles only for disabled/available state. Preserve the current visual system.

**Step 4: Run focused tests, lint, and build**

```bash
cd apps/web
npm test -- src/pages/ReportPage.test.tsx src/services/reportApi.test.ts
npm run lint
npm run build
```

Expected: PASS, lint reports zero errors, build succeeds.

**Step 5: Commit**

```bash
git add apps/web/src/services/reportApi.ts \
  apps/web/src/services/reportApi.test.ts \
  apps/web/src/pages/ReportPage.tsx \
  apps/web/src/pages/ReportPage.test.tsx \
  apps/web/src/styles/app.css
git commit -m "feat: simplify wealth report purchase flow"
```

---

### Task 8: Render the independent three-year wealth report and retain old readers

**Files:**
- Create: `apps/web/src/components/report/WealthReportReader.tsx`
- Create: `apps/web/src/components/report/CareerReportReader.tsx`
- Modify: `apps/web/src/services/reportApi.ts`
- Modify: `apps/web/src/pages/ReportReaderPage.tsx`
- Modify: `apps/web/src/pages/ReportReaderPage.test.tsx`
- Modify: `apps/web/src/pages/ReportLibraryPage.tsx`
- Modify: `apps/web/src/pages/ReportLibraryPage.test.tsx`
- Modify: `apps/web/src/styles/app.css`

**Step 1: Define a discriminated frontend content union**

Add `WealthNarrativePlan`, `WealthPathSummary`, and `WealthYearNarrative` interfaces matching the API. Represent saved reports as a union keyed by `contentVersion`, because old wealth v1 reports use the career content shape.

Write tests for:

- v2 reader title `三年总断`;
- five-path overview is always present;
- primary, secondary, and risk are clearly differentiated;
- all three years render with generic ordinal labels, not a two-item conditional;
- 2028 content renders without layout or key warnings;
- v1 wealth and career snapshots still use the legacy reader;
- the library card can read `thesis` from both shapes;
- professional evidence is not exposed for wealth because no new professional report can exist.

**Step 2: Run reader tests and verify failure**

```bash
cd apps/web
npm test -- src/pages/ReportReaderPage.test.tsx src/pages/ReportLibraryPage.test.tsx
```

Expected: FAIL because the reader assumes `CareerNarrativePlan` and hard-codes two years.

**Step 3: Split the readers by content version**

- `ReportReaderPage` loads/error-handles the report and delegates.
- `CareerReportReader` preserves current career and v1 wealth rendering.
- `WealthReportReader` renders in this order:
  1. `先看你的钱路` thesis and primary/secondary/risk;
  2. five-path overview;
  3. three annual sections;
  4. three-year route;
  5. generated date and library link.
- Generate Chinese ordinals from an array or formatter supporting at least five items; never use `index === 0 ? 第一 : 第二`.
- Keep the paper/card width aligned with the shared report background and validate the third annual card at mobile width.

**Step 4: Run frontend regression checks**

```bash
cd apps/web
npm test -- src/pages/ReportReaderPage.test.tsx src/pages/ReportLibraryPage.test.tsx
npm test
npm run lint
npm run build
```

Expected: all frontend tests PASS, lint zero errors, build succeeds.

**Step 5: Commit**

```bash
git add apps/web/src/components/report \
  apps/web/src/services/reportApi.ts \
  apps/web/src/pages/ReportReaderPage.tsx \
  apps/web/src/pages/ReportReaderPage.test.tsx \
  apps/web/src/pages/ReportLibraryPage.tsx \
  apps/web/src/pages/ReportLibraryPage.test.tsx \
  apps/web/src/styles/app.css
git commit -m "feat: render three-year wealth reports"
```

---

### Task 9: Update the API contract, language standard, and reviewed acceptance cases

**Files:**
- Create: `apps/server/src/test/resources/report/wealth-v2-golden-cases.json`
- Create: `apps/server/src/test/java/com/bazi/app/report/wealth/WealthGoldenCaseTest.java`
- Modify: `contracts/openapi.yaml`
- Modify: `docs/design/report-reader-language-standard.md`
- Modify: `docs/design/mingshu-backend-algorithm-review-brief.md`
- Modify: `docs/design/README.md`

**Step 1: Add contract and golden-case assertions**

The golden-case file stores evidence and route expectations only, not entire prose snapshots:

```json
[
  {
    "fixtureId": "lin-1995-10-08-1430",
    "asOf": "2026-08-23",
    "horizonYears": 3,
    "requiredYears": [2026, 2027, 2028],
    "requiredPaths": [
      "stable_income",
      "skill_income",
      "project_income",
      "cooperation_income",
      "retention"
    ],
    "forbiddenEvidenceKeys": []
  }
]
```

Do not record accidental exact scores until each underlying rule has been reviewed. Add exact required/forbidden evidence keys only after manual chart verification.

**Step 2: Update OpenAPI**

- mark `wealthContext` deprecated and optional;
- document wealth plain as three years;
- document wealth professional as unavailable;
- make `ReportInfo.content` a `oneOf` career/legacy and wealth-v2 schemas;
- define all wealth v2 fields and require the five path codes;
- do not expose `horizonYears` as a request property.

**Step 3: Update language and algorithm documentation**

- change the plain-language standard from “future one to two years” to topic-configured two to five years;
- retain one main focus per year and plain Chinese rules;
- document the product/technical distinction: wealth sells three years, engine supports five;
- document the exact v2 scoring table, tie behavior, evidence-family requirement, and limitations;
- state that survey answers are removed from wealth v2.

**Step 4: Run contract and golden-case tests**

```bash
cd apps/server
./mvnw -Dtest=com.bazi.app.report.wealth.WealthGoldenCaseTest test
cd ../..
git diff --check
```

Expected: PASS and no whitespace errors.

**Step 5: Commit**

```bash
git add contracts/openapi.yaml \
  docs/design/report-reader-language-standard.md \
  docs/design/mingshu-backend-algorithm-review-brief.md \
  docs/design/README.md \
  apps/server/src/test/resources/report/wealth-v2-golden-cases.json \
  apps/server/src/test/java/com/bazi/app/report/wealth/WealthGoldenCaseTest.java
git commit -m "docs: specify wealth v2 evidence and API contract"
```

---

### Task 10: Run end-to-end acceptance without changing payment state

**Files:**
- Modify only if a real defect is found: `e2e/run_e2e.py`
- No production file should be changed merely to make screenshots pass.

**Step 1: Run complete automated verification**

```bash
cd apps/server
./mvnw test
cd ../web
npm test
npm run lint
npm run build
cd ../..
git diff --check
```

Expected: backend and frontend suites PASS, lint zero errors, build succeeds, no whitespace errors.

**Step 2: Start local services**

Use the project's existing MySQL/local profile and `VITE_API_MODE=http`. Do not modify production payment or membership state.

**Step 3: Verify the real browser flow**

At a 430px viewport:

1. register/login;
2. generate the fixed chart;
3. enter 命书报告 and choose 财富;
4. verify there is no questionnaire;
5. verify professional is visible but disabled and has no price;
6. generate plain wealth without actual charging;
7. verify redirect to the saved report;
8. verify five paths and 2026/2027/2028 all render;
9. return to 我的命书 and reopen the report;
10. verify an old v1 wealth fixture still opens.

Capture one full-page screenshot for content review, but do not add it to Git unless requested.

**Step 4: Perform manual content acceptance**

Reject the release if any answer is “no”:

- Can the report be identified as wealth content after hiding its title?
- Are all five paths calculated and visible?
- Does every primary judgment have traceable support and limitation evidence?
- Do the three years have distinct roles and explicit transitions?
- Is the Chinese natural without report jargon or missing sentence subjects?
- Does 2028 add a new decision rather than repeat 2026/2027?
- Are there no guarantees or specific investment recommendations?
- Does a generation failure leave no saved paid report?

**Step 5: Commit any test-only acceptance update**

Only if Step 3 required a legitimate E2E improvement:

```bash
git add e2e/run_e2e.py
git commit -m "test: cover three-year wealth report flow"
```

Otherwise do not create an empty commit.

---

## Final release checkpoint

- New wealth reports use `wealth-narrative-v2` and exactly three years.
- Internal tests prove 2–5 year support; no year selector is exposed.
- Wealth professional is disabled in UI and rejected by API.
- Wealth questionnaire is gone and cannot influence results.
- All five paths are calculated every year.
- Primary, secondary, and risk selections are deterministic and evidence-backed.
- Old career and wealth v1 snapshots remain readable.
- Database schema requires no destructive migration because report bodies remain JSON snapshots.
- No PDF, payment, membership quota, or LLM scope has leaked into this release.
