package com.bazi.app.report.wealth;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record WealthYearEvaluation(
    int year,
    String ganZhi,
    Map<WealthPath, WealthPathEvaluation> paths,
    WealthPath primaryIncomePath,
    WealthPath secondaryIncomePath,
    WealthPath mainRisk,
    boolean tied) {

  public WealthYearEvaluation {
    Objects.requireNonNull(ganZhi, "ganZhi");
    Objects.requireNonNull(paths, "paths");
    Objects.requireNonNull(primaryIncomePath, "primaryIncomePath");
    Objects.requireNonNull(secondaryIncomePath, "secondaryIncomePath");
    Objects.requireNonNull(mainRisk, "mainRisk");
    EnumMap<WealthPath, WealthPathEvaluation> copy = new EnumMap<>(WealthPath.class);
    copy.putAll(paths);
    if (copy.size() != WealthPath.values().length) {
      throw new IllegalArgumentException("all five wealth paths are required");
    }
    paths = Map.copyOf(copy);
    if (primaryIncomePath == secondaryIncomePath || primaryIncomePath == WealthPath.RETENTION
        || secondaryIncomePath == WealthPath.RETENTION) {
      throw new IllegalArgumentException("primary and secondary income paths must be distinct");
    }
  }
}
