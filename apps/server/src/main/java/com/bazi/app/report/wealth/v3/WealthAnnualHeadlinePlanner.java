package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthAnnualComparator.ComparedYear;
import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthAssessment.Evidence;
import com.bazi.app.report.wealth.v3.WealthAssessment.Fact;
import com.bazi.app.report.wealth.v3.WealthComparison.Change;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Chooses annual headlines with report-level diversity and no random wording. */
public final class WealthAnnualHeadlinePlanner {

  public List<Headline> plan(List<ComparedYear> comparedYears) {
    if (comparedYears == null || comparedYears.isEmpty()) {
      throw insufficient(Map.of());
    }
    List<List<Candidate>> choices = new ArrayList<>();
    Map<Integer, Integer> counts = new LinkedHashMap<>();
    for (int index = 0; index < comparedYears.size(); index++) {
      List<Candidate> candidates = candidates(comparedYears, index);
      choices.add(candidates);
      counts.put(comparedYears.get(index).assessment().year(), candidates.size());
    }
    if (choices.stream().anyMatch(List::isEmpty)) throw insufficient(counts);

    Selection best = select(choices, 0, new ArrayList<>(), new LinkedHashSet<>(),
        new LinkedHashSet<>(), new LinkedHashSet<>());
    if (best == null) throw insufficient(counts);
    return best.candidates().stream().map(Candidate::headline).toList();
  }

  private List<Candidate> candidates(List<ComparedYear> period, int index) {
    WealthAssessment assessment = period.get(index).assessment();
    String copyStyleSeed = copyStyleSeed(assessment);
    Map<String, Evidence> evidenceById = assessment.evidence().stream()
        .collect(Collectors.toMap(Evidence::id, evidence -> evidence));
    Map<String, Fact> factById = assessment.facts().stream()
        .collect(Collectors.toMap(Fact::id, fact -> fact));
    List<CandidateSeed> seeds = new ArrayList<>();
    for (Decision decision : assessment.decisions()) {
      if (decision.stance().equals("quiet")) continue;
      boolean annualRooted = referencedEvidence(decision, evidenceById).stream()
          .flatMap(evidence -> evidence.rootFactIds().stream())
          .map(factById::get).anyMatch(fact -> fact != null && fact.kind().equals("annual"));
      int changeMagnitude = changeMagnitude(period, index, decision.path());
      int salience = Math.addExact(decision.supportWeight(), decision.limitationWeight());
      String evidenceSignature = referencedEvidence(decision, evidenceById).stream()
          .map(evidence -> evidence.ruleKey() + ":" + evidence.factKey() + ":" + evidence.weight())
          .sorted().collect(Collectors.joining("|"));
      for (var entry : WealthHeadlineVocabulary.forDecision(decision.path(), decision.stance())) {
        seeds.add(new CandidateSeed(entry, decision, annualRooted, changeMagnitude,
            strengthTier(decision.strength()), salience, evidenceSignature));
      }
    }
    seeds.sort(candidateOrder());
    List<Candidate> result = new ArrayList<>();
    int qualityRank = 0;
    CandidateSeed previous = null;
    for (int i = 0; i < seeds.size(); i++) {
      CandidateSeed seed = seeds.get(i);
      if (previous == null || !sameQuality(previous, seed)) qualityRank++;
      List<String> reasons = new ArrayList<>();
      reasons.add("stance." + seed.decision().stance());
      if (seed.annualRooted()) reasons.add("annual_root");
      if (seed.changeMagnitude() > 0) reasons.add("annual_change");
      reasons.add("catalog." + seed.entry().catalogOrder());
      String headlineText = new WealthHeadlineSentenceWriter().write(seed.entry().sentenceSpec(),
          copyStyleSeed + "|" + seed.entry().themeKey(), Math.floorMod(assessment.year(), 5));
      var headline = new Headline(assessment.year(), WealthHeadlineVocabulary.VERSION,
          seed.entry().themeKey(), seed.entry().pathKey(), seed.entry().subjectKey(),
          seed.entry().angleKey(), seed.entry().objectKey(), seed.entry().corePhraseKeys(),
          headlineText, List.of(seed.decision().id()), List.copyOf(reasons),
          qualityRank, i + 1, seeds.size());
      result.add(new Candidate(headline, seed.annualRooted(), seed.changeMagnitude(), seed.salience(),
          seed.entry().catalogOrder(), seed.evidenceSignature()));
      previous = seed;
    }
    return List.copyOf(result);
  }

  private List<Evidence> referencedEvidence(Decision decision, Map<String, Evidence> evidenceById) {
    List<String> ids = new ArrayList<>(decision.supportingEvidenceIds());
    ids.addAll(decision.limitingEvidenceIds());
    return ids.stream().map(evidenceById::get).filter(java.util.Objects::nonNull).toList();
  }

  private String copyStyleSeed(WealthAssessment assessment) {
    String facts = assessment.facts().stream()
        .sorted(Comparator.comparing(Fact::id))
        .map(fact -> fact.kind() + ":" + fact.code() + ":" + fact.value())
        .collect(Collectors.joining("|"));
    String decisions = assessment.decisions().stream()
        .sorted(Comparator.comparing(Decision::path))
        .map(decision -> decision.path() + ":" + decision.stance() + ":" + decision.strength()
            + ":" + decision.supportWeight() + ":" + decision.limitationWeight())
        .collect(Collectors.joining("|"));
    return assessment.year() + "|" + facts + "|" + decisions;
  }

  private int changeMagnitude(List<ComparedYear> period, int index, String path) {
    int result = 0;
    if (index > 0) result = Math.max(result, magnitude(change(period.get(index - 1), path)));
    result = Math.max(result, magnitude(change(period.get(index), path)));
    return result;
  }

  private Change change(ComparedYear year, String path) {
    if (year.comparison() == null) return null;
    return year.comparison().changes().stream().filter(item -> item.path().equals(path)).findFirst().orElse(null);
  }

  private int magnitude(Change change) {
    if (change == null) return 0;
    return Math.addExact(Math.abs(change.supportDelta()), Math.abs(change.limitationDelta()));
  }

  private Comparator<CandidateSeed> candidateOrder() {
    return Comparator.<CandidateSeed>comparingInt(candidate -> candidate.annualRooted() ? 1 : 0).reversed()
        .thenComparing(Comparator.comparingInt(CandidateSeed::changeMagnitude).reversed())
        .thenComparing(Comparator.comparingInt(CandidateSeed::strengthTier).reversed())
        .thenComparing(Comparator.comparingInt(CandidateSeed::salience).reversed())
        .thenComparingInt(candidate -> candidate.entry().catalogOrder())
        .thenComparing(candidate -> candidate.entry().themeKey())
        .thenComparing(candidate -> candidate.entry().subjectKey())
        .thenComparing(CandidateSeed::evidenceSignature);
  }

  private boolean sameQuality(CandidateSeed left, CandidateSeed right) {
    return left.annualRooted() == right.annualRooted()
        && left.changeMagnitude() == right.changeMagnitude()
        && left.strengthTier() == right.strengthTier()
        && left.salience() == right.salience();
  }

  private int strengthTier(String strength) {
    return switch (strength) {
      case "pronounced" -> 3;
      case "supported" -> 2;
      case "limited" -> 1;
      case "none" -> 0;
      default -> throw new IllegalArgumentException("unknown wealth headline strength");
    };
  }

  private Selection select(List<List<Candidate>> choices, int index, List<Candidate> selected,
      Set<String> themes, Set<String> subjects, Set<String> phrases) {
    if (index == choices.size()) return new Selection(List.copyOf(selected));
    Selection best = null;
    for (Candidate candidate : choices.get(index)) {
      Headline headline = candidate.headline();
      if (themes.contains(headline.themeKey()) || subjects.contains(headline.subjectKey())
          || headline.corePhraseKeys().stream().anyMatch(phrases::contains)) continue;
      selected.add(candidate);
      themes.add(headline.themeKey());
      subjects.add(headline.subjectKey());
      phrases.addAll(headline.corePhraseKeys());
      Selection current = select(choices, index + 1, selected, themes, subjects, phrases);
      if (current != null && (best == null || better(current, best))) best = current;
      selected.remove(selected.size() - 1);
      themes.remove(headline.themeKey());
      subjects.remove(headline.subjectKey());
      headline.corePhraseKeys().forEach(phrases::remove);
    }
    return best;
  }

  private boolean better(Selection candidate, Selection current) {
    int worstQualityComparison = Integer.compare(worstQualityRank(candidate), worstQualityRank(current));
    if (worstQualityComparison != 0) return worstQualityComparison < 0;
    int qualityPenaltyComparison = Integer.compare(qualityPenalty(candidate), qualityPenalty(current));
    if (qualityPenaltyComparison != 0) return qualityPenaltyComparison < 0;
    int objectComparison = Integer.compare(distinctObjects(candidate), distinctObjects(current));
    if (objectComparison != 0) return objectComparison > 0;
    int annualComparison = Integer.compare(annualRooted(candidate), annualRooted(current));
    if (annualComparison != 0) return annualComparison > 0;
    int salienceComparison = Integer.compare(totalSalience(candidate), totalSalience(current));
    if (salienceComparison != 0) return salienceComparison > 0;
    return signature(candidate).compareTo(signature(current)) < 0;
  }

  private int worstQualityRank(Selection selection) {
    return selection.candidates().stream().mapToInt(candidate -> candidate.headline().qualityRank()).max().orElseThrow();
  }

  private int qualityPenalty(Selection selection) {
    return selection.candidates().stream().mapToInt(candidate -> candidate.headline().qualityRank() - 1)
        .reduce(0, Math::addExact);
  }

  private int distinctObjects(Selection selection) {
    return (int) selection.candidates().stream().map(candidate -> candidate.headline().objectKey()).distinct().count();
  }

  private int annualRooted(Selection selection) {
    return (int) selection.candidates().stream().filter(Candidate::annualRooted).count();
  }

  private int totalSalience(Selection selection) {
    return selection.candidates().stream().mapToInt(Candidate::salience).reduce(0, Math::addExact);
  }

  private String signature(Selection selection) {
    return selection.candidates().stream()
        .map(candidate -> "%03d:%s:%s".formatted(candidate.catalogOrder(),
            candidate.headline().themeKey(), candidate.evidenceSignature()))
        .collect(Collectors.joining("|"));
  }

  private WealthHeadlinePlanningException insufficient(Map<Integer, Integer> counts) {
    return new WealthHeadlinePlanningException(
        WealthHeadlinePlanningException.Reason.INSUFFICIENT_DIVERSITY, counts);
  }

  private record CandidateSeed(WealthHeadlineVocabulary.Entry entry, Decision decision,
      boolean annualRooted, int changeMagnitude, int strengthTier, int salience, String evidenceSignature) {}

  private record Candidate(Headline headline, boolean annualRooted, int changeMagnitude,
      int salience, int catalogOrder, String evidenceSignature) {}

  private record Selection(List<Candidate> candidates) {}

  public record Headline(int year, String plannerVersion, String themeKey, String pathKey,
      String subjectKey, String angleKey, String objectKey, List<String> corePhraseKeys,
      String text, List<String> decisionIds, List<String> selectionReasonCodes,
      int qualityRank, int candidateRank, int candidateCount) {
    public Headline {
      corePhraseKeys = List.copyOf(corePhraseKeys);
      decisionIds = List.copyOf(decisionIds);
      selectionReasonCodes = List.copyOf(selectionReasonCodes);
    }
  }
}
