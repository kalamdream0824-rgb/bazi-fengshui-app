package com.bazi.app.report.wealth.v3;

import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import java.time.LocalDate;
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
}
