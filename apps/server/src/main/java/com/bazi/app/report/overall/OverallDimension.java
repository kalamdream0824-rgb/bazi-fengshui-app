package com.bazi.app.report.overall;

public enum OverallDimension {
  RHYTHM("rhythm", "生活节奏"),
  CAREER("career", "事业责任"),
  WEALTH("wealth", "钱财安排"),
  RELATIONSHIP("relationship", "关系支持");

  private final String code;
  private final String label;

  OverallDimension(String code, String label) {
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
