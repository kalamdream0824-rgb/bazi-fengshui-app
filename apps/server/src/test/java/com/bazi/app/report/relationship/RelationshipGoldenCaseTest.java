package com.bazi.app.report.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RelationshipGoldenCaseTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void reviewedMaleAndFemaleCasesKeepCalculationAndStatusSpecificMeaning() throws Exception {
    GoldenSuite suite = loadSuite();
    assertEquals(6, suite.cases().size());
    Map<String, CalculationSignature> signatures = new HashMap<>();

    for (GoldenCase golden : suite.cases()) {
      PaipanRequest request = new PaipanRequest(
          golden.name(), golden.gender(), golden.solarDateTime(), golden.birthPlace(), false);
      PaipanResultDto chart = new BaziService().paipan(request);
      Clock clock = Clock.fixed(
          LocalDate.parse(golden.asOf()).atStartOfDay(ZONE).toInstant(), ZONE);
      RelationshipPeriodEvaluation period = new RelationshipPeriodArbitrator().arbitrate(
          new RelationshipDimensionEvaluator().evaluate(
              new RelationshipFactExtractor().extract(
                  request,
                  chart,
                  new AnnualContextFactory(clock).create(
                      request, chart, ReportHorizon.RELATIONSHIP_PRODUCT))));
      RelationshipNarrativePlan plan = new RelationshipNarrativePlanner().plan(
          period, RelationshipStatus.fromCode(golden.status()));

      assertEquals(golden.expectedPrimary(), plan.primaryDimensionCode(), golden.id());
      assertEquals(golden.expectedSecondary(), plan.secondaryDimensionCode(), golden.id());
      assertEquals(golden.expectedRisk(),
          plan.mainRisk() == null ? null : plan.mainRisk().dimensionCode(), golden.id());
      assertTrue(plan.evidenceKeys().containsAll(golden.requiredEvidenceKeys()), golden.id());
      String text = allText(plan);
      golden.requiredText().forEach(fragment -> assertTrue(
          text.contains(fragment), golden.id() + " missing text: " + fragment));
      golden.forbiddenText().forEach(fragment -> assertFalse(
          text.contains(fragment), golden.id() + " contains forbidden text: " + fragment));

      CalculationSignature signature = signature(plan);
      CalculationSignature previous = signatures.putIfAbsent(golden.chartId(), signature);
      if (previous != null) assertEquals(previous, signature, golden.id());
    }
  }

  @Test
  void goldenSuiteCoversTieAndNoNegativeEvidenceBoundaries() throws Exception {
    GoldenSuite suite = loadSuite();
    assertEquals(2, suite.syntheticEdges().size());

    for (SyntheticEdge edge : suite.syntheticEdges()) {
      RelationshipPeriodEvaluation period = new RelationshipPeriodArbitrator().arbitrate(
          years(edge.support(), edge.limitations()));
      assertEquals(edge.expectedPrimary(), period.focus().primaryDimension().code(), edge.id());
      assertEquals(edge.expectedSecondary(), period.focus().secondaryDimension().code(), edge.id());
      assertEquals(edge.expectedTied(), period.focus().tied(), edge.id());
      if (edge.expectedRisk() == null) {
        assertNull(period.mainRisk(), edge.id());
      } else {
        assertNotNull(period.mainRisk(), edge.id());
        assertEquals(edge.expectedRisk(), period.mainRisk().dimension().code(), edge.id());
      }
    }
  }

  private GoldenSuite loadSuite() throws Exception {
    InputStream input = getClass().getResourceAsStream("/report-golden/relationship-v1-cases.json");
    assertNotNull(input, "relationship v1 golden cases must exist");
    return objectMapper.readValue(input, new TypeReference<>() {});
  }

  private CalculationSignature signature(RelationshipNarrativePlan plan) {
    return new CalculationSignature(
        plan.primaryDimensionCode(),
        plan.secondaryDimensionCode(),
        plan.focusTied(),
        plan.mainRisk() == null ? null : plan.mainRisk().dimensionCode(),
        plan.dimensions().stream().map(dimension -> dimension.code()
            + ":" + dimension.tone()
            + ":" + dimension.supportingEvidenceKeys()
            + ":" + dimension.limitingEvidenceKeys()).toList(),
        plan.years().stream().map(year -> year.year()
            + ":" + year.primaryDimensionCode()
            + ":" + year.secondaryDimensionCode()
            + ":" + year.riskDimensionCode()
            + ":" + year.evidenceKeys()).toList(),
        plan.evidenceKeys());
  }

  private String allText(RelationshipNarrativePlan plan) {
    List<String> text = new ArrayList<>();
    text.add(plan.thesis());
    text.add(plan.summary());
    plan.dimensions().forEach(item -> text.add(item.judgment()));
    if (plan.mainRisk() != null) text.add(plan.mainRisk().judgment());
    plan.years().forEach(year -> {
      text.add(year.focus());
      text.add(year.judgment());
      if (year.mainLimit() != null) text.add(year.mainLimit());
      text.addAll(year.realitySignals());
      text.addAll(year.actions());
      text.add(year.transition());
    });
    return String.join("\n", text);
  }

  private List<RelationshipYearEvaluation> years(
      List<List<Integer>> supports,
      List<List<Integer>> limitations) {
    List<RelationshipYearEvaluation> result = new ArrayList<>();
    for (int yearIndex = 0; yearIndex < supports.size(); yearIndex++) {
      Map<RelationshipDimension, RelationshipDimensionEvaluation> dimensions =
          new EnumMap<>(RelationshipDimension.class);
      for (int dimensionIndex = 0;
           dimensionIndex < RelationshipDimension.values().length;
           dimensionIndex++) {
        RelationshipDimension dimension = RelationshipDimension.values()[dimensionIndex];
        int support = supports.get(yearIndex).get(dimensionIndex);
        int limitation = limitations.get(yearIndex).get(dimensionIndex);
        List<RelationshipEvidence> evidence = new ArrayList<>();
        if (support > 0) evidence.add(evidence(
            yearIndex + "." + dimension.code() + ".support", dimension, support));
        if (limitation > 0) evidence.add(evidence(
            yearIndex + "." + dimension.code() + ".limit", dimension, -limitation));
        dimensions.put(dimension, new RelationshipDimensionEvaluation(
            dimension,
            support,
            limitation,
            support + limitation,
            support - limitation,
            RelationshipDimensionEvaluation.toneFor(support, limitation),
            evidence));
      }
      result.add(new RelationshipYearEvaluation(2026 + yearIndex, "丙午", dimensions));
    }
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

  private record CalculationSignature(
      String primary,
      String secondary,
      boolean tied,
      String risk,
      List<String> dimensions,
      List<String> years,
      List<String> evidenceKeys) {}

  private record GoldenSuite(
      List<GoldenCase> cases,
      List<SyntheticEdge> syntheticEdges) {}

  private record GoldenCase(
      String id,
      String chartId,
      String name,
      String gender,
      String solarDateTime,
      String birthPlace,
      String asOf,
      String status,
      String expectedPrimary,
      String expectedSecondary,
      String expectedRisk,
      List<String> requiredEvidenceKeys,
      List<String> requiredText,
      List<String> forbiddenText) {}

  private record SyntheticEdge(
      String id,
      List<List<Integer>> support,
      List<List<Integer>> limitations,
      String expectedPrimary,
      String expectedSecondary,
      boolean expectedTied,
      String expectedRisk) {}
}
