package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.*;

import com.bazi.app.report.wealth.v3.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WealthRetrospectiveSignalExtractorTest {
  private final WealthRetrospectiveSignalExtractor extractor = new WealthRetrospectiveSignalExtractor();

  static WealthAssessment assessment(List<WealthRetrospectivePlan.Citation> citations) {
    return new WealthExpressionPolicy().assess(new WealthAssessmentInput(2025, "乙巳",
        citations.stream().flatMap(c -> c.roots().stream()).distinct().toList(),
        citations.stream().map(WealthRetrospectivePlan.Citation::evidence).toList(), Set.of()));
  }

  static WealthRetrospectivePlan.Citation source(String id, String path, String key,
      int weight, EvidenceFamily family) {
    String kind = switch (family) {
      case NATAL_STRUCTURE, NATAL_COMBINATION -> "natal";
      case ANNUAL_TRIGGER -> "annual";
      case DAYUN_CONTEXT -> "dayun";
    };
    return new WealthRetrospectivePlan.Citation(new WealthAssessment.Evidence(id, path,
        "rule." + key, key, family, List.of("root." + key), weight),
        List.of(new WealthAssessment.Fact("root." + key, kind, key, "值")));
  }

  static WealthAssessment richAssessment() {
    return assessment(List.of(
        source("stable", "stable_income", "annual.stem.ten_god", 4, EvidenceFamily.ANNUAL_TRIGGER),
        source("output", "skill_income", "natal.combination.output_wealth", 3, EvidenceFamily.NATAL_COMBINATION),
        source("wealth", "project_income", "natal.ten_god.month.stem.偏财", 3, EvidenceFamily.NATAL_STRUCTURE),
        source("shared", "cooperation_income", "dayun.stem.ten_god", 2, EvidenceFamily.DAYUN_CONTEXT),
        source("harmony", "cooperation_income", "annual.branch.harmony.day", 2, EvidenceFamily.ANNUAL_TRIGGER),
        source("clash", "retention", "annual.branch.clash.day", -2, EvidenceFamily.ANNUAL_TRIGGER),
        source("harm", "retention", "annual.branch.harm.month", -2, EvidenceFamily.ANNUAL_TRIGGER),
        source("punishment", "retention", "annual.branch.punishment.year", -2, EvidenceFamily.ANNUAL_TRIGGER)));
  }

  @Test void preservesAllFiveDecisionsAndRiskRegardlessOfIncomeFocus() {
    var input = richAssessment();
    var result = extractor.extract(input);
    assertEquals(5, result.paths().size());
    assertEquals(input.risk(), result.risk());
    for (var path : result.paths()) {
      assertEquals(input.decisions().stream().filter(d -> d.path().equals(path.decision().path()))
          .findFirst().orElseThrow(), path.decision());
      assertEquals(input.evidence().stream().filter(e -> e.path().equals(path.decision().path()))
          .map(WealthAssessment.Evidence::id).sorted().toList(),
          path.citations().stream().map(c -> c.evidence().id()).toList());
    }
    assertTrue(result.paths().stream().filter(p -> p.decision().path().equals("retention"))
        .findFirst().orElseThrow().riskSelected());
  }

  @Test void retainsFamiliesActualRootsAndDistinctRelationAngles() {
    var result = extractor.extract(richAssessment());
    var angles = result.paths().stream().flatMap(p -> p.angles().stream())
        .map(WealthRetrospectiveSignalExtractor.AngleSignal::angle).collect(java.util.stream.Collectors.toSet());
    assertTrue(angles.containsAll(Set.of("annual_stem", "dayun_context", "annual_harmony",
        "annual_clash", "annual_harm", "annual_punishment", "natal_output_wealth", "natal_structure")));
    result.paths().forEach(p -> p.citations().forEach(c -> {
      assertFalse(c.roots().isEmpty());
      assertEquals("值", c.roots().get(0).value());
      assertNotNull(c.evidence().family());
    }));
  }

  @Test void reorderingInputsAndDecisionReferencesDoesNotChangeExtraction() {
    var a = richAssessment();
    var shuffled = new WealthAssessment(a.year(), a.ganZhi(), reversed(a.facts()),
        reversed(a.evidence()), reversed(a.decisions()).stream().map(d -> new WealthAssessment.Decision(
            d.id(), d.year(), d.path(), d.supportWeight(), d.limitationWeight(), d.netWeight(),
            d.stance(), d.strength(), reversed(d.supportingEvidenceIds()), reversed(d.limitingEvidenceIds()),
            reversed(d.reasonCodes()))).toList(), a.focus(),
        new WealthAssessment.Risk(a.risk().path(), reversed(a.risk().limitingEvidenceIds())));
    assertEquals(extractor.extract(a), extractor.extract(shuffled));
  }

  @Test void emptyEvidencePreservesQuietPathsWithoutMakingUpSignals() {
    var result = extractor.extract(assessment(List.of()));
    assertEquals(5, result.paths().size());
    assertTrue(result.paths().stream().allMatch(p -> p.angles().isEmpty()
        && p.citations().isEmpty() && p.decision().stance().equals("quiet")));
  }

  @Test void rejectsMissingRootAndMissingDecisionReference() {
    var a = richAssessment();
    assertThrows(IllegalArgumentException.class, () -> extractor.extract(new WealthAssessment(
        a.year(), a.ganZhi(), List.of(), a.evidence(), a.decisions(), a.focus(), a.risk())));
    assertThrows(IllegalArgumentException.class, () -> extractor.extract(new WealthAssessment(
        a.year(), a.ganZhi(), a.facts(), List.of(), a.decisions(), a.focus(), a.risk())));
  }

  @Test void rejectsCrossPathReferenceAndWrongYearInsteadOfMisattributingEvidence() {
    var a = richAssessment();
    var decisions = new ArrayList<>(a.decisions());
    var d = decisions.get(0);
    decisions.set(0, new WealthAssessment.Decision(d.id(), 2024, d.path(), d.supportWeight(),
        d.limitationWeight(), d.netWeight(), d.stance(), d.strength(), List.of("shared"), List.of(), d.reasonCodes()));
    assertThrows(IllegalArgumentException.class, () -> extractor.extract(new WealthAssessment(
        a.year(), a.ganZhi(), a.facts(), a.evidence(), decisions, a.focus(), a.risk())));
    decisions.set(0, new WealthAssessment.Decision(d.id(), 2025, d.path(), d.supportWeight(),
        d.limitationWeight(), d.netWeight(), d.stance(), d.strength(), List.of("shared"), List.of(), d.reasonCodes()));
    assertThrows(IllegalArgumentException.class, () -> extractor.extract(new WealthAssessment(
        a.year(), a.ganZhi(), a.facts(), a.evidence(), decisions, a.focus(), a.risk())));
  }

  static <T> List<T> reversed(List<T> input) {
    var result = new ArrayList<>(input);
    Collections.reverse(result);
    return result;
  }
}
