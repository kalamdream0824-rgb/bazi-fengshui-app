package com.bazi.app.report.wealth.v3;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Domain failure for a report-level headline plan; messages never contain user input. */
public final class WealthHeadlinePlanningException extends RuntimeException {
  private final Reason reason;
  private final Map<Integer, Integer> candidateCounts;

  public WealthHeadlinePlanningException(Reason reason, Map<Integer, Integer> candidateCounts) {
    super("wealth headline planning failed: " + reason.name().toLowerCase());
    this.reason = reason;
    this.candidateCounts = Collections.unmodifiableMap(new LinkedHashMap<>(candidateCounts));
  }

  public Reason reason() {
    return reason;
  }

  public Map<Integer, Integer> candidateCounts() {
    return candidateCounts;
  }

  public enum Reason {
    INSUFFICIENT_DIVERSITY
  }
}
