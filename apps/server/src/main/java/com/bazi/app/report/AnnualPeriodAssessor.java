package com.bazi.app.report;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AnnualPeriodAssessor {

  private final Clock clock;
  private final AnnualRuleCatalog catalog;
  private final AnnualRuleEngine engine = new AnnualRuleEngine();

  public AnnualPeriodAssessor(Clock clock, AnnualRuleCatalog catalog) {
    this.clock = Objects.requireNonNull(clock);
    this.catalog = Objects.requireNonNull(catalog);
  }

  public AnnualPeriodAssessment assess(
      PaipanRequest request,
      PaipanResultDto chart,
      ReportTopic topic,
      ReportHorizon horizon) {
    return assess(request, chart, topic, horizon, null);
  }

  public AnnualPeriodAssessment assess(
      PaipanRequest request,
      PaipanResultDto chart,
      ReportTopic topic,
      ReportHorizon horizon,
      CareerContext careerContext) {
    CareerContextualizer contextualizer = new CareerContextualizer();
    List<YearAssessment> years = new AnnualContextFactory(clock).create(request, chart, horizon).stream()
        .map(context -> assess(context, topic))
        .map(year -> topic == ReportTopic.CAREER && careerContext != null
            ? contextualizer.apply(year, careerContext)
            : year)
        .toList();
    List<AnnualTransition> transitions = new ArrayList<>();
    for (int index = 0; index < years.size() - 1; index++) {
      YearAssessment current = years.get(index);
      YearAssessment next = years.get(index + 1);
      transitions.add(new AnnualTransition(
          current.year(), next.year(), relation(current.stage(), next.stage())));
    }
    List<String> priorities = years.stream()
        .flatMap(year -> year.actions().stream())
        .distinct()
        .limit(3)
        .toList();
    return new AnnualPeriodAssessment(
        topic,
        LocalDate.now(clock),
        horizon,
        years,
        transitions,
        priorities);
  }

  private String relation(AnnualStage current, AnnualStage next) {
    if (current == next) return "延续";
    if (current == AnnualStage.TRANSITION || next == AnnualStage.TRANSITION) return "转折";
    int currentMomentum = momentum(current);
    int nextMomentum = momentum(next);
    if (nextMomentum > currentMomentum) return "增强";
    if (nextMomentum < currentMomentum) return "回落";
    return "转折";
  }

  private int momentum(AnnualStage stage) {
    return switch (stage) {
      case CAUTION -> 0;
      case PREPARE -> 1;
      case STABLE -> 2;
      case ADVANCE -> 3;
      case TRANSITION -> 2;
    };
  }

  private YearAssessment assess(AnnualContext context, ReportTopic topic) {
    AnnualRuleEvaluation evaluation = engine.evaluateYear(context, topic, catalog.rulesFor(topic));
    if (evaluation.results().isEmpty()) return insufficient(context, evaluation);

    AnnualRuleResult lead = evaluation.results().get(0);
    List<AnnualFact> evidence = distinctFacts(evaluation.results(), false);
    List<AnnualFact> counterEvidence = distinctFacts(evaluation.results(), true);
    return new YearAssessment(
        context.year(),
        context.ganZhi(),
        evaluation.stage(),
        lead.headline(),
        lead.conclusion(),
        findings(evaluation.results(), AnnualFindingType.OPPORTUNITY),
        findings(evaluation.results(), AnnualFindingType.PRESSURE),
        distinctStrings(evaluation.results().stream().flatMap(result -> result.actions().stream()).toList()),
        distinctStrings(evaluation.results().stream().flatMap(result -> result.realitySignals().stream()).toList()),
        evidence.stream().map(AnnualFact::toEvidence).toList(),
        counterEvidence.stream().map(AnnualFact::toEvidence).toList(),
        lead.confidence(),
        evaluation.results().stream().map(AnnualRuleResult::ruleKey).toList(),
        evaluation.basis());
  }

  private YearAssessment insufficient(AnnualContext context, AnnualRuleEvaluation evaluation) {
    return new YearAssessment(
        context.year(),
        context.ganZhi(),
        evaluation.stage(),
        catalog.copy("assessment.insufficient.headline"),
        catalog.copy("assessment.insufficient.conclusion"),
        List.of(),
        List.of(),
        List.of(catalog.copy("assessment.insufficient.action")),
        List.of(catalog.copy("assessment.insufficient.signal")),
        List.of(),
        List.of(),
        ConfidenceLevel.LOW,
        List.of(),
        evaluation.basis());
  }

  private List<AnnualFinding> findings(
      List<AnnualRuleResult> results,
      AnnualFindingType type) {
    return results.stream()
        .filter(result -> result.findingType() == type)
        .map(result -> new AnnualFinding(
            result.headline(),
            result.conclusion(),
            result.evidence().stream().map(AnnualFact::key).toList()))
        .toList();
  }

  private List<AnnualFact> distinctFacts(List<AnnualRuleResult> results, boolean counter) {
    Map<String, AnnualFact> facts = new LinkedHashMap<>();
    for (AnnualRuleResult result : results) {
      List<AnnualFact> selected = counter ? result.counterEvidence() : result.evidence();
      for (AnnualFact fact : selected) facts.putIfAbsent(fact.key(), fact);
    }
    return List.copyOf(facts.values());
  }

  private List<String> distinctStrings(List<String> values) {
    Set<String> distinct = new LinkedHashSet<>(values);
    return new ArrayList<>(distinct);
  }
}
