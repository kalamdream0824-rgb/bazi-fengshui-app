package com.bazi.app.report;

import java.util.List;
import java.util.Objects;

public record AnnualRuleResult(
    String ruleKey,
    String category,
    AnnualFindingType findingType,
    String headline,
    String conclusion,
    String plainCopyKey,
    String professionalCopyKey,
    List<String> actions,
    List<String> realitySignals,
    AnnualStage stageHint,
    int priority,
    ConfidenceLevel confidence,
    List<AnnualFact> evidence,
    List<AnnualFact> counterEvidence) {

  public AnnualRuleResult {
    Objects.requireNonNull(ruleKey, "ruleKey");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(findingType, "findingType");
    Objects.requireNonNull(headline, "headline");
    Objects.requireNonNull(conclusion, "conclusion");
    Objects.requireNonNull(plainCopyKey, "plainCopyKey");
    Objects.requireNonNull(professionalCopyKey, "professionalCopyKey");
    Objects.requireNonNull(stageHint, "stageHint");
    Objects.requireNonNull(confidence, "confidence");
    actions = actions == null ? List.of() : List.copyOf(actions);
    realitySignals = realitySignals == null ? List.of() : List.copyOf(realitySignals);
    evidence = evidence == null ? List.of() : List.copyOf(evidence);
    counterEvidence = counterEvidence == null ? List.of() : List.copyOf(counterEvidence);
  }

  public boolean isOpportunity() {
    return findingType == AnnualFindingType.OPPORTUNITY;
  }

  public boolean isPressure() {
    return findingType == AnnualFindingType.PRESSURE;
  }

  AnnualRuleResult withEvaluation(
      int evaluatedPriority,
      ConfidenceLevel evaluatedConfidence,
      List<AnnualFact> supportingFacts,
      List<AnnualFact> counterFacts) {
    return new AnnualRuleResult(
        ruleKey,
        category,
        findingType,
        headline,
        conclusion,
        plainCopyKey,
        professionalCopyKey,
        actions,
        realitySignals,
        stageHint,
        evaluatedPriority,
        evaluatedConfidence,
        supportingFacts,
        counterFacts);
  }
}
