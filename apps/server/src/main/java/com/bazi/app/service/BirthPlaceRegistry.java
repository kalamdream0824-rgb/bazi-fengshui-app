package com.bazi.app.service;

import com.bazi.app.config.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BirthPlaceRegistry {

  private static final String COORDINATE_RESOURCE = "/geo/city-geo.json";
  private static final Map<String, String> MUNICIPALITY_KEYS = Map.of(
      "北京市", "北京",
      "天津市", "天津",
      "上海市", "上海",
      "重庆市", "重庆");

  private final Map<String, Double> longitudeByCity;

  public BirthPlaceRegistry() {
    this.longitudeByCity = loadCoordinates();
  }

  public double longitudeOf(String birthPlace) {
    String[] parts = normalizeWhitespace(birthPlace).split(" ");
    if (parts.length != 2) {
      throw unresolved();
    }

    String municipalityKey = MUNICIPALITY_KEYS.get(parts[0]);
    if (municipalityKey != null) {
      return requiredLongitude(municipalityKey);
    }

    String normalizedCity = stripAdministrativeSuffix(parts[1]);
    Double exact = longitudeByCity.get(normalizedCity);
    if (exact != null) {
      return exact;
    }
    if (!parts[1].matches(".*(自治州|地区|自治盟|盟)$")) {
      throw unresolved();
    }
    return longitudeByCity.entrySet().stream()
        .filter(entry -> normalizedCity.startsWith(entry.getKey()))
        .max(Map.Entry.comparingByKey((left, right) -> Integer.compare(left.length(), right.length())))
        .map(Map.Entry::getValue)
        .orElseThrow(BirthPlaceRegistry::unresolved);
  }

  private double requiredLongitude(String cityKey) {
    Double longitude = longitudeByCity.get(cityKey);
    if (longitude == null) {
      throw new IllegalStateException("missing municipality coordinate: " + cityKey);
    }
    return longitude;
  }

  private Map<String, Double> loadCoordinates() {
    try (InputStream input = BirthPlaceRegistry.class.getResourceAsStream(COORDINATE_RESOURCE)) {
      if (input == null) {
        throw new IllegalStateException("missing city coordinate resource: " + COORDINATE_RESOURCE);
      }
      JsonNode root = new ObjectMapper().readTree(input);
      Map<String, Double> coordinates = new LinkedHashMap<>();
      root.properties().forEach(entry ->
          coordinates.put(entry.getKey(), entry.getValue().path("lng").asDouble()));
      return Map.copyOf(coordinates);
    } catch (IOException error) {
      throw new IllegalStateException("cannot load city coordinate resource", error);
    }
  }

  private static String normalizeWhitespace(String value) {
    if (value == null) {
      return "";
    }
    return value.trim().replaceAll("\\s+", " ");
  }

  private static String stripAdministrativeSuffix(String city) {
    return city.replaceFirst("(自治州|地区|自治盟|盟|市|州)$", "");
  }

  private static BusinessException unresolved() {
    return new BusinessException("BIRTH_PLACE_UNRESOLVED", "无法识别出生城市，请重新选择省市");
  }
}
