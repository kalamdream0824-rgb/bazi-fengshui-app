package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** GREEN input guards, not assertions that the unfinished v3 requirements are satisfied. */
class WealthRemediationFixtureTest {
  @Test
  void syntheticBoundaryWeightsAndCasesMatchTheFrozenManifest() throws Exception {
    for (var input : read("wealth-v2-baseline-inputs.json").get("syntheticCases")) {
      var period = scoredCase(input.get("id").asText());
      assertEquals(3, period.years().size());
      for (var year : period.years()) {
        assertEquals(Set.of(WealthPath.values()), year.paths().keySet());
        for (var path : WealthPath.values()) {
          var expected = input.get("weights").get(path.ordinal());
          var actual = year.paths().get(path);
          assertEquals(expected.get(0).asInt(), supportWeight(actual));
          assertEquals(expected.get(1).asInt(), actual.limitationWeight());
          assertEquals(supportWeight(actual) - actual.limitationWeight(), actual.score());
          assertEquals(actual.scoredEvidence().size(), actual.scoredEvidence().stream()
              .map(e -> e.evidence().key()).distinct().count());
        }
      }
    }
  }

  @Test
  void unchangedFixtureActuallyHasIdenticalEvidenceAcrossYears() throws Exception {
    var period = scoredCase("S07");
    assertEquals(period.years().get(0).paths(), period.years().get(1).paths());
    assertEquals(period.years().get(1).paths(), period.years().get(2).paths());
  }

  @Test
  void sourceMutationChangesOriginsButNotWeightsOrOtherPaths() throws Exception {
    var base = scoredCase("S07");
    var changed = sourceChanged(base);
    assertEquals(base.years().get(0), changed.years().get(0));
    assertEquals(base.years().get(2), changed.years().get(2));
    var before = base.years().get(1).paths().get(WealthPath.STABLE_INCOME);
    var after = changed.years().get(1).paths().get(WealthPath.STABLE_INCOME);
    assertEquals(before.score(), after.score());
    assertEquals(supportWeight(before), supportWeight(after));
    assertEquals(before.limitationWeight(), after.limitationWeight());
    assertNotEquals(before.support().get(0).evidence().key(), after.support().get(0).evidence().key());
    assertNotEquals(before.supportFamilies(), after.supportFamilies());
    for (var path : WealthPath.values()) if (path != WealthPath.STABLE_INCOME) {
      assertEquals(base.years().get(1).paths().get(path), changed.years().get(1).paths().get(path));
    }
  }

  @Test
  void factValueMutationKeepsKeysFamiliesAndWeightsButChangesActualValue() throws Exception {
    var before = factValuePeriod(false).years().get(1);
    var after = factValuePeriod(true).years().get(1);
    assertEquals(before.year(), after.year());
    assertEquals(before.ganZhi(), after.ganZhi());
    assertEquals(before.primaryIncomePath(), after.primaryIncomePath());
    for (var path : WealthPath.values()) {
      var a = before.paths().get(path);
      var b = after.paths().get(path);
      assertEquals(a.score(), b.score());
      assertEquals(a.scoredEvidence().stream().map(e -> List.of(e.evidence().key(), e.evidence().family(), e.weight())).toList(),
          b.scoredEvidence().stream().map(e -> List.of(e.evidence().key(), e.evidence().family(), e.weight())).toList());
    }
    var a = before.paths().get(WealthPath.SKILL_INCOME).support().stream()
        .filter(e -> e.evidence().key().equals("annual.stem.ten_god")).findFirst().orElseThrow();
    var b = after.paths().get(WealthPath.SKILL_INCOME).support().stream()
        .filter(e -> e.evidence().key().equals("annual.stem.ten_god")).findFirst().orElseThrow();
    assertEquals("食神", a.evidence().value());
    assertEquals("伤官", b.evidence().value());
  }

  @Test
  void grossWeightMutationPreservesNetButAddsSupportAndLimit() throws Exception {
    var base = scoredCase("S07");
    var changed = grossWeightsChanged(base);
    var before = base.years().get(1).paths().get(WealthPath.STABLE_INCOME);
    var after = changed.years().get(1).paths().get(WealthPath.STABLE_INCOME);
    assertEquals(before.score(), after.score());
    assertEquals(supportWeight(before) + 1, supportWeight(after));
    assertEquals(before.limitationWeight() + 1, after.limitationWeight());
  }

  @Test
  void natalCombinationAddsAnExistingDerivedRuleNotANewOrigin() throws Exception {
    var base = natalOnly(false);
    var withDerived = natalOnly(true);
    assertEquals(base.natalProfile().tenGodOccurrences(), withDerived.natalProfile().tenGodOccurrences());
    assertEquals(base.natalProfile().groupCounts(), withDerived.natalProfile().groupCounts());
    var evaluator = new WealthPathEvaluator();
    var a = evaluator.evaluatePaths(base).get(WealthPath.SKILL_INCOME);
    var b = evaluator.evaluatePaths(withDerived).get(WealthPath.SKILL_INCOME);
    assertTrue(a.score() >= 6, "net threshold must already be reached before the extra family appears");
    assertEquals(a.score() + 2, b.score(), "keep the existing combination weight, do not retune scoring");
    assertEquals(Set.of(EvidenceFamily.NATAL_STRUCTURE), a.supportFamilies());
    assertEquals(Set.of(EvidenceFamily.NATAL_STRUCTURE, EvidenceFamily.NATAL_COMBINATION), b.supportFamilies());
    assertTrue(b.support().stream().allMatch(e -> e.evidence().key().startsWith("natal.")));
  }

  @Test
  void repeatingTheExactSameEvidenceKeyDoesNotChangeCurrentCalculation() throws Exception {
    var original = fullBirthFacts("R01").get(0);
    var duplicated = new ArrayList<>(original.evidence());
    duplicated.add(original.evidence().get(0));
    var evaluator = new WealthPathEvaluator();
    assertEquals(evaluator.evaluatePaths(original),
        evaluator.evaluatePaths(withAnnualEvidence(original, duplicated)));
  }

  @Test
  void addingNegativeEvidenceNeverIncreasesSupportOrPositiveStatus() throws Exception {
    var original = fullBirthFacts("R01").get(0);
    var withLimit = new ArrayList<>(original.evidence());
    withLimit.add(new WealthEvidence("annual.branch.clash.synthetic_extra", EvidenceFamily.ANNUAL_TRIGGER,
        "合成新增限制", "非真实命盘关系，仅检查单调性"));
    var evaluator = new WealthPathEvaluator();
    var before = evaluator.evaluatePaths(original);
    var after = evaluator.evaluatePaths(withAnnualEvidence(original, withLimit));
    Map<String, Integer> legacyLevels = Map.of("重点", 3, "可以作为补充", 2, "表现一般", 1, "需要谨慎", 0);
    for (var path : WealthPath.values()) {
      assertEquals(supportWeight(before.get(path)), supportWeight(after.get(path)));
      assertTrue(after.get(path).score() <= before.get(path).score());
      assertTrue(legacyLevels.get(after.get(path).status()) <= legacyLevels.get(before.get(path).status()));
    }
    assertEquals(before.get(WealthPath.RETENTION).limitationWeight() + 2,
        after.get(WealthPath.RETENTION).limitationWeight());
  }

  @Test
  void readoutProjectsActualOutputWithoutRepairingIt() throws Exception {
    var period = fullBirthCase("R01");
    var actual = new WealthNarrativePlanner().plan(period);
    var observed = renderLegacy(period);
    assertEquals(actual.thesis(), observed.thesis());
    assertEquals(actual.mainRisk().judgment(), observed.riskSummary());
    for (int i = 0; i < actual.years().size(); i++) {
      var copy = actual.years().get(i);
      var view = observed.years().get(i);
      assertEquals(List.of(period.years().get(i).primaryIncomePath()), view.primaryCandidates());
      assertEquals(List.of(period.years().get(i).secondaryIncomePath()), view.secondaryCandidates());
      assertEquals(period.years().get(i).mainRisk(), view.riskPath());
      assertEquals(copy.focus(), view.focus());
      assertEquals(copy.incomeSource(), view.incomeSource());
      assertEquals(copy.retention(), view.retention());
      assertEquals(copy.mainLimit(), view.mainLimit());
      assertEquals(copy.realitySignals(), view.realitySignals());
      assertEquals(copy.actions(), view.actions());
      assertEquals(JSON.valueToTree(copy.transition()), view.comparison());
      assertEquals(copy.evidenceKeys(), view.evidenceKeys());
    }
  }

  private int supportWeight(WealthPathEvaluation path) {
    return path.support().stream().mapToInt(WealthPathEvaluation.ScoredEvidence::weight).sum();
  }
}
