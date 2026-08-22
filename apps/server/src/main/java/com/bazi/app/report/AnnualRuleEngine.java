package com.bazi.app.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AnnualRuleEngine {

  private static final int MAX_PER_FINDING_TYPE = 2;
  private static final int HIGH_PRIORITY = 80;
  private static final Comparator<AnnualRuleResult> RANKING =
      Comparator.comparingInt(AnnualRuleResult::priority)
          .reversed()
          .thenComparing(AnnualRuleResult::confidence, Comparator.reverseOrder())
          .thenComparing(AnnualRuleResult::ruleKey);

  public List<AnnualRuleResult> evaluate(
      AnnualContext context,
      ReportTopic selectedTopic,
      List<AnnualRule> rules) {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(selectedTopic, "selectedTopic");
    Objects.requireNonNull(rules, "rules");

    List<AnnualRuleResult> eligible = new ArrayList<>();
    for (AnnualRule rule : rules) {
      if (rule.topic() != selectedTopic || !rule.matches(context)) continue;
      List<AnnualFact> evidence = independent(rule.supportingEvidence(context));
      if (evidence.size() < 2) continue;
      List<AnnualFact> counterEvidence = independent(rule.counterEvidence(context));
      AnnualRuleResult draft = Objects.requireNonNull(rule.build(context), "rule result");
      if (!rule.key().equals(draft.ruleKey())) {
        throw new IllegalArgumentException("rule key differs from result key: " + rule.key());
      }
      ConfidenceLevel confidence = counterEvidence.isEmpty()
          ? draft.confidence()
          : lower(draft.confidence());
      eligible.add(draft.withEvaluation(rule.priority(), confidence, evidence, counterEvidence));
    }

    eligible.sort(RANKING);
    Map<String, AnnualRuleResult> byCategory = new LinkedHashMap<>();
    for (AnnualRuleResult result : eligible) {
      byCategory.putIfAbsent(result.category(), result);
    }

    int opportunities = 0;
    int pressures = 0;
    List<AnnualRuleResult> capped = new ArrayList<>();
    for (AnnualRuleResult result : byCategory.values()) {
      if (result.isOpportunity() && opportunities < MAX_PER_FINDING_TYPE) {
        capped.add(result);
        opportunities++;
      } else if (result.isPressure() && pressures < MAX_PER_FINDING_TYPE) {
        capped.add(result);
        pressures++;
      }
    }
    capped.sort(RANKING);
    return List.copyOf(capped);
  }

  public AnnualRuleEvaluation evaluateYear(
      AnnualContext context,
      ReportTopic selectedTopic,
      List<AnnualRule> rules) {
    List<AnnualRuleResult> results = evaluate(context, selectedTopic, rules);
    if (results.isEmpty()) {
      return new AnnualRuleEvaluation(
          selectedTopic,
          AnnualStage.STABLE,
          AssessmentBasis.INSUFFICIENT_EVIDENCE,
          List.of());
    }
    return new AnnualRuleEvaluation(
        selectedTopic,
        selectStage(results),
        AssessmentBasis.REVIEWED_RULES,
        results);
  }

  private AnnualStage selectStage(List<AnnualRuleResult> results) {
    if (results.stream().anyMatch(this::isHighPriorityTransition)) {
      return AnnualStage.TRANSITION;
    }
    long opportunities = results.stream().filter(AnnualRuleResult::isOpportunity).count();
    long pressures = results.stream().filter(AnnualRuleResult::isPressure).count();
    boolean materialPressure = results.stream()
        .filter(AnnualRuleResult::isPressure)
        .anyMatch(result -> result.confidence() != ConfidenceLevel.LOW);
    if (pressures > opportunities && materialPressure) {
      return AnnualStage.CAUTION;
    }
    boolean preparation = results.stream()
        .anyMatch(result -> result.stageHint() == AnnualStage.PREPARE);
    boolean outwardActivation = results.stream()
        .anyMatch(result -> result.stageHint() == AnnualStage.ADVANCE);
    if (preparation && !outwardActivation) {
      return AnnualStage.PREPARE;
    }
    boolean highConfidencePressure = results.stream()
        .filter(AnnualRuleResult::isPressure)
        .anyMatch(result -> result.confidence() == ConfidenceLevel.HIGH);
    if (opportunities > pressures && !highConfidencePressure) {
      return AnnualStage.ADVANCE;
    }
    return AnnualStage.STABLE;
  }

  private boolean isHighPriorityTransition(AnnualRuleResult result) {
    if (result.priority() < HIGH_PRIORITY) return false;
    return combinedEvidence(result).stream()
        .map(AnnualFact::key)
        .anyMatch(key -> key.startsWith("annual.branch.clash."));
  }

  private List<AnnualFact> combinedEvidence(AnnualRuleResult result) {
    List<AnnualFact> combined = new ArrayList<>(result.evidence());
    combined.addAll(result.counterEvidence());
    return combined;
  }

  private static List<AnnualFact> independent(List<AnnualFact> facts) {
    if (facts == null) return List.of();
    Set<String> keys = new LinkedHashSet<>();
    List<AnnualFact> unique = new ArrayList<>();
    for (AnnualFact fact : facts) {
      if (fact != null && keys.add(fact.key())) unique.add(fact);
    }
    return List.copyOf(unique);
  }

  private static ConfidenceLevel lower(ConfidenceLevel confidence) {
    return switch (confidence) {
      case HIGH -> ConfidenceLevel.MEDIUM;
      case MEDIUM, LOW -> ConfidenceLevel.LOW;
    };
  }
}
