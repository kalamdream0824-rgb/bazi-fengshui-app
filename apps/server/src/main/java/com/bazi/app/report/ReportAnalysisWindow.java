package com.bazi.app.report;

import java.util.List;
import java.util.Objects;

public record ReportAnalysisWindow(
    AnnualContext previous,
    List<AnnualContext> productYears) {

  public ReportAnalysisWindow {
    Objects.requireNonNull(previous, "previous");
    Objects.requireNonNull(productYears, "productYears");
    productYears = List.copyOf(productYears);
    if (productYears.isEmpty()) {
      throw new IllegalArgumentException("productYears must not be empty");
    }
    if (previous.year() != productYears.get(0).year() - 1) {
      throw new IllegalArgumentException("previous year must immediately precede product years");
    }
    int expectedYear = productYears.get(0).year();
    for (AnnualContext context : productYears) {
      Objects.requireNonNull(context, "product year");
      if (context.year() != expectedYear) {
        throw new IllegalArgumentException("product years must be continuous");
      }
      expectedYear++;
    }
  }
}
