package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.DaYunDto;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnnualContextFactoryTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"),
      ZoneId.of("Asia/Shanghai"));

  @Test
  void buildsCurrentAndNextTwoAnnualContextsFromOneChart() {
    PaipanRequest request = fixtureRequest();
    PaipanResultDto chart = new BaziService().paipan(request);

    List<AnnualContext> contexts = new AnnualContextFactory(CLOCK).create(request, chart);

    assertEquals(List.of(2026, 2027, 2028), contexts.stream().map(AnnualContext::year).toList());
    assertEquals(List.of("丙午", "丁未", "戊申"), contexts.stream().map(AnnualContext::ganZhi).toList());
    assertEquals(
        List.of("偏财", "正财", "七杀"),
        contexts.stream().map(AnnualContext::yearStemTenGod).toList());
    assertEquals(
        List.of(TenGodGroup.WEALTH, TenGodGroup.WEALTH, TenGodGroup.AUTHORITY),
        contexts.stream().map(AnnualContext::yearStemGroup).toList());
    assertTrue(contexts.stream().allMatch(context -> context.activeDaYun() != null));
  }

  @Test
  void emitsStructuredNatalAndDayunRelationFacts() {
    PaipanRequest request = fixtureRequest();
    List<AnnualContext> contexts = new AnnualContextFactory(CLOCK)
        .create(request, new BaziService().paipan(request));

    AnnualContext year2026 = contexts.get(0);
    AnnualContext year2027 = contexts.get(1);
    AnnualContext year2028 = contexts.get(2);

    assertTrue(year2026.factKeys().contains("annual.stem.group.wealth"));
    assertTrue(year2026.factKeys().contains("annual.branch.harmony.time"));
    assertTrue(year2026.factKeys().contains("annual.branch.punishment.dayun"));
    assertTrue(year2027.factKeys().contains("annual.branch.harmony.dayun"));
    assertTrue(year2028.factKeys().contains("annual.branch.harm.year"));
  }

  @Test
  void resolvesDayunWithAStartInclusiveEndExclusiveRange() {
    DaYunDto first = dayun("甲子", "2018 - 2028");
    DaYunDto second = dayun("乙丑", "2028 - 2038");

    assertEquals(first, AnnualContextFactory.activeDaYun(List.of(first, second), 2027));
    assertEquals(second, AnnualContextFactory.activeDaYun(List.of(first, second), 2028));
    assertNull(AnnualContextFactory.activeDaYun(List.of(first, second), 2038));
  }

  @Test
  void rejectsMalformedDayunYearRangesInsteadOfSilentlySelectingOne() {
    DaYunDto malformed = dayun("甲子", "2018 至 2028");

    assertThrows(
        IllegalArgumentException.class,
        () -> AnnualContextFactory.activeDaYun(List.of(malformed), 2026));
  }

  @ParameterizedTest
  @CsvSource({
      "正印,RESOURCE", "偏印,RESOURCE", "比肩,PEER", "劫财,PEER", "食神,OUTPUT",
      "伤官,OUTPUT", "正财,WEALTH", "偏财,WEALTH", "正官,AUTHORITY", "七杀,AUTHORITY"
  })
  void mapsTenGodsToFunctionalGroups(String tenGod, TenGodGroup expected) {
    assertEquals(expected, TenGodGroup.fromTenGod(tenGod));
  }

  @Test
  void retainsTheSharedNatalAnalysisForLaterRules() {
    PaipanRequest request = fixtureRequest();
    AnnualContext context = new AnnualContextFactory(CLOCK)
        .create(request, new BaziService().paipan(request))
        .get(0);

    assertNotNull(context.natalAnalysis());
    assertEquals("壬（水）", context.natalAnalysis().fact("dayMaster").value());
  }

  @Test
  void exposesStructuredNatalFactsForConditionalRules() {
    PaipanRequest request = fixtureRequest();
    AnnualContext context = new AnnualContextFactory(CLOCK)
        .create(request, new BaziService().paipan(request))
        .get(0);

    assertTrue(context.factKeys().contains("natal.balance.strong"));
    assertTrue(context.factKeys().contains("natal.ten_god.group.output"));
    assertTrue(context.factKeys().contains("natal.ten_god.group.resource"));
    assertTrue(context.factKeys().contains("natal.ten_god.group.wealth"));
    assertTrue(context.factKeys().contains("natal.gender.male"));
    assertTrue(context.factKeys().contains("natal.day.branch.申"));
    assertTrue(context.factKeys().contains("dayun.stem.group.peer"));
    assertTrue(context.factKeys().contains("next.annual.stem.group.wealth"));
  }

  private PaipanRequest fixtureRequest() {
    return new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
  }

  private DaYunDto dayun(String ganZhi, String range) {
    return new DaYunDto("", ganZhi, range, false, "", "", "", "", List.of());
  }
}
