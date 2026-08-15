package com.bazi.app.dto;

public record YunStartDto(
    int year,
    int month,
    int day,
    int hour,
    boolean forward) {
}
