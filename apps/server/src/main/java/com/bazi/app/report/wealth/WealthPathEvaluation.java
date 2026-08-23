package com.bazi.app.report.wealth;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record WealthPathEvaluation(
    WealthPath path,
    int score,
    String status,
    List<ScoredEvidence> scoredEvidence) {

  public WealthPathEvaluation {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(status, "status");
    scoredEvidence = scoredEvidence == null ? List.of() : List.copyOf(scoredEvidence);
  }

  public List<ScoredEvidence> support() {
    return scoredEvidence.stream().filter(item -> item.weight() > 0).toList();
  }

  public List<ScoredEvidence> limitations() {
    return scoredEvidence.stream().filter(item -> item.weight() < 0).toList();
  }

  public Set<EvidenceFamily> supportFamilies() {
    return support().stream()
        .map(item -> item.evidence().family())
        .collect(Collectors.toUnmodifiableSet());
  }

  public int limitationWeight() {
    return limitations().stream().mapToInt(item -> Math.abs(item.weight())).sum();
  }

  public record ScoredEvidence(WealthEvidence evidence, int weight) {
    public ScoredEvidence {
      Objects.requireNonNull(evidence, "evidence");
      if (weight == 0) throw new IllegalArgumentException("scored evidence weight must not be zero");
    }
  }
}
