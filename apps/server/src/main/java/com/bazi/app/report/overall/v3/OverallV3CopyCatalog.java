package com.bazi.app.report.overall.v3;

import com.bazi.app.report.overall.v3.OverallTopicSnapshot.Topic;

/** Customer-visible copy selected only from calculated cross-topic meaning. */
final class OverallV3CopyCatalog {

  Copy find(OverallAnnualDecision decision) {
    String primaryFocus = focusObject(decision.primary());
    String secondaryFocus = focusObject(decision.secondary());
    return switch (decision.conflictKey()) {
      case "capacity_before_career_expansion" -> new Copy(
          "眼前值得推进的是" + secondaryFocus + "，但" + primaryFocus + "已经偏紧。",
          "先延后一件不着急的事，再围绕" + secondaryFocus + "写清本周能完成的一个成果。",
          "这样能为成果留出稳定时间，也能避免休息继续被占用。",
          "开始执行后，每周核对" + primaryFocus + "；四周后统一判断。",
          "有效的信号是：本周成果按时完成，同时每周仍有固定休息时间。",
          "如果连续两周仍靠减少休息推进这件事，就要缩小范围。",
          "先保留最重要的一步，其余安排等时间恢复后再做。");
      case "cash_buffer_before_growth" -> new Copy(
          "有事情可以继续推进，但要先核对" + primaryFocus + "。",
          "先把" + primaryFocus + "算清，再把" + secondaryFocus + "限定为一件四周内可完成的事。",
          "这样能判断" + primaryFocus + "是否改善，保住实际余钱，也能检验投入能否形成成果。",
          "从执行当天起，每周记录" + primaryFocus + "；满四周后统一核对。",
          "有效的信号是：" + primaryFocus + "可以说清，" + secondaryFocus + "也有实际结果。",
          "如果两次核对都发现" + primaryFocus + "变差，就不要继续增加投入。",
          "暂停为" + secondaryFocus + "新增花费，先用已有条件完成一个较小结果。");
      case "relationship_stability_before_growth" -> new Copy(
          "一件重要事情正在向前走，但" + primaryFocus + "还有问题没有说清。",
          "先用一次具体沟通说清彼此需要，再围绕" + secondaryFocus + "完成一个成果。",
          "这样能让" + primaryFocus + "得到明确回应，也能让成果继续推进。",
          "沟通后每周核对" + primaryFocus + "；四周后统一确认。",
          "有效的信号是：" + primaryFocus + "得到清楚答复，约好的事情也有人落实。",
          "如果连续两次沟通都没有回应，就要减少影响" + secondaryFocus + "的共同安排。",
          "先守住自己的时间和边界，只推进" + secondaryFocus + "中不依赖他人的部分。");
      case "career_with_wealth_watch" -> new Copy(
          "当前最值得投入的是" + primaryFocus + "，同时不能忽略" + secondaryFocus + "。",
          "先完成一个与" + primaryFocus + "有关的成果，并把为此发生的花费单独记下。",
          "这样能看清" + primaryFocus + "是否形成成果，也能知道实际余钱有没有减少。",
          "开始以后每周核对" + primaryFocus + "和" + secondaryFocus + "；四周后统一判断。",
          "有效的信号是：" + primaryFocus + "留下成果，实际余钱也没有持续下降。",
          "如果" + primaryFocus + "没有进展且花费连续增加，就要停止追加投入。",
          "改用较低成本处理" + primaryFocus + "，完成一个小结果后再决定是否继续。");
      default -> generic(decision);
    };
  }

  private Copy generic(OverallAnnualDecision decision) {
    OverallTopicSnapshot primary = decision.primary();
    OverallTopicSnapshot secondary = decision.secondary();
    String primaryObject = focusObject(primary);
    String secondaryObject = focusObject(secondary);
    if (primary.stance() == OverallTopicSnapshot.Stance.PRESSURED) {
      return commonTail(
          "当前与" + primaryObject + "有关的负担偏重，也会牵动" + secondaryObject + "。",
          "先压缩当前安排：",
          "这样能减轻当前负担，并避免" + secondaryObject + "继续受影响。",
          "有效的信号是：",
          "缩小范围后",
          primary,
          secondary);
    }
    if (primary.stance() == OverallTopicSnapshot.Stance.MIXED) {
      return commonTail(
          "围绕" + primaryObject + "既有有利条件，也有明显牵制，还要兼顾" + secondaryObject + "。",
          "先只验证一件事：",
          "这样能检验这一步是否有效，也能减少对" + secondaryObject + "的影响。",
          "本轮有效的信号是：",
          "拆分安排后",
          primary,
          secondary);
    }
    if (primary.stance() == OverallTopicSnapshot.Stance.BALANCED) {
      return commonTail(
          primaryObject + "目前变化不大，同时还要照顾" + secondaryObject + "。",
          "先维持现状：",
          "这样能确认当前状态能否保持，也能及时发现" + secondaryObject + "的变化。",
          "保持稳定的信号是：",
          "维持安排后",
          primary,
          secondary);
    }
    return commonTail(
        "目前可以优先改善" + primaryObject + "，但还要兼顾" + secondaryObject + "。",
        "先推进一小步：",
        "这样能确认" + primaryObject + "是否真的改善，同时避免" + secondaryObject + "受影响。",
        "推进有效的信号是：",
        "开始推进后",
        primary,
        secondary);
  }

  private Copy commonTail(
      String problem,
      String actionLead,
      String expectedChange,
      String signalLead,
      String timingLead,
      OverallTopicSnapshot primary,
      OverallTopicSnapshot secondary) {
    String primaryObject = focusObject(primary);
    String secondaryObject = focusObject(secondary);
    return new Copy(
        problem,
        actionLead + action(primary) + "；同时核对" + secondaryObject + "是否变化。",
        expectedChange,
        timingLead + "每周分别核对" + primaryObject + "、" + secondaryObject + "；四周后判断。",
        signalLead + success(primary) + "，而且" + protectedSignal(secondary) + "。",
        "如果连续两周" + primaryObject + "没有改善，或" + secondaryObject + "变差，就缩小当前安排。",
        "如果无效，就把这次行动缩小一半，并先确保" + protectedSignal(secondary) + "。"
    );
  }

  private String action(OverallTopicSnapshot snapshot) {
    String key = snapshot.focusKey();
    if (key.startsWith("wealth.stable_income")) return "列出近四周每笔进账，并标明是否按时";
    if (key.startsWith("wealth.skill_income")) return "把一项新增投入和对应进账单独记账";
    if (key.startsWith("wealth.project_income")) return "列出预计金额、约定日期和实际到账日期";
    if (key.startsWith("wealth.cooperation_income")) return "写清共同用钱的金额、用途和各自责任";
    if (key.startsWith("wealth.retention")) return "记录必要开销后的实际余钱";
    if (key.startsWith("career.role_transition")) return "分别列清新增和减少的责任";
    if (key.startsWith("career.coordination_window")) return "写清需他人配合的事项、期限和回应";
    if (key.startsWith("career.responsibility_upgrade")) return "逐项对照新增责任和对应条件";
    if (key.startsWith("career.visibility")) return "完成一项可展示、可核对的成果";
    if (key.startsWith("career.preparation")) return "选一项短板，完成一次练习或验证";
    if (key.startsWith("career.resource_delivery")) return "记录每项投入对应的实际结果";
    if (key.startsWith("career.overextension")) return "暂停一项次要任务，只保留最重要的一项";
    if (key.startsWith("relationship.connection")) return "主动联系一次，并记录回应能否持续";
    if (key.startsWith("relationship.response")) return "提出一个具体问题，并记录是否得到明确答复";
    if (key.startsWith("relationship.daily_cooperation")) return "约定一件日常事项的做法和完成时间";
    if (key.startsWith("relationship.boundaries")) return "说清一条不能继续退让的边界";
    if (key.startsWith("relationship.stability")) return "约定一次固定沟通，并记录是否如期发生";
    return switch (snapshot.topic()) {
      case RHYTHM -> "删掉一件非必要安排，并固定一段休息时间";
      case CAREER -> "完成一项可核对的成果";
      case WEALTH -> "记录实际收支和手中余钱";
      case RELATIONSHIP -> "提出一个具体问题，并记录实际回应";
    };
  }

  private String success(OverallTopicSnapshot snapshot) {
    String key = snapshot.focusKey();
    if (key.startsWith("wealth.stable_income")) return "四周进账记录完整，延期次数没有增加";
    if (key.startsWith("wealth.skill_income")) return "新增投入与实际进账能够一一对应";
    if (key.startsWith("wealth.project_income")) return "预计金额与实际到账的差异缩小";
    if (key.startsWith("wealth.cooperation_income")) return "共同用钱的金额和责任都有记录";
    if (key.startsWith("wealth.retention")) return "必要开销后仍有可确认的余钱";
    if (key.startsWith("career.coordination_window")) return "需要配合的事项得到明确回应";
    if (key.startsWith("career.responsibility_upgrade")) return "新增责任与对应条件能够逐项对应";
    if (key.startsWith("career.preparation")) return "练习或验证留下可核对结果";
    if (key.startsWith("career.overextension")) return "同时承担的事情已经减少";
    if (key.startsWith("relationship.connection")) return "重要联系连续四周没有中断";
    if (key.startsWith("relationship.response")) return "提出的问题得到明确答复";
    if (key.startsWith("relationship.daily_cooperation")) return "约定的日常事项有人按时落实";
    if (key.startsWith("relationship.boundaries")) return "说清的边界没有被反复打破";
    if (key.startsWith("relationship.stability")) return "固定沟通连续四周如期发生";
    return switch (snapshot.topic()) {
      case RHYTHM -> "非必要安排减少，固定休息时间保留四周";
      case CAREER -> "最重要的事情留下可核对成果";
      case WEALTH -> "实际收支都有记录，手中余钱没有减少";
      case RELATIONSHIP -> "重要问题得到明确回应";
    };
  }

  private String protectedSignal(OverallTopicSnapshot snapshot) {
    String key = snapshot.focusKey();
    if (key.startsWith("wealth.project_income")) return "预计金额和到账日期仍有清楚记录";
    if (key.startsWith("wealth.stable_income")) return "每笔实际进账仍有清楚记录";
    if (key.startsWith("wealth.skill_income")) return "新增投入和对应进账仍有清楚记录";
    if (key.startsWith("wealth.cooperation_income")) return "共同用钱的金额和责任仍说得清";
    if (key.startsWith("wealth.retention")) return "必要开销后的余钱没有减少";
    if (key.startsWith("relationship.connection")) return "重要联系没有中断";
    if (key.startsWith("relationship.response")) return "重要问题仍能得到回应";
    if (key.startsWith("relationship.daily_cooperation")) return "约定的日常事项仍有人落实";
    if (key.startsWith("relationship.boundaries")) return "已经说清的边界没有被反复打破";
    if (key.startsWith("relationship.stability")) return "固定沟通仍能如期发生";
    if (key.startsWith("career.role_transition")) return "新增和减少的责任仍说得清";
    if (key.startsWith("career.coordination_window")) return "需要配合的事项仍得到回应";
    if (key.startsWith("career.responsibility_upgrade")) return "新增责任和对应条件仍能逐项对应";
    if (key.startsWith("career.visibility")) return "可核对的成果仍能按时完成";
    if (key.startsWith("career.preparation")) return "练习或验证仍有清楚结果";
    if (key.startsWith("career.resource_delivery")) return "投入与实际结果仍能对应";
    if (key.startsWith("career.overextension")) return "同时承担的事情没有增加";
    return switch (snapshot.topic()) {
      case RHYTHM -> "固定休息时间没有继续减少";
      case CAREER -> "最重要的事情仍能按时完成";
      case WEALTH -> "必要开销后的余钱没有减少";
      case RELATIONSHIP -> "重要问题仍能得到回应";
    };
  }

  String focusObject(OverallTopicSnapshot snapshot) {
    String focusKey = snapshot.focusKey();
    if (focusKey.startsWith("wealth.stable_income")) return "实际进账的稳定程度";
    if (focusKey.startsWith("wealth.skill_income")) return "新增投入带来的实际进账";
    if (focusKey.startsWith("wealth.project_income")) return "预计进账与实际到账的差异";
    if (focusKey.startsWith("wealth.cooperation_income")) return "共同用钱的金额和责任";
    if (focusKey.startsWith("wealth.retention")) return "必要开销后的实际余钱";
    if (focusKey.startsWith("career.role_transition")) return "责任和安排的变化";
    if (focusKey.startsWith("career.coordination_window")) return "需要他人配合的事情";
    if (focusKey.startsWith("career.responsibility_upgrade")) return "新增责任和对应条件";
    if (focusKey.startsWith("career.visibility")) return "目前最需要证明的事情";
    if (focusKey.startsWith("career.preparation")) return "下一步最需要补齐的能力";
    if (focusKey.startsWith("career.resource_delivery")) return "投入带来的实际结果";
    if (focusKey.startsWith("career.overextension")) return "同时承担的事情";
    if (focusKey.startsWith("relationship.connection")) return "重要联系的持续情况";
    if (focusKey.startsWith("relationship.response")) return "重要问题的实际回应";
    if (focusKey.startsWith("relationship.daily_cooperation")) return "日常安排的共同落实";
    if (focusKey.startsWith("relationship.boundaries")) return "彼此边界的尊重情况";
    if (focusKey.startsWith("relationship.stability")) return "一段重要关系的稳定程度";
    return object(snapshot.topic());
  }

  private String object(Topic topic) {
    return switch (topic) {
      case RHYTHM -> "每天可用的时间和精力";
      case CAREER -> "最重要的一项责任";
      case WEALTH -> "实际收支和手中余钱";
      case RELATIONSHIP -> "一段重要关系里的真实回应";
    };
  }

  record Copy(
      String problem,
      String action,
      String expectedChange,
      String checkTiming,
      String successSignal,
      String adjustmentCondition,
      String fallbackAction) {}
}
