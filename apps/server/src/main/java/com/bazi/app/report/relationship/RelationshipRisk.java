package com.bazi.app.report.relationship;

import java.util.List;
import java.util.Objects;

public record RelationshipRisk(
    RelationshipDimension dimension,
    int limitationWeight,
    List<String> evidenceKeys) {

  public RelationshipRisk {
    Objects.requireNonNull(dimension, "dimension");
    if (limitationWeight <= 0) {
      throw new IllegalArgumentException("relationship risk requires a positive limitation weight");
    }
    evidenceKeys = evidenceKeys == null ? List.of() : List.copyOf(evidenceKeys);
    if (evidenceKeys.isEmpty() || evidenceKeys.stream().anyMatch(String::isBlank)) {
      throw new IllegalArgumentException("relationship risk requires evidence keys");
    }
  }
}
