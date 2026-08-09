package com.bazi.app.dto;

import java.time.LocalDateTime;

public record RecordDto(Long id, PaipanRequest request, PaipanResultDto result, LocalDateTime createdAt) {
}
