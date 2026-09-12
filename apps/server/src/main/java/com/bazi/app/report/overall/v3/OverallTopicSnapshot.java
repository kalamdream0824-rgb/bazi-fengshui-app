package com.bazi.app.report.overall.v3;

import com.bazi.app.report.ConfidenceLevel;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** A prose-free, read-only topic judgment consumed by the overall orchestrator. */
public record OverallTopicSnapshot(
    int year,
    Topic topic,
    String focusKey,
    Stance stance,
    Urgency urgency,
    ConfidenceLevel confidence,
    String opportunityKey,
    String riskKey,
    List<String> actionCandidateKeys,
    List<String> evidenceKeys) {

  public OverallTopicSnapshot {
    if (year <= 0) throw new IllegalArgumentException("snapshot year must be positive");
    Objects.requireNonNull(topic, "topic");
    focusKey = required(focusKey, "snapshot focus key");
    Objects.requireNonNull(stance, "stance");
    Objects.requireNonNull(urgency, "urgency");
    Objects.requireNonNull(confidence, "confidence");
    opportunityKey = optional(opportunityKey);
    riskKey = optional(riskKey);
    actionCandidateKeys = distinctRequired(actionCandidateKeys, "action candidate keys", 1);
    evidenceKeys = distinctRequired(evidenceKeys, "snapshot evidence keys", 2);
  }

  public enum Topic {
    RHYTHM,
    CAREER,
    WEALTH,
    RELATIONSHIP
  }

  public enum Stance {
    SUPPORTIVE,
    BALANCED,
    MIXED,
    PRESSURED
  }

  public enum Urgency {
    LOW,
    MEDIUM,
    HIGH
  }

  static String required(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    return value.trim();
  }

  static List<String> distinctRequired(List<String> values, String label, int minimum) {
    if (values == null || values.size() < minimum) {
      throw new IllegalArgumentException(label + " requires at least " + minimum + " values");
    }
    List<String> result = values.stream().map(value -> required(value, label)).toList();
    if (new HashSet<>(result).size() != result.size()) {
      throw new IllegalArgumentException(label + " must not contain duplicates");
    }
    return List.copyOf(result);
  }

  private static String optional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
