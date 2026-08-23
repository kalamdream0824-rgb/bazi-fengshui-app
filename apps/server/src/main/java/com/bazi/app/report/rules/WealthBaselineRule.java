package com.bazi.app.report.rules;

import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualFact;
import com.bazi.app.report.AnnualFindingType;
import com.bazi.app.report.AnnualRule;
import com.bazi.app.report.AnnualRuleResult;
import com.bazi.app.report.AnnualStage;
import com.bazi.app.report.ConfidenceLevel;
import com.bazi.app.report.ReportTopic;
import java.util.List;

final class WealthBaselineRule implements AnnualRule {

  private static final String KEY = "wealth.baseline";
  private static final int PRIORITY = 40;
  private final AnnualRuleCopy copy;

  WealthBaselineRule(AnnualRuleCopy copy) {
    this.copy = copy;
  }

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public ReportTopic topic() {
    return ReportTopic.WEALTH;
  }

  @Override
  public int priority() {
    return PRIORITY;
  }

  @Override
  public boolean matches(AnnualContext context) {
    return annualGroupFact(context) != null && balanceFact(context) != null;
  }

  @Override
  public List<AnnualFact> supportingEvidence(AnnualContext context) {
    AnnualFact annual = annualGroupFact(context);
    AnnualFact balance = balanceFact(context);
    return annual == null || balance == null ? List.of() : List.of(annual, balance);
  }

  @Override
  public List<AnnualFact> counterEvidence(AnnualContext context) {
    return List.of();
  }

  @Override
  public AnnualRuleResult build(AnnualContext context) {
    Definition definition = definition(context);
    String copyKey = "wealth.baseline." + context.yearStemGroup().code();
    return new AnnualRuleResult(
        KEY,
        definition.category(),
        definition.findingType(),
        copy.get(copyKey + ".headline"),
        copy.get(copyKey + ".conclusion"),
        "copy.wealth.plain",
        "copy.wealth.professional",
        List.of(copy.get(copyKey + ".action")),
        List.of(copy.get(copyKey + ".signal")),
        definition.stage(),
        PRIORITY,
        definition.confidence(),
        List.of(),
        List.of());
  }

  private Definition definition(AnnualContext context) {
    boolean weak = balanceCode(context).equals("weak");
    return switch (context.yearStemGroup()) {
      case RESOURCE -> new Definition(
          "baseline.reserve", AnnualFindingType.OPPORTUNITY,
          AnnualStage.PREPARE, ConfidenceLevel.MEDIUM);
      case PEER -> new Definition(
          "baseline.sharing", weak ? AnnualFindingType.OPPORTUNITY : AnnualFindingType.PRESSURE,
          weak ? AnnualStage.STABLE : AnnualStage.CAUTION, ConfidenceLevel.MEDIUM);
      case OUTPUT -> new Definition(
          "baseline.income_method", weak ? AnnualFindingType.PRESSURE : AnnualFindingType.OPPORTUNITY,
          weak ? AnnualStage.CAUTION : AnnualStage.ADVANCE, ConfidenceLevel.MEDIUM);
      case WEALTH -> new Definition(
          "baseline.income", weak ? AnnualFindingType.PRESSURE : AnnualFindingType.OPPORTUNITY,
          weak ? AnnualStage.CAUTION : AnnualStage.ADVANCE,
          weak ? ConfidenceLevel.MEDIUM : ConfidenceLevel.HIGH);
      case AUTHORITY -> new Definition(
          "baseline.rules", AnnualFindingType.PRESSURE,
          weak ? AnnualStage.CAUTION : AnnualStage.STABLE, ConfidenceLevel.MEDIUM);
    };
  }

  private AnnualFact annualGroupFact(AnnualContext context) {
    String key = "annual.stem.group." + context.yearStemGroup().code();
    return context.facts().stream().filter(fact -> fact.key().equals(key)).findFirst().orElse(null);
  }

  private AnnualFact balanceFact(AnnualContext context) {
    return context.facts().stream()
        .filter(fact -> fact.key().startsWith("natal.balance."))
        .findFirst()
        .orElse(null);
  }

  private String balanceCode(AnnualContext context) {
    AnnualFact fact = balanceFact(context);
    if (fact == null) throw new IllegalArgumentException("wealth baseline requires a natal balance fact");
    return fact.key().substring("natal.balance.".length());
  }

  private record Definition(
      String category,
      AnnualFindingType findingType,
      AnnualStage stage,
      ConfidenceLevel confidence) {}
}
