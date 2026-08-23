package com.bazi.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportPreviewRequest(
    @NotNull @Valid PaipanRequest request,
    @NotBlank String topic,
    @NotBlank String edition,
    @Valid CareerContextRequest careerContext,
    @Deprecated
    @Valid WealthContextRequest wealthContext) {
}
