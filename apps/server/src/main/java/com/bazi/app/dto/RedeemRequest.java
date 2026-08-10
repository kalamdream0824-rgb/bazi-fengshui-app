package com.bazi.app.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemRequest(@NotBlank String code) {
}
