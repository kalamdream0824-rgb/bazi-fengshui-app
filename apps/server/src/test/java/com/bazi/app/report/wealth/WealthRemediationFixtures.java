package com.bazi.app.report.wealth;

import com.bazi.app.report.TenGodGroup;
import com.bazi.app.report.wealth.v3.WealthAnnualComparator;
import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthAssessmentInput;
import com.bazi.app.report.wealth.v3.WealthExpressionPolicy;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3;
import com.bazi.app.report.wealth.v3.WealthNarrativeWriter;
import com.bazi.app.report.wealth.v3.WealthV3Analyzer;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Test inputs only. No v3 policy, ranking, provenance inference or copy generation lives here. */
final class WealthRemediationFixtures {
  static final ObjectMapper JSON = new ObjectMapper();

  private WealthRemediationFixtures() {}

  /** Synthetic evidence is injected after scoring; it does not represent a complete birth chart. */
  static WealthPeriodEvaluation scoredCase(String id) throws IOException {
    JsonNode input = find(read("wealth-v2-baseline-inputs.json").get("syntheticCases"), id);
    List<WealthYearEvaluation> years = new ArrayList<>();
    for (int index = 0; index < 3; index++) {
      Map<WealthPath, List<WealthPathEvaluation.ScoredEvidence>> paths = new EnumMap<>(WealthPath.class);
      for (WealthPath path : WealthPath.values()) {
        JsonNode pair = input.get("weights").get(path.ordinal());
        List<WealthPathEvaluation.ScoredEvidence> evidence = new ArrayList<>();
        String key = id + "." + path.code();
        if (input.get("changeSourceByYear").asBoolean()) key += ".source" + index;
        if (pair.get(0).asInt() > 0) evidence.add(scored(
            key + ".support", pair.get(0).asInt(), EvidenceFamily.ANNUAL_TRIGGER, "合成支持条件"));
        if (pair.get(1).asInt() > 0) evidence.add(scored(
            key + ".limit", -pair.get(1).asInt(), EvidenceFamily.DAYUN_CONTEXT, "合成限制条件"));
        paths.put(path, evidence);
      }
      years.add(arbitrate(2026 + index, List.of("丙午", "丁未", "戊申").get(index), paths));
    }
    return new WealthPeriodEvaluation(years);
  }

  /** Frozen full-birth facts, recomputed by the real evaluator; never read the frozen prose. */
  static List<WealthYearFacts> fullBirthFacts(String id) throws IOException {
    JsonNode sample = find(read("wealth-v2-baseline-20260829.json").get("cases"), id);
    return JSON.convertValue(sample.get("facts"), new TypeReference<List<WealthYearFacts>>() {});
  }

  static WealthPeriodEvaluation fullBirthCase(String id) throws IOException {
    return new WealthPathEvaluator().evaluate(fullBirthFacts(id));
  }

  static WealthPeriodEvaluation withPathWeights(
      WealthPeriodEvaluation period, WealthPath path, int support, int limitation) {
    WealthPeriodEvaluation result = period;
    for (int i = 0; i < period.years().size(); i++) {
      result = changeEvidence(result, i, path, ignored -> {
        List<WealthPathEvaluation.ScoredEvidence> items = new ArrayList<>();
        if (support > 0) items.add(scored("variant." + path.code() + ".support", support,
            EvidenceFamily.ANNUAL_TRIGGER, "合成支持条件"));
        if (limitation > 0) items.add(scored("variant." + path.code() + ".limit", -limitation,
            EvidenceFamily.DAYUN_CONTEXT, "合成限制条件"));
        return items;
      });
    }
    return result;
  }

  static WealthPeriodEvaluation changeEvidence(
      WealthPeriodEvaluation period, int yearIndex, WealthPath path,
      UnaryOperator<List<WealthPathEvaluation.ScoredEvidence>> mutation) {
    List<WealthYearEvaluation> years = new ArrayList<>(period.years());
    WealthYearEvaluation original = years.get(yearIndex);
    Map<WealthPath, List<WealthPathEvaluation.ScoredEvidence>> evidence = new EnumMap<>(WealthPath.class);
    original.paths().forEach((p, evaluation) -> evidence.put(p, evaluation.scoredEvidence()));
    evidence.put(path, mutation.apply(evidence.get(path)));
    years.set(yearIndex, arbitrate(original.year(), original.ganZhi(), evidence));
    return new WealthPeriodEvaluation(years);
  }

  static WealthPeriodEvaluation sourceChanged(WealthPeriodEvaluation period) {
    return changeEvidence(period, 1, WealthPath.STABLE_INCOME, old -> {
      List<WealthPathEvaluation.ScoredEvidence> items = new ArrayList<>(old);
      WealthPathEvaluation.ScoredEvidence first = items.get(0);
      items.set(0, scored(first.evidence().key() + ".different_source", first.weight(),
          EvidenceFamily.DAYUN_CONTEXT, "改由大运支持的合成条件"));
      return items;
    });
  }

  static WealthPeriodEvaluation grossWeightsChanged(WealthPeriodEvaluation period) {
    return changeEvidence(period, 1, WealthPath.STABLE_INCOME, old -> {
      List<WealthPathEvaluation.ScoredEvidence> items = new ArrayList<>(old);
      WealthPathEvaluation.ScoredEvidence first = items.get(0);
      items.set(0, new WealthPathEvaluation.ScoredEvidence(first.evidence(), first.weight() + 1));
      items.add(scored("synthetic.extra.limit", -1, EvidenceFamily.DAYUN_CONTEXT, "新增合成限制"));
      return items;
    });
  }

  /** Deliberate fact-level boundary, not a physically consistent chart for these calendar years. */
  static WealthYearFacts syntheticAnnualFacts(int year, String tenGod, String dayun) throws IOException {
    WealthNatalProfile natal = fullBirthFacts("R01").get(0).natalProfile();
    List<WealthEvidence> evidence = new ArrayList<>();
    evidence.add(new WealthEvidence("annual.stem.ten_god", EvidenceFamily.ANNUAL_TRIGGER,
        "合成年度十神条件", tenGod));
    if (dayun != null) evidence.add(new WealthEvidence("dayun.stem.ten_god", EvidenceFamily.DAYUN_CONTEXT,
        "合成大运十神条件", dayun));
    return new WealthYearFacts(year, List.of("丙午", "丁未", "戊申").get(year - 2026),
        tenGod, TenGodGroup.fromTenGod(tenGod), dayun, natal, evidence);
  }

  static WealthPeriodEvaluation factValuePeriod(boolean changeMiddleValue) throws IOException {
    return new WealthPathEvaluator().evaluate(factValueFacts(changeMiddleValue));
  }

  static List<WealthYearFacts> factValueFacts(boolean changeMiddleValue) throws IOException {
    List<WealthYearFacts> facts = new ArrayList<>();
    for (int year = 2026; year <= 2028; year++) {
      facts.add(syntheticAnnualFacts(year,
          year == 2027 && changeMiddleValue ? "伤官" : "食神", null));
    }
    return List.copyOf(facts);
  }

  static WealthYearFacts natalOnly(boolean includeDerivedCombination) throws IOException {
    WealthYearFacts base = syntheticAnnualFacts(2026, "正印", null);
    if (includeDerivedCombination) return base;
    WealthNatalProfile n = base.natalProfile();
    WealthNatalProfile withoutCombination = new WealthNatalProfile(
        n.dayMaster(), n.balanceLevel(), n.tenGodOccurrences(), n.groupCounts(),
        n.evidence().stream().filter(e -> !e.key().equals("natal.combination.output_wealth")).toList());
    return new WealthYearFacts(base.year(), base.ganZhi(), base.annualStemTenGod(), base.annualStemGroup(),
        base.activeDayunTenGod(), withoutCombination, base.evidence());
  }

  static WealthYearFacts withAnnualEvidence(WealthYearFacts original, List<WealthEvidence> evidence) {
    return new WealthYearFacts(original.year(), original.ganZhi(), original.annualStemTenGod(),
        original.annualStemGroup(), original.activeDayunTenGod(), original.natalProfile(), evidence);
  }

  static WealthYearEvaluation arbitrate(
      int year, String ganZhi, Map<WealthPath, List<WealthPathEvaluation.ScoredEvidence>> evidence) {
    Map<WealthPath, WealthPathEvaluation> paths = new EnumMap<>(WealthPath.class);
    for (WealthPath path : WealthPath.values()) {
      List<WealthPathEvaluation.ScoredEvidence> items = evidence.getOrDefault(path, List.of());
      paths.put(path, new WealthPathEvaluation(path,
          items.stream().mapToInt(WealthPathEvaluation.ScoredEvidence::weight).sum(),
          "合成评分输入：不提供表达等级", items));
    }
    // Always exercise the real arbitrator; never reuse a frozen selected direction/risk.
    return new WealthArbitrator().arbitrate(year, ganZhi, paths);
  }

  static WealthPathEvaluation.ScoredEvidence scored(
      String key, int weight, EvidenceFamily family, String value) {
    return new WealthPathEvaluation.ScoredEvidence(
        new WealthEvidence(key, family, "合成边界：非现实经历", value), weight);
  }

  static Reading renderLegacy(WealthPeriodEvaluation period) {
    // Read-only projection of ACTUAL v2 output. Do not infer ties, suppress risks or rewrite copy here.
    // Tasks 4–6 retarget this boundary to v3's actual fields, keeping the semantic assertions intact.
    WealthNarrativePlan actual = new WealthNarrativePlanner().plan(period);
    List<YearReading> years = new ArrayList<>();
    for (int i = 0; i < period.years().size(); i++) {
      WealthYearEvaluation evaluated = period.years().get(i);
      WealthNarrativePlan.YearNarrative copy = actual.years().get(i);
      years.add(new YearReading(copy.year(), List.of(evaluated.primaryIncomePath()),
          List.of(evaluated.secondaryIncomePath()), evaluated.mainRisk(), copy.focus(),
          copy.incomeSource(), copy.retention(), copy.mainLimit(), copy.realitySignals(),
          copy.actions(), JSON.valueToTree(copy.transition()), copy.evidenceKeys()));
    }
    return new Reading(actual.thesis(), actual.mainRisk() == null ? null : actual.mainRisk().judgment(), years);
  }

  static List<WealthAssessment> fullBirthAssessments(String id) throws IOException {
    var sample = find(read("wealth-v2-baseline-20260829.json").get("cases"), id);
    var request = JSON.treeToValue(sample.get("request"), PaipanRequest.class);
    var chart = new BaziService().paipan(request);
    var zone = ZoneId.of("Asia/Shanghai");
    var clock = Clock.fixed(LocalDate.parse(sample.get("asOf").asText()).atStartOfDay(zone).toInstant(), zone);
    var contexts = new AnnualContextFactory(clock).create(request, chart, ReportHorizon.of(sample.get("horizonYears").asInt()));
    return new WealthV3Analyzer().analyze(chart, contexts);
  }

  static Reading render(WealthPeriodEvaluation period) {
    return renderAssessments(period.years().stream().map(WealthRemediationFixtures::assessScored).toList());
  }

  static Reading renderAssessments(List<WealthAssessment> assessments) {
    var actual = new WealthNarrativeWriter().plan(assessments, LocalDate.of(assessments.get(0).year(), 8, 29));
    return observe(actual);
  }

  /** Only projects actual v3 blocks and the evidence referenced by those blocks; no answer calculation. */
  static Reading observe(WealthNarrativeV3 actual) {
    List<YearReading> years = new ArrayList<>();
    for (var year : actual.years()) {
      var reading = new ArrayList<>(year.income());
      reading.add(year.retention());
      if (year.risk() != null) reading.add(year.risk().reading());
      var decisionIds = reading.stream().flatMap(b -> b.decisionIds().stream()).collect(Collectors.toSet());
      Set<String> evidenceIds = new java.util.LinkedHashSet<>();
      year.decisions().stream().filter(d -> decisionIds.contains(d.id())).forEach(d -> {
        evidenceIds.addAll(d.supportingEvidenceIds()); evidenceIds.addAll(d.limitingEvidenceIds());
      });
      var evidenceKeys = year.evidence().stream().filter(e -> evidenceIds.contains(e.id())).map(e -> e.factKey()).distinct().toList();
      years.add(new YearReading(year.year(), year.focus().primaryCandidates().stream().map(WealthRemediationFixtures::path).toList(),
          year.focus().secondaryCandidates().stream().map(WealthRemediationFixtures::path).toList(),
          year.risk() == null ? null : path(year.risk().path()), year.overview().text(),
          year.income().stream().map(b -> b.text()).collect(Collectors.joining()), year.retention().text(),
          year.risk() == null ? null : year.risk().reading().text(),
          year.observations().stream().map(b -> b.text()).toList(), year.actions().stream().map(b -> b.text()).toList(),
          JSON.valueToTree(year.comparison()), evidenceKeys));
    }
    return new Reading(actual.thesis().text(), actual.riskSummary() == null ? null : actual.riskSummary().text(), years);
  }

  private static WealthPath path(String code) {
    return Arrays.stream(WealthPath.values()).filter(p -> p.code().equals(code)).findFirst().orElseThrow();
  }

  static boolean permitsStrongExpression(WealthYearFacts facts, WealthPath path) {
    // Task 4 now observes the REAL v3 expression policy; legacy scoring and prose stay unchanged.
    return new WealthV3Analyzer().assess(facts).decisions().stream()
        .anyMatch(d -> d.path().equals(path.code()) && d.strength().equals("pronounced"));
  }

  static WealthAssessment assessScored(WealthYearEvaluation year) {
    // Explicit provenance for SYNTHETIC score fixtures only, not a resolver for actual chart facts.
    Map<String, WealthAssessment.Fact> roots = new LinkedHashMap<>();
    List<WealthAssessment.Evidence> evidence = new ArrayList<>();
    for (WealthPath path : WealthPath.values()) {
      for (var scored : year.paths().get(path).scoredEvidence()) {
        var source = scored.evidence();
        if (!source.key().matches("^(S[0-9]+|variant|synthetic)\\..*")) {
          throw new IllegalArgumentException("real chart evidence must use WealthProvenanceResolver");
        }
        String rootId = year.year() + ".synthetic.fact." + source.key();
        String kind = switch (source.family()) {
          case NATAL_STRUCTURE, NATAL_COMBINATION -> "natal";
          case ANNUAL_TRIGGER -> "annual";
          case DAYUN_CONTEXT -> "dayun";
        };
        roots.put(rootId, new WealthAssessment.Fact(rootId, kind, source.key(), source.value()));
        evidence.add(new WealthAssessment.Evidence(year.year() + ".synthetic.evidence." + path.code() + "." + source.key(),
            path.code(), "synthetic.rule." + path.code() + "." + source.key(), source.key(),
            source.family(), List.of(rootId), scored.weight()));
      }
    }
    return new WealthExpressionPolicy().assess(new WealthAssessmentInput(year.year(), year.ganZhi(),
        List.copyOf(roots.values()), evidence, Set.of()));
  }

  static List<WealthAnnualComparator.ComparedYear> compareScored(WealthPeriodEvaluation period) {
    return new WealthAnnualComparator().comparePeriod(period.years().stream()
        .map(WealthRemediationFixtures::assessScored).toList());
  }

  static List<WealthAnnualComparator.ComparedYear> compareFacts(List<WealthYearFacts> facts) {
    var analyzer = new WealthV3Analyzer();
    return new WealthAnnualComparator().comparePeriod(facts.stream().map(analyzer::assess).toList());
  }

  /** Test observations, NOT the v3 API schema or a replacement content engine. */
  record Reading(String thesis, String riskSummary, List<YearReading> years) {}

  record YearReading(
      int year, List<WealthPath> primaryCandidates, List<WealthPath> secondaryCandidates,
      WealthPath riskPath, String focus, String incomeSource, String retention, String mainLimit,
      List<String> realitySignals, List<String> actions, JsonNode comparison,
      List<String> evidenceKeys) {}

  static JsonNode read(String resource) throws IOException {
    try (InputStream stream = WealthRemediationFixtures.class
        .getResourceAsStream("/report/" + resource)) {
      if (stream == null) throw new IOException("Missing wealth test resource: " + resource);
      return JSON.readTree(stream);
    }
  }

  static JsonNode find(JsonNode cases, String id) {
    for (JsonNode sample : cases) if (sample.get("id").asText().equals(id)) return sample;
    throw new IllegalArgumentException("Unknown wealth test case: " + id);
  }
}
