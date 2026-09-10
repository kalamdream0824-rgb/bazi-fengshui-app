package com.bazi.app.report.classic;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.PillarDto;
import com.bazi.app.report.ReportAnalysis;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;

final class RootedSupportManualReview {

  private static final ObjectMapper JSON = new ObjectMapper();

  private RootedSupportManualReview() {}

  static Review review(Path inputs, String caseId, String reviewedBirthPlace) throws Exception {
    JsonNode fixture = findFixture(JSON.readTree(inputs.toFile()), caseId);
    PaipanRequest standardRequest = JSON.treeToValue(fixture.path("request"), PaipanRequest.class);
    PaipanRequest trueSolarRequest = new PaipanRequest(
        standardRequest.name(),
        standardRequest.gender(),
        standardRequest.solarDateTime(),
        reviewedBirthPlace,
        true);

    BaziService service = new BaziService();
    PaipanResultDto standardChart = service.paipan(standardRequest);
    PaipanResultDto trueSolarChart = service.paipan(trueSolarRequest);
    ReportAnalysis analysis = ReportAnalysis.from(standardRequest, standardChart);

    requirePillar(standardChart, "month", "丁", "未", List.of("己", "丁", "乙"));
    requirePillar(standardChart, "day", "乙", "未", List.of("己", "丁", "乙"));
    requirePillar(standardChart, "time", "己", "卯", List.of("乙"));
    requirePillar(standardChart, "year", "壬", "申", List.of("庚", "壬", "戊"));

    return new Review(
        reviewedBirthPlace,
        trueSolarChart.trueSolar().original(),
        trueSolarChart.trueSolar().adjusted(),
        trueSolarChart.trueSolar().offsetMinutes(),
        trueSolarChart.trueSolar().eotMinutes(),
        pillars(standardChart),
        pillars(trueSolarChart),
        trueSolarChart.trueSolar().boundaryChanged(),
        analysis.wangShuaiLevel(),
        analysis.wangShuaiScore(),
        analysis.rootedSupportScore(),
        "偏弱",
        "有根",
        "KEEP_SERVING_BAND_USE_ROOT_AS_MODIFIER",
        List.of("时支卯为直接根", "年干壬为明透印星", "申中壬为远位藏印", "月支未与日支未均藏乙"),
        List.of("未月不直接生扶乙木", "月干丁与未中丁泄木", "月日两未及时干己使财星偏重", "申中庚对乙木形成约束"));
  }

  private static JsonNode findFixture(JsonNode source, String caseId) {
    for (JsonNode fixture : source.path("fullBirthCases")) {
      if (caseId.equals(fixture.path("id").asText())) return fixture;
    }
    throw new IllegalArgumentException("unknown review case: " + caseId);
  }

  private static void requirePillar(
      PaipanResultDto chart,
      String key,
      String gan,
      String zhi,
      List<String> hiddenStems) {
    PillarDto pillar = chart.pillars().get(key);
    if (pillar == null
        || !gan.equals(pillar.gan())
        || !zhi.equals(pillar.zhi())
        || !hiddenStems.equals(pillar.hideGan().stream().map(item -> item.gan()).toList())) {
      throw new IllegalStateException("R05 structure no longer matches reviewed " + key + " pillar");
    }
  }

  private static String pillars(PaipanResultDto chart) {
    return String.join("·", List.of("year", "month", "day", "time").stream()
        .map(key -> chart.pillars().get(key))
        .map(pillar -> pillar.gan() + pillar.zhi())
        .toList());
  }

  record Review(
      String reviewedBirthPlace,
      String originalTime,
      String adjustedTrueSolarTime,
      int longitudeOffsetMinutes,
      double equationOfTimeMinutes,
      String standardTimePillars,
      String trueSolarTimePillars,
      boolean trueSolarBoundaryChanged,
      String servingBand,
      double servingScore,
      double shadowScore,
      String manualBandDecision,
      String manualModifier,
      String recommendation,
      List<String> supportingFactors,
      List<String> limitingFactors) {}
}
