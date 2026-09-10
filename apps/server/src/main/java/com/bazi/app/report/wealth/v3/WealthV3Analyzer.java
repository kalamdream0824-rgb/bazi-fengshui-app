package com.bazi.app.report.wealth.v3;

import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.WeakSupportProfile;
import com.bazi.app.report.wealth.WealthFactExtractor;
import com.bazi.app.report.wealth.WealthPathEvaluator;
import com.bazi.app.report.wealth.WealthYearFacts;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;

/** Wealth v3 analysis; ReportService uses the complete chart entry for new snapshots. */
public final class WealthV3Analyzer {
  public static final String CALCULATION_VERSION = "wealth-path-v2";

  public WealthAssessment assess(WealthYearFacts year) {
    return assess(year, Map.of(), null);
  }

  public WealthAssessment assess(WealthYearFacts year, Map<String, String> natalBranches, String dayunBranch) {
    var raw = new WealthPathEvaluator().evaluatePaths(year);
    return new WealthExpressionPolicy().assess(new WealthProvenanceResolver()
        .resolve(year, raw, natalBranches, dayunBranch));
  }

  /** Internal judgments with adjacent differences; prose and saving remain separate responsibilities. */
  public List<WealthAnnualComparator.ComparedYear> analyzeWithComparisons(PaipanResultDto chart, List<AnnualContext> contexts) {
    return new WealthAnnualComparator().comparePeriod(analyze(chart, contexts));
  }

  public List<WealthAssessment> analyze(PaipanResultDto chart, List<AnnualContext> contexts) {
    if (contexts == null) throw new IllegalArgumentException("annual contexts are required");
    ReportHorizon.of(contexts.size());
    List<WealthYearFacts> facts = new WealthFactExtractor().extract(chart, contexts);
    Map<String, String> natalBranches = new LinkedHashMap<>();
    for (String key : List.of("year", "month", "day", "time")) natalBranches.put(key, chart.pillars().get(key).zhi());
    List<WealthAssessment> result = new ArrayList<>();
    for (int i = 0; i < contexts.size(); i++) {
      var dayun = contexts.get(i).activeDaYun();
      String branch = dayun == null ? null : dayun.ganZhi().substring(1);
      result.add(withWeakSupportProfile(
          assess(facts.get(i), natalBranches, branch),
          contexts.get(i).natalAnalysis().weakSupportProfile()));
    }
    return List.copyOf(result);
  }

  private WealthAssessment withWeakSupportProfile(
      WealthAssessment assessment,
      WeakSupportProfile profile) {
    List<WealthAssessment.Fact> facts = new ArrayList<>(assessment.facts());
    facts.add(new WealthAssessment.Fact(
        assessment.year() + ".fact.natal.weak_support_profile",
        "natal",
        "natal.weak_support_profile",
        profile.name()));
    facts.sort(Comparator.comparing(WealthAssessment.Fact::id));
    return new WealthAssessment(
        assessment.year(),
        assessment.ganZhi(),
        facts,
        assessment.evidence(),
        assessment.decisions(),
        assessment.focus(),
        assessment.risk());
  }
}
