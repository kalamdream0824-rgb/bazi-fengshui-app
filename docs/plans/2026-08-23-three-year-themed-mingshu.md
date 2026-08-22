# Three-Year Themed Mingshu Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** Replace the generic five-chapter prototype with evidence-backed, topic-first reports covering the current and next two calendar years for overall fortune, career, wealth and relationships, then regenerate one plain-language and one professional acceptance PDF.

**Architecture:** Keep `BaziService` as the deterministic calendar source and keep Apache FOP as the native-text renderer. Add a three-year analysis layer that derives annual stem/branch relations from the natal chart and the active Dayun, evaluates conditional topic rules into a shared `YearAssessment`, and then presents the same conclusions through separate plain-language and professional presenters. The report body is organized by selected topic and year; chart structure moves to an appendix.

**Tech Stack:** Java 17, Spring Boot 3.5, `lunar-java` 1.7.4, Apache FOP 2.11, Apache PDFBox 3.0.8, JUnit 5, YAML copy resources.

---

## Confirmed product decisions

- Initial shipped topics are only `overall`, `career`, `wealth` and `relationship`.
- Both editions cover the same period: the current calendar year plus the next two calendar years.
- The annual Ganzhi uses the traditional Lichun boundary, but each section is labeled with its Gregorian year for readability.
- No monthly predictions are generated.
- The selected topic must account for at least 80% of the report body.
- Other life areas appear only when they materially affect the selected topic.
- Plain edition target: 9–12 pages, direct language, no unexplained specialist terms.
- Professional edition target: 13–16 pages, full rule path, evidence, counter-evidence, method boundary and confidence.
- Both editions use the same `YearAssessment`; presentation must never change the underlying conclusion.
- User-visible stage labels are `蓄`, `稳`, `进`, `转`, `慎`. Do not expose numerical fortune scores.
- A conclusion needs at least two independent evidence items. Insufficient evidence produces a restrained “依据不足，建议观察” result instead of filler.
- Do not infer exact events, exact months, medical outcomes, guaranteed wealth, marriage dates or other certainty claims.
- This plan stops after two revised sample PDFs pass content review. API persistence, orders, membership quota and frontend purchase flow remain in the existing paid-report plan.

## Existing prototype to preserve

- Calendar and chart facts: `apps/server/src/main/java/com/bazi/app/service/BaziService.java`
- Current report contract: `apps/server/src/main/java/com/bazi/app/report/ReportDocument.java`
- Current simplified evidence: `apps/server/src/main/java/com/bazi/app/report/ReportAnalysis.java`
- Generic topic templates to replace: `apps/server/src/main/java/com/bazi/app/report/TopicRules.java`
- Current composer: `apps/server/src/main/java/com/bazi/app/report/ReportComposer.java`
- Native-text renderer: `apps/server/src/main/java/com/bazi/app/report/ReportPdfRenderer.java`
- PDF resources: `apps/server/src/main/resources/report/`
- Existing fixture catalogue: `contracts/fixtures/bazi-cases.json`

## Worktree and commit guard

The current worktree already contains uncommitted UI, documentation and report-prototype changes. Do not clean, reset, stage or commit unrelated files. The commit commands below are review checkpoints only; execute them only after the user explicitly requests a commit.

---

### Task 1: Restrict the shipped topic catalogue and define the three-year contract

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/ReportTopic.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualStage.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/ConfidenceLevel.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/AssessmentBasis.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/YearAssessment.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/ThreeYearAssessment.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/ReportDocument.java`
- Modify: `contracts/openapi.yaml`
- Modify: `apps/server/src/test/java/com/bazi/app/report/ReportTopicTest.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/ThreeYearAssessmentTest.java`

**Step 1: Write the failing shipped-topic test**

```java
@Test
void shipsOnlyFourReviewedTopics() {
  assertEquals(
      List.of("overall", "career", "wealth", "relationship"),
      Arrays.stream(ReportTopic.values()).map(ReportTopic::code).toList());
  assertThrows(BusinessException.class, () -> ReportTopic.fromCode("family"));
}
```

**Step 2: Write the failing assessment invariant test**

```java
@Test
void assessmentRequiresThreeConsecutiveYearsAndEvidenceBackedConclusions() {
  assertThrows(IllegalArgumentException.class, () -> new ThreeYearAssessment(
      ReportTopic.CAREER,
      LocalDate.of(2026, 8, 23),
      List.of(year(2026), year(2028))));

  assertThrows(IllegalArgumentException.class, () -> new YearAssessment(
      2026, "丙午", AnnualStage.ADVANCE, "推进期", "职责扩张",
      List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
      ConfidenceLevel.HIGH, List.of(), AssessmentBasis.REVIEWED_RULES));
}
```

**Step 3: Run the tests and verify RED**

Run:

```bash
cd apps/server
./mvnw -Dtest=ReportTopicTest,ThreeYearAssessmentTest test
```

Expected: FAIL because the annual assessment types do not exist and deferred topics are still present.

**Step 4: Add the minimum immutable contract**

```java
public enum AnnualStage {
  PREPARE("蓄"), STABLE("稳"), ADVANCE("进"), TRANSITION("转"), CAUTION("慎");
}

public enum ConfidenceLevel {
  LOW, MEDIUM, HIGH
}

public enum AssessmentBasis {
  REVIEWED_RULES, INSUFFICIENT_EVIDENCE
}

public record YearAssessment(
    int year,
    String ganZhi,
    AnnualStage stage,
    String headline,
    String conclusion,
    List<AnnualFinding> opportunities,
    List<AnnualFinding> pressures,
    List<String> actions,
    List<String> realitySignals,
    List<ReportEvidence> evidence,
    List<ReportEvidence> counterEvidence,
    ConfidenceLevel confidence,
    List<String> ruleKeys,
    AssessmentBasis basis) {

  public YearAssessment {
    if (basis == AssessmentBasis.REVIEWED_RULES
        && (evidence == null || evidence.stream().map(ReportEvidence::key).distinct().count() < 2)) {
      throw new IllegalArgumentException("annual conclusion requires at least two evidence items");
    }
  }
}

public record AnnualFinding(String title, String explanation, List<String> evidenceKeys) {}

public record ThreeYearAssessment(
    ReportTopic topic,
    LocalDate generatedOn,
    List<YearAssessment> years,
    String trajectory,
    List<String> priorities) {

  public ThreeYearAssessment {
    if (years == null || years.size() != 3
        || years.get(1).year() != years.get(0).year() + 1
        || years.get(2).year() != years.get(1).year() + 1) {
      throw new IllegalArgumentException("three consecutive years are required");
    }
  }
}
```

Use defensive `List.copyOf` in compact constructors. An `INSUFFICIENT_EVIDENCE` result must be low-confidence and contain no matched rule findings; it is the only result allowed below the two-evidence gate. Add an appendix collection to `ReportDocument`; the five main chapters remain topic-first, while chart facts live in `ReportAppendix`.

**Step 5: Restrict `ReportTopic` and OpenAPI**

Keep only:

```java
OVERALL("overall", "综合运势"),
CAREER("career", "事业运势"),
WEALTH("wealth", "财富运势"),
RELATIONSHIP("relationship", "感情运势");
```

Update `ReportTopic` in `contracts/openapi.yaml` to the same four values. Do not leave deferred topics visible in the public contract.

**Step 6: Run tests and verify GREEN**

Run: `cd apps/server && ./mvnw -Dtest=ReportTopicTest,ThreeYearAssessmentTest test`

Expected: PASS.

**Step 7: Review checkpoint**

```bash
git diff --check
git diff -- apps/server/src/main/java/com/bazi/app/report contracts/openapi.yaml apps/server/src/test/java/com/bazi/app/report
```

Optional commit after explicit approval:

```bash
git add apps/server/src/main/java/com/bazi/app/report contracts/openapi.yaml apps/server/src/test/java/com/bazi/app/report
git commit -m "refactor(report): define three-year themed contract"
```

---

### Task 2: Calculate deterministic facts for the current and next two years

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualFact.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualContext.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualContextFactory.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/BranchRelation.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/BranchRelations.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/TenGodGroup.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/ReportAnalysis.java`
- Test: `apps/server/src/test/java/com/bazi/app/report/AnnualContextFactoryTest.java`
- Test: `apps/server/src/test/java/com/bazi/app/report/BranchRelationsTest.java`

**Step 1: Write failing branch-relation table tests**

Use explicit symmetric tables; do not calculate relations through index arithmetic.

```java
@ParameterizedTest
@CsvSource({
  "子,午,CLASH", "丑,未,CLASH", "寅,申,CLASH", "卯,酉,CLASH", "辰,戌,CLASH", "巳,亥,CLASH",
  "子,丑,HARMONY", "寅,亥,HARMONY", "卯,戌,HARMONY", "辰,酉,HARMONY", "巳,申,HARMONY", "午,未,HARMONY",
  "子,未,HARM", "丑,午,HARM", "寅,巳,HARM", "卯,辰,HARM", "申,亥,HARM", "酉,戌,HARM"
})
void detectsSymmetricRelations(String left, String right, BranchRelation expected) {
  assertTrue(BranchRelations.between(left, right).contains(expected));
  assertTrue(BranchRelations.between(right, left).contains(expected));
}
```

Add separate tests for the six breaks and the documented punishment groups. Self-punishment must only match identical `辰`, `午`, `酉` or `亥`.

**Step 2: Write a failing fixed-clock annual context test**

```java
@Test
void buildsCurrentAndNextTwoAnnualContextsFromOneChart() {
  Clock clock = Clock.fixed(Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));
  PaipanRequest request = fixtureRequest("1995-10-08T14:30:00", "male");
  PaipanResultDto chart = new BaziService().paipan(request);

  List<AnnualContext> contexts = new AnnualContextFactory(clock).create(request, chart);

  assertEquals(List.of(2026, 2027, 2028), contexts.stream().map(AnnualContext::year).toList());
  assertEquals(List.of("丙午", "丁未", "戊申"), contexts.stream().map(AnnualContext::ganZhi).toList());
  assertTrue(contexts.stream().allMatch(context -> context.activeDaYun() != null));
}
```

**Step 3: Run tests and verify RED**

Run: `cd apps/server && ./mvnw -Dtest=AnnualContextFactoryTest,BranchRelationsTest test`

Expected: FAIL because the annual fact types do not exist.

**Step 4: Implement explicit annual facts**

`AnnualContext` must contain:

```java
public record AnnualContext(
    int year,
    String ganZhi,
    String yearStemTenGod,
    TenGodGroup yearStemGroup,
    DaYunDto activeDaYun,
    List<AnnualFact> natalRelations,
    List<AnnualFact> dayunRelations,
    ReportAnalysis natalAnalysis) {}
```

For each year:

1. Obtain the year's Ganzhi from `Solar.fromYmdHms(year, 7, 1, 12, 0, 0).getLunar().getYearInGanZhiExact()`. July is used only to retrieve the Ganzhi after Lichun; the report must state that the traditional year changes at Lichun.
2. Derive the annual stem Ten God relative to the natal day stem using `LunarUtil.SHI_SHEN`.
3. Resolve the active Dayun by `startYear <= year < endYear` using the existing Dayun list.
4. Compare the annual branch with all four natal branches and the active Dayun branch.
5. Store facts as structured keys, not prose, for example `annual.branch.clash.day` and `annual.stem.group.output`.

Map Ten Gods into functional groups:

```text
resource: 正印, 偏印
peer: 比肩, 劫财
output: 食神, 伤官
wealth: 正财, 偏财
authority: 正官, 七杀
```

Do not add month-level data.

**Step 5: Run tests and verify GREEN**

Run: `cd apps/server && ./mvnw -Dtest=AnnualContextFactoryTest,BranchRelationsTest test`

Expected: PASS for the fixed 2026–2028 fixture and every relation table case.

**Step 6: Review checkpoint**

Run `git diff --check` and inspect all relation tables manually. A wrong table silently contaminates every report.

Optional commit after explicit approval:

```bash
git add apps/server/src/main/java/com/bazi/app/report apps/server/src/test/java/com/bazi/app/report
git commit -m "feat(report): derive three-year annual facts"
```

---

### Task 3: Replace unconditional templates with a conditional rule engine

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/AnnualContext.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualRule.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualFindingType.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualRuleResult.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualRuleEvaluation.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/AnnualRuleEngine.java`
- Delete after Task 4 migration: `apps/server/src/main/java/com/bazi/app/report/TopicRules.java`
- Delete after Task 4 migration: `apps/server/src/main/java/com/bazi/app/report/ReportRule.java`
- Delete after Task 4 migration: `apps/server/src/main/java/com/bazi/app/report/ReportRuleResult.java`
- Test: `apps/server/src/test/java/com/bazi/app/report/AnnualRuleEngineTest.java`

**Step 1: Write failing rule eligibility tests**

```java
@Test
void ignoresRulesThatDoNotMatchTheAnnualFacts() {
  AnnualContext context = contextWith("annual.stem.group.resource");
  AnnualRule rule = ruleRequiring("annual.stem.group.output");
  assertTrue(engine.evaluate(context, ReportTopic.CAREER, List.of(rule)).isEmpty());
}

@Test
void refusesAConclusionWithOnlyOneEvidenceItem() {
  AnnualContext context = contextWith("annual.stem.group.output");
  AnnualRule rule = ruleRequiring("annual.stem.group.output");
  assertTrue(engine.evaluate(context, ReportTopic.CAREER, List.of(rule)).isEmpty());
}

@Test
void counterEvidenceLowersConfidence() {
  AnnualContext context = contextWith(
      "annual.stem.group.output",
      "natal.balance.supportive",
      "annual.branch.clash.dayun");
  AnnualRuleResult result = engine.evaluate(
      context, ReportTopic.CAREER, List.of(careerVisibilityRule())).get(0);
  assertEquals(ConfidenceLevel.MEDIUM, result.confidence());
  assertFalse(result.counterEvidence().isEmpty());
}
```

**Step 2: Run test and verify RED**

Run: `cd apps/server && ./mvnw -Dtest=AnnualRuleEngineTest test`

Expected: FAIL because conditional rules and evidence gates do not exist.

**Step 3: Define the rule interface**

```java
public interface AnnualRule {
  String key();
  ReportTopic topic();
  int priority();
  boolean matches(AnnualContext context);
  List<AnnualFact> supportingEvidence(AnnualContext context);
  List<AnnualFact> counterEvidence(AnnualContext context);
  AnnualRuleResult build(AnnualContext context);
}
```

`AnnualRuleEngine` must:

1. Require the purchased `ReportTopic` as an explicit `evaluate` argument, then filter by selected topic and `matches`.
2. Drop any result with fewer than two independent supporting evidence keys.
3. De-duplicate results by conclusion category.
4. Sort by priority, then confidence.
5. Cap a year at two opportunities and two pressures.
6. Produce an explicit insufficient-evidence result only when no reviewed rule survives.
7. Select `AnnualStage` from the surviving rules without exposing scores:
   - `TRANSITION` when a high-priority Dayun or natal-palace clash is present.
   - `CAUTION` when pressure findings dominate and have medium/high confidence.
   - `ADVANCE` when opportunity findings dominate and no high-confidence pressure contradicts them.
   - `PREPARE` when resource/learning findings dominate without outward activation.
   - `STABLE` otherwise.

Internal weights are allowed only for ordering. Never include them in `YearAssessment` or PDF copy.

**Step 4: Run test and verify GREEN**

Run: `cd apps/server && ./mvnw -Dtest=AnnualRuleEngineTest test`

Expected: PASS.

**Step 5: Preserve the old composer only as a temporary compatibility path**

Do not delete `TopicRules.java` during Task 3: the reviewed annual catalogues do not exist until Task 4, and deleting it now would leave `ReportComposer` unable to generate the existing samples. After Task 4 wires the four catalogues, verify the old path has no consumers:

```bash
rg "TopicRules" apps/server/src
```

Expected before Task 4 migration: `ReportComposer` and the legacy rule types. Expected after Task 4 migration: no matches, then delete `TopicRules.java`, `ReportRule.java` and `ReportRuleResult.java` together.

**Step 6: Review checkpoint**

Optional commit after explicit approval:

```bash
git add apps/server/src/main/java/com/bazi/app/report apps/server/src/test/java/com/bazi/app/report
git commit -m "refactor(report): evaluate conditional annual rules"
```

---

### Task 4: Implement the four reviewed topic rule catalogues

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/rules/OverallAnnualRules.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/rules/CareerAnnualRules.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/rules/WealthAnnualRules.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/rules/RelationshipAnnualRules.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/rules/AnnualRuleCatalog.java`
- Create: `apps/server/src/main/resources/report/annual-copy.yml`
- Test: `apps/server/src/test/java/com/bazi/app/report/AnnualTopicRulesTest.java`

**Step 1: Write failing topic-divergence tests**

For the fixed 1995 fixture:

```java
@Test
void fourTopicsProduceDifferentRulePathsForTheSameThreeYears() {
  Map<ReportTopic, ThreeYearAssessment> reports = assessAllTopics(fixture1995());

  assertEquals(4, reports.size());
  assertEquals(4, reports.values().stream()
      .map(report -> report.years().stream()
          .flatMap(year -> year.ruleKeys().stream())
          .collect(joining("|")))
      .distinct()
      .count());
}

@Test
void threeYearsCannotRepeatTheSameHeadlineAndEvidence() {
  ThreeYearAssessment report = assess(fixture1995(), ReportTopic.CAREER);
  assertEquals(3, report.years().stream().map(YearAssessment::headline).distinct().count());
  assertEquals(3, report.years().stream()
      .map(year -> year.evidence().stream().map(ReportEvidence::key).sorted().toList())
      .distinct().count());
}
```

Add a second natal fixture from `contracts/fixtures/bazi-cases.json`; for the same topic, at least two of three annual headlines or stage labels must differ. This is a regression guard against hidden templates, not a claim that every chart is unique in every sentence.

**Step 2: Run test and verify RED**

Run: `cd apps/server && ./mvnw -Dtest=AnnualTopicRulesTest test`

Expected: FAIL because the four catalogues do not exist.

**Step 3: Implement a small reviewed rule set**

Do not attempt hundreds of rules. Begin with 5–7 conditional rules per topic.

Required initial rule families:

```text
overall:
- support/restore: resource or peer annual stem + weak/middle natal support
- express/advance: output annual stem + adequate natal support
- responsibility/pressure: authority or wealth annual stem + weak natal support
- transition: annual branch clashes active Dayun or two natal branches

career:
- visibility: output activation + natal output/resource evidence
- responsibility: authority activation + resource or adequate support
- preparation: resource activation + next-year outward activation
- role transition: annual branch clashes Dayun/month branch
- overextension: outward/wealth activation + weak support or conflicting clash

wealth:
- monetization: output activation + natal wealth present
- resource opportunity: wealth activation + adequate support
- cash-flow pressure: wealth activation + weak support
- competition/shared money: peer activation + natal wealth present
- contract caution: Dayun/natal relation conflict + wealth activation

relationship:
- cooperation: annual branch harmonizes the day branch + no day-branch harm/clash
- boundary change: annual branch clashes/harms/punishes the day branch
- relationship activation: spouse-star group appears with a second relationship evidence item
- communication repair: resource/output relation supports expression and reception
- pace caution: relationship activation plus Dayun conflict
```

For relationship rules, spouse-star evidence is `wealth` for male charts and `authority` for female charts. It is only an activation signal; never translate it into marriage, breakup or fidelity claims.

Every rule needs:

- a stable rule key;
- match conditions;
- at least two support evidence selectors;
- optional counter-evidence selectors;
- a neutral conclusion category;
- plain copy key;
- professional copy key;
- one actionable reality signal.

Store reviewed wording in `annual-copy.yml`; Java rule files contain facts and rule wiring, not paragraphs.

**Step 4: Run tests and verify GREEN**

Run: `cd apps/server && ./mvnw -Dtest=AnnualTopicRulesTest test`

Expected: PASS for four topic paths, three non-identical years and two differing natal fixtures.

**Step 5: Content review checkpoint**

Export a debug JSON containing all 12 `topic × year` results for the 1995 fixture. Review rule key, support evidence, counter-evidence and confidence before writing PDF prose.

Optional commit after explicit approval:

```bash
git add apps/server/src/main/java/com/bazi/app/report/rules apps/server/src/main/resources/report/annual-copy.yml apps/server/src/test/java/com/bazi/app/report/AnnualTopicRulesTest.java
git commit -m "feat(report): add four annual topic catalogues"
```

---

### Task 5: Build distinct plain and professional presenters around the same assessment

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportPresenter.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/PlainReportPresenter.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/ProfessionalReportPresenter.java`
- Modify: `apps/server/src/main/java/com/bazi/app/report/ReportComposer.java`
- Modify: `apps/server/src/main/resources/report/report-copy.yml`
- Test: `apps/server/src/test/java/com/bazi/app/report/ReportPresenterTest.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/ReportComposerTest.java`

**Step 1: Write failing shared-conclusion tests**

```java
@Test
void editionsShareAnnualConclusionsButNotPresentationDensity() {
  ThreeYearAssessment assessment = assessor.assess(fixture1995(), ReportTopic.CAREER);
  ReportDocument plain = plainPresenter.present(assessment, fixtureProfile());
  ReportDocument professional = professionalPresenter.present(assessment, fixtureProfile());

  assertEquals(annualConclusionKeys(plain), annualConclusionKeys(professional));
  assertEquals(annualEvidenceKeys(plain), annualEvidenceKeys(professional));
  assertTrue(flatten(professional).length() > flatten(plain).length() * 1.25);
}
```

**Step 2: Write the failing plain-language terminology test**

```java
@Test
void plainEditionContainsNoUnexplainedSpecialistTerms() {
  String text = flatten(plainPresenter.present(careerAssessment(), fixtureProfile()));
  for (String term : List.of("透干", "通根", "制化", "月令", "食伤", "官杀", "比劫")) {
    assertFalse(text.contains(term), term);
  }
}
```

**Step 3: Write the failing topic-focus test**

Tag every body section with `sectionTopic`. Excluding cover, navigation and appendix, at least 80% of body sections must have the purchased topic tag.

```java
assertTrue(topicShare(plain) >= 0.80);
assertTrue(topicShare(professional) >= 0.80);
```

**Step 4: Run tests and verify RED**

Run: `cd apps/server && ./mvnw -Dtest=ReportPresenterTest,ReportComposerTest test`

Expected: FAIL because the presenter split and new chapter structure do not exist.

**Step 5: Implement the five topic-first chapters**

```text
壹 未来三年主题总览
贰 当前年度主题详解
叁 下一年度主题详解
肆 第三年度主题详解
伍 三年行动路线
附录 命盘与计算依据
```

Plain annual section order:

```text
年度标签 → 一句话结论 → 机会 → 压力 → 建议 → 现实观察信号 → “为什么这样判断”的普通话解释
```

Professional annual section order:

```text
年度标签 → 核心结论 → 机会/压力 → 规则键 → 完整证据 → 反向证据 → 置信等级 → 方法边界 → 现实观察信号
```

Do not place generic personality analysis in the body. The appendix may contain pillars, five-element counts, the current simplified balance calculation, Ten Gods and Dayun data.

**Step 6: Run tests and verify GREEN**

Run: `cd apps/server && ./mvnw -Dtest=ReportPresenterTest,ReportComposerTest test`

Expected: PASS for shared conclusions, plain terminology and 80% topic focus.

**Step 7: Review checkpoint**

Optional commit after explicit approval:

```bash
git add apps/server/src/main/java/com/bazi/app/report apps/server/src/main/resources/report apps/server/src/test/java/com/bazi/app/report
git commit -m "refactor(report): present topic-first three-year narratives"
```

---

### Task 6: Redesign the native-text PDF for the three-year timeline

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/report/ReportPdfRenderer.java`
- Modify: `apps/server/src/main/resources/report/mingshu.fo.xml`
- Modify: `apps/server/src/main/resources/report/fop.xconf`
- Modify: `apps/server/src/main/resources/report/fonts/README.md`
- Add before production: `apps/server/src/main/resources/report/fonts/NotoSerifSC-Regular.ttf`
- Add before production: `apps/server/src/main/resources/report/fonts/OFL.txt`
- Modify: `apps/server/src/test/java/com/bazi/app/report/ReportPdfRendererTest.java`

**Step 1: Change the failing PDF acceptance tests**

```java
@Test
void plainPdfLeadsWithTopicAndThreeYearTimeline() {
  PdfInspection pdf = inspect(render(ReportEdition.PLAIN, ReportTopic.CAREER));
  assertTrue(pdf.text().contains("未来三年事业运势"));
  assertTrue(pdf.text().contains("2026"));
  assertTrue(pdf.text().contains("2027"));
  assertTrue(pdf.text().contains("2028"));
  assertTrue(pdf.pages() >= 9 && pdf.pages() <= 12);
}

@Test
void professionalPdfAddsEvidenceWithoutChangingConclusions() {
  PdfInspection pdf = inspect(render(ReportEdition.PROFESSIONAL, ReportTopic.CAREER));
  assertTrue(pdf.text().contains("支持证据"));
  assertTrue(pdf.text().contains("反向证据"));
  assertTrue(pdf.text().contains("置信等级"));
  assertTrue(pdf.pages() >= 13 && pdf.pages() <= 16);
}
```

Keep the existing checks that every page has extractable text, no page is image-only, JavaScript is absent and Chinese glyphs extract correctly.

**Step 2: Run test and verify RED**

Run: `cd apps/server && ./mvnw -Dtest=ReportPdfRendererTest test`

Expected: FAIL because the current PDF still leads with chart structure and uses the old page ranges.

**Step 3: Implement the timeline layout**

- Cover title must be `{subject} · 未来三年{topicLabel}`.
- Page 2 is the three-year trajectory: three equal year panels with stage label, headline and one priority.
- Each annual chapter uses the year and topic in every running header.
- Plain edition puts one year on 1–2 pages and keeps all supporting explanation in ordinary language.
- Professional edition may use 2–3 pages per year for evidence and method details.
- Appendix is visually quieter and comes after the action roadmap.
- Do not use percentage scores, star ratings, red/green good/bad coloring or ornamental charts without data meaning.

**Step 4: Make the font gate explicit**

The existing macOS Songti fallback may be used only for local sample inspection. A production build must fail a dedicated `productionFontIsBundled` test until `NotoSerifSC-Regular.ttf` and `OFL.txt` are present. Do not copy or commit a proprietary system font.

**Step 5: Run test and verify GREEN**

Run: `cd apps/server && ./mvnw -Dtest=ReportPdfRendererTest test`

Expected: PASS with 9–12 plain pages, 13–16 professional pages and searchable Chinese on every page.

**Step 6: Visual review**

Render at least these pages to PNG with `pdftoppm` and inspect at 100% and 200%:

```text
cover
three-year overview
one plain annual page
one professional evidence page
appendix
```

Reject the sample if the professional version uses large blank areas to reach its page target.

Optional commit after explicit approval:

```bash
git add apps/server/src/main/java/com/bazi/app/report/ReportPdfRenderer.java apps/server/src/main/resources/report apps/server/src/test/java/com/bazi/app/report/ReportPdfRendererTest.java
git commit -m "feat(report): render three-year topic timeline"
```

---

### Task 7: Add anti-template, safety and sample acceptance coverage

**Files:**
- Create: `apps/server/src/test/java/com/bazi/app/report/ThreeYearReportComplianceTest.java`
- Create: `apps/server/src/test/resources/report/three-year-golden-cases.json`
- Modify: `docs/samples/README.md`
- Regenerate: `docs/samples/命书样本-通俗版-事业与职场.pdf`
- Regenerate: `docs/samples/命书样本-专业版-事业与职场.pdf`
- Modify: `docs/design/commercial-design.md`

**Step 1: Add failing compliance tests**

Required checks:

- All `4 topics × 3 years × at least 6 natal fixtures` produce valid assessments.
- Every normal conclusion has at least two independent support evidence keys.
- Counter-evidence is retained, not discarded after confidence selection.
- The three annual headlines are not identical for one report.
- The four topic rule-key signatures are not identical for one chart.
- At least two of three annual results differ between two natal fixtures for the same topic.
- Plain copy contains no unexplained specialist terms.
- Specialist terms in professional copy occur near a method explanation.
- No content contains `一定`, `注定`, `必发财`, `必离婚`, `保证`, `改运`, `消灾`.
- No report emits marriage dates, breakup dates, exact income or medical claims.
- Generated content is deterministic for the same chart, topic, edition, content version and fixed clock.

**Step 2: Run test and verify RED**

Run: `cd apps/server && ./mvnw -Dtest=ThreeYearReportComplianceTest test`

Expected: FAIL until all rule catalogues and presenters meet the anti-template constraints.

**Step 3: Add reviewed golden cases**

`three-year-golden-cases.json` stores only stable facts and reviewed rule expectations, not entire prose snapshots. Start it as an empty JSON array. For each accepted case, record `fixtureId`, `asOf`, `topic`, `year`, `requiredEvidenceKeys`, `requiredRuleKeys` and `forbiddenRuleKeys` only after manually checking the underlying chart, annual relation and rule premise. Do not pre-fill a concrete expectation from the current accidental output, and do not make tests pass by copying whatever the engine happens to emit.

**Step 4: Regenerate the two acceptance PDFs**

Use the same fixture and topic as the current samples so the user can compare versions directly:

```text
林先生
男
1995-10-08 14:30
上海
事业运势
as-of 2026-08-23
```

Overwrite the two files in `docs/samples/` only after all automated checks pass.

**Step 5: Run full verification**

```bash
cd apps/server
./mvnw test
cd ../..
git diff --check
pdfinfo 'docs/samples/命书样本-通俗版-事业与职场.pdf'
pdfinfo 'docs/samples/命书样本-专业版-事业与职场.pdf'
```

Expected:

- backend test suite passes;
- plain PDF is 9–12 pages;
- professional PDF is 13–16 pages;
- both PDFs report Apache FOP as creator;
- neither is encrypted and neither contains JavaScript;
- every page has extractable Chinese text.

**Step 6: Manual content acceptance**

Ask the reviewer to answer these questions before proceeding to API/payment work:

1. Can the selected topic be identified from any random body page?
2. Does each year provide a visibly different priority and reason?
3. Is the plain version understandable without BaZi knowledge?
4. Does the professional version expose evidence and limitations rather than merely adding jargon?
5. Do the two prices still feel proportionate to the resulting value?

**Step 7: Documentation and review checkpoint**

Update `docs/design/commercial-design.md` to state that only four themes are reviewed for launch and that all reports use a three-year annual timeline.

Optional commit after explicit approval:

```bash
git add apps/server/src/test docs/samples docs/design/commercial-design.md
git commit -m "test(report): verify three-year themed samples"
```

---

## Recommended execution checkpoints

1. After Task 2: inspect the 2026–2028 annual facts before interpreting them.
2. After Task 4: review debug JSON for all four topics before writing presentation copy.
3. After Task 5: review plain/professional text without PDF styling to isolate content quality.
4. After Task 6: review visual density and page count.
5. After Task 7: decide whether the content is strong enough to proceed to report persistence and payment integration.

## Explicitly deferred

- Monthly predictions.
- More than four launch topics.
- Definitive Yongshen or mixed-school conclusions.
- Exact event, income, health, marriage or breakup predictions.
- LLM-generated report prose.
- Report APIs, entitlement persistence, payment fulfillment and frontend purchase flow.
- Real payment providers.
