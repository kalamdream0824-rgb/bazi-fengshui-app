package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthAnnualHeadlinePlanner.Headline;
import java.util.List;

/** Writes concrete annual money-state copy without inventing an occupation or income source. */
final class WealthAnnualBodyWriter {

  String income(WealthAssessment year, Decision decision, boolean continuation) {
    String text = switch (decision.stance()) {
      case "quiet" -> quietIncome(decision.path());
      case "restricted" -> restrictedIncome(decision.path());
      case "mixed" -> mixedIncome(decision.path()) + limitingDetail(year, decision);
      case "supportive" -> supportiveIncome(decision.path(), decision.strength());
      default -> throw new IllegalArgumentException("unknown wealth stance: " + decision.stance());
    };
    if (continuation && text.startsWith("这一年")) {
      text = "同时，" + text.substring("这一年".length());
    }
    return checked(text);
  }

  String retention(WealthAssessment year, Decision decision, Headline headline) {
    String text = switch (decision.stance()) {
      case "quiet" -> "这一年先按实际进账和支出核对结余，不预先判断能留下多少。";
      case "restricted" -> "这一年即使进账增加，实际结余也可能没有提高。"
          + retentionCheck(year, decision);
      case "mixed" -> "这一年有机会多留下一些钱，但新增花费也可能同时增加。"
          + retentionCheck(year, decision);
      case "supportive" -> switch (decision.strength()) {
        case "pronounced" -> "这一年实际结余增加的迹象较明显，但仍要按已经到账的钱安排支出。";
        case "supported" -> "这一年有增加实际结余的迹象，先核对进账增加后是否真的留下了钱。";
        case "limited" -> "这一年结余增加的迹象较弱，不要据此提前提高固定花费。";
        default -> throw new IllegalArgumentException("unknown wealth strength: " + decision.strength());
      };
      default -> throw new IllegalArgumentException("unknown wealth stance: " + decision.stance());
    };
    return contextualized("核对" + object(headline) + "后，再看月底结余：", text);
  }

  private String retentionCheck(WealthAssessment year, Decision decision) {
    List<String> items = WealthPlainCopyV3.causes(year, decision).stream().map(cause -> switch (cause) {
      case "shared_responsibility" -> "共同用钱时各自承担的金额";
      case "extra_spending" -> "随进账增加的新增花费";
      case "timing_changes" -> "到账日期变化带来的临时开销";
      case "money_responsibility" -> "钱与他人有关时增加的开销";
      case "receipt_expectation" -> "尚未实际到账的预计进账";
      default -> throw new IllegalArgumentException("unknown wealth copy cause: " + cause);
    }).toList();
    if (items.isEmpty()) return "最后仍要以实际收支记录为准。";
    if (items.size() > 2) {
      return "核对结余时，要先扣除" + String.join("、", items.subList(0, 2))
          + "；还要算上" + String.join("、", items.subList(2, items.size())) + "。";
    }
    return "核对结余时，要先扣除" + String.join("、", items) + "。";
  }

  String risk(WealthAssessment year, Decision decision, Headline headline) {
    String lead = switch (decision.path()) {
      case "stable_income" -> "进账出现中断，却已经增加了长期支出。";
      case "skill_income" -> "投入的时间或花费增加，实际进账却没有同步提高。";
      case "project_income" -> "预计的钱没有按时到账，却提前安排了支出。";
      case "cooperation_income" -> "共同用钱的责任没有说清，最后增加了额外开销。";
      case "retention" -> "进账看似增加，最后留下的钱却没有变多。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + decision.path());
    };
    String focus = decision.path().equals(headline.pathKey())
        ? "围绕" + object(headline) + "，最需要防的是"
        : "除了核对" + object(headline) + "，还要防";
    return contextualized(focus, lead + limitingDetail(year, decision));
  }

  String observation(String path, Headline headline, boolean primary) {
    String object = object(headline);
    if (primary) {
      return checked(switch (path) {
        case "stable_income" -> verification(headline) + "，确认进账是否连续、能否覆盖现有支出。";
        case "skill_income" -> verification(headline) + "，比较投入前后实际进账有没有增加。";
        case "project_income" -> verification(headline) + "，只按最后到账和扣除花费后的结果判断。";
        case "cooperation_income" -> verification(headline) + "，确认事前约定与最后结果是否一致。";
        case "retention" -> verification(headline) + "，再看月底实际结余有没有增加。";
        default -> throw new IllegalArgumentException("unknown wealth path: " + path);
      });
    }
    return checked(switch (path) {
      case "stable_income" -> "核对每次进账日期，同时检查" + object + "，判断有没有较长中断。";
      case "skill_income" -> "比较投入前后的实际进账，同时核对" + object + "，确认投入有没有产生回报。";
      case "project_income" -> "核对预计与实际到账情况，同时检查" + object + "，确认是否延期或变动。";
      case "cooperation_income" -> "核对共同资金的金额、用途和分担结果，再看" + object + "有没有因此变化。";
      case "retention" -> "核对每月实际结余，再看" + object + "是否压低了最后留下的钱。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    });
  }

  String action(String path, Headline headline, boolean primary) {
    String object = object(headline);
    if (primary) {
      return checked(switch (path) {
        case "stable_income" -> "先" + verification(headline) + "，再决定是否增加固定支出。";
        case "skill_income" -> "先" + verification(headline) + "，再决定是否继续增加时间或花费。";
        case "project_income" -> "先" + verification(headline) + "，再安排相关支出，不提前使用尚未到账的钱。";
        case "cooperation_income" -> "先" + verification(headline) + "，再安排共同资金；说不清时先不追加。";
        case "retention" -> "先" + verification(headline) + "，再安排其余支出，不提前使用预计结余。";
        default -> throw new IllegalArgumentException("unknown wealth path: " + path);
      });
    }
    return checked(switch (path) {
      case "stable_income" -> "先按已经到账的钱安排固定支出，并以" + object + "检查能否持续。";
      case "skill_income" -> "先记录投入与实际进账，根据" + object + "确认是否继续增加投入。";
      case "project_income" -> "先等钱实际到账，根据" + object + "安排对应支出。";
      case "cooperation_income" -> "共同用钱前先写清金额、用途和各自承担多少，再核对这项安排是否让" + object + "发生变化。";
      case "retention" -> "先预留必要支出和临时开销，根据" + object + "核对最后还能留下多少。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    });
  }

  private String supportiveIncome(String path, String strength) {
    String signal = switch (path) {
      case "stable_income" -> "进账保持连续";
      case "skill_income" -> "增加投入后，实际进账提高";
      case "project_income" -> "预计的钱按时到账";
      case "cooperation_income" -> "共同用钱的责任和开销容易说清";
      default -> throw new IllegalArgumentException("unknown income path: " + path);
    };
    return switch (strength) {
      case "pronounced" -> "这一年" + signal + "的迹象较明显。";
      case "supported" -> "这一年" + signal + "的迹象存在，但不算突出。";
      case "limited" -> "这一年" + signal + "的迹象较弱，不宜据此提前增加支出。";
      default -> throw new IllegalArgumentException("unknown wealth strength: " + strength);
    };
  }

  private String mixedIncome(String path) {
    return switch (path) {
      case "stable_income" -> "这一年有持续进账的机会，也有中断或波动的可能。";
      case "skill_income" -> "这一年增加投入后，有些进账可能提高，也可能投入增加而进账没有变化。";
      case "project_income" -> "这一年有些预计的钱可能按时到账，也可能出现延期或金额变化。";
      case "cooperation_income" -> "这一年共同用钱的责任有机会说清，也可能增加额外开销。";
      default -> throw new IllegalArgumentException("unknown income path: " + path);
    };
  }

  private String restrictedIncome(String path) {
    return switch (path) {
      case "stable_income" -> "这一年进账更容易出现中断或波动，安排长期支出要更谨慎。";
      case "skill_income" -> "这一年投入增加后，实际进账未必同步提高。";
      case "project_income" -> "这一年预计的钱可能延后到账，金额也可能与预期不同。";
      case "cooperation_income" -> "这一年钱与他人有关时，额外责任和开销更容易增加。";
      default -> throw new IllegalArgumentException("unknown income path: " + path);
    };
  }

  private String quietIncome(String path) {
    return switch (path) {
      case "stable_income" -> "这一年不单独判断进账能否持续，安排支出仍以已经到账的钱为准。";
      case "skill_income" -> "这一年不单独判断增加投入能否提高进账，先核对实际投入与实际进账。";
      case "project_income" -> "这一年不单独判断到账时间，安排支出仍以实际到账为准。";
      case "cooperation_income" -> "这一年不单独判断共同用钱的结果，涉及他人时先说清各自责任。";
      default -> throw new IllegalArgumentException("unknown income path: " + path);
    };
  }

  private String limitingDetail(WealthAssessment year, Decision decision) {
    List<String> causes = WealthPlainCopyV3.causes(year, decision);
    return causes.stream().map(cause -> switch (cause) {
      case "shared_responsibility" -> "如果涉及共同用钱，先确认各自承担的金额。";
      case "extra_spending" -> "还要把随进账增加的新增花费算进去。";
      case "timing_changes" -> "到账日期发生变化时，不要按原日期提前花钱。";
      case "money_responsibility" -> "钱与他人有关时，要为额外开销留出金额。";
      case "receipt_expectation" -> "预计进账尚未到账前，不要把它计入可用金额。";
      default -> throw new IllegalArgumentException("unknown wealth copy cause: " + cause);
    }).reduce("", String::concat);
  }

  private String object(Headline headline) {
    return WealthHeadlineVocabulary.entry(headline.themeKey()).objectText();
  }

  private String subject(Headline headline) {
    String value = WealthHeadlineVocabulary.entry(headline.themeKey()).sentenceSpec().subject();
    return value.startsWith("今年") ? value.substring("今年".length()) : value;
  }

  private String verification(Headline headline) {
    return WealthHeadlineVocabulary.entry(headline.themeKey()).sentenceSpec().verification();
  }

  private String contextualized(String lead, String text) {
    String body = text.strip();
    if (body.startsWith("这一年")) body = body.substring("这一年".length());
    if (body.endsWith("。")) body = body.substring(0, body.length() - 1);
    body = body.replace("。", "；");
    return checked(lead + body + "。");
  }

  private String checked(String text) {
    new WealthChineseCopyPolicy().validateBodyParagraph(text);
    return text;
  }
}
