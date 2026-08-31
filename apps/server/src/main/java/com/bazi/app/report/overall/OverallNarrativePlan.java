package com.bazi.app.report.overall;

import com.bazi.app.report.ReportContent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record OverallNarrativePlan(
    int horizonYears,
    String thesis,
    String summary,
    List<YearNarrative> years,
    List<String> route,
    String readingNote,
    List<String> evidenceKeys) implements ReportContent {

  public OverallNarrativePlan {
    if (horizonYears < 2 || horizonYears > 5) {
      throw new IllegalArgumentException("overall narrative horizon must be between two and five years");
    }
    Objects.requireNonNull(thesis, "thesis");
    Objects.requireNonNull(summary, "summary");
    Objects.requireNonNull(readingNote, "readingNote");
    years = copy(years);
    route = copy(route);
    evidenceKeys = distinct(evidenceKeys);
    if (years.size() != horizonYears) {
      throw new IllegalArgumentException("overall narrative requires every configured year");
    }
  }

  public record YearNarrative(
      int year,
      String ganZhi,
      String primaryCode,
      String primaryLabel,
      String secondaryCode,
      String secondaryLabel,
      String headline,
      String verdict,
      String linkage,
      List<DimensionReading> dimensions,
      String priorityIssue,
      List<String> actions,
      String changeCondition,
      String transition,
      List<String> evidenceKeys) {

    public YearNarrative {
      Objects.requireNonNull(ganZhi, "ganZhi");
      Objects.requireNonNull(primaryCode, "primaryCode");
      Objects.requireNonNull(primaryLabel, "primaryLabel");
      Objects.requireNonNull(secondaryCode, "secondaryCode");
      Objects.requireNonNull(secondaryLabel, "secondaryLabel");
      Objects.requireNonNull(headline, "headline");
      Objects.requireNonNull(verdict, "verdict");
      linkage = linkage == null ? "" : linkage;
      Objects.requireNonNull(priorityIssue, "priorityIssue");
      Objects.requireNonNull(changeCondition, "changeCondition");
      Objects.requireNonNull(transition, "transition");
      dimensions = copy(dimensions);
      actions = copy(actions);
      evidenceKeys = distinct(evidenceKeys);
      if (dimensions.size() != OverallDimension.values().length) {
        throw new IllegalArgumentException("overall narrative year requires all dimensions");
      }
    }
  }

  public record DimensionReading(
      String code,
      String label,
      String stance,
      String judgment,
      List<String> evidenceKeys) {

    public DimensionReading {
      Objects.requireNonNull(code, "code");
      Objects.requireNonNull(label, "label");
      Objects.requireNonNull(stance, "stance");
      Objects.requireNonNull(judgment, "judgment");
      evidenceKeys = distinct(evidenceKeys);
      if (evidenceKeys.isEmpty()) {
        throw new IllegalArgumentException("dimension reading requires evidence keys");
      }
    }
  }

  private static <T> List<T> copy(List<T> values) {
    return values == null ? List.of() : List.copyOf(values);
  }

  private static List<String> distinct(List<String> values) {
    return values == null
        ? List.of()
        : List.copyOf(new ArrayList<>(new LinkedHashSet<>(values)));
  }
}
