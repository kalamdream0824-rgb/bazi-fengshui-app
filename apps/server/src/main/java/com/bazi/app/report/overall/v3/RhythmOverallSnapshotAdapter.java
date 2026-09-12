package com.bazi.app.report.overall.v3;

import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.ConfidenceLevel;
import com.bazi.app.report.overall.OverallDimension;
import com.bazi.app.report.overall.OverallDimensionEvaluation;
import com.bazi.app.report.overall.OverallDimensionEvaluator;
import com.bazi.app.report.overall.OverallStance;
import java.util.List;
import java.util.Locale;

final class RhythmOverallSnapshotAdapter {

  List<OverallTopicSnapshot> adapt(List<AnnualContext> contexts) {
    OverallDimensionEvaluator evaluator = new OverallDimensionEvaluator();
    return contexts.stream().map(context -> adapt(
        context.year(),
        evaluator.evaluate(context).stream()
            .filter(item -> item.dimension() == OverallDimension.RHYTHM)
            .findFirst()
            .orElseThrow())).toList();
  }

  private OverallTopicSnapshot adapt(int year, OverallDimensionEvaluation evaluation) {
    String stance = evaluation.stance().name().toLowerCase(Locale.ROOT);
    String focusKey = "rhythm." + stance;
    return new OverallTopicSnapshot(
        year,
        OverallTopicSnapshot.Topic.RHYTHM,
        focusKey,
        stance(evaluation.stance()),
        evaluation.stance() == OverallStance.PRESSURED
            ? OverallTopicSnapshot.Urgency.HIGH
            : evaluation.stance() == OverallStance.MIXED
                ? OverallTopicSnapshot.Urgency.MEDIUM
                : OverallTopicSnapshot.Urgency.LOW,
        evaluation.directAnnualEvidenceKeys().size() >= 2
            ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM,
        evaluation.directAnnualEvidenceKeys().size(),
        evaluation.supportWeight() > evaluation.limitationWeight()
            ? focusKey + ".opportunity" : null,
        evaluation.limitationWeight() > 0 ? focusKey + ".risk" : null,
        List.of(focusKey + ".reduce", focusKey + ".protect"),
        evaluation.allEvidenceKeys());
  }

  private OverallTopicSnapshot.Stance stance(OverallStance stance) {
    return switch (stance) {
      case SUPPORTIVE -> OverallTopicSnapshot.Stance.SUPPORTIVE;
      case BALANCED -> OverallTopicSnapshot.Stance.BALANCED;
      case MIXED -> OverallTopicSnapshot.Stance.MIXED;
      case PRESSURED -> OverallTopicSnapshot.Stance.PRESSURED;
    };
  }
}
