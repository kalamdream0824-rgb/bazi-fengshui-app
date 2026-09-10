package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthHeadlineVocabulary;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3.Block;
import com.bazi.app.report.wealth.v3.WealthNarrativeWriter;
import com.bazi.app.report.wealth.v3.WealthV3Analyzer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WealthNarrativeV3Test {
  private final WealthNarrativeWriter writer = new WealthNarrativeWriter();
  private static final LocalDate AS_OF = LocalDate.of(2026, 8, 29);

  @Test
  void noNegativeEvidenceProducesNeitherAnnualRisksNorRiskSummary() throws Exception {
    var content = content(scoredCase("S05"));
    assertNull(content.riskSummary());
    assertTrue(content.years().stream().allMatch(y -> y.risk() == null));
    assertEquals(5, content.pathSummaries().size());
  }

  @Test
  void aRiskInOneYearDoesNotBecomeAThreeYearClaim() throws Exception {
    var period = changeEvidence(scoredCase("S05"), 1, WealthPath.PROJECT_INCOME, old -> {
      var items = new ArrayList<>(old);
      items.add(scored("synthetic.risk", -1, EvidenceFamily.ANNUAL_TRIGGER, "单年合成限制"));
      return items;
    });
    var content = content(period);
    assertNotNull(content.riskSummary());
    assertEquals(List.of(2027), content.riskSummary().years());
    assertTrue(content.riskSummary().text().contains("2027"));
    assertFalse(content.riskSummary().text().contains("2026"));
    assertFalse(content.riskSummary().text().contains("2028"));
    assertNull(content.years().get(0).risk());
    assertNotNull(content.years().get(1).risk());
    assertNull(content.years().get(2).risk());
  }

  @ParameterizedTest
  @ValueSource(strings = {"S02", "S08"})
  void weakOrRestrictedIncomeNeverGetsStrongWinnerCopy(String id) throws Exception {
    var content = content(scoredCase(id));
    assertTrue(content.years().stream().allMatch(y -> y.focus().primaryCandidates().isEmpty()));
    for (var block : content.blocks()) {
      assertFalse(block.text().contains("主要来自"));
      assertFalse(block.text().contains("最旺"));
      assertFalse(block.text().contains("明显增收机会"));
    }
  }

  @Test
  void tiedIncomeCopyNamesAllPeersInTheThesisWithoutDemotingOneToAnAssistant() throws Exception {
    var content = content(scoredCase("S03"));
    var year = content.years().get(0);
    assertEquals("tied", year.focus().state());
    assertTrue(content.thesis().text().contains("进账稳定"));
    assertTrue(content.thesis().text().contains("投入回报"));
    assertFalse(content.thesis().text().contains("辅助"));
    assertEquals(Set.of("2026.decision.stable_income", "2026.decision.skill_income"),
        year.income().stream().flatMap(b -> b.decisionIds().stream()).collect(Collectors.toSet()));
  }

  @Test
  void mixedIncomeChangesItsTextAndRetainsTheNonDominantLimit() throws Exception {
    var positive = withPathWeights(scoredCase("S02"), WealthPath.STABLE_INCOME, 8, 0);
    var mixed = withPathWeights(positive, WealthPath.STABLE_INCOME, 8, 1);
    mixed = withPathWeights(mixed, WealthPath.COOPERATION_INCOME, 1, 3);
    var before = content(positive).years().get(0);
    var after = content(mixed).years().get(0);
    assertNotEquals(before.income().get(0).text(), after.income().get(0).text());
    assertEquals("cooperation_income", after.risk().path());
    var incomeIds = after.income().stream().flatMap(b -> b.decisionIds().stream()).collect(Collectors.toSet());
    var included = after.decisions().stream().filter(d -> incomeIds.contains(d.id()))
        .flatMap(d -> d.limitingEvidenceIds().stream()).toList();
    assertTrue(included.stream().anyMatch(id -> id.contains("stable_income.limit")));
  }

  @Test
  void oneAnnualIncomeParagraphIntroducesTheYearOnlyOnce() throws Exception {
    for (var year : content(scoredCase("S07")).years()) {
      for (var block : year.income()) {
        assertTrue(occurrences(block.text(), "这一年") <= 1, block.text());
      }
    }
  }

  @Test
  void identicalAnnualConditionsKeepTheSameJudgmentButUseDistinctPlannedHeadlines() throws Exception {
    var content = content(scoredCase("S07"));
    assertEquals("wealth-plain-v3.16", content.copyVersion());
    assertEquals("wealth-headline-v3", content.headlinePlannerVersion());
    assertEquals(3, content.years().stream().map(y -> y.overview().text()).distinct().count());
    assertEquals(3, content.years().stream().map(y -> y.headlineMeta().themeKey()).distinct().count());
    assertEquals(3, content.years().stream().map(y -> y.retention().text()).distinct().count());
    assertEquals(content.years().stream().flatMap(y -> y.observations().stream()).count(),
        content.years().stream().flatMap(y -> y.observations().stream()).map(Block::text).distinct().count());
    assertEquals(content.years().stream().flatMap(y -> y.actions().stream()).count(),
        content.years().stream().flatMap(y -> y.actions().stream()).map(Block::text).distinct().count());
    assertEquals("unchanged", content.years().get(0).comparison().direction());
    assertTrue(content.years().get(0).comparison().reading().text().contains("没有明显变化"));
    assertNull(content.years().get(2).comparison());
  }

  @Test
  void incomeParagraphDoesNotBorrowAnUnrelatedHeadlineSubjectToFakeAnnualDifference() throws Exception {
    var content = content(scoredCase("S08"));

    for (var year : content.years()) {
      String headlinePath = year.headlineMeta().pathKey();
      String subject = WealthHeadlineVocabulary.entry(year.headlineMeta().themeKey())
          .sentenceSpec().subject().replaceFirst("^今年", "");
      for (var income : year.income()) {
        boolean coversHeadlinePath = income.decisionIds().stream()
            .anyMatch(id -> id.endsWith(".decision." + headlinePath));
        if (!coversHeadlinePath) assertFalse(income.text().contains(subject), income.text());
      }
    }
  }

  @Test
  void annualObservationAndActionExpandTheObjectNamedByThePlannedHeadline() throws Exception {
    var content = content(scoredCase("S07"));

    for (var year : content.years()) {
      String object = WealthHeadlineVocabulary.entry(year.headlineMeta().themeKey()).objectText();
      assertTrue(year.observations().get(0).text().contains(object), year.observations().toString());
      assertTrue(year.actions().get(0).text().contains(object), year.actions().toString());
      assertTrue(year.retention().text().contains(object), year.retention().text());
      assertTrue(year.retention().templateId().contains(year.headlineMeta().themeKey()),
          year.retention().templateId());
      if (year.risk() != null) {
        assertTrue(year.risk().reading().text().contains(object), year.risk().reading().text());
        assertTrue(year.risk().reading().templateId().contains(year.headlineMeta().themeKey()),
            year.risk().reading().templateId());
      }
    }
  }

  @Test
  void annualHeadlineMetadataAndReferencesComeFromTheReportLevelPlan() throws Exception {
    var content = content(scoredCase("S07"));
    for (var year : content.years()) {
      assertNotNull(year.headlineMeta());
      assertEquals("wealth-headline-v3", year.headlineMeta().plannerVersion());
      assertEquals(1, year.overview().decisionIds().size());
      assertTrue(year.overview().decisionIds().stream().allMatch(id -> id.startsWith(year.year() + ".decision.")));
      assertEquals(year.headlineMeta().pathKey(), year.overview().decisionIds().get(0)
          .substring((year.year() + ".decision.").length()));
      assertFalse(year.headlineMeta().selectionReasonCodes().isEmpty());
    }
  }

  @Test
  void thesisRespondsToActualIncomeDirections() throws Exception {
    var stable = content(withPathWeights(scoredCase("S02"), WealthPath.STABLE_INCOME, 8, 0));
    var skill = content(withPathWeights(scoredCase("S02"), WealthPath.SKILL_INCOME, 8, 0));
    assertNotEquals(stable.thesis().text(), skill.thesis().text());
    assertTrue(stable.thesis().text().contains("保持连续"));
    assertTrue(skill.thesis().text().contains("转成实际进账"));
  }

  @Test
  void reportRouteReadsAsOneOrderedPlanInsteadOfUnrelatedTips() throws Exception {
    var route = content(scoredCase("S07")).route();

    assertEquals(2, route.size());
    assertTrue(route.get(0).text().startsWith("先"), route.toString());
    assertTrue(route.get(1).text().startsWith("最后"), route.toString());
  }

  @Test
  void everyBlockHasAppropriateYearAndDecisionReferences() throws Exception {
    var content = content(scoredCase("S07"));
    var decisions = content.years().stream().flatMap(y -> y.decisions().stream())
        .collect(Collectors.toMap(d -> d.id(), d -> d));
    Set<String> ids = new java.util.HashSet<>();
    for (var block : content.blocks()) {
      assertTrue(ids.add(block.id()));
      assertFalse(block.years().isEmpty());
      if (block.kind().equals("interpretation")) assertFalse(block.decisionIds().isEmpty());
      for (String id : block.decisionIds()) {
        assertTrue(decisions.containsKey(id));
        assertTrue(block.years().contains(decisions.get(id).year()));
      }
    }
    for (var year : content.years()) {
      for (var block : List.of(year.overview(), year.retention())) assertEquals(List.of(year.year()), block.years());
      assertTrue(year.actions().stream().allMatch(b -> b.kind().equals("general_advice")));
      assertTrue(year.observations().stream().allMatch(b -> b.kind().equals("observation")));
      if (year.comparison() != null) assertEquals(List.of(year.year(), year.year() + 1), year.comparison().reading().years());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 5})
  void internalCopySupportsTheTechnicalHorizonWithoutChangingSharedYears(int count) throws Exception {
    var paths = scoredCase("S07").years().get(0).paths();
    var years = IntStream.range(2026, 2026 + count).mapToObj(y ->
        assessScored(new WealthArbitrator().arbitrate(y, "丙午", paths))).toList();
    var content = writer.plan(years, AS_OF);
    assertNotNull(content);
    assertEquals(count, content.horizonYears());
    assertEquals(count, content.years().size());
    var expected = content(scoredCase("S07")).years().get(0);
    var actual = content.years().get(0);
    assertEquals(expected.facts(), actual.facts());
    assertEquals(expected.evidence(), actual.evidence());
    assertEquals(expected.decisions(), actual.decisions());
    assertEquals(expected.focus(), actual.focus());
    assertEquals(expected.risk(), actual.risk());
  }

  @Test
  void allZeroCoverageIsRejectedInsteadOfSellingGeneralAdvice() throws Exception {
    var years = assessments(scoredCase("S01"));
    assertThrows(IllegalArgumentException.class, () -> writer.plan(years, AS_OF));
  }

  @Test
  void unresolvedSourcesAreRejectedInsteadOfBeingHiddenInPlainCopy() throws Exception {
    var years = fullBirthFacts("R01").stream().map(new WealthV3Analyzer()::assess).toList();
    assertThrows(IllegalArgumentException.class, () -> writer.plan(years, AS_OF));
  }

  @Test
  void startDateMustMatchTheActualFirstYear() throws Exception {
    var years = assessments(scoredCase("S07"));
    assertThrows(IllegalArgumentException.class, () -> writer.plan(years, AS_OF.plusYears(1)));
  }

  @Test
  void unknownTemplateAndOverstrongAdviceCannotBypassTheCopyLicense() throws Exception {
    var content = content(scoredCase("S07"));
    var b = content.thesis();
    var unknown = new Block(b.id(), b.kind(), b.text(), "unknown.template", b.years(), b.decisionIds());
    assertThrows(IllegalArgumentException.class, () -> writer.validateDraft(withThesis(content, unknown)));
    var disguised = new Block(b.id(), "general_advice", "明年一定赚大钱。", b.templateId(), b.years(), b.decisionIds());
    assertThrows(IllegalArgumentException.class, () -> writer.validateDraft(withThesis(content, disguised)));
    assertDoesNotThrow(() -> writer.validateDraft(content));
  }

  @Test
  void outputIsDeterministicAndUsesShortNaturalSentences() throws Exception {
    var period = scoredCase("S07");
    var content = content(period);
    assertEquals(content, content(period));
    for (var block : content.blocks()) {
      assertFalse(block.text().isBlank());
      assertTrue(block.text().length() <= 240, block.text());
      for (String banned : List.of("现有证据不足", "先观察再判断", "卡点", "卡住", "承接", "交付", "兑现", "复盘", "第一年", "第二年", "第三年")) {
        assertFalse(block.text().contains(banned), block.text());
      }
      for (String sentence : block.text().split("[。！？；]")) assertTrue(sentence.codePoints().filter(c -> c >= 0x4e00 && c <= 0x9fff).count() <= 48, sentence);
    }
  }

  @Test
  void customerFacingCopyNeverInfersOccupationOrIncomeSource() throws Exception {
    List<String> forbidden = List.of(
        "工资", "加薪", "客户", "项目", "订单", "接单", "接活", "服务",
        "报价", "回款", "生意", "手艺", "按单", "按次", "合作收入");

    for (String fixture : List.of("S02", "S03", "S05", "S07", "S08")) {
      WealthNarrativeV3 content = content(scoredCase(fixture));
      String visibleCopy = content.blocks().stream()
          .map(Block::text)
          .collect(Collectors.joining("\n"));

      for (String word : forbidden) {
        assertFalse(visibleCopy.contains(word), fixture + " inferred " + word + ":\n" + visibleCopy);
      }
    }
  }

  @Test
  void customerFacingCopyExplainsMoneyStateInsteadOfEarningMethod() throws Exception {
    String visibleCopy = content(scoredCase("S07")).blocks().stream()
        .map(Block::text)
        .collect(Collectors.joining("\n"));

    for (String dimension : List.of("进账", "到账", "支出", "结余", "责任", "风险")) {
      assertTrue(visibleCopy.contains(dimension), dimension + " missing from:\n" + visibleCopy);
    }
  }

  private WealthNarrativeV3 content(WealthPeriodEvaluation period) {
    var content = writer.plan(assessments(period), AS_OF);
    assertNotNull(content, "the real v3 writer must produce a draft");
    return content;
  }

  private static int occurrences(String text, String token) {
    return (text.length() - text.replace(token, "").length()) / token.length();
  }

  @Test
  void becomingTheRelativeLeaderDoesNotClaimThatItsOwnSupportIncreased() throws Exception {
    var period = withPathWeights(scoredCase("S02"), WealthPath.STABLE_INCOME, 8, 0);
    period = withPathWeights(period, WealthPath.SKILL_INCOME, 4, 0);
    period = changeEvidence(period, 1, WealthPath.STABLE_INCOME, old -> List.of(
        scored("variant.stable_income.support", 2, EvidenceFamily.ANNUAL_TRIGGER, "合成支持条件")));
    var text = content(period).years().get(0).comparison().reading().text();
    assertTrue(text.contains("投入与进账需要优先核对"), text);
    assertFalse(text.contains("投入后实际进账提高的迹象更明显"), text);
  }

  @Test
  void equalScoresWithChangedSourceIndependenceExplainTheStrengthChange() throws Exception {
    var period = withPathWeights(scoredCase("S05"), WealthPath.STABLE_INCOME, 8, 0);
    period = changeEvidence(period, 1, WealthPath.STABLE_INCOME, old -> List.of(
        scored("synthetic.stable.natal", 4, EvidenceFamily.NATAL_STRUCTURE, "合成原局支持"),
        scored("synthetic.stable.annual", 4, EvidenceFamily.ANNUAL_TRIGGER, "合成流年支持")));
    var content = content(period);
    assertEquals("supported", WealthExpressionPolicyTest.decision(assessments(period).get(0), "stable_income").strength());
    assertEquals("pronounced", WealthExpressionPolicyTest.decision(assessments(period).get(1), "stable_income").strength());
    assertTrue(content.years().get(0).comparison().reading().text().contains("进账保持连续的迹象比上年更明显"));
    assertTrue(content.years().get(1).comparison().reading().text().contains("进账保持连续的迹象比上年减弱"));
  }

  @Test
  void multiYearThesisDoesNotCallEveryYearThisYear() throws Exception {
    assertFalse(content(scoredCase("S07")).thesis().text().contains("今年"));
    assertFalse(content(scoredCase("S02")).thesis().text().contains("今年"));
  }

  @Test
  void retentionSummaryDoesNotTreatSavingsAsAnIncomeSource() throws Exception {
    var text = content(scoredCase("S02")).pathSummaries().stream().filter(p -> p.path().equals("retention"))
        .findFirst().orElseThrow().reading().text();
    assertFalse(text.contains("增收重点"), text);
  }

  static List<WealthAssessment> assessments(WealthPeriodEvaluation period) {
    return period.years().stream().map(WealthRemediationFixtures::assessScored).toList();
  }

  static WealthNarrativeV3 withThesis(WealthNarrativeV3 c, Block thesis) {
    return new WealthNarrativeV3(c.asOf(), c.zoneId(), c.horizonYears(), c.calculationVersion(), c.policyVersion(),
        c.copyVersion(), thesis, c.summary(), c.pathSummaries(), c.riskSummary(), c.years(), c.route(), c.readingNote());
  }
}
