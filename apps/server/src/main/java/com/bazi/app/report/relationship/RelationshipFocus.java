package com.bazi.app.report.relationship;

import java.util.List;
import java.util.Objects;

public record RelationshipFocus(
    RelationshipDimension primaryDimension,
    RelationshipDimension secondaryDimension,
    int primaryProminenceWeight,
    int secondaryProminenceWeight,
    boolean tied,
    List<String> primaryEvidenceKeys,
    List<String> secondaryEvidenceKeys) {

  public RelationshipFocus {
    Objects.requireNonNull(primaryDimension, "primaryDimension");
    Objects.requireNonNull(secondaryDimension, "secondaryDimension");
    if (primaryDimension == secondaryDimension) {
      throw new IllegalArgumentException("relationship focus dimensions must be distinct");
    }
    if (primaryProminenceWeight <= 0 || secondaryProminenceWeight <= 0) {
      throw new IllegalArgumentException("relationship focus requires positive prominence");
    }
    if (tied != (primaryProminenceWeight == secondaryProminenceWeight)) {
      throw new IllegalArgumentException("relationship focus tie flag is inconsistent");
    }
    primaryEvidenceKeys = copyEvidenceKeys(primaryEvidenceKeys);
    secondaryEvidenceKeys = copyEvidenceKeys(secondaryEvidenceKeys);
  }

  private static List<String> copyEvidenceKeys(List<String> keys) {
    keys = keys == null ? List.of() : List.copyOf(keys);
    if (keys.isEmpty() || keys.stream().anyMatch(String::isBlank)) {
      throw new IllegalArgumentException("relationship focus requires evidence keys");
    }
    return keys;
  }
}
