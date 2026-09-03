package com.bazi.app.service;

import com.bazi.app.dto.TrueSolarDto;
import java.time.LocalDateTime;

public record ResolvedBirthTime(
    LocalDateTime original,
    LocalDateTime effective,
    TrueSolarDto metadata) {
}
