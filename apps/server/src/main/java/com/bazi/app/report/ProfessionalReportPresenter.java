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
        + " 三年轨迹为“" + assessment.trajectory() + "”。年度规则路径：" + annualPath
        + "。同一命局在不同年份使用独立岁运证据，不以固定段落替换年份判断。";
  }

  @Override
  protected String annualInterpretation(YearAssessment year, ReportTopic topic) {
    String evidence = year.evidence().stream().map(ReportEvidence::display).collect(Collectors.joining("；"));
    String counter = year.counterEvidence().stream().map(ReportEvidence::display).collect(Collectors.joining("；"));
    return "核心结论：" + year.conclusion()
        + "。机会项：" + findings(year.opportunities())
        + "。压力项：" + findings(year.pressures())
        + "。规则键：" + joined(year.ruleKeys(), "无")
        + "。完整证据：" + (evidence.isBlank() ? "未达到双证据门槛" : evidence)
        + "。反向证据：" + (counter.isBlank() ? "本轮未检出足以降低结论的反向事实" : counter)
        + "。置信等级：" + confidence(year.confidence())
        + "。行动建议：" + joined(year.actions(), "继续观察")
        + "。现实观察信号：" + joined(year.realitySignals(), "记录同类变化是否持续")
        + "。方法边界：年度结果只表达主题、压力与行动顺序，不锁定具体事件，不下延到月份，"
        + "且不能替代职业、财务或关系中的现实决策。";
  }

  @Override
  protected String annualMethodNote(YearAssessment year) {
    return "方法边界｜规则键=" + joined(year.ruleKeys(), "无")
        + "；证据基准=" + year.basis()
        + "；置信等级=" + confidence(year.confidence())
        + "；反向证据=" + (year.counterEvidence().isEmpty() ? "未检出" : year.counterEvidence().size() + "项")
        + "。阶段标签由通过证据门槛的机会、压力和转换类规则共同决定。";
  }

  @Override
  protected String routeInterpretation(ThreeYearAssessment assessment) {
    String annualActions = assessment.years().stream()
        .map(year -> year.year() + "年：" + joined(year.actions(), "继续观察"))
        .collect(Collectors.joining("；"));
    return "三年行动路线以跨年度去重后的优先项为主："
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
