package com.bazi.app.dto;

public record TrueSolarDto(
    String original,
    String adjusted,
    int offsetMinutes,
    double eotMinutes,
    double longitude,
    String originalShichen,
    String adjustedShichen,
    boolean boundaryChanged) {
}
