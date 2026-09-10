package com.bazi.app.report.classic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RootedSupportShadowAuditTest {

  private static final Path INPUTS = Path.of(
      "src/test/resources/report/wealth-v2-baseline-inputs.json");
  private static final Path SNAPSHOT = Path.of(
      "src/test/resources/report-golden/rooted-support-shadow-audit-v1.json");
  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void auditsAllFixedBirthCasesWithoutChangingServingOutput() throws Exception {
    RootedSupportShadowAudit.Summary summary = RootedSupportShadowAudit.evaluate(INPUTS);

    assertEquals(12, summary.sampleCount());
    assertEquals(11, summary.uniqueChartCount());
    assertTrue(summary.projectionOnly());
    assertTrue(summary.scoreChangedCount() > 0,
        "the shadow layer should observe at least one secondary root");
    assertEquals(summary.projectedCrossBandCount() * 10,
        summary.minimumPotentiallyAffectedTopicYears());
    assertEquals(summary.projectedCrossBandCount() * 11,
        summary.maximumPotentiallyAffectedTopicYears());

    List<RootedSupportShadowAudit.CaseResult> cases = summary.cases();
    assertEquals(12, cases.size());
    assertEquals(
        List.of("R01", "R02", "R03", "R04", "R05", "R06",
            "R07", "R08", "R09", "R10", "R11", "R12"),
        cases.stream().map(RootedSupportShadowAudit.CaseResult::id).toList());
    for (RootedSupportShadowAudit.CaseResult item : cases) {
      double delta = item.delta();
      assertTrue(delta >= 0 && delta <= 0.4,
          item.id() + " has an invalid root delta: " + delta);
    }
  }

  @Test
  void fixedSampleDistributionMatchesTheReviewedSnapshot() throws Exception {
    JsonNode actual = JSON.valueToTree(RootedSupportShadowAudit.evaluate(INPUTS));
    assertTrue(Files.isRegularFile(SNAPSHOT),
        "missing reviewed shadow audit snapshot:\n"
            + JSON.writerWithDefaultPrettyPrinter().writeValueAsString(actual));

    assertEquals(JSON.readTree(SNAPSHOT.toFile()), actual);
  }

  @Test
  void auditUsesStableDecimalsAndNamesRepeatedPillarRoots() throws Exception {
    RootedSupportShadowAudit.CaseResult r05 = RootedSupportShadowAudit.evaluate(INPUTS).cases()
        .stream()
        .filter(item -> "R05".equals(item.id()))
        .findFirst()
        .orElseThrow();

    assertEquals(1.8, r05.rootedSupportScore());
    assertTrue(r05.evidence().contains("月支未藏乙"), r05.evidence());
    assertTrue(r05.evidence().contains("日支未藏乙"), r05.evidence());
  }
}
