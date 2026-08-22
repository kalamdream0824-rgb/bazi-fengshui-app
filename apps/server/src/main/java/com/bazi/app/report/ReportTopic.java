package com.bazi.app.report;

import com.bazi.app.config.BusinessException;
import java.util.Arrays;

public enum ReportTopic {
  OVERALL("overall", "综合运势"),
  CAREER("career", "事业运势"),
  WEALTH("wealth", "财富运势"),
  RELATIONSHIP("relationship", "感情运势");

  private final String code;
  private final String label;

  ReportTopic(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String code() {
    return code;
  }

  public String label() {
    return label;
  }

  public static ReportTopic fromCode(String code) {
    return Arrays.stream(values())
        .filter(value -> value.code.equals(code))
        .findFirst()
        .orElseThrow(() -> new BusinessException("REPORT_TOPIC_UNSUPPORTED", "不支持的命书主题"));
  }
}
