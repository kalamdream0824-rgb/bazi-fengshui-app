package com.bazi.app.report.relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record RelationshipDimensionEvaluation(
    RelationshipDimension dimension,
    int supportWeight,
    int limitationWeight,
    int prominenceWeight,
    int netWeight,
    RelationshipTone tone,
    List<RelationshipEvidence> evidence) {

  public RelationshipDimensionEvaluation {
    Objects.requireNonNull(dimension, "dimension");
    Objects.requireNonNull(tone, "tone");
    evidence = evidence == null ? List.of() : List.copyOf(evidence);
    if (evidence.stream().anyMatch(item -> item.dimension() != dimension)) {
      throw new IllegalArgumentException("relationship evidence belongs to another dimension");
    }
    Set<String> keys = new HashSet<>();
    if (evidence.stream().map(RelationshipEvidence::key).anyMatch(key -> !keys.add(key))) {
      throw new IllegalArgumentException("duplicate relationship evidence key in dimension");
    }
    int expectedSupport = evidence.stream()
        .filter(item -> item.weight() > 0)
        .mapToInt(RelationshipEvidence::weight)
        .sum();
    int expectedLimitation = evidence.stream()
        .filter(item -> item.weight() < 0)
        .mapToInt(item -> Math.abs(item.weight()))
        .sum();
    if (supportWeight != expectedSupport
        || limitationWeight != expectedLimitation
        || prominenceWeight != expectedSupport + expectedLimitation
        || netWeight != expectedSupport - expectedLimitation
        || tone != toneFor(expectedSupport, expectedLimitation)) {
      throw new IllegalArgumentException("inconsistent relationship dimension evaluation");
    }
  }

  public List<RelationshipEvidence> support() {
    return evidence.stream().filter(item -> item.weight() > 0).toList();
  }

  public List<RelationshipEvidence> limitations() {
    return evidence.stream().filter(item -> item.weight() < 0).toList();
  }

  public Set<RelationshipEvidenceFamily> evidenceFamilies() {
    return evidence.stream()
        .map(RelationshipEvidence::family)
        .collect(Collectors.toUnmodifiableSet());
  }

  static RelationshipTone toneFor(int support, int limitation) {
    if (support == 0 && limitation == 0) return RelationshipTone.QUIET;
    if (limitation == 0) return RelationshipTone.SUPPORTIVE;
    if (support == 0 || limitation > support) return RelationshipTone.PRESSURED;
    return RelationshipTone.MIXED;
  }
}
