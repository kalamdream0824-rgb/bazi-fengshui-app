package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.config.BusinessException;
import com.bazi.app.domain.constants.ReportProducts;
import org.junit.jupiter.api.Test;

class ReportProductsTest {

  @Test
  void allReleasedPlainTopicsUseServerPrice() {
    assertProduct("career", "report_career_plain");
    assertProduct("wealth", "report_wealth_plain");
    assertProduct("relationship", "report_relationship_plain");
  }

  @Test
  void unreleasedTopicsAndProfessionalEditionCannotBeSold() {
    assertUnavailable("overall", "plain");
    assertUnavailable("career", "professional");
    assertUnavailable("wealth", "professional");
    assertUnavailable("relationship", "professional");
  }

  private void assertProduct(String topic, String expectedCode) {
    var product = ReportProducts.of(topic, "plain");
    assertEquals(expectedCode, product.code());
    assertEquals(topic, product.topic());
    assertEquals("plain", product.edition());
    assertEquals(690, product.amountCents());
  }

  private void assertUnavailable(String topic, String edition) {
    BusinessException error = assertThrows(
        BusinessException.class, () -> ReportProducts.of(topic, edition));
    assertEquals("REPORT_PRODUCT_UNAVAILABLE", error.getCode());
  }
}
