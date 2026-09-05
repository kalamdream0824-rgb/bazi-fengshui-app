package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthRetrospectivePlan.Citation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lossless normalization of scored decisions. Does not use income focus or generate copy. */
public final class WealthRetrospectiveSignalExtractor {
  public Signals extract(WealthAssessment assessment) {
    if (assessment == null || assessment.year() < 1) {
      throw new IllegalArgumentException("retrospective assessment is required");
    }
    var facts = index(assessment.facts(), WealthAssessment.Fact::id);
    var evidence = index(assessment.evidence(), WealthAssessment.Evidence::id);
    var decisions = index(assessment.decisions(), Decision::path);
    if (!decisions.keySet().equals(WealthRetrospectivePlan.SUBJECTS)) {
      throw new IllegalArgumentException("retrospective assessment requires all five paths");
    }
    Map<String, Citation> citations = new HashMap<>();
    for (var item : evidence.values()) {
      if (!facts.keySet().containsAll(item.rootFactIds())) {
        throw new IllegalArgumentException("missing retrospective root: " + item.id());
      }
      citations.put(item.id(), new Citation(item, item.rootFactIds().stream().map(facts::get).toList()));
    }
    List<PathSignal> paths = new ArrayList<>();
    for (String path : decisions.keySet().stream().sorted().toList()) {
      Decision d = normalize(decisions.get(path));
      List<Citation> items = citations.values().stream().filter(c -> c.evidence().path().equals(path))
          .sorted(Comparator.comparing(c -> c.evidence().id())).toList();
      List<String> supporting = items.stream().filter(c -> c.evidence().weight() > 0)
          .map(c -> c.evidence().id()).toList();
      List<String> limiting = items.stream().filter(c -> c.evidence().weight() < 0)
          .map(c -> c.evidence().id()).toList();
      int positive = items.stream().filter(c -> c.evidence().weight() > 0)
          .mapToInt(c -> c.evidence().weight()).reduce(0, Math::addExact);
      int negative = items.stream().filter(c -> c.evidence().weight() < 0)
          .mapToInt(c -> Math.negateExact(c.evidence().weight())).reduce(0, Math::addExact);
      String stance = positive == 0 ? (negative == 0 ? "quiet" : "restricted")
          : negative == 0 ? "supportive" : "mixed";
      if (d.year() != assessment.year() || !supporting.equals(d.supportingEvidenceIds())
          || !limiting.equals(d.limitingEvidenceIds()) || d.supportWeight() != positive
          || d.limitationWeight() != negative || d.netWeight() != Math.subtractExact(positive, negative)
          || !d.stance().equals(stance)) {
        throw new IllegalArgumentException("inconsistent retrospective decision: " + d.id());
      }
      Map<String, List<Citation>> grouped = new TreeMap<>();
      items.forEach(c -> grouped.computeIfAbsent(angle(c), key -> new ArrayList<>()).add(c));
      paths.add(new PathSignal(d, items, grouped.entrySet().stream()
          .map(entry -> new AngleSignal(entry.getKey(), entry.getValue())).toList(),
          assessment.risk() != null && path.equals(assessment.risk().path())));
    }
    WealthAssessment.Risk risk = assessment.risk();
    if (risk != null) {
      Decision d = decisions.get(risk.path());
      if (d == null || risk.limitingEvidenceIds().isEmpty()
          || !d.limitingEvidenceIds().containsAll(risk.limitingEvidenceIds())) {
        throw new IllegalArgumentException("untraceable retrospective risk");
      }
      risk = new WealthAssessment.Risk(risk.path(), sorted(risk.limitingEvidenceIds()));
    }
    return new Signals(assessment.year(), assessment.ganZhi(), paths, risk);
  }

  private static Decision normalize(Decision d) {
    return new Decision(d.id(), d.year(), d.path(), d.supportWeight(), d.limitationWeight(),
        d.netWeight(), d.stance(), d.strength(), sorted(d.supportingEvidenceIds()),
        sorted(d.limitingEvidenceIds()), sorted(d.reasonCodes()));
  }

  private static List<String> sorted(List<String> values) {
    return values.stream().distinct().sorted().toList();
  }

  private static <T> Map<String, T> index(List<T> values, Function<T, String> key) {
    Map<String, T> result = new HashMap<>();
    for (T value : values) {
      if (key.apply(value) == null || result.putIfAbsent(key.apply(value), value) != null) {
        throw new IllegalArgumentException("duplicate or missing retrospective source id");
      }
    }
    return result;
  }

  /** Relation kinds stay separate; rule/fact keys and values remain in the citations. */
  private static String angle(Citation citation) {
    String key = citation.evidence().factKey();
    return switch (citation.evidence().family()) {
      case ANNUAL_TRIGGER -> {
        if (key.startsWith("annual.branch.clash.")) yield "annual_clash";
        if (key.startsWith("annual.branch.harm.")) yield "annual_harm";
        if (key.startsWith("annual.branch.punishment.")) yield "annual_punishment";
        if (key.startsWith("annual.branch.harmony.")) yield "annual_harmony";
        yield key.equals("annual.stem.ten_god") ? "annual_stem" : "annual_context";
      }
      case DAYUN_CONTEXT -> "dayun_context";
      case NATAL_COMBINATION -> switch (key) {
        case "natal.combination.output_wealth" -> "natal_output_wealth";
        case "natal.combination.wealth_capacity" -> "natal_wealth_capacity";
        case "natal.combination.peer_wealth" -> "natal_shared_responsibility";
        default -> "natal_combination";
      };
      case NATAL_STRUCTURE -> key.equals("natal.balance") ? "natal_balance" : "natal_structure";
    };
  }

  public record Signals(int year, String ganZhi, List<PathSignal> paths, WealthAssessment.Risk risk) {
    public Signals { paths = List.copyOf(paths); }
  }

  public record PathSignal(Decision decision, List<Citation> citations, List<AngleSignal> angles,
      boolean riskSelected) {
    public PathSignal { citations = List.copyOf(citations); angles = List.copyOf(angles); }
  }

  public record AngleSignal(String angle, List<Citation> citations) {
    public AngleSignal { citations = List.copyOf(citations); }

    public Set<String> rootKeys() {
      return citations.stream().flatMap(c -> c.roots().stream())
          .map(f -> f.kind() + "\u0000" + f.code()).collect(Collectors.toUnmodifiableSet());
    }
  }
}
