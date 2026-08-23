package com.bazi.app.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record AnnualPeriodAssessment(
    ReportTopic topic,
    LocalDate generatedOn,
    ReportHorizon horizon,
    List<YearAssessment> years,
    List<AnnualTransition> transitions,
    List<String> priorities) {

  public AnnualPeriodAssessment {
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(generatedOn, "generatedOn");
    Objects.requireNonNull(horizon, "horizon");
    years = years == null ? List.of() : List.copyOf(years);
    transitions = transitions == null ? List.of() : List.copyOf(transitions);
    priorities = priorities == null ? List.of() : List.copyOf(priorities);
    if (years.size() != horizon.years() || years.get(0).year() != generatedOn.getYear()) {
      throw new IllegalArgumentException("the configured consecutive report years are required");
    }
    for (int index = 1; index < years.size(); index++) {
      if (years.get(index).year() != years.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("the configured consecutive report years are required");
      }
    }
    if (transitions.size() != years.size() - 1) {
      throw new IllegalArgumentException("every adjacent year requires one transition");
    }
    for (int index = 0; index < transitions.size(); index++) {
      AnnualTransition transition = transitions.get(index);
      if (transition.fromYear() != years.get(index).year()
          || transition.toYear() != years.get(index + 1).year()) {
        throw new IllegalArgumentException("annual transitions must match the assessed years");
      }
    }
  }

  public String periodLabel() {
    return switch (horizon.years()) {
      case 2 -> "两年";
      case 3 -> "三年";
      case 4 -> "四年";
      case 5 -> "五年";
      default -> throw new IllegalStateException("unsupported report horizon");
    };
  }

  public String trajectory() {
    String annualPath = years.stream()
        .map(year -> year.year() + year.stage().label())
        .collect(Collectors.joining(" → "));
    String relations = transitions.stream()
        .map(AnnualTransition::relation)
        .collect(Collectors.joining(" → "));
    return relations.isBlank() ? annualPath : annualPath + "｜走势" + relations;
  }
}
