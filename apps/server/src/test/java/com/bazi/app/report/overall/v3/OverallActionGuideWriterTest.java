package com.bazi.app.report.overall.v3;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.report.AnnualActionGuide;
import com.bazi.app.report.AnnualActionGuidePolicy;
import com.bazi.app.report.ConfidenceLevel;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OverallActionGuideWriterTest {

  private final OverallActionGuideWriter writer = new OverallActionGuideWriter();

  @ParameterizedTest
  @MethodSource("conflicts")
  void writesACompleteObservableLoopForEverySupportedConflict(
      OverallTopicSnapshot.Topic primary,
      OverallTopicSnapshot.Topic secondary,
      String conflictKey,
      String primaryResult,
      String secondaryProtection) {
    OverallAnnualDecision decision = decision(primary, secondary, conflictKey);

    AnnualActionGuide guide = writer.write(decision);

    assertEquals("overall." + code(primary) + "." + code(secondary) + "." + conflictKey,
        guide.focusKey());
    assertTrue(guide.expectedChange().contains(primaryResult), guide.expectedChange());
    assertTrue(guide.expectedChange().contains(secondaryProtection), guide.expectedChange());
    assertTrue(guide.checkTiming().matches(".*([0-9一二三四五六七八九十]+[天周月次]).*"),
        guide.checkTiming());
    assertNotEquals(guide.action(), guide.fallbackAction());
    assertDoesNotThrow(() -> AnnualActionGuidePolicy.validate(List.of(guide)));
    assertDoesNotThrow(() -> OverallV3NarrativePolicy.validate(List.of(guide)));
  }

  @Test
  void differentCrossTopicFocusesCannotShareOneDecisionTuple() {
    AnnualActionGuide capacity = writer.write(decision(
        OverallTopicSnapshot.Topic.RHYTHM,
        OverallTopicSnapshot.Topic.CAREER,
        "capacity_before_career_expansion"));
    AnnualActionGuide cash = writer.write(decision(
        OverallTopicSnapshot.Topic.WEALTH,
        OverallTopicSnapshot.Topic.CAREER,
        "cash_buffer_before_growth"));

    assertNotEquals(tuple(capacity), tuple(cash));
    assertDoesNotThrow(() -> AnnualActionGuidePolicy.validate(List.of(capacity, cash)));
  }

  @Test
  void differentCalculatedFocusesWithinOneTopicPairCannotShareOneDecisionTuple() {
    OverallAnnualDecision retainedDecision = decision(
        snapshot(OverallTopicSnapshot.Topic.WEALTH, "wealth.retention.restricted", true),
        snapshot(OverallTopicSnapshot.Topic.CAREER, "career.visibility", false),
        "cash_buffer_before_growth");
    OverallAnnualDecision delayedDecision = decision(
        snapshot(OverallTopicSnapshot.Topic.WEALTH, "wealth.project_income.restricted", true),
        snapshot(OverallTopicSnapshot.Topic.CAREER, "career.visibility", false),
        "cash_buffer_before_growth");
    AnnualActionGuide retainedCash = writer.write(retainedDecision);
    AnnualActionGuide delayedCash = writer.write(delayedDecision);

    assertNotEquals(retainedDecision.primary().focusKey(), delayedDecision.primary().focusKey());
    assertNotEquals(tuple(retainedCash), tuple(delayedCash));
  }

  @Test
  void copyDoesNotInventAnUnconfirmedIdentityOrReturnNoAnswerLanguage() {
    String copy = conflicts().map(arguments -> {
      Object[] values = arguments.get();
      return writer.write(decision(
          (OverallTopicSnapshot.Topic) values[0],
          (OverallTopicSnapshot.Topic) values[1],
          (String) values[2])).lines();
    }).flatMap(List::stream).reduce("", String::concat);

    for (String forbidden : List.of(
        "老板", "客户", "负责人", "职位", "配偶", "对象", "求职", "创业",
        "卡点", "卡住", "现有证据不足", "先观察再判断", "视情况")) {
      assertFalse(copy.contains(forbidden), forbidden + " found in: " + copy);
    }
  }

  @Test
  void focusKeyContainsCalculationMeaningButNoCalendarPosition() {
    AnnualActionGuide guide = writer.write(decision(
        OverallTopicSnapshot.Topic.CAREER,
        OverallTopicSnapshot.Topic.WEALTH,
        "career_with_wealth_watch"));

    assertTrue(guide.focusKey().contains("career_with_wealth_watch"));
    assertFalse(guide.focusKey().matches(".*20[0-9]{2}.*"), guide.focusKey());
    assertFalse(guide.focusKey().contains("first"), guide.focusKey());
  }

  private static Stream<Arguments> conflicts() {
    return Stream.of(
        Arguments.of(
            OverallTopicSnapshot.Topic.RHYTHM,
            OverallTopicSnapshot.Topic.CAREER,
            "capacity_before_career_expansion",
            "稳定时间",
            "成果"),
        Arguments.of(
            OverallTopicSnapshot.Topic.WEALTH,
            OverallTopicSnapshot.Topic.CAREER,
            "cash_buffer_before_growth",
            "实际余钱",
            "成果"),
        Arguments.of(
            OverallTopicSnapshot.Topic.RELATIONSHIP,
            OverallTopicSnapshot.Topic.CAREER,
            "relationship_stability_before_growth",
            "明确回应",
            "成果"),
        Arguments.of(
            OverallTopicSnapshot.Topic.CAREER,
            OverallTopicSnapshot.Topic.WEALTH,
            "career_with_wealth_watch",
            "成果",
            "实际余钱"));
  }

  private OverallAnnualDecision decision(
      OverallTopicSnapshot.Topic primary,
      OverallTopicSnapshot.Topic secondary,
      String conflictKey) {
    OverallTopicSnapshot first = snapshot(primary, true);
    OverallTopicSnapshot second = snapshot(secondary, false);
    return decision(first, second, conflictKey);
  }

  private OverallAnnualDecision decision(
      OverallTopicSnapshot first,
      OverallTopicSnapshot second,
      String conflictKey) {
    return new OverallAnnualDecision(
        2026,
        first,
        second,
        conflictKey,
        first.focusKey() + "__" + second.focusKey() + "__" + conflictKey,
        first.actionCandidateKeys().get(0),
        Stream.concat(first.evidenceKeys().stream(), second.evidenceKeys().stream())
            .toList());
  }

  private OverallTopicSnapshot snapshot(OverallTopicSnapshot.Topic topic, boolean primary) {
    String code = code(topic);
    return snapshot(topic, code + ".focus." + (primary ? "primary" : "secondary"), primary);
  }

  private OverallTopicSnapshot snapshot(
      OverallTopicSnapshot.Topic topic, String focusKey, boolean primary) {
    String code = code(topic);
    return new OverallTopicSnapshot(
        2026,
        topic,
        focusKey,
        primary ? OverallTopicSnapshot.Stance.PRESSURED : OverallTopicSnapshot.Stance.SUPPORTIVE,
        primary ? OverallTopicSnapshot.Urgency.HIGH : OverallTopicSnapshot.Urgency.MEDIUM,
        ConfidenceLevel.HIGH,
        1,
        primary ? null : code + ".opportunity",
        primary ? code + ".risk" : null,
        List.of(code + ".act", code + ".fallback"),
        List.of("annual." + code + ".direct", "natal." + code + ".base"));
  }

  private String code(OverallTopicSnapshot.Topic topic) {
    return topic.name().toLowerCase(java.util.Locale.ROOT);
  }

  private String tuple(AnnualActionGuide guide) {
    return guide.action() + "|" + guide.expectedChange() + "|" + guide.successSignal();
  }
}
