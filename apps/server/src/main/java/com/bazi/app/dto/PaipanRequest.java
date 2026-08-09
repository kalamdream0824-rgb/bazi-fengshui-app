package com.bazi.app.dto;

import jakarta.validation.constraints.NotBlank;

public record PaipanRequest(
    String name,
    @NotBlank String gender,
    @NotBlank String solarDateTime,
    String birthPlace,
    boolean trueSolarTime) {

  public boolean isMale() {
    return "male".equals(gender);
  }
}
