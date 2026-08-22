package com.bazi.app.report;

import java.util.List;
import java.util.Objects;

public record YearAssessment(
    int year,
    String ganZhi,
    AnnualStage stage,
    String headline,
    String conclusion,
    List<AnnualFinding> opportunities,
    List<AnnualFinding> pressures,
    List<String> actions,
    List<String> realitySignals,
    List<ReportEvidence> evidence,
    List<ReportEvidence> counterEvidence,
    ConfidenceLevel confidence,
    List<String> ruleKeys,
    AssessmentBasis basis) {

  public YearAssessment {
    Objects.requireNonNull(ganZhi, "ganZhi");
    Objects.requireNonNull(stage, "stage");
    Objects.requireNonNull(headline, "headline");
    Objects.requireNonNull(conclusion, "conclusion");
    Objects.requireNonNull(confidence, "confidence");
    Objects.requireNonNull(basis, "basis");
    opportunities = copy(opportunities);
    pressures = copy(pressures);
    actions = copy(actions);
    realitySignals = copy(realitySignals);
    evidence = copy(evidence);
    counterEvidence = copy(counterEvidence);
    ruleKeys = copy(ruleKeys);
    long independentEvidence = evidence.stream()
        .map(ReportEvidence::key)
        .filter(Objects::nonNull)
        .distinct()
        .count();
    if (basis == AssessmentBasis.REVIEWED_RULES && independentEvidence < 2) {
      throw new IllegalArgumentException("annual conclusion requires at least two independent evidence items");
    }
    if (basis == AssessmentBasis.INSUFFICIENT_EVIDENCE
        && (confidence != ConfidenceLevel.LOW || !ruleKeys.isEmpty()
            || !opportunities.isEmpty() || !pressures.isEmpty())) {
      throw new IllegalArgumentException("insufficient-evidence result must be low confidence without rule findings");
    }
  }

  private static <T> List<T> copy(List<T> values) {
    return values == null ? List.of() : List.copyOf(values);
  }
}
