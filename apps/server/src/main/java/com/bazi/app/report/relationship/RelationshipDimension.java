package com.bazi.app.report.relationship;

public enum RelationshipDimension {
  CONNECTION("connection", "关系连接"),
  RESPONSE("response", "回应与表达"),
  DAILY_COOPERATION("daily_cooperation", "日常配合"),
  BOUNDARIES("boundaries", "矛盾与边界"),
  STABILITY("stability", "长期稳定");

  private final String code;
  private final String label;

  RelationshipDimension(String code, String label) {
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
