package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.bazi.app.report.wealth.WealthRemediationFixtures.changeEvidence;
import static com.bazi.app.report.wealth.WealthRemediationFixtures.scored;
import static com.bazi.app.report.wealth.WealthRemediationFixtures.scoredCase;

import com.bazi.app.report.wealth.v3.WealthNarrativeWriter;
import com.bazi.app.report.wealth.v3.WealthAnnualComparator;
import com.bazi.app.report.wealth.v3.WealthAnnualHeadlinePlanner;
import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthThesisPlanner;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class WealthThesisPlannerTest {

  @Test
  void changingTheDominantAnnualFactValueMustChangeTheVisibleThesis() throws Exception {
    var base = withDominantEvidence("偏财", EvidenceFamily.ANNUAL_TRIGGER,
        "synthetic.annual.stem.ten_god");
    var changed = withDominantEvidence("七杀", EvidenceFamily.ANNUAL_TRIGGER,
        "synthetic.annual.stem.ten_god");

    assertNotEquals(thesis(base), thesis(changed));
  }

  @Test
  void changingInputToIncomeIntoIncomeRetentionMustChangeTheVisibleThesis() throws Exception {
    assertNotEquals(
        thesis(rewriteStableEvidence("natal.combination.output_wealth",
            EvidenceFamily.NATAL_COMBINATION, "投入转成收入")),
        thesis(rewriteStableEvidence("natal.combination.wealth_capacity",
            EvidenceFamily.NATAL_COMBINATION, "进账与承受力")));
  }

  @Test
  void changingSupportLimitRatioMustChangeTheVisibleThesis() throws Exception {
    var supportive = WealthRemediationFixtures.withPathWeights(scoredCase("S02"),
        WealthPath.STABLE_INCOME, 8, 0);
    var mixed = WealthRemediationFixtures.withPathWeights(scoredCase("S02"),
        WealthPath.STABLE_INCOME, 8, 4);

    assertNotEquals(thesis(supportive), thesis(mixed));
  }

  @Test
  void changingThePrimaryEvidenceFamilyMustChangeTheVisibleThesis() throws Exception {
    assertNotEquals(
        thesis(rewriteStableEvidence("annual.stem.ten_god",
            EvidenceFamily.ANNUAL_TRIGGER, "偏财")),
        thesis(rewriteStableEvidence("dayun.stem.ten_god",
            EvidenceFamily.DAYUN_CONTEXT, "偏财")));
  }

  @Test
  void addingSharedMoneyAsThePrimaryMeaningMustChangeTheVisibleThesis() throws Exception {
    assertNotEquals(
        thesis(rewriteStableEvidence("natal.combination.output_wealth",
            EvidenceFamily.NATAL_COMBINATION, "投入转成收入")),
        thesis(rewriteStableEvidence("natal.combination.peer_wealth",
            EvidenceFamily.NATAL_COMBINATION, "同类与财星同见")));
  }

  @Test
  void concreteThesisObjectMustComeFromTheActualPrimaryFocusWhenOneExists() throws Exception {
    for (String fixtureId : IntStream.rangeClosed(1, 12)
        .mapToObj(index -> "R%02d".formatted(index)).toList()) {
      var compared = new WealthAnnualComparator().comparePeriod(
          WealthRemediationFixtures.fullBirthAssessments(fixtureId));
      var headlines = new WealthAnnualHeadlinePlanner().plan(compared);
      var plan = new WealthThesisPlanner().plan(compared, headlines);

      for (int index = 0; index < plan.years().size(); index++) {
        var focus = plan.years().get(index);
        var assessment = compared.get(index).assessment();
        assertEquals(assessment.focus().primaryCandidates(), focus.primaryPaths());
        if (!focus.primaryPaths().isEmpty()) {
          assertTrue(focus.primaryPaths().contains(focus.path()),
              fixtureId + " " + focus.year() + " selected " + focus.path()
                  + " outside actual primary focus " + focus.primaryPaths());
        }
      }
    }
  }

  private WealthPeriodEvaluation withDominantEvidence(
      String value, EvidenceFamily family, String factKey) throws Exception {
    WealthPeriodEvaluation result = scoredCase("S02");
    for (int index = 0; index < 3; index++) {
      result = changeEvidence(result, index, WealthPath.STABLE_INCOME,
          ignored -> List.of(scored(factKey, 8, family, value)));
    }
    return result;
  }

  private String thesis(WealthPeriodEvaluation period) {
    return thesis(WealthNarrativeV3Test.assessments(period));
  }

  private List<WealthAssessment> rewriteStableEvidence(
      String factKey, EvidenceFamily family, String value) throws Exception {
    var period = WealthRemediationFixtures.withPathWeights(scoredCase("S02"),
        WealthPath.STABLE_INCOME, 8, 0);
    return WealthNarrativeV3Test.assessments(period).stream()
        .map(assessment -> rewriteStableEvidence(assessment, factKey, family, value))
        .toList();
  }

  private WealthAssessment rewriteStableEvidence(
      WealthAssessment assessment, String factKey, EvidenceFamily family, String value) {
    var decision = assessment.decisions().stream()
        .filter(item -> item.path().equals(WealthPath.STABLE_INCOME.code()))
        .findFirst().orElseThrow();
    String evidenceId = decision.supportingEvidenceIds().get(0);
    var evidence = new ArrayList<>(assessment.evidence());
    int evidenceIndex = IntStream.range(0, evidence.size())
        .filter(index -> evidence.get(index).id().equals(evidenceId)).findFirst().orElseThrow();
    var originalEvidence = evidence.get(evidenceIndex);
    String rootId = originalEvidence.rootFactIds().get(0);
    evidence.set(evidenceIndex, new WealthAssessment.Evidence(
        originalEvidence.id(), originalEvidence.path(), "metamorphic." + factKey, factKey,
        family, originalEvidence.rootFactIds(), originalEvidence.weight()));
    var facts = assessment.facts().stream().map(fact -> fact.id().equals(rootId)
        ? new WealthAssessment.Fact(fact.id(), switch (family) {
          case ANNUAL_TRIGGER -> "annual";
          case DAYUN_CONTEXT -> "dayun";
          case NATAL_COMBINATION, NATAL_STRUCTURE -> "natal";
        }, factKey, value)
        : fact).toList();
    return new WealthAssessment(assessment.year(), assessment.ganZhi(), facts, evidence,
        assessment.decisions(), assessment.focus(), assessment.risk());
  }

  private String thesis(List<WealthAssessment> assessments) {
    return new WealthNarrativeWriter()
        .plan(assessments, LocalDate.of(assessments.get(0).year(), 8, 29))
        .thesis().text();
  }
}
