package com.bazi.app.report;

import java.util.Objects;

public record AnnualTransition(int fromYear, int toYear, String relation) {

  public AnnualTransition {
    Objects.requireNonNull(relation, "relation");
    if (toYear != fromYear + 1) {
      throw new IllegalArgumentException("annual transition years must be consecutive");
    }
  }
}
