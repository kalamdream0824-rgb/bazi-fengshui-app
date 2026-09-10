package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthAnnualComparator;
import com.bazi.app.report.wealth.v3.WealthAnnualHeadlinePlanner;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3;
import com.bazi.app.report.wealth.v3.WealthNarrativeWriter;
import com.bazi.app.report.wealth.v3.WealthSemanticClaimWriter;
import com.bazi.app.report.wealth.v3.WealthThesisPlanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-chart acceptance gate for the user-visible wealth thesis. */
class WealthThesisCollisionTest {
  private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
  private static final Path BASELINE = Path.of("target/wealth-thesis-collision-baseline.json");
  private static final String REPORTED_TEMPLATE = "到账节奏在这份分析里更值得关注";
  private static List<ThesisCase> cases;

  @BeforeAll
  static void captureBaselineBeforeAcceptanceAssertions() throws Exception {
    List<ThesisCase> generated = new ArrayList<>();
    for (int index = 1; index <= 12; index++) {
      String fixtureId = "R%02d".formatted(index);
      List<WealthAssessment> assessments = WealthRemediationFixtures.fullBirthAssessments(fixtureId);
      LocalDate asOf = LocalDate.of(assessments.get(0).year(), 8, 29);
      WealthNarrativeV3 report = new WealthNarrativeWriter().plan(assessments, asOf);
      var compared = new WealthAnnualComparator().comparePeriod(assessments);
      var headlines = new WealthAnnualHeadlinePlanner().plan(compared);
      var plan = new WealthThesisPlanner().plan(compared, headlines);
      var claimWriter = new WealthSemanticClaimWriter();
      List<ClaimLine> claimLines = plan.years().stream().map(focus -> {
        String conclusion = claimWriter.conclusion(focus.claim(), plan.evidenceSignature());
        String check = claimWriter.check(focus.claim(), plan.evidenceSignature());
        return new ClaimLine(focus.year(), focus.claim().semanticKey(), conclusion, check,
            conclusion + "；" + check + "。");
      }).toList();
      generated.add(new ThesisCase(fixtureId, evidenceSignature(report), plan.evidenceSignature(),
          report.thesis().text(),
          report.thesis().decisionIds(), claimLines));
    }
    cases = List.copyOf(generated);

    Map<String, Object> output = new LinkedHashMap<>();
    output.put("notice", "Fixed development fixtures only; not real-world accuracy evidence");
    output.put("sampleCount", cases.size());
    output.put("uniqueEvidenceSignatureCount", cases.stream()
        .map(ThesisCase::evidenceSignature).distinct().count());
    output.put("uniqueVisibleThesisCount", cases.stream()
        .map(ThesisCase::visibleThesis).distinct().count());
    output.put("differentSignatureSameThesisCount", differentSignatureSameThesisCount(cases));
    output.put("reportedTemplateOccurrenceCount", cases.stream()
        .filter(sample -> sample.visibleThesis().contains(REPORTED_TEMPLATE)).count());
    output.put("dominantCriticalClaimLines", dominantCriticalClaimLines(cases));
    output.put("collisionGroups", collisionGroups(cases));
    output.put("cases", cases);
    Files.createDirectories(BASELINE.getParent());
    Files.writeString(BASELINE, JSON.writerWithDefaultPrettyPrinter()
        .writeValueAsString(output) + "\n");
  }

  @Test
  void baselineContainsExactlyTheTwelveFullBirthFixtures() {
    assertEquals(IntStream.rangeClosed(1, 12).mapToObj(i -> "R%02d".formatted(i)).toList(),
        cases.stream().map(ThesisCase::fixtureId).toList());
  }

  @Test
  void differentEvidenceSignaturesMustNotProduceIdenticalVisibleTheses() {
    assertEquals(0, differentSignatureSameThesisCount(cases),
        () -> "Different thesis evidence was collapsed into identical copy: "
            + collisionGroups(cases) + "; inspect " + BASELINE);
  }

  @Test
  void reportedGenericConclusionMustNotRemainInNewTheses() {
    assertEquals(0, cases.stream()
        .filter(sample -> sample.visibleThesis().contains(REPORTED_TEMPLATE)).count(),
        () -> "The reported generic conclusion still dominates fixed fixtures; inspect " + BASELINE);
  }

  @Test
  void oneGenericDirectionSkeletonMustNotBeReusedAcrossDifferentMoneyObjects() {
    for (String generic : List.of(
        "这方面出现的积极信号较多",
        "这方面有一定积极信号",
        "这方面既有积极信号，也有需要留意的牵制",
        "这方面受到的牵制较多")) {
      assertEquals(0, cases.stream().filter(sample -> sample.visibleThesis().contains(generic)).count(),
          () -> "Generic direction copy still hides the actual money object: " + generic);
    }
  }

  @Test
  void oneCriticalClaimLineMustNotDominateMoreThanTwoFixedReports() {
    assertEquals(Map.of(), dominantCriticalClaimLines(cases),
        () -> "One critical wealth claim line still dominates the fixed corpus: "
            + dominantCriticalClaimLines(cases) + "; inspect " + BASELINE);
  }

  @Test
  void identicalAnnualThesisJudgmentsAreGroupedUnderTheirActualYears() throws Exception {
    var assessments = WealthRemediationFixtures.fullBirthAssessments("R09");
    String thesis = new WealthNarrativeWriter()
        .plan(assessments, LocalDate.of(assessments.get(0).year(), 8, 29)).thesis().text();

    assertEquals(1, occurrences(thesis, "2026、2027、2028年，"), thesis);
    assertEquals(true, thesis.startsWith("2026、2027、2028年，"), thesis);
  }

  private static int occurrences(String text, String phrase) {
    return (text.length() - text.replace(phrase, "").length()) / phrase.length();
  }

  private static String evidenceSignature(WealthNarrativeV3 report) throws Exception {
    Set<String> selectedDecisionIds = Set.copyOf(report.thesis().decisionIds());
    List<Object> semanticEvidence = new ArrayList<>();
    for (var year : report.years()) {
      Map<String, WealthAssessment.Evidence> evidenceById = new LinkedHashMap<>();
      year.evidence().forEach(evidence -> evidenceById.put(evidence.id(), evidence));
      Map<String, WealthAssessment.Fact> factsById = new LinkedHashMap<>();
      year.facts().forEach(fact -> factsById.put(fact.id(), fact));
      year.decisions().stream().filter(decision -> selectedDecisionIds.contains(decision.id()))
          .sorted(Comparator.comparing(WealthAssessment.Decision::id)).forEach(decision -> {
            List<String> evidenceIds = new ArrayList<>();
            evidenceIds.addAll(decision.supportingEvidenceIds());
            evidenceIds.addAll(decision.limitingEvidenceIds());
            var citations = evidenceIds.stream().distinct().sorted().map(evidenceById::get)
                .map(evidence -> List.of(evidence.path(), evidence.ruleKey(), evidence.factKey(),
                    evidence.family().name(), evidence.weight(), evidence.rootFactIds().stream()
                        .map(factsById::get)
                        .map(fact -> List.of(fact.kind(), fact.code(), fact.value()))
                        .sorted(Comparator.comparing(Object::toString)).toList()))
                .toList();
            semanticEvidence.add(List.of(year.year(), decision.path(), decision.stance(),
                decision.strength(), citations));
          });
    }
    byte[] bytes = JSON.writeValueAsBytes(semanticEvidence);
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  private static int differentSignatureSameThesisCount(List<ThesisCase> samples) {
    int count = 0;
    for (int left = 0; left < samples.size(); left++) {
      for (int right = left + 1; right < samples.size(); right++) {
        ThesisCase a = samples.get(left);
        ThesisCase b = samples.get(right);
        if (a.visibleThesis().equals(b.visibleThesis())
            && !a.evidenceSignature().equals(b.evidenceSignature())) count++;
      }
    }
    return count;
  }

  private static List<List<String>> collisionGroups(List<ThesisCase> samples) {
    Map<String, List<ThesisCase>> groups = new LinkedHashMap<>();
    samples.forEach(sample -> groups.computeIfAbsent(sample.visibleThesis(), ignored -> new ArrayList<>())
        .add(sample));
    return groups.values().stream()
        .filter(group -> group.stream().map(ThesisCase::evidenceSignature).distinct().count() > 1)
        .map(group -> group.stream().map(ThesisCase::fixtureId).toList()).toList();
  }

  private static Map<String, List<String>> dominantCriticalClaimLines(List<ThesisCase> samples) {
    Map<String, List<String>> fixturesByLine = new LinkedHashMap<>();
    samples.forEach(sample -> sample.claimLines().forEach(line ->
        List.of(line.conclusion(), line.check(), line.visibleText()).forEach(fragment -> fixturesByLine
            .computeIfAbsent(fragment, ignored -> new ArrayList<>()).add(sample.fixtureId()))));
    Map<String, List<String>> result = new LinkedHashMap<>();
    fixturesByLine.forEach((line, fixtureIds) -> {
      List<String> distinct = fixtureIds.stream().distinct().toList();
      if (distinct.size() > 2) result.put(line, distinct);
    });
    return result;
  }

  private record ThesisCase(String fixtureId, String evidenceSignature, String planEvidenceSignature,
      String visibleThesis, List<String> decisionIds, List<ClaimLine> claimLines) {}

  private record ClaimLine(int year, String semanticKey, String conclusion, String check,
      String visibleText) {}
}
