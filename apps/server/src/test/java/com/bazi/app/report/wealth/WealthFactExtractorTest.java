package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.TenGodGroup;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WealthFactExtractorTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private PaipanRequest request;
  private PaipanResultDto chart;
  private List<AnnualContext> contexts;

  @BeforeEach
  void setUp() {
    request = new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
    chart = new BaziService().paipan(request);
    contexts = new AnnualContextFactory(CLOCK)
        .create(request, chart, ReportHorizon.WEALTH_PRODUCT);
  }

  @Test
  void retainsVisibleHiddenTenGodsAndPillarPositions() {
    WealthNatalProfile profile = new WealthFactExtractor().extractNatal(chart, contexts.get(0));

    assertEquals("壬", profile.dayMaster());
    assertEquals("偏强", profile.balanceLevel());
    assertTrue(profile.tenGodOccurrences().stream().anyMatch(item ->
        item.tenGod().equals("正财") && item.position().equals("time.stem") && item.visible()));
    assertTrue(profile.tenGodOccurrences().stream().anyMatch(item ->
        item.tenGod().equals("正财")
            && item.position().startsWith("time.branch.hidden")
            && !item.visible()));
    assertEquals(4, profile.groupCounts().get(TenGodGroup.OUTPUT));
    assertEquals(2, profile.groupCounts().get(TenGodGroup.WEALTH));
    assertEquals(2, profile.groupCounts().get(TenGodGroup.PEER));
    assertEquals(2, profile.groupCounts().get(TenGodGroup.RESOURCE));
    assertEquals(2, profile.groupCounts().get(TenGodGroup.AUTHORITY));
  }

  @Test
  void preservesAnnualDayunAndRelationFactsForThreeYears() {
    List<WealthYearFacts> years = new WealthFactExtractor().extract(chart, contexts);

    assertEquals(List.of(2026, 2027, 2028), years.stream().map(WealthYearFacts::year).toList());
    assertEquals(List.of("偏财", "正财", "七杀"),
        years.stream().map(WealthYearFacts::annualStemTenGod).toList());
    assertTrue(years.stream().allMatch(year -> year.activeDayunTenGod() != null
        && !year.activeDayunTenGod().isBlank()));
    assertTrue(years.get(0).evidence().stream().anyMatch(item ->
        item.key().equals("annual.branch.harmony.time")
            && item.family() == EvidenceFamily.ANNUAL_TRIGGER));
    assertTrue(years.get(0).evidence().stream().anyMatch(item ->
        item.key().equals("annual.branch.punishment.dayun")
            && item.family() == EvidenceFamily.DAYUN_CONTEXT));
    assertTrue(years.get(1).evidence().stream().anyMatch(item ->
        item.key().equals("annual.branch.harmony.dayun")
            && item.family() == EvidenceFamily.DAYUN_CONTEXT));
  }

  @Test
  void emitsUniqueReviewableEvidenceWithoutScoringIt() {
    WealthFactExtractor extractor = new WealthFactExtractor();
    WealthNatalProfile profile = extractor.extractNatal(chart, contexts.get(0));
    List<WealthYearFacts> years = extractor.extract(chart, contexts);

    assertEquals(profile.evidence().size(),
        new HashSet<>(profile.evidence().stream().map(WealthEvidence::key).toList()).size());
    for (WealthYearFacts year : years) {
      assertEquals(year.evidence().size(),
          new HashSet<>(year.evidence().stream().map(WealthEvidence::key).toList()).size());
      assertTrue(year.evidence().stream().allMatch(item -> !item.label().isBlank()));
      assertTrue(year.evidence().stream().allMatch(item -> !item.value().isBlank()));
      assertTrue(year.evidence().stream().allMatch(item -> item.family() != null));
    }
    assertFalse(profile.evidence().isEmpty());
    assertNotNull(years.get(0).natalProfile());
  }
}
