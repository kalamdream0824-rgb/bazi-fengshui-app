package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.ReportContent;
import com.bazi.app.report.NarrativeTimeline;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Versioned wealth v3 snapshot. Public product responses remain three years. */
public record WealthNarrativeV3(String asOf, String zoneId, int horizonYears, String calculationVersion,
    String policyVersion, String copyVersion,
    @JsonInclude(JsonInclude.Include.NON_NULL) String headlinePlannerVersion,
    Block thesis, Block summary, List<PathSummary> pathSummaries,
    Block riskSummary, List<Year> years, List<Block> route, Block readingNote,
    @JsonInclude(JsonInclude.Include.NON_NULL) NarrativeTimeline timeline) implements ReportContent {
  public WealthNarrativeV3 {
    pathSummaries = List.copyOf(pathSummaries); years = List.copyOf(years); route = List.copyOf(route);
  }

  /** Source-compatible constructor for historical v3.0-v3.2 snapshots and the pre-v3.3 writer. */
  public WealthNarrativeV3(String asOf, String zoneId, int horizonYears, String calculationVersion,
      String policyVersion, String copyVersion, Block thesis, Block summary, List<PathSummary> pathSummaries,
      Block riskSummary, List<Year> years, List<Block> route, Block readingNote) {
    this(asOf, zoneId, horizonYears, calculationVersion, policyVersion, copyVersion, null,
        thesis, summary, pathSummaries, riskSummary, years, route, readingNote, null);
  }

  /** Source-compatible constructor for v3.3 snapshots created before timeline support. */
  public WealthNarrativeV3(String asOf, String zoneId, int horizonYears, String calculationVersion,
      String policyVersion, String copyVersion, String headlinePlannerVersion,
      Block thesis, Block summary, List<PathSummary> pathSummaries,
      Block riskSummary, List<Year> years, List<Block> route, Block readingNote) {
    this(asOf, zoneId, horizonYears, calculationVersion, policyVersion, copyVersion,
        headlinePlannerVersion, thesis, summary, pathSummaries, riskSummary, years, route,
        readingNote, null);
  }

  public WealthNarrativeV3 withTimeline(NarrativeTimeline value) {
    return new WealthNarrativeV3(
        asOf, zoneId, horizonYears, calculationVersion, policyVersion, copyVersion,
        headlinePlannerVersion, thesis, summary, pathSummaries, riskSummary, years, route,
        readingNote, value);
  }

  public record Block(String id, String kind, String text, String templateId, List<Integer> years, List<String> decisionIds) {
    public Block { years = List.copyOf(years); decisionIds = List.copyOf(decisionIds); }
  }
  public record PathSummary(String path, Block reading, List<String> yearDecisionIds) {
    public PathSummary { yearDecisionIds = List.copyOf(yearDecisionIds); }
  }
  public record Risk(String path, List<String> limitingEvidenceIds, Block reading) {
    public Risk { limitingEvidenceIds = List.copyOf(limitingEvidenceIds); }
  }
  public record Comparison(int toYear, String direction, List<WealthComparison.Change> changes, Block reading) {
    public Comparison { changes = List.copyOf(changes); }
  }
  public record HeadlineMeta(String plannerVersion, String themeKey, String pathKey, String subjectKey,
      String angleKey, String objectKey, List<String> corePhraseKeys, List<String> selectionReasonCodes) {
    public HeadlineMeta {
      corePhraseKeys = List.copyOf(corePhraseKeys);
      selectionReasonCodes = List.copyOf(selectionReasonCodes);
    }
  }
  public record ActionGuide(String path, Block problem, Block action, Block expectedChange,
      Block checkTiming, Block successSignal, Block adjustmentCondition, Block fallbackAction) {
    public ActionGuide {
      if (path == null || path.isBlank()) throw new IllegalArgumentException("action guide path is required");
      Objects.requireNonNull(problem, "action guide problem");
      Objects.requireNonNull(action, "action guide action");
      Objects.requireNonNull(expectedChange, "action guide expected change");
      Objects.requireNonNull(checkTiming, "action guide check timing");
      Objects.requireNonNull(successSignal, "action guide success signal");
      Objects.requireNonNull(adjustmentCondition, "action guide adjustment condition");
      Objects.requireNonNull(fallbackAction, "action guide fallback action");
    }

    List<Block> blocks() {
      return List.of(problem, action, expectedChange, checkTiming, successSignal,
          adjustmentCondition, fallbackAction);
    }
  }
  public record Year(int year, String ganZhi, List<WealthAssessment.Fact> facts, List<WealthAssessment.Evidence> evidence,
      List<WealthAssessment.Decision> decisions, WealthAssessment.Focus focus,
      @JsonInclude(JsonInclude.Include.NON_NULL) HeadlineMeta headlineMeta,
      Block overview, List<Block> income,
      Block retention, Risk risk, List<Block> observations, List<Block> actions,
      @JsonInclude(JsonInclude.Include.NON_NULL) ActionGuide actionGuide, Comparison comparison) {
    public Year {
      facts = List.copyOf(facts); evidence = List.copyOf(evidence); decisions = List.copyOf(decisions);
      income = List.copyOf(income); observations = List.copyOf(observations); actions = List.copyOf(actions);
    }

    /** Source-compatible constructor for historical snapshots and the pre-v3.3 writer. */
    public Year(int year, String ganZhi, List<WealthAssessment.Fact> facts,
        List<WealthAssessment.Evidence> evidence, List<WealthAssessment.Decision> decisions,
        WealthAssessment.Focus focus, Block overview, List<Block> income, Block retention, Risk risk,
        List<Block> observations, List<Block> actions, Comparison comparison) {
      this(year, ganZhi, facts, evidence, decisions, focus, null, overview, income, retention,
          risk, observations, actions, null, comparison);
    }
  }

  /** All actual text blocks, useful for language/references checks and review rendering. */
  public List<Block> blocks() {
    List<Block> result = new ArrayList<>(List.of(thesis, summary));
    pathSummaries.forEach(p -> result.add(p.reading()));
    if (riskSummary != null) result.add(riskSummary);
    for (Year y : years) {
      result.add(y.overview()); result.addAll(y.income()); result.add(y.retention());
      if (y.risk() != null) result.add(y.risk().reading());
      result.addAll(y.observations()); result.addAll(y.actions());
      if (y.actionGuide() != null) result.addAll(y.actionGuide().blocks());
      if (y.comparison() != null) result.add(y.comparison().reading());
    }
    result.addAll(route); result.add(readingNote);
    return List.copyOf(result);
  }
}
