package com.bazi.app.dto;

import java.util.List;

public record PillarDto(
    String label,
    String gan,
    String zhi,
    String shiShen,
    List<HideGanDto> hideGan,
    String naYin,
    String diShi,
    String xunKong,
    List<String> shenSha) {
}
