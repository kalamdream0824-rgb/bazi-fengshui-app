package com.bazi.app.report.overall.v3;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualActionGuide;
import com.bazi.app.report.AnnualActionGuidePolicy;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ConfidenceLevel;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OverallV3NarrativePlannerTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private List<OverallTopicSnapshot> productSnapshots;
  private List<OverallAnnualDecision> productDecisions;
  private List<OverallTopicSnapshot> previousSnapshots;
  private OverallAnnualDecision previousDecision;

  @BeforeEach
  void setUp() {
    PaipanRequest request = new PaipanRequest(
        "综合正文样本", "male", "1995-10-08T14:30:00", "上海", false);
    PaipanResultDto chart = new BaziService().paipan(request);
    AnnualContextFactory contextFactory = new AnnualContextFactory(CLOCK);
    List<AnnualContext> product = contextFactory.create(request, chart, ReportHorizon.of(3));
    AnnualContext previous = contextFactory.createYear(request, chart, product.get(0).year() - 1);
    OverallSnapshotFactory snapshotFactory = new OverallSnapshotFactory(CLOCK);
    OverallDecisionArbitrator arbitrator = new OverallDecisionArbitrator();
    List<AnnualContext> fullWindow = new ArrayList<>();
    fullWindow.add(previous);
    fullWindow.addAll(product);
    List<OverallTopicSnapshot> fullSnapshots = snapshotFactory.create(request, chart, fullWindow);
    List<OverallAnnualDecision> fullDecisions = arbitrator.arbitratePeriod(fullSnapshots);
    previousSnapshots = fullSnapshots.stream()
        .filter(snapshot -> snapshot.year() == previous.year())
        .toList();
    productSnapshots = fullSnapshots.stream()
        .filter(snapshot -> snapshot.year() != previous.year())
        .toList();
    previousDecision = fullDecisions.get(0);
    productDecisions = fullDecisions.subList(1, fullDecisions.size());
  }

  @Test
  void createsOneActionLoopAndTwoCompressedObservationsForEveryProductYear() {
    OverallV3NarrativePlan plan = new OverallV3NarrativePlanner()
        .plan(productSnapshots, productDecisions);

    assertEquals(3, plan.horizonYears());
    assertEquals(3, plan.years().size());
    assertNull(plan.timeline());
    assertFalse(plan.thesis().isBlank());
    assertFalse(plan.summary().isBlank());
    for (int index = 0; index < plan.years().size(); index++) {
      OverallV3NarrativePlan.YearNarrative year = plan.years().get(index);
      OverallAnnualDecision decision = productDecisions.get(index);
      assertEquals(decision.year(), year.year());
      assertEquals(code(decision.primary().topic()), year.primaryCode());
      assertEquals(code(decision.secondary().topic()), year.secondaryCode());
      assertEquals(decision.decisionKey(), year.decisionKey());
      assertEquals(decision.conflictKey(), year.conflictKey());
      assertFalse(year.linkage().isBlank());
      assertFalse(year.transition().isBlank());
      assertEquals(2, year.observations().size());
      Set<String> expectedObservations = Set.of(OverallTopicSnapshot.Topic.values()).stream()
          .filter(topic -> topic != decision.primary().topic())
          .filter(topic -> topic != decision.secondary().topic())
          .map(this::code)
          .collect(Collectors.toSet());
      assertEquals(expectedObservations, year.observations().stream()
          .map(OverallV3NarrativePlan.Observation::topicCode)
          .collect(Collectors.toSet()));
      assertTrue(year.evidenceKeys().containsAll(decision.evidenceKeys()));
    }
    assertDoesNotThrow(() -> AnnualActionGuidePolicy.validate(plan.years().stream()
        .map(OverallV3NarrativePlan.YearNarrative::actionGuide)
        .toList()));
  }

  @Test
  void repeatedCalculationProducesTheSameStrictPlan() {
    OverallV3NarrativePlanner planner = new OverallV3NarrativePlanner();

    assertEquals(
        planner.plan(productSnapshots, productDecisions),
        planner.plan(productSnapshots, productDecisions));
  }

  @Test
  void aRealPrimaryMayRemainTheSameForAllThreeYears() {
    List<OverallTopicSnapshot> snapshots = new ArrayList<>();
    List<OverallAnnualDecision> decisions = new ArrayList<>();
    List<String> careerFocuses = List.of(
        "career.visibility", "career.preparation", "career.responsibility_upgrade");
    for (int offset = 0; offset < 3; offset++) {
      int year = 2026 + offset;
      List<OverallTopicSnapshot> annual = annualSnapshots(year, careerFocuses.get(offset));
      snapshots.addAll(annual);
      decisions.add(new OverallDecisionArbitrator().arbitrate(annual));
    }

    OverallV3NarrativePlan plan = new OverallV3NarrativePlanner().plan(snapshots, decisions);

    assertEquals(List.of("career", "career", "career"), plan.years().stream()
        .map(OverallV3NarrativePlan.YearNarrative::primaryCode)
        .toList());
    assertTrue(plan.thesis().contains("三年"), plan.thesis());
    assertEquals(3, plan.years().stream()
        .map(OverallV3NarrativePlan.YearNarrative::headline).distinct().count());
  }

  @Test
  void previousYearAddsAnEvidenceOwnedTimelineWithoutCopyingAnnualActionLines() {
    OverallV3NarrativePlan plan = new OverallV3NarrativePlanner().plan(
        productSnapshots, productDecisions, previousSnapshots, previousDecision);

    assertNotNull(plan.timeline());
    assertEquals(productDecisions.get(0).year() - 1, plan.timeline().past().year());
    assertEquals(previousDecision.evidenceKeys(), plan.timeline().past().evidenceKeys());
    assertEquals(productDecisions.get(0).year(), plan.timeline().present().year());
    assertEquals(List.of(productDecisions.get(1).year(), productDecisions.get(2).year()),
        plan.timeline().future().stream().map(step -> step.year()).toList());
    List<String> annualLines = plan.years().stream()
        .flatMap(year -> year.actionGuide().lines().stream())
        .toList();
    List<String> timelineLines = new ArrayList<>();
    timelineLines.add(plan.timeline().present().headline());
    timelineLines.add(plan.timeline().present().judgment());
    timelineLines.add(plan.timeline().present().priority());
    plan.timeline().future().forEach(step -> {
      timelineLines.add(step.headline());
      timelineLines.add(step.action());
    });
    assertTrue(timelineLines.stream().noneMatch(annualLines::contains));
  }

  @Test
  void differentCalculationFocusesDoNotShareOneActionDecisionTuple() {
    List<OverallTopicSnapshot> first = annualSnapshots(2026, "career.visibility");
    List<OverallTopicSnapshot> second = annualSnapshots(2026, "career.preparation");
    OverallDecisionArbitrator arbitrator = new OverallDecisionArbitrator();
    AnnualActionGuide firstGuide = new OverallActionGuideWriter().write(arbitrator.arbitrate(first));
    AnnualActionGuide secondGuide = new OverallActionGuideWriter().write(arbitrator.arbitrate(second));

    assertNotEquals(tuple(firstGuide), tuple(secondGuide));
  }

  private List<OverallTopicSnapshot> annualSnapshots(int year, String careerFocus) {
    return List.of(
        snapshot(year, OverallTopicSnapshot.Topic.RHYTHM, "rhythm.balanced",
            OverallTopicSnapshot.Stance.BALANCED, OverallTopicSnapshot.Urgency.LOW,
            ConfidenceLevel.MEDIUM, 1),
        snapshot(year, OverallTopicSnapshot.Topic.CAREER, careerFocus,
            OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.HIGH,
            ConfidenceLevel.HIGH, 2),
        snapshot(year, OverallTopicSnapshot.Topic.WEALTH, "wealth.retention.mixed",
            OverallTopicSnapshot.Stance.MIXED, OverallTopicSnapshot.Urgency.MEDIUM,
            ConfidenceLevel.MEDIUM, 1),
        snapshot(year, OverallTopicSnapshot.Topic.RELATIONSHIP, "relationship.response.mixed",
            OverallTopicSnapshot.Stance.MIXED, OverallTopicSnapshot.Urgency.LOW,
            ConfidenceLevel.MEDIUM, 1));
  }

  private OverallTopicSnapshot snapshot(
      int year,
      OverallTopicSnapshot.Topic topic,
      String focus,
      OverallTopicSnapshot.Stance stance,
      OverallTopicSnapshot.Urgency urgency,
      ConfidenceLevel confidence,
      int directEvidence) {
    String code = code(topic);
    return new OverallTopicSnapshot(
        year,
        topic,
        focus,
        stance,
        urgency,
        confidence,
        directEvidence,
        stance == OverallTopicSnapshot.Stance.PRESSURED ? null : code + ".opportunity",
        stance == OverallTopicSnapshot.Stance.PRESSURED ? code + ".risk" : null,
        List.of(focus + ".act", focus + ".fallback"),
        List.of("annual." + code + ".direct", "natal." + code + ".base"));
  }

  private String tuple(AnnualActionGuide guide) {
    return guide.action() + "|" + guide.expectedChange() + "|" + guide.successSignal();
  }

  private String code(OverallTopicSnapshot.Topic topic) {
    return topic.name().toLowerCase(java.util.Locale.ROOT);
  }
}
