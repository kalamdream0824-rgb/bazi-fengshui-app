package com.bazi.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.config.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
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
  void rejectsUnknownNameEvenWhenItStartsWithAKnownCity() {
    BusinessException error = assertThrows(
        BusinessException.class,
        () -> registry.longitudeOf("广东省 深圳火星市"));

    assertEquals("BIRTH_PLACE_UNRESOLVED", error.getCode());
  }

  @Test
  void rejectsProvinceOnlyInsteadOfFallingBackToCapital() {
    BusinessException error = assertThrows(
        BusinessException.class,
        () -> registry.longitudeOf("广东省"));

    assertEquals("BIRTH_PLACE_UNRESOLVED", error.getCode());
  }

  @Test
  void backendAndFrontendCoordinateFilesStayIdentical() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode backend = mapper.readTree(
        Path.of("src", "main", "resources", "geo", "city-geo.json").toFile());
    JsonNode frontend = mapper.readTree(
        Path.of("..", "web", "src", "data", "cityGeo.json").toFile());

    assertEquals(frontend, backend);
  }
}
