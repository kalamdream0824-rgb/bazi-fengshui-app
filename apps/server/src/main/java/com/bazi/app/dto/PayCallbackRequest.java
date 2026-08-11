package com.bazi.app.dto;

import jakarta.validation.constraints.NotNull;

/** 真实支付渠道回调请求（当前为占位，资质接入后启用签名校验）。 */
public record PayCallbackRequest(
    @NotNull Long orderId,
    @NotNull String provider,
    @NotNull String providerTradeNo) {
}
