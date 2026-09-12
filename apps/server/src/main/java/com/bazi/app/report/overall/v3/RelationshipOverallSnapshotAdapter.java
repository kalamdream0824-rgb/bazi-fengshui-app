package com.bazi.app.report.overall.v3;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.ConfidenceLevel;
import com.bazi.app.report.relationship.RelationshipDimensionEvaluation;
import com.bazi.app.report.relationship.RelationshipDimensionEvaluator;
import com.bazi.app.report.relationship.RelationshipFactExtractor;
import com.bazi.app.report.relationship.RelationshipPeriodArbitrator;
import com.bazi.app.report.relationship.RelationshipPeriodEvaluation;
import com.bazi.app.report.relationship.RelationshipTone;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class RelationshipOverallSnapshotAdapter {

  List<OverallTopicSnapshot> adapt(
      PaipanRequest request,
      PaipanResultDto chart,
      List<AnnualContext> contexts) {
    var evaluations = new RelationshipDimensionEvaluator().evaluate(
        new RelationshipFactExtractor().extract(request, chart, contexts));
    var period = new RelationshipPeriodArbitrator().arbitrate(evaluations);
    return period.years().stream().map(this::adapt).toList();
  }

  private OverallTopicSnapshot adapt(RelationshipPeriodEvaluation.Year year) {
    var dimension = year.focus().primaryDimension();
    RelationshipDimensionEvaluation evaluation = year.dimensions().get(dimension);
    String tone = evaluation.tone().name().toLowerCase(Locale.ROOT);
    String focusKey = "relationship." + dimension.code() + "." + tone;
    LinkedHashSet<String> evidence = new LinkedHashSet<>(year.focus().primaryEvidenceKeys());
    evidence.addAll(year.focus().secondaryEvidenceKeys());
    if (year.mainRisk() != null) evidence.addAll(year.mainRisk().evidenceKeys());
    return new OverallTopicSnapshot(
        year.year(),
        OverallTopicSnapshot.Topic.RELATIONSHIP,
        focusKey,
        stance(evaluation.tone()),
        urgency(year),
        confidence(evaluation),
        (int) evidence.stream().filter(key -> key.startsWith("annual.")).count(),
        evaluation.supportWeight() > 0 ? focusKey + ".opportunity" : null,
        year.mainRisk() == null ? null
            : "relationship." + year.mainRisk().dimension().code() + ".risk",
        List.of(focusKey + ".clarify", focusKey + ".verify"),
        List.copyOf(evidence));
  }

  private OverallTopicSnapshot.Stance stance(RelationshipTone tone) {
    return switch (tone) {
      case SUPPORTIVE -> OverallTopicSnapshot.Stance.SUPPORTIVE;
      case MIXED -> OverallTopicSnapshot.Stance.MIXED;
      case PRESSURED -> OverallTopicSnapshot.Stance.PRESSURED;
      case QUIET -> OverallTopicSnapshot.Stance.BALANCED;
    };
  }

  private OverallTopicSnapshot.Urgency urgency(RelationshipPeriodEvaluation.Year year) {
    if (year.mainRisk() == null) return OverallTopicSnapshot.Urgency.LOW;
    return year.mainRisk().dimension() == year.focus().primaryDimension()
        ? OverallTopicSnapshot.Urgency.HIGH
        : OverallTopicSnapshot.Urgency.MEDIUM;
  }

  private ConfidenceLevel confidence(RelationshipDimensionEvaluation evaluation) {
    long families = evaluation.evidenceFamilies().size();
    return families >= 2 ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM;
  }
}
