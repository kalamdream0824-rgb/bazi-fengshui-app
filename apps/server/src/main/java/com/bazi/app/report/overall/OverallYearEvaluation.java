package com.bazi.app.report.overall;

import java.util.List;
import java.util.Objects;

public record OverallYearEvaluation(
    int year,
    String ganZhi,
    OverallDimension primaryDimension,
    OverallDimension secondaryDimension,
    List<OverallDimensionEvaluation> dimensions) {

  public OverallYearEvaluation {
    Objects.requireNonNull(ganZhi, "ganZhi");
    Objects.requireNonNull(primaryDimension, "primaryDimension");
    Objects.requireNonNull(secondaryDimension, "secondaryDimension");
    dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
    if (primaryDimension == secondaryDimension) {
      throw new IllegalArgumentException("primary and secondary dimensions must differ");
    }
    if (dimensions.size() != OverallDimension.values().length
        || dimensions.stream().map(OverallDimensionEvaluation::dimension).distinct().count()
            != OverallDimension.values().length) {
      throw new IllegalArgumentException("every year requires all overall dimensions");
    }
  }

  public OverallDimensionEvaluation primary() {
    return dimension(primaryDimension);
  }

  public OverallDimensionEvaluation secondary() {
    return dimension(secondaryDimension);
  }

  public OverallDimensionEvaluation dimension(OverallDimension selected) {
    return dimensions.stream()
        .filter(item -> item.dimension() == selected)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("missing overall dimension: " + selected));
  }
}
