package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.wealth.EvidenceFamily;
import com.bazi.app.report.wealth.WealthPath;
import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthAssessment.Evidence;
import com.bazi.app.report.wealth.v3.WealthAssessment.Fact;
import com.bazi.app.report.wealth.v3.WealthAssessment.Focus;
import com.bazi.app.report.wealth.v3.WealthComparison.Change;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Compares already assessed years, without scoring or choosing prose. */
public final class WealthAnnualComparator {
  private static final Set<String> PATHS = Arrays.stream(WealthPath.values())
      .map(WealthPath::code).collect(Collectors.toUnmodifiableSet());

  public WealthComparison compare(WealthAssessment current, WealthAssessment next) {
    if (current == null || next == null || (long) next.year() != (long) current.year() + 1) {
      throw new IllegalArgumentException("wealth comparison requires consecutive years");
    }
    View before = view(current);
    View after = view(next);
    List<Change> changes = new ArrayList<>();
    for (WealthPath path : WealthPath.values()) {
      String code = path.code();
      Decision a = before.decisions().get(code);
      Decision b = after.decisions().get(code);
      Map<Source, String> oldSources = before.sources().get(code);
      Map<Source, String> newSources = after.sources().get(code);
      List<String> added = unmatched(newSources, oldSources);
      List<String> removed = unmatched(oldSources, newSources);
      int supportDelta = Math.subtractExact(b.supportWeight(), a.supportWeight());
      int limitationDelta = Math.subtractExact(b.limitationWeight(), a.limitationWeight());
      boolean judgmentChanged = !a.stance().equals(b.stance()) || !a.strength().equals(b.strength())
          || !Set.copyOf(a.reasonCodes()).equals(Set.copyOf(b.reasonCodes()))
          || !role(current.focus(), code).equals(role(next.focus(), code));
      if (supportDelta != 0 || limitationDelta != 0 || !added.isEmpty() || !removed.isEmpty() || judgmentChanged) {
        changes.add(new Change(code, supportDelta, limitationDelta, added, removed));
      }
    }
    return new WealthComparison(next.year(), changes.isEmpty() ? "unchanged" : "changed", changes);
  }

  public List<ComparedYear> comparePeriod(List<WealthAssessment> years) {
    if (years == null) throw new IllegalArgumentException("wealth comparison requires annual assessments");
    ReportHorizon.of(years.size());
    List<ComparedYear> result = new ArrayList<>();
    for (int i = 0; i < years.size(); i++) {
      result.add(new ComparedYear(years.get(i), i + 1 < years.size() ? compare(years.get(i), years.get(i + 1)) : null));
    }
    return List.copyOf(result);
  }

  private List<String> unmatched(Map<Source, String> candidates, Map<Source, String> other) {
    return candidates.entrySet().stream().filter(e -> !other.containsKey(e.getKey()))
        .map(Map.Entry::getValue).sorted().toList();
  }

  private String role(Focus focus, String path) {
    if (focus.primaryCandidates().contains(path)) return focus.state() + ".primary";
    return focus.secondaryCandidates().contains(path) ? "secondary" : "unselected";
  }

  // IDs only locate actual facts. Matching uses rule/source/weight and root values, never ID spelling.
  private record Root(String kind, String code, String value) {}
  private record Source(String path, String ruleKey, String factKey, EvidenceFamily family, int weight, Set<Root> roots) {}
  private record View(Map<String, Decision> decisions, Map<String, Map<Source, String>> sources) {}

  /** Validate the comparison's inputs, not the later ready-report or prose contract. */
  private View view(WealthAssessment year) {
    if (year.year() < 1) throw new IllegalArgumentException("invalid wealth comparison year");
    Map<String, Fact> facts = new HashMap<>();
    Map<List<String>, String> rootValues = new HashMap<>();
    Set<String> ids = new HashSet<>();
    for (Fact fact : year.facts()) {
      uniqueId(ids, fact.id());
      required(fact.code()); required(fact.value());
      if (!Set.of("natal", "annual", "dayun").contains(fact.kind())) {
        throw new IllegalArgumentException("invalid wealth root kind");
      }
      String old = rootValues.putIfAbsent(List.of(fact.kind(), fact.code()), fact.value());
      if (old != null && !old.equals(fact.value())) throw new IllegalArgumentException("conflicting wealth root values");
      facts.put(fact.id(), fact);
    }
    Map<String, Map<Source, String>> sources = new HashMap<>();
    Map<String, List<Evidence>> evidenceByPath = new HashMap<>();
    for (String path : PATHS) {
      sources.put(path, new HashMap<>());
      evidenceByPath.put(path, new ArrayList<>());
    }
    Set<List<String>> sourceKeys = new HashSet<>();
    for (Evidence evidence : year.evidence()) {
      uniqueId(ids, evidence.id());
      required(evidence.ruleKey()); required(evidence.factKey());
      if (!PATHS.contains(evidence.path()) || evidence.family() == null || evidence.weight() == 0
          || evidence.rootFactIds().isEmpty() || !facts.keySet().containsAll(evidence.rootFactIds())
          || new HashSet<>(evidence.rootFactIds()).size() != evidence.rootFactIds().size()
          || !sourceKeys.add(List.of(evidence.path(), evidence.factKey()))) {
        throw new IllegalArgumentException("invalid or untraceable wealth comparison evidence");
      }
      Set<Root> roots = evidence.rootFactIds().stream().map(facts::get)
          .map(f -> new Root(f.kind(), f.code(), f.value())).collect(Collectors.toSet());
      var source = new Source(evidence.path(), evidence.ruleKey(), evidence.factKey(), evidence.family(), evidence.weight(), roots);
      sources.get(evidence.path()).put(source, evidence.id());
      evidenceByPath.get(evidence.path()).add(evidence);
    }
    Map<String, Decision> decisions = new HashMap<>();
    for (Decision decision : year.decisions()) {
      uniqueId(ids, decision.id());
      required(decision.stance()); required(decision.strength());
      if (!PATHS.contains(decision.path()) || decision.year() != year.year()
          || decisions.putIfAbsent(decision.path(), decision) != null) {
        throw new IllegalArgumentException("invalid or duplicate wealth path judgment");
      }
      List<Evidence> items = evidenceByPath.get(decision.path());
      List<Evidence> support = items.stream().filter(e -> e.weight() > 0).toList();
      List<Evidence> limits = items.stream().filter(e -> e.weight() < 0).toList();
      int positive = support.stream().mapToInt(Evidence::weight).reduce(0, Math::addExact);
      int negative = limits.stream().mapToInt(e -> Math.negateExact(e.weight())).reduce(0, Math::addExact);
      if (decision.supportWeight() != positive || decision.limitationWeight() != negative
          || decision.netWeight() != Math.subtractExact(positive, negative)
          || !sameReferences(decision.supportingEvidenceIds(), support)
          || !sameReferences(decision.limitingEvidenceIds(), limits)) {
        throw new IllegalArgumentException("wealth comparison judgment conflicts with scored evidence");
      }
    }
    if (!decisions.keySet().equals(PATHS)) throw new IllegalArgumentException("wealth comparison requires all five paths");
    validateFocus(year.focus());
    return new View(decisions, sources);
  }

  private boolean sameReferences(List<String> actual, List<Evidence> expected) {
    return actual.size() == expected.size() && new HashSet<>(actual).size() == actual.size()
        && Set.copyOf(actual).equals(expected.stream().map(Evidence::id).collect(Collectors.toSet()));
  }

  private void validateFocus(Focus focus) {
    if (focus == null) throw new IllegalArgumentException("wealth comparison requires an explicit focus state");
    List<String> selected = new ArrayList<>(focus.primaryCandidates());
    selected.addAll(focus.secondaryCandidates());
    boolean shape = "none".equals(focus.state()) ? selected.isEmpty()
        : "leading".equals(focus.state()) ? focus.primaryCandidates().size() == 1
        : "tied".equals(focus.state()) && focus.primaryCandidates().size() >= 2 && focus.secondaryCandidates().isEmpty();
    if (!shape || !PATHS.containsAll(selected) || selected.contains(WealthPath.RETENTION.code())
        || new HashSet<>(selected).size() != selected.size()) {
      throw new IllegalArgumentException("invalid wealth comparison focus");
    }
  }

  private void uniqueId(Set<String> ids, String id) {
    required(id);
    if (!ids.add(id)) throw new IllegalArgumentException("duplicate wealth comparison id: " + id);
  }

  private void required(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("wealth comparison source fields cannot be blank");
  }

  /** Wrapper keeps the original independent assessment unchanged; final comparison is null. */
  public record ComparedYear(WealthAssessment assessment, WealthComparison comparison) {}
}
