package com.bazi.app.report.overall.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.bazi.app.report.AnnualActionGuide;
import com.bazi.app.report.ConfidenceLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Release gates for failure modes that must not be carried from overall v2 into v3. */
class OverallV3RegressionGateTest {

  private final OverallActionGuideWriter writer = new OverallActionGuideWriter();

  @Test
  void calendarPositionAloneCannotChangeCustomerCopy() {
    AnnualActionGuide first = writer.write(decision(2026));
    AnnualActionGuide second = writer.write(decision(2027));

    assertEquals(first.focusKey(), second.focusKey());
    assertEquals(first.lines(), second.lines());
  }

  @Test
  void contextFreeOverallCopyDoesNotInventAJobOrRelationshipStatus() {
    String report = String.join("", writer.write(decision(2026)).lines());

    for (String inventedIdentity : List.of(
        "老板", "客户", "负责人", "职位", "配偶", "对象", "求职", "创业")) {
      assertFalse(report.contains(inventedIdentity),
          inventedIdentity + " found in context-free overall copy: " + report);
    }
  }

  @Test
  void knownNoAnswerPhrasesRemainForbidden() {
    String report = String.join("", writer.write(decision(2026)).lines());

    for (String phrase : List.of(
        "卡点", "卡住", "现有证据不足", "先观察再判断", "可以作为关注", "视情况")) {
      assertFalse(report.contains(phrase), phrase + " found in overall copy: " + report);
    }
  }

  private OverallAnnualDecision decision(int year) {
    OverallTopicSnapshot primary = snapshot(
        year,
        OverallTopicSnapshot.Topic.WEALTH,
        OverallTopicSnapshot.Stance.PRESSURED,
        OverallTopicSnapshot.Urgency.HIGH);
    OverallTopicSnapshot secondary = snapshot(
        year,
        OverallTopicSnapshot.Topic.CAREER,
        OverallTopicSnapshot.Stance.SUPPORTIVE,
        OverallTopicSnapshot.Urgency.MEDIUM);
    return new OverallAnnualDecision(
        year,
        primary,
        secondary,
        "cash_buffer_before_growth",
        "wealth.primary__career.secondary__cash_buffer_before_growth",
        "wealth.act",
        List.of("annual.wealth.direct", "natal.wealth.base", "annual.career.direct"));
  }

  private OverallTopicSnapshot snapshot(
      int year,
      OverallTopicSnapshot.Topic topic,
      OverallTopicSnapshot.Stance stance,
      OverallTopicSnapshot.Urgency urgency) {
    String code = topic.name().toLowerCase(java.util.Locale.ROOT);
    return new OverallTopicSnapshot(
        year,
        topic,
        code + ".focus",
        stance,
        urgency,
        ConfidenceLevel.HIGH,
        1,
        stance == OverallTopicSnapshot.Stance.SUPPORTIVE ? code + ".opportunity" : null,
        stance == OverallTopicSnapshot.Stance.PRESSURED ? code + ".risk" : null,
        List.of(code + ".act", code + ".fallback"),
        List.of("annual." + code + ".direct", "natal." + code + ".base"));
  }
}
