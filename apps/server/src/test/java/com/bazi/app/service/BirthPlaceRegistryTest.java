package com.bazi.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.config.BusinessException;
import org.junit.jupiter.api.Test;

class BirthPlaceRegistryTest {

  private final BirthPlaceRegistry registry = new BirthPlaceRegistry();

  @Test
  void resolvesSelectedPrefectureCityLongitude() {
    assertEquals(114.05, registry.longitudeOf("广东省 深圳市"), 0.0001);
  }


  @Test
  void resolvesMunicipalityDistrictToMunicipalityLongitude() {
    assertEquals(121.47, registry.longitudeOf("上海市 黄浦区"), 0.0001);
  }

  @Test
  void rejectsUnsupportedLocation() {
    BusinessException error = assertThrows(
        BusinessException.class,
        () -> registry.longitudeOf("广东省 火星市"));

    assertEquals("BIRTH_PLACE_UNRESOLVED", error.getCode());
  }

  @Test
  void rejectsProvinceOnlyInsteadOfFallingBackToCapital() {
    BusinessException error = assertThrows(
        BusinessException.class,
        () -> registry.longitudeOf("广东省"));

    assertEquals("BIRTH_PLACE_UNRESOLVED", error.getCode());
  }
}
