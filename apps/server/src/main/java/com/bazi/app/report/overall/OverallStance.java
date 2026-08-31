package com.bazi.app.report.overall;

public enum OverallStance {
  SUPPORTIVE("较顺"),
  BALANCED("平稳"),
  PRESSURED("需收紧"),
  MIXED("有机会也有牵制");

  private final String label;

  OverallStance(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
