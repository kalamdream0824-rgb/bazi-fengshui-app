package com.bazi.app.report;

import com.bazi.app.dto.DaYunDto;
import com.bazi.app.dto.HideGanDto;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.PillarDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReportAnalysis {

  private static final Map<String, String> GAN_WUXING = Map.of(
      "甲", "木", "乙", "木", "丙", "火", "丁", "火", "戊", "土",
      "己", "土", "庚", "金", "辛", "金", "壬", "水", "癸", "水");
  private static final Map<String, String> ZHI_WUXING = Map.ofEntries(
      Map.entry("子", "水"), Map.entry("丑", "土"), Map.entry("寅", "木"), Map.entry("卯", "木"),
      Map.entry("辰", "土"), Map.entry("巳", "火"), Map.entry("午", "火"), Map.entry("未", "土"),
      Map.entry("申", "金"), Map.entry("酉", "金"), Map.entry("戌", "土"), Map.entry("亥", "水"));
  private static final Map<String, String> SHENG = Map.of("木", "火", "火", "土", "土", "金", "金", "水", "水", "木");

  private final PaipanRequest request;
  private final PaipanResultDto result;
  private final Map<String, ReportEvidence> facts;
  private final double wangShuaiScore;
  private final String wangShuaiLevel;

  private ReportAnalysis(
      PaipanRequest request,
      PaipanResultDto result,
      Map<String, ReportEvidence> facts,
      double wangShuaiScore,
      String wangShuaiLevel) {
    this.request = request;
    this.result = result;
    this.facts = facts;
    this.wangShuaiScore = wangShuaiScore;
    this.wangShuaiLevel = wangShuaiLevel;
  }

  public static ReportAnalysis from(PaipanRequest request, PaipanResultDto result) {
    Map<String, ReportEvidence> facts = new LinkedHashMap<>();
    String pillars = String.join(" · ", List.of("year", "month", "day", "time").stream()
        .map(key -> ganZhi(result.pillars().get(key)))
        .toList());
    PillarDto day = result.pillars().get("day");
    String dayElement = GAN_WUXING.getOrDefault(day.gan(), "未识别");
    Score balance = scoreBalance(result, dayElement);
    String elementCounts = "木" + result.wuXing().getOrDefault("mu", 0)
        + "、火" + result.wuXing().getOrDefault("huo", 0)
        + "、土" + result.wuXing().getOrDefault("tu", 0)
        + "、金" + result.wuXing().getOrDefault("jin", 0)
        + "、水" + result.wuXing().getOrDefault("shui", 0);

    DaYunDto current = result.daYun().stream().filter(DaYunDto::isCurrent).findFirst()
        .orElse(result.daYun().isEmpty() ? null : result.daYun().get(0));
    DaYunDto next = null;
    if (current != null) {
      int index = result.daYun().indexOf(current);
      if (index >= 0 && index + 1 < result.daYun().size()) {
        next = result.daYun().get(index + 1);
      }
    }

    Set<String> tenGods = new LinkedHashSet<>();
    for (String key : List.of("year", "month", "day", "time")) {
      PillarDto pillar = result.pillars().get(key);
      if (pillar.shiShen() != null && !pillar.shiShen().isBlank() && !"日主".equals(pillar.shiShen())) {
        tenGods.add(pillar.shiShen());
      }
      pillar.hideGan().stream().map(HideGanDto::shiShen).filter(value -> value != null && !value.isBlank()).forEach(tenGods::add);
    }
    List<String> combinations = combinations(tenGods);
    String dayHidden = day.hideGan().stream().map(item -> item.gan() + "（" + item.shiShen() + "）").reduce((a, b) -> a + "、" + b).orElse("无");

    put(facts, "solar", "公历", result.solarText());
    put(facts, "lunar", "农历", result.lunarText());
    put(facts, "gender", "性别", request.isMale() ? "男" : "女");
    put(facts, "pillars", "四柱", pillars);
    put(facts, "dayMaster", "日主", day.gan() + "（" + dayElement + "）");
    put(facts, "sanYuan", "辅助坐标", "胎元" + result.taiYuan() + "、命宫" + result.mingGong() + "、身宫" + result.shenGong());
    put(facts, "elements", "五行计数", elementCounts);
    put(facts, "balance", "旺衰粗判", balance.level + "（" + formatScore(balance.score) + "分；" + String.join("；", balance.reasons) + "）");
    put(facts, "monthCommand", "月令", result.pillars().get("month").zhi() + "（" + ZHI_WUXING.get(result.pillars().get("month").zhi()) + "）");
    put(facts, "currentDayun", "当前大运", current == null ? "暂无" : current.ganZhi() + "，" + current.yearRange() + "，天干十神" + current.shiShen());
    put(facts, "nextDayun", "下一步大运", next == null ? "暂无" : next.ganZhi() + "，" + next.yearRange() + "，天干十神" + next.shiShen());
    put(facts, "currentYear", "当前流年", result.currentYearGanZhi());
    put(facts, "tenGods", "命局十神", tenGods.isEmpty() ? "无可用数据" : String.join("、", tenGods));
    put(facts, "combinations", "十神组合", combinations.isEmpty() ? "未命中预设组合" : String.join("、", combinations));
    put(facts, "dayHidden", "日支藏干", day.zhi() + "藏" + dayHidden + "；自坐" + day.ziZuo());
    put(facts, "structure", "结构摘要", day.gan() + "日主，" + balance.level + "，五行计数" + elementCounts);
    return new ReportAnalysis(request, result, facts, balance.score, balance.level);
  }

  public ReportEvidence fact(String key) {
    ReportEvidence value = facts.get(key);
    if (value == null) {
      throw new IllegalArgumentException("Unknown report evidence: " + key);
    }
    return value;
  }

  public List<ReportEvidence> facts(String... keys) {
    return List.of(keys).stream().map(this::fact).toList();
  }

  public PaipanRequest request() {
    return request;
  }

  public PaipanResultDto result() {
    return result;
  }

  public double wangShuaiScore() {
    return wangShuaiScore;
  }

  public String wangShuaiLevel() {
    return wangShuaiLevel;
  }

  private static void put(Map<String, ReportEvidence> facts, String key, String label, String value) {
    facts.put(key, new ReportEvidence(key, label, value == null || value.isBlank() ? "暂无" : value));
  }

  private static String ganZhi(PillarDto pillar) {
    return pillar.gan() + pillar.zhi();
  }

  private static Score scoreBalance(PaipanResultDto result, String dayElement) {
    double score = 0;
    List<String> reasons = new ArrayList<>();
    String monthElement = ZHI_WUXING.get(result.pillars().get("month").zhi());
    if (supports(dayElement, monthElement)) {
      score += 2;
      reasons.add("月令生扶+2");
    } else {
      reasons.add("月令不直接生扶+0");
    }
    for (String key : List.of("year", "day", "time")) {
      PillarDto pillar = result.pillars().get(key);
      if (supports(dayElement, ZHI_WUXING.get(pillar.zhi()))) {
        score += 1;
        reasons.add(pillar.zhi() + "支生扶+1");
      }
    }
    for (String key : List.of("year", "month", "time")) {
      String element = GAN_WUXING.get(result.pillars().get(key).gan());
      if (dayElement.equals(element)) {
        score += 1;
        reasons.add(result.pillars().get(key).gan() + "同类+1");
      } else if (dayElement.equals(SHENG.get(element))) {
        score += 0.5;
        reasons.add(result.pillars().get(key).gan() + "生扶+0.5");
      }
    }
    return new Score(score, score >= 4 ? "偏强" : score <= 1.5 ? "偏弱" : "中和", reasons);
  }

  private static boolean supports(String dayElement, String otherElement) {
    return dayElement.equals(otherElement) || dayElement.equals(SHENG.get(otherElement));
  }

  private static List<String> combinations(Set<String> tenGods) {
    List<String> hits = new ArrayList<>();
    if (tenGods.contains("伤官") && tenGods.contains("正印")) hits.add("伤官配印");
    if (tenGods.contains("食神") && (tenGods.contains("正财") || tenGods.contains("偏财"))) hits.add("食神生财");
    if (tenGods.contains("七杀") && (tenGods.contains("正印") || tenGods.contains("偏印"))) hits.add("杀印相生");
    if (tenGods.contains("正官") && tenGods.contains("正印")) hits.add("官印相生");
    if ((tenGods.contains("比肩") || tenGods.contains("劫财")) && (tenGods.contains("正财") || tenGods.contains("偏财"))) hits.add("比劫见财");
    return hits;
  }

  private static String formatScore(double score) {
    return score == Math.rint(score) ? Integer.toString((int) score) : Double.toString(score);
  }

  private record Score(double score, String level, List<String> reasons) {}
}
