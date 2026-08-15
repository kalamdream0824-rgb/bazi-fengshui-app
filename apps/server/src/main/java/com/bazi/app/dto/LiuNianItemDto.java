package com.bazi.app.dto;

import java.util.List;

public record LiuNianItemDto(
    int year,
    int age,
    String ganZhi,
    String naYin,
    String shiShen,
    List<String> shenSha) {
}
