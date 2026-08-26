package com.bazi.app.report.relationship;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record RelationshipYearEvaluation(
    int year,
    String ganZhi,
    Map<RelationshipDimension, RelationshipDimensionEvaluation> dimensions) {

  public RelationshipYearEvaluation {
    Objects.requireNonNull(ganZhi, "ganZhi");
    Objects.requireNonNull(dimensions, "dimensions");
    if (ganZhi.isBlank()) {
      throw new IllegalArgumentException("relationship year GanZhi must not be blank");
    }
    EnumMap<RelationshipDimension, RelationshipDimensionEvaluation> copy =
        new EnumMap<>(RelationshipDimension.class);
    copy.putAll(dimensions);
    if (copy.size() != RelationshipDimension.values().length) {
      throw new IllegalArgumentException("all five relationship dimensions are required");
    }
    copy.forEach((dimension, evaluation) -> {
      if (evaluation == null || evaluation.dimension() != dimension) {
        throw new IllegalArgumentException("relationship dimension key does not match evaluation");
      }
    });
    dimensions = Map.copyOf(copy);
  }

  public int totalSupportWeight() {
    return dimensions.values().stream()
        .mapToInt(RelationshipDimensionEvaluation::supportWeight)
        .sum();
  }

  public int totalLimitationWeight() {
    return dimensions.values().stream()
        .mapToInt(RelationshipDimensionEvaluation::limitationWeight)
        .sum();
  }

  public int climateWeight() {
    return totalSupportWeight() - totalLimitationWeight();
  }
}
