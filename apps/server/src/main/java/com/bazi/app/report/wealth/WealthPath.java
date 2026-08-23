package com.bazi.app.report.wealth;

public enum WealthPath {
  STABLE_INCOME("stable_income", "稳定收入"),
  SKILL_INCOME("skill_income", "靠能力赚钱"),
  PROJECT_INCOME("project_income", "项目和额外收入"),
  COOPERATION_INCOME("cooperation_income", "合作带来的收入"),
  RETENTION("retention", "把钱留下");

  private final String code;
  private final String label;

  WealthPath(String code, String label) {
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
