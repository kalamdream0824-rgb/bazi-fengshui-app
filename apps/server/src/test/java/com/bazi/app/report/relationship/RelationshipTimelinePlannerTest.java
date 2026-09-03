package com.bazi.app.report.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.NarrativeTimelinePlanner;
import com.bazi.app.report.ReportTopic;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RelationshipTimelinePlannerTest {

  private final RelationshipTimelinePlanner planner = new RelationshipTimelinePlanner();

  @Test
  void datingReviewChecksDateResponseAndRelationshipConfirmation() {
    Window window = window(false);

    NarrativeTimeline timeline = plan(window, RelationshipStatus.DATING);
    String past = String.join("", timeline.past().checkpoints());

    assertTrue(past.contains("约会"), past);
    assertTrue(past.contains("回应"), past);
    assertTrue(past.contains("关系"), past);
    assertTrue(past.contains("确认"), past);
    assertSafeReview(timeline);
  }

  @Test
  void marriedReviewChecksSharedLifeAndResponsibilityAllocation() {
    Window window = window(false);

    NarrativeTimeline timeline = plan(window, RelationshipStatus.MARRIED);
    String past = String.join("", timeline.past().checkpoints());

    assertTrue(past.contains("共同生活"), past);
    assertTrue(past.contains("责任分配"), past);
    assertSafeReview(timeline);
  }

  @Test
  void presentUsesCurrentFiveDimensionFocusAndRealWorldPriority() {
    Window window = window(false);

    NarrativeTimeline timeline = plan(window, RelationshipStatus.DATING);

    assertTrue(timeline.present().headline().contains("相处联系"), timeline.present().headline());
    assertTrue(timeline.present().priority().contains("边界"), timeline.present().priority());
    assertEvidenceBelongsTo(window.product().years().get(0), timeline.present().evidenceKeys());
  }

  @Test
  void relationshipTimelineDoesNotUseWorkplaceProgressJargon() {
    Window window = window(false);

    for (RelationshipStatus status : List.of(RelationshipStatus.DATING, RelationshipStatus.MARRIED)) {
      NarrativeTimeline timeline = plan(window, status);
      String text = timeline.present().headline()
          + timeline.present().judgment()
          + timeline.present().priority();

      assertFalse(text.contains("推进"), status + ": " + text);
    }
  }

  @Test
  void futureYearsUseDifferentVerbsAndObjectsEvenWhenFocusRepeats() {
    Window window = window(true);

    NarrativeTimeline timeline = plan(window, RelationshipStatus.DATING);
    List<NarrativeTimeline.FutureStep> future = timeline.future();

    assertEquals(2, future.size());
    assertTrue(future.get(0).action().startsWith("安排"), future.get(0).action());
    assertTrue(future.get(0).action().contains("相处时间"), future.get(0).action());
    assertTrue(future.get(1).action().startsWith("确认"), future.get(1).action());
    assertTrue(future.get(1).action().contains("联系节奏"), future.get(1).action());
    assertFalse(future.get(0).action().equals(future.get(1).action()));
  }

  @Test
  void everyTimelineSectionKeepsEvidenceFromItsOwnYear() {
    Window window = window(false);
    NarrativeTimeline timeline = plan(window, RelationshipStatus.MARRIED);

    assertEvidenceBelongsTo(window.previous(), timeline.past().evidenceKeys());
    assertEvidenceBelongsTo(window.product().years().get(0), timeline.present().evidenceKeys());
    for (int index = 0; index < timeline.future().size(); index++) {
      assertEvidenceBelongsTo(
          window.product().years().get(index + 1),
          timeline.future().get(index).evidenceKeys());
    }
  }

  @Test
  void rejectsSingleStatusBecauseItHasItsOwnTwoYearProduct() {
    Window window = window(false);

    assertThrows(IllegalArgumentException.class,
        () -> plan(window, RelationshipStatus.SINGLE));
  }

  private NarrativeTimeline plan(Window window, RelationshipStatus status) {
    return planner.plan(new NarrativeTimelinePlanner.Input<>(
        ReportTopic.RELATIONSHIP.code(),
        window.previous(),
        window.product().years(),
        Optional.of(status)));
  }

  private void assertSafeReview(NarrativeTimeline timeline) {
    String review = timeline.past().headline()
        + String.join("", timeline.past().checkpoints())
        + timeline.past().bridge();
    for (String forbidden : List.of("分手", "结婚", "背叛", "第三者", "事实证明", "准确命中")) {
      assertFalse(review.contains(forbidden), forbidden + " in " + review);
    }
  }

  private void assertEvidenceBelongsTo(
      RelationshipPeriodEvaluation.Year year,
      List<String> keys) {
    Set<String> available = new LinkedHashSet<>();
    year.dimensions().values().stream()
        .flatMap(dimension -> dimension.evidence().stream())
        .map(RelationshipEvidence::key)
        .forEach(available::add);
    assertFalse(keys.isEmpty());
    assertTrue(available.containsAll(keys), keys + " not in " + available);
  }

  private Window window(boolean repeatedFutureFocus) {
    List<RelationshipYearEvaluation> all = List.of(
        year(2025, weights(1, 6, 0, 1, 5), weights(0, 0, 0, 2, 0)),
        year(2026, weights(6, 4, 1, 0, 2), weights(0, 0, 0, 5, 0)),
        repeatedFutureFocus
            ? year(2027, weights(6, 4, 1, 0, 2), weights(0, 0, 0, 3, 0))
            : year(2027, weights(1, 2, 6, 0, 5), weights(0, 0, 0, 3, 0)),
        repeatedFutureFocus
            ? year(2028, weights(6, 4, 1, 0, 2), weights(0, 0, 0, 3, 0))
            : year(2028, weights(2, 1, 3, 6, 4), weights(0, 0, 0, 1, 0)));
    RelationshipPeriodArbitrator arbitrator = new RelationshipPeriodArbitrator();
    RelationshipPeriodEvaluation analysis = arbitrator.arbitrate(all);
    RelationshipPeriodEvaluation product = arbitrator.arbitrate(all.subList(1, all.size()));
    return new Window(analysis.years().get(0), product);
  }

  private RelationshipYearEvaluation year(
      int year,
      Map<RelationshipDimension, Integer> support,
      Map<RelationshipDimension, Integer> limitation) {
    Map<RelationshipDimension, RelationshipDimensionEvaluation> dimensions =
        new EnumMap<>(RelationshipDimension.class);
    for (RelationshipDimension dimension : RelationshipDimension.values()) {
      int supportWeight = support.get(dimension);
      int limitationWeight = limitation.get(dimension);
      List<RelationshipEvidence> evidence = new ArrayList<>();
      if (supportWeight > 0) {
        evidence.add(evidence(year + "." + dimension.code() + ".support", dimension, supportWeight));
      }
      if (limitationWeight > 0) {
        evidence.add(evidence(year + "." + dimension.code() + ".limit", dimension, -limitationWeight));
      }
      dimensions.put(dimension, new RelationshipDimensionEvaluation(
          dimension,
          supportWeight,
          limitationWeight,
          supportWeight + limitationWeight,
          supportWeight - limitationWeight,
          RelationshipDimensionEvaluation.toneFor(supportWeight, limitationWeight),
          evidence));
    }
    return new RelationshipYearEvaluation(year, "丙午", dimensions);
  }

  private Map<RelationshipDimension, Integer> weights(
      int connection,
      int response,
      int dailyCooperation,
      int boundaries,
      int stability) {
    Map<RelationshipDimension, Integer> result = new EnumMap<>(RelationshipDimension.class);
    result.put(RelationshipDimension.CONNECTION, connection);
    result.put(RelationshipDimension.RESPONSE, response);
    result.put(RelationshipDimension.DAILY_COOPERATION, dailyCooperation);
    result.put(RelationshipDimension.BOUNDARIES, boundaries);
    result.put(RelationshipDimension.STABILITY, stability);
    return result;
  }

  private RelationshipEvidence evidence(
      String key,
      RelationshipDimension dimension,
      int weight) {
    return new RelationshipEvidence(
        key,
        RelationshipEvidenceFamily.ANNUAL_TRIGGER,
        key,
        dimension,
        weight,
        List.of("source." + key));
  }

  private record Window(
      RelationshipPeriodEvaluation.Year previous,
      RelationshipPeriodEvaluation product) {}
}
