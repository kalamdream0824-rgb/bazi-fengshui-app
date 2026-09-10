package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.wealth.v3.DefaultWealthReportGenerator;
import com.bazi.app.report.wealth.v3.WealthHeadlineVocabulary;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Acceptance gate for the final text customers can actually see. */
class WealthVisibleCopyQualityTest {
  private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
  private static final Path INPUTS = Path.of(
      "src/test/resources/report/wealth-v2-baseline-inputs.json");
  private static final Path BASELINE = Path.of(
      "target/wealth-visible-copy-quality-baseline.json");
  private static final List<String> SUBJECTLESS_SOURCE_SKELETONS = List.of(
      "变化直接在当年",
      "当年变化会牵动原有安排",
      "也受较长阶段的收支状态影响",
      "还要和一贯的收支方式对照");
  private static final List<String> ABSTRACT_BODY_PHRASES = List.of(
      "这方面",
      "它主要关系到",
      "有利条件",
      "需要留意的限制",
      "条件的组合",
      "本身的倾向",
      "相对位置",
      "这一方向",
      "不必另起一套安排",
      "的支撑",
      "整体资金判断",
      "整体信号",
      "主要风险",
      "年的预计进账的");
  private static final List<String> ANNUAL_BODY_TEMPLATE_PHRASES = List.of(
      "是较值得关注的方向",
      "可以作为一个方向",
      "有可以考虑的部分",
      "有积攒结余的空间",
      "需要防备的情况",
      "可以留意",
      "自己更忙以后",
      "给日常必需开支和新增投入分别设一个上限",
      "的同时，先为",
      "的同时，共同用钱前",
      "再用",
      "并以");
  private static final List<String> REPORT_SUMMARY_TEMPLATE_PHRASES = List.of(
      "更值得关注",
      "可以作为一个方向",
      "有一定空间",
      "只有较轻的倾向",
      "有积攒结余的空间",
      "想把钱留下来",
      "判断资金状况时",
      "命局有根",
      "承载状态",
      "条件合适时可以推进",
      "不宜把安排做满");
  private static final List<String> THESIS_ROUTE_TIMELINE_TEMPLATE_PHRASES = List.of(
      "有较多支持",
      "有一定支持",
      "主要依据是",
      "还要看",
      "更值得认真经营",
      "今年可以尝试",
      "有机会，同时要把成本和变动算进去",
      "再留出一部分钱应对变化");
  private static final List<String> THESIS_TIMELINE_UNRESOLVED_SUBJECTS = List.of(
      "预计中的钱",
      "增加的投入",
      "共同用钱时，责任与开销",
      "共同用钱时，责任和开销",
      "较可能说清");
  private static final List<String> CONTROLLED_VARIANT_AWKWARD_PHRASES = List.of(
      "按期收到或延期或",
      "转成实际进账已经有一些机会",
      "当前判断更偏向可以",
      "当前更支持");
  private static final List<String> CONCRETE_MONEY_OBJECTS = WealthHeadlineVocabulary.entries().stream()
      .map(WealthHeadlineVocabulary.Entry::objectText)
      .distinct()
      .toList();
  private static List<VisibleCase> cases;

  @BeforeAll
  static void captureVisibleCopyBaseline() throws Exception {
    JsonNode inputs = JSON.readTree(INPUTS.toFile());
    ZoneId zone = ZoneId.of(inputs.get("zoneId").asText());
    List<VisibleCase> generated = new ArrayList<>();
    for (JsonNode fixture : inputs.get("fullBirthCases")) {
      String fixtureId = fixture.get("id").asText();
      LocalDate asOf = LocalDate.parse(fixture.get("asOf").asText());
      Clock clock = Clock.fixed(asOf.atStartOfDay(zone).toInstant(), zone);
      PaipanRequest request = JSON.treeToValue(fixture.get("request"), PaipanRequest.class);
      var chart = new BaziService().paipan(request);
      AnnualContextFactory factory = new AnnualContextFactory(clock);
      var product = factory.create(request, chart, ReportHorizon.WEALTH_PRODUCT);
      var previous = factory.createYear(request, chart, product.get(0).year() - 1);
      var report = new DefaultWealthReportGenerator().generate(chart, previous, product, asOf);
      List<VisibleLine> lines = visibleLines(report);
      generated.add(new VisibleCase(fixtureId, report.copyVersion(), lines,
          subjectlessHits(lines), duplicateGroups(lines), unresolvedDuplicateGroups(lines)));
    }
    cases = List.copyOf(generated);

    Map<String, Object> output = new LinkedHashMap<>();
    output.put("notice", "Fixed development fixtures only; not real-world accuracy evidence");
    output.put("scope", "Final user-visible wealth report copy, including timeline and report sections");
    output.put("sampleCount", cases.size());
    output.put("subjectlessSourceSkeletons", SUBJECTLESS_SOURCE_SKELETONS);
    output.put("subjectlessSkeletonHitCounts", subjectlessHitCounts(cases));
    output.put("unresolvedThesisTimelineSubjectHits", unresolvedSubjectHits(cases));
    output.put("presentTimelineMissingConcreteObject", presentTimelineObjectFailures(cases));
    output.put("dominantForwardTimelineLines", dominantForwardTimelineLines(cases));
    output.put("reportsWithRawRepeatedSentences", cases.stream()
        .filter(sample -> !sample.duplicateGroups().isEmpty()).count());
    output.put("reportsWithUnresolvedRepeatedSentences", cases.stream()
        .filter(sample -> !sample.unresolvedDuplicateGroups().isEmpty()).count());
    output.put("reportsWithRepeatedAnnualFocusSentences", cases.stream()
        .filter(sample -> !repeatedAnnualFocusSentences(sample).isEmpty()).count());
    output.put("cases", cases);
    Files.createDirectories(BASELINE.getParent());
    Files.writeString(BASELINE,
        JSON.writerWithDefaultPrettyPrinter().writeValueAsString(output) + "\n");
  }

  @Test
  void baselineContainsExactlyTheTwelveFullBirthFixtures() {
    assertEquals(IntStream.rangeClosed(1, 12).mapToObj(i -> "R%02d".formatted(i)).toList(),
        cases.stream().map(VisibleCase::fixtureId).toList());
  }

  @Test
  void evidenceExplanationMustNameTheKindOfChangeInsteadOfUsingASubjectlessSkeleton() {
    Map<String, Long> hits = subjectlessHitCounts(cases);
    assertEquals(Map.of(), hits,
        () -> "Subjectless conclusion skeletons remain in customer-visible copy: " + hits
            + "; inspect " + BASELINE);
  }

  @Test
  void retrospectiveAndAnnualComparisonMustNameTheMoneyObjectInsteadOfUsingAbstractGlue() {
    Map<String, List<String>> hits = new LinkedHashMap<>();
    for (VisibleCase sample : cases) {
      List<String> matched = ABSTRACT_BODY_PHRASES.stream()
          .filter(phrase -> sample.lines().stream().anyMatch(line -> line.text().contains(phrase)))
          .toList();
      if (!matched.isEmpty()) hits.put(sample.fixtureId(), matched);
    }
    assertEquals(Map.of(), hits,
        () -> "Abstract retrospective/comparison copy remains: " + hits
            + "; inspect " + BASELINE);
  }

  @Test
  void annualBodyMustDescribeAConcreteMoneyChangeInsteadOfRankingATemplateDirection() {
    Map<String, List<String>> hits = new LinkedHashMap<>();
    for (VisibleCase sample : cases) {
      List<String> matched = ANNUAL_BODY_TEMPLATE_PHRASES.stream()
          .filter(phrase -> sample.lines().stream()
              .filter(line -> isAnnualBody(line.sectionId()))
              .anyMatch(line -> line.text().contains(phrase)))
          .toList();
      if (!matched.isEmpty()) hits.put(sample.fixtureId(), matched);
    }
    assertEquals(Map.of(), hits,
        () -> "Template-ranked annual body copy remains: " + hits
            + "; inspect " + BASELINE);
  }

  @Test
  void reportLevelSummaryMustDescribeMoneyStatesInsteadOfRankingAbstractDirections() {
    Map<String, List<String>> hits = new LinkedHashMap<>();
    for (VisibleCase sample : cases) {
      List<String> matched = REPORT_SUMMARY_TEMPLATE_PHRASES.stream()
          .filter(phrase -> sample.lines().stream()
              .filter(line -> isReportLevelSummary(line.sectionId()))
              .anyMatch(line -> line.text().contains(phrase)))
          .toList();
      if (!matched.isEmpty()) hits.put(sample.fixtureId(), matched);
    }
    assertEquals(Map.of(), hits,
        () -> "Abstract report-level summary copy remains: " + hits
            + "; inspect " + BASELINE);
  }

  @Test
  void thesisRouteAndForwardTimelineMustStateTheMoneyChangeAndNextMoveDirectly() {
    Map<String, List<String>> hits = new LinkedHashMap<>();
    for (VisibleCase sample : cases) {
      List<String> matched = THESIS_ROUTE_TIMELINE_TEMPLATE_PHRASES.stream()
          .filter(phrase -> sample.lines().stream()
              .filter(line -> isThesisRouteOrForwardTimeline(line.sectionId()))
              .anyMatch(line -> line.text().contains(phrase)))
          .toList();
      if (!matched.isEmpty()) hits.put(sample.fixtureId(), matched);
    }
    assertEquals(Map.of(), hits,
        () -> "Formulaic thesis/route/forward-timeline copy remains: " + hits
            + "; inspect " + BASELINE);
  }

  @Test
  void thesisAndForwardTimelineMustNotUseAnUnresolvedMoneySubject() {
    Map<String, List<String>> hits = unresolvedSubjectHits(cases);
    assertEquals(Map.of(), hits,
        () -> "Thesis/current/future timeline still contains an unresolved subject: " + hits
            + "; inspect " + BASELINE);
  }

  @Test
  void presentTimelineMustNameTheConcreteMoneyObjectBeingChecked() {
    Map<String, List<String>> failures = presentTimelineObjectFailures(cases);
    assertEquals(Map.of(), failures,
        () -> "Present timeline does not name its concrete money object: " + failures
            + "; inspect " + BASELINE);
  }

  @Test
  void oneForwardTimelineLineMustNotDominateMoreThanTwoFixedReports() {
    Map<String, List<String>> dominant = dominantForwardTimelineLines(cases);
    assertEquals(Map.of(), dominant,
        () -> "One present/future timeline line still dominates the fixed corpus: " + dominant
            + "; inspect " + BASELINE);
  }

  @Test
  void controlledVariantsMustRemainNaturalChinese() {
    Map<String, List<String>> hits = new LinkedHashMap<>();
    for (VisibleCase sample : cases) {
      List<String> matched = CONTROLLED_VARIANT_AWKWARD_PHRASES.stream()
          .filter(phrase -> sample.lines().stream().anyMatch(line -> line.text().contains(phrase)))
          .toList();
      if (!matched.isEmpty()) hits.put(sample.fixtureId(), matched);
    }
    assertEquals(Map.of(), hits,
        () -> "Controlled variants contain awkward Chinese: " + hits + "; inspect " + BASELINE);
  }

  @Test
  void oneReportMustNotRepeatTheSameCriticalSentenceAcrossDifferentSections() {
    Map<String, List<DuplicateGroup>> collisions = new LinkedHashMap<>();
    cases.stream().filter(sample -> !sample.unresolvedDuplicateGroups().isEmpty())
        .forEach(sample -> collisions.put(sample.fixtureId(), sample.unresolvedDuplicateGroups()));
    assertEquals(Map.of(), collisions,
        () -> "Critical sentences are repeated across report sections: " + collisions
            + "; inspect " + BASELINE);
  }

  @Test
  void annualBodyMustNotRelyOnTheReaderToHideRepeatedSentencesAcrossYears() {
    Map<String, List<DuplicateGroup>> collisions = new LinkedHashMap<>();
    for (VisibleCase sample : cases) {
      List<DuplicateGroup> annualDuplicates = repeatedAnnualFocusSentences(sample);
      if (!annualDuplicates.isEmpty()) collisions.put(sample.fixtureId(), annualDuplicates);
    }
    assertEquals(Map.of(), collisions,
        () -> "Annual copy still repeats and is merely consolidated by the reader: " + collisions
            + "; inspect " + BASELINE);
  }

  private static List<VisibleLine> visibleLines(WealthNarrativeV3 report) {
    List<VisibleLine> lines = new ArrayList<>();
    add(lines, "thesis", report.thesis().text());
    add(lines, "summary", report.summary().text());
    report.pathSummaries().forEach(path ->
        add(lines, "path." + path.path(), path.reading().text()));
    if (report.riskSummary() != null) add(lines, "riskSummary", report.riskSummary().text());
    if (report.timeline() != null) addTimeline(lines, report.timeline());
    for (WealthNarrativeV3.Year year : report.years()) {
      add(lines, "year." + year.year() + ".overview", year.overview().text());
      for (var block : year.income()) add(lines, block.id(), block.text());
      add(lines, "year." + year.year() + ".retention", year.retention().text());
      if (year.risk() != null) add(lines, "year." + year.year() + ".risk",
          year.risk().reading().text());
      year.observations().forEach(block -> add(lines, block.id(), block.text()));
      year.actions().forEach(block -> add(lines, block.id(), block.text()));
      if (year.comparison() != null) add(lines, "year." + year.year() + ".comparison",
          year.comparison().reading().text());
    }
    report.route().forEach(block -> add(lines, block.id(), block.text()));
    add(lines, "readingNote", report.readingNote().text());
    return List.copyOf(lines);
  }

  private static void addTimeline(List<VisibleLine> lines, NarrativeTimeline timeline) {
    add(lines, "timeline.past.headline", timeline.past().headline());
    for (int i = 0; i < timeline.past().checkpoints().size(); i++) {
      add(lines, "timeline.past.checkpoint." + i, timeline.past().checkpoints().get(i));
    }
    add(lines, "timeline.past.bridge", timeline.past().bridge());
    add(lines, "timeline.present.headline", timeline.present().headline());
    add(lines, "timeline.present.judgment", timeline.present().judgment());
    add(lines, "timeline.present.priority", timeline.present().priority());
    timeline.future().forEach(step -> {
      add(lines, "timeline.future." + step.year() + ".headline", step.headline());
      add(lines, "timeline.future." + step.year() + ".action", step.action());
    });
  }

  private static void add(List<VisibleLine> lines, String sectionId, String text) {
    if (text == null || text.isBlank()) return;
    String[] sentences = text.split("(?<=[。！？])");
    for (int index = 0; index < sentences.length; index++) {
      String normalized = sentences[index].strip();
      if (!normalized.isEmpty()) lines.add(new VisibleLine(sectionId + "." + index, normalized));
    }
  }

  private static List<String> subjectlessHits(List<VisibleLine> lines) {
    return SUBJECTLESS_SOURCE_SKELETONS.stream()
        .filter(skeleton -> lines.stream().anyMatch(line -> line.text().contains(skeleton)))
        .toList();
  }

  private static Map<String, Long> subjectlessHitCounts(List<VisibleCase> samples) {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (String skeleton : SUBJECTLESS_SOURCE_SKELETONS) {
      long count = samples.stream().filter(sample -> sample.subjectlessHits().contains(skeleton)).count();
      if (count > 0) counts.put(skeleton, count);
    }
    return counts;
  }

  private static Map<String, List<String>> unresolvedSubjectHits(List<VisibleCase> samples) {
    Map<String, List<String>> hits = new LinkedHashMap<>();
    for (VisibleCase sample : samples) {
      List<String> matched = THESIS_TIMELINE_UNRESOLVED_SUBJECTS.stream()
          .filter(phrase -> sample.lines().stream()
              .filter(line -> isThesisOrForwardTimeline(line.sectionId()))
              .anyMatch(line -> line.text().contains(phrase)))
          .toList();
      if (!matched.isEmpty()) hits.put(sample.fixtureId(), matched);
    }
    return hits;
  }

  private static Map<String, List<String>> presentTimelineObjectFailures(
      List<VisibleCase> samples) {
    Map<String, List<String>> failures = new LinkedHashMap<>();
    for (VisibleCase sample : samples) {
      List<String> incomplete = sample.lines().stream()
          .filter(line -> line.sectionId().startsWith("timeline.present.headline.")
              || line.sectionId().startsWith("timeline.present.judgment."))
          .filter(line -> CONCRETE_MONEY_OBJECTS.stream()
              .noneMatch(object -> line.text().contains(object)))
          .map(VisibleLine::text)
          .toList();
      if (!incomplete.isEmpty()) failures.put(sample.fixtureId(), incomplete);
    }
    return failures;
  }

  private static Map<String, List<String>> dominantForwardTimelineLines(List<VisibleCase> samples) {
    Map<String, List<String>> fixturesByLine = new LinkedHashMap<>();
    for (VisibleCase sample : samples) {
      sample.lines().stream()
          .filter(line -> line.sectionId().startsWith("timeline.present.")
              || line.sectionId().startsWith("timeline.future."))
          .forEach(line -> fixturesByLine.computeIfAbsent(line.text(), ignored -> new ArrayList<>())
              .add(sample.fixtureId()));
    }
    Map<String, List<String>> dominant = new LinkedHashMap<>();
    fixturesByLine.forEach((line, fixtureIds) -> {
      List<String> distinct = fixtureIds.stream().distinct().toList();
      if (distinct.size() > 2) dominant.put(line, distinct);
    });
    return dominant;
  }

  private static List<DuplicateGroup> duplicateGroups(List<VisibleLine> lines) {
    Map<String, List<String>> sectionsByText = new LinkedHashMap<>();
    lines.forEach(line -> sectionsByText
        .computeIfAbsent(line.text(), ignored -> new ArrayList<>()).add(line.sectionId()));
    return sectionsByText.entrySet().stream()
        .map(entry -> new DuplicateGroup(entry.getKey(),
            new ArrayList<>(new LinkedHashSet<>(entry.getValue()))))
        .filter(group -> group.sectionIds().size() > 1)
        .toList();
  }

  private static List<DuplicateGroup> unresolvedDuplicateGroups(List<VisibleLine> lines) {
    return duplicateGroups(lines).stream().filter(group -> {
      List<String> categories = group.sectionIds().stream()
          .map(WealthVisibleCopyQualityTest::consolidatedAnnualCategory).distinct().toList();
      return categories.size() != 1 || categories.get(0) == null;
    }).toList();
  }

  private static List<DuplicateGroup> repeatedAnnualFocusSentences(VisibleCase sample) {
    return sample.duplicateGroups().stream()
        .filter(group -> group.sectionIds().stream()
            .map(WealthVisibleCopyQualityTest::consolidatedAnnualCategory)
            .allMatch(category -> category != null && !category.equals("income")))
        .toList();
  }

  private static String consolidatedAnnualCategory(String sectionId) {
    if (sectionId.matches("\\d{4}\\.income\\..*")) return "income";
    if (sectionId.matches("year\\.\\d{4}\\.retention\\..*")) return "retention";
    if (sectionId.matches("year\\.\\d{4}\\.risk\\..*")) return "risk";
    if (sectionId.matches("\\d{4}\\.observation\\..*")) return "observation";
    if (sectionId.matches("\\d{4}\\.action\\..*")) return "action";
    // Comparison paragraphs are not consolidated by the reader, so their sentences must differ.
    return null;
  }

  private static boolean isAnnualBody(String sectionId) {
    return sectionId.matches("\\d{4}\\.(income|observation|action)\\..*")
        || sectionId.matches("year\\.\\d{4}\\.(retention|risk)\\..*");
  }

  private static boolean isReportLevelSummary(String sectionId) {
    return sectionId.equals("summary.0")
        || sectionId.equals("summary.1")
        || sectionId.startsWith("path.")
        || sectionId.startsWith("riskSummary")
        || sectionId.startsWith("readingNote");
  }

  private static boolean isThesisRouteOrForwardTimeline(String sectionId) {
    return sectionId.startsWith("thesis.")
        || sectionId.startsWith("wealth.route.")
        || sectionId.startsWith("timeline.present.")
        || sectionId.startsWith("timeline.future.");
  }

  private static boolean isThesisOrForwardTimeline(String sectionId) {
    return sectionId.startsWith("thesis.")
        || sectionId.startsWith("timeline.present.")
        || sectionId.startsWith("timeline.future.");
  }

  private record VisibleLine(String sectionId, String text) {}

  private record DuplicateGroup(String text, List<String> sectionIds) {}

  private record VisibleCase(String fixtureId, String copyVersion, List<VisibleLine> lines,
      List<String> subjectlessHits, List<DuplicateGroup> duplicateGroups,
      List<DuplicateGroup> unresolvedDuplicateGroups) {}
}
