package com.bazi.app.report.wealth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WealthNarrativePlanner {

  public WealthNarrativePlan plan(WealthPeriodEvaluation period) {
    if (period == null) throw new IllegalArgumentException("wealth evaluation is required");
    WealthPath primary = mostFrequent(period, true, Set.of());
    WealthPath secondary = mostFrequent(period, false, Set.of(primary));
    WealthPath risk = mostFrequentRisk(period);

    List<WealthNarrativePlan.PathSummary> paths = new ArrayList<>();
    for (WealthPath path : WealthPath.values()) {
      paths.add(new WealthNarrativePlan.PathSummary(
          path.code(),
          path.label(),
          status(path, primary, secondary, risk),
          WealthPlainCopy.get("path." + path.code()),
          evidenceKeys(period, path, true),
          evidenceKeys(period, path, false)));
    }

    WealthNarrativePlan.RiskSummary mainRisk = new WealthNarrativePlan.RiskSummary(
        risk.code(), risk.label(), WealthPlainCopy.get("risk." + risk.code()),
        evidenceKeys(period, risk, false));

    List<WealthNarrativePlan.YearNarrative> years = new ArrayList<>();
    for (int index = 0; index < period.years().size(); index++) {
      years.add(year(period.years().get(index), index));
    }

    String summary = "主要看" + primary.label() + "，同时保留" + secondary.label()
        + "，并留意" + risk.label() + "。";
    return new WealthNarrativePlan(
        period.years().size(),
        WealthPlainCopy.get("thesis"),
        summary,
        paths,
        primary.code(),
        secondary.code(),
        mainRisk,
        years,
        List.of(
            WealthPlainCopy.get("route.1"),
            WealthPlainCopy.get("route.2"),
            WealthPlainCopy.get("route.3")));
  }

  private WealthNarrativePlan.YearNarrative year(WealthYearEvaluation year, int index) {
    WealthPath primary = year.primaryIncomePath();
    WealthPath risk = year.mainRisk();
    String role = index == 0 ? "year.first" : index == 1 ? "year.second" : "year.later";
    LinkedHashSet<String> evidence = new LinkedHashSet<>();
    evidence.addAll(keys(year.paths().get(primary).support()));
    evidence.addAll(keys(year.paths().get(risk).limitations()));
    if (evidence.isEmpty()) evidence.addAll(keys(year.paths().get(primary).scoredEvidence()));

    return new WealthNarrativePlan.YearNarrative(
        year.year(),
        year.ganZhi(),
        WealthPlainCopy.get(role + ".focus"),
        WealthPlainCopy.get("income." + primary.code()),
        WealthPlainCopy.get(role + ".retention"),
        WealthPlainCopy.get("risk." + risk.code()),
        List.of(
            WealthPlainCopy.get("signal." + primary.code()),
            WealthPlainCopy.get(role + ".signal")),
        List.of(
            WealthPlainCopy.get("action." + primary.code()),
            WealthPlainCopy.get(role + ".action")),
        WealthPlainCopy.get(role + ".transition"),
        List.copyOf(evidence));
  }

  private WealthPath mostFrequent(
      WealthPeriodEvaluation period, boolean primaryOnly, Set<WealthPath> excluded) {
    Map<WealthPath, Integer> counts = new EnumMap<>(WealthPath.class);
    for (WealthYearEvaluation year : period.years()) {
      counts.merge(year.primaryIncomePath(), primaryOnly ? 1 : 2, Integer::sum);
      if (!primaryOnly) counts.merge(year.secondaryIncomePath(), 1, Integer::sum);
    }
    return counts.entrySet().stream()
        .filter(entry -> entry.getKey() != WealthPath.RETENTION)
        .filter(entry -> !excluded.contains(entry.getKey()))
        .max(Comparator.<Map.Entry<WealthPath, Integer>>comparingInt(Map.Entry::getValue)
            .thenComparing(entry -> -entry.getKey().ordinal()))
        .orElseThrow()
        .getKey();
  }

  private WealthPath mostFrequentRisk(WealthPeriodEvaluation period) {
    Map<WealthPath, Integer> counts = new EnumMap<>(WealthPath.class);
    for (WealthYearEvaluation year : period.years()) counts.merge(year.mainRisk(), 1, Integer::sum);
    return counts.entrySet().stream()
        .max(Comparator.<Map.Entry<WealthPath, Integer>>comparingInt(Map.Entry::getValue)
            .thenComparing(entry -> -entry.getKey().ordinal()))
        .orElseThrow()
        .getKey();
  }

  private String status(
      WealthPath path, WealthPath primary, WealthPath secondary, WealthPath risk) {
    if (path == primary) return "主要方向";
    if (path == secondary) return "辅助方向";
    if (path == risk) return "需要留意";
    return "表现一般";
  }

  private List<String> evidenceKeys(
      WealthPeriodEvaluation period, WealthPath path, boolean supporting) {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    for (WealthYearEvaluation year : period.years()) {
      WealthPathEvaluation evaluation = year.paths().get(path);
      keys.addAll(keys(supporting ? evaluation.support() : evaluation.limitations()));
    }
    return List.copyOf(keys);
  }

  private List<String> keys(List<WealthPathEvaluation.ScoredEvidence> evidence) {
    return evidence.stream().map(item -> item.evidence().key()).distinct().toList();
  }
}
