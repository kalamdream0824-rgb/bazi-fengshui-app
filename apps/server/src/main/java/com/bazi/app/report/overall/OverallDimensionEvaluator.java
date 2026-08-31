package com.bazi.app.report.overall;

import com.bazi.app.report.AnnualContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OverallDimensionEvaluator {

  public List<OverallDimensionEvaluation> evaluate(AnnualContext context) {
    Set<String> facts = context.factKeys();
    Map<OverallDimension, Score> scores = new LinkedHashMap<>();
    for (OverallDimension dimension : OverallDimension.values()) scores.put(dimension, new Score(facts));

    applyBalance(scores, facts);
    applyAnnualGroup(scores, context.yearStemGroup().code());
    applyDayunGroups(scores, facts);
    applyRelations(scores, facts);

    return scores.entrySet().stream()
        .map(entry -> entry.getValue().toEvaluation(entry.getKey()))
        .toList();
  }

  private void applyBalance(Map<OverallDimension, Score> scores, Set<String> facts) {
    String key = facts.contains("natal.balance.weak")
        ? "natal.balance.weak"
        : facts.contains("natal.balance.strong")
            ? "natal.balance.strong"
            : "natal.balance.middle";
    for (Score score : scores.values()) {
      if ("natal.balance.weak".equals(key)) score.limit(key, 2);
      else score.support(key, 1);
    }
  }

  private void applyAnnualGroup(Map<OverallDimension, Score> scores, String group) {
    String key = "annual.stem.group." + group;
    switch (group) {
      case "resource" -> {
        scores.get(OverallDimension.RHYTHM).support(key, 3);
        scores.get(OverallDimension.CAREER).support(key, 2);
        scores.get(OverallDimension.WEALTH).support(key, 1);
        scores.get(OverallDimension.RELATIONSHIP).support(key, 2);
      }
      case "peer" -> {
        scores.get(OverallDimension.RHYTHM).support(key, 1);
        scores.get(OverallDimension.CAREER).support(key, 2);
        scores.get(OverallDimension.WEALTH).limit(key, 3);
        scores.get(OverallDimension.RELATIONSHIP).support(key, 2);
      }
      case "output" -> {
        scores.get(OverallDimension.RHYTHM).limit(key, 1);
        scores.get(OverallDimension.CAREER).support(key, 3);
        scores.get(OverallDimension.WEALTH).support(key, 2);
        scores.get(OverallDimension.RELATIONSHIP).support(key, 1);
      }
      case "wealth" -> {
        scores.get(OverallDimension.RHYTHM).limit(key, 2);
        scores.get(OverallDimension.CAREER).support(key, 2);
        scores.get(OverallDimension.WEALTH).support(key, 3);
        scores.get(OverallDimension.RELATIONSHIP).limit(key, 1);
      }
      case "authority" -> {
        scores.get(OverallDimension.RHYTHM).limit(key, 2);
        scores.get(OverallDimension.CAREER).support(key, 3);
        scores.get(OverallDimension.WEALTH).limit(key, 1);
        scores.get(OverallDimension.RELATIONSHIP).limit(key, 1);
      }
      default -> throw new IllegalArgumentException("unsupported annual group: " + group);
    }
  }

  private void applyDayunGroups(Map<OverallDimension, Score> scores, Set<String> facts) {
    addWhenPresent(scores.get(OverallDimension.RHYTHM), facts, "dayun.stem.group.resource", 2, true);
    addWhenPresent(scores.get(OverallDimension.CAREER), facts, "dayun.stem.group.authority", 2, true);
    addWhenPresent(scores.get(OverallDimension.CAREER), facts, "dayun.stem.group.output", 2, true);
    addWhenPresent(scores.get(OverallDimension.WEALTH), facts, "dayun.stem.group.wealth", 2, true);
    addWhenPresent(scores.get(OverallDimension.WEALTH), facts, "dayun.stem.group.output", 1, true);
    addWhenPresent(scores.get(OverallDimension.RELATIONSHIP), facts, "dayun.stem.group.peer", 1, true);
    addWhenPresent(scores.get(OverallDimension.RELATIONSHIP), facts, "dayun.stem.group.resource", 1, true);
  }

  private void applyRelations(Map<OverallDimension, Score> scores, Set<String> facts) {
    for (String key : facts) {
      if (!key.startsWith("annual.branch.")) continue;
      boolean harmony = key.contains(".harmony.");
      boolean disruptive = key.contains(".clash.") || key.contains(".harm.")
          || key.contains(".punishment.") || key.contains(".break.");
      if (!harmony && !disruptive) continue;
      boolean day = key.endsWith(".day");
      boolean dayun = key.endsWith(".dayun");
      if (harmony) {
        scores.get(OverallDimension.RHYTHM).support(key, dayun ? 3 : 1);
        scores.get(OverallDimension.CAREER).support(key, dayun ? 2 : 1);
        scores.get(OverallDimension.WEALTH).support(key, 1);
        scores.get(OverallDimension.RELATIONSHIP).support(key, day ? 3 : 1);
      } else {
        scores.get(OverallDimension.RHYTHM).limit(key, dayun ? 3 : 1);
        scores.get(OverallDimension.CAREER).limit(key, dayun ? 3 : 1);
        scores.get(OverallDimension.WEALTH).limit(key, dayun ? 2 : 1);
        scores.get(OverallDimension.RELATIONSHIP).limit(key, day ? 3 : 1);
      }
    }
  }

  private void addWhenPresent(Score score, Set<String> facts, String key, int weight, boolean support) {
    if (!facts.contains(key)) return;
    if (support) score.support(key, weight);
    else score.limit(key, weight);
  }

  private static final class Score {
    private final Set<String> availableFacts;
    private final Map<String, Integer> support = new LinkedHashMap<>();
    private final Map<String, Integer> limitations = new LinkedHashMap<>();

    private Score(Set<String> availableFacts) {
      this.availableFacts = availableFacts;
    }

    private void support(String key, int weight) {
      add(support, limitations, key, weight);
    }

    private void limit(String key, int weight) {
      add(limitations, support, key, weight);
    }

    private void add(Map<String, Integer> target, Map<String, Integer> opposite, String key, int weight) {
      if (!availableFacts.contains(key) || weight <= 0 || opposite.containsKey(key)) return;
      target.merge(key, weight, Math::max);
    }

    private OverallDimensionEvaluation toEvaluation(OverallDimension dimension) {
      int supportWeight = support.values().stream().mapToInt(Integer::intValue).sum();
      int limitationWeight = limitations.values().stream().mapToInt(Integer::intValue).sum();
      OverallStance stance;
      if (supportWeight >= limitationWeight + 2) stance = OverallStance.SUPPORTIVE;
      else if (limitationWeight >= supportWeight + 2) stance = OverallStance.PRESSURED;
      else if (supportWeight > 0 && limitationWeight > 0) stance = OverallStance.MIXED;
      else stance = OverallStance.BALANCED;
      LinkedHashSet<String> direct = new LinkedHashSet<>();
      support.keySet().stream().filter(key -> key.startsWith("annual.")).forEach(direct::add);
      limitations.keySet().stream().filter(key -> key.startsWith("annual.")).forEach(direct::add);
      return new OverallDimensionEvaluation(
          dimension,
          stance,
          supportWeight,
          limitationWeight,
          new ArrayList<>(support.keySet()),
          new ArrayList<>(limitations.keySet()),
          new ArrayList<>(direct));
    }
  }
}
