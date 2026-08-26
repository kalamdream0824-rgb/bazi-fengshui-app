package com.bazi.app.report;

public record ReportHorizon(int years) {

  public static final int MIN_YEARS = 2;
  public static final int MAX_YEARS = 5;
  public static final ReportHorizon WEALTH_PRODUCT = new ReportHorizon(3);
  public static final ReportHorizon RELATIONSHIP_PRODUCT = new ReportHorizon(3);

  public ReportHorizon {
    if (years < MIN_YEARS || years > MAX_YEARS) {
      throw new IllegalArgumentException("report horizon must be between 2 and 5 years");
    }
  }

  public static ReportHorizon of(int years) {
    return new ReportHorizon(years);
  }
}
