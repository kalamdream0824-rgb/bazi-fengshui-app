package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Closed, versioned plain-language vocabulary. No user data, random wording or report-position branches. */
final class WealthPlainCopyV3 {
  static final String VERSION = "wealth-plain-v3.4";

  record Words(String object, String meaning, String limitation, String observation, String action) {}
  private static final Map<String, Words> WORDS = Map.of(
      "stable_income", new Words("进账的持续性", "它看的是钱能否持续进入，不判断这笔钱来自哪种工作。",
          "进账持续性也可能发生变化，安排长期支出前要留出余地。",
          "可以留意各次进账之间是否规律，以及是否出现明显中断。",
          "记录每次进账的日期和金额，观察持续性。"),
      "skill_income", new Words("投入与进账是否相称", "它看的是增加投入以后，实际进账是否同步变化。",
          "投入增加未必带来相同幅度的进账，要把时间和花费一起算进去。",
          "可以留意自己更忙以后，实际进账是否同步增加。",
          "把投入的时间、花费和实际进账放在一起核对。"),
      "project_income", new Words("到账的节奏", "它看的是钱何时到账以及是否延后，不判断收入形式。",
          "预期中的钱不等于已经到账，提前支出时要留出余地。",
          "可以留意原本预期的进账是否按时出现，以及延后多久。",
          "把预计到账和实际到账分开记录，不提前花尚未收到的钱。"),
      "cooperation_income", new Words("涉及他人时的资金责任", "它看的是钱与他人有关时，责任和支出边界是否清楚。",
          "涉及他人的钱容易增加额外责任，要先分清各自承担什么。",
          "如果有共同用钱的情况，可以留意责任和开销是否清楚。",
          "涉及共同用钱时，先写清金额、用途和各自承担的部分。"),
      "retention", new Words("最后能留下的钱", "进账和结余是两件事，新增收入还要扣掉为此增加的花费。",
          "留钱方面存在限制，新增花费需要与收入一起衡量。",
          "可以留意收入增加时，支出是否也跟着增加。",
          "给日常必需开支和新增投入分别设一个上限。"));

  static Words words(String path) { return WORDS.get(path); }
  static String label(String path) {
    return switch (path) {
      case "stable_income" -> "进账稳定";
      case "skill_income" -> "投入回报";
      case "project_income" -> "到账节奏";
      case "cooperation_income" -> "资金责任";
      case "retention" -> "收支结余";
      default -> throw new IllegalArgumentException("unknown wealth path");
    };
  }

  static String retrospectiveQuestion(String subject) {
    return switch (subject) {
      case "stable_income" -> "进账是否能够持续";
      case "skill_income" -> "时间和花费是否换来相称的进账";
      case "project_income" -> "预计进账与实际到账是否一致";
      case "cooperation_income" -> "与他人有关的钱是否增加额外责任";
      case "retention" -> "进账以后实际留下的钱有没有增加";
      default -> throw new IllegalArgumentException("unknown wealth retrospective subject: " + subject);
    };
  }

  static String retrospectiveObject(String subject) {
    return switch (subject) {
      case "stable_income" -> "进账持续性";
      case "skill_income" -> "投入和回报";
      case "project_income" -> "到账时间";
      case "cooperation_income" -> "共同用钱时的责任";
      case "retention" -> "实际结余";
      default -> throw new IllegalArgumentException("unknown wealth retrospective subject: " + subject);
    };
  }

  static String retrospectiveDirection(String direction, String strength) {
    return switch (direction) {
      case "supportive" -> switch (strength) {
        case "pronounced" -> "这方面的变化比较明显。";
        case "supported" -> "这方面有一定变化。";
        case "limited" -> "这方面的变化迹象较弱。";
        default -> throw new IllegalArgumentException("unknown retrospective strength: " + strength);
      };
      case "mixed" -> "这方面可能有进展，也容易受到限制。";
      case "restricted" -> "这方面受到的限制更明显。";
      default -> throw new IllegalArgumentException("unknown retrospective direction: " + direction);
    };
  }

  static String retrospectiveAngle(String angle) {
    return switch (angle) {
      case "annual_stem" -> "重点核对这一年直接出现的收支变化。";
      case "annual_harmony" -> "重点核对它是否受到他人或原有安排牵动。";
      case "annual_clash" -> "重点核对是否出现突然且明显的变动。";
      case "annual_harm" -> "重点核对是否有零散且不易察觉的损耗。";
      case "annual_punishment" -> "重点核对同类收支问题是否反复出现。";
      case "annual_context" -> "重点核对这一年的收支条件是否改变。";
      case "dayun_context" -> "还要结合较长一段时间的收支状态核对。";
      case "natal_output_wealth" -> "还要核对长期投入能否转成实际进账。";
      case "natal_wealth_capacity" -> "还要核对进账增加后能否承受相应开销。";
      case "natal_shared_responsibility" -> "还要核对共同用钱是否影响最后结余。";
      case "natal_balance" -> "还要核对进账增加时，额外开销是否同步增加。";
      case "natal_combination" -> "还要核对原有收支条件是否相互牵动。";
      case "natal_structure" -> "还要和自己一贯的收支方式对照。";
      default -> throw new IllegalArgumentException("unknown retrospective angle: " + angle);
    };
  }

  static String retrospectiveAngleQuestion(String angle) {
    return switch (angle) {
      case "annual_stem" -> "当年的直接收支变化是否和这项结果一致";
      case "annual_harmony" -> "他人或原有安排是否带来额外影响";
      case "annual_clash" -> "是否出现突然且明显的收支变动";
      case "annual_harm" -> "是否有零散且不易察觉的损耗";
      case "annual_punishment" -> "同类收支问题是否反复出现";
      case "annual_context" -> "当年的收支条件是否发生改变";
      case "dayun_context" -> "较长一段时间的收支状态是否持续影响结果";
      case "natal_output_wealth" -> "长期投入是否真正转成进账";
      case "natal_wealth_capacity" -> "进账增加后，相关开销是否也跟着增加";
      case "natal_shared_responsibility" -> "共同用钱是否影响最后结余";
      case "natal_balance" -> "额外开销是否随着进账一起增加";
      case "natal_combination" -> "原有收支条件是否相互牵动";
      case "natal_structure" -> "自己一贯的收支方式是否影响结果";
      default -> throw new IllegalArgumentException("unknown retrospective angle: " + angle);
    };
  }

  static String retrospectiveBridge(String subject, String direction) {
    String object = retrospectiveObject(subject);
    return switch (direction) {
      case "supportive" -> "去年先把" + object + "核对清楚，再看今年是否延续。";
      case "mixed" -> "去年先把" + object + "的进展和反复分开看，再决定今年怎么安排。";
      case "restricted" -> "去年先看清" + object + "受到哪些限制，再决定今年先守住什么。";
      default -> throw new IllegalArgumentException("unknown retrospective direction: " + direction);
    };
  }

  static List<String> causes(WealthAssessment year, Decision d) {
    Set<String> result = new LinkedHashSet<>();
    for (var e : year.evidence()) {
      if (!d.limitingEvidenceIds().contains(e.id())) continue;
      String key = e.factKey();
      if (key.equals("natal.combination.peer_wealth")) result.add("shared_responsibility");
      else if (key.equals("natal.balance")) result.add("extra_spending");
      else if (key.matches("annual\\.branch\\.(clash|harm|punishment)\\..+")) result.add("timing_changes");
      else if (key.equals("dayun.stem.ten_god")) result.add("money_responsibility");
      else if (key.equals("annual.stem.ten_god")) result.add("receipt_expectation");
    }
    return result.stream().sorted().toList();
  }

  static String limitation(WealthAssessment year, Decision d) {
    List<String> causes = causes(year, d);
    if (causes.isEmpty()) return words(d.path()).limitation();
    return String.join("", causes.stream().map(c -> switch (c) {
      case "shared_responsibility" -> "涉及共同用钱时，责任和额外开销要先看清。";
      case "extra_spending" -> "进账增加不等于结余增加，新增花费要留出余地。";
      case "timing_changes" -> "到账时间变化或临时增加开销，是需要防备的情况。";
      case "money_responsibility" -> "与他人有关的支出要留出余地，别把进账全当作可用的钱。";
      case "receipt_expectation" -> "预计进账和实际到账需要分开，不能提前使用尚未收到的钱。";
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
      case "stable_income" -> "安排长期支出时";
      case "skill_income" -> "增加投入时";
      case "project_income" -> "按预计进账安排花费时";
      case "cooperation_income" -> "钱与他人有关时";
      case "retention" -> "想把钱留下来";
      default -> throw new IllegalArgumentException("unknown wealth caution");
    };
    return context + "，要留意" + concerns(year, d) + "。";
  }

  static String concerns(WealthAssessment year, Decision d) {
    String concerns = String.join("、", causes(year, d).stream().map(c -> switch (c) {
      case "shared_responsibility" -> "共同用钱的责任";
      case "extra_spending" -> "新增花费";
      case "timing_changes" -> "到账时间变化";
      case "money_responsibility" -> "与他人有关的开销";
      case "receipt_expectation" -> "预计与实际到账的差距";
      default -> throw new IllegalArgumentException("unknown wealth copy cause");
    }).toList());
    if (concerns.isEmpty()) concerns = switch (d.path()) {
      case "stable_income" -> "进账中断的可能";
      case "skill_income" -> "投入是否值得";
      case "project_income" -> "到账延后";
      case "cooperation_income" -> "资金责任和开销";
      default -> "收入与支出";
    };
    return concerns;
  }

  static String route(String path) {
    return switch (path) {
      case "stable_income" -> "先核对进账是否持续，再决定能承担多少长期支出。";
      case "skill_income" -> "比较投入增加前后的实际进账，避免只看到忙碌程度。";
      case "project_income" -> "把预计到账和实际到账分开记录，别提前使用尚未收到的钱。";
      case "cooperation_income" -> "涉及共同用钱时，先明确金额、用途和各自责任。";
      case "retention" -> "给临时开销留一笔余钱，收入增加后也不要马上提高固定花费。";
      default -> throw new IllegalArgumentException("unknown wealth route");
    };
  }
}
