package com.bazi.app.report.relationship;

import java.util.Arrays;

public enum RelationshipStatus {
  SINGLE("single", "单身或尚未确定关系"),
  DATING("dating", "已确认交往关系"),
  MARRIED("married", "已婚或长期共同生活");

  private final String code;
  private final String label;

  RelationshipStatus(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String code() {
    return code;
  }

  public String label() {
    return label;
  }

  public static RelationshipStatus fromCode(String code) {
    return Arrays.stream(values())
        .filter(value -> value.code.equals(code))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unsupported relationship status: " + code));
  }
}
