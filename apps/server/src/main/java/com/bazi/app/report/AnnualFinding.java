package com.bazi.app.report;

import java.util.List;

public record AnnualFinding(String title, String explanation, List<String> evidenceKeys) {

  public AnnualFinding {
    evidenceKeys = evidenceKeys == null ? List.of() : List.copyOf(evidenceKeys);
  }
}
