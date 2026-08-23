package com.bazi.app.report;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class CareerContextualizer {

  YearAssessment apply(YearAssessment year, CareerContext context) {
    return new YearAssessment(
        year.year(),
        year.ganZhi(),
        year.stage(),
        year.headline() + statusSuffix(context.status()),
        year.conclusion() + "。" + statusInterpretation(context.status()),
        year.opportunities(),
        year.pressures(),
        appendDistinct(year.actions(), goalAction(context.goal())),
        appendDistinct(year.realitySignals(), paceSignal(context.pace())),
        year.evidence(),
        year.counterEvidence(),
        List.of(
            new ReportEvidence("career.context.status." + code(context.status()),
                "当前事业状态", context.status().label()),
            new ReportEvidence("career.context.goal." + code(context.goal()),
                "当前事业目标", context.goal().label()),
            new ReportEvidence("career.context.pace." + code(context.pace()),
                "当前工作体感", context.pace().label())),
        year.confidence(),
        year.ruleKeys(),
        year.basis());
  }

  private String statusSuffix(CareerContext.Status status) {
    return switch (status) {
      case EMPLOYED -> "（岗位内变化）";
      case SELF_EMPLOYED -> "（经营与交付）";
      case JOB_SEEKING -> "（求职与入场）";
      case STUDYING -> "（学习与入行）";
    };
  }

  private String statusInterpretation(CareerContext.Status status) {
    return switch (status) {
      case EMPLOYED -> "结合你目前在职，这里优先解释为岗位内职责、协作和评价变化，不直接等同于跳槽";
      case SELF_EMPLOYED -> "结合你目前自营或创业，这里优先解释为客户、项目、团队和交付责任变化，不套用传统升职路径";
      case JOB_SEEKING -> "结合你目前求职中，这里不能解读为现岗位晋升，优先解释为岗位机会、面试推进和重新进入组织";
      case STUDYING -> "结合你目前正在学习或准备入行，这里优先解释为能力证明、作品、实习和进入目标行业的准备";
    };
  }

  private String goalAction(CareerContext.Goal goal) {
    return switch (goal) {
      case PROMOTION -> "把实际承担转化为明确的评价标准、权限或职位安排";
      case JOB_CHANGE -> "只筛选与目标方向匹配、权责和成长路径清楚的岗位";
      case STABILITY -> "优先守住核心职责与稳定交付，不用额外扩张来证明自己";
      case TRANSITION -> "先用项目、作品或小范围尝试验证新方向，再承担不可逆成本";
    };
  }

  private String paceSignal(CareerContext.Pace pace) {
    return switch (pace) {
      case SMOOTH -> "当前顺利节奏能否持续，并开始转化为更正式的评价或回报";
      case STALLED -> "停滞是否来自职责不清、资源不足或方向不匹配，而非单次延误";
      case HIGH_PRESSURE -> "高压是否连续影响交付质量、恢复速度和判断能力";
      case PREPARING_CHANGE -> "是否出现持续投递、内部沟通、作品准备或新方向试做";
    };
  }

  private List<String> appendDistinct(List<String> values, String value) {
    LinkedHashSet<String> result = new LinkedHashSet<>(values);
    result.add(value);
    return new ArrayList<>(result);
  }

  private String code(Enum<?> value) {
    return value.name().toLowerCase();
  }
}
