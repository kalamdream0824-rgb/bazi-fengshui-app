package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.scoredCase;
import static com.bazi.app.report.wealth.WealthRemediationFixtures.withPathWeights;
import static com.bazi.app.report.wealth.WealthRetrospectiveSignalExtractorTest.assessment;
import static com.bazi.app.report.wealth.WealthRetrospectiveSignalExtractorTest.source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.wealth.v3.DefaultWealthReportGenerator;
import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3;
import com.bazi.app.report.wealth.v3.WealthNarrativeWriter;
import com.bazi.app.report.wealth.v3.WealthTimelinePlanner;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WealthTimelinePlannerTest {

  private static final LocalDate AS_OF = LocalDate.of(2026, 9, 2);
  private final WealthNarrativeWriter writer = new WealthNarrativeWriter();
  private final WealthTimelinePlanner planner = new WealthTimelinePlanner();

  @ParameterizedTest
  @MethodSource("pathReviewCases")
  void pastReviewUsesMoneySpecificObjectsForAllFivePaths(
      WealthPath path,
      List<String> expectedObjects) throws Exception {
    Generated generated = generated(path);

    NarrativeTimeline timeline = planner.plan(generated.previous(), generated.content());
    String review = visiblePast(timeline.past());

    assertEquals(2025, timeline.past().year());
    assertEquals(2026, timeline.present().year());
    assertEquals(List.of(2027, 2028), timeline.future().stream()
        .map(NarrativeTimeline.FutureStep::year)
        .toList());
    expectedObjects.forEach(value -> assertTrue(review.contains(value), value + " in " + review));
    for (String careerCopy : List.of("职责", "成果", "岗位", "升职", "工作变化", "具体金额")) {
      assertFalse(review.contains(careerCopy), careerCopy + " in " + review);
    }
  }

  @Test
  void currentReadingUsesFocusAndRetentionWhileFutureYearsUseDistinctActions() throws Exception {
    Generated generated = generated(WealthPath.PROJECT_INCOME);

    NarrativeTimeline timeline = planner.plan(generated.previous(), generated.content());

    assertTrue(timeline.present().judgment().contains("到账节奏"));
    assertTrue(timeline.present().priority().contains("结余"));
    assertEquals(2, timeline.future().size());
    assertEquals(2, timeline.future().stream().map(NarrativeTimeline.FutureStep::headline).distinct().count());
    assertEquals(2, timeline.future().stream().map(NarrativeTimeline.FutureStep::action).distinct().count());
  }

  @Test
  void pastReviewPresentsOnePrimaryOneSecondaryAndOneHiddenEffect() throws Exception {
    Generated generated = generated(WealthPath.PROJECT_INCOME);

    NarrativeTimeline.PastReview past = planner.plan(generated.previous(), generated.content()).past();

    assertTrue(past.headline().startsWith("主判断："), past.headline());
    assertTrue(past.checkpoints().get(0).startsWith("次判断："), past.checkpoints().toString());
    assertTrue(past.checkpoints().get(1).startsWith("隐性影响："), past.checkpoints().toString());
  }

  @Test
  void samePrimaryPathWithDifferentSecondaryAndHiddenEvidenceChangesTheCompleteReview()
      throws Exception {
    WealthNarrativeV3 content = generated(WealthPath.PROJECT_INCOME).content();
    WealthAssessment first = assessment(List.of(
        source("project-a", "project_income", "annual.stem.ten_god", 8,
            EvidenceFamily.ANNUAL_TRIGGER),
        source("skill-a", "skill_income", "natal.combination.output_wealth", 4,
            EvidenceFamily.NATAL_COMBINATION),
        source("retention-a", "retention", "annual.branch.harm.day", -2,
            EvidenceFamily.ANNUAL_TRIGGER)));
    WealthAssessment second = assessment(List.of(
        source("project-b", "project_income", "annual.stem.ten_god", 8,
            EvidenceFamily.ANNUAL_TRIGGER),
        source("stable-b", "stable_income", "dayun.stem.ten_god", 5,
            EvidenceFamily.DAYUN_CONTEXT),
        source("retention-b", "retention", "annual.branch.clash.day", -3,
            EvidenceFamily.ANNUAL_TRIGGER)));

    assertEquals(List.of("project_income"), first.focus().primaryCandidates());
    assertEquals(List.of("project_income"), second.focus().primaryCandidates());
    assertNotEquals(visiblePast(planner.plan(first, content).past()),
        visiblePast(planner.plan(second, content).past()));
  }

  @Test
  void sameAssessmentProducesTheSameTimelineOneHundredTimes() throws Exception {
    Generated generated = generated(WealthPath.COOPERATION_INCOME);
    NarrativeTimeline expected = planner.plan(generated.previous(), generated.content());

    for (int index = 0; index < 100; index++) {
      assertEquals(expected, planner.plan(generated.previous(), generated.content()));
    }
  }

  @ParameterizedTest
  @MethodSource("pathReviewCases")
  void entireTimelineAvoidsUnprovenOccupationAndIncomeSource(
      WealthPath path,
      List<String> ignoredExpectedObjects) throws Exception {
    Generated generated = generated(path);
    NarrativeTimeline timeline = planner.plan(generated.previous(), generated.content());
    String visibleCopy = String.join("\n",
        timeline.past().headline(),
        String.join("\n", timeline.past().checkpoints()),
        timeline.past().bridge(),
        timeline.present().headline(),
        timeline.present().judgment(),
        timeline.present().priority(),
        timeline.future().stream()
            .flatMap(step -> Stream.of(step.headline(), step.action()))
            .reduce("", (left, right) -> left + "\n" + right));

    for (String word : List.of(
        "工资", "加薪", "客户", "项目", "订单", "接单", "接活", "服务",
        "报价", "回款", "生意", "手艺", "按单", "按次", "合作收入")) {
      assertFalse(visibleCopy.contains(word), path + " inferred " + word + ":\n" + visibleCopy);
    }
  }

  @Test
  void timelineEvidenceBelongsToItsOwnAssessmentYear() throws Exception {
    Generated generated = generated(WealthPath.COOPERATION_INCOME);
    NarrativeTimeline timeline = planner.plan(generated.previous(), generated.content());

    assertEvidenceBelongsTo(generated.previous(), timeline.past().evidenceKeys());
    for (int index = 0; index < generated.content().years().size(); index++) {
      WealthNarrativeV3.Year year = generated.content().years().get(index);
      List<String> keys = index == 0
          ? timeline.present().evidenceKeys()
          : timeline.future().get(index - 1).evidenceKeys();
      assertEvidenceBelongsTo(year, keys);
    }
  }

  @Test
  void generatorEvaluatesPreviousYearWithoutAddingItToTheSoldThreeYears() {
    PaipanRequest request = new PaipanRequest(
        "林先生", "male", "1995-10-08T14:30:00", "上海", false);
    var chart = new BaziService().paipan(request);
    ZoneId zone = ZoneId.of("Asia/Shanghai");
    Clock clock = Clock.fixed(AS_OF.atStartOfDay(zone).toInstant(), zone);
    AnnualContextFactory factory = new AnnualContextFactory(clock);

    WealthNarrativeV3 content = new DefaultWealthReportGenerator().generate(
        chart,
        factory.createYear(request, chart, 2025),
        factory.create(request, chart, ReportHorizon.WEALTH_PRODUCT),
        AS_OF);

    assertNotNull(content.timeline());
    assertEquals(3, content.horizonYears());
    assertEquals(List.of(2026, 2027, 2028), content.years().stream()
        .map(WealthNarrativeV3.Year::year)
        .toList());
    assertEquals(2025, content.timeline().past().year());
  }

  private Generated generated(WealthPath path) throws Exception {
    var period = withPathWeights(scoredCase("S01"), path, 8, 0);
    List<WealthAssessment> productYears = WealthNarrativeV3Test.assessments(period);
    WealthNarrativeV3 content = writer.plan(productYears, AS_OF);
    return new Generated(asPrevious(productYears.get(0)), content);
  }

  private WealthAssessment asPrevious(WealthAssessment source) {
    List<WealthAssessment.Decision> decisions = source.decisions().stream()
        .map(decision -> new WealthAssessment.Decision(
            "2025.decision." + decision.path(),
            2025,
            decision.path(),
            decision.supportWeight(),
            decision.limitationWeight(),
            decision.netWeight(),
            decision.stance(),
            decision.strength(),
            decision.supportingEvidenceIds(),
            decision.limitingEvidenceIds(),
            decision.reasonCodes()))
        .toList();
    WealthAssessment.Risk risk = source.risk() == null
        ? null
        : new WealthAssessment.Risk(source.risk().path(), source.risk().limitingEvidenceIds());
    return new WealthAssessment(
        2025,
        "乙巳",
        source.facts(),
        source.evidence(),
        decisions,
        source.focus(),
        risk);
  }

  private void assertEvidenceBelongsTo(WealthAssessment year, List<String> keys) {
    Set<String> available = new LinkedHashSet<>();
    year.facts().stream().map(WealthAssessment.Fact::id).forEach(available::add);
    year.evidence().stream().map(WealthAssessment.Evidence::factKey).forEach(available::add);
    assertFalse(keys.isEmpty());
    assertTrue(available.containsAll(keys), keys + " not in " + available);
  }

  private void assertEvidenceBelongsTo(WealthNarrativeV3.Year year, List<String> keys) {
    Set<String> available = new LinkedHashSet<>();
    year.facts().stream().map(WealthAssessment.Fact::id).forEach(available::add);
    year.evidence().stream().map(WealthAssessment.Evidence::factKey).forEach(available::add);
    assertFalse(keys.isEmpty());
    assertTrue(available.containsAll(keys), keys + " not in " + available);
  }

  private String visiblePast(NarrativeTimeline.PastReview past) {
    return String.join("\n", past.headline(), String.join("\n", past.checkpoints()), past.bridge());
  }

  private static Stream<Arguments> pathReviewCases() {
    return Stream.of(
        Arguments.of(WealthPath.STABLE_INCOME, List.of("进账", "持续")),
        Arguments.of(WealthPath.SKILL_INCOME, List.of("投入", "进账")),
        Arguments.of(WealthPath.PROJECT_INCOME, List.of("预计进账", "实际到账")),
        Arguments.of(WealthPath.COOPERATION_INCOME, List.of("他人", "责任")),
        Arguments.of(WealthPath.RETENTION, List.of("留下", "结余")));
  }

  private record Generated(WealthAssessment previous, WealthNarrativeV3 content) {}
}
