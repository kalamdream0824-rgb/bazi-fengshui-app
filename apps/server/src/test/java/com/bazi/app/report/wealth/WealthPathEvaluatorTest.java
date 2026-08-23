package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WealthPathEvaluatorTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private PaipanResultDto chart;
  private List<AnnualContext> threeYears;

  @BeforeEach
  void setUp() {
    PaipanRequest request = new PaipanRequest(
        "林先生", "male", "1995-10-08T14:30:00", "上海", false);
    chart = new BaziService().paipan(request);
    threeYears = new AnnualContextFactory(CLOCK)
        .create(request, chart, ReportHorizon.WEALTH_PRODUCT);
  }

  @Test
  void evaluatesAllFivePathsForEveryProductYear() {
    WealthPeriodEvaluation period = new WealthPathEvaluator()
        .evaluate(new WealthFactExtractor().extract(chart, threeYears));

    assertEquals(3, period.years().size());
    for (WealthYearEvaluation year : period.years()) {
      assertEquals(Set.of(WealthPath.values()), year.paths().keySet());
      assertFalse(year.paths().values().stream().anyMatch(path -> path.status().isBlank()));
    }
    assertTrue(Set.of(WealthPath.PROJECT_INCOME, WealthPath.SKILL_INCOME)
        .contains(period.years().get(0).primaryIncomePath()));
    assertEquals(WealthPath.STABLE_INCOME, period.years().get(1).primaryIncomePath());
  }

  @Test
  void focusLabelRequiresSupportFromTwoIndependentEvidenceFamilies() {
    WealthPeriodEvaluation period = new WealthPathEvaluator()
        .evaluate(new WealthFactExtractor().extract(chart, threeYears));

    period.years().stream()
        .flatMap(year -> year.paths().values().stream())
        .filter(path -> path.status().equals("重点"))
        .forEach(path -> assertTrue(path.supportFamilies().size() >= 2, path.toString()));
  }

  @Test
  void keepsLimitationsAndCountsOneEvidenceKeyOncePerPath() {
    WealthFactExtractor extractor = new WealthFactExtractor();
    WealthYearFacts original = extractor.extract(chart, threeYears).get(0);
    List<WealthEvidence> duplicated = new ArrayList<>(original.evidence());
    duplicated.add(original.evidence().get(0));
    WealthYearFacts withDuplicate = new WealthYearFacts(
        original.year(),
        original.ganZhi(),
        original.annualStemTenGod(),
        original.annualStemGroup(),
        original.activeDayunTenGod(),
        original.natalProfile(),
        duplicated);

    WealthYearEvaluation year = new WealthPathEvaluator().evaluate(List.of(withDuplicate)).years().get(0);

    for (WealthPathEvaluation path : year.paths().values()) {
      List<String> keys = path.scoredEvidence().stream()
          .map(item -> item.evidence().key())
          .toList();
      assertEquals(keys.size(), new HashSet<>(keys).size());
    }
    assertFalse(year.paths().get(WealthPath.RETENTION).limitations().isEmpty());
  }

  @Test
  void technicalFiveYearEvaluationDoesNotChangeTheProductConstant() {
    PaipanRequest request = new PaipanRequest(
        "林先生", "male", "1995-10-08T14:30:00", "上海", false);
    List<AnnualContext> fiveYears = new AnnualContextFactory(CLOCK)
        .create(request, chart, ReportHorizon.of(5));

    WealthPeriodEvaluation period = new WealthPathEvaluator()
        .evaluate(new WealthFactExtractor().extract(chart, fiveYears));

    assertEquals(5, period.years().size());
    assertEquals(3, ReportHorizon.WEALTH_PRODUCT.years());
  }
}
