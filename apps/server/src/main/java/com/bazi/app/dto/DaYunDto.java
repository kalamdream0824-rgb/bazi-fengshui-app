package com.bazi.app.dto;

import java.util.List;

public record DaYunDto(
    String ageRange,
    String ganZhi,
    String yearRange,
    boolean isCurrent,
    String naYin,
    String xunKong,
    String shiShen,
    String starFortune,
    List<String> shenSha) {
}
