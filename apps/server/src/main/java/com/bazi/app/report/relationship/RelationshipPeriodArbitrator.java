package com.bazi.app.report.relationship;

import com.bazi.app.report.ReportHorizon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class RelationshipPeriodArbitrator {

  public RelationshipPeriodEvaluation arbitrate(List<RelationshipYearEvaluation> evaluations) {
    validatePeriod(evaluations);

    List<RelationshipPeriodEvaluation.Year> years = evaluations.stream()
        .map(year -> new RelationshipPeriodEvaluation.Year(
            year,
            focus(year.dimensions()),
            risk(year.dimensions())))
        .toList();
    Map<RelationshipDimension, DimensionScore> periodScores = aggregate(evaluations);
    List<RelationshipPeriodEvaluation.Transition> transitions = new ArrayList<>();
    for (int index = 0; index + 1 < evaluations.size(); index++) {
      RelationshipYearEvaluation from = evaluations.get(index);
      RelationshipYearEvaluation to = evaluations.get(index + 1);
      transitions.add(new RelationshipPeriodEvaluation.Transition(
          from.year(),
          to.year(),
          transition(from.climateWeight(), to.climateWeight()),
          from.climateWeight(),
          to.climateWeight()));
    }

    return new RelationshipPeriodEvaluation(
        years,
        focusScores(periodScores),
        riskScores(periodScores),
        transitions);
  }

  private RelationshipFocus focus(
      Map<RelationshipDimension, RelationshipDimensionEvaluation> dimensions) {
    return focusScores(scores(dimensions));
  }

  private RelationshipRisk risk(
      Map<RelationshipDimension, RelationshipDimensionEvaluation> dimensions) {
    return riskScores(scores(dimensions));
  }

  private Map<RelationshipDimension, DimensionScore> scores(
      Map<RelationshipDimension, RelationshipDimensionEvaluation> dimensions) {
    Map<RelationshipDimension, DimensionScore> scores = new EnumMap<>(RelationshipDimension.class);
    dimensions.forEach((dimension, evaluation) -> scores.put(
        dimension,
        new DimensionScore(
            dimension,
            evaluation.prominenceWeight(),
            evaluation.limitationWeight(),
            evaluation.evidence().stream().map(RelationshipEvidence::key).sorted().toList(),
            evaluation.limitations().stream().map(RelationshipEvidence::key).sorted().toList())));
    return Map.copyOf(scores);
  }

  private RelationshipFocus focusScores(Map<RelationshipDimension, DimensionScore> scores) {
    List<DimensionScore> ranked = scores.values().stream()
        .filter(score -> score.prominenceWeight() > 0)
        .sorted(Comparator.comparingInt(DimensionScore::prominenceWeight).reversed()
            .thenComparingInt(score -> score.dimension().ordinal()))
        .toList();
    if (ranked.size() < 2) {
      throw new IllegalArgumentException(
          "relationship arbitration requires two evidence-backed dimensions");
    }
    DimensionScore primary = ranked.get(0);
    DimensionScore secondary = ranked.get(1);
    return new RelationshipFocus(
        primary.dimension(),
        secondary.dimension(),
        primary.prominenceWeight(),
        secondary.prominenceWeight(),
        primary.prominenceWeight() == secondary.prominenceWeight(),
        primary.evidenceKeys(),
        secondary.evidenceKeys());
  }

  private RelationshipRisk riskScores(Map<RelationshipDimension, DimensionScore> scores) {
    return scores.values().stream()
        .filter(score -> score.limitationWeight() > 0)
        .sorted(Comparator.comparingInt(DimensionScore::limitationWeight).reversed()
            .thenComparingInt(score -> score.dimension().ordinal()))
        .findFirst()
        .map(score -> new RelationshipRisk(
            score.dimension(), score.limitationWeight(), score.limitationEvidenceKeys()))
        .orElse(null);
  }

  private Map<RelationshipDimension, DimensionScore> aggregate(
      List<RelationshipYearEvaluation> years) {
    Map<RelationshipDimension, MutableScore> mutable =
        new EnumMap<>(RelationshipDimension.class);
    for (RelationshipDimension dimension : RelationshipDimension.values()) {
      mutable.put(dimension, new MutableScore());
    }
    for (RelationshipYearEvaluation year : years) {
      year.dimensions().forEach((dimension, evaluation) -> {
        MutableScore score = mutable.get(dimension);
        score.prominenceWeight += evaluation.prominenceWeight();
        score.limitationWeight += evaluation.limitationWeight();
        evaluation.evidence().stream().map(RelationshipEvidence::key)
            .forEach(score.evidenceKeys::add);
        evaluation.limitations().stream().map(RelationshipEvidence::key)
            .forEach(score.limitationEvidenceKeys::add);
      });
    }
    Map<RelationshipDimension, DimensionScore> result =
        new EnumMap<>(RelationshipDimension.class);
    mutable.forEach((dimension, score) -> result.put(
        dimension,
        new DimensionScore(
            dimension,
            score.prominenceWeight,
            score.limitationWeight,
            List.copyOf(score.evidenceKeys),
            List.copyOf(score.limitationEvidenceKeys))));
    return Map.copyOf(result);
  }

  private RelationshipPeriodEvaluation.TransitionDirection transition(int from, int to) {
    if (to > from) return RelationshipPeriodEvaluation.TransitionDirection.EASING;
    if (to < from) return RelationshipPeriodEvaluation.TransitionDirection.TIGHTENING;
    return RelationshipPeriodEvaluation.TransitionDirection.STEADY;
  }

  private void validatePeriod(List<RelationshipYearEvaluation> evaluations) {
    if (evaluations == null
        || evaluations.size() < ReportHorizon.MIN_YEARS
        || evaluations.size() > ReportHorizon.MAX_YEARS) {
      throw new IllegalArgumentException(
          "relationship arbitration requires two through five years");
    }
    for (int index = 1; index < evaluations.size(); index++) {
      if (evaluations.get(index).year() != evaluations.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("relationship arbitration years must be consecutive");
      }
    }
  }

  private record DimensionScore(
      RelationshipDimension dimension,
      int prominenceWeight,
      int limitationWeight,
      List<String> evidenceKeys,
      List<String> limitationEvidenceKeys) {}

  private static final class MutableScore {
    private int prominenceWeight;
    private int limitationWeight;
    private final TreeSet<String> evidenceKeys = new TreeSet<>();
    private final TreeSet<String> limitationEvidenceKeys = new TreeSet<>();
  }
}
