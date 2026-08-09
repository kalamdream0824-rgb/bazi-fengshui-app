package com.bazi.app.dto;

import java.util.List;

public record DaYunDto(String ageRange, String ganZhi, String yearRange, boolean isCurrent, List<String> shenSha) {
}
