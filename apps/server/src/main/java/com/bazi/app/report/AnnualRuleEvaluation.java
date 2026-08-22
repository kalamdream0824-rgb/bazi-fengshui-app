package com.bazi.app.report;

import java.util.List;
import java.util.Objects;

public record AnnualRuleEvaluation(
    ReportTopic topic,
    AnnualStage stage,
    AssessmentBasis basis,
    List<AnnualRuleResult> results) {

  public AnnualRuleEvaluation {
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(stage, "stage");
    Objects.requireNonNull(basis, "basis");
    results = results == null ? List.of() : List.copyOf(results);
  }

}
