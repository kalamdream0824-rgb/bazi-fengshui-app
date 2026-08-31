package com.bazi.app.report;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record NarrativeTimeline(
    PastReview past,
    PresentReading present,
    List<FutureStep> future) {

  public NarrativeTimeline {
    Objects.requireNonNull(past, "past");
    Objects.requireNonNull(present, "present");
    future = future == null ? List.of() : List.copyOf(future);
    if (past.year() != present.year() - 1) {
      throw new IllegalArgumentException("past year must be immediately before present year");
    }
    int expectedYear = present.year() + 1;
    for (FutureStep step : future) {
      Objects.requireNonNull(step, "future step");
      if (step.year() != expectedYear) {
        throw new IllegalArgumentException("future years must be continuous after present year");
      }
      expectedYear++;
    }
    rejectDuplicateCopy(past, present, future);
  }

  private static void rejectDuplicateCopy(
      PastReview past,
      PresentReading present,
      List<FutureStep> future) {
    List<String> copies = new ArrayList<>();
    copies.add(past.headline());
    copies.addAll(past.checkpoints());
    copies.add(past.bridge());
    copies.add(present.headline());
    copies.add(present.judgment());
    copies.add(present.priority());
    for (FutureStep step : future) {
      copies.add(step.headline());
      copies.add(step.action());
    }
    Set<String> unique = new HashSet<>();
    for (String copy : copies) {
      if (!unique.add(copy.strip())) {
        throw new IllegalArgumentException("timeline copy must not repeat");
      }
    }
  }

  public record PastReview(
      int year,
      String headline,
      List<String> checkpoints,
      String bridge,
      List<String> evidenceKeys) {

    public PastReview {
      headline = requireText(headline, "past headline");
      bridge = requireText(bridge, "past bridge");
      checkpoints = immutableTexts(checkpoints, "past checkpoints");
      if (checkpoints.size() != 2 || checkpoints.get(0).equals(checkpoints.get(1))) {
        throw new IllegalArgumentException("past review must contain exactly two distinct checkpoints");
      }
      RetrospectiveLanguagePolicy.validate(checkpoints);
      evidenceKeys = requiredEvidence(evidenceKeys, "past evidenceKeys");
    }
  }

  public record PresentReading(
      int year,
      String headline,
      String judgment,
      String priority,
      List<String> evidenceKeys) {

    public PresentReading {
      headline = requireText(headline, "present headline");
      judgment = requireText(judgment, "present judgment");
      priority = requireText(priority, "present priority");
      evidenceKeys = requiredEvidence(evidenceKeys, "present evidenceKeys");
    }
  }

  public record FutureStep(
      int year,
      String headline,
      String action,
      List<String> evidenceKeys) {

    public FutureStep {
      headline = requireText(headline, "future headline");
      action = requireText(action, "future action");
      evidenceKeys = requiredEvidence(evidenceKeys, "future evidenceKeys");
    }
  }

  private static List<String> requiredEvidence(List<String> values, String field) {
    List<String> result = immutableTexts(values, field);
    if (result.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be empty");
    }
    return result;
  }

  private static List<String> immutableTexts(List<String> values, String field) {
    Objects.requireNonNull(values, field);
    return values.stream().map(value -> requireText(value, field)).toList();
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.strip();
  }
}
