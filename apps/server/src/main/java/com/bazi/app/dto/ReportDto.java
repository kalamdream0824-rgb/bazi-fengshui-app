package com.bazi.app.dto;

import java.time.LocalDateTime;

public record ReportDto(
    Long id,
    Long recordId,
    String topic,
    String edition,
    String status,
    String contentVersion,
    LocalDateTime createdAt,
    LocalDateTime generatedAt) {
}
