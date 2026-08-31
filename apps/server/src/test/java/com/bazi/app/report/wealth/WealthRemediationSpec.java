package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.report.wealth.v3.WealthNarrativeWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Opt-in acceptance specifications for tasks 4–6, not legacy compatibility expectations.
 * Tasks 4–6 selections/strength/comparison/copy all call actual v3. Legacy compatibility stays separate.
 * Run explicitly: mvn -o -Dtest=WealthRemediationSpec test
 * The Spec suffix deliberately keeps unfinished v3 criteria outside default *Test discovery.
 * Do not disable assertions, assert that failures are expected, or rewrite v2 snapshots to pass.
 */
class WealthRemediationSpec {
  @Test
  @DisplayName("RISK-01 无负向依据时不选择特定风险")
  void noNegativeEvidenceDoesNotSelectARisk() throws Exception {
    WealthPeriodEvaluation period = scoredCase("S05");
    WealthYearEvaluation year = period.years().get(0);
    assertTrue(year.paths().values().stream().allMatch(p -> p.limitations().isEmpty()),
        "fixture must contain no negative evidence");
    assertNull(assessScored(year).risk(),
        "RISK-01: zero negative evidence must not create a risk direction");
  }

  @Test
  @DisplayName("RISK-02 三年均无负向时不生成风险摘要")
  void noNegativeEvidenceDoesNotCreateRiskSummary() throws Exception {
    assertNull(render(scoredCase("S05")).riskSummary(),
        "RISK-02: generic money advice must not become a chart-specific risk summary");
  }

  @Test
  @DisplayName("RISK-03 无负向年度不生成具体风险正文")
  void noNegativeEvidenceDoesNotCreateAnnualRiskCopy() throws Exception {
    assertNull(render(scoredCase("S05")).years().get(0).mainLimit(),
        "RISK-03: annual risk must be absent rather than invented or filled with generic advice");
  }

  @Test
  @DisplayName("RISK-04 有负向时保留真实风险，不可一律删除")
  void negativeEvidenceStillProducesAGroundedRisk() throws Exception {
    var period = scoredCase("S07");
    var reading = render(period);
    var year = reading.years().get(0);
    assertNotNull(year.riskPath());
    assertFalse(period.years().get(0).paths().get(year.riskPath()).limitations().isEmpty());
    assertNotNull(year.mainLimit());
    assertFalse(year.mainLimit().isBlank());
    assertNotNull(reading.riskSummary());
  }

  @ParameterizedTest(name = "FOCUS-01 无明确收入候选：{0}")
  @ValueSource(strings = {"S01", "S02", "S08"})
  void zeroWeakOrNegativePathsDoNotForceAWinner(String id) throws Exception {
    assertTrue(assessScored(scoredCase(id).years().get(0)).focus().primaryCandidates().isEmpty(),
        "FOCUS-01: " + id + " must not turn relative first place into an income opportunity");
  }

  @Test
  @DisplayName("FOCUS-02 并列支持不能用固定顺序拆成唯一主辅")
  void tiedCandidatesRemainPeers() throws Exception {
    var focus = assessScored(scoredCase("S03").years().get(0)).focus();
    assertEquals(Set.of(WealthPath.STABLE_INCOME.code(), WealthPath.SKILL_INCOME.code()),
        Set.copyOf(focus.primaryCandidates()), "FOCUS-02: keep all equally supported primary candidates");
    assertTrue(focus.secondaryCandidates().isEmpty(), "FOCUS-02: tied peers must not be demoted to secondary");
  }

  @Test
  @DisplayName("FOCUS-03 没有合格辅助方向时允许留空")
  void weakRunnerUpIsNotForcedIntoSecondaryDirection() throws Exception {
    WealthPeriodEvaluation period = withPathWeights(scoredCase("S02"), WealthPath.STABLE_INCOME, 6, 0);
    assertTrue(assessScored(period.years().get(0)).focus().secondaryCandidates().isEmpty(),
        "FOCUS-03: a one-point runner-up is not a qualified secondary direction");
  }

  @Test
  @DisplayName("FOCUS-04 合格主辅方向仍应保留，不可一律置空")
  void qualifiedPrimaryAndSecondaryDirectionsRemainAvailable() throws Exception {
    var period = withPathWeights(scoredCase("S02"), WealthPath.STABLE_INCOME, 8, 0);
    period = withPathWeights(period, WealthPath.SKILL_INCOME, 4, 0);
    var focus = assessScored(period.years().get(0)).focus();
    assertEquals(List.of(WealthPath.STABLE_INCOME.code()), focus.primaryCandidates());
    assertEquals(List.of(WealthPath.SKILL_INCOME.code()), focus.secondaryCandidates());
  }

  @ParameterizedTest(name = "COPY-01 弱支持或全受限不强断收入来源：{0}")
  @ValueSource(strings = {"S02", "S08"})
  void weakOrNegativeIncomeCopyDoesNotMakeAStrongClaim(String id) throws Exception {
    String text = render(scoredCase(id)).years().get(0).incomeSource();
    assertFalse(text.contains("主要来自"), "COPY-01: unsupported strong income claim: " + text);
  }

  @Test
  @DisplayName("MIXED-01 增加反向依据后不能沿用单面有利正文")
  void mixedIncomeDoesNotReuseUnqualifiedPositiveCopy() throws Exception {
    WealthPeriodEvaluation positive = withPathWeights(scoredCase("S02"), WealthPath.STABLE_INCOME, 8, 0);
    WealthPeriodEvaluation mixed = withPathWeights(positive, WealthPath.STABLE_INCOME, 8, 1);
    assertEquals(positive.years().get(0).primaryIncomePath(), mixed.years().get(0).primaryIncomePath(),
        "fixture must preserve the selected direction");
    assertNotEquals(render(positive).years().get(0).incomeSource(), render(mixed).years().get(0).incomeSource(),
        "MIXED-01: the selected income reading must retain its limiting condition");
  }

  @Test
  @DisplayName("MIXED-02 主方向限制不是最大风险时也必须保留依据")
  void selectedIncomeRetainsItsLimitEvenWhenAnotherRiskIsLarger() throws Exception {
    WealthPeriodEvaluation period = withPathWeights(scoredCase("S02"), WealthPath.STABLE_INCOME, 8, 1);
    period = withPathWeights(period, WealthPath.COOPERATION_INCOME, 1, 3);
    var year = period.years().get(0);
    assertEquals(WealthPath.STABLE_INCOME, year.primaryIncomePath(), "fixture income must remain stable");
    assertEquals(WealthPath.COOPERATION_INCOME, year.mainRisk(), "fixture needs a different, larger risk");
    String limitKey = year.paths().get(WealthPath.STABLE_INCOME).limitations().get(0).evidence().key();
    assertTrue(render(period).years().get(0).evidenceKeys().contains(limitKey),
        "MIXED-02: selected income limitation was dropped from the reading: " + limitKey);
  }

  @Test
  @DisplayName("SOURCE-01 同源原局组合不能冒充第二个独立来源升级表达")
  void derivedNatalCombinationDoesNotCreateIndependentSupport() throws Exception {
    WealthYearFacts base = natalOnly(false);
    WealthYearFacts withDerived = natalOnly(true);
    assertFalse(permitsStrongExpression(base, WealthPath.SKILL_INCOME),
        "fixture without derived combination must not already have strong expression");
    assertFalse(permitsStrongExpression(withDerived, WealthPath.SKILL_INCOME),
        "SOURCE-01: NATAL_STRUCTURE plus its NATAL_COMBINATION are still one origin group");
  }

  @Test
  @DisplayName("SOURCE-02 原局和独立年度支持充分时仍允许较突出表达")
  void independentNatalAndAnnualSupportMayPermitStrongerExpression() throws Exception {
    assertTrue(permitsStrongExpression(syntheticAnnualFacts(2026, "食神", null), WealthPath.SKILL_INCOME),
        "SOURCE-02: do not suppress every strong expression merely to pass the same-origin case");
  }

  @ParameterizedTest(name = "YEAR-01 同一年度不受报告年序影响：{0}")
  @ValueSource(strings = {"focus", "incomeSource", "retention", "mainLimit", "realitySignals", "actions"})
  void sameYearReadingDoesNotDependOnPositionInReport(String field) throws Exception {
    WealthPeriodEvaluation original = fullBirthCase("R01");
    WealthPeriodEvaluation shifted = fullBirthCase("Y27");
    assertEquals(original.years().get(1), shifted.years().get(0), "same-year calculation precondition");
    assertEquals(JSON.valueToTree(renderAssessments(fullBirthAssessments("R01")).years().get(1)).required(field),
        JSON.valueToTree(renderAssessments(fullBirthAssessments("Y27")).years().get(0)).required(field),
        "YEAR-01: 2027 " + field + " must not depend on being first or second in a report");
  }

  @Test
  @DisplayName("YEAR-02 相邻年依据相同时不制造不同年度剧情")
  void unchangedAnnualEvidenceDoesNotForceDifferentFocus() throws Exception {
    var content = new WealthNarrativeWriter().plan(
        WealthNarrativeV3Test.assessments(scoredCase("S07")), LocalDate.of(2026, 8, 29));
    List<String> headlines = content.years().stream().map(year -> year.overview().text()).toList();
    assertEquals(3, Set.copyOf(headlines).size(), "report-level headings should use different angles");
    assertTrue(content.years().stream().limit(2)
        .allMatch(year -> year.comparison() != null && year.comparison().direction().equals("unchanged")),
        "YEAR-02: identical scored evidence must still be described as unchanged");
    assertTrue(headlines.stream().noneMatch(text -> text.contains("比上年")
        || text.contains("逐年") || text.contains("越来越")),
        "YEAR-02: different angles must not be presented as a made-up progression: " + headlines);
  }

  @Test
  @DisplayName("YEAR-03 净分相同但来源改变时比较结果必须反映差异")
  void equalScoresWithDifferentSourcesChangeAnnualComparison() throws Exception {
    WealthPeriodEvaluation base = scoredCase("S07");
    WealthPeriodEvaluation changed = sourceChanged(base);
    var original = compareScored(base).get(0).comparison();
    var actual = compareScored(changed).get(0).comparison();
    assertEquals("unchanged", original.direction());
    assertNotEquals(original, actual,
        "YEAR-03: a changed source cannot produce the same comparison as unchanged evidence");
    var difference = WealthAnnualComparatorTest.change(actual, WealthPath.STABLE_INCOME.code());
    assertEquals(0, difference.supportDelta());
    assertEquals(0, difference.limitationDelta());
    assertFalse(difference.addedEvidenceIds().isEmpty());
    assertFalse(difference.removedEvidenceIds().isEmpty());
  }

  @Test
  @DisplayName("YEAR-04 同一依据键的事实值改变也必须参与年度比较")
  void equalKeysAndScoresWithChangedFactValuesChangeComparison() throws Exception {
    var base = compareFacts(factValueFacts(false));
    var changed = compareFacts(factValueFacts(true));
    assertEquals("unchanged", base.get(0).comparison().direction());
    assertNotEquals(base.get(0).comparison(), changed.get(0).comparison(),
        "YEAR-04: comparison must retain changed fact values, not just evidence keys and net scores");
    var difference = WealthAnnualComparatorTest.change(changed.get(0).comparison(), WealthPath.SKILL_INCOME.code());
    assertEquals(0, difference.supportDelta());
    assertEquals(0, difference.limitationDelta());
    assertTrue(difference.addedEvidenceIds().stream().anyMatch(id -> id.endsWith("annual.stem.ten_god")));
    assertTrue(changed.get(1).assessment().facts().stream()
        .anyMatch(f -> f.code().equals("annual.stem.ten_god") && f.value().equals("伤官")));
  }

  @Test
  @DisplayName("YEAR-05 支持和限制同时增加但净分不变也算变化")
  void equalNetWithChangedSupportAndLimitChangesComparison() throws Exception {
    WealthPeriodEvaluation base = scoredCase("S07");
    WealthPeriodEvaluation changed = grossWeightsChanged(base);
    var original = compareScored(base).get(0).comparison();
    var actual = compareScored(changed).get(0).comparison();
    assertEquals("unchanged", original.direction());
    assertNotEquals(original, actual,
        "YEAR-05: +1 support and +1 limitation must not disappear behind an unchanged net score");
    var difference = WealthAnnualComparatorTest.change(actual, WealthPath.STABLE_INCOME.code());
    assertEquals(1, difference.supportDelta());
    assertEquals(1, difference.limitationDelta());
  }

  @Test
  @DisplayName("YEAR-06 相同输入重复生成的阅读内容必须一致")
  void identicalInputsProduceIdenticalReadings() throws Exception {
    for (var input : List.of(scoredCase("S07"), sourceChanged(scoredCase("S07")))) {
      assertEquals(render(input), render(input), "YEAR-06: no random wording or clock-based content drift");
    }
    var facts = factValueFacts(true).stream().map(new com.bazi.app.report.wealth.v3.WealthV3Analyzer()::assess).toList();
    assertEquals(renderAssessments(facts), renderAssessments(facts));
    var birth = fullBirthAssessments("R01");
    assertEquals(renderAssessments(birth), renderAssessments(birth));
  }

  @Test
  @DisplayName("ROOT-01 三年总论必须响应不同的主要收入方向")
  void thesisIsNotTheSameFixedPlotForDifferentIncomeDirections() throws Exception {
    WealthPeriodEvaluation stable = withPathWeights(scoredCase("S02"), WealthPath.STABLE_INCOME, 8, 0);
    WealthPeriodEvaluation skill = withPathWeights(scoredCase("S02"), WealthPath.SKILL_INCOME, 8, 0);
    assertNotEquals(render(stable).thesis(), render(skill).thesis(),
        "ROOT-01: a personalized thesis cannot be the same generic three-stage plot for both directions");
  }
}
