package com.bazi.app.report.relationship;

import java.util.List;
import java.util.Objects;

public record RelationshipEvidence(
    String key,
    RelationshipEvidenceFamily family,
    String label,
    RelationshipDimension dimension,
    int weight,
    List<String> sourceFactKeys) {

  public RelationshipEvidence {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(family, "family");
    Objects.requireNonNull(label, "label");
    Objects.requireNonNull(dimension, "dimension");
    if (key.isBlank() || label.isBlank()) {
      throw new IllegalArgumentException("relationship evidence fields must not be blank");
    }
    if (weight == 0) {
      throw new IllegalArgumentException("relationship evidence weight must not be zero");
    }
    sourceFactKeys = sourceFactKeys == null ? List.of() : List.copyOf(sourceFactKeys);
    if (sourceFactKeys.isEmpty() || sourceFactKeys.stream().anyMatch(String::isBlank)) {
      throw new IllegalArgumentException("relationship evidence requires source facts");
    }
  }
}
