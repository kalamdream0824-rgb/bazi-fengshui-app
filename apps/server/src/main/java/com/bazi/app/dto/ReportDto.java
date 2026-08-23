package com.bazi.app.dto;

import com.bazi.app.report.CareerNarrativePlan;
import java.time.LocalDateTime;

public record ReportDto(
    Long id,
    String subject,
    String topic,
    String edition,
    String status,
    String contentVersion,
    CareerNarrativePlan content,
    LocalDateTime createdAt,
    LocalDateTime generatedAt) {
}
