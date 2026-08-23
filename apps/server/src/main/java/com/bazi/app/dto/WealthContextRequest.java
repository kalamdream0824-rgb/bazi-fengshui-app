package com.bazi.app.dto;

import com.bazi.app.report.WealthContext;

public record WealthContextRequest(
    String incomeSource,
    String goal,
    String pace) {

  public WealthContext toDomain() {
    return WealthContext.fromCodes(incomeSource, goal, pace);
  }
}
