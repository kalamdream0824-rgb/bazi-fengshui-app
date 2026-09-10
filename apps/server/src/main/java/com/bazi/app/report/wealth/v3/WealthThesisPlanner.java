package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthAnnualComparator.ComparedYear;
import com.bazi.app.report.wealth.v3.WealthAnnualHeadlinePlanner.Headline;
import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthAssessment.Evidence;
import com.bazi.app.report.wealth.v3.WealthAssessment.Fact;
import com.bazi.app.report.wealth.v3.WealthThesisPlan.Citation;
import com.bazi.app.report.wealth.v3.WealthThesisPlan.YearFocus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Builds the thesis from the same evidence-backed annual reading plan shown in the report. */
public final class WealthThesisPlanner {

  public WealthThesisPlan plan(List<ComparedYear> comparedYears, List<Headline> headlines) {
    if (comparedYears == null || headlines == null || comparedYears.size() != headlines.size()
        || comparedYears.isEmpty()) {
      throw new IllegalArgumentException("wealth thesis requires an aligned annual headline plan");
    }
    List<YearFocus> focuses = new ArrayList<>();
    for (int index = 0; index < comparedYears.size(); index++) {
      WealthAssessment assessment = comparedYears.get(index).assessment();
      Headline headline = headlines.get(index);
      if (headline.year() != assessment.year()) {
        throw new IllegalArgumentException("wealth thesis year is not aligned with annual headline");
      }
      String selectedPath = assessment.focus().primaryCandidates().isEmpty()
          ? headline.pathKey() : assessment.focus().primaryCandidates().get(0);
      Decision decision = assessment.decisions().stream()
          .filter(item -> item.path().equals(selectedPath)).findFirst().orElseThrow(() ->
              new IllegalArgumentException("wealth thesis focus decision is not traceable"));
      WealthSemanticClaim claim = new WealthSemanticClaimResolver().resolve(assessment, selectedPath);
      Map<String, Evidence> evidenceById = unique(assessment.evidence(), Evidence::id);
      Map<String, Fact> factById = unique(assessment.facts(), Fact::id);
      List<String> evidenceIds = new ArrayList<>(claim.supportingEvidenceIds());
      evidenceIds.addAll(claim.limitingEvidenceIds());
      List<Citation> primaryCitations = citations(evidenceIds, evidenceById, factById);
      List<String> focusDecisionIds = assessment.focus().primaryCandidates().stream()
          .map(path -> assessment.decisions().stream().filter(item -> item.path().equals(path))
              .findFirst().orElseThrow().id()).collect(Collectors.toCollection(ArrayList::new));
      if (!focusDecisionIds.contains(decision.id())) focusDecisionIds.add(decision.id());
      String secondaryPath = null;
      String secondaryObjectText = null;
      List<Citation> allCitations = new ArrayList<>(primaryCitations);
      if (!headline.pathKey().equals(decision.path())) {
        Decision secondary = assessment.decisions().stream()
            .filter(item -> item.id().equals(headline.decisionIds().get(0)))
            .filter(item -> item.path().equals(headline.pathKey())).findFirst().orElseThrow(() ->
                new IllegalArgumentException("wealth thesis secondary decision is not traceable"));
        List<String> secondaryEvidenceIds = new ArrayList<>(secondary.supportingEvidenceIds());
        secondaryEvidenceIds.addAll(secondary.limitingEvidenceIds());
        allCitations.addAll(citations(secondaryEvidenceIds, evidenceById, factById));
        secondaryPath = secondary.path();
        secondaryObjectText = WealthHeadlineVocabulary.entry(headline.themeKey()).objectText();
        if (!focusDecisionIds.contains(secondary.id())) focusDecisionIds.add(secondary.id());
      }
      focuses.add(new YearFocus(claim, assessment.focus().state(),
          assessment.focus().primaryCandidates(), focusDecisionIds,
          secondaryPath, secondaryObjectText, allCitations, claim.dominantEvidenceIds()));
    }
    return new WealthThesisPlan(focuses);
  }

  private static List<Citation> citations(List<String> ids, Map<String, Evidence> evidenceById,
      Map<String, Fact> factById) {
    return ids.stream().distinct().sorted().map(evidenceById::get).map(evidence -> {
      if (evidence == null || !factById.keySet().containsAll(evidence.rootFactIds())) {
        throw new IllegalArgumentException("wealth thesis evidence is not traceable");
      }
      return new Citation(evidence, evidence.rootFactIds().stream().map(factById::get).toList());
    }).toList();
  }

  private static <T> Map<String, T> unique(List<T> values, Function<T, String> key) {
    return values.stream().collect(Collectors.toMap(key, Function.identity(), (left, right) -> {
      throw new IllegalArgumentException("duplicate wealth thesis source id");
    }));
  }
}
