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

    assertTrue(review.headline().startsWith("主判断：2025年先回看"), review.headline());
    assertTrue(review.checkpoints().get(0).startsWith("次判断：再回看"), review.checkpoints().toString());
    assertTrue(review.checkpoints().get(1).startsWith("隐性影响：还要回看"), review.checkpoints().toString());
    assertEquals(plan.evidenceKeys(), review.evidenceKeys());
    assertEquals(review, writer.write(plan));
    for (String forbidden : List.of(
        "工资", "客户", "项目", "订单", "按次", "卡点", "抓手", "闭环", "原局", "十神", "大运", "流年")) {
      assertFalse(visible(review).contains(forbidden), forbidden + " in " + visible(review));
    }
    for (String sentence : visible(review).split("(?<=[。！？])")) {
      assertTrue(sentence.strip().length() <= 48, sentence.length() + " chars: " + sentence);
    }
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
        Arguments.of("stable_income", "进账是否能够持续"),
        Arguments.of("skill_income", "时间和花费是否换来相称的进账"),
        Arguments.of("project_income", "预计进账与实际到账是否一致"),
        Arguments.of("cooperation_income", "与他人有关的钱是否增加额外责任"),
        Arguments.of("retention", "进账以后实际留下的钱有没有增加"));
  }

  private static Stream<Arguments> directionCases() {
    return Stream.of(
        Arguments.of("supportive", "pronounced", "支持比较明显"),
        Arguments.of("supportive", "supported", "有一定支持"),
        Arguments.of("supportive", "limited", "有迹象，但力度不强"),
        Arguments.of("mixed", "limited", "有支持，也有实际限制"),
        Arguments.of("restricted", "limited", "限制更明显"));
  }

  private static Stream<Arguments> angleCases() {
    return Stream.of(
        Arguments.of("annual_stem", "这一年直接出现的收支变化"),
        Arguments.of("annual_harmony", "他人或原有安排牵动"),
        Arguments.of("annual_clash", "突然且明显的变动"),
        Arguments.of("annual_harm", "零散且不易察觉的损耗"),
        Arguments.of("annual_punishment", "同类收支问题是否反复出现"),
        Arguments.of("annual_context", "这一年的收支条件是否改变"),
        Arguments.of("dayun_context", "较长一段时间的收支状态"),
        Arguments.of("natal_output_wealth", "长期投入能否转成实际进账"),
        Arguments.of("natal_wealth_capacity", "进账增加后能否承受相应开销"),
        Arguments.of("natal_shared_responsibility", "共同用钱是否影响最后结余"),
        Arguments.of("natal_balance", "额外开销是否同步增加"),
        Arguments.of("natal_combination", "原有收支条件是否相互牵动"),
        Arguments.of("natal_structure", "自己一贯的收支方式"));
  }
}
