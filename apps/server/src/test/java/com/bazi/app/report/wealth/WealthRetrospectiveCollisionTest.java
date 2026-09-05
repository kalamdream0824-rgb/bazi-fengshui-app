package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.wealth.v3.DefaultWealthReportGenerator;
import com.bazi.app.report.wealth.v3.WealthRetrospectiveArbitrator;
import com.bazi.app.report.wealth.v3.WealthRetrospectivePlan;
import com.bazi.app.report.wealth.v3.WealthV3Analyzer;
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
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Red acceptance gate for v3.5; fixture reports are not evidence of real-world accuracy. */
class WealthRetrospectiveCollisionTest {
  private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
  private static final Path INPUTS = Path.of(
      "src/test/resources/report/wealth-v2-baseline-inputs.json");
  private static final Path BASELINE = Path.of(
      "target/wealth-retrospective-collision-baseline.json");
  private static List<ReviewCase> cases;

  @BeforeAll
  static void captureBaselineBeforeAcceptanceAssertions() throws Exception {
    JsonNode inputs = JSON.readTree(INPUTS.toFile());
    ZoneId zone = ZoneId.of(inputs.get("zoneId").asText());
    List<ReviewCase> generated = new ArrayList<>();
    for (JsonNode fixture : inputs.get("fullBirthCases")) {
      LocalDate asOf = LocalDate.parse(fixture.get("asOf").asText());
      Clock clock = Clock.fixed(asOf.atStartOfDay(zone).toInstant(), zone);
      PaipanRequest request = JSON.treeToValue(fixture.get("request"), PaipanRequest.class);
      var chart = new BaziService().paipan(request);
      AnnualContextFactory factory = new AnnualContextFactory(clock);
      var product = factory.create(request, chart, ReportHorizon.WEALTH_PRODUCT);
      var previous = factory.createYear(request, chart, product.get(0).year() - 1);
      var contexts = new ArrayList<com.bazi.app.report.AnnualContext>();
      contexts.add(previous);
      contexts.addAll(product);
      var previousAssessment = new WealthV3Analyzer().analyze(chart, contexts).get(0);
      var arbitrator = new WealthRetrospectiveArbitrator();
      WealthRetrospectivePlan plan = arbitrator.plan(previousAssessment);
      for (int repetition = 0; repetition < 100; repetition++) {
        assertEquals(plan, arbitrator.plan(previousAssessment),
            fixture.get("id").asText() + " repetition " + repetition);
      }
      var report = new DefaultWealthReportGenerator().generate(chart, previous, product, asOf);
      assertEquals("wealth-plain-v3.5", report.copyVersion());
      var past = report.timeline().past();
      assertEquals(asOf.getYear() - 1, past.year());
      assertTrue(!past.evidenceKeys().isEmpty()
          && past.evidenceKeys().stream().noneMatch(String::isBlank));
      assertEquals(plan.evidenceKeys(), past.evidenceKeys());
      generated.add(reviewCase(fixture.get("id").asText(), plan, past));
    }
    cases = List.copyOf(generated);

    Map<String, Object> output = new LinkedHashMap<>();
    output.put("signatureVersion", WealthRetrospectivePlan.VERSION);
    output.put("signatureDefinition",
        "WealthRetrospectivePlan.evidenceSignature: SHA-256 of the planner version, year, "
            + "selected subjects, directions, strengths, angles, cited rules and resolved root values; "
            + "no fixture identity, copy or wall-clock time");
    output.put("collisionCountDefinition",
        "Unordered pairs of cases with identical visibleBlock and different evidenceSignature");
    output.put("largestGroupDefinition",
        "Maximum distinct evidence signatures per visibleBlock; 1 means no collision");
    output.put("notice", "Fixed development fixtures only; not real-world accuracy evidence");
    output.put("sampleCount", cases.size());
    output.put("uniqueSignatureCount", cases.stream()
        .map(ReviewCase::evidenceSignature).distinct().count());
    output.put("uniqueVisibleBlockCount", cases.stream()
        .map(ReviewCase::visibleBlock).distinct().count());
    output.put("differentSignatureSameBlockCount", differentSignatureSameBlockCount(cases));
    output.put("largestDifferentSignatureCollisionGroup", largestDifferentSignatureCollisionGroup(cases));
    output.put("collisionGroups", collisionGroups(cases));
    output.put("cases", cases);
    Files.createDirectories(BASELINE.getParent());
    Files.writeString(BASELINE, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(output) + "\n");
  }

  @Test
  void baselineContainsExactlyTheTwelveFullBirthFixtures() {
    assertEquals(IntStream.rangeClosed(1, 12).mapToObj(i -> "R%02d".formatted(i)).toList(),
        cases.stream().map(ReviewCase::fixtureId).toList());
  }

  @Test
  void differentEvidenceSignaturesMustNotProduceIdenticalCompleteReviews() {
    assertEquals(0, differentSignatureSameBlockCount(cases),
        () -> "Different evidence was collapsed into identical visible reviews: "
            + collisionGroups(cases) + "; inspect " + BASELINE);
  }

  @Test
  void r03R04R05HaveDifferentEvidenceAndMustNotShareOneCompleteReview() {
    List<ReviewCase> regression = cases.stream()
        .filter(sample -> List.of("R03", "R04", "R05").contains(sample.fixtureId())).toList();
    assertEquals(3, regression.size());
    assertEquals(3, regression.stream().map(ReviewCase::evidenceSignature).distinct().count(),
        "The known regression requires three distinct effective evidence sets");
    assertTrue(regression.stream().map(ReviewCase::visibleBlock).distinct().count() > 1,
        "R03/R04/R05 cite different evidence but all show the same complete review; inspect " + BASELINE);
  }

  private static ReviewCase reviewCase(
      String fixtureId, WealthRetrospectivePlan plan, NarrativeTimeline.PastReview past) {
    List<String> keys = past.evidenceKeys().stream().distinct().sorted().toList();
    List<String> lines = new ArrayList<>();
    lines.add(past.headline());
    lines.addAll(past.checkpoints());
    lines.add(past.bridge());
    return new ReviewCase(fixtureId, plan.plannerVersion(), plan.completeness(),
        plan.evidenceSignature(), String.join("\n", lines), keys,
        past.headline(), past.checkpoints().get(0), past.checkpoints().get(1), past.bridge());
  }

  private static int differentSignatureSameBlockCount(List<ReviewCase> samples) {
    int count = 0;
    for (int left = 0; left < samples.size(); left++) {
      for (int right = left + 1; right < samples.size(); right++) {
        ReviewCase a = samples.get(left);
        ReviewCase b = samples.get(right);
        if (a.visibleBlock().equals(b.visibleBlock())
            && !a.evidenceSignature().equals(b.evidenceSignature())) count++;
      }
    }
    return count;
  }

  private static Map<String, List<ReviewCase>> byVisibleBlock(List<ReviewCase> samples) {
    Map<String, List<ReviewCase>> groups = new LinkedHashMap<>();
    samples.forEach(sample -> groups.computeIfAbsent(sample.visibleBlock(), key -> new ArrayList<>())
        .add(sample));
    return groups;
  }

  private static long largestDifferentSignatureCollisionGroup(List<ReviewCase> samples) {
    return byVisibleBlock(samples).values().stream()
        .mapToLong(group -> group.stream().map(ReviewCase::evidenceSignature).distinct().count())
        .max().orElse(0);
  }

  private static List<List<String>> collisionGroups(List<ReviewCase> samples) {
    return byVisibleBlock(samples).values().stream()
        .filter(group -> group.stream().map(ReviewCase::evidenceSignature).distinct().count() > 1)
        .map(group -> group.stream().map(ReviewCase::fixtureId).toList()).toList();
  }

  private record ReviewCase(
      String fixtureId, String plannerVersion, String completeness, String evidenceSignature,
      String visibleBlock, List<String> evidenceKeys, String primary, String secondary,
      String hidden, String bridge) {}
}
