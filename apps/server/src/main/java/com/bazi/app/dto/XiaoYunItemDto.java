package com.bazi.app.dto;

import java.util.List;

public record XiaoYunItemDto(
    int year,
    int age,
    String ganZhi,
    String naYin,
    String shiShen,
    List<String> shenSha) {
}
