package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthAnnualHeadlinePlanner.Headline;

/** Writes one observable action loop from the selected annual money-state decision. */
final class WealthActionGuideWriter {

  Copy write(Decision decision, Headline headline) {
    if (!decision.path().equals(headline.pathKey())) {
      throw new IllegalArgumentException("wealth action guide must follow the selected headline path");
    }
    var entry = WealthHeadlineVocabulary.entry(headline.themeKey());
    String object = entry.objectText();
    String verification = entry.sentenceSpec().verification();
    return new Copy(
        checked(problem(decision, object)),
        checked(action(decision.path(), verification)),
        checked(expectedChange(decision.path(), object)),
        checked("每周核对以下情况：" + object + "；满四周后，再用真实记录决定是否调整。"),
        checked(successSignal(decision.path(), object)),
        checked(adjustmentCondition(decision.path(), object)),
        checked(fallbackAction(decision.path(), object)));
  }

  private String problem(Decision decision, String object) {
    String caution = switch (decision.path()) {
      case "stable_income" -> "不能只凭判断增加长期支出";
      case "skill_income" -> "不能只看自己是否更忙";
      case "project_income" -> "不能把未到账的钱当成可用金额";
      case "cooperation_income" -> "不能等支出发生后再分责任";
      case "retention" -> "不能只看收到多少钱";
      default -> throw new IllegalArgumentException("unknown wealth path: " + decision.path());
    };
    String currentState = switch (decision.stance()) {
      case "supportive" -> "现在已有改善迹象，但仍要用实际记录确认";
      case "mixed" -> "现在有好转也有反复，要把两种情况都记下来";
      case "restricted" -> "现在压力更明显，要先避免问题继续扩大";
      case "quiet" -> "现在变化不突出，要先用真实记录建立基线";
      default -> throw new IllegalArgumentException("unknown wealth stance: " + decision.stance());
    };
    return "今年要先核对“" + object + "”；" + caution + "，" + currentState + "。";
  }

  private String action(String path, String verification) {
    return switch (path) {
      case "stable_income" -> "连续四周，" + verification
          + "，并记下每次实际进账的金额；记录完成前，不增加长期支出。";
      case "skill_income" -> "连续四周，" + verification
          + "，同时记下为此增加的时间和花费；记录完成前，不继续增加投入。";
      case "project_income" -> "从现在起，" + verification
          + "，把预计日期、实际日期和实际金额分开记录；不提前使用尚未到账的钱。";
      case "cooperation_income" -> "每次涉及共同用钱前，" + verification
          + "，再写清金额、用途和各自承担多少；说不清时不追加资金。";
      case "retention" -> "连续四周，" + verification
          + "，并分别记录实际进账、必要支出、临时支出和最后结余。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String expectedChange(String path, String object) {
    return switch (path) {
      case "stable_income" -> "这样可以看出" + object + "是否稳定，以及现有进账能否覆盖长期支出。";
      case "skill_income" -> "这样可以看出" + object + "是否对应更多实际进账，还是只增加了投入。";
      case "project_income" -> "这样可以看出" + object + "的偏差主要来自延后、金额变化还是相关花费。";
      case "cooperation_income" -> "这样可以看出" + object + "是否清楚，以及哪些额外开销来自临时决定。";
      case "retention" -> "这样可以看出" + object + "是否改善，以及哪类支出最影响最后结余。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String successSignal(String path, String object) {
    return switch (path) {
      case "stable_income" -> "有效的信号是：四周内没有出现无法覆盖日常开支的进账中断，并且能说清"
          + object + "。";
      case "skill_income" -> "有效的信号是：扣除新增时间和花费后，实际结余没有下降，并且能说清"
          + object + "。";
      case "project_income" -> "有效的信号是：未到账的钱没有被提前花掉，并且能说清" + object + "与实际结果的差距。";
      case "cooperation_income" -> "有效的信号是：临时增加的共同开销减少，并且能说清" + object + "。";
      case "retention" -> "有效的信号是：四周后的实际结余有所增加，或者能明确指出" + object + "受哪类支出影响。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String adjustmentCondition(String path, String object) {
    return switch (path) {
      case "stable_income" -> "四周后，如果" + object + "仍然经常变化，或者现有进账无法覆盖日常开支，就要缩减长期支出。";
      case "skill_income" -> "四周后，如果" + object + "没有改善，或者投入增加但结余下降，就要停止继续增加投入。";
      case "project_income" -> "四周后，如果" + object + "仍有明显差距，或者未到账金额继续增加，就要改用实际到账安排支出。";
      case "cooperation_income" -> "四周后，如果" + object + "仍说不清，或者额外开销继续增加，就要暂停新的共同资金安排。";
      case "retention" -> "四周后，如果" + object + "没有改善，或者临时支出继续增加，就要重新设置开销上限。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String fallbackAction(String path, String object) {
    String fallback = switch (path) {
      case "stable_income" -> "先暂停一项可以延后的长期支出，把已到账的钱优先留给日常开支";
      case "skill_income" -> "先暂停一项新增投入，只保留花费较少且能记录结果的做法";
      case "project_income" -> "先停止用未到账的钱安排支出，直到实际到账重新稳定";
      case "cooperation_income" -> "在金额、用途和各自承担部分明确前，先不增加共同资金安排";
      case "retention" -> "先减少一项可以延后的花费，再按最近四周真实支出重设上限";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
    return fallback + "；调整前重点核对：" + object + "。";
  }

  private String checked(String text) {
    new WealthChineseCopyPolicy().validateBodyParagraph(text);
    return text;
  }

  record Copy(String problem, String action, String expectedChange, String checkTiming,
      String successSignal, String adjustmentCondition, String fallbackAction) {}
}
