package com.bazi.app.report.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.report.TenGodGroup;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RelationshipDimensionEvaluatorTest {

  @Test
  void calculatesSignedWeightsAndToneForEveryDimension() {
    RelationshipYearFacts facts = yearFacts(List.of(
        evidence("connection.support", RelationshipDimension.CONNECTION, 3),
        evidence("connection.limit", RelationshipDimension.CONNECTION, -1),
        evidence("response.limit", RelationshipDimension.RESPONSE, -4),
        evidence("daily.support", RelationshipDimension.DAILY_COOPERATION, 2),
        evidence("stability.support", RelationshipDimension.STABILITY, 1),
        evidence("stability.limit", RelationshipDimension.STABILITY, -3)));

    Map<RelationshipDimension, RelationshipDimensionEvaluation> result =
        new RelationshipDimensionEvaluator().evaluateDimensions(facts);

    assertEquals(Set.of(RelationshipDimension.values()), result.keySet());
    assertEvaluation(result.get(RelationshipDimension.CONNECTION), 3, 1, 4, 2,
        RelationshipTone.MIXED);
    assertEvaluation(result.get(RelationshipDimension.RESPONSE), 0, 4, 4, -4,
        RelationshipTone.PRESSURED);
    assertEvaluation(result.get(RelationshipDimension.DAILY_COOPERATION), 2, 0, 2, 2,
        RelationshipTone.SUPPORTIVE);
    assertEvaluation(result.get(RelationshipDimension.BOUNDARIES), 0, 0, 0, 0,
        RelationshipTone.QUIET);
    assertEvaluation(result.get(RelationshipDimension.STABILITY), 1, 3, 4, -2,
        RelationshipTone.PRESSURED);
  }

  @Test
  void combinesNatalAndAnnualEvidenceInStableKeyOrder() {
    RelationshipEvidence natal = evidence(
        "z.natal.connection", RelationshipDimension.CONNECTION, 1,
        RelationshipEvidenceFamily.NATAL_STRUCTURE);
    RelationshipEvidence annual = evidence(
        "a.annual.connection", RelationshipDimension.CONNECTION, 4,
        RelationshipEvidenceFamily.ANNUAL_TRIGGER);
    RelationshipYearFacts facts = yearFacts(List.of(natal), List.of(annual));

    RelationshipDimensionEvaluation connection = new RelationshipDimensionEvaluator()
        .evaluateDimensions(facts)
        .get(RelationshipDimension.CONNECTION);

    assertEquals(5, connection.supportWeight());
    assertEquals(
        List.of("a.annual.connection", "z.natal.connection"),
        connection.evidence().stream().map(RelationshipEvidence::key).toList());
    assertEquals(
        Set.of(RelationshipEvidenceFamily.NATAL_STRUCTURE,
            RelationshipEvidenceFamily.ANNUAL_TRIGGER),
        connection.evidenceFamilies());
  }

  @Test
  void countsAnIdenticalEvidenceKeyOnlyOncePerDimension() {
    RelationshipEvidence item = evidence(
        "same.connection", RelationshipDimension.CONNECTION, 3);
    RelationshipYearFacts facts = yearFacts(List.of(item, item));

    RelationshipDimensionEvaluation result = new RelationshipDimensionEvaluator()
        .evaluateDimensions(facts)
        .get(RelationshipDimension.CONNECTION);

    assertEquals(3, result.supportWeight());
    assertEquals(1, result.evidence().size());
  }

  @Test
  void rejectsConflictingEvidenceWithTheSameKey() {
    RelationshipEvidence first = evidence(
        "same.connection", RelationshipDimension.CONNECTION, 3);
    RelationshipEvidence conflicting = evidence(
        "same.connection", RelationshipDimension.CONNECTION, -2);

    assertThrows(
        IllegalArgumentException.class,
        () -> new RelationshipDimensionEvaluator().evaluateDimensions(
            yearFacts(List.of(first, conflicting))));
  }

  @Test
  void evaluatesConsecutiveYearsWithoutArbitratingThem() {
    RelationshipYearFacts first = yearFacts(2026, List.of(
        evidence("2026.connection", RelationshipDimension.CONNECTION, 2)));
    RelationshipYearFacts second = yearFacts(2027, List.of(
        evidence("2027.response", RelationshipDimension.RESPONSE, 3)));

    List<RelationshipYearEvaluation> result =
        new RelationshipDimensionEvaluator().evaluate(List.of(first, second));

    assertEquals(List.of(2026, 2027),
        result.stream().map(RelationshipYearEvaluation::year).toList());
    assertEquals(RelationshipTone.SUPPORTIVE,
        result.get(1).dimensions().get(RelationshipDimension.RESPONSE).tone());
  }

  @Test
  void rejectsNonConsecutiveYears() {
    RelationshipYearFacts first = yearFacts(2026, List.of());
    RelationshipYearFacts third = yearFacts(2028, List.of());

    assertThrows(
        IllegalArgumentException.class,
        () -> new RelationshipDimensionEvaluator().evaluate(List.of(first, third)));
  }

  @Test
  void evaluationApiCannotReceiveRelationshipStatus() {
    List<Executable> api = new ArrayList<>(
        Arrays.asList(RelationshipDimensionEvaluator.class.getDeclaredConstructors()));
    api.addAll(Arrays.asList(RelationshipDimensionEvaluator.class.getDeclaredMethods()));

    assertTrue(api.stream()
        .flatMap(item -> Arrays.stream(item.getParameterTypes()))
        .noneMatch(RelationshipStatus.class::equals));
  }

  private void assertEvaluation(
      RelationshipDimensionEvaluation evaluation,
      int support,
      int limitation,
      int prominence,
      int net,
      RelationshipTone tone) {
    assertEquals(support, evaluation.supportWeight());
    assertEquals(limitation, evaluation.limitationWeight());
    assertEquals(prominence, evaluation.prominenceWeight());
    assertEquals(net, evaluation.netWeight());
    assertEquals(tone, evaluation.tone());
  }

  private RelationshipEvidence evidence(
      String key,
      RelationshipDimension dimension,
      int weight) {
    return evidence(key, dimension, weight, RelationshipEvidenceFamily.ANNUAL_TRIGGER);
  }

  private RelationshipEvidence evidence(
      String key,
      RelationshipDimension dimension,
      int weight,
      RelationshipEvidenceFamily family) {
    return new RelationshipEvidence(
        key, family, key, dimension, weight, List.of("source." + key));
  }

  private RelationshipYearFacts yearFacts(List<RelationshipEvidence> annualEvidence) {
    return yearFacts(2026, List.of(), annualEvidence);
  }

  private RelationshipYearFacts yearFacts(
      List<RelationshipEvidence> natalEvidence,
      List<RelationshipEvidence> annualEvidence) {
    return yearFacts(2026, natalEvidence, annualEvidence);
  }

  private RelationshipYearFacts yearFacts(
      int year,
      List<RelationshipEvidence> annualEvidence) {
    return yearFacts(year, List.of(), annualEvidence);
  }

  private RelationshipYearFacts yearFacts(
      int year,
      List<RelationshipEvidence> natalEvidence,
      List<RelationshipEvidence> annualEvidence) {
    RelationshipNatalProfile natal = new RelationshipNatalProfile(
        "male",
        "壬",
        "申",
        "偏强",
        TenGodGroup.WEALTH,
        List.of(),
        new EnumMap<>(TenGodGroup.class),
        natalEvidence);
    return new RelationshipYearFacts(
        year,
        "丙午",
        "偏财",
        TenGodGroup.WEALTH,
        null,
        null,
        natal,
        annualEvidence);
  }
}
