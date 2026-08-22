package com.bazi.app.report;

public enum AnnualStage {
  PREPARE("蓄"),
  STABLE("稳"),
  ADVANCE("进"),
  TRANSITION("转"),
  CAUTION("慎");

  private final String label;

  AnnualStage(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
