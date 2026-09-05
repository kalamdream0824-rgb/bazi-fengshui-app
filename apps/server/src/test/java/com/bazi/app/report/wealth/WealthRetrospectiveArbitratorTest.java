package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRetrospectiveSignalExtractorTest.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.wealth.v3.*;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WealthRetrospectiveArbitratorTest {
  private final WealthRetrospectiveArbitrator arbitrator = new WealthRetrospectiveArbitrator();

  @Test void retentionCanLeadEvenWhenIncomeFocusSelectsAnotherPath() {
    var input = assessment(List.of(
        source("income", "stable_income", "annual.stem.ten_god", 4, EvidenceFamily.ANNUAL_TRIGGER),
        source("limit", "retention", "annual.branch.clash.day", -8, EvidenceFamily.ANNUAL_TRIGGER),
        source("skill", "skill_income", "natal.combination.output_wealth", 3, EvidenceFamily.NATAL_COMBINATION)));
    assertEquals(List.of("stable_income"), input.focus().primaryCandidates());
    var result = arbitrator.plan(input);
    assertEquals("retention", result.primary().subject());
    assertEquals("restricted", result.primary().direction());
    assertEquals("annual_clash", result.primary().angle());
    assertEquals("complete", result.completeness());
  }

  @Test void hiddenSlotPrioritizesEvidenceBackedRetention() {
    var input = assessment(List.of(
        source("income", "stable_income", "annual.stem.ten_god", 8, EvidenceFamily.ANNUAL_TRIGGER),
        source("skill", "skill_income", "natal.combination.output_wealth", 6, EvidenceFamily.NATAL_COMBINATION),
        source("limit", "retention", "annual.branch.clash.day", -2, EvidenceFamily.ANNUAL_TRIGGER),
        source("shared", "cooperation_income", "dayun.stem.ten_god", 1, EvidenceFamily.DAYUN_CONTEXT)));
    var result = arbitrator.plan(input);
    assertEquals("stable_income", result.primary().subject());
    assertEquals("skill_income", result.secondary().subject());
    assertEquals("retention", result.hidden().subject());
  }

  @Test void mixedPrimaryRetainsItsCounterevidenceAndRisk() {
    var input = assessment(List.of(
        source("positive", "retention", "natal.combination.wealth_capacity", 5, EvidenceFamily.NATAL_COMBINATION),
        source("negative", "retention", "annual.branch.clash.day", -4, EvidenceFamily.ANNUAL_TRIGGER)));
    var result = arbitrator.plan(input);
    assertEquals("mixed", result.primary().direction());
    assertEquals(2, result.primary().citations().size());
    assertNotNull(result.hidden());
    assertNull(result.secondary());
    assertNotEquals(result.primary().angle(), result.hidden().angle());
    assertTrue(Collections.disjoint(result.primary().dominantRootKeys(), result.hidden().dominantRootKeys()));
  }

  @Test void tiesUseExplicitSubjectPriorityAndIgnoreFocusAndListOrder() {
    var input = assessment(List.of(
        source("income", "stable_income", "annual.stem.ten_god", 4, EvidenceFamily.ANNUAL_TRIGGER),
        source("skill", "skill_income", "annual.stem.ten_god", 4, EvidenceFamily.ANNUAL_TRIGGER)));
    var reversedFocus = new WealthAssessment(input.year(), input.ganZhi(), reversed(input.facts()),
        reversed(input.evidence()), reversed(input.decisions()),
        new WealthAssessment.Focus("leading", List.of("skill_income"), List.of()), input.risk());
    assertEquals("stable_income", arbitrator.plan(input).primary().subject());
    assertEquals(arbitrator.plan(input), arbitrator.plan(reversedFocus));
    assertNull(arbitrator.plan(input).secondary(), "same annual-stem angle cannot fill two slots");
  }

  @Test void specificityBreaksEqualWeightsBeforeSubjectOrder() {
    var input = assessment(List.of(
        source("income", "stable_income", "natal.ten_god.month.stem.正财", 4, EvidenceFamily.NATAL_STRUCTURE),
        source("skill", "skill_income", "annual.stem.ten_god", 4, EvidenceFamily.ANNUAL_TRIGGER)));
    assertEquals("skill_income", arbitrator.plan(input).primary().subject());
  }

  @Test void absoluteNetWeightBreaksEqualSalienceBeforeSubjectOrder() {
    var input = assessment(List.of(
        source("income", "stable_income", "annual.stem.ten_god", 3, EvidenceFamily.ANNUAL_TRIGGER),
        source("cost", "stable_income", "dayun.stem.ten_god", -3, EvidenceFamily.DAYUN_CONTEXT),
        source("skill", "skill_income", "annual.stem.ten_god", 6, EvidenceFamily.ANNUAL_TRIGGER)));
    assertEquals("skill_income", arbitrator.plan(input).primary().subject());
  }

  @Test void unresolvedProvenanceIsPreservedForAuditButNotPromotedToAReview() {
    var citation = source("income", "stable_income", "annual.stem.ten_god", 4,
        EvidenceFamily.ANNUAL_TRIGGER);
    var input = new WealthExpressionPolicy().assess(new WealthAssessmentInput(2025, "乙巳",
        citation.roots(), List.of(citation.evidence()), java.util.Set.of("income")));
    assertEquals(1, new WealthRetrospectiveSignalExtractor().extract(input).paths().stream()
        .mapToInt(p -> p.citations().size()).sum());
    assertEquals("empty", arbitrator.plan(input).completeness());
  }

  @Test void annualHarmAndClashWithTheSamePathProduceDifferentAngles() {
    var clash = arbitrator.plan(assessment(List.of(source("limit", "retention",
        "annual.branch.clash.day", -2, EvidenceFamily.ANNUAL_TRIGGER))));
    var harm = arbitrator.plan(assessment(List.of(source("limit", "retention",
        "annual.branch.harm.day", -2, EvidenceFamily.ANNUAL_TRIGGER))));
    assertNotEquals(clash.primary().angle(), harm.primary().angle());
    assertNotEquals(clash.evidenceSignature(), harm.evidenceSignature());
  }

  @Test void missingEvidenceDoesNotBecomeAnInventedThreePointReview() {
    var empty = arbitrator.plan(assessment(List.of()));
    assertEquals("empty", empty.completeness());
    var one = arbitrator.plan(assessment(List.of(source("income", "stable_income",
        "annual.stem.ten_god", 4, EvidenceFamily.ANNUAL_TRIGGER))));
    assertEquals("partial", one.completeness());
    assertNull(one.secondary());
    assertNull(one.hidden());
  }

  @Test void twelveBirthFixturesHaveTraceableDeterministicPlans() throws Exception {
    var json = new ObjectMapper().findAndRegisterModules();
    var fixtures = json.readTree(Path.of("src/test/resources/report/wealth-v2-baseline-inputs.json").toFile());
    var zone = ZoneId.of(fixtures.get("zoneId").asText());
    var output = new ArrayList<Map<String, Object>>();
    var signatures = new LinkedHashMap<String, String>();
    for (var fixture : fixtures.get("fullBirthCases")) {
      String id = fixture.get("id").asText();
      var asOf = LocalDate.parse(fixture.get("asOf").asText());
      var request = json.treeToValue(fixture.get("request"), PaipanRequest.class);
      var chart = new BaziService().paipan(request);
      var factory = new AnnualContextFactory(Clock.fixed(asOf.atStartOfDay(zone).toInstant(), zone));
      var contexts = new ArrayList<com.bazi.app.report.AnnualContext>();
      contexts.add(factory.createYear(request, chart, asOf.getYear() - 1));
      contexts.addAll(factory.create(request, chart, com.bazi.app.report.ReportHorizon.WEALTH_PRODUCT));
      var input = new WealthV3Analyzer().analyze(chart, contexts).get(0);
      var expected = arbitrator.plan(input);
      for (int i = 0; i < 100; i++) {
        var evidence = new ArrayList<>(input.evidence());
        var decisions = new ArrayList<>(input.decisions());
        var facts = new ArrayList<>(input.facts());
        Collections.shuffle(evidence, new Random(i));
        Collections.shuffle(decisions, new Random(i + 100));
        Collections.shuffle(facts, new Random(i + 200));
        var actual = arbitrator.plan(new WealthAssessment(input.year(), input.ganZhi(), facts,
            evidence, decisions, input.focus(), input.risk()));
        assertEquals(expected, actual, id + " permutation " + i);
        assertEquals(expected.evidenceSignature(), actual.evidenceSignature());
      }
      assertNotNull(expected.primary(), id);
      for (var observation : expected.observations()) {
        for (var citation : observation.citations()) {
          assertTrue(input.evidence().stream().anyMatch(e -> e.id().equals(citation.evidence().id())), id);
          assertTrue(input.facts().containsAll(citation.roots()), id);
        }
      }
      signatures.put(id, expected.evidenceSignature());
      var row = new LinkedHashMap<String, Object>();
      row.put("id", id);
      row.put("completeness", expected.completeness());
      row.put("missingSlots", expected.missingSlots());
      row.put("evidenceSignature", expected.evidenceSignature());
      row.put("plan", expected);
      output.add(row);
    }
    assertEquals(12, output.size());
    assertEquals(3, List.of("R03", "R04", "R05").stream().map(signatures::get).distinct().count());
    var report = new LinkedHashMap<String, Object>();
    report.put("plannerVersion", WealthRetrospectivePlan.VERSION);
    report.put("sampleCount", output.size());
    report.put("completeCount", output.stream().filter(r -> r.get("completeness").equals("complete")).count());
    report.put("uniqueEvidenceSignatureCount", signatures.values().stream().distinct().count());
    report.put("permutationsPerSample", 100);
    report.put("cases", output);
    Files.writeString(Path.of("target/wealth-retrospective-plan-preflight.json"),
        json.writerWithDefaultPrettyPrinter().writeValueAsString(report) + "\n");
  }
}
