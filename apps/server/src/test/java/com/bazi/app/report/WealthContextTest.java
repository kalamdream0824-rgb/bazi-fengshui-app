package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.config.BusinessException;
import org.junit.jupiter.api.Test;

class WealthContextTest {

  @Test
  void buildsACompleteWealthRealityContextFromApiCodes() {
    WealthContext context = WealthContext.fromCodes(
        "salary", "increase_income", "income_fluctuating");

    assertEquals("固定工资为主", context.incomeSource().label());
    assertEquals("希望增加收入", context.goal().label());
    assertEquals("近期收入有波动", context.pace().label());
  }

  @Test
  void rejectsUnknownWealthQuestionnaireOptions() {
    assertThrows(BusinessException.class, () ->
        WealthContext.fromCodes("investment", "increase_income", "stable"));
  }
}
