package com.bazi.app.dto;

import java.util.List;

public record LiuNianItemDto(
    int year,
    int age,
    String ganZhi,
    String naYin,
    String shiShen,
    String starFortune,
    String xunKong,
    List<String> shenSha) {
}
