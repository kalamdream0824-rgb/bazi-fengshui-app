package com.bazi.app.report.overall.v3;

import com.bazi.app.report.overall.v3.OverallTopicSnapshot.Topic;

/** Customer-visible copy selected only from calculated cross-topic meaning. */
final class OverallV3CopyCatalog {

  Copy find(OverallAnnualDecision decision) {
    String primaryFocus = focusObject(decision.primary());
    String secondaryFocus = focusObject(decision.secondary());
    return switch (decision.conflictKey()) {
      case "capacity_before_career_expansion" -> new Copy(
          "眼前有事情值得推进，但" + primaryFocus + "已经偏紧。",
          "先延后一件不着急的事，再围绕" + secondaryFocus + "写清本周能完成的一个成果。",
          "这样能为成果留下稳定时间，也能避免休息继续被占用。",
          "开始执行后，每周核对一次；四周后统一判断。",
          "有效的信号是：" + secondaryFocus + "按时完成，同时每周仍有固定休息时间。",
          "如果连续两周仍靠减少休息才能完成，就要缩小这件事的范围。",
          "先保留最重要的一步，其余安排顺延到时间恢复以后。");
      case "cash_buffer_before_growth" -> new Copy(
          "有事情可以继续推进，但要先核对" + primaryFocus + "。",
          "先把" + primaryFocus + "算清，再把" + secondaryFocus + "限定为一件四周内可完成的事。",
          "这样能判断" + primaryFocus + "是否改善，保住实际余钱，也能检验投入能否形成成果。",
          "从执行当天起，每周记一次收支；满四周后统一核对。",
          "有效的信号是：" + primaryFocus + "可以说清，" + secondaryFocus + "也有实际结果。",
          "如果两次核对都发现余钱减少，就不要继续增加投入。",
          "暂停新增花费，先用已有条件完成一个较小的结果。");
      case "relationship_stability_before_growth" -> new Copy(
          "一件重要事情正在向前走，但" + primaryFocus + "还有问题没有说清。",
          "先用一次具体沟通说清彼此需要，再围绕" + secondaryFocus + "完成一个成果。",
          "这样能让重要问题得到明确回应，也能让成果继续推进。",
          "沟通后每周看一次双方行动；四周后统一确认。",
          "有效的信号是：" + primaryFocus + "得到清楚答复，约好的事情也有人落实。",
          "如果连续两次沟通都没有回应，就要减少新的共同安排。",
          "先守住自己的时间和边界，只推进不依赖他人的部分。");
      case "career_with_wealth_watch" -> new Copy(
          "当前最值得投入的是" + primaryFocus + "，同时不能忽略" + secondaryFocus + "。",
          "先完成一个与" + primaryFocus + "有关的成果，并把为此发生的花费单独记下。",
          "这样能看清成果是否真的推进，也能知道实际余钱有没有减少。",
          "开始以后每周核对一次进度和收支；四周后统一判断。",
          "有效的信号是：" + primaryFocus + "留下成果，实际余钱也没有持续下降。",
          "如果成果没有进展且花费连续增加，就要停止追加投入。",
          "改为完成一个成本更低的小结果，再决定是否继续。");
      default -> generic(decision.primary(), decision.secondary());
    };
  }

  private Copy generic(OverallTopicSnapshot primary, OverallTopicSnapshot secondary) {
    String primaryObject = focusObject(primary);
    String primaryResult = result(primary.topic());
    String secondaryObject = focusObject(secondary);
    String secondaryResult = result(secondary.topic());
    return new Copy(
        "现在要优先处理" + primaryObject + "，同时照顾" + secondaryObject + "。",
        "先为" + primaryObject + "完成一件可以核对的小事，再记录" + secondaryObject + "有没有受到影响。",
        "这样能让" + primaryResult + "，也能让" + secondaryResult + "。",
        "开始执行后，每周核对一次；四周后统一判断。",
        "有效的信号是：主要事情有实际进展，另一件重要事情也没有变差。",
        "如果连续两周只有投入却没有变化，就要减少当前安排。",
        "先缩小行动范围，只保留四周内可以确认结果的一步。"
    );
  }

  private String focusObject(OverallTopicSnapshot snapshot) {
    String focusKey = snapshot.focusKey();
    if (focusKey.startsWith("wealth.stable_income")) return "实际进账是否稳定";
    if (focusKey.startsWith("wealth.skill_income")) return "新增投入和实际进账";
    if (focusKey.startsWith("wealth.project_income")) return "预计进账和实际到账";
    if (focusKey.startsWith("wealth.cooperation_income")) return "共同用钱的金额和责任";
    if (focusKey.startsWith("wealth.retention")) return "必要开销后的实际余钱";
    if (focusKey.startsWith("career.role_transition")) return "责任和安排的变化";
    if (focusKey.startsWith("career.coordination_window")) return "需要他人配合的事情";
    if (focusKey.startsWith("career.responsibility_upgrade")) return "新增责任和对应条件";
    if (focusKey.startsWith("career.visibility")) return "目前最需要证明的事情";
    if (focusKey.startsWith("career.preparation")) return "下一步最需要补齐的能力";
    if (focusKey.startsWith("career.resource_delivery")) return "投入能否换来实际结果";
    if (focusKey.startsWith("career.overextension")) return "同时承担的事情是否过多";
    if (focusKey.startsWith("relationship.connection")) return "重要联系能否持续";
    if (focusKey.startsWith("relationship.response")) return "重要问题能否得到回应";
    if (focusKey.startsWith("relationship.daily_cooperation")) return "日常安排能否共同落实";
    if (focusKey.startsWith("relationship.boundaries")) return "彼此边界能否得到尊重";
    if (focusKey.startsWith("relationship.stability")) return "一段重要关系能否稳定";
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

  private String result(Topic topic) {
    return switch (topic) {
      case RHYTHM -> "重点事项得到稳定时间";
      case CAREER -> "投入逐步形成可以确认的成果";
      case WEALTH -> "必要开销得到保障，实际余钱不再减少";
      case RELATIONSHIP -> "重要问题得到明确回应";
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
