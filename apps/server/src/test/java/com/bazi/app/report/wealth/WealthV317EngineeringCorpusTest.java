package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.wealth.v3.DefaultWealthReportGenerator;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Engineering acceptance corpus for current wealth plain copy; not real-world accuracy evidence. */
class WealthV317EngineeringCorpusTest {
  private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
  private static final Path INPUTS = Path.of(
      "src/test/resources/report/wealth-v3.17-engineering-corpus.json");
  private static final Path BASELINE = Path.of(
      "target/wealth-v3.17-engineering-corpus-baseline.json");
  private static final List<String> AWKWARD_PHRASES = List.of(
      "按期收到或延期或",
      "转成实际进账已经有一些机会",
      "当前判断更偏向可以",
      "当前更支持",
      "预计中的钱",
      "到账节奏在这份分析里更值得关注");
  private static List<CorpusCase> cases;
  private static Map<String, List<String>> dominantCriticalLines;
  private static Map<String, List<String>> awkwardPhraseHits;

  @BeforeAll
  static void generateCorpusBaseline() throws Exception {
    List<CorpusCase> generated = new ArrayList<>();
    if (Files.exists(INPUTS)) {
      JsonNode root = JSON.readTree(INPUTS.toFile());
      ZoneId zone = ZoneId.of(root.path("zoneId").asText("Asia/Shanghai"));
      for (JsonNode fixture : root.path("cases")) {
        String id = fixture.path("id").asText();
        LocalDate asOf = LocalDate.parse(fixture.path("asOf").asText());
        PaipanRequest request = JSON.treeToValue(fixture.path("request"), PaipanRequest.class);
        Clock clock = Clock.fixed(asOf.atStartOfDay(zone).toInstant(), zone);
        var chart = new BaziService().paipan(request);
        var factory = new AnnualContextFactory(clock);
        var product = factory.create(request, chart, ReportHorizon.WEALTH_PRODUCT);
        var previous = factory.createYear(request, chart, product.get(0).year() - 1);
        var generator = new DefaultWealthReportGenerator();
        WealthNarrativeV3 first;
        WealthNarrativeV3 second;
        try {
          first = generator.generate(chart, previous, product, asOf);
          second = generator.generate(chart, previous, product, asOf);
        } catch (RuntimeException error) {
          throw new IllegalArgumentException("engineering corpus case failed: " + id, error);
        }
        generated.add(new CorpusCase(
            id,
            pillars(chart),
            digest(JSON.writeValueAsString(semanticProjection(first))),
            digest(JSON.writeValueAsString(first)),
            first.copyVersion(),
            JSON.valueToTree(first).equals(JSON.valueToTree(second)),
            first.thesis().text(),
            criticalLines(first),
            allVisibleText(first)));
      }
    }
    cases = List.copyOf(generated);
    dominantCriticalLines = linesUsedByMoreThanThreeReports(cases);
    awkwardPhraseHits = awkwardPhraseHits(cases);

    Map<String, Object> output = new LinkedHashMap<>();
    output.put("notice", "Synthetic engineering corpus only; not real-world accuracy evidence");
    output.put("sampleCount", cases.size());
    output.put("uniquePillarCount", cases.stream().map(CorpusCase::pillars).distinct().count());
    output.put("uniqueSemanticSignatureCount",
        cases.stream().map(CorpusCase::semanticSignature).distinct().count());
    output.put("uniqueReportSignatureCount",
        cases.stream().map(CorpusCase::reportSignature).distinct().count());
    output.put("uniqueThesisCount",
        cases.stream().map(CorpusCase::visibleThesis).distinct().count());
    output.put("dominantCriticalLines", dominantCriticalLines);
    output.put("awkwardPhraseHits", awkwardPhraseHits);
    output.put("semanticCollisionGroups", collisionGroups(cases, true));
    output.put("reportCollisionGroups", collisionGroups(cases, false));
    output.put("cases", cases);
    Files.createDirectories(BASELINE.getParent());
    Files.writeString(BASELINE,
        JSON.writerWithDefaultPrettyPrinter().writeValueAsString(output) + "\n");
  }

  @Test
  void corpusContainsThirtyIndependentFourPillarCharts() {
    assertEquals(30, cases.size(), "engineering corpus must contain exactly 30 inputs");
    assertEquals(30, cases.stream().map(CorpusCase::pillars).distinct().count(),
        "different input rows must not collapse to the same four pillars");
  }

  @Test
  void everyReportUsesCurrentCopyAndIsDeterministic() {
    assertEquals(List.of(), cases.stream()
        .filter(sample -> !"wealth-plain-v3.18".equals(sample.copyVersion())
            || !sample.deterministic())
        .map(CorpusCase::id)
        .toList());
  }

  @Test
  void differentChartsMustNotCollapseToAnIdenticalFullReport() {
    assertEquals(30, cases.stream().map(CorpusCase::reportSignature).distinct().count(),
        () -> "different charts produced identical reports: " + collisionGroups(cases, false)
            + "; inspect " + BASELINE);
  }

  @Test
  void oneCriticalLineMustNotAppearInMoreThanTenPercentOfReports() {
    assertEquals(Map.of(), dominantCriticalLines,
        () -> "critical copy appears in more than 3/30 reports: " + dominantCriticalLines
            + "; inspect " + BASELINE);
  }

  @Test
  void knownAwkwardOrGenericPhrasesMustRemainAbsent() {
    assertEquals(Map.of(), awkwardPhraseHits,
        () -> "known awkward phrases returned: " + awkwardPhraseHits + "; inspect " + BASELINE);
  }

  @Test
  void everyReportContainsAThesisAndForwardTimeline() {
    assertTrue(cases.stream().allMatch(sample -> sample.criticalLines().size() >= 8),
        () -> "missing thesis or forward timeline; inspect " + BASELINE);
  }

  private static String pillars(com.bazi.app.dto.PaipanResultDto chart) {
    return List.of("year", "month", "day", "time").stream()
        .map(key -> chart.pillars().get(key).gan() + chart.pillars().get(key).zhi())
        .reduce((left, right) -> left + "·" + right)
        .orElseThrow();
  }

  private static Object semanticProjection(WealthNarrativeV3 report) {
    return report.years().stream().map(year -> Map.of(
        "year", year.year(),
        "decisions", year.decisions().stream().map(decision -> Map.of(
            "path", decision.path(),
            "stance", decision.stance(),
            "strength", decision.strength(),
            "support", decision.supportingEvidenceIds(),
            "limit", decision.limitingEvidenceIds())).toList())).toList();
  }

  private static String digest(String value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(value.getBytes(StandardCharsets.UTF_8)));
  }

  private static List<String> criticalLines(WealthNarrativeV3 report) {
    LinkedHashSet<String> lines = new LinkedHashSet<>();
    addSentences(lines, report.thesis().text());
    if (report.timeline() != null) {
      lines.add(report.timeline().present().headline());
      lines.add(report.timeline().present().judgment());
      lines.add(report.timeline().present().priority());
      report.timeline().future().forEach(step -> {
        lines.add(step.headline());
        lines.add(step.action());
      });
    }
    report.years().forEach(year -> lines.add(year.overview().text()));
    return List.copyOf(lines);
  }

  private static void addSentences(LinkedHashSet<String> target, String text) {
    for (String sentence : text.split("(?<=[。！？])")) {
      String normalized = sentence.strip();
      if (!normalized.isEmpty()) target.add(normalized);
    }
  }

  private static List<String> allVisibleText(WealthNarrativeV3 report) {
    List<String> values = new ArrayList<>();
    collectText(JSON.valueToTree(report), values);
    return List.copyOf(values);
  }

  private static void collectText(JsonNode node, List<String> values) {
    if (node.isTextual()) {
      values.add(node.asText());
      return;
    }
    if (node.isArray()) {
      node.forEach(child -> collectText(child, values));
      return;
    }
    if (node.isObject()) {
      node.properties().forEach(entry -> collectText(entry.getValue(), values));
    }
  }

  private static Map<String, List<String>> linesUsedByMoreThanThreeReports(
      List<CorpusCase> samples) {
    Map<String, List<String>> idsByLine = new LinkedHashMap<>();
    samples.forEach(sample -> sample.criticalLines().forEach(line ->
        idsByLine.computeIfAbsent(line, ignored -> new ArrayList<>()).add(sample.id())));
    Map<String, List<String>> result = new LinkedHashMap<>();
    idsByLine.forEach((line, ids) -> {
      List<String> distinct = ids.stream().distinct().toList();
      if (distinct.size() > 3) result.put(line, distinct);
    });
    return result;
  }

  private static Map<String, List<String>> awkwardPhraseHits(List<CorpusCase> samples) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    samples.forEach(sample -> {
      List<String> hits = AWKWARD_PHRASES.stream()
          .filter(phrase -> sample.allVisibleText().stream().anyMatch(text -> text.contains(phrase)))
          .toList();
      if (!hits.isEmpty()) result.put(sample.id(), hits);
    });
    return result;
  }

  private static List<List<String>> collisionGroups(List<CorpusCase> samples, boolean semantic) {
    Map<String, List<String>> idsBySignature = new LinkedHashMap<>();
    samples.forEach(sample -> idsBySignature
        .computeIfAbsent(semantic ? sample.semanticSignature() : sample.reportSignature(),
            ignored -> new ArrayList<>())
        .add(sample.id()));
    return idsBySignature.values().stream().filter(ids -> ids.size() > 1).toList();
  }

  private record CorpusCase(
      String id,
      String pillars,
      String semanticSignature,
      String reportSignature,
      String copyVersion,
      boolean deterministic,
      String visibleThesis,
      List<String> criticalLines,
      List<String> allVisibleText) {}
}
