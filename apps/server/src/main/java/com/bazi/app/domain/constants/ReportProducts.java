package com.bazi.app.domain.constants;

import com.bazi.app.config.BusinessException;

/** Server-owned catalog for report products that are ready to sell. */
public final class ReportProducts {

  private static final int PLAIN_AMOUNT_CENTS = 690;

  private ReportProducts() {}

  public static ReportProduct of(String topic, String edition) {
    if (!"plain".equals(edition)) {
      throw unavailable();
    }
    return switch (topic) {
      case "career" -> new ReportProduct(
          "report_career_plain", "career", "plain", PLAIN_AMOUNT_CENTS);
      case "wealth" -> new ReportProduct(
          "report_wealth_plain", "wealth", "plain", PLAIN_AMOUNT_CENTS);
      case "relationship" -> new ReportProduct(
          "report_relationship_plain", "relationship", "plain", PLAIN_AMOUNT_CENTS);
      default -> throw unavailable();
    };
  }

  private static BusinessException unavailable() {
    return new BusinessException("REPORT_PRODUCT_UNAVAILABLE", "这个主题或版本暂未开放购买");
  }

  public record ReportProduct(
      String code,
      String topic,
      String edition,
      int amountCents) {}
}
