package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnnualPeriodAssessorTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private PaipanRequest request;
  private PaipanResultDto chart;
  private AnnualPeriodAssessor assessor;

  @BeforeEach
  void setUp() {
    request = new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
    chart = new BaziService().paipan(request);
    assessor = new AnnualPeriodAssessor(CLOCK, new AnnualRuleCatalog());
  }

  @Test
  void wealthProductAssessesExactlyThreeYears() {
    AnnualPeriodAssessment result = assessor.assess(
        request, chart, ReportTopic.WEALTH, ReportHorizon.WEALTH_PRODUCT);

    assertEquals(List.of(2026, 2027, 2028),
        result.years().stream().map(YearAssessment::year).toList());
    assertEquals(2, result.transitions().size());
  }

  @Test
  void technicalFiveYearHorizonProducesFourAdjacentTransitions() {
    AnnualPeriodAssessment result = assessor.assess(
        request, chart, ReportTopic.WEALTH, ReportHorizon.of(5));

    assertEquals(5, result.years().size());
    assertEquals(4, result.transitions().size());
    assertEquals(2026, result.transitions().get(0).fromYear());
    assertEquals(2030, result.transitions().get(3).toYear());
  }
}
