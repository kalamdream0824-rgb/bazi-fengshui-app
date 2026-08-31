package com.bazi.app.report.overall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OverallNarrativePlannerTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"),
      ZoneId.of("Asia/Shanghai"));

  @Test
  void writesThreeDifferentAnnualHeadlinesAndAllFourDimensions() {
    OverallNarrativePlan content = content();

    assertEquals(3, content.horizonYears());
    assertEquals(3, content.years().size());
    assertEquals(3, content.years().stream().map(OverallNarrativePlan.YearNarrative::headline)
        .distinct().count());
    for (OverallNarrativePlan.YearNarrative year : content.years()) {
      assertEquals(4, year.dimensions().size());
      assertEquals(Set.of("rhythm", "career", "wealth", "relationship"),
          year.dimensions().stream()
              .map(OverallNarrativePlan.DimensionReading::code)
              .collect(Collectors.toSet()));
      assertEquals(2, year.actions().size());
      assertFalse(year.priorityIssue().isBlank());
      assertFalse(year.changeCondition().isBlank());
    }
  }

  @Test
  void keepsEveryDimensionSpecificInsteadOfUsingInterchangeableCopy() {
    for (OverallNarrativePlan.YearNarrative year : content().years()) {
      assertContainsAny(reading(year, "rhythm"), "精力", "安排", "休息", "节奏");
      assertContainsAny(reading(year, "career"), "工作", "责任", "任务", "成果");
      assertContainsAny(reading(year, "wealth"), "钱", "收入", "支出", "花费");
      assertContainsAny(reading(year, "relationship"), "关系", "相处", "沟通", "支持");
    }
  }

  @Test
  void rejectsMechanicalOrNoAnswerLanguage() {
    String text = flatten(content());

    for (String forbidden : List.of(
        "卡点", "抓手", "赋能", "赛道", "闭环", "现有证据不足", "先观察再判断",
        "可能可以", "视情况而定", "整体安排的几件事", "日常安排的几件事",
        "的直接变化会增多", "里容易积累小问题", "，但不要因为有余力")) {
      assertFalse(text.contains(forbidden), forbidden + " found in: " + text);
    }
    assertTrue(content().years().stream().allMatch(year ->
        year.dimensions().stream().allMatch(dimension -> !dimension.evidenceKeys().isEmpty())));
  }

  @Test
  void doesNotRepeatTheSameActionSentenceAcrossYears() {
    List<String> actions = content().years().stream()
        .flatMap(year -> year.actions().stream())
        .toList();

    assertEquals(actions.size(), actions.stream().distinct().count(), actions.toString());
  }

  @Test
  void sameDimensionUsesDifferentJudgmentWhenAnnualEvidenceChanges() {
    OverallNarrativePlan content = content(new PaipanRequest(
        "测试命盘R03", "male", "1988-02-04T10:00:00", "北京", false));

    Map<String, List<String>> judgmentsByDimension = content.years().stream()
        .flatMap(year -> year.dimensions().stream())
        .collect(Collectors.groupingBy(
            OverallNarrativePlan.DimensionReading::code,
            Collectors.mapping(OverallNarrativePlan.DimensionReading::judgment, Collectors.toList())));

    judgmentsByDimension.forEach((dimension, judgments) -> assertEquals(
        judgments.size(), judgments.stream().distinct().count(),
        dimension + " should explain changed annual evidence: " + judgments));
  }

  @Test
  void repeatedPrimaryFocusExplainsWhatBecomesMoreSerious() {
    OverallNarrativePlan content = content(new PaipanRequest(
        "测试命盘R03", "male", "1988-02-04T10:00:00", "北京", false));
    OverallNarrativePlan.YearNarrative first = content.years().get(0);
    OverallNarrativePlan.YearNarrative second = content.years().get(1);

    assertEquals("relationship", first.primaryCode());
    assertEquals("relationship", second.primaryCode());
    assertFalse(first.priorityIssue().equals(second.priorityIssue()),
        "priority issue must show the annual difference");
    assertFalse(first.changeCondition().equals(second.changeCondition()),
        "change condition must show the annual difference");
    assertContainsAny(first.transition(), "分歧", "变动", "压力", "安排");
  }

  @Test
  void easingTransitionSaysWhatGetsEasierInsteadOfOnlyReturningTheLabel() {
    OverallYearEvaluation pressured = syntheticYear(
        2026, OverallStance.PRESSURED, "annual.branch.clash.day");
    OverallYearEvaluation supportive = syntheticYear(
        2027, OverallStance.SUPPORTIVE, "annual.branch.harmony.day");
    OverallPeriodEvaluation period = new OverallPeriodEvaluation(
        2,
        List.of(pressured, supportive),
        List.of(new OverallTransition(2026, 2027, "缓和")));

    String transition = new OverallNarrativePlanner().plan(period).years().get(0).transition();

    assertTrue(transition.contains("缓和"), transition);
    assertContainsAny(transition, "沟通", "分歧", "配合", "相处");
    assertFalse(transition.equals("下一年仍要关注关系支持，整体表现为缓和。"));
  }

  @Test
  void everyYearExplainsHowThePrimaryFocusAffectsTheSecondaryFocus() {
    for (OverallNarrativePlan.YearNarrative year : content().years()) {
      assertFalse(year.linkage().isBlank());
      assertTrue(year.linkage().contains(year.primaryLabel()), year.linkage());
      assertTrue(year.linkage().contains(year.secondaryLabel()), year.linkage());
    }
  }

  @Test
  void legacyV1JsonWithoutLinkageRemainsReadable() throws Exception {
    ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    ObjectNode legacy = json.valueToTree(content());
    legacy.withArray("years").forEach(year -> ((ObjectNode) year).remove("linkage"));

    OverallNarrativePlan restored = json.treeToValue(legacy, OverallNarrativePlan.class);

    assertTrue(restored.years().stream().allMatch(year -> year.linkage().isEmpty()));
  }

  @Test
  void repeatedCalculationProducesTheSameNarrative() {
    OverallPeriodEvaluation period = period();
    OverallNarrativePlanner planner = new OverallNarrativePlanner();

    assertEquals(planner.plan(period), planner.plan(period));
  }

  private OverallNarrativePlan content() {
    return new OverallNarrativePlanner().plan(period());
  }

  private OverallNarrativePlan content(PaipanRequest request) {
    PaipanResultDto chart = new BaziService().paipan(request);
    List<AnnualContext> contexts = new AnnualContextFactory(CLOCK)
        .create(request, chart, ReportHorizon.of(3));
    return new OverallNarrativePlanner().plan(new OverallPeriodArbitrator().arbitrate(contexts));
  }

  private OverallPeriodEvaluation period() {
    PaipanRequest request = new PaipanRequest(
        "林先生", "male", "1995-10-08T14:30:00", "上海", false);
    PaipanResultDto chart = new BaziService().paipan(request);
    List<AnnualContext> contexts = new AnnualContextFactory(CLOCK)
        .create(request, chart, ReportHorizon.of(3));
    return new OverallPeriodArbitrator().arbitrate(contexts);
  }

  private String reading(OverallNarrativePlan.YearNarrative year, String code) {
    return year.dimensions().stream()
        .filter(item -> item.code().equals(code))
        .findFirst()
        .orElseThrow()
        .judgment();
  }

  private OverallYearEvaluation syntheticYear(
      int year, OverallStance relationshipStance, String relationshipKey) {
    List<OverallDimensionEvaluation> dimensions = List.of(
        syntheticDimension(OverallDimension.RHYTHM, OverallStance.BALANCED,
            "annual.stem.group.resource"),
        syntheticDimension(OverallDimension.CAREER, OverallStance.BALANCED,
            "annual.stem.group.resource"),
        syntheticDimension(OverallDimension.WEALTH, OverallStance.BALANCED,
            "annual.stem.group.resource"),
        syntheticDimension(OverallDimension.RELATIONSHIP, relationshipStance, relationshipKey));
    return new OverallYearEvaluation(
        year, year == 2026 ? "丙午" : "丁未",
        OverallDimension.RELATIONSHIP, OverallDimension.RHYTHM, dimensions);
  }

  private OverallDimensionEvaluation syntheticDimension(
      OverallDimension dimension, OverallStance stance, String key) {
    boolean pressured = stance == OverallStance.PRESSURED;
    return new OverallDimensionEvaluation(
        dimension,
        stance,
        pressured ? 1 : 3,
        pressured ? 3 : 1,
        pressured ? List.of("natal.balance.middle") : List.of(key),
        pressured ? List.of(key) : List.of("natal.balance.weak"),
        List.of(key));
  }

  private void assertContainsAny(String text, String... words) {
    assertTrue(List.of(words).stream().anyMatch(text::contains), text);
  }

  private String flatten(OverallNarrativePlan content) {
    return content.thesis() + content.summary() + content.readingNote()
        + String.join("", content.route())
        + content.years().stream()
            .map(year -> year.headline() + year.verdict() + year.priorityIssue()
                + String.join("", year.actions()) + year.changeCondition() + year.transition()
                + year.dimensions().stream()
                    .map(OverallNarrativePlan.DimensionReading::judgment)
                    .collect(Collectors.joining()))
            .collect(Collectors.joining());
  }
}
