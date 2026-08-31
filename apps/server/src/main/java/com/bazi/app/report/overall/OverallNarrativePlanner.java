package com.bazi.app.report.overall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OverallNarrativePlanner {

  private final OverallPlainCopy copy = new OverallPlainCopy();
  private final OverallEvidenceAngleResolver angleResolver = new OverallEvidenceAngleResolver();

  public OverallNarrativePlan plan(OverallPeriodEvaluation period) {
    List<OverallNarrativePlan.YearNarrative> years = new ArrayList<>();
    for (int index = 0; index < period.years().size(); index++) {
      OverallYearEvaluation year = period.years().get(index);
      List<OverallNarrativePlan.DimensionReading> dimensions = year.dimensions().stream()
          .map(this::reading)
          .toList();
      years.add(new OverallNarrativePlan.YearNarrative(
          year.year(),
          year.ganZhi(),
          year.primaryDimension().code(),
          year.primaryDimension().label(),
          year.secondaryDimension().code(),
          year.secondaryDimension().label(),
          copy.headline(year.primaryDimension(), year.primary().stance(), index),
          verdict(year),
          copy.linkage(
              year.primaryDimension(), year.secondaryDimension(), year.primary().stance()),
          dimensions,
          copy.priorityIssue(
              year.primaryDimension(), year.primary().stance(), angleResolver.resolve(year.primary())),
          List.of(
              copy.action(year.primaryDimension(), year.primary().stance(), index),
              copy.action(year.secondaryDimension(), year.secondary().stance(), index)),
          copy.changeCondition(
              year.primaryDimension(), year.primary().stance(), angleResolver.resolve(year.primary())),
          transition(period, index),
          evidenceKeys(year)));
    }

    return new OverallNarrativePlan(
        period.horizonYears(),
        thesis(period),
        summary(period),
        years,
        route(period),
        "这份综合命书用于比较三年的生活重点和处理顺序。它不代替具体的工作、财务、关系或健康决定。",
        years.stream().flatMap(year -> year.evidenceKeys().stream()).distinct().toList());
  }

  private OverallNarrativePlan.DimensionReading reading(OverallDimensionEvaluation evaluation) {
    OverallEvidenceAngle angle = angleResolver.resolve(evaluation);
    return new OverallNarrativePlan.DimensionReading(
        evaluation.dimension().code(),
        evaluation.dimension().label(),
        evaluation.stance().label(),
        copy.judgment(evaluation.dimension(), evaluation.stance(), angle),
        evaluation.allEvidenceKeys());
  }

  private String thesis(OverallPeriodEvaluation period) {
    OverallDimension first = period.years().get(0).primaryDimension();
    OverallDimension last = period.years().get(period.years().size() - 1).primaryDimension();
    if (first == last) {
      return "这三年的主要事情都围绕" + first.label() + "展开，但每一年的轻重和处理方式不同。";
    }
    return "这三年先处理" + first.label() + "，随后重点会转到" + last.label()
        + "；其他方面也要同时照看，但不必平均用力。";
  }

  private String summary(OverallPeriodEvaluation period) {
    OverallYearEvaluation supportYear = period.years().stream()
        .max(Comparator.comparingInt(this::totalSupport).thenComparingInt(OverallYearEvaluation::year))
        .orElseThrow();
    OverallYearEvaluation pressureYear = period.years().stream()
        .max(Comparator.comparingInt(this::totalPressure).thenComparingInt(OverallYearEvaluation::year))
        .orElseThrow();
    if (supportYear.year() == pressureYear.year()) {
      return supportYear.year() + "年既有推进条件，也有较明显的牵制，适合边做边确认承受范围。";
    }
    return supportYear.year() + "年相对更适合推进已有计划；" + pressureYear.year()
        + "年更需要控制负担，先处理最影响日常的一件事。";
  }

  private String verdict(OverallYearEvaluation year) {
    return copy.judgment(
        year.primaryDimension(), year.primary().stance(), angleResolver.resolve(year.primary()))
        + "同时，" + copy.judgment(
            year.secondaryDimension(), year.secondary().stance(), angleResolver.resolve(year.secondary()));
  }

  private String transition(OverallPeriodEvaluation period, int index) {
    OverallYearEvaluation year = period.years().get(index);
    if (index >= period.transitions().size()) {
      return "这是本次三年判断的最后一年，后续需要结合实际变化重新计算。";
    }
    OverallTransition transition = period.transitions().get(index);
    OverallYearEvaluation next = period.years().get(index + 1);
    if ("转向".equals(transition.relation())) {
      return "下一年重点会从" + year.primaryDimension().label() + "转到"
          + next.primaryDimension().label() + "。";
    }
    String change = copy.evidenceChange(angleResolver.resolve(next.primary()).primaryKey());
    return switch (transition.relation()) {
      case "加强" -> "下一年仍要关注" + year.primaryDimension().label() + "；" + change
          + "，同一问题会比今年更需要主动处理，整体表现为加强。";
      case "缓和" -> "下一年仍要关注" + year.primaryDimension().label() + "；" + change
          + "，前一年的紧张感会有所缓和，但仍要看实际行动。";
      default -> "下一年仍要关注" + year.primaryDimension().label() + "；" + change
          + "，处理顺序暂时不变，整体表现为延续。";
    };
  }

  private List<String> route(OverallPeriodEvaluation period) {
    Set<OverallDimension> ordered = new LinkedHashSet<>();
    period.years().stream().map(OverallYearEvaluation::primaryDimension).forEach(ordered::add);
    period.years().stream().map(OverallYearEvaluation::secondaryDimension).forEach(ordered::add);
    List<OverallDimension> dimensions = ordered.stream().limit(3).toList();
    List<String> route = new ArrayList<>();
    for (int index = 0; index < dimensions.size(); index++) {
      OverallDimension dimension = dimensions.get(index);
          OverallDimensionEvaluation strongest = period.years().stream()
              .map(year -> year.dimension(dimension))
              .max(Comparator.comparingInt(OverallDimensionEvaluation::salience))
              .orElseThrow();
      route.add(copy.action(dimension, strongest.stance(), index));
    }
    return List.copyOf(route);
  }

  private int totalSupport(OverallYearEvaluation year) {
    return year.dimensions().stream().mapToInt(OverallDimensionEvaluation::supportWeight).sum();
  }

  private int totalPressure(OverallYearEvaluation year) {
    return year.dimensions().stream().mapToInt(OverallDimensionEvaluation::limitationWeight).sum();
  }

  private List<String> evidenceKeys(OverallYearEvaluation year) {
    return year.dimensions().stream()
        .flatMap(dimension -> dimension.allEvidenceKeys().stream())
        .distinct()
        .toList();
  }
}
