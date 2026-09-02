package com.bazi.app.report.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RelationshipNarrativePlannerTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private RelationshipPeriodEvaluation evaluation;
  private RelationshipNarrativePlan single;
  private RelationshipNarrativePlan dating;
  private RelationshipNarrativePlan married;

  @BeforeEach
  void setUp() {
    PaipanRequest request = new PaipanRequest(
        "林先生", "male", "1995-10-08T14:30:00", "上海", false);
    PaipanResultDto chart = new BaziService().paipan(request);
    List<RelationshipYearFacts> facts = new RelationshipFactExtractor().extract(
        request,
        chart,
        new AnnualContextFactory(CLOCK)
            .create(request, chart, ReportHorizon.RELATIONSHIP_PRODUCT));
    evaluation = new RelationshipPeriodArbitrator().arbitrate(
        new RelationshipDimensionEvaluator().evaluate(facts));

    RelationshipNarrativePlanner planner = new RelationshipNarrativePlanner();
    single = planner.plan(evaluation, RelationshipStatus.SINGLE);
    dating = planner.plan(evaluation, RelationshipStatus.DATING);
    married = planner.plan(evaluation, RelationshipStatus.MARRIED);
  }

  @Test
  void buildsFiveDimensionsAndThreeCompleteYearsForEveryStatus() {
    for (RelationshipNarrativePlan plan : List.of(single, dating, married)) {
      assertEquals(3, plan.horizonYears());
      assertEquals(5, plan.dimensions().size());
      assertEquals(3, plan.years().size());
      assertFalse(plan.thesis().isBlank());
      assertFalse(plan.summary().isBlank());
      assertNotEquals(plan.primaryDimensionCode(), plan.secondaryDimensionCode());
      assertFalse(plan.evidenceKeys().isEmpty());
      for (RelationshipNarrativePlan.YearNarrative year : plan.years()) {
        assertFalse(year.judgment().isBlank());
        assertEquals(2, year.realitySignals().size());
        assertEquals(2, year.actions().size());
        assertEquals(2, year.actions().stream().distinct().count());
        assertFalse(year.transition().isBlank());
        assertFalse(year.evidenceKeys().isEmpty());
      }
    }
  }

  @Test
  void oldPlannerEntryKeepsTimelineAbsentWhilePreviousYearEntryAddsIt() {
    RelationshipPeriodEvaluation.Year previous = new RelationshipPeriodArbitrator()
        .arbitrate(List.of(year(2025, 4, 3, 1), year(2026, 4, 3, 1)))
        .years().get(0);

    RelationshipNarrativePlan current = new RelationshipNarrativePlanner().plan(
        evaluation, RelationshipStatus.DATING, previous);

    assertNull(dating.timeline());
    assertNotNull(current.timeline());
    assertEquals(2025, current.timeline().past().year());
    assertEquals(List.of(2027, 2028), current.timeline().future().stream()
        .map(com.bazi.app.report.NarrativeTimeline.FutureStep::year)
        .toList());
  }

  @Test
  void historicalJsonWithoutTimelineStillDeserializes() throws Exception {
    ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    var legacyJson = json.valueToTree(dating);
    ((com.fasterxml.jackson.databind.node.ObjectNode) legacyJson).remove("timeline");

    RelationshipNarrativePlan restored = json.treeToValue(
        legacyJson, RelationshipNarrativePlan.class);

    assertNull(restored.timeline());
    assertEquals(dating.years(), restored.years());
  }

  @Test
  void statusChangesOnlyNarrativeAndKeepsCalculationFieldsIdentical() {
    CalculationSignature expected = calculation(single);

    assertEquals(expected, calculation(dating));
    assertEquals(expected, calculation(married));
    assertEquals(3, Stream.of(single, dating, married)
        .map(RelationshipNarrativePlan::thesis).distinct().count());
    assertEquals(3, Stream.of(single, dating, married)
        .map(RelationshipNarrativePlan::summary).distinct().count());
    assertEquals(3, Stream.of(single, dating, married)
        .map(plan -> plan.years().get(0).judgment()).distinct().count());
    assertEquals(3, Stream.of(single, dating, married)
        .map(plan -> plan.years().get(0).realitySignals()).distinct().count());
    assertEquals(3, Stream.of(single, dating, married)
        .map(plan -> plan.years().get(0).actions()).distinct().count());
  }

  @Test
  void everyStatusAnswersItsOwnRealWorldQuestion() {
    assertTrue(single.firstYear().actions().stream()
        .anyMatch(text -> text.contains("继续了解")));
    assertTrue(dating.firstYear().actions().stream()
        .anyMatch(text -> text.contains("两个人")));
    assertTrue(married.firstYear().actions().stream()
        .anyMatch(text -> text.contains("共同生活")));

    assertTrue(single.thesis().contains("值得继续了解"));
    assertTrue(dating.thesis().contains("继续走下去"));
    assertTrue(married.thesis().contains("共同生活"));
  }

  @Test
  void omitsRiskAndAnnualLimitWhenThereIsNoNegativeEvidence() {
    RelationshipNarrativePlan plan = new RelationshipNarrativePlanner().plan(
        noRiskEvaluation(), RelationshipStatus.SINGLE);

    assertNull(plan.mainRisk());
    assertTrue(plan.years().stream().allMatch(year -> year.mainLimit() == null));
    assertFalse(plan.summary().contains("留意问题"));
  }

  @Test
  void keepsAdviceStableWhenOnlyTheCalendarPositionChanges() {
    RelationshipNarrativePlan plan = new RelationshipNarrativePlanner().plan(
        repeatedFocusEvaluation(), RelationshipStatus.DATING);

    List<String> primaryActions = plan.years().stream()
        .map(year -> year.actions().get(0))
        .toList();
    // A new year alone must not invent a new relationship stage or a different recommendation.
    assertEquals(1, primaryActions.stream().distinct().count());
  }

  private CalculationSignature calculation(RelationshipNarrativePlan plan) {
    List<DimensionSignature> dimensions = plan.dimensions().stream()
        .map(item -> new DimensionSignature(
            item.code(), item.tone(), item.supportingEvidenceKeys(), item.limitingEvidenceKeys()))
        .toList();
    List<YearSignature> years = plan.years().stream()
        .map(year -> new YearSignature(
            year.year(),
            year.primaryDimensionCode(),
            year.secondaryDimensionCode(),
            year.riskDimensionCode(),
            year.evidenceKeys()))
        .toList();
    return new CalculationSignature(
        plan.primaryDimensionCode(),
        plan.secondaryDimensionCode(),
        plan.mainRisk() == null ? null : plan.mainRisk().dimensionCode(),
        dimensions,
        years,
        plan.evidenceKeys());
  }

  private RelationshipPeriodEvaluation noRiskEvaluation() {
    return new RelationshipPeriodArbitrator().arbitrate(List.of(
        year(2026, 3, 2, 0),
        year(2027, 2, 3, 0)));
  }

  private RelationshipPeriodEvaluation repeatedFocusEvaluation() {
    return new RelationshipPeriodArbitrator().arbitrate(List.of(
        year(2026, 4, 2, 0),
        year(2027, 4, 2, 0),
        year(2028, 4, 2, 0)));
  }

  private RelationshipYearEvaluation year(
      int year,
      int connectionSupport,
      int responseSupport,
      int boundaryLimitation) {
    Map<RelationshipDimension, RelationshipDimensionEvaluation> dimensions =
        new EnumMap<>(RelationshipDimension.class);
    for (RelationshipDimension dimension : RelationshipDimension.values()) {
      int support = dimension == RelationshipDimension.CONNECTION
          ? connectionSupport
          : dimension == RelationshipDimension.RESPONSE ? responseSupport : 0;
      int limitation = dimension == RelationshipDimension.BOUNDARIES
          ? boundaryLimitation : 0;
      List<RelationshipEvidence> evidence = new ArrayList<>();
      if (support > 0) {
        evidence.add(evidence(year + "." + dimension.code() + ".support", dimension, support));
      }
      if (limitation > 0) {
        evidence.add(evidence(year + "." + dimension.code() + ".limit", dimension, -limitation));
      }
      dimensions.put(dimension, new RelationshipDimensionEvaluation(
          dimension,
          support,
          limitation,
          support + limitation,
          support - limitation,
          RelationshipDimensionEvaluation.toneFor(support, limitation),
          evidence));
    }
    return new RelationshipYearEvaluation(year, "丙午", dimensions);
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

  private record CalculationSignature(
      String primary,
      String secondary,
      String risk,
      List<DimensionSignature> dimensions,
      List<YearSignature> years,
      List<String> evidenceKeys) {}

  private record DimensionSignature(
      String code,
      String tone,
      List<String> support,
      List<String> limitation) {}

  private record YearSignature(
      int year,
      String primary,
      String secondary,
      String risk,
      List<String> evidenceKeys) {}
}
