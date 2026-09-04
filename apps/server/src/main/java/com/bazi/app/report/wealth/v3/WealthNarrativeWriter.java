package com.bazi.app.report.wealth.v3;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.bazi.app.report.wealth.WealthPath;
import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3.Block;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3.HeadlineMeta;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3.PathSummary;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3.Year;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3.Risk;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3.Comparison;

/** Deterministic draft copy only. No live report generation, saving or page integration. */
public final class WealthNarrativeWriter {
  public static final String COPY_VERSION = WealthPlainCopyV3.VERSION;

  public WealthNarrativeV3 plan(List<WealthAssessment> years, LocalDate asOf) {
    if (years == null || years.isEmpty() || asOf == null || years.get(0).year() != asOf.getYear()) {
      throw new IllegalArgumentException("wealth narrative analysis date must match its first year");
    }
    for (var year : years) {
      if (year.decisions().stream().anyMatch(d -> d.reasonCodes().contains("legacy_provenance_unresolved"))) {
        throw new IllegalArgumentException("wealth narrative requires resolved provenance");
      }
      var checked = new WealthExpressionPolicy().assess(new WealthAssessmentInput(year.year(), year.ganZhi(),
          year.facts(), year.evidence(), Set.of()));
      if (!checked.equals(year)) throw new IllegalArgumentException("wealth narrative judgment violates expression policy");
    }
    if (years.stream().flatMap(y -> y.decisions().stream()).allMatch(d -> d.stance().equals("quiet"))) {
      throw new IllegalArgumentException("wealth narrative coverage is insufficient: general advice only");
    }
    var compared = new WealthAnnualComparator().comparePeriod(years);
    var headlinePlan = new WealthAnnualHeadlinePlanner().plan(compared);
    List<Year> result = new ArrayList<>();
    for (int i = 0; i < compared.size(); i++) {
      var item = compared.get(i);
      var a = item.assessment();
      var headline = headlinePlan.get(i);
      if (headline.year() != a.year()) {
        throw new IllegalStateException("wealth headline plan is not aligned with report years");
      }
      List<Decision> income = incomeDecisions(a);
      List<Block> incomeBlocks = new ArrayList<>();
      for (int offset = 0; offset < income.size(); offset += 2) {
        var group = income.subList(offset, Math.min(offset + 2, income.size()));
        String text = group.stream().map(d -> WealthPlainCopyV3.pathText(a, d)).collect(Collectors.joining());
        if (group.size() == 1 && !group.get(0).stance().equals("quiet")) text += WealthPlainCopyV3.words(group.get(0).path()).meaning();
        incomeBlocks.add(judgment(a.year() + ".income." + offset / 2,
            "income." + group.stream().map(d -> d.path() + "." + d.stance() + "." + d.strength()).collect(Collectors.joining("+")),
            text, List.of(a.year()), group));
      }
      Decision retention = decision(a, WealthPath.RETENTION.code());
      Risk risk = a.risk() == null ? null : new Risk(a.risk().path(), a.risk().limitingEvidenceIds(),
          judgment(a.year() + ".risk", "risk." + a.risk().path(),
              WealthPlainCopyV3.limitation(a, decision(a, a.risk().path())), List.of(a.year()), List.of(decision(a, a.risk().path()))));
      List<String> practical = practicalPaths(a);
      var actions = practical.stream().map(p -> block(a.year() + ".action." + p, "general_advice", "action." + p,
          WealthPlainCopyV3.words(p).action(), List.of(a.year()), List.of(decision(a, p)))).toList();
      var observations = practical.stream().map(p -> block(a.year() + ".observation." + p, "observation", "observation." + p,
          WealthPlainCopyV3.words(p).observation(), List.of(a.year()), List.of(decision(a, p)))).toList();
      Comparison comparison = null;
      if (item.comparison() != null) {
        var next = years.get(i + 1);
        List<Decision> related = new ArrayList<>();
        var paths = item.comparison().changes().isEmpty()
            ? a.decisions().stream().filter(d -> !d.stance().equals("quiet")).map(Decision::path).toList()
            : item.comparison().changes().stream().map(c -> c.path()).toList();
        paths.forEach(p -> { related.add(decision(a, p)); related.add(decision(next, p)); });
        comparison = new Comparison(next.year(), item.comparison().direction(), item.comparison().changes(),
            judgment(a.year() + ".comparison", "comparison." + item.comparison().direction(),
                comparisonText(a, next, item.comparison()), List.of(a.year(), next.year()), related));
      }
      Block overview = judgment(a.year() + ".overview", "annual_overview." + headline.themeKey(),
          headline.text(), List.of(a.year()), List.of(decision(a, headline.pathKey())));
      HeadlineMeta headlineMeta = new HeadlineMeta(headline.plannerVersion(), headline.themeKey(),
          headline.pathKey(), headline.subjectKey(), headline.angleKey(), headline.objectKey(),
          headline.corePhraseKeys(), headline.selectionReasonCodes());
      result.add(new Year(a.year(), a.ganZhi(), a.facts(), a.evidence(), a.decisions(), a.focus(),
          headlineMeta, overview,
          incomeBlocks, judgment(a.year() + ".retention", "retention." + retention.stance() + "." + retention.strength(),
              retentionText(a, retention), List.of(a.year()), List.of(retention)),
          risk, observations, actions, comparison));
    }
    List<Integer> yearNumbers = years.stream().map(WealthAssessment::year).toList();
    List<PathSummary> paths = new ArrayList<>();
    for (WealthPath path : WealthPath.values()) {
      var decisions = years.stream().map(y -> decision(y, path.code())).toList();
      paths.add(new PathSummary(path.code(), judgment("wealth.path." + path.code(), "path_summary." + path.code(),
          grouped(years, y -> WealthPlainCopyV3.compact(y, decision(y, path.code()))), yearNumbers, decisions),
          decisions.stream().map(Decision::id).toList()));
    }
    var riskYears = years.stream().filter(y -> y.risk() != null).toList();
    Block riskSummary = riskYears.isEmpty() ? null : judgment("wealth.risk_summary", "risk_summary.years",
        grouped(riskYears, y -> WealthPlainCopyV3.caution(y, decision(y, y.risk().path()))),
        riskYears.stream().map(WealthAssessment::year).toList(), riskYears.stream().map(y -> decision(y, y.risk().path())).toList());
    Set<String> routePaths = new LinkedHashSet<>();
    years.forEach(y -> routePaths.addAll(practicalPaths(y)));
    var route = routePaths.stream().limit(3).map(p -> block("wealth.route." + p, "general_advice", "route." + p,
        WealthPlainCopyV3.route(p), yearNumbers, years.stream().map(y -> decision(y, p)).toList())).toList();
    return new WealthNarrativeV3(asOf.toString(), "Asia/Shanghai", years.size(), WealthV3Analyzer.CALCULATION_VERSION,
        WealthExpressionPolicy.VERSION, COPY_VERSION, WealthHeadlineVocabulary.VERSION,
        judgment("wealth.thesis", "thesis.focus_groups", grouped(years, this::focusText), yearNumbers,
            years.stream().flatMap(y -> focusDecisions(y).stream()).toList()),
        judgment("wealth.summary", "summary.retention_groups", closingSummary(years),
            yearNumbers, years.stream().map(y -> decision(y, "retention")).toList()),
        paths, riskSummary, result, route,
        block("wealth.reading_note", "method_note", "reading_note",
            "本报告按传统命理规则整理资金变化，不判断职业或收入来源，也不能替代现实中的收益与风险判断。观察事项只用于核对现实情况，不是预测成立的证明。", yearNumbers, List.of()),
        null);
  }

  /** Current-copy validation only: never re-render or overwrite saved historical reports with this method. */
  public void validateDraft(WealthNarrativeV3 draft) {
    if (draft == null) throw new IllegalArgumentException("wealth draft is required");
    var years = draft.years().stream().map(y -> new WealthAssessment(y.year(), y.ganZhi(), y.facts(), y.evidence(),
        y.decisions(), y.focus(), y.risk() == null ? null : new WealthAssessment.Risk(y.risk().path(), y.risk().limitingEvidenceIds()))).toList();
    // The closed generator is the executable license: template, kind, wording, scope and references must all match.
    if (!plan(years, LocalDate.parse(draft.asOf())).equals(draft.withTimeline(null))) {
      throw new IllegalArgumentException("wealth draft violates the current copy license or reference scope");
    }
  }

  private String focusText(WealthAssessment year) {
    var focus = year.focus();
    String names = focus.primaryCandidates().stream().map(WealthPlainCopyV3::label).collect(Collectors.joining("、"));
    if (focus.state().equals("tied")) return names + "可以一起看，不必硬分先后。";
    if (focus.state().equals("leading")) {
      var d = decision(year, focus.primaryCandidates().get(0));
      return names + (d.stance().equals("mixed") ? "可以关注，但限制也要一起看。"
          : d.strength().equals("pronounced") ? "在这份分析里更值得关注。" : "可以作为关注的一个方向。");
    }
    return year.risk() == null ? "各项资金变化都不突出，先看进账、支出和结余是否稳定。"
        : "没有哪项资金变化明显领先。" + WealthPlainCopyV3.caution(year, decision(year, year.risk().path()));
  }

  private List<Decision> focusDecisions(WealthAssessment year) {
    if (!year.focus().primaryCandidates().isEmpty()) return year.focus().primaryCandidates().stream().map(p -> decision(year, p)).toList();
    return year.decisions().stream().filter(d -> !d.stance().equals("quiet")).toList();
  }

  private List<Decision> incomeDecisions(WealthAssessment year) {
    List<String> paths = new ArrayList<>(year.focus().primaryCandidates());
    paths.addAll(year.focus().secondaryCandidates());
    if (paths.isEmpty()) return year.decisions().stream().filter(d -> !d.path().equals("retention")).toList();
    return paths.stream().map(p -> decision(year, p)).toList();
  }

  private List<String> practicalPaths(WealthAssessment year) {
    Set<String> paths = new LinkedHashSet<>();
    year.focus().primaryCandidates().stream().findFirst().ifPresent(paths::add);
    if (year.risk() != null) paths.add(year.risk().path());
    paths.add("retention");
    paths.addAll(year.focus().primaryCandidates());
    return paths.stream().limit(2).toList();
  }

  private String retentionText(WealthAssessment year, Decision d) {
    if (year.risk() != null && year.risk().path().equals("retention")) {
      return d.stance().equals("mixed") ? "有积攒结余的空间，但不能把全部进账当作可花的钱。"
          : "留钱方面需要多留余地，不宜提前把预期收入花出去。";
    }
    String reading = WealthPlainCopyV3.pathText(year, d);
    if (year.risk() != null) {
      // Shared limits remain in the annual risk block; keep distinct retention limits here.
      for (String sentence : WealthPlainCopyV3.limitation(year, decision(year, year.risk().path())).split("(?<=。)")) {
        reading = reading.replace(sentence, "");
      }
    }
    return reading;
  }

  private String closingSummary(List<WealthAssessment> years) {
    var limited = years.stream().filter(y -> decision(y, "retention").limitationWeight() > 0).toList();
    if (!limited.isEmpty()) return limited.stream().map(y -> String.valueOf(y.year())).collect(Collectors.joining("、"))
        + "年，不宜把全部进账都算成能留下的钱。判断资金状况时，也要把新增花费算进去。";
    if (years.stream().allMatch(y -> decision(y, "retention").stance().equals("quiet"))) return "本报告不对结余增减作具体判断，实际情况仍要以收支记录为准。";
    return "进账增加和结余增加是两件事。除了看有多少钱进入，也要核对最后留下多少。";
  }

  private String comparisonText(WealthAssessment current, WealthAssessment next, WealthComparison comparison) {
    if (comparison.direction().equals("unchanged")) return "到" + next.year() + "年，资金重点和需要留意的限制延续，不必另起一套安排。";
    List<String> phrases = new ArrayList<>();
    for (var change : comparison.changes()) {
      String name = WealthPlainCopyV3.label(change.path());
      int support = change.supportDelta();
      int limitation = change.limitationDelta();
      String text;
      if (support > 0 && limitation > 0) text = "有利条件增加，限制也增加，不能只看好的一面。";
      else if (support < 0 && limitation < 0) text = "有利条件减少，但需要留意的限制也减轻了。";
      else if (limitation > 0) text = support < 0 ? "有利条件减少，需要留意的限制增多。" : "需要留意的限制比上年多。";
      else if (limitation < 0) text = support > 0 ? "有利条件增加，需要留意的限制减少。" : "需要留意的限制比上年少。";
      else if (support > 0) text = decision(next, change.path()).strength().equals("limited")
          ? "有利条件增加，但仍不宜当作明确的增收方向。" : "有利条件比上年多，可以重新考虑投入多少。";
      else if (support < 0) text = "有利条件比上年少，不宜照搬上年的收入预期。";
      else if (!change.addedEvidenceIds().isEmpty() || !change.removedEvidenceIds().isEmpty()) text = sourceContinuation(current, next, change.path());
      else if (current.focus().primaryCandidates().contains(change.path()) && next.focus().state().equals("tied")) text = "由单独领先变为与其他方向并列。";
      else if (next.focus().primaryCandidates().contains(change.path())) text = "本身的倾向没变，只是相对位置更靠前。";
      else text = "本身的倾向没变，其他方向相对更靠前。";
      phrases.add(name + "：" + text);
    }
    return "到" + next.year() + "年：" + String.join("", phrases);
  }

  private String sourceContinuation(WealthAssessment current, WealthAssessment next, String path) {
    var d = decision(next, path);
    int before = strengthRank(decision(current, path).strength());
    int after = strengthRank(d.strength());
    if (after > before) return "条件的组合不同，对这一方向的关注程度提高。";
    if (after < before) return "条件的组合不同，对这一方向的关注程度降低。";
    if (d.stance().equals("mixed")) return "具体条件改变，仍需同时看机会与限制。";
    if (d.stance().equals("restricted")) return "具体条件改变，仍以留意限制为主。";
    return d.strength().equals("pronounced") ? "具体条件改变，这一方向仍值得多留意。"
        : d.strength().equals("supported") ? "具体条件改变，仍可以作为一个方向。" : "具体条件改变，仍不宜当作明确的增收方向。";
  }

  private int strengthRank(String strength) {
    return switch (strength) {
      case "none" -> 0;
      case "limited" -> 1;
      case "supported" -> 2;
      case "pronounced" -> 3;
      default -> throw new IllegalArgumentException("unknown wealth expression strength");
    };
  }

  private String grouped(List<WealthAssessment> years, Function<WealthAssessment, String> text) {
    Map<String, List<Integer>> groups = new LinkedHashMap<>();
    years.forEach(y -> groups.computeIfAbsent(text.apply(y), ignored -> new ArrayList<>()).add(y.year()));
    return groups.entrySet().stream().map(e -> e.getValue().stream().map(String::valueOf).collect(Collectors.joining("、"))
        + "年：" + e.getKey()).collect(Collectors.joining());
  }

  private Decision decision(WealthAssessment year, String path) {
    return year.decisions().stream().filter(d -> d.path().equals(path)).findFirst().orElseThrow();
  }

  private Block judgment(String id, String template, String text, List<Integer> years, List<Decision> decisions) {
    String kind = decisions.stream().anyMatch(d -> !d.stance().equals("quiet")) ? "interpretation" : "method_note";
    return block(id, kind, template, text, years, decisions);
  }

  private Block block(String id, String kind, String template, String text, List<Integer> years, List<Decision> decisions) {
    if (text.isBlank() || text.length() > 240) throw new IllegalArgumentException("wealth copy length: " + id + ": " + text);
    for (String banned : List.of("现有证据不足", "先观察再判断", "卡点", "卡住", "承接", "交付", "兑现", "复盘", "第一年", "第二年", "第三年", "一定赚")) {
      if (text.contains(banned)) throw new IllegalArgumentException("unlicensed wealth phrase: " + banned);
    }
    for (String sentence : text.split("[。！？；]")) {
      if (sentence.codePoints().filter(c -> c >= 0x4e00 && c <= 0x9fff).count() > 48) {
        throw new IllegalArgumentException("wealth sentence too long: " + sentence);
      }
    }
    return new Block(id, kind, text, template, years, decisions.stream().map(Decision::id).distinct().toList());
  }
}
