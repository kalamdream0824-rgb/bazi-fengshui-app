package com.bazi.app.report;

import com.bazi.app.dto.DaYunDto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AnnualContext(
    int year,
    String ganZhi,
    String yearStemTenGod,
    TenGodGroup yearStemGroup,
    DaYunDto activeDaYun,
    List<AnnualFact> natalFacts,
    List<AnnualFact> natalRelations,
    List<AnnualFact> dayunRelations,
    ReportAnalysis natalAnalysis) {

  public AnnualContext {
    Objects.requireNonNull(ganZhi, "ganZhi");
    Objects.requireNonNull(yearStemTenGod, "yearStemTenGod");
    Objects.requireNonNull(yearStemGroup, "yearStemGroup");
    Objects.requireNonNull(natalAnalysis, "natalAnalysis");
    natalFacts = natalFacts == null ? List.of() : List.copyOf(natalFacts);
    natalRelations = natalRelations == null ? List.of() : List.copyOf(natalRelations);
    dayunRelations = dayunRelations == null ? List.of() : List.copyOf(dayunRelations);
  }

  public List<AnnualFact> facts() {
    List<AnnualFact> facts = new ArrayList<>();
    facts.add(new AnnualFact(
        "annual.stem.group." + yearStemGroup.code(),
        "流年天干功能组",
        yearStemTenGod + "（" + yearStemGroup.label() + "）"));
    facts.addAll(natalFacts);
    facts.addAll(natalRelations);
    facts.addAll(dayunRelations);
    return List.copyOf(facts);
  }

  public Set<String> factKeys() {
    Set<String> keys = new LinkedHashSet<>();
    facts().stream().map(AnnualFact::key).forEach(keys::add);
    return Set.copyOf(keys);
  }
}
