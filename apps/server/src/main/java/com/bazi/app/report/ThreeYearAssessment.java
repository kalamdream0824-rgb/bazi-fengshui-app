package com.bazi.app.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ThreeYearAssessment(
    ReportTopic topic,
    LocalDate generatedOn,
    List<YearAssessment> years,
    String trajectory,
    List<String> priorities) {

  public ThreeYearAssessment {
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(generatedOn, "generatedOn");
    Objects.requireNonNull(trajectory, "trajectory");
    years = years == null ? List.of() : List.copyOf(years);
    priorities = priorities == null ? List.of() : List.copyOf(priorities);
    int expectedYears = topic == ReportTopic.CAREER || topic == ReportTopic.WEALTH ? 2 : 3;
    if (years.size() != expectedYears || years.get(0).year() != generatedOn.getYear()) {
      throw new IllegalArgumentException("the configured consecutive report years are required");
    }
    for (int index = 1; index < years.size(); index++) {
      if (years.get(index).year() != years.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("the configured consecutive report years are required");
      }
    }
  }

  public String periodLabel() {
    return years.size() == 2 ? "两年" : "三年";
  }

  static ThreeYearAssessment from(AnnualPeriodAssessment assessment) {
    return new ThreeYearAssessment(
        assessment.topic(),
        assessment.generatedOn(),
        assessment.years(),
        assessment.trajectory(),
        assessment.priorities());
  }
}
