package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WealthNarrativePlannerTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private WealthNarrativePlan plan;

  @BeforeEach
  void setUp() {
    PaipanRequest request = new PaipanRequest(
        "林先生", "male", "1995-10-08T14:30:00", "上海", false);
    PaipanResultDto chart = new BaziService().paipan(request);
    WealthPeriodEvaluation evaluation = new WealthPathEvaluator().evaluate(
        new WealthFactExtractor().extract(
            chart,
            new AnnualContextFactory(CLOCK)
                .create(request, chart, ReportHorizon.WEALTH_PRODUCT)));
    plan = new WealthNarrativePlanner().plan(evaluation);
  }

  @Test
  void buildsFivePathOverviewAndThreeDistinctAnnualRoles() {
    assertEquals(3, plan.horizonYears());
    assertEquals(5, plan.paths().size());
    assertEquals(3, plan.years().size());
    assertEquals(List.of(2026, 2027, 2028),
        plan.years().stream().map(WealthNarrativePlan.YearNarrative::year).toList());
    assertNotEquals(plan.years().get(0).focus(), plan.years().get(1).focus());
    assertNotEquals(plan.years().get(1).focus(), plan.years().get(2).focus());
    assertTrue(plan.years().get(1).transition().contains("今年"));
    assertTrue(plan.years().get(2).transition().contains("前两年"));
  }

  @Test
  void everyYearHasConcreteSignalsActionsAndTraceableEvidence() {
    for (WealthNarrativePlan.YearNarrative year : plan.years()) {
      assertEquals(2, year.realitySignals().size());
      assertEquals(2, year.actions().size());
      assertFalse(year.evidenceKeys().isEmpty());
      assertFalse(year.incomeSource().isBlank());
      assertFalse(year.retention().isBlank());
      assertFalse(year.mainLimit().isBlank());
    }
  }

  @Test
  void plainCopyUsesNaturalMoneySpecificChineseWithoutPromisesOrJargon() {
    String text = String.join("", readerLines());
    for (String required : List.of("收入", "到账", "支出", "成本", "分配", "结余")) {
      assertTrue(text.contains(required), required + " missing from " + text);
    }
    for (String forbidden : List.of(
        "现有证据不足", "先观察再判断", "卡点", "现在卡住了", "变现", "现金流",
        "止损", "资产配置", "财富自由", "抓住机会", "发力", "赋能", "兑现", "承接",
        "正财", "偏财", "食神", "伤官", "比肩", "劫财", "七杀", "正官",
        "晋升", "职责", "授权", "股票", "基金", "加密货币", "保证赚钱", "一定发财")) {
      assertFalse(text.contains(forbidden), forbidden + " in " + text);
    }
    for (String line : readerLines()) {
      assertTrue(line.length() <= 48, line.length() + " chars: " + line);
    }
  }

  @Test
  void skillIncomeNamesWhoseAbilitySolvesWhoseProblem() {
    assertEquals(
        "这一年的钱主要来自用自己的能力，解决别人愿意付钱的问题。",
        plan.years().get(2).incomeSource());
  }

  private List<String> readerLines() {
    return Stream.of(
            Stream.of(plan.thesis(), plan.summary()),
            plan.paths().stream().map(WealthNarrativePlan.PathSummary::judgment),
            Stream.of(plan.mainRisk().judgment()),
            plan.years().stream().flatMap(year -> Stream.of(
                Stream.of(
                    year.focus(), year.incomeSource(), year.retention(), year.mainLimit(), year.transition()),
                year.realitySignals().stream(),
                year.actions().stream()).flatMap(stream -> stream)),
            plan.route().stream())
        .flatMap(stream -> stream)
        .toList();
  }
}
