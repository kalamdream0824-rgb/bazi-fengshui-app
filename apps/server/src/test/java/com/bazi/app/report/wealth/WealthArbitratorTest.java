package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WealthArbitratorTest {

  @Test
  void selectsDifferentPrimaryAndSecondaryPathsAndAlwaysProvidesRisk() {
    Map<WealthPath, WealthPathEvaluation> paths = paths(8, 6, 5, 2, -1);

    WealthYearEvaluation result = new WealthArbitrator().arbitrate(2026, "丙午", paths);

    assertEquals(WealthPath.STABLE_INCOME, result.primaryIncomePath());
    assertEquals(WealthPath.SKILL_INCOME, result.secondaryIncomePath());
    assertNotEquals(result.primaryIncomePath(), result.secondaryIncomePath());
    assertNotNull(result.mainRisk());
    assertEquals(WealthPath.RETENTION, result.mainRisk());
  }

  @Test
  void breaksTiesDeterministicallyAndExposesTheTie() {
    Map<WealthPath, WealthPathEvaluation> paths = paths(7, 7, 3, 1, 0);

    WealthYearEvaluation result = new WealthArbitrator().arbitrate(2026, "丙午", paths);

    assertEquals(WealthPath.STABLE_INCOME, result.primaryIncomePath());
    assertEquals(WealthPath.SKILL_INCOME, result.secondaryIncomePath());
    assertTrue(result.tied());
  }

  private Map<WealthPath, WealthPathEvaluation> paths(
      int stable,
      int skill,
      int project,
      int cooperation,
      int retention) {
    Map<WealthPath, WealthPathEvaluation> result = new EnumMap<>(WealthPath.class);
    result.put(WealthPath.STABLE_INCOME, evaluation(WealthPath.STABLE_INCOME, stable));
    result.put(WealthPath.SKILL_INCOME, evaluation(WealthPath.SKILL_INCOME, skill));
    result.put(WealthPath.PROJECT_INCOME, evaluation(WealthPath.PROJECT_INCOME, project));
    result.put(WealthPath.COOPERATION_INCOME, evaluation(WealthPath.COOPERATION_INCOME, cooperation));
    result.put(WealthPath.RETENTION, evaluation(WealthPath.RETENTION, retention));
    return result;
  }

  private WealthPathEvaluation evaluation(WealthPath path, int score) {
    return new WealthPathEvaluation(path, score, "表现一般", List.of());
  }
}
