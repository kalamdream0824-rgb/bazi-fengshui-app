package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.scoredCase;
import static com.bazi.app.report.wealth.WealthRemediationFixtures.withPathWeights;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    String review = String.join("", timeline.past().checkpoints());

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

    assertTrue(timeline.present().judgment().contains("额外进账"));
    assertTrue(timeline.present().priority().contains("结余"));
    assertEquals(2, timeline.future().size());
    assertEquals(2, timeline.future().stream().map(NarrativeTimeline.FutureStep::headline).distinct().count());
    assertEquals(2, timeline.future().stream().map(NarrativeTimeline.FutureStep::action).distinct().count());
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

  private static Stream<Arguments> pathReviewCases() {
    return Stream.of(
        Arguments.of(WealthPath.STABLE_INCOME, List.of("收入来源", "到账")),
        Arguments.of(WealthPath.SKILL_INCOME, List.of("服务", "付钱")),
        Arguments.of(WealthPath.PROJECT_INCOME, List.of("回款", "成本")),
        Arguments.of(WealthPath.COOPERATION_INCOME, List.of("分账", "共同开销")),
        Arguments.of(WealthPath.RETENTION, List.of("支出", "留下")));
  }

  private record Generated(WealthAssessment previous, WealthNarrativeV3 content) {}
}
