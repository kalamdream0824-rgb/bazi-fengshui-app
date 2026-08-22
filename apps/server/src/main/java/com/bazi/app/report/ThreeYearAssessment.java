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
    if (years.size() != 3
        || years.get(0).year() != generatedOn.getYear()
        || years.get(1).year() != years.get(0).year() + 1
        || years.get(2).year() != years.get(1).year() + 1) {
      throw new IllegalArgumentException("current and next two consecutive years are required");
    }
  }
}
