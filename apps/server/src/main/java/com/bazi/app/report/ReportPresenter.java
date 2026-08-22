package com.bazi.app.report;

import com.bazi.app.report.ReportDocument.ReportProfile;

public interface ReportPresenter {

  ReportDocument present(ThreeYearAssessment assessment, ReportProfile profile, String subject);

  default ReportDocument present(ThreeYearAssessment assessment, ReportProfile profile) {
    return present(assessment, profile, "命主");
  }
}
