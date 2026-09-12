package com.bazi.app.report.overall.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bazi.app.report.ConfidenceLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class OverallDecisionArbitratorTest {

  private final OverallDecisionArbitrator arbitrator = new OverallDecisionArbitrator();

  @Test
  void protectsCapacityBeforeTakingACareerOpportunity() {
    OverallAnnualDecision decision = arbitrator.arbitrate(List.of(
        snapshot(2026, OverallTopicSnapshot.Topic.CAREER,
            OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.MEDIUM,
            ConfidenceLevel.HIGH),
        snapshot(2026, OverallTopicSnapshot.Topic.RHYTHM,
            OverallTopicSnapshot.Stance.PRESSURED, OverallTopicSnapshot.Urgency.HIGH,
            ConfidenceLevel.HIGH)));

    assertEquals(OverallTopicSnapshot.Topic.RHYTHM, decision.primary().topic());
    assertEquals(OverallTopicSnapshot.Topic.CAREER, decision.secondary().topic());
    assertEquals("capacity_before_career_expansion", decision.conflictKey());
  }

  @Test
  void protectsCashBeforeTakingACareerOpportunity() {
    OverallAnnualDecision decision = arbitrator.arbitrate(List.of(
        snapshot(2026, OverallTopicSnapshot.Topic.CAREER,
            OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.MEDIUM,
            ConfidenceLevel.HIGH),
        snapshot(2026, OverallTopicSnapshot.Topic.WEALTH,
            OverallTopicSnapshot.Stance.PRESSURED, OverallTopicSnapshot.Urgency.HIGH,
            ConfidenceLevel.HIGH)));

    assertEquals(OverallTopicSnapshot.Topic.WEALTH, decision.primary().topic());
    assertEquals("cash_buffer_before_growth", decision.conflictKey());
  }

  @Test
  void handlesRelationshipPressureWithoutAssumingRelationshipStatus() {
    OverallAnnualDecision decision = arbitrator.arbitrate(List.of(
        snapshot(2026, OverallTopicSnapshot.Topic.CAREER,
            OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.MEDIUM,
            ConfidenceLevel.HIGH),
        snapshot(2026, OverallTopicSnapshot.Topic.RELATIONSHIP,
            OverallTopicSnapshot.Stance.PRESSURED, OverallTopicSnapshot.Urgency.HIGH,
            ConfidenceLevel.HIGH)));

    assertEquals(OverallTopicSnapshot.Topic.RELATIONSHIP, decision.primary().topic());
    assertEquals("relationship_stability_before_growth", decision.conflictKey());
  }

  @Test
  void choosesTheBetterSupportedOpportunityWhenThereIsNoHighRisk() {
    OverallAnnualDecision decision = arbitrator.arbitrate(List.of(
        snapshot(2026, OverallTopicSnapshot.Topic.WEALTH,
            OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.MEDIUM,
            ConfidenceLevel.MEDIUM),
        snapshot(2026, OverallTopicSnapshot.Topic.CAREER,
            OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.MEDIUM,
            ConfidenceLevel.HIGH)));

    assertEquals(OverallTopicSnapshot.Topic.CAREER, decision.primary().topic());
    assertEquals("career_with_wealth_watch", decision.conflictKey());
  }

  @Test
  void directAnnualEvidenceOutranksGenericConfidenceAtTheSameUrgency() {
    OverallAnnualDecision decision = arbitrator.arbitrate(List.of(
        snapshot(2026, OverallTopicSnapshot.Topic.WEALTH,
            OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.MEDIUM,
            ConfidenceLevel.MEDIUM, 2),
        snapshot(2026, OverallTopicSnapshot.Topic.CAREER,
            OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.MEDIUM,
            ConfidenceLevel.HIGH, 1)));

    assertEquals(OverallTopicSnapshot.Topic.WEALTH, decision.primary().topic());
  }

  @Test
  void secondaryTopicUsesThePrimaryTopicsDependencyBeforeTheFixedTieBreak() {
    OverallAnnualDecision decision = arbitrator.arbitrate(List.of(
        snapshot(2026, OverallTopicSnapshot.Topic.CAREER,
            OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.HIGH,
            ConfidenceLevel.HIGH, 2),
        snapshot(2026, OverallTopicSnapshot.Topic.RHYTHM,
            OverallTopicSnapshot.Stance.BALANCED, OverallTopicSnapshot.Urgency.LOW,
            ConfidenceLevel.MEDIUM),
        snapshot(2026, OverallTopicSnapshot.Topic.WEALTH,
            OverallTopicSnapshot.Stance.BALANCED, OverallTopicSnapshot.Urgency.LOW,
            ConfidenceLevel.MEDIUM),
        snapshot(2026, OverallTopicSnapshot.Topic.RELATIONSHIP,
            OverallTopicSnapshot.Stance.BALANCED, OverallTopicSnapshot.Urgency.LOW,
            ConfidenceLevel.MEDIUM)));

    assertEquals(OverallTopicSnapshot.Topic.CAREER, decision.primary().topic());
    assertEquals(OverallTopicSnapshot.Topic.WEALTH, decision.secondary().topic());
  }

  @Test
  void exactTiesAndInputOrderHaveOneDeterministicResult() {
    List<OverallTopicSnapshot> input = new ArrayList<>(List.of(
        snapshot(2026, OverallTopicSnapshot.Topic.WEALTH,
            OverallTopicSnapshot.Stance.BALANCED, OverallTopicSnapshot.Urgency.LOW,
            ConfidenceLevel.MEDIUM),
        snapshot(2026, OverallTopicSnapshot.Topic.CAREER,
            OverallTopicSnapshot.Stance.BALANCED, OverallTopicSnapshot.Urgency.LOW,
            ConfidenceLevel.MEDIUM),
        snapshot(2026, OverallTopicSnapshot.Topic.RHYTHM,
            OverallTopicSnapshot.Stance.BALANCED, OverallTopicSnapshot.Urgency.LOW,
            ConfidenceLevel.MEDIUM)));
    OverallAnnualDecision ordered = arbitrator.arbitrate(input);
    Collections.reverse(input);
    OverallAnnualDecision reversed = arbitrator.arbitrate(input);

    assertEquals(ordered, reversed);
    assertEquals(OverallTopicSnapshot.Topic.RHYTHM, ordered.primary().topic());
  }

  @Test
  void keepsTheSameRealPrimaryAcrossYearsInsteadOfRotatingIt() {
    List<OverallTopicSnapshot> period = IntStream.range(0, 3)
        .boxed()
        .flatMap(index -> List.of(
            snapshot(2026 + index, OverallTopicSnapshot.Topic.CAREER,
                OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.MEDIUM,
                ConfidenceLevel.HIGH),
            snapshot(2026 + index, OverallTopicSnapshot.Topic.WEALTH,
                OverallTopicSnapshot.Stance.SUPPORTIVE, OverallTopicSnapshot.Urgency.LOW,
                ConfidenceLevel.MEDIUM)).stream())
        .toList();

    assertEquals(List.of(
            OverallTopicSnapshot.Topic.CAREER,
            OverallTopicSnapshot.Topic.CAREER,
            OverallTopicSnapshot.Topic.CAREER),
        arbitrator.arbitratePeriod(period).stream()
            .map(decision -> decision.primary().topic())
            .toList());
  }

  private OverallTopicSnapshot snapshot(
      int year,
      OverallTopicSnapshot.Topic topic,
      OverallTopicSnapshot.Stance stance,
      OverallTopicSnapshot.Urgency urgency,
      ConfidenceLevel confidence) {
    return snapshot(year, topic, stance, urgency, confidence, 1);
  }

  private OverallTopicSnapshot snapshot(
      int year,
      OverallTopicSnapshot.Topic topic,
      OverallTopicSnapshot.Stance stance,
      OverallTopicSnapshot.Urgency urgency,
      ConfidenceLevel confidence,
      int directEvidenceCount) {
    String key = topic.name().toLowerCase();
    return new OverallTopicSnapshot(
        year,
        topic,
        key + ".focus",
        stance,
        urgency,
        confidence,
        directEvidenceCount,
        stance == OverallTopicSnapshot.Stance.PRESSURED ? null : key + ".opportunity",
        stance == OverallTopicSnapshot.Stance.PRESSURED ? key + ".risk" : null,
        List.of(key + ".act", key + ".fallback"),
        List.of("annual." + key + ".direct", "natal." + key + ".base"));
  }
}
