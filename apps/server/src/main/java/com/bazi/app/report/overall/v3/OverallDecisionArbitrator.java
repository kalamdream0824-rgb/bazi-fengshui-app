package com.bazi.app.report.overall.v3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Selects one annual priority without using year position, randomness, or prior-year variety. */
public final class OverallDecisionArbitrator {

  private static final Comparator<OverallTopicSnapshot> SUBSTANTIVE_RANKING =
      Comparator.comparingInt((OverallTopicSnapshot item) -> item.urgency().ordinal())
          .reversed()
          .thenComparing(Comparator.comparingInt(OverallTopicSnapshot::directEvidenceCount)
              .reversed())
          .thenComparing(Comparator.comparingInt(
                  (OverallTopicSnapshot item) -> item.confidence().ordinal())
              .reversed());

  private static final Comparator<OverallTopicSnapshot> RANKING =
      SUBSTANTIVE_RANKING
          .thenComparingInt(item -> item.topic().ordinal())
          .thenComparing(OverallTopicSnapshot::focusKey);

  private final OverallConflictCatalog conflicts = new OverallConflictCatalog();

  public OverallAnnualDecision arbitrate(List<OverallTopicSnapshot> snapshots) {
    List<OverallTopicSnapshot> ranked = validateAndRank(snapshots);
    OverallTopicSnapshot primary = ranked.get(0);
    OverallTopicSnapshot secondary = ranked.stream()
        .filter(item -> item.topic() != primary.topic())
        .sorted(SUBSTANTIVE_RANKING
            .thenComparingInt(item -> conflicts.dependencyRank(primary.topic(), item.topic()))
            .thenComparingInt(item -> item.topic().ordinal())
            .thenComparing(OverallTopicSnapshot::focusKey))
        .findFirst()
        .orElseThrow();
    String conflictKey = conflicts.resolve(primary, secondary);
    LinkedHashSet<String> evidence = new LinkedHashSet<>(primary.evidenceKeys());
    evidence.addAll(secondary.evidenceKeys());
    return new OverallAnnualDecision(
        primary.year(),
        primary,
        secondary,
        conflictKey,
        primary.focusKey() + "__" + secondary.focusKey() + "__" + conflictKey,
        primary.actionCandidateKeys().get(0),
        List.copyOf(evidence));
  }

  public List<OverallAnnualDecision> arbitratePeriod(List<OverallTopicSnapshot> snapshots) {
    if (snapshots == null || snapshots.isEmpty()) {
      throw new IllegalArgumentException("overall period snapshots are required");
    }
    Map<Integer, List<OverallTopicSnapshot>> byYear = new TreeMap<>();
    for (OverallTopicSnapshot snapshot : snapshots) {
      byYear.computeIfAbsent(snapshot.year(), ignored -> new ArrayList<>()).add(snapshot);
    }
    List<Integer> years = List.copyOf(byYear.keySet());
    for (int index = 1; index < years.size(); index++) {
      if (years.get(index) != years.get(index - 1) + 1) {
        throw new IllegalArgumentException("overall decision years must be consecutive");
      }
    }
    return byYear.values().stream().map(this::arbitrate).toList();
  }

  private List<OverallTopicSnapshot> validateAndRank(List<OverallTopicSnapshot> snapshots) {
    if (snapshots == null || snapshots.size() < 2) {
      throw new IllegalArgumentException("overall arbitration requires at least two topics");
    }
    int year = snapshots.get(0).year();
    EnumSet<OverallTopicSnapshot.Topic> topics = EnumSet.noneOf(OverallTopicSnapshot.Topic.class);
    for (OverallTopicSnapshot snapshot : snapshots) {
      if (snapshot.year() != year) {
        throw new IllegalArgumentException("overall arbitration requires one year at a time");
      }
      if (!topics.add(snapshot.topic())) {
        throw new IllegalArgumentException("overall arbitration received a duplicate topic");
      }
    }
    return snapshots.stream().sorted(RANKING).toList();
  }
}
