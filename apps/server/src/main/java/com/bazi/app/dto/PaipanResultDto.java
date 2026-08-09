package com.bazi.app.dto;

import java.util.List;
import java.util.Map;

public record PaipanResultDto(
    String solarText,
    String lunarText,
    String shengXiao,
    String timeZhi,
    Map<String, PillarDto> pillars,
    Map<String, Integer> wuXing,
    List<DaYunDto> daYun,
    String currentYearGanZhi,
    LiuNianDto currentLiuNian,
    TrueSolarDto trueSolar) {
}
