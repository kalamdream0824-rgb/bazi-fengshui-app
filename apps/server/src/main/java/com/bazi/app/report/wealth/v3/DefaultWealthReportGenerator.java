package com.bazi.app.report.wealth.v3;

import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class DefaultWealthReportGenerator implements WealthReportGenerator {
  private final WealthV3Analyzer analyzer = new WealthV3Analyzer();
  private final WealthNarrativeWriter writer = new WealthNarrativeWriter();

  @Override
  public WealthNarrativeV3 generate(PaipanResultDto chart, List<AnnualContext> contexts, LocalDate asOf) {
    WealthNarrativeV3 content = writer.plan(analyzer.analyze(chart, contexts), asOf);
    writer.validateDraft(content);
    return content;
  }

  @Override
  public WealthNarrativeV3 generate(
      PaipanResultDto chart,
      AnnualContext previous,
      List<AnnualContext> productYears,
      LocalDate asOf) {
    if (previous == null || productYears == null || productYears.size() != 3) {
      throw new IllegalArgumentException("wealth timeline requires one previous and three product years");
    }
    List<AnnualContext> analysisContexts = new ArrayList<>();
    analysisContexts.add(previous);
    analysisContexts.addAll(productYears);
    var assessments = analyzer.analyze(chart, analysisContexts);
    WealthNarrativeV3 base = writer.plan(assessments.subList(1, assessments.size()), asOf);
    WealthNarrativeV3 content = base.withTimeline(
        new WealthTimelinePlanner().plan(assessments.get(0), base));
    writer.validateDraft(content);
    return content;
  }
}
