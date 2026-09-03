package com.bazi.app.service;

import com.bazi.app.config.BusinessException;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.TrueSolarDto;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class BirthTimeResolver {

  private static final int CHINA_TIME_ZONE_HOURS = 8;
  private static final DateTimeFormatter DISPLAY_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  private final BirthPlaceRegistry birthPlaceRegistry;

  public BirthTimeResolver(BirthPlaceRegistry birthPlaceRegistry) {
    this.birthPlaceRegistry = Objects.requireNonNull(birthPlaceRegistry);
  }

  public ResolvedBirthTime resolve(PaipanRequest request) {
    Objects.requireNonNull(request, "request");
    LocalDateTime original = parse(request.solarDateTime());
    if (!request.trueSolarTime()) {
      return new ResolvedBirthTime(original, original, null);
    }

    double longitude = birthPlaceRegistry.longitudeOf(request.birthPlace());
    double longitudeMinutes = 4 * longitude - 60 * CHINA_TIME_ZONE_HOURS;
    double eotMinutes = equationOfTimeMinutes(original);
    long totalOffsetSeconds = Math.round((longitudeMinutes + eotMinutes) * 60);
    LocalDateTime effective = original.plusSeconds(totalOffsetSeconds);
    String originalShichen = shichen(original.getHour());
    String adjustedShichen = shichen(effective.getHour());
    TrueSolarDto metadata = new TrueSolarDto(
        DISPLAY_TIME.format(original),
        DISPLAY_TIME.format(effective),
        (int) Math.round(longitudeMinutes),
        roundToOneDecimal(eotMinutes),
        longitude,
        originalShichen,
        adjustedShichen,
        !originalShichen.equals(adjustedShichen));
    return new ResolvedBirthTime(original, effective, metadata);
  }

  static double equationOfTimeMinutes(LocalDateTime time) {
    int daysInYear = Year.isLeap(time.getYear()) ? 366 : 365;
    double hour = time.getHour()
        + time.getMinute() / 60.0
        + time.getSecond() / 3600.0
        + time.getNano() / 3_600_000_000_000.0;
    double gamma = 2 * Math.PI / daysInYear
        * (time.getDayOfYear() - 1 + (hour - 12) / 24.0);
    return 229.18 * (0.000075
        + 0.001868 * Math.cos(gamma)
        - 0.032077 * Math.sin(gamma)
        - 0.014615 * Math.cos(2 * gamma)
        - 0.040849 * Math.sin(2 * gamma));
  }

  private LocalDateTime parse(String value) {
    try {
      return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (DateTimeParseException | NullPointerException error) {
      throw new BusinessException("BIRTH_TIME_INVALID", "出生时间格式不正确");
    }
  }

  private static double roundToOneDecimal(double value) {
    return Math.round(value * 10) / 10.0;
  }

  private static String shichen(int hour) {
    return switch (hour) {
      case 23, 0 -> "子";
      case 1, 2 -> "丑";
      case 3, 4 -> "寅";
      case 5, 6 -> "卯";
      case 7, 8 -> "辰";
      case 9, 10 -> "巳";
      case 11, 12 -> "午";
      case 13, 14 -> "未";
      case 15, 16 -> "申";
      case 17, 18 -> "酉";
      case 19, 20 -> "戌";
      default -> "亥";
    };
  }
}
