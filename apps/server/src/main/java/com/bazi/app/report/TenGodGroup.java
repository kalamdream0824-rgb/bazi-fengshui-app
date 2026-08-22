package com.bazi.app.report;

import java.util.Map;

public enum TenGodGroup {
  RESOURCE("resource", "生扶"),
  PEER("peer", "同类"),
  OUTPUT("output", "表达"),
  WEALTH("wealth", "财富"),
  AUTHORITY("authority", "责任");

  private static final Map<String, TenGodGroup> GROUPS = Map.of(
      "正印", RESOURCE,
      "偏印", RESOURCE,
      "比肩", PEER,
      "劫财", PEER,
      "食神", OUTPUT,
      "伤官", OUTPUT,
      "正财", WEALTH,
      "偏财", WEALTH,
      "正官", AUTHORITY,
      "七杀", AUTHORITY);

  private final String code;
  private final String label;

  TenGodGroup(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String code() {
    return code;
  }

  public String label() {
    return label;
  }

  public static TenGodGroup fromTenGod(String tenGod) {
    TenGodGroup group = GROUPS.get(tenGod);
    if (group == null) {
      throw new IllegalArgumentException("unknown Ten God: " + tenGod);
    }
    return group;
  }
}
