package com.bazi.app.report;

import com.bazi.app.config.BusinessException;
import java.util.Arrays;

public enum ReportEdition {
  PLAIN("plain", "通俗版"),
  PROFESSIONAL("professional", "专业版");

  private final String code;
  private final String label;

  ReportEdition(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String code() {
    return code;
  }

  public String label() {
    return label;
  }

  public static ReportEdition fromCode(String code) {
    return Arrays.stream(values())
        .filter(value -> value.code.equals(code))
        .findFirst()
        .orElseThrow(() -> new BusinessException("REPORT_EDITION_UNSUPPORTED", "不支持的命书版本"));
  }
}
