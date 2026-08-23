package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CareerBaselineCoverageTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"),
      ZoneId.of("Asia/Shanghai"));

  private AnnualRuleCatalog catalog;
  private AnnualRuleEngine engine;
  private AnnualContext reference;

  @BeforeEach
  void setUp() {
    catalog = new AnnualRuleCatalog();
    engine = new AnnualRuleEngine();
    PaipanRequest request = new PaipanRequest(
        "覆盖测试", "male", "1995-10-08T14:30:00", "上海", false);
    reference = new AnnualContextFactory(CLOCK)
        .create(request, new BaziService().paipan(request))
        .get(0);
  }

  @Test
  void everyAnnualGroupAndBalanceStateHasASubstantiveCareerBaseline() {
    for (TenGodGroup group : TenGodGroup.values()) {
      for (String balance : List.of("weak", "middle", "strong")) {
        AnnualRuleEvaluation evaluation = engine.evaluateYear(
            context(group, balance, List.of()),
            ReportTopic.CAREER,
            catalog.rulesFor(ReportTopic.CAREER));

        AnnualRuleResult baseline = evaluation.results().stream()
            .filter(result -> result.ruleKey().equals("career.baseline"))
            .findFirst()
            .orElseThrow(() -> new AssertionError(group + "/" + balance + " has no baseline"));

        assertEquals(AssessmentBasis.REVIEWED_RULES, evaluation.basis());
        assertEquals(2, baseline.evidence().size(), group + "/" + balance);
        assertFalse(baseline.headline().contains("证据不足"));
        assertFalse(baseline.conclusion().contains("继续观察"));
        assertFalse(baseline.actions().isEmpty());
        assertFalse(baseline.realitySignals().isEmpty());
      }
    }
  }

  @Test
  void carryingCapacityChangesAuthorityFromOpportunityToPressure() {
    AnnualRuleResult strong = baseline(context(TenGodGroup.AUTHORITY, "strong", List.of()));
    AnnualRuleResult weak = baseline(context(TenGodGroup.AUTHORITY, "weak", List.of()));

    assertTrue(strong.isOpportunity());
    assertEquals(AnnualStage.ADVANCE, strong.stageHint());
    assertTrue(weak.isPressure());
    assertEquals(AnnualStage.CAUTION, weak.stageHint());
    assertFalse(strong.headline().equals(weak.headline()));
  }

  @Test
  void exposesRealCounterEvidenceInsteadOfAPlaceholder() {
    AnnualFact clash = new AnnualFact(
        "annual.branch.clash.dayun", "流年地支关系", "流年与大运相冲");
    AnnualRuleResult result = baseline(context(
        TenGodGroup.AUTHORITY,
        "strong",
        List.of(clash)));

    assertEquals(List.of("annual.branch.clash.dayun"),
        result.counterEvidence().stream().map(AnnualFact::key).toList());
    assertEquals(ConfidenceLevel.MEDIUM, result.confidence());
  }

  @Test
  void dayunConflictLowersAStrongCareerOpportunityInsteadOfBeingIgnored() {
    AnnualFact clash = new AnnualFact(
        "annual.branch.clash.dayun", "流年地支关系", "流年与大运相冲");
    AnnualContext context = context(TenGodGroup.AUTHORITY, "strong", List.of(clash));

    AnnualRuleResult responsibility = engine
        .evaluate(context, ReportTopic.CAREER, catalog.rulesFor(ReportTopic.CAREER)).stream()
        .filter(result -> result.ruleKey().equals("career.responsibility_upgrade"))
        .findFirst()
        .orElseThrow();

    assertEquals(List.of("annual.branch.clash.dayun"),
        responsibility.counterEvidence().stream().map(AnnualFact::key).toList());
    assertEquals(ConfidenceLevel.MEDIUM, responsibility.confidence());
  }

  @Test
  void representativeChartsProduceTwoReviewedCareerYearsWithoutFallbackCopy() {
    BaziService baziService = new BaziService();
    ThreeYearAssessor assessor = new ThreeYearAssessor(CLOCK, catalog);

    for (int index = 0; index < 36; index++) {
      int birthYear = 1978 + index;
      int month = index % 12 + 1;
      int day = index * 5 % 25 + 1;
      int hour = index * 7 % 24;
      PaipanRequest request = new PaipanRequest(
          "样本" + index,
          index % 2 == 0 ? "male" : "female",
          "%04d-%02d-%02dT%02d:30:00".formatted(birthYear, month, day, hour),
          "上海",
          false);

      ThreeYearAssessment assessment = assessor.assess(
          request,
          baziService.paipan(request),
          ReportTopic.CAREER);

      assertEquals(2, assessment.years().size(), "sample=" + index);
      for (YearAssessment year : assessment.years()) {
        assertEquals(AssessmentBasis.REVIEWED_RULES, year.basis(),
            "sample=" + index + ", year=" + year.year());
        assertTrue(year.evidence().stream().map(ReportEvidence::key).distinct().count() >= 2,
            "sample=" + index + ", year=" + year.year());
        assertFalse((year.headline() + year.conclusion()).contains("证据不足"),
            "sample=" + index + ", year=" + year.year());
      }
    }
  }

  private AnnualRuleResult baseline(AnnualContext context) {
    return engine.evaluate(context, ReportTopic.CAREER, catalog.rulesFor(ReportTopic.CAREER)).stream()
        .filter(result -> result.ruleKey().equals("career.baseline"))
        .findFirst()
        .orElseThrow();
  }

  private AnnualContext context(
      TenGodGroup annualGroup,
      String balance,
      List<AnnualFact> relationFacts) {
    List<AnnualFact> natalFacts = new ArrayList<>();
    natalFacts.add(new AnnualFact(
        "natal.balance." + balance,
        "命局承载状态",
        switch (balance) {
          case "weak" -> "偏弱";
          case "middle" -> "中和";
          case "strong" -> "偏强";
          default -> throw new IllegalArgumentException(balance);
        }));
    if (!balance.equals("weak")) {
      natalFacts.add(new AnnualFact("natal.balance.adequate", "命局承载状态", "具备承载基础"));
    }
    return new AnnualContext(
        2026,
        "丙午",
        tenGod(annualGroup),
        annualGroup,
        null,
        natalFacts,
        relationFacts,
        List.of(),
        reference.natalAnalysis());
  }

  private String tenGod(TenGodGroup group) {
    return switch (group) {
      case RESOURCE -> "正印";
      case PEER -> "比肩";
      case OUTPUT -> "食神";
      case WEALTH -> "正财";
      case AUTHORITY -> "正官";
    };
  }
}
