package com.bazi.app.report.relationship;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RelationshipAnnualNarrativeTest {

  @Test
  void explainsIncreasingSupportEvenWhenFocusAndToneStayTheSameForEveryStatus() {
    for (RelationshipStatus status : RelationshipStatus.values()) {
      var plan = plan(status, year(2026, 6, 0), year(2027, 8, 0));
      assertNotEquals(plan.years().get(0).judgment(), plan.years().get(1).judgment());
      assertTrue(plan.years().get(1).judgment().contains("相比上一年"));
      assertTrue(plan.years().get(1).judgment().contains("主动"));
      assertFalse(plan.years().get(1).judgment().contains("没有明显变化"));
    }
  }

  @Test
  void explicitlyKeepsUnchangedEvidenceInsteadOfInventingAnnualStagesThroughFiveYears() {
    for (RelationshipStatus status : RelationshipStatus.values()) {
      var plan = plan(status, year(2026, 6, 0), year(2027, 6, 0),
          year(2028, 6, 0), year(2029, 6, 0), year(2030, 6, 0));
      assertTrue(plan.thesis().contains("未来五年"));
      for (int index = 1; index < 5; index++) {
        assertTrue(plan.years().get(index).judgment().contains("没有明显变化"));
        assertFalse(plan.years().get(index).focus().contains("第三年"));
        assertEquals(plan.years().get(0).focus(), plan.years().get(index).focus());
        assertEquals(plan.years().get(0).actions(), plan.years().get(index).actions());
      }
      assertFalse(plan.years().get(4).transition().contains("三年"));
      assertFalse(plan.years().get(4).transition().contains("2031"));
    }
  }

  @Test
  void describesBothSupportAndLimitationGrowthWhenNetWeightDoesNotChange() {
    var plan = plan(RelationshipStatus.DATING, year(2026, 6, 1), year(2027, 8, 3));
    String text = plan.years().get(1).judgment();
    assertTrue(text.contains("主动"));
    assertTrue(text.contains("但"));
    assertTrue(text.contains("投入"));
    assertFalse(text.contains("没有明显变化"));
    assertFalse(plan.years().get(0).transition().contains("变化不大"));
    assertTrue(plan.years().get(0).transition().contains("2027"));
  }

  @Test
  void explainsReplacementOfAnnualEvidenceWithoutPretendingTheYearIsBetter() {
    var first = year(2026, 6, 0);
    var second = replaceEvidence(year(2027, 6, 0), RelationshipDimension.CONNECTION,
        "annual.branch.harmony.time.connection");
    var plan = plan(RelationshipStatus.SINGLE, first, second);
    String text = plan.years().get(1).judgment();
    assertTrue(text.contains("变化在于"));
    assertTrue(text.contains("主动"));
    assertTrue(text.contains("相处安排"));
    assertFalse(text.contains("没有明显变化"));
    assertFalse(text.contains("更顺利"));
  }

  @Test
  void noticesNewRiskInAnotherDimensionEvenWhenPrimaryEvidenceIsUnchanged() {
    var second = withDimension(year(2027, 6, 0), RelationshipDimension.BOUNDARIES, 0, 2);
    var plan = plan(RelationshipStatus.MARRIED, year(2026, 6, 0), second);
    assertEquals("connection", plan.years().get(1).primaryDimensionCode());
    assertTrue(plan.years().get(1).judgment().contains("分歧"));
    assertFalse(plan.years().get(1).judgment().contains("没有明显变化"));
    assertEquals("boundaries", plan.years().get(1).riskDimensionCode());
  }

  @Test
  void describesFocusSwitchInsteadOfPredeterminedSecondYearMilestone() {
    var second = withDimension(year(2027, 3, 0), RelationshipDimension.RESPONSE, 9, 0);
    var plan = plan(RelationshipStatus.DATING, year(2026, 6, 0), second);
    assertTrue(plan.years().get(1).judgment().contains("转向"));
    assertTrue(plan.years().get(1).focus().contains("想法"));
    assertFalse(plan.years().get(1).focus().contains("进入稳定安排"));
  }

  @Test
  void pressuredSignalsDoNotPromisePositiveOutcomesAndRemainObservable() {
    for (RelationshipStatus status : RelationshipStatus.values()) {
      var plan = plan(status, year(2026, 1, 8), year(2027, 1, 8));
      String signals = String.join("", plan.firstYear().realitySignals());
      assertTrue(signals.contains("是否"));
      assertFalse(signals.contains("都会主动"));
      assertFalse(signals.contains("连续几周都有回应"));
      assertFalse(signals.contains("会主动问候"));
    }
  }

  @Test
  void annualDifferencesAreNotHiddenByDominantNatalBackground() {
    var first = withNatalResponse(year(2026, 6, 0));
    var second = withNatalResponse(year(2027, 8, 0));
    var plan = plan(RelationshipStatus.DATING, first, second);
    assertEquals("response", plan.firstYear().primaryDimensionCode());
    assertTrue(plan.years().get(1).judgment().contains("主动"));
    assertFalse(plan.years().get(1).judgment().contains("没有明显变化"));
    assertFalse(plan.firstYear().judgment().contains("natal"));
  }

  @Test
  void focusSwitchExplainsLossOfPreviousFocusEvenWhenOtherRisksAlsoChange() {
    var first = withDimension(withDimension(year(2026, 8, 0),
        RelationshipDimension.BOUNDARIES, 0, 3), RelationshipDimension.STABILITY, 0, 2);
    var second = year(2027, 1, 0);
    var plan = plan(RelationshipStatus.SINGLE, first, second);
    String judgment = plan.years().get(1).judgment();
    assertTrue(judgment.contains("转向"));
    assertTrue(judgment.contains("主动联系的有利条件减少"));
  }

  @Test
  void changingCalendarLabelsAloneDoesNotCreateAnEvidenceDifference() {
    var first = year(2026, 6, 0);
    var dims = new EnumMap<RelationshipDimension, RelationshipDimensionEvaluation>(RelationshipDimension.class);
    first.dimensions().forEach((dimension, value) -> dims.put(dimension,
        evaluated(dimension, value.evidence().stream().map(item -> new RelationshipEvidence(
            item.key(), item.family(), "2027年新显示文字", item.dimension(), item.weight(), item.sourceFactKeys())).toList())));
    var second = new RelationshipYearEvaluation(2027, "丁未", dims);
    assertTrue(plan(RelationshipStatus.SINGLE, first, second).years().get(1)
        .judgment().contains("没有明显变化"));
  }

  @Test
  void allDimensionsAndTonesHaveObservableCopyWithoutChangingEvaluation() {
    for (var dimension : RelationshipDimension.values()) {
      for (int[] weights : List.of(new int[]{12, 0}, new int[]{12, 3}, new int[]{1, 12})) {
        var first = withDimension(year(2026, 6, 0), dimension, weights[0], weights[1]);
        var second = withDimension(year(2027, 6, 0), dimension, weights[0] + 1, weights[1]);
        var evaluation = new RelationshipPeriodArbitrator().arbitrate(List.of(first, second));
        for (var status : RelationshipStatus.values()) {
          var result = new RelationshipNarrativePlanner().plan(evaluation, status);
          assertEquals(dimension.code(), result.firstYear().primaryDimensionCode());
          assertEquals(first, evaluation.years().get(0).evaluation());
          assertEquals(second, evaluation.years().get(1).evaluation());
          assertEquals(2, result.firstYear().actions().stream().distinct().count());
          assertTrue(result.firstYear().realitySignals().stream().allMatch(text -> text.contains("是否")));
        }
      }
    }
  }

  @Test
  void transitionIsAShortComparisonNotACopyOfNextYearsFullParagraph() {
    var plan = plan(RelationshipStatus.DATING, year(2026, 6, 1), year(2027, 8, 3));
    assertTrue(plan.firstYear().transition().length() < plan.years().get(1).judgment().length());
    assertTrue(plan.firstYear().transition().contains("2027"));
    assertTrue(plan.firstYear().transition().contains("但"));
  }

  @Test
  void firstYearDoesNotRestateItsMainConclusionAsAnEvidenceSentence() {
    String text = plan(RelationshipStatus.DATING, year(2026, 6, 0), year(2027, 8, 0))
        .firstYear().judgment();
    assertEquals(1, text.split("今年", -1).length - 1);
  }

  private RelationshipNarrativePlan plan(RelationshipStatus status, RelationshipYearEvaluation... years) {
    return new RelationshipNarrativePlanner().plan(
        new RelationshipPeriodArbitrator().arbitrate(List.of(years)), status);
  }

  private RelationshipYearEvaluation year(int year, int support, int limitation) {
    Map<RelationshipDimension, RelationshipDimensionEvaluation> dimensions =
        new EnumMap<>(RelationshipDimension.class);
    for (var dimension : RelationshipDimension.values()) {
      int positive = dimension == RelationshipDimension.CONNECTION ? support
          : dimension == RelationshipDimension.RESPONSE ? 2 : 0;
      int negative = dimension == RelationshipDimension.CONNECTION ? limitation : 0;
      dimensions.put(dimension, evaluation(dimension, positive, negative));
    }
    return new RelationshipYearEvaluation(year, year == 2026 ? "丙午" : "丁未", dimensions);
  }

  private RelationshipDimensionEvaluation evaluation(RelationshipDimension dimension, int support, int limit) {
    List<RelationshipEvidence> evidence = new ArrayList<>();
    if (support > 0) evidence.add(new RelationshipEvidence(
        "annual.stem.group.wealth." + dimension.code(), RelationshipEvidenceFamily.ANNUAL_TRIGGER,
        "流年关系信号", dimension, support, List.of("annual.stem.group.wealth")));
    if (limit > 0) evidence.add(new RelationshipEvidence(
        "annual.branch.clash.day." + dimension.code(), RelationshipEvidenceFamily.SPOUSE_PALACE,
        "流年冲日支", dimension, -limit, List.of("annual.branch.clash.day")));
    return evaluated(dimension, evidence);
  }

  private RelationshipDimensionEvaluation evaluated(RelationshipDimension dimension, List<RelationshipEvidence> evidence) {
    int support = evidence.stream().mapToInt(item -> Math.max(item.weight(), 0)).sum();
    int limit = evidence.stream().mapToInt(item -> Math.max(-item.weight(), 0)).sum();
    return new RelationshipDimensionEvaluation(dimension, support, limit, support + limit,
        support - limit, RelationshipDimensionEvaluation.toneFor(support, limit), evidence);
  }

  private RelationshipYearEvaluation withDimension(RelationshipYearEvaluation year,
      RelationshipDimension dimension, int support, int limit) {
    var dimensions = new EnumMap<>(year.dimensions());
    dimensions.put(dimension, evaluation(dimension, support, limit));
    return new RelationshipYearEvaluation(year.year(), year.ganZhi(), dimensions);
  }

  private RelationshipYearEvaluation withNatalResponse(RelationshipYearEvaluation year) {
    var dimensions = new EnumMap<>(year.dimensions());
    dimensions.put(RelationshipDimension.RESPONSE, evaluated(RelationshipDimension.RESPONSE,
        List.of(new RelationshipEvidence("natal.output", RelationshipEvidenceFamily.NATAL_STRUCTURE,
            "固定原局背景", RelationshipDimension.RESPONSE, 20, List.of("natal.output")))));
    return new RelationshipYearEvaluation(year.year(), year.ganZhi(), dimensions);
  }

  private RelationshipYearEvaluation replaceEvidence(RelationshipYearEvaluation year,
      RelationshipDimension dimension, String key) {
    var dimensions = new EnumMap<>(year.dimensions());
    int weight = dimensions.get(dimension).supportWeight();
    dimensions.put(dimension, evaluated(dimension, List.of(new RelationshipEvidence(key,
        RelationshipEvidenceFamily.ANNUAL_TRIGGER, "流年相合", dimension, weight, List.of(key)))));
    return new RelationshipYearEvaluation(year.year(), year.ganZhi(), dimensions);
  }
}
