package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.fullBirthAssessments;
import static com.bazi.app.report.wealth.WealthRemediationFixtures.read;
import static com.bazi.app.report.wealth.WealthRemediationFixtures.scoredCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.report.wealth.v3.WealthAnnualComparator;
import com.bazi.app.report.wealth.v3.WealthAnnualHeadlinePlanner;
import com.bazi.app.report.wealth.v3.WealthAnnualHeadlinePlanner.Headline;
import com.bazi.app.report.wealth.v3.WealthHeadlinePlanningException;
import com.bazi.app.report.wealth.v3.WealthHeadlineVocabulary;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class WealthHeadlinePlannerPreflightTest {
  private static final Set<String> ALLOWED_TEXT_FRAGMENTS = Set.of(
      "有利条件", "需要留意", "可以作为", "同时出现");
  private final WealthAnnualHeadlinePlanner planner = new WealthAnnualHeadlinePlanner();
  private final WealthAnnualComparator comparator = new WealthAnnualComparator();

  @Test
  void measureTheCompleteFixedCorpusBeforeProductionWiring() throws Exception {
    List<Result> results = new ArrayList<>();
    for (var source : read("wealth-v2-baseline-20260829.json").get("cases")) {
      if (!source.get("kind").asText().equals("full_birth_input")) continue;
      String id = source.get("id").asText();
      results.add(run(id, "full_birth_input", fullBirthAssessments(id)));
    }
    for (String id : List.of("S01", "S02", "S03", "S04", "S05", "S06", "S07", "S08")) {
      results.add(run(id, "synthetic_boundary",
          scoredCase(id).years().stream().map(WealthRemediationFixtures::assessScored).toList()));
    }

    List<Result> full = results.stream().filter(result -> result.kind().equals("full_birth_input")).toList();
    List<Result> synthetic = results.stream().filter(result -> result.kind().equals("synthetic_boundary")).toList();
    assertEquals(15, full.size());
    assertTrue(full.stream().allMatch(Result::success), failureSummary(full));
    assertTrue(full.stream().flatMap(result -> result.headlines().stream())
        .allMatch(headline -> headline.qualityRank() == 1), qualityDowngradeSummary(full));
    assertEquals(List.of("S01"), synthetic.stream().filter(result -> !result.success()).map(Result::id).toList());
    assertEquals(23, results.size());
    assertTrue(results.stream().flatMap(result -> result.headlines().stream())
        .allMatch(headline -> headline.objectKey() != null && !headline.objectKey().isBlank()));
    assertTrue(results.stream().filter(Result::success).allMatch(result ->
        result.headlines().stream().map(headline -> WealthHeadlineVocabulary
            .entry(headline.themeKey()).sentenceSpec().verification()).distinct().count()
            == result.headlines().size()),
        () -> "Selected years collapse different themes into the same visible check object");
    assertTrue(results.stream().filter(Result::success)
        .allMatch(result -> result.textWarnings().isEmpty()),
        () -> "Four-character headline fragments still repeat across years: "
            + results.stream().filter(result -> !result.textWarnings().isEmpty())
                .map(result -> result.id() + "=" + result.textWarnings()).toList());

    results.forEach(this::printCase);
    ObjectNode summary = summary(results);
    System.out.println("PREFLIGHT_SUMMARY " + summary);
    Files.writeString(Path.of("target/wealth-headline-preflight.json"),
        WealthRemediationFixtures.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(summary) + "\n");
  }

  private Result run(String id, String kind, List<com.bazi.app.report.wealth.v3.WealthAssessment> assessments) {
    try {
      List<Headline> headlines = planner.plan(comparator.comparePeriod(assessments));
      int belowYearMax = 0;
      int totalSalienceGap = 0;
      int limitedStrength = 0;
      for (int index = 0; index < headlines.size(); index++) {
        var assessment = assessments.get(index);
        var headline = headlines.get(index);
        var selected = assessment.decisions().stream()
            .filter(decision -> headline.decisionIds().contains(decision.id())).findFirst().orElseThrow();
        int selectedSalience = Math.addExact(selected.supportWeight(), selected.limitationWeight());
        int maximumSalience = assessment.decisions().stream().filter(decision -> !decision.stance().equals("quiet"))
            .mapToInt(decision -> Math.addExact(decision.supportWeight(), decision.limitationWeight())).max().orElse(0);
        if (selectedSalience < maximumSalience) belowYearMax++;
        totalSalienceGap = Math.addExact(totalSalienceGap, maximumSalience - selectedSalience);
        if (selected.strength().equals("limited")) limitedStrength++;
      }
      return new Result(id, kind, true, headlines, null, Map.of(), repeatedFragments(headlines),
          belowYearMax, totalSalienceGap, limitedStrength);
    } catch (WealthHeadlinePlanningException error) {
      return new Result(id, kind, false, List.of(), error.reason().name(), error.candidateCounts(), Set.of(),
          0, 0, 0);
    }
  }

  private Set<String> repeatedFragments(List<Headline> headlines) {
    Map<String, Set<Integer>> yearsByFragment = new TreeMap<>();
    for (Headline headline : headlines) {
      String han = headline.text().replaceAll("[^\\p{IsHan}]", "");
      for (int size = 4; size <= Math.min(8, han.length()); size++) {
        for (int start = 0; start + size <= han.length(); start++) {
          String fragment = han.substring(start, start + size);
          if (ALLOWED_TEXT_FRAGMENTS.contains(fragment)) continue;
          yearsByFragment.computeIfAbsent(fragment, ignored -> new LinkedHashSet<>()).add(headline.year());
        }
      }
    }
    return yearsByFragment.entrySet().stream().filter(entry -> entry.getValue().size() > 1)
        .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
  }

  private ObjectNode summary(List<Result> results) {
    ObjectNode output = WealthRemediationFixtures.JSON.createObjectNode();
    output.put("sampleCount", results.size());
    output.put("successCount", results.stream().filter(Result::success).count());
    output.put("failureCount", results.stream().filter(result -> !result.success()).count());
    var full = results.stream().filter(result -> result.kind().equals("full_birth_input")).toList();
    output.put("fullBirthCount", full.size());
    output.put("fullBirthSuccessCount", full.stream().filter(Result::success).count());
    output.put("fullBirthSelectedHeadlineCount", full.stream().mapToLong(result -> result.headlines().size()).sum());
    output.put("fullBirthSelectedSecondOrLater", full.stream().flatMap(result -> result.headlines().stream())
        .filter(headline -> headline.candidateRank() >= 2).count());
    output.put("fullBirthSelectedThirdOrLater", full.stream().flatMap(result -> result.headlines().stream())
        .filter(headline -> headline.candidateRank() >= 3).count());
    output.put("fullBirthSelectedBelowYearMaxCount", full.stream().mapToInt(Result::selectedBelowYearMaxCount).sum());
    output.put("fullBirthTotalSalienceGap", full.stream().mapToInt(Result::totalSalienceGap).sum());
    output.put("fullBirthSelectedLimitedStrengthCount", full.stream().mapToInt(Result::selectedLimitedStrengthCount).sum());
    output.put("fullBirthDiversityDowngradeCount", full.stream().flatMap(result -> result.headlines().stream())
        .filter(headline -> headline.qualityRank() > 1).count());
    output.put("syntheticCount", results.stream().filter(result -> result.kind().equals("synthetic_boundary")).count());
    output.put("selectedSecondOrLater", results.stream().flatMap(result -> result.headlines().stream())
        .filter(headline -> headline.candidateRank() >= 2).count());
    output.put("selectedThirdOrLater", results.stream().flatMap(result -> result.headlines().stream())
        .filter(headline -> headline.candidateRank() >= 3).count());
    output.put("missingObjectCount", results.stream().flatMap(result -> result.headlines().stream())
        .filter(headline -> headline.objectKey().isBlank()).count());
    output.put("textWarningCaseCount", results.stream().filter(result -> !result.textWarnings().isEmpty()).count());
    output.put("selectedBelowYearMaxCount", results.stream().mapToInt(Result::selectedBelowYearMaxCount).sum());
    output.put("totalSalienceGap", results.stream().mapToInt(Result::totalSalienceGap).sum());
    output.put("selectedLimitedStrengthCount", results.stream().mapToInt(Result::selectedLimitedStrengthCount).sum());
    output.put("diversityDowngradeCount", results.stream().flatMap(result -> result.headlines().stream())
        .filter(headline -> headline.qualityRank() > 1).count());
    output.set("themeDistribution", distribution(results, Headline::themeKey));
    output.set("pathDistribution", distribution(results, Headline::pathKey));
    output.set("subjectDistribution", distribution(results, Headline::subjectKey));
    output.set("objectDistribution", distribution(results, Headline::objectKey));
    ArrayNode failures = output.putArray("failures");
    results.stream().filter(result -> !result.success()).forEach(result -> {
      ObjectNode node = failures.addObject();
      node.put("caseId", result.id());
      node.put("reason", result.failureReason());
      node.set("candidateCounts", WealthRemediationFixtures.JSON.valueToTree(result.failureCandidateCounts()));
    });
    ArrayNode warnings = output.putArray("textWarnings");
    results.stream().filter(result -> !result.textWarnings().isEmpty()).forEach(result -> {
      ObjectNode node = warnings.addObject();
      node.put("caseId", result.id());
      node.set("fragments", WealthRemediationFixtures.JSON.valueToTree(result.textWarnings()));
    });
    return output;
  }

  private ObjectNode distribution(List<Result> results, java.util.function.Function<Headline, String> key) {
    Map<String, Integer> counts = new TreeMap<>();
    results.stream().flatMap(result -> result.headlines().stream())
        .forEach(headline -> counts.merge(key.apply(headline), 1, Integer::sum));
    ObjectNode output = WealthRemediationFixtures.JSON.createObjectNode();
    counts.forEach(output::put);
    return output;
  }

  private void printCase(Result result) {
    if (!result.success()) {
      System.out.printf("PREFLIGHT case=%s kind=%s outcome=failure reason=%s candidateCounts=%s%n",
          result.id(), result.kind(), result.failureReason(), result.failureCandidateCounts());
      return;
    }
    System.out.printf("PREFLIGHT case=%s kind=%s outcome=success themes=%s paths=%s ranks=%s candidateCounts=%s warnings=%d%n",
        result.id(), result.kind(), result.headlines().stream().map(Headline::themeKey).toList(),
        result.headlines().stream().map(Headline::pathKey).toList(),
        result.headlines().stream().map(Headline::candidateRank).toList(),
        result.headlines().stream().map(Headline::candidateCount).toList(), result.textWarnings().size());
  }

  private String failureSummary(List<Result> results) {
    return results.stream().filter(result -> !result.success())
        .map(result -> result.id() + ":" + result.failureReason() + ":" + result.failureCandidateCounts())
        .collect(java.util.stream.Collectors.joining(","));
  }

  private String qualityDowngradeSummary(List<Result> results) {
    return results.stream().filter(Result::success)
        .filter(result -> result.headlines().stream().anyMatch(headline -> headline.qualityRank() > 1))
        .map(result -> result.id() + ":" + result.headlines().stream().map(Headline::qualityRank).toList())
        .collect(java.util.stream.Collectors.joining(","));
  }

  private record Result(String id, String kind, boolean success, List<Headline> headlines,
      String failureReason, Map<Integer, Integer> failureCandidateCounts, Set<String> textWarnings,
      int selectedBelowYearMaxCount, int totalSalienceGap, int selectedLimitedStrengthCount) {
    private Result {
      headlines = List.copyOf(headlines);
      failureCandidateCounts = Collections.unmodifiableMap(new LinkedHashMap<>(failureCandidateCounts));
      textWarnings = Set.copyOf(textWarnings);
    }
  }
}
