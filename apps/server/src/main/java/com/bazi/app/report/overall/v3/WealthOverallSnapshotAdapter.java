package com.bazi.app.report.overall.v3;

import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.ConfidenceLevel;
import com.bazi.app.report.wealth.EvidenceFamily;
import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthV3Analyzer;
import java.util.Comparator;
import java.util.List;

final class WealthOverallSnapshotAdapter {

  private final WealthV3Analyzer analyzer = new WealthV3Analyzer();

  List<OverallTopicSnapshot> adapt(PaipanResultDto chart, List<AnnualContext> contexts) {
    return analyzer.analyze(chart, contexts).stream().map(this::adapt).toList();
  }

  private OverallTopicSnapshot adapt(WealthAssessment assessment) {
    String path = selectedPath(assessment);
    WealthAssessment.Decision decision = assessment.decisions().stream()
        .filter(item -> item.path().equals(path))
        .findFirst()
        .orElseThrow();
    List<String> evidenceKeys = assessment.evidence().stream()
        .map(WealthAssessment.Evidence::ruleKey)
        .distinct()
        .toList();
    String focusKey = "wealth." + path + "." + decision.stance();
    return new OverallTopicSnapshot(
        assessment.year(),
        OverallTopicSnapshot.Topic.WEALTH,
        focusKey,
        stance(decision.stance()),
        urgency(assessment, path, decision),
        confidence(decision.strength()),
        (int) assessment.evidence().stream()
            .filter(evidence -> evidence.family() == EvidenceFamily.ANNUAL_TRIGGER)
            .map(WealthAssessment.Evidence::ruleKey)
            .distinct()
            .count(),
        decision.supportWeight() > 0 ? "wealth." + path + ".opportunity" : null,
        assessment.risk() == null ? null : "wealth." + assessment.risk().path() + ".risk",
        List.of(focusKey + ".act", focusKey + ".fallback"),
        evidenceKeys);
  }

  private String selectedPath(WealthAssessment assessment) {
    if (!assessment.focus().primaryCandidates().isEmpty()) {
      return assessment.focus().primaryCandidates().get(0);
    }
    if (assessment.risk() != null) return assessment.risk().path();
    return assessment.decisions().stream()
        .sorted(Comparator
            .comparingInt((WealthAssessment.Decision item) ->
                item.supportWeight() + item.limitationWeight())
            .reversed()
            .thenComparing(WealthAssessment.Decision::path))
        .findFirst()
        .orElseThrow()
        .path();
  }

  private OverallTopicSnapshot.Stance stance(String stance) {
    return switch (stance) {
      case "supportive" -> OverallTopicSnapshot.Stance.SUPPORTIVE;
      case "mixed" -> OverallTopicSnapshot.Stance.MIXED;
      case "restricted" -> OverallTopicSnapshot.Stance.PRESSURED;
      default -> OverallTopicSnapshot.Stance.BALANCED;
    };
  }

  private OverallTopicSnapshot.Urgency urgency(
      WealthAssessment assessment, String path, WealthAssessment.Decision decision) {
    if (assessment.risk() != null && assessment.risk().path().equals(path)) {
      return OverallTopicSnapshot.Urgency.HIGH;
    }
    if (assessment.risk() != null || decision.stance().equals("mixed")) {
      return OverallTopicSnapshot.Urgency.MEDIUM;
    }
    return OverallTopicSnapshot.Urgency.LOW;
  }

  private ConfidenceLevel confidence(String strength) {
    return switch (strength) {
      case "pronounced" -> ConfidenceLevel.HIGH;
      case "supported" -> ConfidenceLevel.MEDIUM;
      default -> ConfidenceLevel.LOW;
    };
  }
}
