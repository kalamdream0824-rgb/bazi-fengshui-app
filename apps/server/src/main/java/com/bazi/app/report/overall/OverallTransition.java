package com.bazi.app.report.overall;

import java.util.Objects;

public record OverallTransition(int fromYear, int toYear, String relation) {
  public OverallTransition {
    Objects.requireNonNull(relation, "relation");
    if (toYear != fromYear + 1) {
      throw new IllegalArgumentException("overall transitions require adjacent years");
    }
  }
}
