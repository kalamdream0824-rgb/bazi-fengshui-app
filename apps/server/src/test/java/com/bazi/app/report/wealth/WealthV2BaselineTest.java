package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Characterization only: v2 text is recorded, not endorsed as v3's expected behavior. */
class WealthV2BaselineTest {
  static final Path SNAPSHOT = Path.of(
      "src/test/resources/report/wealth-v2-baseline-20260829.json");
  private static final Path INPUTS = Path.of(
      "src/test/resources/report/wealth-v2-baseline-inputs.json");
  private static final ObjectMapper JSON = new ObjectMapper()
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

  @Test
  void frozenV2BaselineExistsBeforeRemediation() throws Exception {
    // Explicit one-time capture only. CREATE_NEW refuses to replace an accepted baseline.
    if (Boolean.getBoolean("wealth.captureBaseline")) {
      Files.writeString(SNAPSHOT, JSON.writerWithDefaultPrettyPrinter()
          .writeValueAsString(capture()) + "\n", StandardOpenOption.CREATE_NEW);
    }
    assertTrue(Files.isRegularFile(SNAPSHOT),
        "Missing frozen wealth v2 baseline; capture the existing engine before remediation");
  }

  @Test
  void currentV2CalculationAndTextMatchFrozenBaseline() throws Exception {
    if (Boolean.getBoolean("wealth.captureBaseline")) return;
    assertTrue(Files.isRegularFile(SNAPSHOT), "Frozen baseline must exist");
    JsonNode expected = JSON.readTree(SNAPSHOT.toFile());
    ObjectNode actual = capture();
    assertEquals(expected.get("caseCount"), actual.get("caseCount"));
    for (int i = 0; i < expected.get("cases").size(); i++) {
      assertEquals(expected.get("cases").get(i), actual.get("cases").get(i),
          expected.get("cases").get(i).get("id").asText());
    }
    assertEquals(expected, actual);
  }

  @Test
  void frozenCommonYearsKeepCalculationAcrossHorizonAndStartYear() throws Exception {
    if (Boolean.getBoolean("wealth.captureBaseline")) return;
    JsonNode baseline = JSON.readTree(SNAPSHOT.toFile());
    JsonNode three = find(baseline, "R01").at("/evaluation/years");
    JsonNode two = find(baseline, "H02").at("/evaluation/years");
    JsonNode five = find(baseline, "H05").at("/evaluation/years");
    JsonNode next = find(baseline, "Y27").at("/evaluation/years");
    for (int i = 0; i < 2; i++) assertEquals(three.get(i), two.get(i));
    for (int i = 0; i < 3; i++) assertEquals(three.get(i), five.get(i));
    for (int i = 0; i < 2; i++) assertEquals(three.get(i + 1), next.get(i));
  }

  @Test
  void allFrozenContentsRemainReadableByLegacyV2Model() throws Exception {
    if (Boolean.getBoolean("wealth.captureBaseline")) return;
    JsonNode baseline = JSON.readTree(SNAPSHOT.toFile());
    for (JsonNode sample : baseline.get("cases")) {
      WealthNarrativePlan legacy = JSON.treeToValue(sample.get("content"), WealthNarrativePlan.class);
      assertEquals(sample.get("content"), JSON.valueToTree(legacy), sample.get("id").asText());
    }
  }

  private static JsonNode find(JsonNode baseline, String id) {
    for (JsonNode sample : baseline.get("cases")) {
      if (sample.get("id").asText().equals(id)) return sample;
    }
    throw new IllegalArgumentException("missing baseline case " + id);
  }

  private static ObjectNode capture() throws Exception {
    JsonNode inputs = JSON.readTree(INPUTS.toFile());
    ObjectNode output = JSON.createObjectNode();
    output.put("baselineCommit", inputs.get("baselineCommit").asText());
    output.put("zoneId", inputs.get("zoneId").asText());
    output.put("notice", "Characterization of v2, not correctness approval or real-world evidence");
    ArrayNode cases = output.putArray("cases");
    for (JsonNode sample : inputs.get("fullBirthCases")) {
      cases.add(fullBirth(sample.get("id").asText(), sample, sample.get("asOf").asText(),
          sample.get("horizonYears").asInt(), inputs.get("zoneId").asText()));
    }
    for (JsonNode sample : inputs.get("technicalCases")) {
      JsonNode original = null;
      for (JsonNode candidate : inputs.get("fullBirthCases")) {
        if (candidate.get("id").asText().equals(sample.get("basedOn").asText())) original = candidate;
      }
      if (original == null) throw new IllegalArgumentException("missing technical fixture source");
      cases.add(fullBirth(sample.get("id").asText(), original, sample.get("asOf").asText(),
          sample.get("horizonYears").asInt(), inputs.get("zoneId").asText()));
    }
    for (JsonNode edge : inputs.get("syntheticCases")) cases.add(synthetic(edge));
    output.put("caseCount", cases.size());
    return output;
  }

  private static ObjectNode fullBirth(
      String id, JsonNode sample, String asOf, int horizon, String zone) throws Exception {
    PaipanRequest request = JSON.treeToValue(sample.get("request"), PaipanRequest.class);
    Clock clock = Clock.fixed(LocalDate.parse(asOf).atStartOfDay(ZoneId.of(zone)).toInstant(),
        ZoneId.of(zone));
    var chart = new BaziService().paipan(request);
    var facts = new WealthFactExtractor().extract(chart,
        new AnnualContextFactory(clock).create(request, chart, ReportHorizon.of(horizon)));
    var evaluation = new WealthPathEvaluator().evaluate(facts);
    ObjectNode result = snapshot(id, "full_birth_input", evaluation);
    result.put("asOf", asOf);
    result.set("request", sample.get("request"));
    result.set("facts", JSON.valueToTree(facts));
    // Deliberately exclude BaziService's wall-clock display fields from this fixed-year baseline.
    result.put("balanceScore", new AnnualContextFactory(clock)
        .create(request, chart, ReportHorizon.of(horizon)).get(0).natalAnalysis().wangShuaiScore());
    return result;
  }

  private static ObjectNode synthetic(JsonNode edge) {
    List<WealthYearEvaluation> years = new ArrayList<>();
    for (int yearIndex = 0; yearIndex < 3; yearIndex++) {
      Map<WealthPath, WealthPathEvaluation> paths = new EnumMap<>(WealthPath.class);
      for (WealthPath path : WealthPath.values()) {
        JsonNode pair = edge.get("weights").get(path.ordinal());
        int support = pair.get(0).asInt();
        int limit = pair.get(1).asInt();
        String prefix = edge.get("id").asText() + "." + path.code();
        if (edge.get("changeSourceByYear").asBoolean()) prefix += ".source" + yearIndex;
        List<WealthPathEvaluation.ScoredEvidence> items = new ArrayList<>();
        if (support > 0) items.add(scored(prefix + ".support", support, EvidenceFamily.ANNUAL_TRIGGER));
        if (limit > 0) items.add(scored(prefix + ".limit", -limit, EvidenceFamily.DAYUN_CONTEXT));
        int score = support - limit;
        // These inputs intentionally enter AFTER scoring. All supports are one source family.
        String status = score >= 3 ? "可以作为补充" : score >= 0 ? "表现一般" : "需要谨慎";
        paths.put(path, new WealthPathEvaluation(path, score, status, items));
      }
      years.add(new WealthArbitrator().arbitrate(
          2026 + yearIndex, List.of("丙午", "丁未", "戊申").get(yearIndex), paths));
    }
    ObjectNode result = snapshot(edge.get("id").asText(), "synthetic_scored_evidence",
        new WealthPeriodEvaluation(years));
    result.set("syntheticInput", edge);
    return result;
  }

  private static WealthPathEvaluation.ScoredEvidence scored(
      String key, int weight, EvidenceFamily family) {
    return new WealthPathEvaluation.ScoredEvidence(
        new WealthEvidence(key, family, "合成测试依据", "非真实命盘条件"), weight);
  }

  private static ObjectNode snapshot(String id, String kind, WealthPeriodEvaluation evaluation) {
    ObjectNode sample = JSON.createObjectNode();
    sample.put("id", id);
    sample.put("kind", kind);
    sample.put("horizonYears", evaluation.years().size());
    sample.put("contentVersion", "wealth-narrative-v2");
    sample.set("evaluation", JSON.valueToTree(evaluation));
    sample.set("content", JSON.valueToTree(new WealthNarrativePlanner().plan(evaluation)));
    return sample;
  }
}
