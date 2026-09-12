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
import com.bazi.app.report.wealth.v3.WealthNarrativeV3.ActionGuide;
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
    var thesisPlan = new WealthThesisPlanner().plan(compared, headlinePlan);
    var annualBodyWriter = new WealthAnnualBodyWriter();
    var reportSummaryWriter = new WealthReportSummaryWriter();
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
        List<String> sentences = new ArrayList<>();
        for (int index = 0; index < group.size(); index++) {
          sentences.add(annualBodyWriter.income(a, group.get(index), index > 0));
        }
        String text = String.join("", sentences);
        if (group.size() == 1 && !group.get(0).stance().equals("quiet")) text += WealthPlainCopyV3.words(group.get(0).path()).meaning();
        incomeBlocks.add(judgment(a.year() + ".income." + offset / 2,
            "income." + group.stream().map(d -> d.path() + "." + d.stance() + "." + d.strength()).collect(Collectors.joining("+")),
            text, List.of(a.year()), group));
      }
      Decision retention = decision(a, WealthPath.RETENTION.code());
      Risk risk = a.risk() == null ? null : new Risk(a.risk().path(), a.risk().limitingEvidenceIds(),
          judgment(a.year() + ".risk", "risk." + a.risk().path() + "." + headline.themeKey(),
              annualBodyWriter.risk(a, decision(a, a.risk().path()), headline),
              List.of(a.year()), List.of(decision(a, a.risk().path()))));
      List<String> practical = practicalPaths(a, headline.pathKey());
      List<Block> actions = new ArrayList<>();
      List<Block> observations = new ArrayList<>();
      for (int practicalIndex = 0; practicalIndex < practical.size(); practicalIndex++) {
        String path = practical.get(practicalIndex);
        boolean primary = practicalIndex == 0;
        actions.add(block(a.year() + ".action." + path, "general_advice",
            "action." + headline.themeKey() + "." + path,
            annualBodyWriter.action(path, headline, primary),
            List.of(a.year()), List.of(decision(a, path))));
        observations.add(block(a.year() + ".observation." + path, "observation",
            "observation." + headline.themeKey() + "." + path,
            annualBodyWriter.observation(path, headline, primary),
            List.of(a.year()), List.of(decision(a, path))));
      }
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
                new WealthAnnualComparisonWriter().write(a, next, item.comparison()),
                List.of(a.year(), next.year()), related));
      }
      Block overview = judgment(a.year() + ".overview", "annual_overview." + headline.themeKey(),
          headline.text(), List.of(a.year()), List.of(decision(a, headline.pathKey())));
      HeadlineMeta headlineMeta = new HeadlineMeta(headline.plannerVersion(), headline.themeKey(),
          headline.pathKey(), headline.subjectKey(), headline.angleKey(), headline.objectKey(),
          headline.corePhraseKeys(), headline.selectionReasonCodes());
      Decision guideDecision = decision(a, headline.pathKey());
      var guideCopy = new WealthActionGuideWriter().write(guideDecision, headline);
      String guideTemplate = "action_guide." + headline.themeKey() + ".";
      ActionGuide actionGuide = new ActionGuide(headline.pathKey(),
          judgment(a.year() + ".guide.problem", guideTemplate + "problem",
              guideCopy.problem(), List.of(a.year()), List.of(guideDecision)),
          block(a.year() + ".guide.action", "general_advice", guideTemplate + "action",
              guideCopy.action(), List.of(a.year()), List.of(guideDecision)),
          block(a.year() + ".guide.expected", "observation", guideTemplate + "expected_change",
              guideCopy.expectedChange(), List.of(a.year()), List.of(guideDecision)),
          block(a.year() + ".guide.timing", "method_note", guideTemplate + "check_timing",
              guideCopy.checkTiming(), List.of(a.year()), List.of(guideDecision)),
          block(a.year() + ".guide.success", "observation", guideTemplate + "success_signal",
              guideCopy.successSignal(), List.of(a.year()), List.of(guideDecision)),
          block(a.year() + ".guide.condition", "observation", guideTemplate + "adjustment_condition",
              guideCopy.adjustmentCondition(), List.of(a.year()), List.of(guideDecision)),
          block(a.year() + ".guide.fallback", "general_advice", guideTemplate + "fallback_action",
              guideCopy.fallbackAction(), List.of(a.year()), List.of(guideDecision)));
      result.add(new Year(a.year(), a.ganZhi(), a.facts(), a.evidence(), a.decisions(), a.focus(),
          headlineMeta, overview,
          incomeBlocks, judgment(a.year() + ".retention", "retention." + headline.themeKey() + "."
              + retention.stance() + "." + retention.strength(),
              annualBodyWriter.retention(a, retention, headline), List.of(a.year()), List.of(retention)),
          risk, observations, actions, actionGuide, comparison));
    }
    List<Integer> yearNumbers = years.stream().map(WealthAssessment::year).toList();
    List<PathSummary> paths = new ArrayList<>();
    for (WealthPath path : WealthPath.values()) {
      var decisions = years.stream().map(y -> decision(y, path.code())).toList();
      paths.add(new PathSummary(path.code(), judgment("wealth.path." + path.code(), "path_summary." + path.code(),
          grouped(years, y -> reportSummaryWriter.path(y, decision(y, path.code()))), yearNumbers, decisions),
          decisions.stream().map(Decision::id).toList()));
    }
    var riskYears = years.stream().filter(y -> y.risk() != null).toList();
    String riskSummaryText = riskYears.isEmpty() ? "" : removeRepeatedSentences(
        grouped(riskYears, y -> reportSummaryWriter.risk(decision(y, y.risk().path()))),
        paths.stream().map(path -> path.reading().text()).toList());
    Block riskSummary = riskSummaryText.isBlank() ? null
        : judgment("wealth.risk_summary", "risk_summary.years", riskSummaryText,
            riskYears.stream().map(WealthAssessment::year).toList(),
            riskYears.stream().map(y -> decision(y, y.risk().path())).toList());
    Set<String> routePaths = new LinkedHashSet<>();
    years.forEach(y -> routePaths.addAll(practicalPaths(y)));
    var selectedRoutePaths = routePaths.stream().limit(3).toList();
    var routeWriter = new WealthRouteWriter();
    List<Block> route = new ArrayList<>();
    for (int index = 0; index < selectedRoutePaths.size(); index++) {
      String path = selectedRoutePaths.get(index);
      route.add(block("wealth.route." + path, "general_advice", "route." + path,
          routeWriter.write(path, index, selectedRoutePaths.size()), yearNumbers,
          years.stream().map(y -> decision(y, path)).toList()));
    }
    return new WealthNarrativeV3(asOf.toString(), "Asia/Shanghai", years.size(), WealthV3Analyzer.CALCULATION_VERSION,
        WealthExpressionPolicy.VERSION, COPY_VERSION, WealthHeadlineVocabulary.VERSION,
        judgment("wealth.thesis", "thesis.evidence_focus", new WealthThesisWriter().write(thesisPlan),
            yearNumbers, thesisPlan.decisionIds().stream()
                .map(id -> years.stream().flatMap(year -> year.decisions().stream())
                    .filter(decision -> decision.id().equals(id)).findFirst().orElseThrow()).toList()),
        judgment("wealth.summary", "summary.retention_groups", reportSummaryWriter.closing(years),
            yearNumbers, years.stream().map(y -> decision(y, "retention")).toList()),
        paths, riskSummary, result, route,
        block("wealth.reading_note", "method_note", "reading_note",
            reportSummaryWriter.readingNote(years), yearNumbers, List.of()),
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

  private List<String> practicalPaths(WealthAssessment year, String headlinePath) {
    Set<String> paths = new LinkedHashSet<>();
    paths.add(headlinePath);
    if (year.risk() != null) paths.add(year.risk().path());
    paths.add("retention");
    paths.addAll(year.focus().primaryCandidates());
    paths.addAll(year.focus().secondaryCandidates());
    return paths.stream().limit(2).toList();
  }

  private String grouped(List<WealthAssessment> years, Function<WealthAssessment, String> text) {
    Map<String, List<Integer>> groups = new LinkedHashMap<>();
    years.forEach(y -> groups.computeIfAbsent(text.apply(y), ignored -> new ArrayList<>()).add(y.year()));
    return groups.entrySet().stream().map(e -> e.getValue().stream().map(String::valueOf).collect(Collectors.joining("、"))
        + "年：" + e.getKey()).collect(Collectors.joining());
  }

  private String removeRepeatedSentences(String text, List<String> alreadyVisible) {
    Set<String> used = alreadyVisible.stream().flatMap(value -> sentences(value).stream())
        .collect(Collectors.toSet());
    return sentences(text).stream().filter(sentence -> !used.contains(sentence))
        .collect(Collectors.joining());
  }

  private List<String> sentences(String text) {
    return List.of(text.split("(?<=[。！？])")).stream().map(String::strip)
        .filter(sentence -> !sentence.isEmpty()).toList();
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
