package com.bazi.app.dto;

import java.util.List;

public record LiuYueItemDto(
    int index,
    String monthName,
    String ganZhi,
    String naYin,
    String xunKong,
    String shiShen,
    List<String> shenSha) {
}
