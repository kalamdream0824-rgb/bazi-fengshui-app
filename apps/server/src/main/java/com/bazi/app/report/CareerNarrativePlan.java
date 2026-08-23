package com.bazi.app.report;

import java.util.List;
import java.util.Objects;

public record CareerNarrativePlan(
    String thesis,
    String contextSummary,
    List<YearNarrative> years,
    List<String> route) {

  public CareerNarrativePlan {
    Objects.requireNonNull(thesis, "thesis");
    Objects.requireNonNull(contextSummary, "contextSummary");
    years = years == null ? List.of() : List.copyOf(years);
    route = route == null ? List.of() : List.copyOf(route);
  }

  public record YearNarrative(
      int year,
      String ganZhi,
      String stage,
      String headline,
      String verdict,
      List<String> reasons,
      String obstacle,
      List<String> actions,
      String changeCondition,
      List<String> evidenceKeys,
      List<ReportEvidence> evidence,
      List<ReportEvidence> counterEvidence,
      String confidence) {

    public YearNarrative {
      Objects.requireNonNull(ganZhi, "ganZhi");
      Objects.requireNonNull(stage, "stage");
      Objects.requireNonNull(headline, "headline");
      Objects.requireNonNull(verdict, "verdict");
      Objects.requireNonNull(obstacle, "obstacle");
      Objects.requireNonNull(changeCondition, "changeCondition");
      Objects.requireNonNull(confidence, "confidence");
      reasons = reasons == null ? List.of() : List.copyOf(reasons);
      actions = actions == null ? List.of() : List.copyOf(actions);
      evidenceKeys = evidenceKeys == null ? List.of() : List.copyOf(evidenceKeys);
      evidence = evidence == null ? List.of() : List.copyOf(evidence);
      counterEvidence = counterEvidence == null ? List.of() : List.copyOf(counterEvidence);
    }
  }
}
