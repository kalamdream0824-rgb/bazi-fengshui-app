package com.bazi.app.report.wealth;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class WealthArbitrator {

  public WealthYearEvaluation arbitrate(
      int year,
      String ganZhi,
      Map<WealthPath, WealthPathEvaluation> paths) {
    if (paths == null || paths.size() != WealthPath.values().length) {
      throw new IllegalArgumentException("all five wealth paths are required for arbitration");
    }
    List<WealthPathEvaluation> income = paths.values().stream()
        .filter(item -> item.path() != WealthPath.RETENTION)
        .sorted(Comparator.comparingInt(WealthPathEvaluation::score).reversed()
            .thenComparingInt(item -> item.path().ordinal()))
        .toList();
    WealthPathEvaluation primary = income.get(0);
    WealthPathEvaluation secondary = income.get(1);
    WealthPath mainRisk = paths.values().stream()
        .sorted(Comparator.comparingInt(WealthPathEvaluation::limitationWeight).reversed()
            .thenComparingInt(WealthPathEvaluation::score)
            .thenComparingInt(item -> item.path().ordinal()))
        .map(WealthPathEvaluation::path)
        .findFirst()
        .orElse(WealthPath.RETENTION);
    return new WealthYearEvaluation(
        year,
        ganZhi,
        paths,
        primary.path(),
        secondary.path(),
        mainRisk,
        primary.score() == secondary.score());
  }
}
