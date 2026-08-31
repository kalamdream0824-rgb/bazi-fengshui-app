package com.bazi.app.report.overall;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record OverallDimensionEvaluation(
    OverallDimension dimension,
    OverallStance stance,
    int supportWeight,
    int limitationWeight,
    List<String> supportingEvidenceKeys,
    List<String> limitingEvidenceKeys,
    List<String> directAnnualEvidenceKeys) {

  public OverallDimensionEvaluation {
    Objects.requireNonNull(dimension, "dimension");
    Objects.requireNonNull(stance, "stance");
    if (supportWeight < 0 || limitationWeight < 0) {
      throw new IllegalArgumentException("dimension weights cannot be negative");
    }
    supportingEvidenceKeys = copy(supportingEvidenceKeys);
    limitingEvidenceKeys = copy(limitingEvidenceKeys);
    directAnnualEvidenceKeys = copy(directAnnualEvidenceKeys);
    if (supportingEvidenceKeys.isEmpty() && limitingEvidenceKeys.isEmpty()) {
      throw new IllegalArgumentException("a dimension requires traceable evidence");
    }
  }

  public int salience() {
    return Math.max(supportWeight, limitationWeight)
        + Math.abs(supportWeight - limitationWeight)
        + directAnnualEvidenceKeys.size();
  }

  public List<String> allEvidenceKeys() {
    LinkedHashSet<String> keys = new LinkedHashSet<>(supportingEvidenceKeys);
    keys.addAll(limitingEvidenceKeys);
    return List.copyOf(keys);
  }

  private static List<String> copy(List<String> values) {
    return values == null ? List.of() : List.copyOf(new ArrayList<>(new LinkedHashSet<>(values)));
  }
}
