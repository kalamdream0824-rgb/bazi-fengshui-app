package com.bazi.app.report.rules;

import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualFindingType;
import com.bazi.app.report.AnnualRule;
import com.bazi.app.report.AnnualStage;
import com.bazi.app.report.ConfidenceLevel;
import com.bazi.app.report.ReportTopic;
import java.util.List;
import java.util.function.Predicate;

final class AnnualRuleFactory {
  private static final Predicate<AnnualContext> ALWAYS = context -> true;
  private final AnnualRuleCopy copy;

  AnnualRuleFactory(AnnualRuleCopy copy) {
    this.copy = copy;
  }

  AnnualRule rule(String key, ReportTopic topic, String category, AnnualFindingType type,
      AnnualStage stage, int priority, ConfidenceLevel confidence, List<List<String>> groups) {
    return rule(key, topic, category, type, stage, priority, confidence, groups, ALWAYS);
  }

  AnnualRule rule(String key, ReportTopic topic, String category, AnnualFindingType type,
      AnnualStage stage, int priority, ConfidenceLevel confidence, List<List<String>> groups,
      Predicate<AnnualContext> condition) {
    return new FactAnnualRule(
        key, topic, category, type, stage, priority, confidence, groups, List.of(), condition, copy);
  }

  @SafeVarargs
  static List<List<String>> groups(List<String>... groups) {
    return List.of(groups);
  }

  static List<String> one(String selector) {
    return List.of(selector);
  }

  static List<String> any(String... selectors) {
    return List.of(selectors);
  }
}
