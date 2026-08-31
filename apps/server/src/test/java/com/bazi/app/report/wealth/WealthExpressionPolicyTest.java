package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.*;

import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthAssessment.Evidence;
import com.bazi.app.report.wealth.v3.WealthAssessment.Fact;
import com.bazi.app.report.wealth.v3.WealthAssessmentInput;
import com.bazi.app.report.wealth.v3.WealthExpressionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WealthExpressionPolicyTest {
  private static final String STABLE = "stable_income";
  private static final String SKILL = "skill_income";
  private static final List<Fact> ROOTS = List.of(
      new Fact("n", "natal", "natal.occurrence", "正财"),
      new Fact("a", "annual", "annual.stem", "正财"),
      new Fact("d", "dayun", "dayun.stem", "正财"));
  private final WealthExpressionPolicy policy = new WealthExpressionPolicy();

  @Test
  void evaluatesAllFivePathsWithoutInventingFocusOrRisk() {
    var result = policy.assess(input(List.of()));
    assertEquals(5, result.decisions().size());
    assertEquals("none", result.focus().state());
    assertTrue(result.focus().primaryCandidates().isEmpty());
    assertTrue(result.focus().secondaryCandidates().isEmpty());
    assertNull(result.risk());
    for (var d : result.decisions()) {
      assertEquals("quiet", d.stance());
      assertEquals("none", d.strength());
      assertEquals(List.of("no_evidence"), d.reasonCodes());
    }
  }

  @ParameterizedTest
  @CsvSource({"1,none,limited", "2,none,limited", "3,leading,supported", "5,leading,supported", "6,leading,supported"})
  void aRelativeWinnerNeedsTheThresholdAndSingleOriginDoesNotBecomePronounced(
      int support, String focus, String strength) {
    var result = policy.assess(input(List.of(e("s", STABLE, support, EvidenceFamily.ANNUAL_TRIGGER, "a"))));
    assertEquals(focus, result.focus().state());
    assertEquals(strength, decision(result, STABLE).strength());
    assertTrue(result.focus().secondaryCandidates().isEmpty());
  }

  @Test
  void keepsPrimaryTiesInsteadOfUsingPathOrderAsSuperiority() {
    var result = policy.assess(input(List.of(
        e("s", STABLE, 6, EvidenceFamily.ANNUAL_TRIGGER, "a"),
        e("k", SKILL, 6, EvidenceFamily.ANNUAL_TRIGGER, "a"))));
    assertEquals("tied", result.focus().state());
    assertEquals(List.of(STABLE, SKILL), result.focus().primaryCandidates());
    assertTrue(result.focus().secondaryCandidates().isEmpty());
  }

  @Test
  void keepsAllQualifiedSecondaryTiesButNeverRanksRetentionAsIncome() {
    var result = policy.assess(input(List.of(
        e("s", STABLE, 8, EvidenceFamily.ANNUAL_TRIGGER, "a"),
        e("k", SKILL, 3, EvidenceFamily.ANNUAL_TRIGGER, "a"),
        e("p", "project_income", 3, EvidenceFamily.ANNUAL_TRIGGER, "a"),
        e("c", "cooperation_income", 1, EvidenceFamily.ANNUAL_TRIGGER, "a"),
        e("r", "retention", 20, EvidenceFamily.ANNUAL_TRIGGER, "a"))));
    assertEquals(List.of(STABLE), result.focus().primaryCandidates());
    assertEquals(List.of(SKILL, "project_income"), result.focus().secondaryCandidates());
    assertEquals(20, decision(result, "retention").supportWeight());
  }

  @Test
  void independentPositiveSourcesPermitPronouncedButNotCertainExpression() {
    var result = policy.assess(input(List.of(
        e("s1", STABLE, 3, EvidenceFamily.NATAL_STRUCTURE, "n"),
        e("s2", STABLE, 3, EvidenceFamily.ANNUAL_TRIGGER, "a"))));
    var decision = decision(result, STABLE);
    assertEquals("supportive", decision.stance());
    assertEquals("pronounced", decision.strength());
    assertEquals(6, decision.netWeight());
  }

  @Test
  void twoNatalFamiliesAreStillOneOrigin() {
    var result = policy.assess(input(List.of(
        e("s1", STABLE, 3, EvidenceFamily.NATAL_STRUCTURE, "n"),
        e("s2", STABLE, 3, EvidenceFamily.NATAL_COMBINATION, "n"))));
    assertEquals("supported", decision(result, STABLE).strength());
    assertTrue(decision(result, STABLE).reasonCodes().contains("single_origin"));
    assertEquals(6, decision(result, STABLE).netWeight(), "do not change the raw weights");
  }

  @Test
  void differentFamiliesWithOverlappingRootFactsAreNotIndependent() {
    var result = policy.assess(input(List.of(
        e("s1", STABLE, 3, EvidenceFamily.ANNUAL_TRIGGER, "a", "n"),
        e("s2", STABLE, 3, EvidenceFamily.DAYUN_CONTEXT, "a", "d"))));
    assertEquals("supported", decision(result, STABLE).strength());
  }

  @Test
  void renamingARootIdCannotHideSharedAncestry() {
    var roots = new ArrayList<>(ROOTS);
    roots.add(new Fact("a-alias", "annual", "annual.stem", "正财"));
    var result = policy.assess(new WealthAssessmentInput(2026, "丙午", roots, List.of(
        e("s1", STABLE, 3, EvidenceFamily.ANNUAL_TRIGGER, "a", "n"),
        e("s2", STABLE, 3, EvidenceFamily.DAYUN_CONTEXT, "a-alias", "d")), Set.of()));
    assertEquals("supported", decision(result, STABLE).strength());
  }

  @Test
  void oneCompositeEvidenceDoesNotCountAsTwoIndependentSupports() {
    var result = policy.assess(input(List.of(
        e("s1", STABLE, 8, EvidenceFamily.ANNUAL_TRIGGER, "a", "n"))));
    assertEquals("supported", decision(result, STABLE).strength());
  }

  @ParameterizedTest
  @CsvSource({"8,1,7,leading", "8,8,0,none", "2,8,-6,none"})
  void mixedConditionsKeepBothSidesRegardlessOfNet(int support, int limit, int net, String focus) {
    var result = policy.assess(input(List.of(
        e("s", STABLE, support, EvidenceFamily.ANNUAL_TRIGGER, "a"),
        e("l", STABLE, -limit, EvidenceFamily.DAYUN_CONTEXT, "d"))));
    var d = decision(result, STABLE);
    assertEquals("mixed", d.stance());
    assertEquals("limited", d.strength());
    assertEquals(support, d.supportWeight());
    assertEquals(limit, d.limitationWeight());
    assertEquals(net, d.netWeight());
    assertEquals(List.of("s"), d.supportingEvidenceIds());
    assertEquals(List.of("l"), d.limitingEvidenceIds());
    assertEquals(focus, result.focus().state());
    assertNotNull(result.risk());
    assertEquals(List.of("l"), result.risk().limitingEvidenceIds());
  }

  @Test
  void riskUsesNegativeEvidenceAndPreservesOtherLimitations() {
    var result = policy.assess(input(List.of(
        e("s", STABLE, 8, EvidenceFamily.ANNUAL_TRIGGER, "a"),
        e("l", STABLE, -1, EvidenceFamily.DAYUN_CONTEXT, "d"),
        e("r", "cooperation_income", -3, EvidenceFamily.DAYUN_CONTEXT, "d"))));
    assertNotNull(result.risk());
    assertEquals("cooperation_income", result.risk().path());
    assertEquals(List.of("r"), result.risk().limitingEvidenceIds());
    assertEquals(List.of("l"), decision(result, STABLE).limitingEvidenceIds());
  }

  @Test
  void allNegativePathsNeverBecomeAnIncomeOpportunity() {
    var result = policy.assess(input(List.of(
        e("s", STABLE, -2, EvidenceFamily.DAYUN_CONTEXT, "d"),
        e("k", SKILL, -2, EvidenceFamily.DAYUN_CONTEXT, "d"))));
    assertEquals("none", result.focus().state());
    assertNotNull(result.risk());
    assertEquals(STABLE, result.risk().path(), "equal risks use display order only");
    assertEquals("restricted", decision(result, STABLE).stance());
    assertEquals("limited", decision(result, STABLE).strength());
  }

  @Test
  void unresolvedProvenanceCapsExpressionAndDisqualifiesIncomeFocus() {
    var result = policy.assess(new WealthAssessmentInput(2026, "丙午", ROOTS, List.of(
        e("s1", STABLE, 3, EvidenceFamily.NATAL_STRUCTURE, "n"),
        e("s2", STABLE, 3, EvidenceFamily.ANNUAL_TRIGGER, "a")), Set.of("s2")));
    assertEquals("limited", decision(result, STABLE).strength());
    assertTrue(decision(result, STABLE).reasonCodes().contains("legacy_provenance_unresolved"));
    assertEquals("none", result.focus().state());
    assertEquals(6, decision(result, STABLE).netWeight());
  }

  @Test
  void aFamilyLabelWithoutThatOriginInTheRootsCannotUpgradeExpression() {
    var result = policy.assess(input(List.of(
        e("s1", STABLE, 3, EvidenceFamily.NATAL_STRUCTURE, "n"),
        e("s2", STABLE, 3, EvidenceFamily.ANNUAL_TRIGGER, "n"))));
    assertEquals("limited", decision(result, STABLE).strength());
    assertTrue(decision(result, STABLE).reasonCodes().contains("legacy_provenance_unresolved"));
  }

  @Test
  void rejectsDanglingReferencesAndDuplicateRulesInsteadOfSilentlyDroppingWeights() {
    assertThrows(IllegalArgumentException.class, () -> policy.assess(input(List.of(
        e("s", STABLE, 3, EvidenceFamily.ANNUAL_TRIGGER, "missing")))));
    var first = e("s", STABLE, 3, EvidenceFamily.ANNUAL_TRIGGER, "a");
    var duplicateRule = new Evidence("renamed", first.path(), first.ruleKey(), first.factKey(),
        first.family(), first.rootFactIds(), first.weight());
    assertThrows(IllegalArgumentException.class, () -> policy.assess(input(List.of(first, duplicateRule))));
  }

  @Test
  void addingNegativeEvidenceCannotIncreaseExpressionStrength() {
    var before = List.of(e("s1", STABLE, 3, EvidenceFamily.NATAL_STRUCTURE, "n"),
        e("s2", STABLE, 3, EvidenceFamily.ANNUAL_TRIGGER, "a"));
    assertEquals("pronounced", decision(policy.assess(input(before)), STABLE).strength());
    for (int limit = 1; limit <= 8; limit++) {
      var after = new ArrayList<>(before);
      after.add(e("l", STABLE, -limit, EvidenceFamily.DAYUN_CONTEXT, "d"));
      var decision = decision(policy.assess(input(after)), STABLE);
      assertEquals("limited", decision.strength());
      assertEquals(6, decision.supportWeight());
      assertEquals(limit, decision.limitationWeight());
    }
  }

  @Test
  void orderingAndRepeatedCallsDoNotChangeJudgments() {
    var evidence = List.of(e("z", STABLE, 3, EvidenceFamily.NATAL_STRUCTURE, "n"),
        e("annual-support", STABLE, 3, EvidenceFamily.ANNUAL_TRIGGER, "a"));
    var reversed = new ArrayList<>(evidence);
    Collections.reverse(reversed);
    assertEquals(policy.assess(input(evidence)), policy.assess(input(reversed)));
  }

  static Decision decision(WealthAssessment result, String path) {
    var decision = result.decisions().stream().filter(d -> d.path().equals(path)).findFirst().orElse(null);
    assertNotNull(decision, "missing assessment for " + path);
    return decision;
  }

  private static WealthAssessmentInput input(List<Evidence> evidence) {
    return new WealthAssessmentInput(2026, "丙午", ROOTS, evidence, Set.of());
  }

  private static Evidence e(String id, String path, int weight, EvidenceFamily family, String... roots) {
    return new Evidence(id, path, "rule." + id, "fact." + id, family, List.of(roots), weight);
  }
}
