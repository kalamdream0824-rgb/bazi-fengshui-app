package com.bazi.app.report.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RelationshipPeriodArbitratorTest {

  @Test
  void selectsFocusByProminenceInsteadOfNetWeight() {
    RelationshipYearEvaluation first = year(
        2026,
        weights(1, 3, 0, 0, 2),
        weights(0, 0, 0, 4, 0));
    RelationshipYearEvaluation second = year(
        2027,
        weights(1, 3, 0, 0, 2),
        weights(0, 0, 0, 4, 0));

    RelationshipPeriodEvaluation result = new RelationshipPeriodArbitrator()
        .arbitrate(List.of(first, second));

    assertEquals(RelationshipDimension.BOUNDARIES,
        result.years().get(0).focus().primaryDimension());
    assertEquals(RelationshipDimension.RESPONSE,
        result.years().get(0).focus().secondaryDimension());
    assertEquals(RelationshipDimension.BOUNDARIES,
        result.years().get(0).mainRisk().dimension());
    assertEquals(RelationshipDimension.BOUNDARIES,
        result.focus().primaryDimension());
    assertFalse(result.focus().primaryEvidenceKeys().isEmpty());
    assertFalse(result.focus().secondaryEvidenceKeys().isEmpty());
  }

  @Test
  void breaksFocusAndRiskTiesByStableDimensionOrder() {
    RelationshipYearEvaluation first = year(
        2026,
        weights(4, 4, 1, 0, 1),
        weights(0, 0, 0, 2, 2));
    RelationshipYearEvaluation second = year(
        2027,
        weights(4, 4, 1, 0, 1),
        weights(0, 0, 0, 2, 2));

    RelationshipPeriodEvaluation result = new RelationshipPeriodArbitrator()
        .arbitrate(List.of(first, second));

    assertEquals(RelationshipDimension.CONNECTION, result.focus().primaryDimension());
    assertEquals(RelationshipDimension.RESPONSE, result.focus().secondaryDimension());
    assertTrue(result.focus().tied());
    assertEquals(RelationshipDimension.BOUNDARIES, result.mainRisk().dimension());
  }

  @Test
  void omitsRiskWhenThereIsNoNegativeEvidence() {
    RelationshipYearEvaluation first = year(
        2026, weights(3, 2, 1, 0, 0), weights(0, 0, 0, 0, 0));
    RelationshipYearEvaluation second = year(
        2027, weights(2, 3, 1, 0, 0), weights(0, 0, 0, 0, 0));

    RelationshipPeriodEvaluation result = new RelationshipPeriodArbitrator()
        .arbitrate(List.of(first, second));

    assertNull(result.mainRisk());
    assertTrue(result.years().stream().allMatch(year -> year.mainRisk() == null));
  }

  @Test
  void buildsTransitionsFromAdjacentSupportAndLimitationClimate() {
    RelationshipYearEvaluation first = year(
        2026, weights(2, 1, 0, 0, 0), weights(0, 0, 0, 0, 0));
    RelationshipYearEvaluation second = year(
        2027, weights(5, 1, 0, 0, 0), weights(0, 0, 0, 0, 0));
    RelationshipYearEvaluation third = year(
        2028, weights(1, 1, 0, 0, 0), weights(0, 0, 0, 4, 0));

    RelationshipPeriodEvaluation result = new RelationshipPeriodArbitrator()
        .arbitrate(List.of(first, second, third));

    assertEquals(2, result.transitions().size());
    assertEquals(RelationshipPeriodEvaluation.TransitionDirection.EASING,
        result.transitions().get(0).direction());
    assertEquals(RelationshipPeriodEvaluation.TransitionDirection.TIGHTENING,
        result.transitions().get(1).direction());
    assertEquals(2026, result.transitions().get(0).fromYear());
    assertEquals(2028, result.transitions().get(1).toYear());
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 4, 5})
  void acceptsEverySupportedTechnicalHorizon(int years) {
    List<RelationshipYearEvaluation> evaluations = new ArrayList<>();
    for (int index = 0; index < years; index++) {
      evaluations.add(year(
          2026 + index,
          weights(2, 1, 0, 0, 0),
          weights(0, 0, 0, 0, 0)));
    }

    RelationshipPeriodEvaluation result = new RelationshipPeriodArbitrator()
        .arbitrate(evaluations);

    assertEquals(years, result.years().size());
    assertEquals(years - 1, result.transitions().size());
  }

  @Test
  void rejectsPeriodsOutsideTheTechnicalBoundary() {
    RelationshipPeriodArbitrator arbitrator = new RelationshipPeriodArbitrator();
    RelationshipYearEvaluation template = year(
        2026, weights(2, 1, 0, 0, 0), weights(0, 0, 0, 0, 0));
    List<RelationshipYearEvaluation> sixYears = new ArrayList<>();
    for (int index = 0; index < 6; index++) {
      sixYears.add(year(
          2026 + index,
          weights(2, 1, 0, 0, 0),
          weights(0, 0, 0, 0, 0)));
    }

    assertThrows(IllegalArgumentException.class, () -> arbitrator.arbitrate(List.of(template)));
    assertThrows(IllegalArgumentException.class, () -> arbitrator.arbitrate(sixYears));
  }

  @Test
  void rejectsAResultWithoutTwoEvidenceBackedFocusDimensions() {
    RelationshipYearEvaluation first = year(
        2026, weights(3, 0, 0, 0, 0), weights(0, 0, 0, 0, 0));
    RelationshipYearEvaluation second = year(
        2027, weights(3, 0, 0, 0, 0), weights(0, 0, 0, 0, 0));

    assertThrows(
        IllegalArgumentException.class,
        () -> new RelationshipPeriodArbitrator().arbitrate(List.of(first, second)));
  }

  @Test
  void arbitrationApiCannotReceiveRelationshipStatus() {
    List<Executable> api = new ArrayList<>(
        Arrays.asList(RelationshipPeriodArbitrator.class.getDeclaredConstructors()));
    api.addAll(Arrays.asList(RelationshipPeriodArbitrator.class.getDeclaredMethods()));

    assertTrue(api.stream()
        .flatMap(item -> Arrays.stream(item.getParameterTypes()))
        .noneMatch(RelationshipStatus.class::equals));
  }

  private RelationshipYearEvaluation year(
      int year,
      Map<RelationshipDimension, Integer> support,
      Map<RelationshipDimension, Integer> limitation) {
    Map<RelationshipDimension, RelationshipDimensionEvaluation> dimensions =
        new EnumMap<>(RelationshipDimension.class);
    for (RelationshipDimension dimension : RelationshipDimension.values()) {
      List<RelationshipEvidence> evidence = new ArrayList<>();
      int supportWeight = support.get(dimension);
      int limitationWeight = limitation.get(dimension);
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
}
