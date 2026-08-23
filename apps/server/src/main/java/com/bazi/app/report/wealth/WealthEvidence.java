package com.bazi.app.report.wealth;

import java.util.Objects;

public record WealthEvidence(
    String key,
    EvidenceFamily family,
    String label,
    String value) {

  public WealthEvidence {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(family, "family");
    Objects.requireNonNull(label, "label");
    Objects.requireNonNull(value, "value");
    if (key.isBlank() || label.isBlank() || value.isBlank()) {
      throw new IllegalArgumentException("wealth evidence fields must not be blank");
    }
  }
}
