package com.bazi.app.report;

import com.bazi.app.report.rules.AnnualRuleCatalog;
import java.util.stream.Collectors;

public final class ProfessionalReportPresenter extends BaseReportPresenter {

  public ProfessionalReportPresenter() {
    this(new AnnualRuleCatalog());
  }

  public ProfessionalReportPresenter(AnnualRuleCatalog catalog) {
    super(catalog, ReportEdition.PROFESSIONAL);
  }

  @Override
  protected String overviewInterpretation(ThreeYearAssessment assessment) {
    String annualPath = assessment.years().stream()
        .map(year -> year.year() + "年【" + year.stage().label() + "】" + year.headline()
            + "，规则=" + joined(year.ruleKeys(), "证据不足"))
        .collect(Collectors.joining("；"));
    return topicCopy(assessment.topic(), "professional")
        + " " + assessment.periodLabel() + "轨迹为“" + assessment.trajectory() + "”。年度规则路径：" + annualPath
        + "。同一命局在不同年份使用独立岁运证据，不以固定段落替换年份判断。";
  }

  @Override
  protected String annualInterpretation(YearAssessment year, ReportTopic topic) {
    String evidence = year.evidence().stream().map(ReportEvidence::display).collect(Collectors.joining("；"));
    String counter = year.counterEvidence().stream().map(ReportEvidence::display).collect(Collectors.joining("；"));
    String context = year.contextEvidence().stream().map(ReportEvidence::display).collect(Collectors.joining("；"));
    StringBuilder text = new StringBuilder("核心结论：").append(year.conclusion());
    if (!year.opportunities().isEmpty()) {
      text.append("。机会项：").append(findings(year.opportunities()));
    }
    if (!year.pressures().isEmpty()) {
      text.append("。压力项：").append(findings(year.pressures()));
    }
    text.append("。规则键：").append(joined(year.ruleKeys(), "无"));
    if (!evidence.isBlank()) {
      text.append("。完整证据：").append(evidence);
    }
    if (!counter.isBlank()) {
      text.append("。反向证据：").append(counter);
    }
    if (!context.isBlank()) {
      text.append("。现实上下文（不计入命理证据）：").append(context);
    }
    text.append("。置信等级：").append(confidence(year.confidence()))
        .append("。行动建议：").append(joined(year.actions(), "记录后再行动"))
        .append("。现实观察信号：")
        .append(joined(year.realitySignals(), "记录同类变化是否持续"))
        .append("。方法边界：年度结果只表达主题、压力与行动顺序，不锁定具体事件，不下延到月份，")
        .append("且不能替代职业、财务或关系中的现实决策。");
    return text.toString();
  }

  @Override
  protected String annualMethodNote(YearAssessment year) {
    String note = "方法边界｜规则键=" + joined(year.ruleKeys(), "无")
        + "；证据基准=" + year.basis()
        + "；置信等级=" + confidence(year.confidence());
    if (!year.counterEvidence().isEmpty()) {
      note += "；反向证据=" + year.counterEvidence().size() + "项";
    }
    return note + "。阶段标签由通过证据门槛的机会、压力和转换类规则共同决定。";
  }

  @Override
  protected String routeInterpretation(ThreeYearAssessment assessment) {
    String annualActions = assessment.years().stream()
        .map(year -> year.year() + "年：" + joined(year.actions(), "继续观察"))
        .collect(Collectors.joining("；"));
    return assessment.periodLabel() + "行动路线以跨年度去重后的优先项为主："
        + joined(assessment.priorities(), "继续记录现实信号")
        + "。逐年执行索引：" + annualActions
        + "。复核时应同时记录命中信号与未命中信号，避免只收集支持原结论的材料。";
  }

  private String confidence(ConfidenceLevel confidence) {
    return switch (confidence) {
      case HIGH -> "高";
      case MEDIUM -> "中";
      case LOW -> "低";
    };
  }
}
