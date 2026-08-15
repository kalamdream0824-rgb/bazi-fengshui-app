package com.bazi.app.dto;

import java.util.List;
import java.util.Map;

public record PaipanResultDto(
    String solarText,
    String lunarText,
    String shengXiao,
    String timeZhi,
    Map<String, PillarDto> pillars,
    String taiYuan,
    String taiYuanNaYin,
    String mingGong,
    String mingGongNaYin,
    String shenGong,
    String shenGongNaYin,
    Map<String, Integer> wuXing,
    List<DaYunDto> daYun,
    YunStartDto yunStart,
    List<LiuNianItemDto> liuNianList,
    List<XiaoYunItemDto> xiaoYunList,
    List<LiuYueItemDto> currentYearLiuYue,
    String currentYearGanZhi,
    LiuNianDto currentLiuNian,
    TrueSolarDto trueSolar) {
}
