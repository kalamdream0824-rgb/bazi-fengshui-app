package com.bazi.app.report;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import java.time.Clock;

/** Legacy adapter for PDF and career flows that still use the old type name. */
public final class ThreeYearAssessor {

  private final AnnualPeriodAssessor delegate;

  public ThreeYearAssessor(Clock clock, AnnualRuleCatalog catalog) {
    this.delegate = new AnnualPeriodAssessor(clock, catalog);
  }

  public ThreeYearAssessment assess(
      PaipanRequest request,
      PaipanResultDto chart,
      ReportTopic topic) {
    return assess(request, chart, topic, null);
  }

  public ThreeYearAssessment assess(
      PaipanRequest request,
      PaipanResultDto chart,
      ReportTopic topic,
      CareerContext careerContext) {
    ReportHorizon horizon = topic == ReportTopic.CAREER || topic == ReportTopic.WEALTH
        ? ReportHorizon.of(2)
        : ReportHorizon.of(3);
    return ThreeYearAssessment.from(
        delegate.assess(request, chart, topic, horizon, careerContext));
  }
}
