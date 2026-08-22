package com.bazi.app.report.rules;

import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualFact;
import com.bazi.app.report.AnnualFindingType;
import com.bazi.app.report.AnnualRule;
import com.bazi.app.report.AnnualRuleResult;
import com.bazi.app.report.AnnualStage;
import com.bazi.app.report.ConfidenceLevel;
import com.bazi.app.report.ReportTopic;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

final class FactAnnualRule implements AnnualRule {

  private final String key;
  private final ReportTopic topic;
  private final String category;
  private final AnnualFindingType findingType;
  private final AnnualStage stage;
  private final int priority;
  private final ConfidenceLevel confidence;
  private final List<List<String>> evidenceGroups;
  private final List<String> counterSelectors;
  private final Predicate<AnnualContext> condition;
  private final AnnualRuleCopy copy;

  FactAnnualRule(
      String key,
      ReportTopic topic,
      String category,
      AnnualFindingType findingType,
      AnnualStage stage,
      int priority,
      ConfidenceLevel confidence,
      List<List<String>> evidenceGroups,
      List<String> counterSelectors,
      Predicate<AnnualContext> condition,
      AnnualRuleCopy copy) {
    this.key = Objects.requireNonNull(key);
    this.topic = Objects.requireNonNull(topic);
    this.category = Objects.requireNonNull(category);
    this.findingType = Objects.requireNonNull(findingType);
    this.stage = Objects.requireNonNull(stage);
    this.priority = priority;
    this.confidence = Objects.requireNonNull(confidence);
    this.evidenceGroups = List.copyOf(evidenceGroups);
    this.counterSelectors = List.copyOf(counterSelectors);
    this.condition = Objects.requireNonNull(condition);
    this.copy = Objects.requireNonNull(copy);
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public ReportTopic topic() {
    return topic;
  }

  @Override
  public int priority() {
    return priority;
  }

  @Override
  public boolean matches(AnnualContext context) {
    return condition.test(context)
        && evidenceGroups.stream().allMatch(group -> firstMatch(context, group) != null);
  }

  @Override
  public List<AnnualFact> supportingEvidence(AnnualContext context) {
    List<AnnualFact> facts = new ArrayList<>();
    for (List<String> group : evidenceGroups) {
      AnnualFact fact = firstMatch(context, group);
      if (fact != null && facts.stream().noneMatch(existing -> existing.key().equals(fact.key()))) {
        facts.add(fact);
      }
    }
    return List.copyOf(facts);
  }

  @Override
  public List<AnnualFact> counterEvidence(AnnualContext context) {
    return context.facts().stream()
        .filter(fact -> counterSelectors.stream().anyMatch(selector -> matches(selector, fact.key())))
        .toList();
  }

  @Override
  public AnnualRuleResult build(AnnualContext context) {
    return new AnnualRuleResult(
        key,
        category,
        findingType,
        copy.get(key + ".headline"),
        copy.get(key + ".conclusion"),
        "copy." + topic.code() + ".plain",
        "copy." + topic.code() + ".professional",
        List.of(copy.get(key + ".action")),
        List.of(copy.get(key + ".signal")),
        stage,
        priority,
        confidence,
        List.of(),
        List.of());
  }

  private AnnualFact firstMatch(AnnualContext context, List<String> selectors) {
    return context.facts().stream()
        .filter(fact -> selectors.stream().anyMatch(selector -> matches(selector, fact.key())))
        .findFirst()
        .orElse(null);
  }

  private static boolean matches(String selector, String factKey) {
    if (selector.endsWith("*")) {
      return factKey.startsWith(selector.substring(0, selector.length() - 1));
    }
    return selector.equals(factKey);
  }
}
