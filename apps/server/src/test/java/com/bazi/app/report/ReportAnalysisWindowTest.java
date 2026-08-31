package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportAnalysisWindowTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-09-01T00:00:00Z"),
      ZoneId.of("Asia/Shanghai"));

  @Test
  void keepsPreviousYearOutsideTheConfiguredProductYears() {
    PaipanRequest request = fixtureRequest();
    PaipanResultDto chart = new BaziService().paipan(request);
    AnnualContextFactory factory = new AnnualContextFactory(CLOCK);
    List<AnnualContext> productYears = factory.create(request, chart, ReportHorizon.of(3));

    ReportAnalysisWindow window = new ReportAnalysisWindow(
        factory.createYear(request, chart, 2025),
        productYears);

    assertEquals(2025, window.previous().year());
    assertEquals(List.of(2026, 2027, 2028), window.productYears().stream()
        .map(AnnualContext::year)
        .toList());
    assertEquals(3, window.productYears().size());
    assertThrows(UnsupportedOperationException.class,
        () -> window.productYears().add(window.previous()));
  }

  @Test
  void rejectsPreviousYearThatDoesNotImmediatelyPrecedeProductYears() {
    PaipanRequest request = fixtureRequest();
    PaipanResultDto chart = new BaziService().paipan(request);
    AnnualContextFactory factory = new AnnualContextFactory(CLOCK);

    assertThrows(IllegalArgumentException.class, () -> new ReportAnalysisWindow(
        factory.createYear(request, chart, 2024),
        factory.create(request, chart, ReportHorizon.of(2))));
  }

  @Test
  void rejectsEmptyOrNonContinuousProductYears() {
    PaipanRequest request = fixtureRequest();
    PaipanResultDto chart = new BaziService().paipan(request);
    AnnualContextFactory factory = new AnnualContextFactory(CLOCK);

    assertThrows(IllegalArgumentException.class,
        () -> new ReportAnalysisWindow(factory.createYear(request, chart, 2025), List.of()));
    assertThrows(IllegalArgumentException.class, () -> new ReportAnalysisWindow(
        factory.createYear(request, chart, 2025),
        List.of(factory.createYear(request, chart, 2026), factory.createYear(request, chart, 2028))));
  }

  private PaipanRequest fixtureRequest() {
    return new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
  }
}
