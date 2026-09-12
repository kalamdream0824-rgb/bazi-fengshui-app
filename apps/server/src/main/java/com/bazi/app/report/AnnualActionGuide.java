package com.bazi.app.report;

import java.util.List;
import java.util.Objects;

/** A topic-owned action loop with an observable outcome and a fallback. */
public record AnnualActionGuide(
    String focusKey,
    String problem,
    String action,
    String expectedChange,
    String checkTiming,
    String successSignal,
    String adjustmentCondition,
    String fallbackAction,
    List<String> evidenceKeys) {

  public AnnualActionGuide {
    focusKey = focusKey == null || focusKey.isBlank() ? "legacy.unspecified" : focusKey;
    problem = requireText(problem, "problem");
    action = requireText(action, "action");
    expectedChange = requireText(expectedChange, "expectedChange");
    checkTiming = requireText(checkTiming, "checkTiming");
    successSignal = requireText(successSignal, "successSignal");
    adjustmentCondition = requireText(adjustmentCondition, "adjustmentCondition");
    fallbackAction = requireText(fallbackAction, "fallbackAction");
    evidenceKeys = evidenceKeys == null ? List.of() : List.copyOf(evidenceKeys);
    if (evidenceKeys.isEmpty() || evidenceKeys.stream().anyMatch(key -> key == null || key.isBlank())) {
      throw new IllegalArgumentException("annual action guide requires evidence keys");
    }
  }

  /** Keeps already stored v5 snapshots and source callers readable. */
  public AnnualActionGuide(
      String problem,
      String action,
      String expectedChange,
      String checkTiming,
      String successSignal,
      String adjustmentCondition,
      String fallbackAction,
      List<String> evidenceKeys) {
    this("legacy.unspecified", problem, action, expectedChange, checkTiming, successSignal,
        adjustmentCondition, fallbackAction, evidenceKeys);
  }

  public List<String> lines() {
    return List.of(problem, action, expectedChange, checkTiming, successSignal,
        adjustmentCondition, fallbackAction);
  }

  private static String requireText(String text, String field) {
    Objects.requireNonNull(text, field);
    if (text.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    return text;
  }
}
