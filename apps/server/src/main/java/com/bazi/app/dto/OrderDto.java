package com.bazi.app.dto;

import com.bazi.app.domain.Order;
import java.time.LocalDateTime;

public record OrderDto(
    Long id,
    String plan,
    int amountCents,
    String status,
    String provider,
    String providerTradeNo,
    LocalDateTime createdAt,
    LocalDateTime paidAt) {

  public static OrderDto from(Order order) {
    return new OrderDto(
        order.getId(),
        order.getPlan(),
        order.getAmountCents() == null ? 0 : order.getAmountCents(),
        order.getStatus(),
        order.getProvider(),
        order.getProviderTradeNo(),
        order.getCreatedAt(),
        order.getPaidAt());
  }
}
