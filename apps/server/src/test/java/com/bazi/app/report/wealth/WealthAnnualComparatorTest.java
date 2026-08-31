package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.*;

import com.bazi.app.report.wealth.v3.WealthAnnualComparator;
import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthAssessment.Evidence;
import com.bazi.app.report.wealth.v3.WealthAssessment.Fact;
import com.bazi.app.report.wealth.v3.WealthAssessment.Focus;
import com.bazi.app.report.wealth.v3.WealthAssessmentInput;
import com.bazi.app.report.wealth.v3.WealthComparison;
import com.bazi.app.report.wealth.v3.WealthExpressionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Explicit synthetic provenance tests, not complete birth charts or real-world outcomes. */
class WealthAnnualComparatorTest {
  private static final String STABLE = "stable_income";
  private static final String SKILL = "skill_income";
  private final WealthAnnualComparator comparator = new WealthAnnualComparator();

  @Test
  void unchangedConditionsDoNotChangeBecauseOfYearOrRecordIds() {
    var result = comparator.compare(year(2026, e(2026, "income", STABLE, 4)),
        year(2027, e(2027, "income", STABLE, 4)));
    assertEquals(2027, result.toYear());
    assertEquals("unchanged", result.direction());
    assertTrue(result.changes().isEmpty());
  }

  @Test
  void renamingRootAndEvidenceIdsDoesNotCreateAChange() {
    var before = year(2026, e(2026, "income", STABLE, 4));
    var original = e(2027, "income", STABLE, 4);
    var renamed = new Evidence("renamed-evidence", original.path(), original.ruleKey(), original.factKey(),
        original.family(), List.of("renamed-root"), original.weight());
    var after = assess(2027, List.of(new Fact("renamed-root", "annual", "annual.stem", "正财")),
        List.of(renamed), Set.of());
    assertEquals("unchanged", comparator.compare(before, after).direction());
  }

  @Test
  void sameWeightButDifferentSourceRetainsBothYearReferences() {
    var result = comparator.compare(year(2026, e(2026, "old", STABLE, 4)),
        year(2027, e(2027, "new", STABLE, 4)));
    var change = change(result, STABLE);
    assertEquals(0, change.supportDelta());
    assertEquals(0, change.limitationDelta());
    assertEquals(List.of("2027.evidence.new"), change.addedEvidenceIds());
    assertEquals(List.of("2026.evidence.old"), change.removedEvidenceIds());
  }

  @Test
  void sameRuleAndWeightButChangedFactValueIsNotLost() {
    var afterRoots = List.of(new Fact("2027.root.annual", "annual", "annual.stem", "偏财"));
    var result = comparator.compare(year(2026, e(2026, "income", STABLE, 4)),
        assess(2027, afterRoots, List.of(e(2027, "income", STABLE, 4)), Set.of()));
    assertEquals(List.of("2027.evidence.income"), change(result, STABLE).addedEvidenceIds());
    assertEquals(List.of("2026.evidence.income"), change(result, STABLE).removedEvidenceIds());
    assertEquals(0, change(result, STABLE).supportDelta());
  }

  @Test
  void aChangedRootOnlyAffectsPathsThatActuallyReferenceIt() {
    var before = year(2026, e(2026, "income", STABLE, 4),
        e(2026, "skill", SKILL, 3, EvidenceFamily.NATAL_STRUCTURE, "natal"));
    var after = assess(2027, List.of(new Fact("2027.root.annual", "annual", "annual.stem", "偏财"),
        new Fact("2027.root.natal", "natal", "natal.stem", "正财")), List.of(
        e(2027, "income", STABLE, 4), e(2027, "skill", SKILL, 3, EvidenceFamily.NATAL_STRUCTURE, "natal")), Set.of());
    assertEquals(List.of(STABLE), comparator.compare(before, after).changes().stream().map(c -> c.path()).toList());
  }

  @Test
  void changingUnusedFactsDoesNotInventAnIncomeChange() {
    var roots = new ArrayList<>(roots(2027));
    roots.add(new Fact("unused", "annual", "annual.unused", "未参与任何计分的测试值"));
    var result = comparator.compare(year(2026, e(2026, "income", STABLE, 4)),
        assess(2027, roots, List.of(e(2027, "income", STABLE, 4)), Set.of()));
    assertEquals("unchanged", result.direction());
  }

  @Test
  void changedRuleIsComparedEvenWhenTheSourceAndWeightStayTheSame() {
    var original = e(2027, "income", STABLE, 4);
    var otherRule = new Evidence(original.id(), STABLE, "rule.another", original.factKey(),
        original.family(), original.rootFactIds(), original.weight());
    assertEquals("changed", comparator.compare(year(2026, e(2026, "income", STABLE, 4)),
        year(2027, otherRule)).direction());
  }

  @Test
  void changedFamilyIsComparedWithoutRelyingOnTheRootIds() {
    var before = e(2026, "income", STABLE, 4, EvidenceFamily.ANNUAL_TRIGGER, "annual", "dayun");
    var after = e(2027, "income", STABLE, 4, EvidenceFamily.DAYUN_CONTEXT, "annual", "dayun");
    assertEquals("changed", comparator.compare(year(2026, before), year(2027, after)).direction());
  }

  @Test
  void changedRootRelationshipIsComparedEvenIfFactValuesAndWeightsMatch() {
    var before = e(2026, "income", STABLE, 4, EvidenceFamily.ANNUAL_TRIGGER, "annual", "natal");
    var after = e(2027, "income", STABLE, 4, EvidenceFamily.ANNUAL_TRIGGER, "annual", "dayun");
    assertEquals("changed", comparator.compare(year(2026, before), year(2027, after)).direction());
  }

  @Test
  void supportAndLimitationIncreasesAreNotHiddenByAnEqualNet() {
    var before = year(2026, e(2026, "income", STABLE, 4), e(2026, "limit", STABLE, -1));
    var after = year(2027, e(2027, "income", STABLE, 5), e(2027, "limit", STABLE, -2));
    var change = change(comparator.compare(before, after), STABLE);
    assertEquals(1, change.supportDelta());
    assertEquals(1, change.limitationDelta());
    assertEquals(List.of("2027.evidence.income", "2027.evidence.limit"), change.addedEvidenceIds());
    assertEquals(List.of("2026.evidence.income", "2026.evidence.limit"), change.removedEvidenceIds());
  }

  @Test
  void removedEvidenceProducesSignedDeltasAndNoFabricatedNextYearReference() {
    var result = comparator.compare(year(2026, e(2026, "income", STABLE, 4), e(2026, "limit", STABLE, -2)),
        year(2027, e(2027, "income", STABLE, 4)));
    var change = change(result, STABLE);
    assertEquals(0, change.supportDelta());
    assertEquals(-2, change.limitationDelta());
    assertTrue(change.addedEvidenceIds().isEmpty());
    assertEquals(List.of("2026.evidence.limit"), change.removedEvidenceIds());
  }

  @Test
  void aSignChangePreservesSupportAndLimitationSeparately() {
    var result = comparator.compare(year(2026, e(2026, "income", STABLE, 3)),
        year(2027, e(2027, "income", STABLE, -3)));
    assertEquals(-3, change(result, STABLE).supportDelta());
    assertEquals(3, change(result, STABLE).limitationDelta());
  }

  @Test
  void aFormerLeaderBecomingTiedIsRecordedEvenWithoutItsOwnScoreChange() {
    var before = year(2026, e(2026, "income", STABLE, 4), e(2026, "skill", SKILL, 3));
    var after = year(2027, e(2027, "income", STABLE, 4), e(2027, "skill", SKILL, 4));
    assertEquals("leading", before.focus().state());
    assertEquals("tied", after.focus().state());
    var result = comparator.compare(before, after);
    assertEquals(List.of(STABLE, SKILL), result.changes().stream().map(c -> c.path()).toList());
    var leader = change(result, STABLE);
    assertEquals(0, leader.supportDelta());
    assertEquals(0, leader.limitationDelta());
    assertTrue(leader.addedEvidenceIds().isEmpty());
    assertTrue(leader.removedEvidenceIds().isEmpty());
  }

  @Test
  void sourceResolutionChangesAreNotMistakenForUnchangedExpressionPermission() {
    var before = assess(2026, roots(2026), List.of(e(2026, "income", STABLE, 4)), Set.of("2026.evidence.income"));
    var after = year(2027, e(2027, "income", STABLE, 4));
    assertEquals("limited", WealthExpressionPolicyTest.decision(before, STABLE).strength());
    assertEquals("supported", WealthExpressionPolicyTest.decision(after, STABLE).strength());
    var result = comparator.compare(before, after);
    assertEquals(0, change(result, STABLE).supportDelta());
    assertTrue(change(result, STABLE).addedEvidenceIds().isEmpty());
    assertTrue(change(result, STABLE).removedEvidenceIds().isEmpty());
  }

  @Test
  void collectionOrderIncludingTiedCandidatesAndRootReferencesIsNotAChange() {
    var before = year(2026, e(2026, "income", STABLE, 4, EvidenceFamily.ANNUAL_TRIGGER, "annual", "natal"),
        e(2026, "skill", SKILL, 4));
    var next = year(2027, e(2027, "income", STABLE, 4, EvidenceFamily.ANNUAL_TRIGGER, "natal", "annual"),
        e(2027, "skill", SKILL, 4));
    var reordered = new WealthAssessment(next.year(), next.ganZhi(), reversed(next.facts()), reversed(next.evidence()),
        reversed(next.decisions()), new Focus("tied", reversed(next.focus().primaryCandidates()), List.of()), next.risk());
    assertEquals(comparator.compare(before, next), comparator.compare(before, reordered));
    assertEquals("unchanged", comparator.compare(before, reordered).direction());
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 5})
  void periodsKeepEveryAssessmentAndOnlyTheFinalComparisonIsNull(int count) {
    var input = period(2026, count);
    var result = comparator.comparePeriod(input);
    assertEquals(count, result.size());
    for (int i = 0; i < count; i++) {
      assertEquals(input.get(i), result.get(i).assessment());
      if (i == count - 1) assertNull(result.get(i).comparison());
      else assertEquals(comparator.compare(input.get(i), input.get(i + 1)), result.get(i).comparison());
    }
  }

  @Test
  void theSamePairDoesNotDependOnReportPositionOrHorizon() {
    var full = comparator.comparePeriod(period(2026, 5));
    var shifted = comparator.comparePeriod(period(2027, 3));
    var shortPeriod = comparator.comparePeriod(period(2027, 2));
    assertFalse(full.isEmpty());
    assertEquals(full.get(1), shifted.get(0));
    assertEquals(shifted.get(0), shortPeriod.get(0));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 6})
  void invalidPeriodLengthsAreRejected(int count) {
    assertThrows(IllegalArgumentException.class, () -> comparator.comparePeriod(period(2026, count)));
  }

  @ParameterizedTest
  @ValueSource(ints = {2025, 2026, 2028})
  void comparisonRequiresTheImmediatelyFollowingYear(int nextYear) {
    assertThrows(IllegalArgumentException.class, () -> comparator.compare(year(2026), year(nextYear)));
  }

  @Test
  void danglingRootReferencesAreRejectedInsteadOfBeingTreatedAsNoChange() {
    var valid = year(2026, e(2026, "income", STABLE, 4));
    var invalid = new WealthAssessment(valid.year(), valid.ganZhi(), List.of(), valid.evidence(), valid.decisions(), valid.focus(), valid.risk());
    assertThrows(IllegalArgumentException.class, () -> comparator.compare(invalid, year(2027)));
  }

  @Test
  void duplicateEvidenceIdsAreRejectedInsteadOfSilentlyDiscarded() {
    var valid = year(2026, e(2026, "income", STABLE, 4));
    var invalid = new WealthAssessment(valid.year(), valid.ganZhi(), valid.facts(),
        List.of(valid.evidence().get(0), valid.evidence().get(0)), valid.decisions(), valid.focus(), valid.risk());
    assertThrows(IllegalArgumentException.class, () -> comparator.compare(invalid, year(2027)));
  }

  @Test
  void missingPathJudgmentsAndMismatchedTotalsAreRejected() {
    var valid = year(2026, e(2026, "income", STABLE, 4));
    var missing = new WealthAssessment(2026, valid.ganZhi(), valid.facts(), valid.evidence(),
        valid.decisions().subList(1, 5), valid.focus(), valid.risk());
    assertThrows(IllegalArgumentException.class, () -> comparator.compare(missing, year(2027)));
    var decisions = new ArrayList<>(valid.decisions());
    var old = decisions.get(0);
    decisions.set(0, new Decision(old.id(), old.year(), old.path(), 99, 0, 99, old.stance(), old.strength(),
        old.supportingEvidenceIds(), old.limitingEvidenceIds(), old.reasonCodes()));
    var inconsistent = new WealthAssessment(2026, valid.ganZhi(), valid.facts(), valid.evidence(), decisions, valid.focus(), valid.risk());
    assertThrows(IllegalArgumentException.class, () -> comparator.compare(inconsistent, year(2027)));
  }

  static WealthComparison.Change change(WealthComparison result, String path) {
    assertEquals("changed", result.direction());
    var found = result.changes().stream().filter(c -> c.path().equals(path)).findFirst().orElse(null);
    assertNotNull(found, "missing changed path: " + path);
    return found;
  }

  private static List<WealthAssessment> period(int start, int count) {
    return IntStream.range(start, start + count).mapToObj(y -> year(y, e(y, "income", STABLE, y - 2023))).toList();
  }

  private static WealthAssessment year(int year, Evidence... evidence) {
    return assess(year, roots(year), List.of(evidence), Set.of());
  }

  private static WealthAssessment assess(int year, List<Fact> facts, List<Evidence> evidence, Set<String> unresolved) {
    String ganZhi = List.of("丙午", "丁未", "戊申", "己酉", "庚戌", "辛亥").get(Math.floorMod(year - 2026, 6));
    return new WealthExpressionPolicy().assess(new WealthAssessmentInput(year, ganZhi, facts, evidence, unresolved));
  }

  private static List<Fact> roots(int year) {
    return List.of("natal", "annual", "dayun").stream()
        .map(kind -> new Fact(year + ".root." + kind, kind, kind + ".stem", "正财")).toList();
  }

  private static Evidence e(int year, String key, String path, int weight) {
    return e(year, key, path, weight, EvidenceFamily.ANNUAL_TRIGGER, "annual");
  }

  private static Evidence e(int year, String key, String path, int weight, EvidenceFamily family, String... kinds) {
    return new Evidence(year + ".evidence." + key, path, "rule." + key, "source." + key, family,
        List.of(kinds).stream().map(k -> year + ".root." + k).toList(), weight);
  }

  private static <T> List<T> reversed(List<T> values) {
    var copy = new ArrayList<>(values);
    Collections.reverse(copy);
    return copy;
  }
}
