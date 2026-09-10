package com.bazi.app.report.classic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.domain.constants.WuXingConstants;
import com.bazi.app.domain.constants.ZiZuoConstants;
import com.bazi.app.dto.HideGanDto;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.PillarDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.ReportAnalysis;
import com.bazi.app.report.TenGodGroup;
import com.bazi.app.report.wealth.WealthFactExtractor;
import com.bazi.app.report.wealth.WealthNatalProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlf.calendar.util.LunarUtil;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Compatibility with reviewed statements in classical texts. This suite does not treat a
 * transmitted case, a matching rule, or a passing test as evidence of real-world prediction
 * accuracy.
 */
class ClassicBaziCompatibilityTest {

  private static final String RESOURCE = "/report-golden/classic-bazi-compatibility-v1.json";
  private static final String CALIBRATION_RESOURCE =
      "/report-golden/classic-bazi-calibration-v2.json";
  private static final String HOLDOUT_RESOURCE = "/report-golden/classic-bazi-holdout-v2.json";
  private static final Set<String> VALID_GAN = Set.of("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸");
  private static final Set<String> VALID_ZHI = Set.of("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥");
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void classicFixturesDeclareTraceableSourcesAndStructuralScope() throws Exception {
    JsonNode suite = load();

    assertEquals(1, suite.path("schemaVersion").asInt());
    assertEquals("CLASSICAL_RULE_COMPATIBILITY_ONLY", suite.path("purpose").asText());
    assertFalse(suite.path("accuracyEvidence").asBoolean(true));
    assertTrue(suite.path("cases").size() >= 2, "classic suite needs more than one case");

    for (JsonNode fixture : suite.path("cases")) {
      String id = requiredText(fixture, "id");
      assertEquals("STRUCTURAL_ONLY", requiredText(fixture, "assertionScope"), id);
      JsonNode source = fixture.path("source");
      assertFalse(requiredText(source, "title").isBlank(), id);
      assertFalse(requiredText(source, "edition").isBlank(), id);
      assertFalse(requiredText(source, "locator").isBlank(), id);
      assertTrue(requiredText(source, "url").startsWith("https://"), id);
      assertTrue(Set.of("A", "B", "C").contains(requiredText(source, "provenanceGrade")), id);
      assertFalse(fixture.has("historicalOutcomeExpected"),
          id + " must not turn a transmitted biography into an accuracy assertion");

      JsonNode pillars = fixture.path("canonicalInput").path("pillars");
      assertEquals(4, pillars.size(), id);
      pillars.forEach(item -> assertTrue(validGanZhi(item.asText()), id + ": " + item.asText()));
      assertFalse(requiredText(fixture.path("classicalClaim"), "paraphrase").isBlank(), id);
      assertFalse(requiredText(fixture.path("classicalClaim"), "mappingRationale").isBlank(), id);
    }
  }

  @Test
  void currentEngineMatchesReviewedClassicalStructureMappings() throws Exception {
    JsonNode suite = load();

    for (JsonNode fixture : suite.path("cases")) {
      String id = requiredText(fixture, "id");
      PaipanResultDto chart = chartFromCanonicalPillars(fixture.path("canonicalInput").path("pillars"));
      ReportAnalysis analysis = ReportAnalysis.from(
          new PaipanRequest(id, requiredText(fixture.path("canonicalInput"), "gender"),
              "1900-01-01T12:00:00", "古籍四柱直录", false),
          chart);
      JsonNode expected = fixture.path("expectedCurrentEngine");

      assertEquals(requiredText(expected, "balanceLevel"), analysis.wangShuaiLevel(), id);
      assertEquals(expected.path("wangShuaiScore").asDouble(), analysis.wangShuaiScore(), 0.0001, id);

      WealthNatalProfile profile = new WealthFactExtractor().extractNatal(
          chart,
          new AnnualContext(2025, "乙巳", "正印", TenGodGroup.RESOURCE, null,
              List.of(), List.of(), List.of(), analysis));
      Set<String> evidenceKeys = profile.evidence().stream()
          .map(item -> item.key())
          .collect(Collectors.toSet());
      for (JsonNode required : expected.path("requiredWealthEvidenceKeys")) {
        assertTrue(evidenceKeys.contains(required.asText()), id + " missing " + required.asText());
      }
    }
  }

  @Test
  void pairedCasesPreserveTheClassicalRelativeDifference() throws Exception {
    JsonNode suite = load();
    Map<String, ReportAnalysis> analyses = new LinkedHashMap<>();
    for (JsonNode fixture : suite.path("cases")) {
      String id = requiredText(fixture, "id");
      PaipanResultDto chart = chartFromCanonicalPillars(fixture.path("canonicalInput").path("pillars"));
      analyses.put(id, ReportAnalysis.from(
          new PaipanRequest(id, requiredText(fixture.path("canonicalInput"), "gender"),
              "1900-01-01T12:00:00", "古籍四柱直录", false),
          chart));
    }

    for (JsonNode comparison : suite.path("comparisons")) {
      ReportAnalysis lower = analyses.get(requiredText(comparison, "lowerCaseId"));
      ReportAnalysis higher = analyses.get(requiredText(comparison, "higherCaseId"));
      assertNotNull(lower, comparison.toString());
      assertNotNull(higher, comparison.toString());
      assertTrue(lower.wangShuaiScore() < higher.wangShuaiScore(),
          requiredText(comparison, "id") + " should preserve the source's relative distinction");
    }
  }

  @Test
  void expandedCorpusSeparatesCalibrationFromHoldout() throws Exception {
    JsonNode calibration = load(CALIBRATION_RESOURCE);
    JsonNode holdout = load(HOLDOUT_RESOURCE);

    assertCorpus(calibration, "CALIBRATION", 6, 3);
    assertCorpus(holdout, "HOLDOUT", 2, 1);
    assertEquals("SOURCE_FIRST_NO_RETROACTIVE_RELABEL",
        requiredText(holdout, "freezePolicy"));

    Set<String> calibrationIds = caseIds(calibration);
    Set<String> holdoutIds = caseIds(holdout);
    assertTrue(calibrationIds.stream().noneMatch(holdoutIds::contains),
        "holdout cases must not also appear in calibration");
  }

  @Test
  void declaredPairCompatibilityMatchesTheCurrentEngine() throws Exception {
    assertDeclaredComparisons(load(CALIBRATION_RESOURCE));
    assertDeclaredComparisons(load(HOLDOUT_RESOURCE));
  }

  @Test
  void hiddenRootsBreakTheHoldoutTieInTheShadowScore() throws Exception {
    JsonNode holdout = load(HOLDOUT_RESOURCE);
    ReportAnalysis lower = analysisFor(holdout.path("cases").get(0));
    ReportAnalysis higher = analysisFor(holdout.path("cases").get(1));
    Method score = assertDoesNotThrow(
        () -> ReportAnalysis.class.getMethod("rootedSupportScore"));

    double lowerScore = (double) score.invoke(lower);
    double higherScore = (double) score.invoke(higher);

    assertTrue(lowerScore < higherScore,
        "secondary hidden roots must distinguish the source-relative direction");
    assertEquals("偏弱", lower.wangShuaiLevel(), "shadow scoring must not change serving output");
    assertEquals("偏弱", higher.wangShuaiLevel(), "shadow scoring must not change serving output");
  }

  @Test
  void hiddenRootShadowScoreKeepsAnAuditableReason() throws Exception {
    JsonNode fixture = load(HOLDOUT_RESOURCE).path("cases").get(1);
    ReportAnalysis analysis = analysisFor(fixture);

    String value = assertDoesNotThrow(() -> analysis.fact("rootedSupport")).value();

    assertTrue(value.contains("戌藏丁"), value);
  }

  private JsonNode load() throws Exception {
    return load(RESOURCE);
  }

  private JsonNode load(String resource) throws Exception {
    try (InputStream input = getClass().getResourceAsStream(resource)) {
      assertNotNull(input, "classic compatibility fixture must exist: " + resource);
      return json.readTree(input);
    }
  }

  private void assertCorpus(JsonNode corpus, String partition, int caseCount, int pairCount) {
    assertEquals(2, corpus.path("schemaVersion").asInt());
    assertEquals("CLASSICAL_RULE_COMPATIBILITY_ONLY", requiredText(corpus, "purpose"));
    assertFalse(corpus.path("accuracyEvidence").asBoolean(true));
    assertEquals(partition, requiredText(corpus, "partition"));
    assertEquals(caseCount, corpus.path("cases").size());
    assertEquals(pairCount, corpus.path("comparisons").size());

    Set<String> ids = caseIds(corpus);
    for (JsonNode fixture : corpus.path("cases")) {
      String id = requiredText(fixture, "id");
      assertEquals("STRUCTURAL_ONLY", requiredText(fixture, "assertionScope"), id);
      assertEquals(partition, requiredText(fixture, "partition"), id);
      assertEquals("SOURCE_RECORDED_BEFORE_ENGINE_REVIEW",
          requiredText(fixture, "captureOrder"), id);
      assertFalse(fixture.has("historicalOutcomeExpected"), id);
      JsonNode source = fixture.path("source");
      assertFalse(requiredText(source, "locator").isBlank(), id);
      assertTrue(requiredText(source, "url").startsWith("https://"), id);
      JsonNode pillars = fixture.path("canonicalInput").path("pillars");
      assertEquals(4, pillars.size(), id);
      pillars.forEach(item -> assertTrue(validGanZhi(item.asText()), id + ": " + item.asText()));
    }

    for (JsonNode comparison : corpus.path("comparisons")) {
      assertTrue(ids.contains(requiredText(comparison, "lowerCaseId")), comparison.toString());
      assertTrue(ids.contains(requiredText(comparison, "higherCaseId")), comparison.toString());
      assertEquals("wangShuaiScore", requiredText(comparison, "metric"));
      assertTrue(Set.of("LOWER_THAN", "EQUAL", "GREATER_THAN")
          .contains(requiredText(comparison, "sourceExpectedRelation")));
      assertTrue(Set.of("LOWER_THAN", "EQUAL", "GREATER_THAN")
          .contains(requiredText(comparison, "observedEngineRelation")));
      assertTrue(Set.of("MATCH", "KNOWN_GAP")
          .contains(requiredText(comparison, "engineCompatibility")));
      if ("KNOWN_GAP".equals(requiredText(comparison, "engineCompatibility"))) {
        assertFalse(requiredText(comparison, "gapCode").isBlank());
        assertFalse(requiredText(comparison, "gapReason").isBlank());
      }
    }
  }

  private Set<String> caseIds(JsonNode corpus) {
    return corpus.path("cases").findValuesAsText("id").stream().collect(Collectors.toSet());
  }

  private void assertDeclaredComparisons(JsonNode corpus) {
    Map<String, Double> scores = new LinkedHashMap<>();
    for (JsonNode fixture : corpus.path("cases")) {
      PaipanResultDto chart = chartFromCanonicalPillars(fixture.path("canonicalInput").path("pillars"));
      ReportAnalysis analysis = ReportAnalysis.from(
          new PaipanRequest(requiredText(fixture, "id"),
              requiredText(fixture.path("canonicalInput"), "gender"),
              "1900-01-01T12:00:00", "古籍四柱直录", false),
          chart);
      scores.put(requiredText(fixture, "id"), analysis.wangShuaiScore());
    }

    for (JsonNode comparison : corpus.path("comparisons")) {
      String id = requiredText(comparison, "id");
      double lower = scores.get(requiredText(comparison, "lowerCaseId"));
      double higher = scores.get(requiredText(comparison, "higherCaseId"));
      String observed = lower < higher ? "LOWER_THAN" : lower > higher ? "GREATER_THAN" : "EQUAL";
      String declaredObserved = requiredText(comparison, "observedEngineRelation");
      String expected = requiredText(comparison, "sourceExpectedRelation");
      String compatibility = requiredText(comparison, "engineCompatibility");
      assertEquals(declaredObserved, observed,
          id + " engine relation changed; review the fixture instead of silently relabeling it");
      assertEquals(expected.equals(observed) ? "MATCH" : "KNOWN_GAP", compatibility,
          id + " compatibility label must be derived from source and observed relations");
    }
  }

  private ReportAnalysis analysisFor(JsonNode fixture) {
    PaipanResultDto chart = chartFromCanonicalPillars(fixture.path("canonicalInput").path("pillars"));
    return ReportAnalysis.from(
        new PaipanRequest(requiredText(fixture, "id"),
            requiredText(fixture.path("canonicalInput"), "gender"),
            "1900-01-01T12:00:00", "古籍四柱直录", false),
        chart);
  }

  private String requiredText(JsonNode node, String field) {
    JsonNode value = node.get(field);
    assertNotNull(value, "missing field: " + field);
    assertTrue(value.isTextual(), "field must be text: " + field);
    return value.asText();
  }

  private boolean validGanZhi(String value) {
    return value != null && value.length() == 2
        && VALID_GAN.contains(value.substring(0, 1))
        && VALID_ZHI.contains(value.substring(1, 2));
  }

  private PaipanResultDto chartFromCanonicalPillars(JsonNode values) {
    List<String> ganZhi = new ArrayList<>();
    values.forEach(item -> ganZhi.add(item.asText()));
    String dayGan = ganZhi.get(2).substring(0, 1);
    Map<String, PillarDto> pillars = new LinkedHashMap<>();
    List<String> keys = List.of("year", "month", "day", "time");
    Map<String, Integer> wuXing = new LinkedHashMap<>(Map.of(
        "jin", 0, "mu", 0, "shui", 0, "huo", 0, "tu", 0));

    for (int index = 0; index < keys.size(); index++) {
      String value = ganZhi.get(index);
      String gan = value.substring(0, 1);
      String zhi = value.substring(1, 2);
      List<HideGanDto> hidden = LunarUtil.ZHI_HIDE_GAN.getOrDefault(zhi, List.of()).stream()
          .map(hiddenGan -> new HideGanDto(
              hiddenGan,
              LunarUtil.SHI_SHEN.get(dayGan + hiddenGan),
              WuXingConstants.GAN_WUXING.get(hiddenGan)))
          .toList();
      pillars.put(keys.get(index), new PillarDto(
          keys.get(index), gan, zhi,
          index == 2 ? "日主" : LunarUtil.SHI_SHEN.get(dayGan + gan),
          ZiZuoConstants.ziZuo(gan, zhi), hidden,
          LunarUtil.NAYIN.getOrDefault(value, ""), "", LunarUtil.getXunKong(value), List.of()));
      wuXing.merge(WuXingConstants.GAN_WUXING.get(gan), 1, Integer::sum);
      wuXing.merge(WuXingConstants.ZHI_WUXING.get(zhi), 1, Integer::sum);
    }

    return new PaipanResultDto(
        "古籍四柱直录", "古籍四柱直录", "", ganZhi.get(3).substring(1, 2), pillars,
        "", "", "", "", "", "", wuXing,
        List.of(), null, List.of(), List.of(), List.of(), "", null, null);
  }
}
