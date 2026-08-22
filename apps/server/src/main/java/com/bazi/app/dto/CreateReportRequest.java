package com.bazi.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReportRequest(
    @NotNull Long recordId,
    @NotBlank String topic,
    @NotBlank String edition) {
}
