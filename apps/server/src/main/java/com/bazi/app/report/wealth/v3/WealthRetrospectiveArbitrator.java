package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthRetrospectivePlan.Citation;
import com.bazi.app.report.wealth.v3.WealthRetrospectivePlan.Observation;
import com.bazi.app.report.wealth.v3.WealthRetrospectiveSignalExtractor.AngleSignal;
import com.bazi.app.report.wealth.v3.WealthRetrospectiveSignalExtractor.PathSignal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Selects review angles, not events. Ranking weights are the existing assessment weights. */
public final class WealthRetrospectiveArbitrator {
  // Final tie-break only, not an assertion of greater financial opportunity.
  private static final List<String> SUBJECT_PRIORITY = List.of(
      "retention", "stable_income", "skill_income", "project_income", "cooperation_income");
  private final WealthRetrospectiveSignalExtractor extractor = new WealthRetrospectiveSignalExtractor();

  public WealthRetrospectivePlan plan(WealthAssessment assessment) {
    var signals = extractor.extract(assessment);
    List<PathSignal> paths = signals.paths().stream()
        .filter(p -> !p.citations().isEmpty())
        .filter(p -> !p.decision().reasonCodes().contains("legacy_provenance_unresolved"))
        .sorted(PATH_ORDER).toList();
    List<Candidate> candidates = paths.stream().flatMap(path -> path.angles().stream()
        .sorted(ANGLE_ORDER).map(angle -> new Candidate(path, angle))).toList();
    if (candidates.isEmpty()) return new WealthRetrospectivePlan(signals.year(), null, null, null);

    Observation primary = candidates.get(0).observation();
    Observation secondary = candidates.stream()
        .filter(c -> !c.path().decision().path().equals(primary.subject()))
        .filter(c -> !c.angle().angle().equals(primary.angle()))
        .findFirst().map(Candidate::observation).orElse(null);
    List<Observation> used = secondary == null ? List.of(primary) : List.of(primary, secondary);
    Observation hidden = candidates.stream().filter(c -> availableHidden(c.observation(), used))
        .sorted(Comparator.comparingInt(WealthRetrospectiveArbitrator::hiddenPriority)
            .thenComparing(Candidate::path, PATH_ORDER).thenComparing(Candidate::angle, ANGLE_ORDER))
        .findFirst().map(Candidate::observation).orElse(null);
    return new WealthRetrospectivePlan(signals.year(), primary, secondary, hidden);
  }

  // Salience uses both sides: mixed and restricted years must not disappear behind net-positive paths.
  private static final Comparator<PathSignal> PATH_ORDER = Comparator
      .comparingLong((PathSignal p) -> (long) p.decision().supportWeight() + p.decision().limitationWeight()).reversed()
      .thenComparing(Comparator.comparingLong((PathSignal p) -> Math.abs((long) p.decision().netWeight())).reversed())
      .thenComparing(Comparator.comparingInt((PathSignal p) -> p.citations().stream()
          .mapToInt(WealthRetrospectiveArbitrator::specificity).max().orElse(0)).reversed())
      .thenComparingInt(p -> SUBJECT_PRIORITY.indexOf(p.decision().path()));

  private static final Comparator<AngleSignal> ANGLE_ORDER = Comparator
      .comparingLong((AngleSignal a) -> a.citations().stream()
          .mapToLong(c -> Math.abs((long) c.evidence().weight())).sum()).reversed()
      .thenComparing(Comparator.comparingInt((AngleSignal a) -> a.citations().stream()
          .mapToInt(WealthRetrospectiveArbitrator::specificity).max().orElse(0)).reversed())
      .thenComparing(AngleSignal::angle);

  /** Ordinal specificity only: annual > dayun > natal combination > individual natal occurrence. */
  private static int specificity(Citation c) {
    return switch (c.evidence().family()) {
      case ANNUAL_TRIGGER -> 4;
      case DAYUN_CONTEXT -> 3;
      case NATAL_COMBINATION -> 2;
      case NATAL_STRUCTURE -> 1;
    };
  }

  private static int hiddenPriority(Candidate c) {
    String path = c.path().decision().path();
    if (path.equals("retention")) return 0;
    if (c.path().riskSelected() && c.angle().citations().stream().anyMatch(s -> s.evidence().weight() < 0)) return 1;
    if (path.equals("cooperation_income")) return 2;
    if (path.equals("project_income")) return 3;
    return 4;
  }

  private static boolean availableHidden(Observation candidate, List<Observation> used) {
    return used.stream().noneMatch(o -> o.angle().equals(candidate.angle())
        || o.subject().equals(candidate.subject())
            && !Collections.disjoint(o.dominantRootKeys(), candidate.dominantRootKeys()));
  }

  private record Candidate(PathSignal path, AngleSignal angle) {
    Observation observation() {
      var d = path.decision();
      // Preserve all support AND limitation citations, even when one angle is selected for emphasis.
      return new Observation(d.path(), d.stance(), d.strength(), angle.angle(), path.citations(),
          angle.citations().stream().map(c -> c.evidence().id()).toList());
    }
  }
}
