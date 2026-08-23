package com.bazi.app.report;

import com.bazi.app.report.rules.AnnualRuleCatalog;
import java.util.stream.Collectors;

public final class PlainReportPresenter extends BaseReportPresenter {

  public PlainReportPresenter() {
    this(new AnnualRuleCatalog());
  }

  public PlainReportPresenter(AnnualRuleCatalog catalog) {
    super(catalog, ReportEdition.PLAIN);
  }

  @Override
  protected String overviewInterpretation(ThreeYearAssessment assessment) {
    String years = assessment.years().stream()
        .map(year -> year.year() + "年重点是“" + year.headline() + "”")
        .collect(Collectors.joining("；"));
    return topicCopy(assessment.topic(), "plain") + " " + years
        + "。" + assessment.periodLabel() + "不是同一句话重复，而是按当年的现实信号逐年调整。";
  }

  @Override
  protected String annualInterpretation(YearAssessment year, ReportTopic topic) {
    StringBuilder text = new StringBuilder()
        .append("年度标签：").append(year.stage().label())
        .append("。一句话结论：").append(year.conclusion());
    if (!year.contextEvidence().isEmpty()) {
      text.append("。结合你填写的事业状态：")
          .append(year.contextEvidence().stream().map(ReportEvidence::display).collect(Collectors.joining("；")));
    }
    if (!year.opportunities().isEmpty()) {
      text.append("。机会：").append(findings(year.opportunities()));
    }
    if (!year.pressures().isEmpty()) {
      text.append("。压力：").append(findings(year.pressures()));
    }
    text.append("。建议：").append(joined(year.actions(), "先记录变化，不急于下判断"))
        .append("。现实观察信号：")
        .append(joined(year.realitySignals(), "同类变化是否连续出现"))
        .append("。为什么这样判断：报告同时看了当年的作用方向、你原有的承受能力，以及当年和长期阶段是否互相配合；")
        .append("只有至少两项独立信息相互支持，才会保留这条结论。");
    return text.toString();
  }

  @Override
  protected String annualMethodNote(YearAssessment year) {
    return "";
  }

  @Override
  protected String routeInterpretation(ThreeYearAssessment assessment) {
    return "未来" + assessment.periodLabel() + "不需要同时解决所有问题。建议按年份依次执行："
        + joined(assessment.priorities(), "先记录现实变化，再决定下一步")
        + "。每年复盘一次，若现实信号没有出现，就不要为了迎合报告强行行动。";
  }
}
