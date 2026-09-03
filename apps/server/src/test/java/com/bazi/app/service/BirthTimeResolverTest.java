package com.bazi.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.config.BusinessException;
import com.bazi.app.dto.PaipanRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BirthTimeResolverTest {

  private final BirthTimeResolver resolver =
      new BirthTimeResolver(new BirthPlaceRegistry());

  @Test
  void disabledCorrectionReturnsOriginalTimeWithoutMetadata() {
    PaipanRequest request = request("1995-10-08T13:05:00", "广东省 深圳市", false);

    ResolvedBirthTime result = resolver.resolve(request);

    assertEquals(LocalDateTime.of(1995, 10, 8, 13, 5), result.original());
    assertEquals(result.original(), result.effective());
    assertNull(result.metadata());
  }

  @Test
  void shenzhenCorrectionCrossesFromWeiToWuHour() {
    ResolvedBirthTime result = resolver.resolve(
        request("1995-10-08T13:05:00", "广东省 深圳市", true));

    assertEquals(LocalDateTime.of(1995, 10, 8, 12, 53, 51), result.effective());
    assertEquals("未", result.metadata().originalShichen());
    assertEquals("午", result.metadata().adjustedShichen());
    assertEquals(true, result.metadata().boundaryChanged());
  }

  @Test
  void correctionKeepsHourBoundaryWhenBothTimesRemainWei() {
    ResolvedBirthTime result = resolver.resolve(
        request("1995-10-08T14:30:00", "广东省 深圳市", true));

    assertEquals("未", result.metadata().originalShichen());
    assertEquals("未", result.metadata().adjustedShichen());
    assertFalse(result.metadata().boundaryChanged());
  }

  @Test
  void correctionCanCrossToPreviousCivilDate() {
    ResolvedBirthTime result = resolver.resolve(
        request("2024-02-29T00:05:00", "新疆维吾尔自治区 乌鲁木齐市", true));

    assertEquals(LocalDateTime.of(2024, 2, 28, 21, 42, 23), result.effective());
  }

  @Test
  void leapYearEquationUses366DayDenominator() {
    double result = BirthTimeResolver.equationOfTimeMinutes(
        LocalDateTime.of(2024, 2, 29, 0, 5));

    assertEquals(-13.0145, result, 0.0001);
  }

  @Test
  void metadataPreservesCalculationInputsAndOffsets() {
    ResolvedBirthTime result = resolver.resolve(
        request("1995-10-08T13:05:00", "广东省 深圳市", true));

    assertEquals("1995-10-08T13:05", result.metadata().original());
    assertEquals("1995-10-08T12:53", result.metadata().adjusted());
    assertEquals(-24, result.metadata().offsetMinutes());
    assertEquals(12.7, result.metadata().eotMinutes(), 0.0001);
    assertEquals(114.05, result.metadata().longitude(), 0.0001);
  }

  @Test
  void invalidCivilTimeReturnsStableBusinessError() {
    BusinessException error = assertThrows(
        BusinessException.class,
        () -> resolver.resolve(request("1995-02-30T13:05:00", "广东省 深圳市", true)));

    assertEquals("BIRTH_TIME_INVALID", error.getCode());
  }

  private PaipanRequest request(String time, String place, boolean enabled) {
    return new PaipanRequest("测试", "male", time, place, enabled);
  }
}
