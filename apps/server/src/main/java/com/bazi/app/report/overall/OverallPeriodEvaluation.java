package com.bazi.app.report.overall;

import java.util.List;

public record OverallPeriodEvaluation(
    int horizonYears,
    List<OverallYearEvaluation> years,
    List<OverallTransition> transitions) {

  public OverallPeriodEvaluation {
    years = years == null ? List.of() : List.copyOf(years);
    transitions = transitions == null ? List.of() : List.copyOf(transitions);
    if (horizonYears < 2 || horizonYears > 5 || years.size() != horizonYears) {
      throw new IllegalArgumentException("overall period requires the configured two-to-five years");
    }
    if (transitions.size() != years.size() - 1) {
      throw new IllegalArgumentException("every adjacent overall year requires a transition");
    }
    for (int index = 1; index < years.size(); index++) {
      if (years.get(index).year() != years.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("overall years must be consecutive");
      }
    }
  }
}
