package com.bazi.app.report.relationship;

import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.ReportContent;
import com.bazi.app.report.ReportHorizon;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RelationshipNarrativePlan(
    String relationshipStatus,
    String relationshipStatusLabel,
    int horizonYears,
    String thesis,
    String summary,
    List<DimensionSummary> dimensions,
    String primaryDimensionCode,
    String secondaryDimensionCode,
    boolean focusTied,
    RiskSummary mainRisk,
    List<YearNarrative> years,
    List<String> evidenceKeys,
    @JsonInclude(JsonInclude.Include.NON_NULL) NarrativeTimeline timeline) implements ReportContent {

  public RelationshipNarrativePlan(
      String relationshipStatus,
      String relationshipStatusLabel,
      int horizonYears,
      String thesis,
      String summary,
      List<DimensionSummary> dimensions,
      String primaryDimensionCode,
      String secondaryDimensionCode,
      boolean focusTied,
      RiskSummary mainRisk,
      List<YearNarrative> years,
      List<String> evidenceKeys) {
    this(
        relationshipStatus,
        relationshipStatusLabel,
        horizonYears,
        thesis,
        summary,
        dimensions,
        primaryDimensionCode,
        secondaryDimensionCode,
        focusTied,
        mainRisk,
        years,
        evidenceKeys,
        null);
  }

  public RelationshipNarrativePlan {
    RelationshipStatus status = RelationshipStatus.fromCode(relationshipStatus);
    if (!status.label().equals(relationshipStatusLabel)) {
      throw new IllegalArgumentException("relationship status label does not match status");
    }
    if (horizonYears < ReportHorizon.MIN_YEARS || horizonYears > ReportHorizon.MAX_YEARS) {
      throw new IllegalArgumentException("relationship narrative must contain two through five years");
    }
    thesis = requireText(thesis, "thesis");
    summary = requireText(summary, "summary");
    primaryDimensionCode = requireText(primaryDimensionCode, "primaryDimensionCode");
    secondaryDimensionCode = requireText(secondaryDimensionCode, "secondaryDimensionCode");
    if (primaryDimensionCode.equals(secondaryDimensionCode)) {
      throw new IllegalArgumentException("relationship narrative focus dimensions must be distinct");
    }
    dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
    years = years == null ? List.of() : List.copyOf(years);
    evidenceKeys = copyKeys(evidenceKeys, true);
    if (dimensions.size() != RelationshipDimension.values().length) {
      throw new IllegalArgumentException("relationship narrative requires five dimensions");
    }
    Set<String> dimensionCodes = new HashSet<>();
    dimensions.forEach(item -> dimensionCodes.add(item.code()));
    if (dimensionCodes.size() != RelationshipDimension.values().length) {
      throw new IllegalArgumentException("relationship narrative dimension codes must be unique");
    }
    if (years.size() != horizonYears) {
      throw new IllegalArgumentException("relationship narrative year count does not match horizon");
    }
    for (int index = 1; index < years.size(); index++) {
      if (years.get(index).year() != years.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("relationship narrative years must be consecutive");
      }
    }
  }

  public YearNarrative firstYear() {
    return years.get(0);
  }

  public record DimensionSummary(
      String code,
      String label,
      String status,
      String tone,
      String judgment,
      List<String> supportingEvidenceKeys,
      List<String> limitingEvidenceKeys) {

    public DimensionSummary {
      code = requireText(code, "dimension code");
      label = requireText(label, "dimension label");
      status = requireText(status, "dimension status");
      tone = requireText(tone, "dimension tone");
      judgment = requireText(judgment, "dimension judgment");
      supportingEvidenceKeys = copyKeys(supportingEvidenceKeys, false);
      limitingEvidenceKeys = copyKeys(limitingEvidenceKeys, false);
    }
  }

  public record RiskSummary(
      String dimensionCode,
      String label,
      String judgment,
      List<String> evidenceKeys) {

    public RiskSummary {
      dimensionCode = requireText(dimensionCode, "risk dimension code");
      label = requireText(label, "risk label");
      judgment = requireText(judgment, "risk judgment");
      evidenceKeys = copyKeys(evidenceKeys, true);
    }
  }

  public record YearNarrative(
      int year,
      String ganZhi,
      String focus,
      String judgment,
      String mainLimit,
      List<String> realitySignals,
      List<String> actions,
      String transition,
      String primaryDimensionCode,
      String secondaryDimensionCode,
      String riskDimensionCode,
      List<String> evidenceKeys) {

    public YearNarrative {
      ganZhi = requireText(ganZhi, "year GanZhi");
      focus = requireText(focus, "year focus");
      judgment = requireText(judgment, "year judgment");
      transition = requireText(transition, "year transition");
      primaryDimensionCode = requireText(primaryDimensionCode, "year primary dimension");
      secondaryDimensionCode = requireText(secondaryDimensionCode, "year secondary dimension");
      if (primaryDimensionCode.equals(secondaryDimensionCode)) {
        throw new IllegalArgumentException("year focus dimensions must be distinct");
      }
      if ((mainLimit == null) != (riskDimensionCode == null)) {
        throw new IllegalArgumentException("year limit and risk dimension must appear together");
      }
      if (mainLimit != null) mainLimit = requireText(mainLimit, "year main limit");
      realitySignals = copyLines(realitySignals, "reality signals");
      actions = copyLines(actions, "actions");
      if (realitySignals.size() != 2 || actions.size() != 2) {
        throw new IllegalArgumentException("relationship year requires two signals and two actions");
      }
      if (actions.stream().distinct().count() != actions.size()) {
        throw new IllegalArgumentException("relationship year actions must be distinct");
      }
      evidenceKeys = copyKeys(evidenceKeys, true);
    }
  }

  private static List<String> copyLines(List<String> values, String field) {
    values = values == null ? List.of() : List.copyOf(values);
    values.forEach(value -> requireText(value, field));
    return values;
  }

  private static List<String> copyKeys(List<String> values, boolean required) {
    values = values == null ? List.of() : List.copyOf(values);
    if (required && values.isEmpty()) {
      throw new IllegalArgumentException("relationship narrative requires evidence keys");
    }
    if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException("relationship narrative evidence keys must not be blank");
    }
    return values;
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    if (value.contains("${") || value.contains("{{") || value.matches(".*\\{[a-z_]+}.*")) {
      throw new IllegalArgumentException(field + " contains an unresolved template token");
    }
    return value;
  }
}
