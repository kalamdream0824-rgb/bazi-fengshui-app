package com.bazi.app.report;

import java.util.Objects;

public record AnnualFact(String key, String label, String value) {

  public AnnualFact {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(label, "label");
    Objects.requireNonNull(value, "value");
  }

  public ReportEvidence toEvidence() {
    return new ReportEvidence(key, label, value);
  }
}
