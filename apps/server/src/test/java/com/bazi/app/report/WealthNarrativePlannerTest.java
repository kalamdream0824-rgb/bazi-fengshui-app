package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WealthNarrativePlannerTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private PaipanRequest request;
  private PaipanResultDto chart;
  private ThreeYearAssessor assessor;
  private WealthNarrativePlanner planner;

  @BeforeEach
  void setUp() {
    request = new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
    chart = new BaziService().paipan(request);
    assessor = new ThreeYearAssessor(CLOCK, new AnnualRuleCatalog());
    planner = new WealthNarrativePlanner();
  }

  @Test
  void givesTwoYearsWithOneFocusTwoReasonsAndTwoActions() {
    WealthContext context = WealthContext.fromCodes(
        "mixed", "increase_income", "income_fluctuating");

    CareerNarrativePlan plan = planner.plan(
        assessor.assess(request, chart, ReportTopic.WEALTH), context);

    assertEquals(2, plan.years().size());
    assertFalse(plan.thesis().isBlank());
    for (CareerNarrativePlan.YearNarrative year : plan.years()) {
      assertEquals(2, year.reasons().size());
      assertEquals(2, year.actions().size());
      assertTrue(year.evidenceKeys().size() >= 2, year.toString());
      assertTrue(year.obstacle().endsWith("。"), year.obstacle());
    }
  }

  @Test
  void realityAnswersChangeTheWordingButNeverTheAstrologyEvidence() {
    ThreeYearAssessment assessment = assessor.assess(request, chart, ReportTopic.WEALTH);
    WealthContext salary = WealthContext.fromCodes("salary", "increase_income", "stable");
    WealthContext selfEmployed = WealthContext.fromCodes(
        "self_employed", "stabilize_cashflow", "spending_pressure");

    CareerNarrativePlan salaryPlan = planner.plan(assessment, salary);
    CareerNarrativePlan businessPlan = planner.plan(assessment, selfEmployed);

    assertNotEquals(salaryPlan.thesis(), businessPlan.thesis());
    assertTrue(flatten(salaryPlan).contains("工资") || flatten(salaryPlan).contains("加薪"));
    assertTrue(flatten(businessPlan).contains("客户") || flatten(businessPlan).contains("回款"));
    assertEquals(
        salaryPlan.years().stream().map(CareerNarrativePlan.YearNarrative::evidenceKeys).toList(),
        businessPlan.years().stream().map(CareerNarrativePlan.YearNarrative::evidenceKeys).toList());
  }

  @Test
  void usesNaturalChineseWithoutEmptyAnswersOrFinancialPromises() {
    List<WealthContext> contexts = List.of(
        WealthContext.fromCodes("salary", "increase_income", "stable"),
        WealthContext.fromCodes("self_employed", "stabilize_cashflow", "income_fluctuating"),
        WealthContext.fromCodes("mixed", "reduce_pressure", "spending_pressure"),
        WealthContext.fromCodes("unstable", "new_income_source", "preparing_adjustment"));

    for (WealthContext context : contexts) {
      CareerNarrativePlan plan = planner.plan(
          assessor.assess(request, chart, ReportTopic.WEALTH), context);
      String text = flatten(plan);
      for (String forbidden : List.of(
          "现有证据不足", "先观察再判断", "卡点", "变现", "现金流", "止损",
          "资产配置", "财富自由", "一定赚钱", "必定发财", "稳赚")) {
        assertFalse(text.contains(forbidden), forbidden + " in " + text);
      }
      for (String line : readerLines(plan)) {
        assertTrue(line.length() <= 48, line.length() + " chars: " + line);
      }
    }
  }

  private String flatten(CareerNarrativePlan plan) {
    return plan.thesis() + plan.contextSummary() + String.join("", plan.route()) + plan.years().stream()
        .map(year -> year.headline() + year.verdict() + String.join("", year.reasons())
            + year.obstacle() + String.join("", year.actions()) + year.changeCondition())
        .collect(Collectors.joining());
  }

  private List<String> readerLines(CareerNarrativePlan plan) {
    return java.util.stream.Stream.concat(
        java.util.stream.Stream.of(plan.thesis()),
        plan.years().stream().flatMap(year -> java.util.stream.Stream.of(
            java.util.stream.Stream.of(
                year.headline(), year.verdict(), year.obstacle(), year.changeCondition()),
            year.reasons().stream(), year.actions().stream()).flatMap(stream -> stream)))
        .toList();
  }
}
