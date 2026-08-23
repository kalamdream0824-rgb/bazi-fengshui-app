package com.bazi.app.report.wealth;

import com.bazi.app.report.TenGodGroup;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WealthPathEvaluator {

  public WealthPeriodEvaluation evaluate(List<WealthYearFacts> years) {
    if (years == null || years.isEmpty()) {
      throw new IllegalArgumentException("wealth path evaluation requires annual facts");
    }
    WealthArbitrator arbitrator = new WealthArbitrator();
    List<WealthYearEvaluation> result = years.stream()
        .map(year -> arbitrator.arbitrate(year.year(), year.ganZhi(), evaluatePaths(year)))
        .toList();
    return new WealthPeriodEvaluation(result);
  }

  public Map<WealthPath, WealthPathEvaluation> evaluatePaths(WealthYearFacts year) {
    Map<WealthPath, Accumulator> paths = new EnumMap<>(WealthPath.class);
    for (WealthPath path : WealthPath.values()) paths.put(path, new Accumulator(path));

    Map<String, WealthEvidence> natalEvidence = byKey(year.natalProfile().evidence());
    for (WealthNatalProfile.TenGodOccurrence item : year.natalProfile().tenGodOccurrences()) {
      WealthEvidence evidence = natalEvidence.get(
          "natal.ten_god." + item.position() + "." + item.tenGod());
      if ("正财".equals(item.tenGod())) {
        add(paths, WealthPath.STABLE_INCOME, evidence, item.visible() ? 3 : 1);
        add(paths, WealthPath.RETENTION, evidence, 1);
      } else if ("偏财".equals(item.tenGod())) {
        add(paths, WealthPath.PROJECT_INCOME, evidence, item.visible() ? 3 : 1);
      } else if (item.group() == TenGodGroup.OUTPUT) {
        add(paths, WealthPath.SKILL_INCOME, evidence, item.visible() ? 2 : 1);
        if (item.visible()) add(paths, WealthPath.PROJECT_INCOME, evidence, 1);
      }
    }

    addIfPresent(paths, WealthPath.SKILL_INCOME, natalEvidence,
        "natal.combination.output_wealth", 2);
    addIfPresent(paths, WealthPath.PROJECT_INCOME, natalEvidence,
        "natal.combination.output_wealth", 1);
    addIfPresent(paths, WealthPath.STABLE_INCOME, natalEvidence,
        "natal.combination.wealth_capacity", 1);
    addIfPresent(paths, WealthPath.PROJECT_INCOME, natalEvidence,
        "natal.combination.wealth_capacity", 1);
    addIfPresent(paths, WealthPath.RETENTION, natalEvidence,
        "natal.combination.wealth_capacity", 2);
    addIfPresent(paths, WealthPath.COOPERATION_INCOME, natalEvidence,
        "natal.combination.peer_wealth", -2);
    addIfPresent(paths, WealthPath.RETENTION, natalEvidence,
        "natal.combination.peer_wealth", -2);

    Map<String, WealthEvidence> annualEvidence = byKey(year.evidence());
    WealthEvidence annualStem = annualEvidence.get("annual.stem.ten_god");
    switch (year.annualStemTenGod()) {
      case "正财" -> {
        add(paths, WealthPath.STABLE_INCOME, annualStem, 4);
        add(paths, WealthPath.PROJECT_INCOME, annualStem, 1);
        add(paths, WealthPath.RETENTION, annualStem, 1);
      }
      case "偏财" -> {
        add(paths, WealthPath.STABLE_INCOME, annualStem, 1);
        add(paths, WealthPath.PROJECT_INCOME, annualStem, 4);
      }
      case "食神", "伤官" -> {
        add(paths, WealthPath.SKILL_INCOME, annualStem, 4);
        add(paths, WealthPath.PROJECT_INCOME, annualStem, 1);
      }
      default -> {
        if (year.annualStemGroup() == TenGodGroup.AUTHORITY) {
          add(paths, WealthPath.PROJECT_INCOME, annualStem, -1);
          add(paths, WealthPath.RETENTION, annualStem, 1);
        }
      }
    }

    for (WealthEvidence evidence : year.evidence()) {
      if (evidence.key().contains(".harmony.")) {
        add(paths, WealthPath.PROJECT_INCOME, evidence, 1);
        add(paths, WealthPath.COOPERATION_INCOME, evidence, 2);
      }
    }

    WealthEvidence dayunStem = annualEvidence.get("dayun.stem.ten_god");
    if (dayunStem != null) {
      if ("正财".equals(year.activeDayunTenGod())) {
        add(paths, WealthPath.STABLE_INCOME, dayunStem, 2);
        add(paths, WealthPath.PROJECT_INCOME, dayunStem, 1);
        add(paths, WealthPath.RETENTION, dayunStem, 1);
      } else if ("偏财".equals(year.activeDayunTenGod())) {
        add(paths, WealthPath.STABLE_INCOME, dayunStem, 1);
        add(paths, WealthPath.PROJECT_INCOME, dayunStem, 2);
        add(paths, WealthPath.RETENTION, dayunStem, 1);
      } else if ("比肩".equals(year.activeDayunTenGod())
          || "劫财".equals(year.activeDayunTenGod())) {
        add(paths, WealthPath.COOPERATION_INCOME, dayunStem, 1);
        add(paths, WealthPath.RETENTION, dayunStem, -2);
      }
    }

    boolean wealthActivated = year.annualStemGroup() == TenGodGroup.WEALTH
        || dayunStem != null && ("正财".equals(year.activeDayunTenGod())
            || "偏财".equals(year.activeDayunTenGod()));
    if (wealthActivated && "偏弱".equals(year.natalProfile().balanceLevel())) {
      add(paths, WealthPath.RETENTION, natalEvidence.get("natal.balance"), -3);
    }
    if (wealthActivated) {
      for (WealthEvidence evidence : year.evidence()) {
        if (isDisruptiveRelation(evidence.key())) {
          add(paths, WealthPath.COOPERATION_INCOME, evidence, -2);
          add(paths, WealthPath.RETENTION, evidence, -2);
        }
      }
    }

    Map<WealthPath, WealthPathEvaluation> result = new EnumMap<>(WealthPath.class);
    paths.forEach((path, value) -> result.put(path, value.build()));
    return Map.copyOf(result);
  }

  private Map<String, WealthEvidence> byKey(List<WealthEvidence> evidence) {
    Map<String, WealthEvidence> result = new LinkedHashMap<>();
    for (WealthEvidence item : evidence) result.putIfAbsent(item.key(), item);
    return result;
  }

  private boolean isDisruptiveRelation(String key) {
    return key.contains(".clash.") || key.contains(".punishment.") || key.contains(".harm.");
  }

  private void addIfPresent(
      Map<WealthPath, Accumulator> paths,
      WealthPath path,
      Map<String, WealthEvidence> evidence,
      String key,
      int weight) {
    WealthEvidence item = evidence.get(key);
    if (item != null) add(paths, path, item, weight);
  }

  private void add(
      Map<WealthPath, Accumulator> paths,
      WealthPath path,
      WealthEvidence evidence,
      int weight) {
    if (evidence == null) throw new IllegalArgumentException("missing wealth evidence for " + path);
    paths.get(path).add(evidence, weight);
  }

  private static final class Accumulator {
    private final WealthPath path;
    private final Map<String, WealthPathEvaluation.ScoredEvidence> evidence = new LinkedHashMap<>();

    private Accumulator(WealthPath path) {
      this.path = path;
    }

    private void add(WealthEvidence item, int weight) {
      evidence.putIfAbsent(
          item.key(), new WealthPathEvaluation.ScoredEvidence(item, weight));
    }

    private WealthPathEvaluation build() {
      List<WealthPathEvaluation.ScoredEvidence> scored = new ArrayList<>(evidence.values());
      int score = scored.stream().mapToInt(WealthPathEvaluation.ScoredEvidence::weight).sum();
      long families = scored.stream()
          .filter(item -> item.weight() > 0)
          .map(item -> item.evidence().family())
          .distinct()
          .count();
      String status = score >= 6 && families >= 2
          ? "重点"
          : score >= 3 ? "可以作为补充" : score >= 0 ? "表现一般" : "需要谨慎";
      return new WealthPathEvaluation(path, score, status, scored);
    }
  }
}
