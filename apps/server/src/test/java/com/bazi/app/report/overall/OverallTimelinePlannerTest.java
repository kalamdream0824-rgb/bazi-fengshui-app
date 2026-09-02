package com.bazi.app.report.overall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.NarrativeTimelinePlanner;
import com.bazi.app.report.ReportTopic;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OverallTimelinePlannerTest {

  private final OverallTimelinePlanner planner = new OverallTimelinePlanner();

  @Test
  void pastReviewNamesOneMainAreaAndOneLinkedAreaWithConcreteObjects() {
    Window window = window();

    NarrativeTimeline timeline = plan(window);
    String review = timeline.past().headline()
        + String.join("", timeline.past().checkpoints())
        + timeline.past().bridge();

    assertTrue(review.contains("工作"), review);
    assertTrue(review.contains("钱财") || review.contains("收支"), review);
    assertFalse(review.contains("整体情况"), review);
    assertFalse(review.contains("综合层面"), review);
    assertFalse(review.contains("事实证明"), review);
    assertFalse(review.contains("准确命中"), review);
  }

  @Test
  void presentSaysWhatToHandleFirstAndUsesCurrentPrimaryEvidence() {
    Window window = window();

    NarrativeTimeline timeline = plan(window);

    assertTrue(timeline.present().headline().contains("时间")
        || timeline.present().headline().contains("精力"));
    assertTrue(timeline.present().priority().startsWith("先"), timeline.present().priority());
    assertEvidenceBelongsTo(window.product().years().get(0), timeline.present().evidenceKeys());
  }

  @Test
  void futureYearsUseDifferentFocusesWhenAnnualPrimaryRepeats() {
    Window window = window();

    List<NarrativeTimeline.FutureStep> future = plan(window).future();

    assertEquals(2, future.size());
    assertTrue(future.get(0).headline().contains("工作"), future.get(0).headline());
    assertTrue(future.get(1).headline().contains("关系"), future.get(1).headline());
    assertFalse(future.get(0).headline().equals(future.get(1).headline()));
    assertFalse(future.get(0).action().equals(future.get(1).action()));
  }

  @Test
  void everySectionKeepsEvidenceFromItsOwnYear() {
    Window window = window();
    NarrativeTimeline timeline = plan(window);

    assertEvidenceBelongsTo(window.previous(), timeline.past().evidenceKeys());
    assertEvidenceBelongsTo(window.product().years().get(0), timeline.present().evidenceKeys());
    for (int index = 0; index < timeline.future().size(); index++) {
      assertEvidenceBelongsTo(
          window.product().years().get(index + 1),
          timeline.future().get(index).evidenceKeys());
    }
  }

  private NarrativeTimeline plan(Window window) {
    return planner.plan(new NarrativeTimelinePlanner.Input<>(
        ReportTopic.OVERALL.code(),
        window.previous(),
        window.product().years(),
        Optional.empty()));
  }

  private Window window() {
    OverallYearEvaluation previous = year(
        2025, OverallDimension.CAREER, OverallDimension.WEALTH);
    List<OverallYearEvaluation> productYears = List.of(
        year(2026, OverallDimension.RHYTHM, OverallDimension.CAREER),
        year(2027, OverallDimension.CAREER, OverallDimension.WEALTH),
        year(2028, OverallDimension.CAREER, OverallDimension.RELATIONSHIP));
    OverallPeriodEvaluation product = new OverallPeriodEvaluation(
        3,
        productYears,
        List.of(
            new OverallTransition(2026, 2027, "转向"),
            new OverallTransition(2027, 2028, "延续")));
    return new Window(previous, product);
  }

  private OverallYearEvaluation year(
      int year,
      OverallDimension primary,
      OverallDimension secondary) {
    List<OverallDimensionEvaluation> dimensions = java.util.Arrays.stream(OverallDimension.values())
        .map(dimension -> new OverallDimensionEvaluation(
            dimension,
            OverallStance.BALANCED,
            dimension == primary ? 6 : dimension == secondary ? 4 : 2,
            1,
            List.of(year + "." + dimension.code() + ".support"),
            List.of(year + "." + dimension.code() + ".limit"),
            List.of(year + "." + dimension.code() + ".annual")))
        .toList();
    return new OverallYearEvaluation(year, "丙午", primary, secondary, dimensions);
  }

  private void assertEvidenceBelongsTo(OverallYearEvaluation year, List<String> evidenceKeys) {
    Set<String> available = new LinkedHashSet<>();
    year.dimensions().forEach(dimension -> available.addAll(dimension.allEvidenceKeys()));
    assertFalse(evidenceKeys.isEmpty());
    assertTrue(available.containsAll(evidenceKeys), evidenceKeys + " not in " + available);
  }

  private record Window(OverallYearEvaluation previous, OverallPeriodEvaluation product) {}
}
