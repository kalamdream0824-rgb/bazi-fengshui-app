package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.report.wealth.v3.WealthChineseCopyPolicy;
import com.bazi.app.report.wealth.v3.WealthHeadlineVocabulary;
import org.junit.jupiter.api.Test;

class WealthChineseCopyPolicyTest {

  private final WealthChineseCopyPolicy policy = new WealthChineseCopyPolicy();

  @Test
  void rejectsAbstractGlueAndUnresolvedReferencesInAnnualHeadlines() {
    assertThrows(IllegalArgumentException.class, () -> policy.validateAnnualHeadline(
        "临时开销余地好坏条件同时出现，重点看每次留下的钱。"));
    assertThrows(IllegalArgumentException.class, () -> policy.validateAnnualHeadline(
        "今年这方面有改善空间。先观察再判断。"));
  }

  @Test
  void acceptsEveryMeaningFirstHeadlineInTheClosedCatalog() {
    for (var entry : WealthHeadlineVocabulary.entries()) {
      assertDoesNotThrow(() -> policy.validateAnnualHeadline(entry.text()), entry.themeKey());
    }
  }
}
