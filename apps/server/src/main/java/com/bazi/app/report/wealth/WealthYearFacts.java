package com.bazi.app.report.wealth;

import com.bazi.app.report.TenGodGroup;
import java.util.List;
import java.util.Objects;

public record WealthYearFacts(
    int year,
    String ganZhi,
    String annualStemTenGod,
    TenGodGroup annualStemGroup,
    String activeDayunTenGod,
    WealthNatalProfile natalProfile,
    List<WealthEvidence> evidence) {

  public WealthYearFacts {
    Objects.requireNonNull(ganZhi, "ganZhi");
    Objects.requireNonNull(annualStemTenGod, "annualStemTenGod");
    Objects.requireNonNull(annualStemGroup, "annualStemGroup");
    Objects.requireNonNull(natalProfile, "natalProfile");
    evidence = evidence == null ? List.of() : List.copyOf(evidence);
  }
}
