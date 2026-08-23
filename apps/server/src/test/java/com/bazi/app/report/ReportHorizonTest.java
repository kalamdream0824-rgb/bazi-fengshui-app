package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReportHorizonTest {

  @Test
  void productWealthReportUsesThreeYears() {
    assertEquals(3, ReportHorizon.WEALTH_PRODUCT.years());
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 4, 5})
  void acceptsSupportedHorizons(int years) {
    assertEquals(years, ReportHorizon.of(years).years());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 6})
  void rejectsUnsupportedHorizons(int years) {
    assertThrows(IllegalArgumentException.class, () -> ReportHorizon.of(years));
  }
}
