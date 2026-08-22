package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.DaYunDto;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.service.BaziService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnnualRuleEngineTest {

  private AnnualRuleEngine engine;
  private ReportAnalysis natalAnalysis;

  @BeforeEach
  void setUp() {
    engine = new AnnualRuleEngine();
    PaipanRequest request = new PaipanRequest(
        "林先生", "male", "1995-10-08T14:30:00", "上海", false);
    PaipanResultDto chart = new BaziService().paipan(request);
    natalAnalysis = ReportAnalysis.from(request, chart);
  }

  @Test
  void ignoresRulesThatDoNotMatchTheAnnualFacts() {
    AnnualContext context = contextWith("annual.stem.group.resource");
    AnnualRule rule = rule(
        "career.visibility", ReportTopic.CAREER, "visibility",
        AnnualFindingType.OPPORTUNITY, 60, ConfidenceLevel.HIGH, AnnualStage.ADVANCE,
        List.of("annual.stem.group.output", "natal.balance.supportive"), List.of());

    assertTrue(engine.evaluate(context, ReportTopic.CAREER, List.of(rule)).isEmpty());
  }

  @Test
  void filtersOutRulesForAThemeTheUserDidNotPurchase() {
    AnnualContext context = contextWith(
        "annual.stem.group.output", "natal.balance.supportive");
    AnnualRule career = matchingRule("career.visibility", ReportTopic.CAREER, "visibility", 60);
    AnnualRule wealth = matchingRule("wealth.monetization", ReportTopic.WEALTH, "monetization", 80);

    List<AnnualRuleResult> results = engine.evaluate(
        context, ReportTopic.CAREER, List.of(wealth, career));

    assertEquals(List.of("career.visibility"), results.stream().map(AnnualRuleResult::ruleKey).toList());
  }

  @Test
  void refusesAConclusionWithOnlyOneIndependentEvidenceItem() {
    AnnualContext context = contextWith("annual.stem.group.output");
    AnnualRule rule = rule(
        "career.visibility", ReportTopic.CAREER, "visibility",
        AnnualFindingType.OPPORTUNITY, 60, ConfidenceLevel.HIGH, AnnualStage.ADVANCE,
        List.of("annual.stem.group.output"), List.of());

    assertTrue(engine.evaluate(context, ReportTopic.CAREER, List.of(rule)).isEmpty());
  }

  @Test
  void counterEvidenceLowersConfidenceAndRemainsVisible() {
    AnnualContext context = contextWith(
        "annual.stem.group.output",
        "natal.balance.supportive",
        "annual.branch.clash.dayun");
    AnnualRule rule = rule(
        "career.visibility", ReportTopic.CAREER, "visibility",
        AnnualFindingType.OPPORTUNITY, 60, ConfidenceLevel.HIGH, AnnualStage.ADVANCE,
        List.of("annual.stem.group.output", "natal.balance.supportive"),
        List.of("annual.branch.clash.dayun"));

    AnnualRuleResult result = engine.evaluate(
        context, ReportTopic.CAREER, List.of(rule)).get(0);

    assertEquals(ConfidenceLevel.MEDIUM, result.confidence());
    assertEquals(
        List.of("annual.branch.clash.dayun"),
        result.counterEvidence().stream().map(AnnualFact::key).toList());
  }

  @Test
  void keepsOnlyTheHighestRankedResultForOneConclusionCategory() {
    AnnualContext context = contextWith(
        "annual.stem.group.output", "natal.balance.supportive");
    AnnualRule lower = matchingRule("career.visibility.basic", ReportTopic.CAREER, "visibility", 40);
    AnnualRule higher = matchingRule("career.visibility.strong", ReportTopic.CAREER, "visibility", 80);

    List<AnnualRuleResult> results = engine.evaluate(
        context, ReportTopic.CAREER, List.of(lower, higher));

    assertEquals(List.of("career.visibility.strong"), results.stream().map(AnnualRuleResult::ruleKey).toList());
  }

  @Test
  void capsOneYearAtTwoOpportunitiesAndTwoPressures() {
    AnnualContext context = contextWith(
        "annual.stem.group.output", "natal.balance.supportive",
        "natal.fact.a", "natal.fact.b", "natal.fact.c", "natal.fact.d");
    List<AnnualRule> rules = List.of(
        matchingRule("opportunity.1", ReportTopic.CAREER, "opportunity.1", 90),
        matchingRule("opportunity.2", ReportTopic.CAREER, "opportunity.2", 80),
        matchingRule("opportunity.3", ReportTopic.CAREER, "opportunity.3", 70),
        pressureRule("pressure.1", "pressure.1", 90),
        pressureRule("pressure.2", "pressure.2", 80),
        pressureRule("pressure.3", "pressure.3", 70));

    List<AnnualRuleResult> results = engine.evaluate(context, ReportTopic.CAREER, rules);

    assertEquals(2, results.stream().filter(AnnualRuleResult::isOpportunity).count());
    assertEquals(2, results.stream().filter(AnnualRuleResult::isPressure).count());
    assertFalse(results.stream().anyMatch(result -> result.ruleKey().endsWith(".3")));
  }

  @Test
  void producesAnExplicitInsufficientEvidenceEvaluationWhenNothingSurvives() {
    AnnualContext context = contextWith("annual.stem.group.resource");

    AnnualRuleEvaluation evaluation = engine.evaluateYear(
        context,
        ReportTopic.CAREER,
        List.of(matchingRule("career.visibility", ReportTopic.CAREER, "visibility", 60)));

    assertEquals(AssessmentBasis.INSUFFICIENT_EVIDENCE, evaluation.basis());
    assertEquals(AnnualStage.STABLE, evaluation.stage());
    assertTrue(evaluation.results().isEmpty());
  }

  @Test
  void selectsTransitionForAHighPriorityDayunClash() {
    AnnualContext context = contextWith(
        "annual.stem.group.output", "natal.balance.supportive", "annual.branch.clash.dayun");
    AnnualRule transition = rule(
        "career.transition", ReportTopic.CAREER, "transition",
        AnnualFindingType.PRESSURE, 90, ConfidenceLevel.HIGH, AnnualStage.TRANSITION,
        List.of("annual.stem.group.output", "annual.branch.clash.dayun"), List.of());

    AnnualRuleEvaluation evaluation = engine.evaluateYear(
        context, ReportTopic.CAREER, List.of(transition));

    assertEquals(AnnualStage.TRANSITION, evaluation.stage());
  }

  @Test
  void selectsCautionWhenMediumOrHighConfidencePressuresDominate() {
    AnnualContext context = contextWith(
        "annual.stem.group.output", "natal.balance.supportive");

    AnnualRuleEvaluation evaluation = engine.evaluateYear(
        context,
        ReportTopic.CAREER,
        List.of(
            pressureRule("pressure.1", "pressure.1", 90),
            pressureRule("pressure.2", "pressure.2", 80)));

    assertEquals(AnnualStage.CAUTION, evaluation.stage());
  }

  @Test
  void selectsAdvanceWhenOpportunitiesDominateWithoutHighConfidencePressure() {
    AnnualContext context = contextWith(
        "annual.stem.group.output", "natal.balance.supportive");

    AnnualRuleEvaluation evaluation = engine.evaluateYear(
        context,
        ReportTopic.CAREER,
        List.of(
            matchingRule("opportunity.1", ReportTopic.CAREER, "opportunity.1", 90),
            matchingRule("opportunity.2", ReportTopic.CAREER, "opportunity.2", 80)));

    assertEquals(AnnualStage.ADVANCE, evaluation.stage());
  }

  @Test
  void selectsPrepareForResourceFindingsWithoutOutwardActivation() {
    AnnualContext context = contextWith(
        "annual.stem.group.resource", "natal.balance.supportive");
    AnnualRule preparation = rule(
        "career.preparation", ReportTopic.CAREER, "preparation",
        AnnualFindingType.OPPORTUNITY, 70, ConfidenceLevel.MEDIUM, AnnualStage.PREPARE,
        List.of("annual.stem.group.resource", "natal.balance.supportive"), List.of());

    AnnualRuleEvaluation evaluation = engine.evaluateYear(
        context, ReportTopic.CAREER, List.of(preparation));

    assertEquals(AnnualStage.PREPARE, evaluation.stage());
  }

  @Test
  void defaultsToStableWhenOpportunityAndPressureAreBalanced() {
    AnnualContext context = contextWith(
        "annual.stem.group.output", "natal.balance.supportive");

    AnnualRuleEvaluation evaluation = engine.evaluateYear(
        context,
        ReportTopic.CAREER,
        List.of(
            matchingRule("opportunity.1", ReportTopic.CAREER, "opportunity.1", 80),
            pressureRule("pressure.1", "pressure.1", 70)));

    assertEquals(AnnualStage.STABLE, evaluation.stage());
  }

  private AnnualRule matchingRule(String key, ReportTopic topic, String category, int priority) {
    return rule(
        key, topic, category, AnnualFindingType.OPPORTUNITY, priority,
        ConfidenceLevel.HIGH, AnnualStage.ADVANCE,
        List.of("annual.stem.group.output", "natal.balance.supportive"), List.of());
  }

  private AnnualRule pressureRule(String key, String category, int priority) {
    return rule(
        key, ReportTopic.CAREER, category, AnnualFindingType.PRESSURE, priority,
        ConfidenceLevel.HIGH, AnnualStage.CAUTION,
        List.of("annual.stem.group.output", "natal.balance.supportive"), List.of());
  }

  private AnnualRule rule(
      String key,
      ReportTopic topic,
      String category,
      AnnualFindingType findingType,
      int priority,
      ConfidenceLevel confidence,
      AnnualStage stageHint,
      List<String> supportKeys,
      List<String> counterKeys) {
    return new TestRule(
        key, topic, category, findingType, priority, confidence, stageHint,
        supportKeys, counterKeys);
  }

  private AnnualContext contextWith(String... keys) {
    TenGodGroup group = TenGodGroup.RESOURCE;
    List<AnnualFact> natalFacts = new ArrayList<>();
    List<AnnualFact> natalRelations = new ArrayList<>();
    List<AnnualFact> dayunRelations = new ArrayList<>();
    for (String key : keys) {
      if (key.startsWith("annual.stem.group.")) {
        group = TenGodGroup.valueOf(key.substring(key.lastIndexOf('.') + 1).toUpperCase());
      } else if (key.endsWith(".dayun")) {
        dayunRelations.add(fact(key));
      } else if (key.startsWith("annual.branch.")) {
        natalRelations.add(fact(key));
      } else {
        natalFacts.add(fact(key));
      }
    }
    return new AnnualContext(
        2026,
        "丙午",
        tenGodName(group),
        group,
        new DaYunDto("", "壬午", "2025 - 2035", true, "", "", "", "", List.of()),
        natalFacts,
        natalRelations,
        dayunRelations,
        natalAnalysis);
  }

  private AnnualFact fact(String key) {
    return new AnnualFact(key, key, key);
  }

  private String tenGodName(TenGodGroup group) {
    return switch (group) {
      case RESOURCE -> "正印";
      case PEER -> "比肩";
      case OUTPUT -> "食神";
      case WEALTH -> "正财";
      case AUTHORITY -> "正官";
    };
  }

  private record TestRule(
      String key,
      ReportTopic topic,
      String category,
      AnnualFindingType findingType,
      int priority,
      ConfidenceLevel confidence,
      AnnualStage stageHint,
      List<String> supportKeys,
      List<String> counterKeys) implements AnnualRule {

    @Override
    public boolean matches(AnnualContext context) {
      return context.factKeys().containsAll(supportKeys);
    }

    @Override
    public List<AnnualFact> supportingEvidence(AnnualContext context) {
      return select(context, supportKeys);
    }

    @Override
    public List<AnnualFact> counterEvidence(AnnualContext context) {
      return select(context, counterKeys);
    }

    @Override
    public AnnualRuleResult build(AnnualContext context) {
      return new AnnualRuleResult(
          key,
          category,
          findingType,
          key + "标题",
          key + "结论",
          key + ".plain",
          key + ".professional",
          List.of("行动"),
          List.of("观察信号"),
          stageHint,
          priority,
          confidence,
          List.of(),
          List.of());
    }

    private List<AnnualFact> select(AnnualContext context, List<String> keys) {
      return context.facts().stream().filter(fact -> keys.contains(fact.key())).toList();
    }
  }
}
