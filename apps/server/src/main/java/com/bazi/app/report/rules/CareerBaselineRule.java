package com.bazi.app.report.rules;

import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualFact;
import com.bazi.app.report.AnnualFindingType;
import com.bazi.app.report.AnnualRule;
import com.bazi.app.report.AnnualRuleResult;
import com.bazi.app.report.AnnualStage;
import com.bazi.app.report.ConfidenceLevel;
import com.bazi.app.report.ReportTopic;
import com.bazi.app.report.TenGodGroup;
import java.util.List;

final class CareerBaselineRule implements AnnualRule {

  private static final String KEY = "career.baseline";
  private static final int PRIORITY = 40;
  private static final List<String> ADVERSE_DAYUN_RELATIONS = List.of(
      "annual.branch.clash.dayun",
      "annual.branch.punishment.dayun",
      "annual.branch.harm.dayun");
  private static final List<String> SUPPORTIVE_DAYUN_FACTS = List.of(
      "annual.branch.harmony.dayun",
      "dayun.stem.group.resource");

  private final AnnualRuleCopy copy;

  CareerBaselineRule(AnnualRuleCopy copy) {
    this.copy = copy;
  }

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public ReportTopic topic() {
    return ReportTopic.CAREER;
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
    if (annual == null || balance == null) return List.of();
    return List.of(annual, balance);
  }

  @Override
  public List<AnnualFact> counterEvidence(AnnualContext context) {
    BaselineDefinition definition = definition(context);
    List<String> selectors = definition.findingType() == AnnualFindingType.OPPORTUNITY
        ? ADVERSE_DAYUN_RELATIONS
        : SUPPORTIVE_DAYUN_FACTS;
    return context.facts().stream()
        .filter(fact -> selectors.contains(fact.key()))
        .toList();
  }

  @Override
  public AnnualRuleResult build(AnnualContext context) {
    BaselineDefinition definition = definition(context);
    String copyKey = "career.baseline."
        + context.yearStemGroup().code() + "." + balanceCode(context);
    return new AnnualRuleResult(
        KEY,
        definition.category(),
        definition.findingType(),
        copy.get(copyKey + ".headline"),
        copy.get(copyKey + ".conclusion"),
        "copy.career.plain",
        "copy.career.professional",
        List.of(copy.get(copyKey + ".action")),
        List.of(copy.get(copyKey + ".signal")),
        definition.stage(),
        PRIORITY,
        definition.confidence(),
        List.of(),
        List.of());
  }

  private BaselineDefinition definition(AnnualContext context) {
    String balance = balanceCode(context);
    boolean weak = balance.equals("weak");
    boolean strong = balance.equals("strong");
    return switch (context.yearStemGroup()) {
      case RESOURCE -> new BaselineDefinition(
          "baseline.preparation", AnnualFindingType.OPPORTUNITY,
          strong ? AnnualStage.ADVANCE : AnnualStage.PREPARE,
          strong ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM);
      case PEER -> weak || balance.equals("middle")
          ? new BaselineDefinition(
              "baseline.coordination", AnnualFindingType.OPPORTUNITY,
              AnnualStage.ADVANCE, ConfidenceLevel.MEDIUM)
          : new BaselineDefinition(
              "baseline.competition", AnnualFindingType.PRESSURE,
              AnnualStage.CAUTION, ConfidenceLevel.HIGH);
      case OUTPUT -> weak
          ? new BaselineDefinition(
              "baseline.output_load", AnnualFindingType.PRESSURE,
              AnnualStage.CAUTION, ConfidenceLevel.MEDIUM)
          : new BaselineDefinition(
              "baseline.visibility", AnnualFindingType.OPPORTUNITY,
              AnnualStage.ADVANCE, strong ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM);
      case WEALTH -> weak
          ? new BaselineDefinition(
              "baseline.delivery_load", AnnualFindingType.PRESSURE,
              AnnualStage.CAUTION, ConfidenceLevel.MEDIUM)
          : new BaselineDefinition(
              "baseline.delivery", AnnualFindingType.OPPORTUNITY,
              AnnualStage.ADVANCE, strong ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM);
      case AUTHORITY -> weak
          ? new BaselineDefinition(
              "baseline.responsibility_load", AnnualFindingType.PRESSURE,
              AnnualStage.CAUTION, ConfidenceLevel.MEDIUM)
          : new BaselineDefinition(
              "baseline.responsibility", AnnualFindingType.OPPORTUNITY,
              AnnualStage.ADVANCE, strong ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM);
    };
  }

  private AnnualFact annualGroupFact(AnnualContext context) {
    String key = "annual.stem.group." + context.yearStemGroup().code();
    return context.facts().stream().filter(fact -> fact.key().equals(key)).findFirst().orElse(null);
  }

  private AnnualFact balanceFact(AnnualContext context) {
    return context.facts().stream()
        .filter(fact -> fact.key().equals("natal.balance.weak")
            || fact.key().equals("natal.balance.middle")
            || fact.key().equals("natal.balance.strong"))
        .findFirst()
        .orElse(null);
  }

  private String balanceCode(AnnualContext context) {
    AnnualFact fact = balanceFact(context);
    if (fact == null) throw new IllegalArgumentException("career baseline requires a natal balance fact");
    return fact.key().substring("natal.balance.".length());
  }

  private record BaselineDefinition(
      String category,
      AnnualFindingType findingType,
      AnnualStage stage,
      ConfidenceLevel confidence) {}
}
