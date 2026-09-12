package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Closed, versioned plain-language vocabulary. No user data, random wording or report-position branches. */
final class WealthPlainCopyV3 {
  static final String VERSION = "wealth-plain-v3.18";

  record Words(String object, String meaning, String limitation, String observation, String action) {}
  record RetrospectiveWords(
      String question,
      String object,
      String supportiveSignal,
      String limitingSignal,
      String mixedSentence,
      String supportiveCondition,
      String limitingCondition,
      String mixedCondition) {}
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
  private static final Map<String, RetrospectiveWords> RETROSPECTIVE_WORDS = Map.of(
      "stable_income", new RetrospectiveWords(
          "进账有没有持续下来", "持续进账", "进账保持连续", "进账中断或波动",
          "有些时候进账连续，也可能出现中断或波动。",
          "确实有持续进账", "进账经常中断或波动", "有持续进账，也有进账中断或波动"),
      "skill_income", new RetrospectiveWords(
          "投入增加后，实际进账有没有跟着提高", "投入后的实际进账", "投入后实际进账提高",
          "投入增加但进账没有同步提高", "有些投入能带来更高进账，也可能忙得更多、进账却没提高。",
          "增加投入后，实际进账有所提高", "投入增加了，但实际进账没有提高",
          "有些投入带来更高进账，也有些投入没有带来进账提高"),
      "project_income", new RetrospectiveWords(
          "预计进账与实际到账是否一致", "预计进账的到账情况", "预计进账按时到账",
          "到账延后或金额变化", "有些预计的钱能按时到账，也可能出现延期或金额变化。",
          "预计的钱确实按时到账", "到账出现延期或金额变化",
          "有些预计的钱按时到账，也有些到账延期或金额有变化"),
      "cooperation_income", new RetrospectiveWords(
          "与他人共同用钱时，责任和开销有没有说清", "共同用钱的责任和开销", "资金责任容易分清",
          "额外责任和开销增加", "有些共同用钱的责任容易说清，也可能增加额外责任和开销。",
          "确实把共同用钱的责任和开销说清了", "共同用钱带来额外责任和开销",
          "有些共同用钱的责任和开销说得清楚，也有些带来额外负担"),
      "retention", new RetrospectiveWords(
          "进账增加后，实际留下的钱有没有变多", "实际结余", "实际结余增加",
          "进账增加但结余没有同步增加", "有些时候能多留下一点钱，有些时候进账增加了，结余却没有跟着增加。",
          "实际结余确实有所增加", "进账增加了，但实际结余没有提高",
          "有些月份实际结余增加，也有些月份进账增加但结余没有提高"));

  static Words words(String path) { return WORDS.get(path); }

  static String weakSupportNote(String profile) {
    return switch (profile) {
      case "ROOTLESS" -> "基础命盘里的支持条件较少，所以增加投入或提前安排支出时，建议采用较保守的金额。";
      case "ROOTED" -> "基础命盘里存在一些支持条件，所以不必按最保守的方式理解；新增投入和支出仍要留出余量。";
      case "ROOTED_WITH_VISIBLE_RESOURCE" ->
          "基础命盘里能看到两类支持条件，所以可以保留一定行动空间；但投入和支出不能一次安排到上限。";
      case "NOT_WEAK", "" -> "";
      default -> throw new IllegalArgumentException("unknown weak support profile: " + profile);
    };
  }
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
    return retrospectiveWords(subject).question();
  }

  static String retrospectiveObject(String subject) {
    return retrospectiveWords(subject).object();
  }

  static String retrospectiveDirection(String subject, String direction, String strength) {
    RetrospectiveWords words = retrospectiveWords(subject);
    return switch (direction) {
      case "supportive" -> switch (strength) {
        case "pronounced" -> words.supportiveSignal() + "的信号较明显。";
        case "supported" -> words.supportiveSignal() + "的迹象存在，但不算突出。";
        case "limited" -> words.supportiveSignal() + "的信号较弱。";
        default -> throw new IllegalArgumentException("unknown retrospective strength: " + strength);
      };
      case "mixed" -> words.mixedSentence();
      case "restricted" -> words.limitingSignal() + "的信号更明显。";
      default -> throw new IllegalArgumentException("unknown retrospective direction: " + direction);
    };
  }

  static String retrospectiveAngle(String angle) {
    return switch (angle) {
      case "annual_stem" -> "再对照这一年实际发生的收支变化。";
      case "annual_harmony" -> "再核对他人或原有安排有没有影响收支。";
      case "annual_clash" -> "再核对收支是否出现突然且明显的变动。";
      case "annual_harm" -> "再检查有没有零散、不易察觉的损耗。";
      case "annual_punishment" -> "再看同类收支问题是否反复出现。";
      case "annual_context" -> "再看这一年的收支条件是否改变。";
      case "dayun_context" -> "再和前后一段时间的收支状态对照。";
      case "natal_output_wealth" -> "再核对长期投入有没有转成实际进账。";
      case "natal_wealth_capacity" -> "再核对进账增加后，相关开销是否也增加。";
      case "natal_shared_responsibility" -> "再核对共同用钱是否影响最后结余。";
      case "natal_balance" -> "再核对额外开销是否随着进账增加。";
      case "natal_combination" -> "再核对原有收支条件是否相互影响。";
      case "natal_structure" -> "再和自己一贯的收支方式对照。";
      default -> throw new IllegalArgumentException("unknown retrospective angle: " + angle);
    };
  }

  static String retrospectiveAngleQuestion(String angle) {
    return switch (angle) {
      case "annual_stem" -> "当年直接出现的收支变化";
      case "annual_harmony" -> "他人或原有安排对收支的牵动";
      case "annual_clash" -> "突然且明显的收支变动";
      case "annual_harm" -> "零散、不易察觉的损耗";
      case "annual_punishment" -> "反复出现的同类收支问题";
      case "annual_context" -> "当年收支条件的改变";
      case "dayun_context" -> "前后一段时间的收支状态";
      case "natal_output_wealth" -> "长期投入转成实际进账的情况";
      case "natal_wealth_capacity" -> "进账增加后同步增加的开销";
      case "natal_shared_responsibility" -> "共同用钱带来的责任";
      case "natal_balance" -> "随着进账增加的额外开销";
      case "natal_combination" -> "原有收支条件之间的影响";
      case "natal_structure" -> "一贯的收支方式";
      default -> throw new IllegalArgumentException("unknown retrospective angle: " + angle);
    };
  }

  static String retrospectiveBridge(String subject, String direction) {
    RetrospectiveWords words = retrospectiveWords(subject);
    return switch (direction) {
      case "supportive" -> "如果去年" + words.supportiveCondition()
          + "，今年再看这种情况能否延续。";
      case "mixed" -> "如果去年" + words.mixedCondition()
          + "，今年安排时要把两种情况都算进去。";
      case "restricted" -> "如果去年" + words.limitingCondition()
          + "，今年先避免同类问题再次发生。";
      default -> throw new IllegalArgumentException("unknown retrospective direction: " + direction);
    };
  }

  private static RetrospectiveWords retrospectiveWords(String subject) {
    RetrospectiveWords words = RETROSPECTIVE_WORDS.get(subject);
    if (words == null) {
      throw new IllegalArgumentException("unknown wealth retrospective subject: " + subject);
    }
    return words;
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

}
