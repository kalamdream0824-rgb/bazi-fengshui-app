package com.bazi.app.report.wealth;

import com.bazi.app.dto.HideGanDto;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.PillarDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualFact;
import com.bazi.app.report.TenGodGroup;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WealthFactExtractor {

  private static final List<String> PILLAR_KEYS = List.of("year", "month", "day", "time");

  public WealthNatalProfile extractNatal(PaipanResultDto chart, AnnualContext context) {
    Objects.requireNonNull(chart, "chart");
    Objects.requireNonNull(context, "context");
    validateChart(chart);

    List<WealthNatalProfile.TenGodOccurrence> occurrences = new ArrayList<>();
    Map<TenGodGroup, Integer> counts = new EnumMap<>(TenGodGroup.class);
    Map<String, WealthEvidence> evidence = new LinkedHashMap<>();
    addEvidence(evidence, new WealthEvidence(
        "natal.balance",
        EvidenceFamily.NATAL_STRUCTURE,
        "命局承载状态",
        context.natalAnalysis().wangShuaiLevel()));

    for (String pillarKey : PILLAR_KEYS) {
      PillarDto pillar = chart.pillars().get(pillarKey);
      if (isTenGod(pillar.shiShen())) {
        addOccurrence(
            occurrences, counts, evidence, pillar.shiShen(), pillarKey + ".stem", true);
      }
      List<HideGanDto> hidden = pillar.hideGan();
      for (int index = 0; index < hidden.size(); index++) {
        HideGanDto item = hidden.get(index);
        if (isTenGod(item.shiShen())) {
          addOccurrence(
              occurrences,
              counts,
              evidence,
              item.shiShen(),
              pillarKey + ".branch.hidden." + index,
              false);
        }
      }
    }

    boolean hasOutput = counts.getOrDefault(TenGodGroup.OUTPUT, 0) > 0;
    boolean hasWealth = counts.getOrDefault(TenGodGroup.WEALTH, 0) > 0;
    boolean hasPeer = counts.getOrDefault(TenGodGroup.PEER, 0) > 0;
    if (hasOutput && hasWealth) {
      addEvidence(evidence, new WealthEvidence(
          "natal.combination.output_wealth",
          EvidenceFamily.NATAL_COMBINATION,
          "原局组合",
          "表达与财富同时存在"));
    }
    if (hasWealth && !"偏弱".equals(context.natalAnalysis().wangShuaiLevel())) {
      addEvidence(evidence, new WealthEvidence(
          "natal.combination.wealth_capacity",
          EvidenceFamily.NATAL_COMBINATION,
          "原局组合",
          "财富存在且承载状态不弱"));
    }
    if (hasPeer && hasWealth) {
      addEvidence(evidence, new WealthEvidence(
          "natal.combination.peer_wealth",
          EvidenceFamily.NATAL_COMBINATION,
          "原局组合",
          "同类与财富同时存在"));
    }

    return new WealthNatalProfile(
        chart.pillars().get("day").gan(),
        context.natalAnalysis().wangShuaiLevel(),
        occurrences,
        counts,
        List.copyOf(evidence.values()));
  }

  public List<WealthYearFacts> extract(
      PaipanResultDto chart,
      List<AnnualContext> contexts) {
    Objects.requireNonNull(chart, "chart");
    if (contexts == null || contexts.isEmpty()) {
      throw new IllegalArgumentException("wealth facts require annual contexts");
    }
    WealthNatalProfile natal = extractNatal(chart, contexts.get(0));
    List<WealthYearFacts> years = new ArrayList<>();
    for (int index = 0; index < contexts.size(); index++) {
      AnnualContext context = contexts.get(index);
      if (index > 0 && context.year() != contexts.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("wealth annual contexts must be consecutive");
      }
      Map<String, WealthEvidence> evidence = new LinkedHashMap<>();
      addEvidence(evidence, new WealthEvidence(
          "annual.stem.ten_god",
          EvidenceFamily.ANNUAL_TRIGGER,
          "流年天干十神",
          context.yearStemTenGod()));
      String dayunTenGod = context.activeDaYun() == null ? null : context.activeDaYun().shiShen();
      if (dayunTenGod != null && !dayunTenGod.isBlank()) {
        addEvidence(evidence, new WealthEvidence(
            "dayun.stem.ten_god",
            EvidenceFamily.DAYUN_CONTEXT,
            "大运天干十神",
            dayunTenGod));
      }
      for (AnnualFact fact : context.natalRelations()) {
        addEvidence(evidence, fromAnnualFact(fact, EvidenceFamily.ANNUAL_TRIGGER));
      }
      for (AnnualFact fact : context.dayunRelations()) {
        addEvidence(evidence, fromAnnualFact(fact, EvidenceFamily.DAYUN_CONTEXT));
      }
      years.add(new WealthYearFacts(
          context.year(),
          context.ganZhi(),
          context.yearStemTenGod(),
          context.yearStemGroup(),
          dayunTenGod,
          natal,
          List.copyOf(evidence.values())));
    }
    return List.copyOf(years);
  }

  private void validateChart(PaipanResultDto chart) {
    if (chart.pillars() == null) {
      throw new IllegalArgumentException("wealth facts require four pillars");
    }
    for (String key : PILLAR_KEYS) {
      PillarDto pillar = chart.pillars().get(key);
      if (pillar == null || pillar.gan() == null || pillar.gan().isBlank()
          || pillar.zhi() == null || pillar.zhi().isBlank() || pillar.hideGan() == null) {
        throw new IllegalArgumentException("invalid wealth pillar: " + key);
      }
    }
  }

  private void addOccurrence(
      List<WealthNatalProfile.TenGodOccurrence> occurrences,
      Map<TenGodGroup, Integer> counts,
      Map<String, WealthEvidence> evidence,
      String tenGod,
      String position,
      boolean visible) {
    TenGodGroup group = TenGodGroup.fromTenGod(tenGod);
    occurrences.add(new WealthNatalProfile.TenGodOccurrence(tenGod, group, position, visible));
    counts.merge(group, 1, Integer::sum);
    addEvidence(evidence, new WealthEvidence(
        "natal.ten_god." + position + "." + tenGod,
        EvidenceFamily.NATAL_STRUCTURE,
        visible ? "明现十神" : "藏干十神",
        position + "：" + tenGod));
  }

  private boolean isTenGod(String value) {
    return value != null && !value.isBlank() && !"日主".equals(value);
  }

  private WealthEvidence fromAnnualFact(AnnualFact fact, EvidenceFamily family) {
    return new WealthEvidence(fact.key(), family, fact.label(), fact.value());
  }

  private void addEvidence(Map<String, WealthEvidence> evidence, WealthEvidence item) {
    if (evidence.putIfAbsent(item.key(), item) != null) {
      throw new IllegalArgumentException("duplicate wealth evidence key: " + item.key());
    }
  }
}
