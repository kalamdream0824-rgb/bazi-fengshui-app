package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import java.util.List;
import java.util.stream.Collectors;

/** Writes report-level summaries from the same annual decisions without adding new claims. */
final class WealthReportSummaryWriter {

  String path(WealthAssessment year, Decision decision) {
    String text = switch (decision.stance()) {
      case "quiet" -> quiet(decision.path());
      case "restricted" -> restricted(decision.path());
      case "mixed" -> mixed(year, decision);
      case "supportive" -> supportive(decision.path(), decision.strength());
      default -> throw new IllegalArgumentException("unknown wealth stance: " + decision.stance());
    };
    return checked(text);
  }

  String risk(Decision decision) {
    return checked(switch (decision.path()) {
      case "stable_income" -> "资金风险在于进账中断后，长期支出仍按原计划增加。";
      case "skill_income" -> "资金风险在于投入的时间或花费增加，实际进账却没有同步提高。";
      case "project_income" -> "资金风险在于预计的钱没有按时到账，却提前安排了支出。";
      case "cooperation_income" -> "资金风险在于共同用钱的责任没有说清，最后增加额外开销。";
      case "retention" -> "资金风险在于进账看似增加，最后留下的钱却没有变多。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + decision.path());
    });
  }

  String closing(List<WealthAssessment> years) {
    List<WealthAssessment> limited = years.stream()
        .filter(year -> decision(year, "retention").limitationWeight() > 0)
        .toList();
    if (!limited.isEmpty()) {
      String labels = limited.stream().map(year -> String.valueOf(year.year()))
          .collect(Collectors.joining("、"));
      return checked(labels + "年，进账增加后，实际结余仍可能没有同步提高。"
          + "比较三年时，分别核对已经到账的钱、实际支出和最后留下的钱。");
    }
    if (years.stream().allMatch(year -> decision(year, "retention").stance().equals("quiet"))) {
      return checked("三年结余需要用实际记录核对。分别看已经到账的钱、实际支出和最后留下的钱。");
    }
    return checked("三年里，进账增加和结余增加不能当成同一件事。"
        + "先看钱是否实际到账，再看扣除支出后留下多少。");
  }

  String readingNote(List<WealthAssessment> years) {
    String profile = years.get(0).facts().stream()
        .filter(fact -> fact.code().equals("natal.weak_support_profile"))
        .map(WealthAssessment.Fact::value)
        .findFirst()
        .orElse("");
    return checked("这份报告使用传统命理规则比较资金变化，不推断你的职业和收入来源，"
        + "也不代替真实的收支记录或投资判断。"
        + "文中的核对问题只帮助你对照自己的情况，不代表相应事件一定发生。"
        + WealthPlainCopyV3.weakSupportNote(profile));
  }

  private String supportive(String path, String strength) {
    String signal = switch (path) {
      case "stable_income" -> "进账保持连续";
      case "skill_income" -> "增加投入后，实际进账提高";
      case "project_income" -> "预计的钱按时到账";
      case "cooperation_income" -> "共同用钱的责任和开销容易说清";
      case "retention" -> "实际结余增加";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
    return switch (strength) {
      case "pronounced" -> signal + "的迹象较明显。";
      case "supported" -> signal + "的迹象存在，但不算突出。";
      case "limited" -> signal + "的迹象较弱，安排支出仍要保守。";
      default -> throw new IllegalArgumentException("unknown wealth strength: " + strength);
    };
  }

  private String mixed(WealthAssessment year, Decision decision) {
    return switch (decision.path()) {
      case "stable_income" -> "既有持续进账的机会，也有中断或波动的可能。";
      case "skill_income" -> "投入增加后，实际进账可能提高，也可能没有同步变化。";
      case "project_income" -> "有些预计的钱可能按时到账，也可能出现延期或金额变化。";
      case "cooperation_income" -> "共同用钱的责任有机会说清，也可能增加额外开销。";
      case "retention" -> "实际结余可能增加，也可能被" + concernList(year, decision) + "压低。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + decision.path());
    };
  }

  private String restricted(String path) {
    return switch (path) {
      case "stable_income" -> "进账更容易出现中断或波动，长期支出不要提前增加。";
      case "skill_income" -> "投入增加后，实际进账未必同步提高。";
      case "project_income" -> "预计的钱更容易延后到账，金额也可能与预期不同。";
      case "cooperation_income" -> "钱与他人有关时，额外责任和开销更容易增加。";
      case "retention" -> "进账增加后，实际结余仍可能没有提高。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String quiet(String path) {
    return switch (path) {
      case "stable_income" -> "不单独判断进账能否持续。";
      case "skill_income" -> "不单独判断增加投入能否提高进账。";
      case "project_income" -> "不单独判断预计的钱能否按时到账。";
      case "cooperation_income" -> "不单独判断共同用钱的责任和开销会怎样变化。";
      case "retention" -> "不预先判断最后能留下多少钱，以实际收支记录为准。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String concernList(WealthAssessment year, Decision decision) {
    List<String> concerns = WealthPlainCopyV3.causes(year, decision).stream().map(cause -> switch (cause) {
      case "shared_responsibility" -> "共同用钱的金额";
      case "extra_spending" -> "新增花费";
      case "timing_changes" -> "到账变化带来的开销";
      case "money_responsibility" -> "与他人有关的开销";
      case "receipt_expectation" -> "尚未到账的钱";
      default -> throw new IllegalArgumentException("unknown wealth copy cause: " + cause);
    }).limit(2).toList();
    return concerns.isEmpty() ? "实际支出" : String.join("、", concerns);
  }

  private Decision decision(WealthAssessment year, String path) {
    return year.decisions().stream().filter(item -> item.path().equals(path))
        .findFirst().orElseThrow();
  }

  private String checked(String text) {
    new WealthChineseCopyPolicy().validateBodyParagraph(text);
    return text;
  }
}
