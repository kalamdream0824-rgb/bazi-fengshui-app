package com.bazi.app.report;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.ReportDocument.ReportProfile;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Objects;

public final class ReportComposer {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

  private final ThreeYearAssessor assessor;
  private final ReportPresenter plainPresenter;
  private final ReportPresenter professionalPresenter;

  public ReportComposer() {
    this(Clock.system(BUSINESS_ZONE));
  }

  public ReportComposer(Clock clock) {
    AnnualRuleCatalog catalog = new AnnualRuleCatalog();
    assessor = new ThreeYearAssessor(Objects.requireNonNull(clock), catalog);
    plainPresenter = new PlainReportPresenter(catalog);
    professionalPresenter = new ProfessionalReportPresenter(catalog);
  }

  public ReportDocument compose(
      PaipanRequest request,
      PaipanResultDto result,
      ReportTopic topic,
      ReportEdition edition) {
    Objects.requireNonNull(request);
    Objects.requireNonNull(result);
    Objects.requireNonNull(topic);
    Objects.requireNonNull(edition);
    ReportAnalysis analysis = ReportAnalysis.from(request, result);
    ReportProfile profile = new ReportProfile(
        result.solarText(),
        result.lunarText(),
        request.isMale() ? "男" : "女",
        analysis.fact("pillars").value(),
        analysis.fact("dayMaster").value(),
        analysis.fact("currentDayun").value(),
        analysis.fact("currentYear").value());
    String subject = request.name() == null || request.name().isBlank() ? "命主" : request.name();
    ThreeYearAssessment assessment = assessor.assess(request, result, topic);
    ReportPresenter presenter = edition == ReportEdition.PLAIN
        ? plainPresenter
        : professionalPresenter;
    return presenter.present(assessment, profile, subject);
  }
}
