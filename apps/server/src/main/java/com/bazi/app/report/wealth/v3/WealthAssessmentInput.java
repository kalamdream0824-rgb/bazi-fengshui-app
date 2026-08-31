package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthAssessment.Evidence;
import com.bazi.app.report.wealth.v3.WealthAssessment.Fact;
import java.util.List;
import java.util.Set;

/** Trusted internal scored input with provenance; not accepted from an HTTP client. */
public record WealthAssessmentInput(int year, String ganZhi, List<Fact> facts,
    List<Evidence> evidence, Set<String> unresolvedEvidenceIds) {
  public WealthAssessmentInput {
    facts = List.copyOf(facts);
    evidence = List.copyOf(evidence);
    unresolvedEvidenceIds = Set.copyOf(unresolvedEvidenceIds);
  }
}
