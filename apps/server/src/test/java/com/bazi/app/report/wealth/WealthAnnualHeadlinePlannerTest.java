package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.compareScored;
import static com.bazi.app.report.wealth.WealthRemediationFixtures.read;
import static com.bazi.app.report.wealth.WealthRemediationFixtures.scoredCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.report.wealth.v3.WealthAnnualHeadlinePlanner;
import com.bazi.app.report.wealth.v3.WealthHeadlinePlanningException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WealthAnnualHeadlinePlannerTest {
  private final WealthAnnualHeadlinePlanner planner = new WealthAnnualHeadlinePlanner();

  @Test
  void fixtureManifestCoversNormalTieAndFailureBoundaries() throws Exception {
    var cases = read("wealth-headline-planner-cases.json").get("plannerCases");
    assertEquals(List.of("normal", "deterministic_tie", "insufficient"),
        java.util.stream.StreamSupport.stream(cases.spliterator(), false)
            .map(node -> node.get("id").asText()).toList());
  }

  @Test
  void reportPlanUsesDistinctTraceableSemantics() throws Exception {
    var compared = compareScored(scoredCase("S02"));
    var headlines = planner.plan(compared);

    assertEquals(compared.size(), headlines.size());
    assertEquals(headlines.size(), headlines.stream().map(WealthAnnualHeadlinePlanner.Headline::year).distinct().count());
    assertEquals(headlines.size(), headlines.stream().map(WealthAnnualHeadlinePlanner.Headline::themeKey).distinct().count());
    assertEquals(headlines.size(), headlines.stream().map(WealthAnnualHeadlinePlanner.Headline::subjectKey).distinct().count());

    Set<String> phrases = new HashSet<>();
    for (int i = 0; i < headlines.size(); i++) {
      var headline = headlines.get(i);
      var assessment = compared.get(i).assessment();
      assertFalse(headline.pathKey().isBlank());
      assertFalse(headline.objectKey().isBlank());
      assertTrue(headline.candidateRank() >= 1);
      assertTrue(headline.candidateRank() <= headline.candidateCount());
      assertEquals(1, headline.decisionIds().size());
      assertTrue(assessment.decisions().stream().anyMatch(decision ->
          decision.id().equals(headline.decisionIds().get(0))
              && decision.year() == headline.year()
              && decision.path().equals(headline.pathKey())));
      for (String phrase : headline.corePhraseKeys()) assertTrue(phrases.add(phrase), phrase);
    }

    for (WealthPath path : WealthPath.values()) {
      assertTrue(headlines.stream().filter(h -> h.text().contains(path.label())).count() <= 1,
          path.label() + ":" + headlines);
    }
  }

  @Test
  void candidatePositionDoesNotPretendEqualQualityAlternativesAreDowngrades() throws Exception {
    var headlines = planner.plan(compareScored(scoredCase("S02")));
    assertEquals(List.of(1, 2, 3), headlines.stream()
        .map(WealthAnnualHeadlinePlanner.Headline::candidateRank).toList());
    assertEquals(List.of(1, 1, 1), headlines.stream()
        .map(WealthAnnualHeadlinePlanner.Headline::qualityRank).toList());
  }

  @Test
  void unchangedTiedCandidatesUseTheDocumentedStableOrder() throws Exception {
    var headlines = planner.plan(compareScored(scoredCase("S07")));
    assertEquals(List.of("stable_receipt_support", "stable_continuity_support", "stable_coverage_support"),
        headlines.stream().map(WealthAnnualHeadlinePlanner.Headline::themeKey).toList());
  }

  @Test
  void diversityNeverPushesAnUnchangedYearIntoALowerQualityTier() throws Exception {
    var headlines = planner.plan(compareScored(scoredCase("S07")));
    assertEquals(List.of(1, 1, 1), headlines.stream()
        .map(WealthAnnualHeadlinePlanner.Headline::qualityRank).toList());
  }

  @Test
  void sameInputIsByteForByteDeterministicAcrossRepeatedRuns() throws Exception {
    var compared = compareScored(scoredCase("S07"));
    var expected = planner.plan(compared);
    for (int i = 0; i < 100; i++) assertEquals(expected, planner.plan(compared));
  }

  @Test
  void noGroundedThemeFailsWithADomainReason() throws Exception {
    var error = assertThrows(WealthHeadlinePlanningException.class,
        () -> planner.plan(compareScored(scoredCase("S01"))));
    assertEquals(WealthHeadlinePlanningException.Reason.INSUFFICIENT_DIVERSITY, error.reason());
    assertEquals(List.of(2026, 2027, 2028), error.candidateCounts().keySet().stream().toList());
  }
}
