package com.bazi.app.report.relationship;

import com.bazi.app.report.ReportHorizon;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RelationshipPeriodEvaluation(
    List<Year> years,
    RelationshipFocus focus,
    RelationshipRisk mainRisk,
    List<Transition> transitions) {

  public RelationshipPeriodEvaluation {
    years = years == null ? List.of() : List.copyOf(years);
    transitions = transitions == null ? List.of() : List.copyOf(transitions);
    Objects.requireNonNull(focus, "focus");
    if (years.size() < ReportHorizon.MIN_YEARS || years.size() > ReportHorizon.MAX_YEARS) {
      throw new IllegalArgumentException("relationship period must contain two through five years");
    }
    for (int index = 1; index < years.size(); index++) {
      if (years.get(index).year() != years.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("relationship period years must be consecutive");
      }
    }
    if (transitions.size() != years.size() - 1) {
      throw new IllegalArgumentException("relationship period requires every adjacent transition");
    }
    for (int index = 0; index < transitions.size(); index++) {
      Transition transition = transitions.get(index);
      if (transition.fromYear() != years.get(index).year()
          || transition.toYear() != years.get(index + 1).year()) {
        throw new IllegalArgumentException("relationship transition does not match adjacent years");
      }
    }
  }

  public record Year(
      RelationshipYearEvaluation evaluation,
      RelationshipFocus focus,
      RelationshipRisk mainRisk) {

    public Year {
      Objects.requireNonNull(evaluation, "evaluation");
      Objects.requireNonNull(focus, "focus");
    }

    public int year() {
      return evaluation.year();
    }

    public String ganZhi() {
      return evaluation.ganZhi();
    }

    public Map<RelationshipDimension, RelationshipDimensionEvaluation> dimensions() {
      return evaluation.dimensions();
    }
  }

  public record Transition(
      int fromYear,
      int toYear,
      TransitionDirection direction,
      int fromClimateWeight,
      int toClimateWeight) {

    public Transition {
      Objects.requireNonNull(direction, "direction");
      if (toYear != fromYear + 1) {
        throw new IllegalArgumentException("relationship transition years must be adjacent");
      }
    }
  }

  public enum TransitionDirection {
    EASING,
    STEADY,
    TIGHTENING
  }
}
