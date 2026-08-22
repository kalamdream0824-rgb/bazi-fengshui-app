package com.bazi.app.report;

public enum BranchRelation {
  CLASH("clash", "冲"),
  HARMONY("harmony", "合"),
  HARM("harm", "害"),
  BREAK("break", "破"),
  PUNISHMENT("punishment", "刑");

  private final String code;
  private final String label;

  BranchRelation(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String code() {
    return code;
  }

  public String label() {
    return label;
  }
}
