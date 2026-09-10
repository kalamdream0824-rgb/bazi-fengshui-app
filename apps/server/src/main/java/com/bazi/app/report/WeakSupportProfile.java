package com.bazi.app.report;

public enum WeakSupportProfile {
  NOT_WEAK("不适用"),
  ROOTLESS("偏弱·无根"),
  ROOTED("偏弱·有根"),
  ROOTED_WITH_VISIBLE_RESOURCE("偏弱·有根有印");

  private final String label;

  WeakSupportProfile(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
