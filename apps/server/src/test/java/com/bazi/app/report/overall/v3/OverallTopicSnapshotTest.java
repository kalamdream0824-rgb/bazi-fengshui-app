package com.bazi.app.report.overall.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.report.ConfidenceLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverallTopicSnapshotTest {

  @Test
  void acceptsAnEvidenceBackedSemanticSnapshot() {
    OverallTopicSnapshot snapshot = snapshot(
        OverallTopicSnapshot.Topic.WEALTH, "wealth.retention.expense_limit");

    assertEquals("wealth.retention.expense_limit", snapshot.focusKey());
    assertEquals(List.of("wealth.reserve", "wealth.reduce_commitment"),
        snapshot.actionCandidateKeys());
  }

  @Test
  void rejectsBlankFocusEmptyActionsAndInsufficientEvidence() {
    assertThrows(IllegalArgumentException.class, () -> new OverallTopicSnapshot(
        2026,
        OverallTopicSnapshot.Topic.WEALTH,
        " ",
        OverallTopicSnapshot.Stance.PRESSURED,
        OverallTopicSnapshot.Urgency.HIGH,
        ConfidenceLevel.HIGH,
        null,
        "wealth.expense_limit",
        List.of("wealth.reserve"),
        List.of("annual.stem.group.wealth", "annual.branch.clash.dayun")));
    assertThrows(IllegalArgumentException.class, () -> new OverallTopicSnapshot(
        2026,
        OverallTopicSnapshot.Topic.WEALTH,
        "wealth.retention.expense_limit",
        OverallTopicSnapshot.Stance.PRESSURED,
        OverallTopicSnapshot.Urgency.HIGH,
        ConfidenceLevel.HIGH,
        null,
        "wealth.expense_limit",
        List.of(),
        List.of("annual.stem.group.wealth", "annual.branch.clash.dayun")));
    assertThrows(IllegalArgumentException.class, () -> new OverallTopicSnapshot(
        2026,
        OverallTopicSnapshot.Topic.WEALTH,
        "wealth.retention.expense_limit",
        OverallTopicSnapshot.Stance.PRESSURED,
        OverallTopicSnapshot.Urgency.HIGH,
        ConfidenceLevel.HIGH,
        null,
        "wealth.expense_limit",
        List.of("wealth.reserve"),
        List.of("annual.stem.group.wealth")));
  }

  @Test
  void rejectsDuplicateSemanticKeysInsteadOfSilentlyHidingThem() {
    assertThrows(IllegalArgumentException.class, () -> new OverallTopicSnapshot(
        2026,
        OverallTopicSnapshot.Topic.CAREER,
        "career.output.result",
        OverallTopicSnapshot.Stance.SUPPORTIVE,
        OverallTopicSnapshot.Urgency.MEDIUM,
        ConfidenceLevel.MEDIUM,
        "career.visible_result",
        null,
        List.of("career.finish", "career.finish"),
        List.of("annual.stem.group.output", "annual.stem.group.output")));
  }

  @Test
  void decisionRequiresTwoDifferentTopicsAndOnlyTheirEvidence() {
    OverallTopicSnapshot wealth = snapshot(
        OverallTopicSnapshot.Topic.WEALTH, "wealth.retention.expense_limit");
    OverallTopicSnapshot career = snapshot(
        OverallTopicSnapshot.Topic.CAREER, "career.output.visible_result");

    OverallAnnualDecision decision = new OverallAnnualDecision(
        2026,
        wealth,
        career,
        "cash_buffer_before_growth",
        "wealth_then_career",
        "wealth.reserve",
        List.of("annual.stem.group.wealth", "annual.branch.clash.dayun",
            "annual.stem.group.output"));

    assertEquals(OverallTopicSnapshot.Topic.WEALTH, decision.primary().topic());
    assertThrows(IllegalArgumentException.class, () -> new OverallAnnualDecision(
        2026, wealth, wealth, "same", "same", "wealth.reserve", wealth.evidenceKeys()));
    assertThrows(IllegalArgumentException.class, () -> new OverallAnnualDecision(
        2026, wealth, career, "cash_buffer_before_growth", "wealth_then_career",
        "wealth.reserve", List.of("not.from.either.snapshot")));
  }

  private OverallTopicSnapshot snapshot(
      OverallTopicSnapshot.Topic topic, String focusKey) {
    String topicKey = topic.name().toLowerCase();
    return new OverallTopicSnapshot(
        2026,
        topic,
        focusKey,
        topic == OverallTopicSnapshot.Topic.WEALTH
            ? OverallTopicSnapshot.Stance.PRESSURED
            : OverallTopicSnapshot.Stance.SUPPORTIVE,
        topic == OverallTopicSnapshot.Topic.WEALTH
            ? OverallTopicSnapshot.Urgency.HIGH
            : OverallTopicSnapshot.Urgency.MEDIUM,
        ConfidenceLevel.HIGH,
        topicKey + ".opportunity",
        topicKey + ".risk",
        topic == OverallTopicSnapshot.Topic.WEALTH
            ? List.of("wealth.reserve", "wealth.reduce_commitment")
            : List.of(topicKey + ".finish", topicKey + ".review"),
        topic == OverallTopicSnapshot.Topic.WEALTH
            ? List.of("annual.stem.group.wealth", "annual.branch.clash.dayun")
            : List.of("annual.stem.group.output", "natal.balance.middle"));
  }
}
