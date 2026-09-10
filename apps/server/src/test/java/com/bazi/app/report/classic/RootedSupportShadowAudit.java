package com.bazi.app.report.classic;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.ReportAnalysis;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RootedSupportShadowAudit {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final int MINIMUM_TOPIC_YEARS_PER_CHART = 10;
  private static final int MAXIMUM_TOPIC_YEARS_PER_CHART = 11;

  private RootedSupportShadowAudit() {}

  public static Summary evaluate(Path inputs) throws Exception {
    JsonNode source = JSON.readTree(inputs.toFile());
    List<CaseResult> cases = new ArrayList<>();
    Map<String, Integer> servingLevels = emptyLevelDistribution();
    Map<String, Integer> projectedLevels = emptyLevelDistribution();
    int changed = 0;
    int crossed = 0;
    double deltaTotal = 0;
    double maxDelta = 0;

    for (JsonNode fixture : source.path("fullBirthCases")) {
      String id = fixture.path("id").asText();
      PaipanRequest request = JSON.treeToValue(fixture.path("request"), PaipanRequest.class);
      ReportAnalysis analysis = ReportAnalysis.from(request, new BaziService().paipan(request));
      double base = analysis.wangShuaiScore();
      double rooted = analysis.rootedSupportScore();
      double delta = rounded(rooted - base);
      String servingLevel = analysis.wangShuaiLevel();
      String projectedLevel = levelFor(rooted);
      boolean crossBand = !servingLevel.equals(projectedLevel);

      if (delta > 0) changed++;
      if (crossBand) crossed++;
      deltaTotal += delta;
      maxDelta = Math.max(maxDelta, delta);
      servingLevels.merge(servingLevel, 1, Integer::sum);
      projectedLevels.merge(projectedLevel, 1, Integer::sum);
      cases.add(new CaseResult(
          id,
          pillars(analysis),
          base,
          rooted,
          delta,
          servingLevel,
          projectedLevel,
          crossBand,
          analysis.fact("rootedSupport").value()));
    }
    cases.sort((left, right) -> left.id().compareTo(right.id()));
    int uniqueCharts = (int) cases.stream().map(CaseResult::pillars).distinct().count();
    return new Summary(
        cases.size(),
        uniqueCharts,
        true,
        changed,
        crossed,
        crossed * MINIMUM_TOPIC_YEARS_PER_CHART,
        crossed * MAXIMUM_TOPIC_YEARS_PER_CHART,
        cases.isEmpty() ? 0 : rounded(deltaTotal / cases.size()),
        rounded(maxDelta),
        Collections.unmodifiableMap(new LinkedHashMap<>(servingLevels)),
        Collections.unmodifiableMap(new LinkedHashMap<>(projectedLevels)),
        List.copyOf(cases));
  }

  private static Map<String, Integer> emptyLevelDistribution() {
    Map<String, Integer> levels = new LinkedHashMap<>();
    levels.put("偏弱", 0);
    levels.put("中和", 0);
    levels.put("偏强", 0);
    return levels;
  }

  private static String pillars(ReportAnalysis analysis) {
    return String.join("·", List.of("year", "month", "day", "time").stream()
        .map(key -> {
          var pillar = analysis.result().pillars().get(key);
          return pillar.gan() + pillar.zhi();
        })
        .toList());
  }

  private static String levelFor(double score) {
    return score >= 4 ? "偏强" : score <= 1.5 ? "偏弱" : "中和";
  }

  private static double rounded(double value) {
    return Math.round(value * 1000.0) / 1000.0;
  }

  public record Summary(
      int sampleCount,
      int uniqueChartCount,
      boolean projectionOnly,
      int scoreChangedCount,
      int projectedCrossBandCount,
      int minimumPotentiallyAffectedTopicYears,
      int maximumPotentiallyAffectedTopicYears,
      double averageDelta,
      double maxDelta,
      Map<String, Integer> servingLevelDistribution,
      Map<String, Integer> projectedLevelDistribution,
      List<CaseResult> cases) {}

  public record CaseResult(
      String id,
      String pillars,
      double servingScore,
      double rootedSupportScore,
      double delta,
      String servingLevel,
      String projectedLevel,
      boolean projectedCrossBand,
      String evidence) {}
}
