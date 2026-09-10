package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.JSON;
import static com.bazi.app.report.wealth.WealthRemediationFixtures.scoredCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bazi.app.report.wealth.v3.WealthNarrativeV3;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3.Year;
import com.bazi.app.report.wealth.v3.WealthNarrativeWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class WealthNarrativeV3ContractTest {

  @Test
  void historicalSnapshotRemainsSourceAndJsonCompatibleWithoutNullMetadataFields() throws Exception {
    var current = new WealthNarrativeWriter().plan(
        WealthNarrativeV3Test.assessments(scoredCase("S07")), LocalDate.of(2026, 8, 29));
    List<Year> historicalYears = current.years().stream().map(year -> new Year(
        year.year(), year.ganZhi(), year.facts(), year.evidence(), year.decisions(), year.focus(),
        year.overview(), year.income(), year.retention(), year.risk(), year.observations(),
        year.actions(), year.comparison())).toList();
    var historical = new WealthNarrativeV3(current.asOf(), current.zoneId(), current.horizonYears(),
        current.calculationVersion(), current.policyVersion(), "wealth-plain-v3.2",
        current.thesis(), current.summary(), current.pathSummaries(), current.riskSummary(),
        historicalYears, current.route(), current.readingNote());

    assertNull(historical.headlinePlannerVersion());
    assertNull(historical.timeline());
    assertNull(historical.years().get(0).headlineMeta());
    var json = JSON.readTree(JSON.writeValueAsString(historical));
    assertFalse(json.has("headlinePlannerVersion"));
    assertFalse(json.has("timeline"));
    assertFalse(json.at("/years/0").has("headlineMeta"));
    assertEquals(historical, JSON.treeToValue(json, WealthNarrativeV3.class));
  }

  @Test
  void v33SnapshotCanCarryReportAndAnnualHeadlineMetadata() throws Exception {
    var current = new WealthNarrativeWriter().plan(
        WealthNarrativeV3Test.assessments(scoredCase("S07")), LocalDate.of(2026, 8, 29));

    var json = JSON.readTree(JSON.writeValueAsString(current));
    assertEquals("wealth-headline-v3", json.get("headlinePlannerVersion").asText());
    assertEquals("stable_receipt_support", json.at("/years/0/headlineMeta/themeKey").asText());
    assertEquals(current, JSON.treeToValue(json, WealthNarrativeV3.class));
  }

  @Test
  void historicalV3JsonWithoutTimelineStillDeserializes() throws Exception {
    var current = new WealthNarrativeWriter().plan(
        WealthNarrativeV3Test.assessments(scoredCase("S07")), LocalDate.of(2026, 8, 29));
    var legacyJson = JSON.valueToTree(current);
    ((com.fasterxml.jackson.databind.node.ObjectNode) legacyJson).remove("timeline");

    WealthNarrativeV3 restored = JSON.treeToValue(legacyJson, WealthNarrativeV3.class);

    assertNull(restored.timeline());
    assertEquals(current.years(), restored.years());
  }

  @Test
  void sharedWealthSchemaAllowsTheOptionalV4TimelineField() throws Exception {
    Path schemaPath = Path.of(
        System.getProperty("user.dir"), "..", "..", "contracts", "drafts",
        "wealth-v3.schema.json").normalize();
    var schema = JSON.readTree(schemaPath.toFile());

    assertEquals(
        "#/definitions/NarrativeTimeline",
        schema.at("/definitions/Content/properties/timeline/$ref").asText());
  }
}
