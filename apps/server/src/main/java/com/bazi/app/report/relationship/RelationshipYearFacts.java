package com.bazi.app.report.relationship;

import com.bazi.app.report.TenGodGroup;
import java.util.List;
import java.util.Objects;

public record RelationshipYearFacts(
    int year,
    String ganZhi,
    String annualStemTenGod,
    TenGodGroup annualStemGroup,
    String activeDayunTenGod,
    TenGodGroup activeDayunGroup,
    RelationshipNatalProfile natalProfile,
    List<RelationshipEvidence> evidence) {

  public RelationshipYearFacts {
    Objects.requireNonNull(ganZhi, "ganZhi");
    Objects.requireNonNull(annualStemTenGod, "annualStemTenGod");
    Objects.requireNonNull(annualStemGroup, "annualStemGroup");
    Objects.requireNonNull(natalProfile, "natalProfile");
    evidence = evidence == null ? List.of() : List.copyOf(evidence);
  }
}
