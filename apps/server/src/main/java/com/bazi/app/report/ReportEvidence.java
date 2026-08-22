package com.bazi.app.report;

public record ReportEvidence(String key, String label, String value) {

  public String display() {
    return label + "：" + value;
  }
}
