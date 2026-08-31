package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.wealth.v3.WealthAnnualComparator;
import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthExpressionPolicy;
import com.bazi.app.report.wealth.v3.WealthV3Analyzer;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class WealthComparisonIntegrationTest {
  private final WealthV3Analyzer analyzer = new WealthV3Analyzer();

  @Test
  void fullBirthInputsKeepAllAssessmentsAndProduceOnlyGroundedAdjacentComparisons() throws Exception {
    int count = 0;
    int years = 0;
    int comparisons = 0;
    var captured = JSON.createArrayNode();
    for (JsonNode sample : read("wealth-v2-baseline-20260829.json").get("cases")) {
      if (!sample.get("kind").asText().equals("full_birth_input")) continue;
      count++;
      var input = birth(sample, sample.get("asOf").asText(), sample.get("horizonYears").asInt());
      var assessed = analyzer.analyze(input.chart(), input.contexts());
      var result = analyzer.analyzeWithComparisons(input.chart(), input.contexts());
      assertEquals(assessed.size(), result.size());
      var snapshot = captured.addObject();
      snapshot.set("id", sample.get("id"));
      snapshot.set("years", JSON.valueToTree(result));
      for (int i = 0; i < result.size(); i++) {
        years++;
        var current = result.get(i);
        assertEquals(assessed.get(i), current.assessment(), "comparison must not rewrite the independent judgment");
        if (i + 1 == result.size()) {
          assertNull(current.comparison());
          continue;
        }
        comparisons++;
        var next = result.get(i + 1).assessment();
        var comparison = current.comparison();
        assertNotNull(comparison);
        assertEquals(next.year(), comparison.toYear());
        assertEquals(comparison.changes().isEmpty() ? "unchanged" : "changed", comparison.direction());
        assertEquals(comparison.changes().size(), comparison.changes().stream().map(c -> c.path()).distinct().count());
        for (var change : comparison.changes()) {
          var before = WealthExpressionPolicyTest.decision(current.assessment(), change.path());
          var after = WealthExpressionPolicyTest.decision(next, change.path());
          assertEquals(after.supportWeight() - before.supportWeight(), change.supportDelta());
          assertEquals(after.limitationWeight() - before.limitationWeight(), change.limitationDelta());
          assertReferences(current.assessment(), change.path(), change.removedEvidenceIds());
          assertReferences(next, change.path(), change.addedEvidenceIds());
        }
      }
    }
    assertEquals(15, count);
    assertEquals(46, years);
    assertEquals(31, comparisons);
    if (Boolean.getBoolean("wealth.captureComparisons")) {
      var output = JSON.createObjectNode();
      output.put("scope", "internal-comparison-only-not-a-ready-report");
      output.put("calculationVersion", WealthV3Analyzer.CALCULATION_VERSION);
      output.put("policyVersion", WealthExpressionPolicy.VERSION);
      output.set("cases", captured);
      Files.writeString(Path.of("target/wealth-v3-comparisons.json"),
          JSON.writerWithDefaultPrettyPrinter().writeValueAsString(output) + "\n");
    }
  }

  @Test
  void theSameActualPairIsUnchangedAcrossHorizonsAndShiftedStartingYear() throws Exception {
    var sample = find(read("wealth-v2-baseline-20260829.json").get("cases"), "R01");
    var five = analyze(birth(sample, "2026-08-29", 5));
    var three = analyze(birth(sample, "2026-08-29", 3));
    var two = analyze(birth(sample, "2026-08-29", 2));
    var shifted = analyze(birth(sample, "2027-08-29", 3));
    assertEquals(5, five.size());
    assertEquals(3, three.size());
    assertEquals(2, two.size());
    assertEquals(3, shifted.size());
    assertEquals(five.get(0), two.get(0));
    assertEquals(five.subList(0, 2), three.subList(0, 2));
    assertEquals(five.subList(1, 3), shifted.subList(0, 2));
    assertEquals(three.get(1).assessment(), two.get(1).assessment());
    assertNull(two.get(1).comparison(), "last year has no next-year comparison, not a changed own-year judgment");
    assertEquals(3, ReportHorizon.WEALTH_PRODUCT.years());
  }

  @Test
  void theCombinedEntryStillRejectsUnavailableOrOutOfRangeContexts() throws Exception {
    var sample = find(read("wealth-v2-baseline-20260829.json").get("cases"), "R01");
    var input = birth(sample, "2026-08-29", 3);
    assertThrows(IllegalArgumentException.class, () -> analyzer.analyzeWithComparisons(input.chart(), null));
    assertThrows(IllegalArgumentException.class, () -> analyzer.analyzeWithComparisons(input.chart(), List.of()));
    assertThrows(IllegalArgumentException.class, () -> analyzer.analyzeWithComparisons(input.chart(), input.contexts().subList(0, 1)));
  }

  private List<WealthAnnualComparator.ComparedYear> analyze(BirthInput input) {
    return analyzer.analyzeWithComparisons(input.chart(), input.contexts());
  }

  private static void assertReferences(WealthAssessment year, String path, List<String> ids) {
    var actualIds = year.evidence().stream().filter(e -> e.path().equals(path)).map(e -> e.id()).collect(Collectors.toSet());
    assertTrue(actualIds.containsAll(ids), "difference references must belong to the correct year and path");
  }

  private BirthInput birth(JsonNode sample, String asOf, int horizon) throws Exception {
    var request = JSON.treeToValue(sample.get("request"), PaipanRequest.class);
    var chart = new BaziService().paipan(request);
    var zone = ZoneId.of("Asia/Shanghai");
    var clock = Clock.fixed(LocalDate.parse(asOf).atStartOfDay(zone).toInstant(), zone);
    return new BirthInput(chart, new AnnualContextFactory(clock).create(request, chart, ReportHorizon.of(horizon)));
  }

  private record BirthInput(PaipanResultDto chart, List<AnnualContext> contexts) {}
}
