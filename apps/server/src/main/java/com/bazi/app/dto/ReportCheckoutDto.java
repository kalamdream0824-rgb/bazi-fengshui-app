package com.bazi.app.dto;

public record ReportCheckoutDto(
    Long orderId,
    Long reportId,
    String topic,
    String edition,
    int amountCents,
    String status) {
}
