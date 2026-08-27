package com.bazi.app.report.relationship;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class RelationshipNarrativePlanner {

  private final RelationshipAnnualNarrator annualNarrator = new RelationshipAnnualNarrator();

  public RelationshipNarrativePlan plan(
      RelationshipPeriodEvaluation period,
      RelationshipStatus relationshipStatus) {
    Objects.requireNonNull(period, "period");
    Objects.requireNonNull(relationshipStatus, "relationshipStatus");
    String prefix = relationshipStatus.code();

    List<RelationshipNarrativePlan.DimensionSummary> dimensions = new ArrayList<>();
    for (RelationshipDimension dimension : RelationshipDimension.values()) {
      DimensionTotals totals = totals(period, dimension);
      dimensions.add(new RelationshipNarrativePlan.DimensionSummary(
          dimension.code(),
          dimension.label(),
          dimensionStatus(period, dimension),
          RelationshipDimensionEvaluation.toneFor(
              totals.supportWeight(), totals.limitationWeight())
              .name().toLowerCase(Locale.ROOT),
          RelationshipPlainCopy.get(prefix + ".dimension." + dimension.code()),
          totals.supportingEvidenceKeys(),
          totals.limitingEvidenceKeys()));
    }

    RelationshipNarrativePlan.RiskSummary mainRisk = riskSummary(
        prefix, period.mainRisk());
    List<RelationshipNarrativePlan.YearNarrative> years = new ArrayList<>();
    for (int index = 0; index < period.years().size(); index++) {
      years.add(year(prefix, period, index));
    }

    Map<String, String> summaryVariables = Map.of(
        "primary", period.focus().primaryDimension().label(),
        "secondary", period.focus().secondaryDimension().label(),
        "risk", period.mainRisk() == null ? "" : period.mainRisk().dimension().label());
    String summaryKey = prefix + ".summary."
        + (period.mainRisk() == null ? "without_risk" : "with_risk");

    return new RelationshipNarrativePlan(
        relationshipStatus.code(),
        relationshipStatus.label(),
        period.years().size(),
        RelationshipPlainCopy.format(prefix + ".thesis", Map.of(
            "horizon", List.of("二", "三", "四", "五").get(period.years().size() - 2) + "年")),
        RelationshipPlainCopy.format(summaryKey, summaryVariables),
        dimensions,
        period.focus().primaryDimension().code(),
        period.focus().secondaryDimension().code(),
        period.focus().tied(),
        mainRisk,
        years,
        allEvidenceKeys(period));
  }

  private RelationshipNarrativePlan.YearNarrative year(
      String prefix,
      RelationshipPeriodEvaluation period,
      int index) {
    RelationshipPeriodEvaluation.Year year = period.years().get(index);
    RelationshipDimension primary = year.focus().primaryDimension();
    RelationshipDimension secondary = year.focus().secondaryDimension();
    RelationshipRisk risk = year.mainRisk();
    LinkedHashSet<String> evidenceKeys = new LinkedHashSet<>();
    evidenceKeys.addAll(year.focus().primaryEvidenceKeys());
    evidenceKeys.addAll(year.focus().secondaryEvidenceKeys());
    if (risk != null) evidenceKeys.addAll(risk.evidenceKeys());

    RelationshipPeriodEvaluation.Year previous = index > 0 ? period.years().get(index - 1) : null;
    RelationshipPeriodEvaluation.Year next = index + 1 < period.years().size()
        ? period.years().get(index + 1) : null;

    return new RelationshipNarrativePlan.YearNarrative(
        year.year(),
        year.ganZhi(),
        annualNarrator.focus(prefix, year),
        annualNarrator.judgment(prefix, previous, year),
        risk == null ? null : RelationshipPlainCopy.get(
            prefix + ".risk." + risk.dimension().code()),
        annualNarrator.signals(prefix, year),
        annualNarrator.actions(prefix, year),
        annualNarrator.transition(prefix, year, next),
        primary.code(),
        secondary.code(),
        risk == null ? null : risk.dimension().code(),
        List.copyOf(evidenceKeys));
  }

  private RelationshipNarrativePlan.RiskSummary riskSummary(
      String prefix,
      RelationshipRisk risk) {
    if (risk == null) return null;
    return new RelationshipNarrativePlan.RiskSummary(
        risk.dimension().code(),
        risk.dimension().label(),
        RelationshipPlainCopy.get(prefix + ".risk." + risk.dimension().code()),
        risk.evidenceKeys());
  }

  private String dimensionStatus(
      RelationshipPeriodEvaluation period,
      RelationshipDimension dimension) {
    boolean primary = period.focus().primaryDimension() == dimension;
    boolean secondary = period.focus().secondaryDimension() == dimension;
    boolean risk = period.mainRisk() != null && period.mainRisk().dimension() == dimension;
    if (primary && risk) return "主要内容，也有压力";
    if (secondary && risk) return "还要看看，也有压力";
    if (primary) return "主要内容";
    if (secondary) return "还要看看";
    if (risk) return "需要留意";
    return "不是主要内容";
  }

  private DimensionTotals totals(
      RelationshipPeriodEvaluation period,
      RelationshipDimension dimension) {
    int support = 0;
    int limitation = 0;
    LinkedHashSet<String> supportKeys = new LinkedHashSet<>();
    LinkedHashSet<String> limitationKeys = new LinkedHashSet<>();
    for (RelationshipPeriodEvaluation.Year year : period.years()) {
      RelationshipDimensionEvaluation evaluation = year.dimensions().get(dimension);
      support += evaluation.supportWeight();
      limitation += evaluation.limitationWeight();
      evaluation.support().stream().map(RelationshipEvidence::key).forEach(supportKeys::add);
      evaluation.limitations().stream().map(RelationshipEvidence::key)
          .forEach(limitationKeys::add);
    }
    return new DimensionTotals(
        support, limitation, List.copyOf(supportKeys), List.copyOf(limitationKeys));
  }

  private List<String> allEvidenceKeys(RelationshipPeriodEvaluation period) {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    for (RelationshipPeriodEvaluation.Year year : period.years()) {
      for (RelationshipDimensionEvaluation evaluation : year.dimensions().values()) {
        evaluation.evidence().stream().map(RelationshipEvidence::key).forEach(keys::add);
      }
    }
    return List.copyOf(keys);
  }

  private record DimensionTotals(
      int supportWeight,
      int limitationWeight,
      List<String> supportingEvidenceKeys,
      List<String> limitingEvidenceKeys) {}
}
