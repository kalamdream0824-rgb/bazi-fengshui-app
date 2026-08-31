package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CareerTimelinePlannerTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-09-01T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private PaipanRequest request;
  private PaipanResultDto chart;
  private AnnualPeriodAssessor assessor;
  private AnnualContextFactory contextFactory;
  private CareerTimelinePlanner planner;

  @BeforeEach
  void setUp() {
    request = new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
    chart = new BaziService().paipan(request);
    assessor = new AnnualPeriodAssessor(CLOCK, new AnnualRuleCatalog());
    contextFactory = new AnnualContextFactory(CLOCK);
    planner = new CareerTimelinePlanner();
  }

  @Test
  void writesEmployedReviewAroundResponsibilitiesAndResults() {
    assertReviewObjects(
        CareerContext.fromCodes("employed", "promotion", "smooth"),
        List.of("职责", "成果"));
  }

  @Test
  void writesSelfEmployedReviewAroundClientsAndPayments() {
    assertReviewObjects(
        CareerContext.fromCodes("self_employed", "stability", "high_pressure"),
        List.of("客户", "回款"));
  }

  @Test
  void writesJobSeekingReviewAroundApplicationsAndInterviews() {
    assertReviewObjects(
        CareerContext.fromCodes("job_seeking", "job_change", "stalled"),
        List.of("投递", "面试"));
  }

  @Test
  void writesStudyingReviewAroundLearningAndPortfolioWork() {
    assertReviewObjects(
        CareerContext.fromCodes("studying", "transition", "preparing_change"),
        List.of("学习", "作品"));
  }

  @Test
  void usesAllThreeCareerAnswersForThePresentPriority() {
    List<CareerContext> contexts = List.of(
        CareerContext.fromCodes("employed", "promotion", "smooth"),
        CareerContext.fromCodes("self_employed", "stability", "high_pressure"),
        CareerContext.fromCodes("job_seeking", "job_change", "stalled"),
        CareerContext.fromCodes("studying", "transition", "preparing_change"));

    List<String> priorities = contexts.stream()
        .map(this::timeline)
        .map(value -> value.present().priority())
        .toList();

    assertTrue(priorities.get(0).contains("工作进展顺利"));
    assertTrue(priorities.get(0).contains("升职"));
    assertTrue(priorities.get(1).contains("经营压力较大"));
    assertTrue(priorities.get(1).contains("稳定"));
    assertTrue(priorities.get(2).contains("求职进展不顺"));
    assertTrue(priorities.get(2).contains("工作"));
    assertTrue(priorities.get(3).contains("准备转换方向"));
    assertTrue(priorities.get(3).contains("新方向"));
  }

  @Test
  void assignsEvidenceToTheYearThatProducedEachTimelineSection() {
    CareerContext context = CareerContext.fromCodes("job_seeking", "job_change", "stalled");
    Inputs inputs = inputs(context);
    NarrativeTimeline timeline = planner.plan(inputs.planningInput());

    assertEvidenceBelongsTo(inputs.previous(), timeline.past().evidenceKeys());
    assertEvidenceBelongsTo(inputs.productYears().get(0), timeline.present().evidenceKeys());
    assertEvidenceBelongsTo(inputs.productYears().get(1), timeline.future().get(0).evidenceKeys());
  }

  @Test
  void producesOneFutureActionWithoutRepeatingDetailedCareerCopy() {
    CareerContext context = CareerContext.fromCodes("job_seeking", "job_change", "stalled");
    Inputs inputs = inputs(context);
    NarrativeTimeline timeline = planner.plan(inputs.planningInput());
    CareerNarrativePlan detailed = new CareerNarrativePlanner().plan(
        ThreeYearAssessment.from(inputs.assessment()), context);

    assertEquals(1, timeline.future().size());
    assertEquals(2027, timeline.future().get(0).year());
    Set<String> detailedCopy = new LinkedHashSet<>();
    for (CareerNarrativePlan.YearNarrative year : detailed.years()) {
      detailedCopy.add(year.verdict());
      detailedCopy.add(year.obstacle());
      detailedCopy.addAll(year.actions());
    }
    for (String copy : timelineCopy(timeline)) {
      assertFalse(detailedCopy.contains(copy), copy);
    }
  }

  private void assertReviewObjects(CareerContext context, List<String> expectedObjects) {
    NarrativeTimeline timeline = timeline(context);
    String review = String.join("", timeline.past().checkpoints());

    assertEquals(2025, timeline.past().year());
    assertEquals(2026, timeline.present().year());
    assertEquals(List.of(2027), timeline.future().stream()
        .map(NarrativeTimeline.FutureStep::year)
        .toList());
    assertEquals(2, timeline.past().checkpoints().size());
    expectedObjects.forEach(value -> assertTrue(review.contains(value), value + " in " + review));
    assertFalse(review.contains("工作变化"), review);
  }

  private NarrativeTimeline timeline(CareerContext context) {
    return planner.plan(inputs(context).planningInput());
  }

  private Inputs inputs(CareerContext context) {
    AnnualContext previousContext = contextFactory.createYear(request, chart, 2025);
    YearAssessment previous = assessor.assessYear(
        previousContext, ReportTopic.CAREER, context);
    AnnualPeriodAssessment assessment = assessor.assess(
        request, chart, ReportTopic.CAREER, ReportHorizon.of(2), context);
    return new Inputs(previous, assessment.years(), assessment, context);
  }

  private void assertEvidenceBelongsTo(YearAssessment year, List<String> timelineEvidence) {
    Set<String> available = new LinkedHashSet<>();
    Stream.of(year.evidence(), year.counterEvidence(), year.contextEvidence())
        .flatMap(List::stream)
        .map(ReportEvidence::key)
        .forEach(available::add);
    year.ruleKeys().stream().map(key -> "rule." + key).forEach(available::add);

    assertFalse(timelineEvidence.isEmpty());
    assertTrue(available.containsAll(timelineEvidence), timelineEvidence + " not in " + available);
  }

  private List<String> timelineCopy(NarrativeTimeline timeline) {
    return Stream.concat(
        Stream.of(
            timeline.past().headline(),
            timeline.past().bridge(),
            timeline.present().headline(),
            timeline.present().judgment(),
            timeline.present().priority()),
        Stream.concat(
            timeline.past().checkpoints().stream(),
            timeline.future().stream().flatMap(step -> Stream.of(step.headline(), step.action()))))
        .toList();
  }

  private record Inputs(
      YearAssessment previous,
      List<YearAssessment> productYears,
      AnnualPeriodAssessment assessment,
      CareerContext context) {

    NarrativeTimelinePlanner.Input<YearAssessment, CareerContext> planningInput() {
      return new NarrativeTimelinePlanner.Input<>(
          ReportTopic.CAREER.code(), previous, productYears, Optional.of(context));
    }
  }
}
