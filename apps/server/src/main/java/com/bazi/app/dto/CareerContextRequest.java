package com.bazi.app.dto;

import com.bazi.app.report.CareerContext;

public record CareerContextRequest(
    String status,
    String goal,
    String pace) {

  public CareerContext toDomain() {
    return CareerContext.fromCodes(status, goal, pace);
  }
}
