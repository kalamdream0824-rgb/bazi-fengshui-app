package com.bazi.app.report.overall.v3;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A deterministic cross-topic choice. This type deliberately contains no display copy. */
public record OverallAnnualDecision(
    int year,
    OverallTopicSnapshot primary,
    OverallTopicSnapshot secondary,
    String conflictKey,
    String decisionKey,
    String selectedActionKey,
    List<String> evidenceKeys) {

  public OverallAnnualDecision {
    if (year <= 0) throw new IllegalArgumentException("decision year must be positive");
    if (primary == null || secondary == null) {
      throw new IllegalArgumentException("decision requires primary and secondary snapshots");
    }
    if (primary.year() != year || secondary.year() != year) {
      throw new IllegalArgumentException("decision snapshots must belong to its year");
    }
    if (primary.topic() == secondary.topic()) {
      throw new IllegalArgumentException("decision topics must differ");
    }
    conflictKey = OverallTopicSnapshot.required(conflictKey, "conflict key");
    decisionKey = OverallTopicSnapshot.required(decisionKey, "decision key");
    selectedActionKey = OverallTopicSnapshot.required(selectedActionKey, "selected action key");

    Set<String> candidates = new HashSet<>(primary.actionCandidateKeys());
    candidates.addAll(secondary.actionCandidateKeys());
    if (!candidates.contains(selectedActionKey)) {
      throw new IllegalArgumentException("selected action must come from one decision snapshot");
    }

    evidenceKeys = OverallTopicSnapshot.distinctRequired(
        evidenceKeys, "decision evidence keys", 1);
    Set<String> availableEvidence = new LinkedHashSet<>(primary.evidenceKeys());
    availableEvidence.addAll(secondary.evidenceKeys());
    if (!availableEvidence.containsAll(evidenceKeys)) {
      throw new IllegalArgumentException("decision evidence must come from its snapshots");
    }
  }
}
