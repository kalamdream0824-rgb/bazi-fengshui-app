package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.wealth.v3.WealthAssessmentInput;
import com.bazi.app.report.wealth.v3.WealthExpressionPolicy;
import com.bazi.app.report.wealth.v3.WealthProvenanceResolver;
import com.bazi.app.report.wealth.v3.WealthV3Analyzer;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class WealthProvenanceResolverTest {
  private final WealthProvenanceResolver resolver = new WealthProvenanceResolver();
  private final WealthPathEvaluator evaluator = new WealthPathEvaluator();

  @Test
  void expandsDerivedCombinationsToOriginalOccurrencesInsteadOfUsingTheCombinationAsARoot() throws Exception {
    var facts = natalOnly(true);
    var input = resolver.resolve(facts, evaluator.evaluatePaths(facts));
    var combination = input.evidence().stream().filter(e -> e.path().equals("skill_income")
        && e.factKey().equals("natal.combination.output_wealth")).findFirst().orElse(null);
    assertNotNull(combination);
    var roots = roots(input, combination.rootFactIds());
    assertTrue(roots.stream().allMatch(r -> r.kind().equals("natal")));
    assertTrue(roots.stream().allMatch(r -> r.code().startsWith("natal.ten_god.")));
    assertTrue(roots.size() >= 2);
    assertTrue(input.unresolvedEvidenceIds().isEmpty());
  }

  @Test
  void twoLegacyNatalFamiliesDoNotUpgradeTheNewPolicy() throws Exception {
    var facts = natalOnly(true);
    var result = new WealthExpressionPolicy().assess(resolver.resolve(facts, evaluator.evaluatePaths(facts)));
    assertEquals("supported", WealthExpressionPolicyTest.decision(result, "skill_income").strength());
  }

  @Test
  void missingBranchContextIsExplicitlyUnresolvedWithoutInventingTargetRoots() throws Exception {
    var facts = fullBirthFacts("R01").get(0);
    var input = resolver.resolve(facts, evaluator.evaluatePaths(facts));
    assertFalse(input.unresolvedEvidenceIds().isEmpty());
    assertFalse(input.facts().stream().anyMatch(f -> f.code().startsWith("natal.branch.")));
    var result = new WealthExpressionPolicy().assess(input);
    assertTrue(WealthExpressionPolicyTest.decision(result, "project_income").reasonCodes()
        .contains("legacy_provenance_unresolved"));
    assertFalse(result.focus().primaryCandidates().contains("project_income"));
  }

  @Test
  void yearRelationUsesBothActualBranchesAndTheOriginalRuleWeight() throws Exception {
    var facts = fullBirthFacts("R01").get(0);
    var input = resolver.resolve(facts, evaluator.evaluatePaths(facts), Map.of("time", "未"), null);
    var relation = input.evidence().stream().filter(e -> e.path().equals("cooperation_income")
        && e.factKey().equals("annual.branch.harmony.time")).findFirst().orElse(null);
    assertNotNull(relation);
    assertEquals(2, relation.weight());
    assertEquals(Set.of("annual.branch", "natal.branch.time"),
        roots(input, relation.rootFactIds()).stream().map(r -> r.code()).collect(Collectors.toSet()));
    assertFalse(input.unresolvedEvidenceIds().contains(relation.id()));
  }

  @Test
  void weakBalanceLimitationRetainsTheWealthActivationCondition() throws Exception {
    var facts = fullBirthFacts("R05").stream().filter(f -> evaluator.evaluatePaths(f)
        .get(WealthPath.RETENTION).scoredEvidence().stream()
        .anyMatch(e -> e.evidence().key().equals("natal.balance"))).findFirst().orElseThrow();
    var input = resolver.resolve(facts, evaluator.evaluatePaths(facts));
    var limit = input.evidence().stream().filter(e -> e.path().equals("retention")
        && e.factKey().equals("natal.balance")).findFirst().orElse(null);
    assertNotNull(limit);
    assertEquals(-3, limit.weight());
    var roots = roots(input, limit.rootFactIds());
    assertTrue(roots.stream().anyMatch(f -> f.code().equals("natal.balance")));
    assertTrue(roots.stream().anyMatch(f -> f.code().equals("annual.stem.ten_god")
        || f.code().equals("dayun.stem.ten_god")));
  }

  @Test
  void unknownScoredSourceFailsInsteadOfFabricatingAProvenanceRecord() throws Exception {
    var original = syntheticAnnualFacts(2026, "正财", null);
    var unknown = new WealthEvidence("annual.branch.harmony.unmapped", EvidenceFamily.ANNUAL_TRIGGER,
        "未知来源", "不能从文案猜测根事实");
    var facts = withAnnualEvidence(original, List.of(original.evidence().get(0), unknown));
    assertThrows(IllegalArgumentException.class, () -> resolver.resolve(facts, evaluator.evaluatePaths(facts)));
  }

  @Test
  void inconsistentEvidenceValueIsRejected() throws Exception {
    var original = syntheticAnnualFacts(2026, "正财", null);
    var facts = withAnnualEvidence(original, List.of(new WealthEvidence("annual.stem.ten_god",
        EvidenceFamily.ANNUAL_TRIGGER, "不一致的测试值", "偏财")));
    assertThrows(IllegalArgumentException.class, () -> resolver.resolve(facts, evaluator.evaluatePaths(facts)));
  }

  @Test
  void suppliedBranchesMustReallySupportTheClaimedRelation() throws Exception {
    var facts = fullBirthFacts("R01").get(0);
    assertThrows(IllegalArgumentException.class, () -> resolver.resolve(
        facts, evaluator.evaluatePaths(facts), Map.of("time", "子"), null));
  }

  @Test
  void allFifteenCompleteInputCasesRetainEveryOriginalWeightAndHaveResolvedRoots() throws Exception {
    int cases = 0;
    var captured = JSON.createArrayNode();
    for (var sample : read("wealth-v2-baseline-20260829.json").get("cases")) {
      if (!sample.get("kind").asText().equals("full_birth_input")) continue;
      cases++;
      var request = JSON.treeToValue(sample.get("request"), PaipanRequest.class);
      var chart = new BaziService().paipan(request);
      var clock = Clock.fixed(LocalDate.parse(sample.get("asOf").asText())
          .atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant(), ZoneId.of("Asia/Shanghai"));
      var contexts = new AnnualContextFactory(clock).create(request, chart,
          ReportHorizon.of(sample.get("horizonYears").asInt()));
      var facts = new WealthFactExtractor().extract(chart, contexts);
      var result = new WealthV3Analyzer().analyze(chart, contexts);
      assertEquals(facts.size(), result.size());
      var snapshot = captured.addObject();
      snapshot.set("id", sample.get("id"));
      snapshot.set("years", JSON.valueToTree(result));
      for (int i = 0; i < facts.size(); i++) {
        var raw = evaluator.evaluatePaths(facts.get(i));
        var year = result.get(i);
        assertEquals(5, year.decisions().size());
        assertFalse(year.decisions().stream().anyMatch(d -> d.reasonCodes().contains("legacy_provenance_unresolved")),
            sample.get("id") + " year " + year.year());
        for (var path : WealthPath.values()) {
          var decision = WealthExpressionPolicyTest.decision(year, path.code());
          assertEquals(raw.get(path).score(), decision.netWeight());
          assertEquals(raw.get(path).limitationWeight(), decision.limitationWeight());
          assertEquals(raw.get(path).scoredEvidence().stream().collect(Collectors.toMap(
              e -> e.evidence().key(), e -> e.weight())), year.evidence().stream()
              .filter(e -> e.path().equals(path.code())).collect(Collectors.toMap(e -> e.factKey(), e -> e.weight())));
        }
        assertTrue(year.evidence().stream().allMatch(e -> !e.rootFactIds().isEmpty()));
      }
    }
    assertEquals(15, cases);
    if (Boolean.getBoolean("wealth.captureAssessments")) {
      var output = JSON.createObjectNode();
      output.put("scope", "internal-assessment-only-not-a-ready-report");
      output.put("calculationVersion", WealthV3Analyzer.CALCULATION_VERSION);
      output.put("policyVersion", WealthExpressionPolicy.VERSION);
      output.set("cases", captured);
      // Generated diagnostic artifact only; never overwrite the accepted v2 baseline.
      Files.writeString(Path.of("target/wealth-v3-assessments.json"),
          JSON.writerWithDefaultPrettyPrinter().writeValueAsString(output) + "\n");
    }
  }

  @Test
  void repeatedAndShiftedYearAssessmentIsIndependentOfReportPosition() throws Exception {
    var analyzer = new WealthV3Analyzer();
    var a = fullBirthFacts("R01").get(1);
    var b = fullBirthFacts("Y27").get(0);
    assertEquals(analyzer.assess(a), analyzer.assess(b));
    assertEquals(analyzer.assess(a), analyzer.assess(a));
    assertTrue(analyzer.assess(a).decisions().stream().anyMatch(d -> d.netWeight() != 0));
  }

  @Test
  void fullContextResultsKeepCommonYearsAcrossTwoThreeFiveYearsAndShiftedStart() throws Exception {
    var sample = find(read("wealth-v2-baseline-20260829.json").get("cases"), "R01");
    var request = JSON.treeToValue(sample.get("request"), PaipanRequest.class);
    var chart = new BaziService().paipan(request);
    var zone = ZoneId.of("Asia/Shanghai");
    var factory = new AnnualContextFactory(Clock.fixed(LocalDate.of(2026, 8, 29).atStartOfDay(zone).toInstant(), zone));
    var shifted = new AnnualContextFactory(Clock.fixed(LocalDate.of(2027, 8, 29).atStartOfDay(zone).toInstant(), zone));
    var analyzer = new WealthV3Analyzer();
    var three = analyzer.analyze(chart, factory.create(request, chart, ReportHorizon.of(3)));
    var two = analyzer.analyze(chart, factory.create(request, chart, ReportHorizon.of(2)));
    var five = analyzer.analyze(chart, factory.create(request, chart, ReportHorizon.of(5)));
    var next = analyzer.analyze(chart, shifted.create(request, chart, ReportHorizon.of(3)));
    assertEquals(three.subList(0, 2), two);
    assertEquals(three, five.subList(0, 3));
    assertEquals(three.subList(1, 3), next.subList(0, 2));
    assertEquals(3, ReportHorizon.WEALTH_PRODUCT.years());
    assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(chart, List.of()));
    assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(chart, factory.create(request, chart).subList(0, 1)));
  }

  private List<com.bazi.app.report.wealth.v3.WealthAssessment.Fact> roots(WealthAssessmentInput input, List<String> ids) {
    return ids.stream().map(id -> input.facts().stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow()).toList();
  }
}
