package com.bazi.app.report;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class WealthNarrativePlanner {

  public CareerNarrativePlan plan(ThreeYearAssessment assessment, WealthContext context) {
    Objects.requireNonNull(assessment, "assessment");
    Objects.requireNonNull(context, "context");
    if (assessment.topic() != ReportTopic.WEALTH) {
      throw new IllegalArgumentException("wealth narrative requires the wealth topic");
    }

    List<CareerNarrativePlan.YearNarrative> years = new ArrayList<>();
    for (int index = 0; index < assessment.years().size(); index++) {
      years.add(year(assessment.years().get(index), context, index));
    }
    LinkedHashSet<String> route = new LinkedHashSet<>();
    years.forEach(year -> route.addAll(year.actions()));
    return new CareerNarrativePlan(
        thesis(context, assessment.years()),
        contextSummary(context),
        years,
        route.stream().limit(3).toList());
  }

  private CareerNarrativePlan.YearNarrative year(
      YearAssessment year,
      WealthContext context,
      int index) {
    String ruleKey = year.ruleKeys().isEmpty() ? "wealth.baseline" : year.ruleKeys().get(0);
    return new CareerNarrativePlan.YearNarrative(
        year.year(),
        year.ganZhi(),
        year.stage().label(),
        headline(ruleKey, year.stage(), context.incomeSource()),
        verdict(year.stage(), context.incomeSource()),
        List.of(
            astrologyReason(ruleKey, year.stage(), context.incomeSource()),
            contextReason(context, index)),
        obstacle(context, index),
        List.of(goalAction(context.goal(), index), sourceAction(context.incomeSource(), index)),
        changeCondition(context, index),
        year.evidence().stream().map(ReportEvidence::key).distinct().toList(),
        year.evidence(),
        year.counterEvidence(),
        confidence(year.confidence()));
  }

  private String contextSummary(WealthContext context) {
    return context.incomeSource().label() + " · " + context.goal().label() + " · " + context.pace().label();
  }

  private String thesis(WealthContext context, List<YearAssessment> years) {
    boolean strengthens = momentum(years.get(1).stage()) > momentum(years.get(0).stage());
    return switch (context.goal()) {
      case INCREASE_INCOME -> switch (context.incomeSource()) {
        case SALARY -> strengthens
            ? "今年先把加薪理由准备好，明年再主动争取更多收入。"
            : "这两年先争取看得见的加薪，不要只靠增加工作量。";
        case SELF_EMPLOYED -> strengthens
            ? "今年先稳定客户和回款，明年再争取增加收入。"
            : "这两年先做好最赚钱的项目，不要同时铺开太多生意。";
        case MIXED -> "先稳住工资，再用一个小副业尝试增加收入。";
        case UNSTABLE -> "先找到一项能持续带来收入的工作或服务，再考虑增加收入。";
      };
      case STABILIZE_CASHFLOW -> switch (context.incomeSource()) {
        case SALARY -> "这两年先让每月结余稳定下来，再安排较大的支出。";
        case SELF_EMPLOYED -> "先缩短回款时间，并留够下一轮经营所需的钱。";
        case MIXED -> "先分清工资和副业各自的收支，不要混在一起使用。";
        case UNSTABLE -> "先确定每月最低生活需要，再争取一项稳定收入。";
      };
      case REDUCE_PRESSURE -> "先减少不必要的固定支出，再考虑增加新的花费。";
      case NEW_INCOME_SOURCE -> switch (context.incomeSource()) {
        case SALARY, MIXED -> "先用业余时间做一次小额尝试，有人付费后再继续。";
        case SELF_EMPLOYED -> "先在现有客户中测试新服务，不要一开始就花大钱。";
        case UNSTABLE -> "先选一项最熟悉的能力，争取获得第一笔持续收入。";
      };
    };
  }

  private String headline(
      String ruleKey,
      AnnualStage stage,
      WealthContext.IncomeSource source) {
    return switch (ruleKey) {
      case "wealth.realization" -> switch (source) {
        case SALARY -> "今年更值得争取加薪或奖金";
        case SELF_EMPLOYED -> "今年更容易遇到能带来收入的项目";
        case MIXED -> "主业和副业都有增加收入的机会";
        case UNSTABLE -> "今年更需要把收入来源落实下来";
      };
      case "wealth.monetization" -> switch (source) {
        case SALARY -> "已有能力有机会换来更多收入";
        case SELF_EMPLOYED -> "现有服务有机会带来更多收入";
        case MIXED -> "副业可以先做小规模尝试";
        case UNSTABLE -> "一项熟悉的能力可能带来收入";
      };
      case "wealth.cash_pressure" -> switch (source) {
        case SALARY -> "收入可能增加，但钱不一定留得住";
        case SELF_EMPLOYED -> "项目金额变大，更要留意回款";
        case MIXED -> "主业和副业的支出容易一起增加";
        case UNSTABLE -> "收入还不稳定，先守住生活开支";
      };
      case "wealth.competition" -> "共同支出和收益分配要说清楚";
      case "wealth.contract_caution" -> "合同金额和付款时间要写清楚";
      case "wealth.compliance" -> "涉及钱的手续需要先处理清楚";
      case "wealth.coordination" -> switch (source) {
        case SALARY -> "有人提供机会，更容易增加收入";
        case SELF_EMPLOYED -> "转介绍和合作更容易带来客户";
        case MIXED -> "合适的合作能帮助副业起步";
        case UNSTABLE -> "熟人或同行可能带来工作机会";
      };
      default -> defaultHeadline(stage, source);
    };
  }

  private String defaultHeadline(AnnualStage stage, WealthContext.IncomeSource source) {
    return switch (stage) {
      case ADVANCE -> source == WealthContext.IncomeSource.SELF_EMPLOYED
          ? "今年可以主动争取更多合适的客户"
          : "今年可以主动争取增加收入";
      case PREPARE -> "今年先理清收支，再寻找增收机会";
      case CAUTION -> "今年先守住已有收入，少做冒险决定";
      case TRANSITION -> "今年收支容易变化，要提前留有余地";
      case STABLE -> "今年先让现有收入和支出保持稳定";
    };
  }

  private String verdict(AnnualStage stage, WealthContext.IncomeSource source) {
    return switch (stage) {
      case ADVANCE -> switch (source) {
        case SALARY -> "可以主动谈加薪或奖金，但要用已经做成的事来说明。";
        case SELF_EMPLOYED -> "可以争取更多客户，但要先算清成本和实际到手的钱。";
        case MIXED -> "可以尝试增加副业收入，但不要影响现有工资。";
        case UNSTABLE -> "可以多找收入机会，但先争取一项能够持续的来源。";
      };
      case PREPARE -> "先把每月收入和必要支出记清楚，再决定下一步。";
      case CAUTION -> "先保证必要开支，不要因为想多赚钱就增加不熟悉的投入。";
      case TRANSITION -> "收入或支出可能出现变化，先留一笔应急的钱再做决定。";
      case STABLE -> "保持现有收入来源，同时把不必要的支出慢慢减下来。";
    };
  }

  private String astrologyReason(
      String ruleKey,
      AnnualStage stage,
      WealthContext.IncomeSource source) {
    return switch (ruleKey) {
      case "wealth.realization" -> "今年与收入有关的机会增多，但最终能拿到多少仍取决于实际行动。";
      case "wealth.monetization" -> "今年更适合用已有能力换取收入，先验证是否真的有人愿意付费。";
      case "wealth.cash_pressure" -> "今年进账和花费都可能增加，收入增加不代表最后能留下更多钱。";
      case "wealth.competition" -> "今年共同用钱和收益分配的问题更明显，约定不清容易产生争议。";
      case "wealth.contract_caution" -> "今年涉及金额和付款时间的安排容易变化，口头约定不够稳妥。";
      case "wealth.compliance" -> "今年收入更容易受到合同、税费或单位规则影响，需要先办好手续。";
      case "wealth.coordination" -> "今年他人带来的介绍或合作更容易转成实际的收入机会。";
      default -> defaultAstrologyReason(stage, source);
    };
  }

  private String defaultAstrologyReason(
      AnnualStage stage,
      WealthContext.IncomeSource source) {
    return switch (stage) {
      case ADVANCE -> source == WealthContext.IncomeSource.SELF_EMPLOYED
          ? "今年与客户和收入有关的机会增加，主动联系更容易获得回应。"
          : "今年与收入有关的机会增加，主动争取比等待更容易有回应。";
      case PREPARE -> "今年更适合补足信息和准备条件，看清收支后再行动会更稳。";
      case CAUTION -> "今年花钱或赚钱时更容易遇到压力，先保留余地会更稳。";
      case TRANSITION -> "今年收入和支出的安排容易变化，原来的计划可能需要调整。";
      case STABLE -> "今年收入变化不算明显，把现有收支安排好比冒进更重要。";
    };
  }

  private String contextReason(WealthContext context, int index) {
    if (index == 1) {
      return switch (context.goal()) {
        case INCREASE_INCOME -> "明年是否继续增加收入，要看今年的尝试能否带来实际进账。";
        case STABILIZE_CASHFLOW -> "明年能否更稳定，要看今年能否减少拖延进账和额外花费。";
        case REDUCE_PRESSURE -> "明年压力能否减轻，要看今年能否真正减少固定支出。";
        case NEW_INCOME_SOURCE -> "明年是否继续新方向，要看今年有没有人持续愿意付费。";
      };
    }
    return switch (context.pace()) {
      case STABLE -> "目前收支比较稳定，下一步要看增加收入后能否保持结余。";
      case INCOME_FLUCTUATING -> switch (context.incomeSource()) {
        case SALARY -> "近期工资外收入有波动，先分清哪些收入能够持续。";
        case SELF_EMPLOYED -> "近期项目收入有波动，先找出回款慢或利润低的项目。";
        case MIXED -> "近期总收入有波动，先分清工资和副业分别发生了什么变化。";
        case UNSTABLE -> "近期收入有波动，先找出最有可能持续的一项来源。";
      };
      case SPENDING_PRESSURE -> "近期支出压力较大，新增收入应先用于必要开支和应急准备。";
      case PREPARING_ADJUSTMENT -> "你正在调整收支，先小范围尝试，不要一次改变所有安排。";
    };
  }

  private String obstacle(WealthContext context, int index) {
    if (index == 1) {
      return switch (context.goal()) {
        case INCREASE_INCOME -> "需要确认今年增加的收入能不能持续，而不是只出现一次。";
        case STABILIZE_CASHFLOW -> "需要把进账时间和必要支出安排得更稳定。";
        case REDUCE_PRESSURE -> "需要避免收入刚增加，固定支出也跟着增加。";
        case NEW_INCOME_SOURCE -> "需要确认新收入有人持续付费，而不是只有询问。";
      };
    }
    return switch (context.pace()) {
      case STABLE -> "收支虽然稳定，但增加收入的具体办法还需要落实。";
      case INCOME_FLUCTUATING -> switch (context.incomeSource()) {
        case SALARY -> "工资以外的收入不稳定，需要分清哪些机会值得继续。";
        case SELF_EMPLOYED -> "项目收入不稳定，需要先处理客户数量和回款时间。";
        case MIXED -> "工资和副业变化不同，需要分别看清各自的收支。";
        case UNSTABLE -> "收入来源不稳定，需要先找到能够持续的一项工作或服务。";
      };
      case SPENDING_PRESSURE -> "必要支出占用较多，需要先找出可以减少的固定花费。";
      case PREPARING_ADJUSTMENT -> "想调整的事情较多，需要先选一项影响最大的开始。";
    };
  }

  private String goalAction(WealthContext.Goal goal, int index) {
    return switch (goal) {
      case INCREASE_INCOME -> index == 0
          ? "列出一种最可行的增收办法，并在一个月内完成第一次尝试。"
          : "只保留已经带来实际收入的办法，再逐步增加投入。";
      case STABILIZE_CASHFLOW -> index == 0
          ? "记录每笔收入何时到账，并提前安排当月必要支出。"
          : "把能稳定到账的收入优先用于固定开支和应急准备。";
      case REDUCE_PRESSURE -> index == 0
          ? "列出三项最大支出，先减少其中一项非必要花费。"
          : "收入增加后不要马上增加固定支出，先保留结余。";
      case NEW_INCOME_SOURCE -> index == 0
          ? "选一项最熟悉的能力，先完成一次小额付费尝试。"
          : "有人重复付费后，再决定是否长期投入这个方向。";
    };
  }

  private String sourceAction(WealthContext.IncomeSource source, int index) {
    return switch (source) {
      case SALARY -> index == 0
          ? "整理已经做成的三件事，找合适的时间谈工资或奖金。"
          : "每季度核对工作增加了多少，收入是否也有变化。";
      case SELF_EMPLOYED -> index == 0
          ? "逐个核对项目成本、付款时间和最后实际赚到的钱。"
          : "优先保留付款准时、实际收入较高的客户。";
      case MIXED -> index == 0
          ? "把工资和副业的钱分开记录，不用工资长期补副业。"
          : "副业连续三个月有收入后，再考虑增加时间。";
      case UNSTABLE -> index == 0
          ? "先争取一项能连续三个月带来收入的工作或服务。"
          : "收入稳定前，保留必要生活费，不增加长期支出。";
    };
  }

  private String changeCondition(WealthContext context, int index) {
    if (index == 1) {
      return "连续三个月没有实际收入，或必要支出仍在增加，就要重新选择办法。";
    }
    return switch (context.pace()) {
      case STABLE -> "尝试三个月仍没有增加实际收入，就不要继续追加时间或钱。";
      case INCOME_FLUCTUATING -> "连续两个月收入下降，就先减少投入并保留必要开支。";
      case SPENDING_PRESSURE -> "必要支出已经超过稳定收入，就先暂停新的花费和尝试。";
      case PREPARING_ADJUSTMENT -> "一个月内看不到实际进展，就把计划缩小到一件事。";
    };
  }

  private int momentum(AnnualStage stage) {
    return switch (stage) {
      case CAUTION -> 0;
      case PREPARE -> 1;
      case STABLE, TRANSITION -> 2;
      case ADVANCE -> 3;
    };
  }

  private String confidence(ConfidenceLevel confidence) {
    return switch (confidence) {
      case HIGH -> "高";
      case MEDIUM -> "中";
      case LOW -> "低";
    };
  }
}
