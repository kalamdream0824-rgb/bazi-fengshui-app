package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.WealthPath;
import com.bazi.app.report.wealth.WealthPathEvaluation;
import com.bazi.app.report.wealth.WealthYearFacts;
import com.bazi.app.report.wealth.WealthEvidence;
import com.bazi.app.report.wealth.WealthNatalProfile.TenGodOccurrence;
import com.bazi.app.report.wealth.EvidenceFamily;
import com.bazi.app.report.BranchRelations;
import com.bazi.app.report.BranchRelation;
import com.bazi.app.report.TenGodGroup;
import com.bazi.app.report.wealth.v3.WealthAssessment.Fact;
import com.bazi.app.report.wealth.v3.WealthAssessment.Evidence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Expands existing score sources, never recalculates or adjusts their weights. */
public final class WealthProvenanceResolver {
  private static final Pattern RELATION = Pattern.compile(
      "^annual\\.branch\\.(harmony|clash|punishment|harm)\\.(year|month|day|time|dayun)$");

  public WealthAssessmentInput resolve(WealthYearFacts year, Map<WealthPath, WealthPathEvaluation> raw) {
    return resolve(year, raw, Map.of(), null);
  }

  public WealthAssessmentInput resolve(WealthYearFacts year, Map<WealthPath, WealthPathEvaluation> raw,
      Map<String, String> natalBranches, String dayunBranch) {
    if (!raw.keySet().equals(Set.of(WealthPath.values()))) {
      throw new IllegalArgumentException("all five raw wealth paths are required");
    }
    if (year.annualStemGroup() != TenGodGroup.fromTenGod(year.annualStemTenGod())) {
      throw new IllegalArgumentException("annual wealth fact group conflicts with its value");
    }
    Builder builder = new Builder(year, natalBranches, dayunBranch);
    List<Evidence> evidence = new ArrayList<>();
    Set<String> unresolved = new LinkedHashSet<>();
    for (WealthPath path : WealthPath.values()) {
      WealthPathEvaluation scored = raw.get(path);
      if (scored == null || scored.path() != path || scored.score() != scored.scoredEvidence().stream()
          .mapToInt(WealthPathEvaluation.ScoredEvidence::weight).reduce(0, Math::addExact)) {
        throw new IllegalArgumentException("raw wealth path or total is inconsistent: " + path);
      }
      for (var item : scored.scoredEvidence()) {
        WealthEvidence source = item.evidence();
        if (!source.equals(builder.catalog.get(source.key()))) {
          throw new IllegalArgumentException("scored wealth source is absent or inconsistent: " + source.key());
        }
        Resolved resolved = builder.resolve(source);
        String id = year.year() + ".evidence." + path.code() + "." + source.key();
        evidence.add(new Evidence(id, path.code(), "wealth.v2." + path.code() + "." + resolved.rule(),
            source.key(), source.family(), resolved.roots().stream().distinct().sorted().toList(), item.weight()));
        if (!resolved.complete()) unresolved.add(id);
      }
    }
    return new WealthAssessmentInput(year.year(), year.ganZhi(),
        builder.roots.values().stream().sorted(Comparator.comparing(Fact::id)).toList(),
        evidence.stream().sorted(Comparator.comparing(Evidence::id)).toList(), unresolved);
  }

  private record Resolved(String rule, List<String> roots, boolean complete) {}

  private static final class Builder {
    private final WealthYearFacts year;
    private final Map<String, String> natalBranches;
    private final String dayunBranch;
    private final Map<String, WealthEvidence> catalog = new LinkedHashMap<>();
    private final Map<String, Fact> roots = new LinkedHashMap<>();

    Builder(WealthYearFacts year, Map<String, String> natalBranches, String dayunBranch) {
      this.year = year;
      this.natalBranches = Map.copyOf(natalBranches);
      this.dayunBranch = dayunBranch;
      List<WealthEvidence> all = new ArrayList<>(year.natalProfile().evidence());
      all.addAll(year.evidence());
      for (WealthEvidence source : all) {
        WealthEvidence previous = catalog.putIfAbsent(source.key(), source);
        if (previous != null && !previous.equals(source)) {
          throw new IllegalArgumentException("conflicting wealth source: " + source.key());
        }
      }
    }

    Resolved resolve(WealthEvidence source) {
      String key = source.key();
      if (key.startsWith("natal.ten_god.")) {
        family(source, EvidenceFamily.NATAL_STRUCTURE);
        TenGodOccurrence occurrence = year.natalProfile().tenGodOccurrences().stream()
            .filter(o -> key.equals("natal.ten_god." + o.position() + "." + o.tenGod()))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("missing natal occurrence: " + key));
        value(source, occurrence.position() + "：" + occurrence.tenGod());
        return new Resolved("natal.ten_god." + occurrence.tenGod() + (occurrence.visible() ? ".visible" : ".hidden"),
            List.of(occurrence(occurrence)), true);
      }
      if (key.startsWith("natal.combination.")) {
        family(source, EvidenceFamily.NATAL_COMBINATION);
        List<String> ids = new ArrayList<>(group(TenGodGroup.WEALTH));
        switch (key) {
          case "natal.combination.output_wealth" -> ids.addAll(group(TenGodGroup.OUTPUT));
          case "natal.combination.peer_wealth" -> ids.addAll(group(TenGodGroup.PEER));
          case "natal.combination.wealth_capacity" -> {
            if (!Set.of("中和", "偏强").contains(year.natalProfile().balanceLevel())) {
              throw new IllegalArgumentException("wealth capacity has no supporting balance condition");
            }
            ids.add(balance());
          }
          default -> throw new IllegalArgumentException("unmapped wealth combination: " + key);
        }
        return new Resolved(key, ids, true);
      }
      if (key.equals("natal.balance")) {
        family(source, EvidenceFamily.NATAL_STRUCTURE);
        value(source, year.natalProfile().balanceLevel());
        if (!"偏弱".equals(year.natalProfile().balanceLevel())) {
          throw new IllegalArgumentException("scored balance limitation is not weak");
        }
        List<String> ids = new ArrayList<>(List.of(balance()));
        ids.addAll(activation());
        return new Resolved("weak_balance.wealth_activated", ids, true);
      }
      if (key.equals("annual.stem.ten_god")) {
        family(source, EvidenceFamily.ANNUAL_TRIGGER);
        value(source, year.annualStemTenGod());
        return new Resolved("annual.stem." + year.annualStemTenGod(), List.of(annualStem()), true);
      }
      if (key.equals("dayun.stem.ten_god")) {
        family(source, EvidenceFamily.DAYUN_CONTEXT);
        value(source, year.activeDayunTenGod());
        return new Resolved("dayun.stem." + year.activeDayunTenGod(), List.of(dayunStem()), true);
      }
      var matcher = RELATION.matcher(key);
      if (!matcher.matches()) throw new IllegalArgumentException("unmapped scored wealth source: " + key);
      String relationCode = matcher.group(1);
      String target = matcher.group(2);
      boolean dayun = target.equals("dayun");
      family(source, dayun ? EvidenceFamily.DAYUN_CONTEXT : EvidenceFamily.ANNUAL_TRIGGER);
      String annualBranch = year.ganZhi().substring(1);
      String targetBranch = dayun ? dayunBranch : natalBranches.get(target);
      List<String> ids = new ArrayList<>();
      ids.add(root("annual", "annual.branch", annualBranch));
      if (targetBranch != null) {
        BranchRelation relation = Arrays.stream(BranchRelation.values())
            .filter(r -> r.code().equals(relationCode)).findFirst().orElseThrow();
        if (!BranchRelations.between(annualBranch, targetBranch).contains(relation)) {
          throw new IllegalArgumentException("wealth relation conflicts with actual branches: " + key);
        }
        value(source, annualBranch + "与" + targetBranch + relation.label() + "（" + target + "）");
        ids.add(root(dayun ? "dayun" : "natal", dayun ? "dayun.branch" : "natal.branch." + target, targetBranch));
      }
      if (!relationCode.equals("harmony")) ids.addAll(activation());
      // A missing branch never becomes a fabricated root; keep the known roots and cap interpretation.
      return new Resolved("relation." + relationCode + (dayun ? ".dayun" : ".natal"), ids, targetBranch != null);
    }

    private String occurrence(TenGodOccurrence occurrence) {
      if (occurrence.group() != TenGodGroup.fromTenGod(occurrence.tenGod())) {
        throw new IllegalArgumentException("natal occurrence group conflicts with its value");
      }
      return root("natal", "natal.ten_god." + occurrence.position(), occurrence.tenGod());
    }

    private List<String> group(TenGodGroup group) {
      List<String> ids = year.natalProfile().tenGodOccurrences().stream()
          .filter(o -> o.group() == group).map(this::occurrence).toList();
      if (ids.isEmpty()) throw new IllegalArgumentException("missing combination condition: " + group);
      return ids;
    }

    private String balance() {
      return root("natal", "natal.balance", year.natalProfile().balanceLevel());
    }

    private String annualStem() {
      return root("annual", "annual.stem.ten_god", year.annualStemTenGod());
    }

    private String dayunStem() {
      return root("dayun", "dayun.stem.ten_god", year.activeDayunTenGod());
    }

    private List<String> activation() {
      List<String> ids = new ArrayList<>();
      if (year.annualStemGroup() == TenGodGroup.WEALTH) ids.add(annualStem());
      if ("正财".equals(year.activeDayunTenGod()) || "偏财".equals(year.activeDayunTenGod())) ids.add(dayunStem());
      if (ids.isEmpty()) throw new IllegalArgumentException("missing wealth activation condition");
      return ids;
    }

    private String root(String kind, String code, String value) {
      if (value == null || value.isBlank()) throw new IllegalArgumentException("missing wealth root value: " + code);
      String id = year.year() + ".fact." + code;
      Fact fact = new Fact(id, kind, code, value);
      Fact previous = roots.putIfAbsent(id, fact);
      if (previous != null && !previous.equals(fact)) throw new IllegalArgumentException("conflicting wealth root: " + code);
      return id;
    }

    private void family(WealthEvidence source, EvidenceFamily expected) {
      if (source.family() != expected) throw new IllegalArgumentException("unexpected wealth source family: " + source.key());
    }

    private void value(WealthEvidence source, String expected) {
      if (!source.value().equals(expected)) throw new IllegalArgumentException("wealth source value conflicts: " + source.key());
    }
  }
}
