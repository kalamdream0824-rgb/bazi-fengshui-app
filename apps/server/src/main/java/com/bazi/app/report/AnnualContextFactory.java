package com.bazi.app.report;

import com.bazi.app.dto.DaYunDto;
import com.bazi.app.dto.HideGanDto;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.PillarDto;
import com.nlf.calendar.Solar;
import com.nlf.calendar.util.LunarUtil;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AnnualContextFactory {

  private static final Pattern YEAR_RANGE = Pattern.compile("^\\s*(\\d{4})\\s*-\\s*(\\d{4})\\s*$");
  private static final List<String> PILLAR_KEYS = List.of("year", "month", "day", "time");

  private final Clock clock;

  public AnnualContextFactory(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public List<AnnualContext> create(PaipanRequest request, PaipanResultDto chart) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(chart, "chart");
    ReportAnalysis analysis = ReportAnalysis.from(request, chart);
    int firstYear = LocalDate.now(clock).getYear();
    String dayStem = chart.pillars().get("day").gan();
    List<AnnualContext> contexts = new ArrayList<>();
    for (int year = firstYear; year < firstYear + 3; year++) {
      String ganZhi = Solar.fromYmdHms(year, 7, 1, 12, 0, 0)
          .getLunar()
          .getYearInGanZhiExact();
      String annualStem = ganZhi.substring(0, 1);
      String annualBranch = ganZhi.substring(1, 2);
      String tenGod = LunarUtil.SHI_SHEN.get(dayStem + annualStem);
      if (tenGod == null || tenGod.isBlank()) {
        throw new IllegalStateException("cannot derive annual stem Ten God for " + dayStem + annualStem);
      }
      TenGodGroup group = TenGodGroup.fromTenGod(tenGod);
      DaYunDto activeDaYun = activeDaYun(chart.daYun(), year);
      contexts.add(new AnnualContext(
          year,
          ganZhi,
          tenGod,
          group,
          activeDaYun,
          structuredFacts(request, chart, analysis, activeDaYun, year, dayStem),
          natalRelations(annualBranch, chart),
          dayunRelations(annualBranch, activeDaYun),
          analysis));
    }
    return List.copyOf(contexts);
  }

  private static List<AnnualFact> structuredFacts(
      PaipanRequest request,
      PaipanResultDto chart,
      ReportAnalysis analysis,
      DaYunDto activeDaYun,
      int year,
      String dayStem) {
    Map<String, AnnualFact> facts = new LinkedHashMap<>();
    String balanceCode = switch (analysis.wangShuaiLevel()) {
      case "偏弱" -> "weak";
      case "中和" -> "middle";
      case "偏强" -> "strong";
      default -> throw new IllegalStateException("unknown balance level: " + analysis.wangShuaiLevel());
    };
    put(facts, "natal.balance." + balanceCode, "命局承载状态", analysis.wangShuaiLevel());
    if (!"weak".equals(balanceCode)) {
      put(facts, "natal.balance.adequate", "命局承载状态", analysis.wangShuaiLevel());
    }

    for (PillarDto pillar : chart.pillars().values()) {
      addTenGodGroup(facts, pillar.shiShen());
      for (HideGanDto hidden : pillar.hideGan()) addTenGodGroup(facts, hidden.shiShen());
    }

    String genderCode = request.isMale() ? "male" : "female";
    put(facts, "natal.gender." + genderCode, "性别取法", request.isMale() ? "男" : "女");
    String dayBranch = chart.pillars().get("day").zhi();
    put(facts, "natal.day.branch", "日支", dayBranch);
    put(facts, "natal.day.branch." + dayBranch, "日支", dayBranch);

    if (activeDaYun != null && activeDaYun.shiShen() != null && !activeDaYun.shiShen().isBlank()) {
      TenGodGroup dayunGroup = TenGodGroup.fromTenGod(activeDaYun.shiShen());
      put(facts, "dayun.stem.group." + dayunGroup.code(), "大运天干功能组", activeDaYun.shiShen());
    }

    String nextGanZhi = Solar.fromYmdHms(year + 1, 7, 1, 12, 0, 0)
        .getLunar()
        .getYearInGanZhiExact();
    String nextTenGod = LunarUtil.SHI_SHEN.get(dayStem + nextGanZhi.substring(0, 1));
    TenGodGroup nextGroup = TenGodGroup.fromTenGod(nextTenGod);
    put(facts, "next.annual.stem.group." + nextGroup.code(), "下一年天干功能组", nextTenGod);
    return List.copyOf(facts.values());
  }

  private static void addTenGodGroup(Map<String, AnnualFact> facts, String tenGod) {
    if (tenGod == null || tenGod.isBlank() || "日主".equals(tenGod)) return;
    TenGodGroup group = TenGodGroup.fromTenGod(tenGod);
    put(facts, "natal.ten_god.group." + group.code(), "原局十神功能组", tenGod);
  }

  private static void put(
      Map<String, AnnualFact> facts,
      String key,
      String label,
      String value) {
    facts.putIfAbsent(key, new AnnualFact(key, label, value));
  }

  static DaYunDto activeDaYun(List<DaYunDto> dayuns, int year) {
    if (dayuns == null) return null;
    for (DaYunDto dayun : dayuns) {
      Matcher matcher = YEAR_RANGE.matcher(dayun.yearRange());
      if (!matcher.matches()) {
        throw new IllegalArgumentException("invalid Dayun year range: " + dayun.yearRange());
      }
      int startYear = Integer.parseInt(matcher.group(1));
      int endYear = Integer.parseInt(matcher.group(2));
      if (endYear <= startYear) {
        throw new IllegalArgumentException("invalid Dayun year range: " + dayun.yearRange());
      }
      if (startYear <= year && year < endYear) return dayun;
    }
    return null;
  }

  private static List<AnnualFact> natalRelations(String annualBranch, PaipanResultDto chart) {
    List<AnnualFact> facts = new ArrayList<>();
    for (String pillarKey : PILLAR_KEYS) {
      PillarDto pillar = chart.pillars().get(pillarKey);
      if (pillar == null) {
        throw new IllegalArgumentException("missing natal pillar: " + pillarKey);
      }
      appendRelations(facts, annualBranch, pillar.zhi(), pillarKey);
    }
    return List.copyOf(facts);
  }

  private static List<AnnualFact> dayunRelations(String annualBranch, DaYunDto activeDaYun) {
    if (activeDaYun == null) return List.of();
    String ganZhi = activeDaYun.ganZhi();
    if (ganZhi == null || ganZhi.codePointCount(0, ganZhi.length()) != 2) {
      throw new IllegalArgumentException("invalid Dayun GanZhi: " + ganZhi);
    }
    int split = ganZhi.offsetByCodePoints(0, 1);
    String dayunBranch = ganZhi.substring(split);
    List<AnnualFact> facts = new ArrayList<>();
    appendRelations(facts, annualBranch, dayunBranch, "dayun");
    return List.copyOf(facts);
  }

  private static void appendRelations(
      List<AnnualFact> facts,
      String annualBranch,
      String targetBranch,
      String targetKey) {
    for (BranchRelation relation : BranchRelations.between(annualBranch, targetBranch).stream().sorted().toList()) {
      facts.add(new AnnualFact(
          "annual.branch." + relation.code() + "." + targetKey,
          "流年地支关系",
          annualBranch + "与" + targetBranch + relation.label() + "（" + targetKey + "）"));
    }
  }
}
