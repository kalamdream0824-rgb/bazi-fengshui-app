package com.bazi.app.report.relationship;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Compares evidence without changing calculation weights, focus or risk. */
record RelationshipYearComparison(boolean focusChanged, List<Change> changes) {

  static RelationshipYearComparison between(
      RelationshipPeriodEvaluation.Year previous, RelationshipPeriodEvaluation.Year current) {
    if (current.year() != previous.year() + 1) {
      throw new IllegalArgumentException("relationship comparison requires adjacent years");
    }
    List<Change> changes = java.util.Arrays.stream(RelationshipDimension.values())
        .map(dimension -> new Change(dimension,
            previous.dimensions().get(dimension), current.dimensions().get(dimension)))
        .filter(Change::changed)
        .sorted(Comparator.comparingInt((Change change) -> priority(change, previous, current))
            .thenComparing(Comparator.comparingInt(Change::magnitude).reversed())
            .thenComparingInt(change -> change.dimension().ordinal()))
        .toList();
    return new RelationshipYearComparison(
        previous.focus().primaryDimension() != current.focus().primaryDimension(), changes);
  }

  private static int priority(Change change, RelationshipPeriodEvaluation.Year previous,
      RelationshipPeriodEvaluation.Year current) {
    if (change.dimension() == current.focus().primaryDimension()) return 0;
    if (change.dimension() == previous.focus().primaryDimension()) return 1;
    if (current.mainRisk() != null && change.dimension() == current.mainRisk().dimension()) return 2;
    if (change.limitDelta() != 0) return 3;
    return 4;
  }

  record Change(RelationshipDimension dimension,
                RelationshipDimensionEvaluation before, RelationshipDimensionEvaluation after) {
    int supportDelta() { return after.supportWeight() - before.supportWeight(); }
    int limitDelta() { return after.limitationWeight() - before.limitationWeight(); }
    int magnitude() { return Math.abs(supportDelta()) + Math.abs(limitDelta()); }
    boolean changed() {
      return magnitude() != 0 || !signatures(before).equals(signatures(after));
    }
    List<RelationshipEvidence> added() { return difference(after, before); }
    List<RelationshipEvidence> removed() { return difference(before, after); }
  }

  private static List<RelationshipEvidence> difference(RelationshipDimensionEvaluation from,
      RelationshipDimensionEvaluation other) {
    Map<String, Signature> existing = signatures(other);
    return from.evidence().stream()
        .filter(item -> !Objects.equals(signature(item), existing.get(item.key())))
        .sorted(Comparator.comparingInt((RelationshipEvidence item) -> Math.abs(item.weight()))
            .reversed().thenComparing(RelationshipEvidence::key))
        .toList();
  }

  private static Map<String, Signature> signatures(RelationshipDimensionEvaluation evaluation) {
    return evaluation.evidence().stream().collect(Collectors.toUnmodifiableMap(
        RelationshipEvidence::key, RelationshipYearComparison::signature));
  }

  // Labels may contain calendar text; only rule identity, origin and weight define a difference.
  private static Signature signature(RelationshipEvidence item) {
    return new Signature(item.family(), item.weight(), item.sourceFactKeys().stream().sorted().toList());
  }

  private record Signature(RelationshipEvidenceFamily family, int weight, List<String> sources) {}
}
