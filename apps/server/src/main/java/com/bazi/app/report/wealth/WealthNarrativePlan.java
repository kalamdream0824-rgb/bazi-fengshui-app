package com.bazi.app.report.wealth;

import com.bazi.app.report.ReportContent;
import java.util.List;
import java.util.Objects;

public record WealthNarrativePlan(
    int horizonYears,
    String thesis,
    String summary,
    List<PathSummary> paths,
    String primaryPathCode,
    String secondaryPathCode,
    RiskSummary mainRisk,
    List<YearNarrative> years,
    List<String> route) implements ReportContent {

  public WealthNarrativePlan {
    if (horizonYears < 1) throw new IllegalArgumentException("horizonYears must be positive");
    Objects.requireNonNull(thesis, "thesis");
    Objects.requireNonNull(summary, "summary");
    Objects.requireNonNull(primaryPathCode, "primaryPathCode");
    Objects.requireNonNull(secondaryPathCode, "secondaryPathCode");
    Objects.requireNonNull(mainRisk, "mainRisk");
    paths = paths == null ? List.of() : List.copyOf(paths);
    years = years == null ? List.of() : List.copyOf(years);
    route = route == null ? List.of() : List.copyOf(route);
  }

  public record PathSummary(
      String code,
      String label,
      String status,
      String judgment,
      List<String> supportingEvidenceKeys,
      List<String> limitingEvidenceKeys) {

    public PathSummary {
      Objects.requireNonNull(code, "code");
      Objects.requireNonNull(label, "label");
      Objects.requireNonNull(status, "status");
      Objects.requireNonNull(judgment, "judgment");
      supportingEvidenceKeys = supportingEvidenceKeys == null
          ? List.of() : List.copyOf(supportingEvidenceKeys);
      limitingEvidenceKeys = limitingEvidenceKeys == null
          ? List.of() : List.copyOf(limitingEvidenceKeys);
    }
  }

  public record RiskSummary(
      String pathCode,
      String label,
      String judgment,
      List<String> evidenceKeys) {

    public RiskSummary {
      Objects.requireNonNull(pathCode, "pathCode");
      Objects.requireNonNull(label, "label");
      Objects.requireNonNull(judgment, "judgment");
      evidenceKeys = evidenceKeys == null ? List.of() : List.copyOf(evidenceKeys);
    }
  }

  public record YearNarrative(
      int year,
      String ganZhi,
      String focus,
      String incomeSource,
      String retention,
      String mainLimit,
      List<String> realitySignals,
      List<String> actions,
      String transition,
      List<String> evidenceKeys) {

    public YearNarrative {
      Objects.requireNonNull(ganZhi, "ganZhi");
      Objects.requireNonNull(focus, "focus");
      Objects.requireNonNull(incomeSource, "incomeSource");
      Objects.requireNonNull(retention, "retention");
      Objects.requireNonNull(mainLimit, "mainLimit");
      Objects.requireNonNull(transition, "transition");
      realitySignals = realitySignals == null ? List.of() : List.copyOf(realitySignals);
      actions = actions == null ? List.of() : List.copyOf(actions);
      evidenceKeys = evidenceKeys == null ? List.of() : List.copyOf(evidenceKeys);
    }
  }
}
