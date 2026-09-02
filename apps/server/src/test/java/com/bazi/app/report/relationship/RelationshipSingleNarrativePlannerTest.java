package com.bazi.app.report.relationship;

import static org.junit.jupiter.api.Assertions.*;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RelationshipSingleNarrativePlannerTest {
  private final RelationshipSingleNarrativePlanner planner = new RelationshipSingleNarrativePlanner();

  @Test
  void realSampleKeepsFiveDimensionsAndOnlyTwoYearsForBothGenders() throws Exception {
    for (String gender : List.of("male", "female")) {
      var request = new PaipanRequest("测试", gender, "1995-10-08T14:30:00", "上海", false);
      var chart = new BaziService().paipan(request);
      var zone = ZoneId.of("Asia/Shanghai");
      var clock = Clock.fixed(LocalDate.of(2026, 8, 27).atStartOfDay(zone).toInstant(), zone);
      var factory = new AnnualContextFactory(clock);
      var evaluator = new RelationshipDimensionEvaluator();
      var extractor = new RelationshipFactExtractor();
      var two = evaluator.evaluate(extractor.extract(request, chart, factory.create(request, chart, ReportHorizon.of(2))));
      var three = evaluator.evaluate(extractor.extract(request, chart, factory.create(request, chart, ReportHorizon.of(3))));
      var plan = planner.plan(period(two));
      assertNotNull(plan);
      assertNull(plan.timeline());
      assertEquals(2, plan.horizonYears());
      assertEquals(2026, plan.currentYear());
      assertEquals(2027, plan.outlookYear());
      assertEquals(three.subList(0, 2), plan.evaluations());
      assertEquals(3, plan.sections().size());
      assertTrue(plan.sections().get(0).paragraphs().stream().anyMatch(s -> s.contains("如果你目前没有")));
      assertTrue(plan.sections().get(0).paragraphs().stream().anyMatch(s -> s.contains("如果已经有")));
      assertFalse(text(plan).contains("2028"));
      assertTrue(plan.outlook().stream().mapToInt(String::length).sum() <= 240);
      var mapper = new ObjectMapper();
      assertEquals(plan, mapper.readValue(mapper.writeValueAsString(plan), RelationshipSingleNarrativePlan.class));
    }
  }

  @Test
  void previousYearEntryBuildsTimelineWithoutExpandingTheTwoYearProduct() {
    var previous = previousYear();
    var product = period(List.of(
        year(2026, RelationshipDimension.CONNECTION, 8, 2),
        year(2027, RelationshipDimension.RESPONSE, 9, 1)));

    var plan = planner.plan(product, previous);

    assertNotNull(plan.timeline());
    assertEquals(2025, plan.timeline().past().year());
    assertEquals(2026, plan.timeline().present().year());
    assertEquals(List.of(2027), plan.timeline().future().stream()
        .map(com.bazi.app.report.NarrativeTimeline.FutureStep::year)
        .toList());
    assertEquals(2, plan.evaluations().size());
    assertEquals(List.of(2026, 2027), plan.evaluations().stream()
        .map(RelationshipYearEvaluation::year)
        .toList());
  }

  @Test
  void pastReviewChecksOpportunitiesResponsesAndBoundariesWithoutInventingAPartner() {
    var plan = planner.plan(
        period(List.of(
            year(2026, RelationshipDimension.CONNECTION, 8, 2),
            year(2027, RelationshipDimension.STABILITY, 9, 1))),
        previousYear());

    String review = plan.timeline().past().headline()
        + String.join("", plan.timeline().past().checkpoints())
        + plan.timeline().past().bridge();
    assertTrue(review.contains("认识机会"), review);
    assertTrue(review.contains("回应"), review);
    assertTrue(review.contains("边界"), review);
    for (String forbidden : List.of(
        "你去年有对象", "你去年认识了", "去年出现了对象", "当时的对方",
        "事实证明", "准确命中", "一定脱单")) {
      assertFalse(review.contains(forbidden), forbidden + ": " + review);
    }
  }

  @Test
  void timelineKeepsCurrentDetailAndAddsOnlyOneNextYearAction() {
    var product = period(List.of(
        year(2026, RelationshipDimension.CONNECTION, 8, 2),
        year(2027, RelationshipDimension.RESPONSE, 9, 1)));
    var legacy = planner.plan(product);

    var plan = planner.plan(product, previousYear());

    assertEquals(legacy.sections(), plan.sections());
    assertEquals(legacy.outlook(), plan.outlook());
    assertEquals(legacy.evaluations(), plan.evaluations());
    assertEquals(1, plan.timeline().future().size());
    assertTrue(plan.timeline().present().headline().contains("认识"));
    assertTrue(plan.timeline().present().headline().contains("机会"));
    assertTrue(plan.timeline().future().get(0).action().contains("回应"));
    Set<String> detailedCopy = new HashSet<>();
    plan.sections().forEach(section -> {
      detailedCopy.addAll(section.paragraphs());
      detailedCopy.addAll(section.signals());
    });
    detailedCopy.addAll(plan.outlook());
    assertFalse(detailedCopy.contains(plan.timeline().present().headline()));
    assertFalse(detailedCopy.contains(plan.timeline().present().judgment()));
    assertFalse(detailedCopy.contains(plan.timeline().present().priority()));
    assertFalse(detailedCopy.contains(plan.timeline().future().get(0).headline()));
    assertFalse(detailedCopy.contains(plan.timeline().future().get(0).action()));
  }

  @Test
  void timelineEvidenceBelongsToTheCorrespondingCalculationYear() {
    var previous = previousYear();
    var product = period(List.of(
        year(2026, RelationshipDimension.CONNECTION, 8, 2),
        year(2027, RelationshipDimension.RESPONSE, 9, 1)));

    var timeline = planner.plan(product, previous).timeline();

    assertEvidenceBelongsTo(previous, timeline.past().evidenceKeys());
    assertEvidenceBelongsTo(product.years().get(0), timeline.present().evidenceKeys());
    assertEvidenceBelongsTo(product.years().get(1), timeline.future().get(0).evidenceKeys());
  }

  @Test
  void historicalSingleJsonWithoutTimelineStillDeserializes() throws Exception {
    var legacy = planner.plan(period(List.of(
        year(2026, RelationshipDimension.CONNECTION, 8, 0),
        year(2027, RelationshipDimension.RESPONSE, 8, 0))));
    var mapper = new ObjectMapper();
    var json = mapper.valueToTree(legacy);
    ((com.fasterxml.jackson.databind.node.ObjectNode) json).remove("timeline");

    var restored = mapper.treeToValue(json, RelationshipSingleNarrativePlan.class);

    assertNull(restored.timeline());
    assertEquals(legacy.evaluations(), restored.evaluations());
  }

  @Test
  void rejectsTimelineThatDoesNotMatchTheSingleProductYears() throws Exception {
    var plan = planner.plan(
        period(List.of(
            year(2026, RelationshipDimension.CONNECTION, 8, 0),
            year(2027, RelationshipDimension.RESPONSE, 8, 0))),
        previousYear());
    var mapper = new ObjectMapper();
    var changed = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(plan);
    ((com.fasterxml.jackson.databind.node.ObjectNode) changed.at("/timeline/present"))
        .put("year", 2028);

    assertThrows(Exception.class,
        () -> mapper.treeToValue(changed, RelationshipSingleNarrativePlan.class));
  }

  @Test
  void changingNextYearCannotChangeCurrentYearConclusionOrSections() {
    var first = year(2026, RelationshipDimension.CONNECTION, 8, 0);
    var a = planner.plan(period(List.of(first, year(2027, RelationshipDimension.CONNECTION, 6, 0))));
    var b = planner.plan(period(List.of(first, year(2027, RelationshipDimension.RESPONSE, 100, 10))));
    assertNotNull(a);
    assertNotNull(b);
    assertEquals(a.thesis(), b.thesis());
    assertEquals(a.summary(), b.summary());
    assertEquals(a.sections(), b.sections());
    assertTrue(a.thesis().contains("主动认识人"));
    assertNotEquals(a.outlook(), b.outlook());
  }

  @Test
  void nextYearRiskDoesNotInventACurrentYearWarning() {
    var plan = planner.plan(period(List.of(year(2026, RelationshipDimension.CONNECTION, 8, 0),
        year(2027, RelationshipDimension.BOUNDARIES, 0, 9))));
    assertNotNull(plan);
    assertEquals(List.of("opportunities", "development"), plan.sections().stream().map(s -> s.id()).toList());
    assertFalse(plan.summary().contains("反复要求你让步"));
    assertTrue(String.join("", plan.outlook()).contains("分歧"));
  }

  @Test
  void everyDimensionAndToneHasDistinctPlainCopyWithoutUnsupportedPromises() {
    Set<String> theses = new HashSet<>();
    for (var dim : RelationshipDimension.values()) {
      for (int[] scores : List.of(new int[]{12, 0}, new int[]{10, 4}, new int[]{1, 12})) {
        var plan = planner.plan(period(List.of(year(2026, dim, scores[0], scores[1]), year(2027, dim, scores[0], scores[1]))));
        assertNotNull(plan);
        theses.add(plan.thesis());
        String copy = text(plan);
        for (String banned : List.of("卡点", "现有证据不足", "一定脱单", "有人愿意稳定靠近你", "三次", "正缘", "{{", "${")) {
          assertFalse(copy.contains(banned), banned + ": " + copy);
        }
        for (String sentence : copy.split("[。！？；]")) assertTrue(sentence.strip().length() <= 48, sentence);
        assertTrue(plan.thesis().length() <= 70);
      }
    }
    assertEquals(15, theses.size());
  }

  @Test
  void quietDimensionsDoNotReceivePositivePredictions() {
    var first = year(2026, RelationshipDimension.RESPONSE, 10, 0);
    var dims = new EnumMap<>(first.dimensions());
    dims.put(RelationshipDimension.CONNECTION, evaluated(RelationshipDimension.CONNECTION, List.of()));
    dims.put(RelationshipDimension.DAILY_COOPERATION, evaluated(RelationshipDimension.DAILY_COOPERATION, List.of()));
    var plan = planner.plan(period(List.of(new RelationshipYearEvaluation(2026, "丙午", dims),
        year(2027, RelationshipDimension.RESPONSE, 10, 0))));
    assertNotNull(plan);
    assertTrue(plan.sections().get(0).paragraphs().get(0).contains("不是今年的主要变化"));
    assertFalse(plan.sections().get(0).paragraphs().get(0).contains("机会不少"));
  }

  @Test
  void outlookRetainsOpposingSupportAndLimitChanges() {
    var plan = planner.plan(period(List.of(year(2026, RelationshipDimension.CONNECTION, 8, 2),
        year(2027, RelationshipDimension.CONNECTION, 10, 4))));
    assertNotNull(plan);
    String text = String.join("", plan.outlook());
    assertTrue(text.contains("更适合主动认识人"));
    assertTrue(text.contains("单方面"));
    assertFalse(text.contains("没有明显变化"));
  }

  @Test
  void unchangedYearsExplicitlyContinueWithoutInventingDatingMilestones() {
    var plan = planner.plan(period(List.of(year(2026, RelationshipDimension.CONNECTION, 8, 0),
        year(2027, RelationshipDimension.CONNECTION, 8, 0))));
    assertNotNull(plan);
    assertTrue(String.join("", plan.outlook()).contains("延续今年"));
    assertTrue(String.join("", plan.outlook()).contains("没有明显变化"));
    assertFalse(String.join("", plan.outlook()).contains("确定关系的年份"));
  }

  @Test
  void equalWeightsWithDifferentSourcesDoNotInventAnImprovement() {
    var first = year(2026, RelationshipDimension.CONNECTION, 8, 0);
    var second = year(2027, RelationshipDimension.CONNECTION, 8, 0);
    var dims = new EnumMap<>(second.dimensions());
    dims.put(RelationshipDimension.CONNECTION, evaluated(RelationshipDimension.CONNECTION, List.of(
        new RelationshipEvidence("annual.branch.harmony.time.connection", RelationshipEvidenceFamily.ANNUAL_TRIGGER,
            "相合", RelationshipDimension.CONNECTION, 8, List.of("changed.source")))));
    var plan = planner.plan(period(List.of(first, new RelationshipYearEvaluation(2027, "丁未", dims))));
    assertNotNull(plan);
    String copy = String.join("", plan.outlook());
    assertTrue(copy.contains("依据"));
    assertFalse(copy.contains("没有明显变化"));
    assertFalse(copy.contains("更顺利"));
  }

  @Test
  void rejectsThreeYearInputRatherThanSilentlyHidingTheThirdYear() {
    assertThrows(IllegalArgumentException.class, () -> planner.plan(period(List.of(
        year(2026, RelationshipDimension.CONNECTION, 8, 0),
        year(2027, RelationshipDimension.CONNECTION, 8, 0),
        year(2028, RelationshipDimension.CONNECTION, 8, 0)))));
  }

  @Test
  void rejectsMalformedStoredSnapshotsAndUnresolvedCopy() throws Exception {
    var plan = planner.plan(period(List.of(year(2026, RelationshipDimension.CONNECTION, 8, 0),
        year(2027, RelationshipDimension.CONNECTION, 8, 0))));
    var mapper = new ObjectMapper();
    var original = mapper.valueToTree(plan);
    for (String field : List.of("relationshipStatus", "horizonYears", "outlookYear", "thesis")) {
      var changed = (com.fasterxml.jackson.databind.node.ObjectNode) original.deepCopy();
      switch (field) {
        case "relationshipStatus" -> changed.put(field, "dating");
        case "horizonYears" -> changed.put(field, 3);
        case "outlookYear" -> changed.put(field, 2030);
        default -> changed.put(field, "未替换{action}");
      }
      assertThrows(Exception.class, () -> mapper.treeToValue(changed, RelationshipSingleNarrativePlan.class), field);
    }
    for (String field : List.of("sections", "outlook", "evaluations", "evidenceKeys")) {
      var changed = (com.fasterxml.jackson.databind.node.ObjectNode) original.deepCopy();
      changed.putArray(field);
      assertThrows(Exception.class, () -> mapper.treeToValue(changed, RelationshipSingleNarrativePlan.class), field);
    }
    var changed = (com.fasterxml.jackson.databind.node.ObjectNode) original.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) changed.at("/sections/0"))
        .putArray("evidenceKeys").add("unknown.evidence");
    assertThrows(Exception.class, () -> mapper.treeToValue(changed, RelationshipSingleNarrativePlan.class));
  }

  private String text(RelationshipSingleNarrativePlan plan) {
    List<String> text = new ArrayList<>(List.of(plan.thesis(), plan.summary()));
    plan.sections().forEach(section -> { text.addAll(section.paragraphs()); text.addAll(section.signals()); });
    text.addAll(plan.outlook());
    return String.join("\n", text);
  }

  private RelationshipPeriodEvaluation period(List<RelationshipYearEvaluation> years) {
    return new RelationshipPeriodArbitrator().arbitrate(years);
  }

  private RelationshipPeriodEvaluation.Year previousYear() {
    return period(List.of(
        year(2025, RelationshipDimension.RESPONSE, 9, 2),
        year(2026, RelationshipDimension.CONNECTION, 8, 1)))
        .years().get(0);
  }

  private void assertEvidenceBelongsTo(
      RelationshipPeriodEvaluation.Year year,
      List<String> evidenceKeys) {
    Set<String> available = year.dimensions().values().stream()
        .flatMap(dimension -> dimension.evidence().stream())
        .map(RelationshipEvidence::key)
        .collect(java.util.stream.Collectors.toSet());
    assertFalse(evidenceKeys.isEmpty());
    assertTrue(available.containsAll(evidenceKeys), evidenceKeys + " not in " + available);
  }

  private RelationshipYearEvaluation year(int year, RelationshipDimension focus, int support, int limit) {
    Map<RelationshipDimension, RelationshipDimensionEvaluation> dims = new EnumMap<>(RelationshipDimension.class);
    for (var dim : RelationshipDimension.values()) {
      int positive = dim == focus ? support : 2;
      int negative = dim == focus ? limit : 0;
      List<RelationshipEvidence> evidence = new ArrayList<>();
      if (positive > 0) evidence.add(new RelationshipEvidence("test." + dim.code() + ".support",
          RelationshipEvidenceFamily.ANNUAL_TRIGGER, "支持", dim, positive, List.of("test.source.support")));
      if (negative > 0) evidence.add(new RelationshipEvidence("test." + dim.code() + ".limit",
          RelationshipEvidenceFamily.ANNUAL_TRIGGER, "限制", dim, -negative, List.of("test.source.limit")));
      dims.put(dim, evaluated(dim, evidence));
    }
    return new RelationshipYearEvaluation(year, year == 2026 ? "丙午" : "丁未", dims);
  }

  private RelationshipDimensionEvaluation evaluated(RelationshipDimension dim, List<RelationshipEvidence> evidence) {
    int positive = evidence.stream().mapToInt(e -> Math.max(0, e.weight())).sum();
    int negative = evidence.stream().mapToInt(e -> Math.max(0, -e.weight())).sum();
    return new RelationshipDimensionEvaluation(dim, positive, negative, positive + negative, positive - negative,
        RelationshipDimensionEvaluation.toneFor(positive, negative), evidence);
  }
}
