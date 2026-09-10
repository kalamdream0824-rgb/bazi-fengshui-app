package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRetrospectivePlanTest.citation;
import static com.bazi.app.report.wealth.WealthRetrospectivePlanTest.observation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.wealth.v3.WealthRetrospectivePlan;
import com.bazi.app.report.wealth.v3.WealthRetrospectivePlan.Observation;
import com.bazi.app.report.wealth.v3.WealthRetrospectiveWriter;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WealthRetrospectiveWriterTest {
  private final WealthRetrospectiveWriter writer = new WealthRetrospectiveWriter();

  @ParameterizedTest
  @MethodSource("subjectCases")
  void everySubjectNamesAConcreteMoneyQuestion(String subject, String expected) {
    NarrativeTimeline.PastReview review = writer.write(plan(
        observed(subject, "supportive", "supported", "annual_stem", "a"),
        observed(otherSubject(subject), "supportive", "supported", "dayun_context", "b"),
        observed("retention".equals(subject) ? "project_income" : "retention",
            "restricted", "limited", "annual_harm", "c")));

    assertTrue(visible(review).contains(expected), visible(review));
  }

  @ParameterizedTest
  @MethodSource("directionCases")
  void directionAndStrengthChangeTheJudgmentWithoutAssertingAnEvent(
      String direction, String strength, String expected) {
    NarrativeTimeline.PastReview review = writer.write(plan(
        observed("stable_income", direction, strength, "annual_stem", "a"),
        observed("skill_income", "supportive", "supported", "dayun_context", "b"),
        observed("retention", "restricted", "limited", "annual_harm", "c")));

    assertTrue(review.headline().contains(expected), review.headline());
    assertFalse(visible(review).contains("准确命中"));
    assertFalse(visible(review).contains("去年一定"));
  }

  @ParameterizedTest
  @MethodSource("angleCases")
  void everyEvidenceAngleHasPlainReviewLanguage(String angle, String expected) {
    NarrativeTimeline.PastReview review = writer.write(plan(
        observed("stable_income", "supportive", "supported", angle, "a"),
        observed("skill_income", "supportive", "supported", differentAngle(angle), "b"),
        observed("retention", "restricted", "limited", "annual_harm".equals(angle)
            ? "annual_clash" : "annual_harm", "c")));

    assertTrue(review.headline().contains(expected), review.headline());
  }

  @Test
  void outputUsesThreeDistinctTraceableSlotsAndNaturalShortSentences() {
    WealthRetrospectivePlan plan = plan(
        observed("stable_income", "supportive", "pronounced", "annual_stem", "a"),
        observed("skill_income", "mixed", "limited", "natal_output_wealth", "b"),
        observed("retention", "restricted", "limited", "annual_clash", "c"));

    NarrativeTimeline.PastReview review = writer.write(plan);

    assertTrue(review.headline().startsWith("主判断：回看2025年，先核对"), review.headline());
    assertTrue(review.checkpoints().get(0).startsWith("次判断：再回看并核对"), review.checkpoints().toString());
    assertTrue(review.checkpoints().get(1).startsWith("隐性影响：还要回看"), review.checkpoints().toString());
    assertEquals(plan.evidenceKeys(), review.evidenceKeys());
    assertEquals(review, writer.write(plan));
    assertEquals(1, Stream.of(visible(review).split("命盘提示", -1)).count() - 1,
        visible(review));
    for (String vague : List.of("这方面", "它主要关系到", "同时，", "从整体收支看")) {
      assertFalse(visible(review).contains(vague), vague + " in " + visible(review));
    }
    for (String forbidden : List.of(
        "工资", "客户", "项目", "订单", "按次", "卡点", "抓手", "闭环", "原局", "十神", "大运", "流年",
        "这一点有支持", "这一点的支持")) {
      assertFalse(visible(review).contains(forbidden), forbidden + " in " + visible(review));
    }
    for (String sentence : visible(review).split("(?<=[。！？])")) {
      assertTrue(sentence.strip().length() <= 48, sentence.length() + " chars: " + sentence);
    }
  }

  @Test
  void hiddenImpactUsesItsEvidenceAngleInsteadOfRepeatingTheSecondaryQuestion() {
    WealthRetrospectivePlan plan = plan(
        observed("project_income", "mixed", "limited", "natal_structure", "a"),
        observed("retention", "mixed", "limited", "natal_shared_responsibility", "b"),
        observed("retention", "mixed", "limited", "annual_stem", "c"));

    NarrativeTimeline.PastReview review = writer.write(plan);

    assertEquals(1, Stream.of(review.checkpoints().get(0), review.checkpoints().get(1))
        .filter(line -> line.contains("进账增加后，实际留下的钱有没有变多")).count());
    assertTrue(review.checkpoints().get(1).contains("当年直接出现的收支变化"),
        review.checkpoints().get(1));
    assertTrue(review.checkpoints().get(1).contains("实际结余"), review.checkpoints().get(1));
    assertFalse(review.checkpoints().get(1).contains("作为补充"), review.checkpoints().get(1));
    assertEquals(1, review.checkpoints().get(1).chars().filter(character -> character == '。').count(),
        review.checkpoints().get(1));
  }

  @Test
  void bridgeReadsNaturallyForSharedMoneyResponsibility() {
    WealthRetrospectivePlan plan = plan(
        observed("cooperation_income", "supportive", "supported", "annual_harmony", "a"),
        observed("stable_income", "supportive", "supported", "annual_stem", "b"),
        observed("retention", "restricted", "limited", "annual_harm", "c"));

    String bridge = writer.write(plan).bridge();

    assertFalse(bridge.contains("责任是否有改善"), bridge);
    assertEquals("如果去年确实把共同用钱的责任和开销说清了，今年再看这种情况能否延续。", bridge);
  }

  @Test
  void bridgeKeepsNaturalWordOrderForInvestmentAndMixedReceipts() {
    String investment = writer.write(plan(
        observed("skill_income", "supportive", "supported", "annual_stem", "a"),
        observed("stable_income", "supportive", "supported", "dayun_context", "b"),
        observed("retention", "restricted", "limited", "annual_harm", "c"))).bridge();
    String receipts = writer.write(plan(
        observed("project_income", "mixed", "limited", "annual_stem", "a"),
        observed("stable_income", "supportive", "supported", "dayun_context", "b"),
        observed("retention", "restricted", "limited", "annual_harm", "c"))).bridge();

    assertEquals("如果去年增加投入后，实际进账有所提高，今年再看这种情况能否延续。", investment);
    assertEquals("如果去年有些预计的钱按时到账，也有些到账延期或金额有变化，今年安排时要把两种情况都算进去。", receipts);
  }

  @Test
  void mixedRetentionReadsAsTwoRealLifePossibilitiesInsteadOfJoinedRuleLabels() {
    NarrativeTimeline.PastReview review = writer.write(plan(
        observed("retention", "mixed", "limited", "annual_stem", "a"),
        observed("stable_income", "supportive", "supported", "dayun_context", "b"),
        observed("project_income", "restricted", "limited", "annual_harm", "c")));

    assertTrue(review.headline().contains(
        "有些时候能多留下一点钱，有些时候进账增加了，结余却没有跟着增加。"),
        review.headline());
  }

  @Test
  void partialPlansUseExplicitScopeCopyInsteadOfInventingConclusions() {
    WealthRetrospectivePlan plan = new WealthRetrospectivePlan(2025,
        observed("stable_income", "supportive", "supported", "annual_stem", "a"), null, null);

    NarrativeTimeline.PastReview review = writer.write(plan);

    assertEquals("次判断：本次回看不再加入另一项具体判断。", review.checkpoints().get(0));
    assertEquals("隐性影响：本次回看不再加入另一项具体影响。", review.checkpoints().get(1));
    assertEquals(List.of("annual.stem.ten_god.a"), review.evidenceKeys());
  }

  @Test
  void emptyPlanCannotMasqueradeAsAnEvidenceBackedReview() {
    assertThrows(IllegalArgumentException.class,
        () -> writer.write(new WealthRetrospectivePlan(2025, null, null, null)));
  }

  private WealthRetrospectivePlan plan(Observation primary, Observation secondary, Observation hidden) {
    return new WealthRetrospectivePlan(2025, primary, secondary, hidden);
  }

  private Observation observed(String subject, String direction, String strength, String angle, String id) {
    var source = citation(id, subject, "annual.stem.ten_god." + id,
        "restricted".equals(direction) ? -3 : 3, "值" + id);
    return new Observation(subject, direction, strength, angle, List.of(source), List.of(id));
  }

  private String otherSubject(String subject) {
    return "stable_income".equals(subject) ? "skill_income" : "stable_income";
  }

  private String differentAngle(String angle) {
    return "dayun_context".equals(angle) ? "annual_stem" : "dayun_context";
  }

  private String visible(NarrativeTimeline.PastReview review) {
    return String.join("\n", review.headline(), String.join("\n", review.checkpoints()), review.bridge());
  }

  private static Stream<Arguments> subjectCases() {
    return Stream.of(
        Arguments.of("stable_income", "进账有没有持续下来"),
        Arguments.of("skill_income", "投入增加后，实际进账有没有跟着提高"),
        Arguments.of("project_income", "预计进账与实际到账是否一致"),
        Arguments.of("cooperation_income", "与他人共同用钱时，责任和开销有没有说清"),
        Arguments.of("retention", "进账增加后，实际留下的钱有没有变多"));
  }

  private static Stream<Arguments> directionCases() {
    return Stream.of(
        Arguments.of("supportive", "pronounced", "进账保持连续的信号较明显"),
        Arguments.of("supportive", "supported", "进账保持连续的迹象存在，但不算突出"),
        Arguments.of("supportive", "limited", "进账保持连续的信号较弱"),
        Arguments.of("mixed", "limited", "有些时候进账连续，也可能出现中断或波动"),
        Arguments.of("restricted", "limited", "进账中断或波动的信号更明显"));
  }

  private static Stream<Arguments> angleCases() {
    return Stream.of(
        Arguments.of("annual_stem", "这一年实际发生的收支变化"),
        Arguments.of("annual_harmony", "他人或原有安排有没有影响收支"),
        Arguments.of("annual_clash", "收支是否出现突然且明显的变动"),
        Arguments.of("annual_harm", "有没有零散、不易察觉的损耗"),
        Arguments.of("annual_punishment", "同类收支问题是否反复出现"),
        Arguments.of("annual_context", "这一年的收支条件是否改变"),
        Arguments.of("dayun_context", "前后一段时间的收支状态"),
        Arguments.of("natal_output_wealth", "长期投入有没有转成实际进账"),
        Arguments.of("natal_wealth_capacity", "进账增加后，相关开销是否也增加"),
        Arguments.of("natal_shared_responsibility", "共同用钱是否影响最后结余"),
        Arguments.of("natal_balance", "额外开销是否随着进账增加"),
        Arguments.of("natal_combination", "原有收支条件是否相互影响"),
        Arguments.of("natal_structure", "自己一贯的收支方式"));
  }
}
