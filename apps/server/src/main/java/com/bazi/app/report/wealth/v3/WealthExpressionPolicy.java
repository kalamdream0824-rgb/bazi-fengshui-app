package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.EvidenceFamily;
import com.bazi.app.report.wealth.WealthPath;
import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthAssessment.Evidence;
import com.bazi.app.report.wealth.v3.WealthAssessment.Fact;
import com.bazi.app.report.wealth.v3.WealthAssessment.Focus;
import com.bazi.app.report.wealth.v3.WealthAssessment.Risk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Expression policy only; never adjusts the original scoring weights. */
public final class WealthExpressionPolicy {
  public static final String VERSION = "wealth-expression-v1";

  public WealthAssessment assess(WealthAssessmentInput input) {
    Map<String, Fact> facts = validate(input);
    List<Evidence> evidence = input.evidence().stream().sorted(Comparator.comparing(Evidence::id)).toList();
    List<Decision> decisions = new ArrayList<>();
    for (WealthPath path : WealthPath.values()) {
      List<Evidence> items = evidence.stream().filter(e -> e.path().equals(path.code())).toList();
      List<Evidence> support = items.stream().filter(e -> e.weight() > 0).toList();
      List<Evidence> limits = items.stream().filter(e -> e.weight() < 0).toList();
      int positive = support.stream().mapToInt(Evidence::weight).reduce(0, Math::addExact);
      int negative = limits.stream().mapToInt(e -> Math.negateExact(e.weight())).reduce(0, Math::addExact);
      int net = Math.subtractExact(positive, negative);
      boolean unresolved = items.stream().anyMatch(e -> input.unresolvedEvidenceIds().contains(e.id())
          || e.rootFactIds().stream().map(facts::get).noneMatch(f -> f.kind().equals(origin(e.family()))));
      boolean independent = independentSupport(support, facts);
      String stance = positive == 0 ? (negative == 0 ? "quiet" : "restricted")
          : negative == 0 ? "supportive" : "mixed";
      String strength = items.isEmpty() ? "none"
          : unresolved || negative > 0 || net < 3 ? "limited"
          : net >= 6 && independent ? "pronounced" : "supported";
      List<String> reasons = new ArrayList<>();
      if (items.isEmpty()) reasons.add("no_evidence");
      else {
        if (negative > 0) reasons.add("has_limitations");
        if (net < 3) reasons.add("limited_net_support");
        if (positive > 0 && !independent) reasons.add("single_origin");
        if (unresolved) reasons.add("legacy_provenance_unresolved");
      }
      decisions.add(new Decision(input.year() + ".decision." + path.code(), input.year(), path.code(),
          positive, negative, net, stance, strength, ids(support), ids(limits), reasons));
    }

    // Stable path order is only a presentation tie-break, never a unique opportunity assertion.
    Decision risk = null;
    for (Decision decision : decisions) {
      if (decision.limitationWeight() > 0
          && (risk == null || decision.limitationWeight() > risk.limitationWeight())) risk = decision;
    }
    return new WealthAssessment(input.year(), input.ganZhi(),
        input.facts().stream().sorted(Comparator.comparing(Fact::id)).toList(), evidence,
        decisions, focus(decisions), risk == null ? null : new Risk(risk.path(), risk.limitingEvidenceIds()));
  }

  private Focus focus(List<Decision> decisions) {
    List<Decision> eligible = decisions.stream()
        .filter(d -> !d.path().equals(WealthPath.RETENTION.code()) && d.netWeight() >= 3)
        .filter(d -> !d.reasonCodes().contains("legacy_provenance_unresolved"))
        .sorted(Comparator.comparingInt(Decision::netWeight).reversed()).toList();
    if (eligible.isEmpty()) return new Focus("none", List.of(), List.of());
    int highest = eligible.get(0).netWeight();
    List<String> primary = eligible.stream().filter(d -> d.netWeight() == highest).map(Decision::path).toList();
    if (primary.size() > 1) return new Focus("tied", primary, List.of());
    List<Decision> rest = eligible.subList(1, eligible.size());
    List<String> secondary = rest.isEmpty() ? List.of()
        : rest.stream().filter(d -> d.netWeight() == rest.get(0).netWeight()).map(Decision::path).toList();
    return new Focus("leading", primary, secondary);
  }

  private boolean independentSupport(List<Evidence> support, Map<String, Fact> facts) {
    for (int i = 0; i < support.size(); i++) {
      for (int j = i + 1; j < support.size(); j++) {
        Evidence a = support.get(i);
        Evidence b = support.get(j);
        if (origin(a.family()).equals(origin(b.family()))) continue;
        Set<String> rootsA = rootIdentities(a, facts);
        if (rootIdentities(b, facts).stream().noneMatch(rootsA::contains)) return true;
      }
    }
    return false;
  }

  private Set<String> rootIdentities(Evidence evidence, Map<String, Fact> facts) {
    return evidence.rootFactIds().stream().map(facts::get)
        .map(f -> f.kind() + "\u0000" + f.code()).collect(Collectors.toSet());
  }

  private String origin(EvidenceFamily family) {
    return switch (family) {
      case NATAL_STRUCTURE, NATAL_COMBINATION -> "natal";
      case ANNUAL_TRIGGER -> "annual";
      case DAYUN_CONTEXT -> "dayun";
    };
  }

  private List<String> ids(List<Evidence> evidence) {
    return evidence.stream().map(Evidence::id).toList();
  }

  private Map<String, Fact> validate(WealthAssessmentInput input) {
    if (input == null || input.year() < 1 || input.ganZhi() == null
        || input.ganZhi().codePointCount(0, input.ganZhi().length()) != 2) {
      throw new IllegalArgumentException("invalid wealth assessment year");
    }
    Map<String, Fact> facts = new HashMap<>();
    Map<String, String> values = new HashMap<>();
    Set<String> allIds = new HashSet<>();
    for (Fact fact : input.facts()) {
      required(fact.id()); required(fact.code()); required(fact.value());
      if (!Set.of("natal", "annual", "dayun").contains(fact.kind()) || !allIds.add(fact.id())) {
        throw new IllegalArgumentException("invalid or duplicate wealth root fact: " + fact.id());
      }
      String previous = values.putIfAbsent(fact.kind() + "\u0000" + fact.code(), fact.value());
      if (previous != null && !previous.equals(fact.value())) {
        throw new IllegalArgumentException("conflicting values for wealth root: " + fact.code());
      }
      facts.put(fact.id(), fact);
    }
    Set<String> paths = java.util.Arrays.stream(WealthPath.values()).map(WealthPath::code).collect(Collectors.toSet());
    Set<String> evidenceIds = new HashSet<>();
    Set<String> sourceKeys = new HashSet<>();
    for (Evidence e : input.evidence()) {
      required(e.id()); required(e.ruleKey()); required(e.factKey());
      if (!allIds.add(e.id()) || !paths.contains(e.path()) || e.family() == null || e.weight() == 0
          || !sourceKeys.add(e.path() + "\u0000" + e.factKey()) || e.rootFactIds().isEmpty()
          || new HashSet<>(e.rootFactIds()).size() != e.rootFactIds().size()
          || !facts.keySet().containsAll(e.rootFactIds())) {
        throw new IllegalArgumentException("invalid, duplicate or untraceable wealth evidence: " + e.id());
      }
      evidenceIds.add(e.id());
    }
    if (!evidenceIds.containsAll(input.unresolvedEvidenceIds())) {
      throw new IllegalArgumentException("unknown unresolved wealth evidence");
    }
    return facts;
  }

  private void required(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("wealth source fields cannot be blank");
  }
}
