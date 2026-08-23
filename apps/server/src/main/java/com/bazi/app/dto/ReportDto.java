package com.bazi.app.dto;

import com.bazi.app.report.ReportContent;
import java.time.LocalDateTime;

public record ReportDto(
    Long id,
    String subject,
    String topic,
    String edition,
    String status,
    String contentVersion,
    ReportContent content,
    LocalDateTime createdAt,
    LocalDateTime generatedAt) {
}
