package com.bazi.app.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(@NotBlank String plan) {
}
