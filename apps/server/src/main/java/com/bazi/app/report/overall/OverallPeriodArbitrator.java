package com.bazi.app.report.overall;

import com.bazi.app.report.AnnualContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class OverallPeriodArbitrator {

  private static final Comparator<OverallDimensionEvaluation> RANKING =
      Comparator.comparingInt(OverallDimensionEvaluation::salience)
          .reversed()
          .thenComparing(OverallDimensionEvaluation::limitationWeight, Comparator.reverseOrder())
          .thenComparing(item -> item.dimension().ordinal());

  private final OverallDimensionEvaluator evaluator;

  public OverallPeriodArbitrator() {
    this(new OverallDimensionEvaluator());
  }

  OverallPeriodArbitrator(OverallDimensionEvaluator evaluator) {
    this.evaluator = Objects.requireNonNull(evaluator);
  }

  public OverallPeriodEvaluation arbitrate(List<AnnualContext> contexts) {
    Objects.requireNonNull(contexts, "contexts");
    List<OverallYearEvaluation> years = new ArrayList<>();
    OverallDimension previous = null;
    for (AnnualContext context : contexts) {
      List<OverallDimensionEvaluation> dimensions = evaluator.evaluate(context);
      OverallDimension primary = selectPrimary(dimensions, previous);
      OverallDimension secondary = ranked(dimensions).stream()
          .map(OverallDimensionEvaluation::dimension)
          .filter(dimension -> dimension != primary)
          .findFirst()
          .orElseThrow();
      years.add(new OverallYearEvaluation(
          context.year(), context.ganZhi(), primary, secondary, dimensions));
      previous = primary;
    }

    List<OverallTransition> transitions = new ArrayList<>();
    for (int index = 1; index < years.size(); index++) {
      OverallYearEvaluation before = years.get(index - 1);
      OverallYearEvaluation after = years.get(index);
      transitions.add(new OverallTransition(
          before.year(), after.year(), relation(before, after)));
    }
    return new OverallPeriodEvaluation(years.size(), years, transitions);
  }

  static OverallDimension selectPrimary(
      List<OverallDimensionEvaluation> dimensions,
      OverallDimension previous) {
    List<OverallDimensionEvaluation> ranked = ranked(dimensions);
    return ranked.get(0).dimension();
  }

  private static List<OverallDimensionEvaluation> ranked(
      List<OverallDimensionEvaluation> dimensions) {
    if (dimensions == null || dimensions.size() < 2) {
      throw new IllegalArgumentException("overall arbitration requires at least two dimensions");
    }
    return dimensions.stream().sorted(RANKING).toList();
  }

  private String relation(OverallYearEvaluation before, OverallYearEvaluation after) {
    if (before.primaryDimension() != after.primaryDimension()) return "转向";
    int beforePressure = before.primary().limitationWeight() - before.primary().supportWeight();
    int afterPressure = after.primary().limitationWeight() - after.primary().supportWeight();
    if (afterPressure >= beforePressure + 2) return "加强";
    if (afterPressure <= beforePressure - 2) return "缓和";
    return "延续";
  }
}
