package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.wealth.v3.DefaultWealthReportGenerator;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class WealthWeakSupportCopyTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-09-06T00:00:00Z"), ZoneId.of("Asia/Shanghai"));
  private static final LocalDate AS_OF = LocalDate.of(2026, 9, 6);

  @Test
  void changesOnlyTheMethodNoteAcrossSixWeakBandFixtures() {
    assertProfileNote(
        "1981-09-15T02:15:00",
        "上海市 上海市",
        "ROOTLESS",
        "基础命盘里的支持条件较少，所以增加投入或提前安排支出时，建议采用较保守的金额。");
    assertProfileNote(
        "1996-03-15T22:15:00",
        "上海市 上海市",
        "ROOTLESS",
        "基础命盘里的支持条件较少，所以增加投入或提前安排支出时，建议采用较保守的金额。");
    assertProfileNote(
        "1980-01-15T02:15:00",
        "上海市 上海市",
        "ROOTED",
        "基础命盘里存在一些支持条件，所以不必按最保守的方式理解；新增投入和支出仍要留出余量。");
    assertProfileNote(
        "1981-08-15T06:15:00",
        "上海市 上海市",
        "ROOTED",
        "基础命盘里存在一些支持条件，所以不必按最保守的方式理解；新增投入和支出仍要留出余量。");
    assertProfileNote(
        "1992-07-18T06:15:00",
        "广东省 广州市",
        "ROOTED_WITH_VISIBLE_RESOURCE",
        "基础命盘里能看到两类支持条件，所以可以保留一定行动空间；但投入和支出不能一次安排到上限。");
    assertProfileNote(
        "1981-11-15T10:15:00",
        "上海市 上海市",
        "ROOTED_WITH_VISIBLE_RESOURCE",
        "基础命盘里能看到两类支持条件，所以可以保留一定行动空间；但投入和支出不能一次安排到上限。");
  }

  private void assertProfileNote(
      String birthTime,
      String birthPlace,
      String profile,
      String expectedNote) {
    PaipanRequest request = new PaipanRequest(
        "弱档文案测试", "female", birthTime, birthPlace, false);
    var chart = new BaziService().paipan(request);
    var factory = new AnnualContextFactory(CLOCK);
    var contexts = factory.create(request, chart, ReportHorizon.WEALTH_PRODUCT);
    var report = new DefaultWealthReportGenerator().generate(
        chart, factory.createYear(request, chart, 2025), contexts, AS_OF);

    assertEquals("wealth-plain-v3.16", report.copyVersion());
    assertTrue(report.readingNote().text().contains(expectedNote), report.readingNote().text());
    for (var year : report.years()) {
      assertTrue(year.facts().stream().anyMatch(fact ->
          fact.code().equals("natal.weak_support_profile") && fact.value().equals(profile)));
      assertFalse(year.evidence().stream().anyMatch(evidence ->
          evidence.factKey().equals("natal.weak_support_profile")),
          "weak support profile must not change path scoring");
      assertEquals(List.of("stable_income", "skill_income", "project_income", "cooperation_income", "retention"),
          year.decisions().stream().map(decision -> decision.path()).toList());
    }
  }
}
