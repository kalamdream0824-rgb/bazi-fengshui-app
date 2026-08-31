package com.bazi.app.report.overall;

import java.util.Objects;

public record OverallEvidenceAngle(String primaryKey, String secondaryKey) {

  public OverallEvidenceAngle {
    Objects.requireNonNull(primaryKey, "primaryKey");
    secondaryKey = secondaryKey == null ? "" : secondaryKey;
    if (primaryKey.isBlank()) {
      throw new IllegalArgumentException("overall evidence angle requires a primary key");
    }
  }
}
