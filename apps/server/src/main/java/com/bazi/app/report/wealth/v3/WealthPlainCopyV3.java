package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.WealthPath;
import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Closed, versioned plain-language vocabulary. No user data, random wording or report-position branches. */
final class WealthPlainCopyV3 {
  static final String VERSION = "wealth-plain-v3.3";

  record Words(String object, String meaning, String limitation, String observation, String action) {}
  private static final Map<String, Words> WORDS = Map.of(
      "stable_income", new Words("固定工资、长期客户这类持续收入", "它看重持续拿到钱，而不是偶尔多一笔进账。",
          "固定收入也有需要留意的限制，不宜把新增支出全押在这一项上。",
          "若有固定收入，可留意到账是否准时，以及是否过于依赖一个客户。",
          "把收入金额、到账时间和可能中断的情况列清楚。"),
      "skill_income", new Words("靠手艺或服务获得的收入", "它看重别人愿意为什么本事付钱，而不是单纯忙了多久。",
          "靠本事赚钱也有限制，多接活之前要看增加的时间是否值得。",
          "可以留意同一种服务是否有人再次付费，而不只是口头称赞。",
          "选一项拿手服务，写清收费、所需时间和完成内容。"),
      "project_income", new Words("按单、按次结算的额外收入", "它更偏向一笔笔结算的钱，不宜直接当作每年都有的固定收入。",
          "额外收入需要连同成本和收款条件一起看，不能只看报价。",
          "若有额外项目，可留意扣除成本和垫付款后，每笔最终留下多少。",
          "接项目前先算成本，再写清付款时间和追加要求的价格。"),
      "cooperation_income", new Words("与别人一起做事带来的收入", "一起接单、介绍客户是这类收入的例子，不代表已经有人找你合作。",
          "合作收入要连同分账和共同开销一起看，不能只看大家赚了多少。",
          "若有合作，可留意分账是否说得清，以及额外开销由谁承担。",
          "合作前写清谁出钱、谁做事、钱怎么分。"),
      "retention", new Words("最后能留下的钱", "进账和结余是两件事，新增收入还要扣掉为此增加的花费。",
          "留钱方面存在限制，新增花费需要与收入一起衡量。",
          "可以留意收入增加时，支出是否也跟着增加。",
          "给日常必需开支和新增投入分别设一个上限。"));

  static Words words(String path) { return WORDS.get(path); }
  static String label(String path) {
    if (path.equals("retention")) return "结余";
    return Arrays.stream(WealthPath.values()).filter(p -> p.code().equals(path)).findFirst().orElseThrow().label();
  }

  static List<String> causes(WealthAssessment year, Decision d) {
    Set<String> result = new LinkedHashSet<>();
    for (var e : year.evidence()) {
      if (!d.limitingEvidenceIds().contains(e.id())) continue;
      String key = e.factKey();
      if (key.equals("natal.combination.peer_wealth")) result.add("shared_money");
      else if (key.equals("natal.balance")) result.add("extra_input");
      else if (key.matches("annual\\.branch\\.(clash|harm|punishment)\\..+")) result.add("changed_arrangements");
      else if (key.equals("dayun.stem.ten_god")) result.add("shared_expenses");
      else if (key.equals("annual.stem.ten_god")) result.add("project_terms");
    }
    return result.stream().sorted().toList();
  }

  static String limitation(WealthAssessment year, Decision d) {
    List<String> causes = causes(year, d);
    if (causes.isEmpty()) return words(d.path()).limitation();
    return String.join("", causes.stream().map(c -> switch (c) {
      case "shared_money" -> "分账和共同开销，是需要先看清的部分。";
      case "extra_input" -> "进账机会不等于结余增加，额外投入要留出余地。";
      case "changed_arrangements" -> "约定反复或临时增加开销，是需要防备的情况。";
      case "shared_expenses" -> "与人有关的支出要留出余地，别把进账全当作可用的钱。";
      case "project_terms" -> "项目要求和付款条件更需要看清，不能只看报价。";
      default -> throw new IllegalArgumentException("unknown wealth copy cause");
    }).toList());
  }

  static String pathText(WealthAssessment year, Decision d) {
    if (d.path().equals("retention")) return switch (d.stance()) {
      case "quiet" -> "本次分析不对结余增减作具体判断。";
      case "restricted" -> "留钱方面需要多留余地。" + limitation(year, d);
      case "mixed" -> "有积攒结余的空间，但支出也要一起看。" + limitation(year, d);
      default -> d.strength().equals("pronounced") ? "积攒结余是较值得关注的方向。"
          : d.strength().equals("supported") ? "积攒结余可以作为一个关注点。" : "结余方面的倾向较轻，不宜预先提高花费。";
    };
    String object = words(d.path()).object();
    return switch (d.stance()) {
      case "quiet" -> label(d.path()) + "不列为本次分析的具体判断重点。";
      case "restricted" -> limitation(year, d);
      case "mixed" -> object + "有可以考虑的部分，但限制也要一起看。" + limitation(year, d);
      case "supportive" -> switch (d.strength()) {
        case "pronounced" -> object + "是较值得关注的方向。";
        case "supported" -> object + "可以作为一个方向。";
        case "limited" -> object + "只有较轻的倾向，不宜直接当作增收重点。";
        default -> throw new IllegalArgumentException("unlicensed supportive wealth copy");
      };
      default -> throw new IllegalArgumentException("unknown wealth stance");
    };
  }

  static String compact(WealthAssessment year, Decision d) {
    String name = label(d.path());
    if (d.stance().equals("quiet")) return name + "不作具体判断。";
    if (d.path().equals("retention") && d.stance().equals("supportive")) return pathText(year, d);
    if (d.stance().equals("supportive")) return name + switch (d.strength()) {
      case "pronounced" -> "更值得关注。";
      case "supported" -> "可以作为一个方向。";
      default -> "只有较轻的倾向，不宜作为增收重点。";
    };
    String concerns = concerns(year, d);
    if (d.path().equals("retention")) return d.stance().equals("mixed")
        ? "有积攒结余的空间，也要留意" + concerns + "。" : "要留意" + concerns + "对结余的影响。";
    return d.stance().equals("mixed") ? name + "有一定空间，也要留意" + concerns + "。" : caution(year, d);
  }

  static String caution(WealthAssessment year, Decision d) {
    String context = switch (d.path()) {
      case "stable_income" -> "安排固定收入时";
      case "skill_income" -> "靠本事赚钱时";
      case "project_income" -> "接额外项目时";
      case "cooperation_income" -> "与人合作赚钱时";
      case "retention" -> "想把钱留下来";
      default -> throw new IllegalArgumentException("unknown wealth caution");
    };
    return context + "，要留意" + concerns(year, d) + "。";
  }

  static String concerns(WealthAssessment year, Decision d) {
    String concerns = String.join("、", causes(year, d).stream().map(c -> switch (c) {
      case "shared_money" -> "分账";
      case "extra_input" -> "新增投入";
      case "changed_arrangements" -> "约定变化";
      case "shared_expenses" -> "共同开销";
      case "project_terms" -> "付款条件";
      default -> throw new IllegalArgumentException("unknown wealth copy cause");
    }).toList());
    if (concerns.isEmpty()) concerns = switch (d.path()) {
      case "stable_income" -> "收入中断的可能";
      case "skill_income" -> "时间投入";
      case "project_income" -> "成本和收款";
      case "cooperation_income" -> "分账和开销";
      default -> "收入与支出";
    };
    return concerns;
  }

  static String route(String path) {
    return switch (path) {
      case "stable_income" -> "先看固定收入能覆盖多少日常开支，再决定额外项目能投入多少。";
      case "skill_income" -> "先比较哪项本事能带来重复付费，再决定要不要增加服务种类。";
      case "project_income" -> "把报价、实际收款和最后结余分开记录，别只看接单金额。";
      case "cooperation_income" -> "一起赚钱前，先把分账约定写下来，别只靠口头承诺。";
      case "retention" -> "给临时开销留一笔余钱，收入增加后也不要马上提高固定花费。";
      default -> throw new IllegalArgumentException("unknown wealth route");
    };
  }
}
