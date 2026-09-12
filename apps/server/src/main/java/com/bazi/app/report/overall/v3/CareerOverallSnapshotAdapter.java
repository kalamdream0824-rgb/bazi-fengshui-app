package com.bazi.app.report.overall.v3;

import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualPeriodAssessor;
import com.bazi.app.report.AnnualStage;
import com.bazi.app.report.ReportEvidence;
import com.bazi.app.report.ReportTopic;
import com.bazi.app.report.YearAssessment;
import java.util.LinkedHashSet;
import java.util.List;

final class CareerOverallSnapshotAdapter {

  private final AnnualPeriodAssessor assessor;

  CareerOverallSnapshotAdapter(AnnualPeriodAssessor assessor) {
    this.assessor = assessor;
  }

  List<OverallTopicSnapshot> adapt(List<AnnualContext> contexts) {
    return contexts.stream().map(context -> adapt(
        assessor.assessYear(context, ReportTopic.CAREER))).toList();
  }

  private OverallTopicSnapshot adapt(YearAssessment assessment) {
    if (assessment.ruleKeys().isEmpty()) {
      throw new IllegalArgumentException("overall career snapshot requires a reviewed rule");
    }
    String focusKey = assessment.ruleKeys().get(0);
    LinkedHashSet<String> evidence = new LinkedHashSet<>();
    assessment.evidence().stream().map(ReportEvidence::key).forEach(evidence::add);
    assessment.counterEvidence().stream().map(ReportEvidence::key).forEach(evidence::add);
    List<String> actions = assessment.ruleKeys().stream()
        .limit(2)
        .map(key -> key + ".action")
        .toList();
    boolean hasOpportunity = !assessment.opportunities().isEmpty();
    boolean hasPressure = !assessment.pressures().isEmpty();
    return new OverallTopicSnapshot(
        assessment.year(),
        OverallTopicSnapshot.Topic.CAREER,
        focusKey,
        stance(assessment.stage()),
        urgency(assessment.stage()),
        assessment.confidence(),
        (int) evidence.stream().filter(key -> key.startsWith("annual.")).count(),
        hasOpportunity ? focusKey + ".opportunity" : null,
        hasPressure ? focusKey + ".risk" : null,
        actions,
        List.copyOf(evidence));
  }

  private OverallTopicSnapshot.Stance stance(AnnualStage stage) {
    return switch (stage) {
      case ADVANCE -> OverallTopicSnapshot.Stance.SUPPORTIVE;
      case TRANSITION -> OverallTopicSnapshot.Stance.MIXED;
      case CAUTION -> OverallTopicSnapshot.Stance.PRESSURED;
      case PREPARE, STABLE -> OverallTopicSnapshot.Stance.BALANCED;
    };
  }

  private OverallTopicSnapshot.Urgency urgency(AnnualStage stage) {
    return switch (stage) {
      case CAUTION, TRANSITION -> OverallTopicSnapshot.Urgency.HIGH;
      case ADVANCE, PREPARE -> OverallTopicSnapshot.Urgency.MEDIUM;
      case STABLE -> OverallTopicSnapshot.Urgency.LOW;
    };
  }
}
