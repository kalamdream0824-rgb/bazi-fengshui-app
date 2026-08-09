package com.bazi.app.domain.constants;

import java.util.Map;

/** 天干地支五行对照（业务领域常量，供计算层使用） */
public final class WuXingConstants {

  private WuXingConstants() {
  }

  public static final Map<String, String> GAN_WUXING = Map.of(
      "甲", "mu", "乙", "mu", "丙", "huo", "丁", "huo", "戊", "tu",
      "己", "tu", "庚", "jin", "辛", "jin", "壬", "shui", "癸", "shui");

  public static final Map<String, String> ZHI_WUXING = Map.ofEntries(
      Map.entry("子", "shui"),
      Map.entry("丑", "tu"),
      Map.entry("寅", "mu"),
      Map.entry("卯", "mu"),
      Map.entry("辰", "tu"),
      Map.entry("巳", "huo"),
      Map.entry("午", "huo"),
      Map.entry("未", "tu"),
      Map.entry("申", "jin"),
      Map.entry("酉", "jin"),
      Map.entry("戌", "tu"),
      Map.entry("亥", "shui"));
}
