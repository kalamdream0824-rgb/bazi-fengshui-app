package com.bazi.app.report.wealth;

import java.util.List;

public record WealthPeriodEvaluation(List<WealthYearEvaluation> years) {

  public WealthPeriodEvaluation {
    years = years == null ? List.of() : List.copyOf(years);
    if (years.isEmpty()) throw new IllegalArgumentException("wealth period requires at least one year");
    for (int index = 1; index < years.size(); index++) {
      if (years.get(index).year() != years.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("wealth evaluation years must be consecutive");
      }
    }
  }
}
