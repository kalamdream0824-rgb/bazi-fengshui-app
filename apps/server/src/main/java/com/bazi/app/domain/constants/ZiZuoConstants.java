package com.bazi.app.domain.constants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 自坐（本柱天干在本柱地支的十二长生）业务常量。
 * 口径与前端 lunar-javascript 的 LunarUtil.CHANG_SHENG / CHANG_SHENG_OFFSET 完全一致：
 * 阳干顺行、阴干逆行，从长生起点推算十二长生。
 */
public final class ZiZuoConstants {

  private ZiZuoConstants() {
  }

  public static final String[] CHANG_SHENG = {"长生", "沐浴", "冠带", "临官", "帝旺", "衰", "病", "死", "墓", "绝", "胎", "养"};

  /** 天干长生起点（地支 0-based 序：子0 丑1 … 亥11） */
  public static final Map<String, Integer> CHANG_SHENG_OFFSET = Map.of(
      "甲", 1, "乙", 6, "丙", 10, "丁", 9, "戊", 10,
      "己", 9, "庚", 7, "辛", 0, "壬", 4, "癸", 3);

  public static final List<String> ZHI = Arrays.asList("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥");

  /** 以本柱天干为太极点，查天干在本柱地支上的十二长生状态 */
  public static String ziZuo(String gan, String zhi) {
    Integer offset = CHANG_SHENG_OFFSET.get(gan);
    int zhiIndex = ZHI.indexOf(zhi);
    if (offset == null || zhiIndex < 0) {
      return "";
    }
    boolean yang = "甲丙戊庚壬".contains(gan);
    int index = offset + (yang ? zhiIndex : -zhiIndex);
    if (index >= 12) {
      index -= 12;
    }
    if (index < 0) {
      index += 12;
    }
    return CHANG_SHENG[index];
  }
}
