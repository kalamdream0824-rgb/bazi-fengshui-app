package com.bazi.app.report.wealth.v3;

import java.util.ArrayList;
import java.util.List;

/** Writes adjacent-year changes with concrete money objects and a usable next action. */
final class WealthAnnualComparisonWriter {

  String write(WealthAssessment current, WealthAssessment next, WealthComparison comparison) {
    if (comparison.toYear() != next.year()) {
      throw new IllegalArgumentException("wealth comparison year does not match its reading");
    }
    String text;
    if (comparison.direction().equals("unchanged")) {
      String path = primaryPath(current);
      text = unchangedText(current.year(), next.year(), path) + next.year() + "年继续"
          + action(path) + "。";
    } else {
      List<String> sentences = new ArrayList<>();
      for (int index = 0; index < comparison.changes().size(); index++) {
        WealthComparison.Change change = comparison.changes().get(index);
        String prefix = index == 0
            ? "与" + current.year() + "年相比，" + next.year() + "年"
            : next.year() + "年";
        sentences.add(prefix + changeText(current, next, change) + "。");
      }
      String actionPath = comparison.changes().get(0).path();
      sentences.add(next.year() + "年先" + action(actionPath) + "。");
      text = String.join("", sentences);
    }
    new WealthChineseCopyPolicy().validateBodyParagraph(text);
    return text;
  }

  private String changeText(
      WealthAssessment current,
      WealthAssessment next,
      WealthComparison.Change change) {
    String path = change.path();
    int support = change.supportDelta();
    int limitation = change.limitationDelta();
    if (support > 0 && limitation > 0) {
      return positiveSignal(path) + "更明显，但" + limitation(path) + "也升高";
    }
    if (support < 0 && limitation < 0) {
      return positiveSignal(path) + "减弱，" + limitation(path) + "也降低";
    }
    if (support < 0 && limitation > 0) {
      return positiveSignal(path) + "减弱，" + limitation(path) + "升高";
    }
    if (support > 0 && limitation < 0) {
      return positiveSignal(path) + "更明显，" + limitation(path) + "降低";
    }
    if (limitation > 0) return limitation(path) + "升高";
    if (limitation < 0) return limitation(path) + "降低";
    if (support > 0) {
      return decision(next, path).strength().equals("limited")
          ? positiveSignal(path) + "比上年多了一些，但仍不明显"
          : positiveSignal(path) + "更明显";
    }
    if (support < 0) return positiveSignal(path) + "减弱";
    if (!change.addedEvidenceIds().isEmpty() || !change.removedEvidenceIds().isEmpty()) {
      return changedSourceText(current, next, path);
    }
    if (current.focus().primaryCandidates().contains(path) && next.focus().state().equals("tied")) {
      return object(path) + "不再是唯一重点，还要同时看其他资金变化";
    }
    if (next.focus().primaryCandidates().contains(path)) {
      return object(path) + "需要优先核对";
    }
    return object(path) + "不再需要优先核对";
  }

  private String changedSourceText(WealthAssessment current, WealthAssessment next, String path) {
    WealthAssessment.Decision before = decision(current, path);
    WealthAssessment.Decision after = decision(next, path);
    int beforeRank = strengthRank(before.strength());
    int afterRank = strengthRank(after.strength());
    if (afterRank > beforeRank) return positiveSignal(path) + "比上年更明显";
    if (afterRank < beforeRank) return positiveSignal(path) + "比上年减弱";
    if (after.stance().equals("mixed")) {
      return positiveSignal(path) + "仍然存在，但" + limitation(path) + "也没有消失";
    }
    if (after.stance().equals("restricted")) return limitation(path) + "仍然较高";
    return switch (after.strength()) {
      case "pronounced" -> positiveSignal(path) + "仍然明显";
      case "supported" -> positiveSignal(path) + "仍然存在";
      default -> positiveSignal(path) + "仍然较弱";
    };
  }

  private String primaryPath(WealthAssessment assessment) {
    if (!assessment.focus().primaryCandidates().isEmpty()) {
      return assessment.focus().primaryCandidates().get(0);
    }
    return assessment.decisions().stream()
        .filter(decision -> !decision.stance().equals("quiet"))
        .map(WealthAssessment.Decision::path)
        .findFirst()
        .orElse("retention");
  }

  private String object(String path) {
    return switch (path) {
      case "stable_income" -> "持续进账";
      case "skill_income" -> "投入与进账";
      case "project_income" -> "预计进账的到账情况";
      case "cooperation_income" -> "共同用钱的责任和开销";
      case "retention" -> "实际结余";
      default -> throw new IllegalArgumentException("unknown wealth comparison path: " + path);
    };
  }

  private String unchangedText(int currentYear, int nextYear, String path) {
    String detail = switch (path) {
      case "stable_income" -> "进账是否持续，以及中断的可能，都没有明显变化。";
      case "skill_income" -> "投入后的实际进账和投入成本，都没有明显变化。";
      case "project_income" -> "预计的钱是否按时到账，以及延期的可能，都没有明显变化。";
      case "cooperation_income" -> "共同用钱的责任和额外开销，都没有明显变化。";
      case "retention" -> "实际结余和新增支出，都没有明显变化。";
      default -> throw new IllegalArgumentException("unknown wealth comparison path: " + path);
    };
    return "与" + currentYear + "年相比，" + nextYear + "年" + detail;
  }

  private String positiveSignal(String path) {
    return switch (path) {
      case "stable_income" -> "进账保持连续的迹象";
      case "skill_income" -> "投入后实际进账提高的迹象";
      case "project_income" -> "按时到账的迹象";
      case "cooperation_income" -> "资金责任容易分清的迹象";
      case "retention" -> "实际结余增加的迹象";
      default -> throw new IllegalArgumentException("unknown wealth comparison path: " + path);
    };
  }

  private String limitation(String path) {
    return switch (path) {
      case "stable_income" -> "进账中断的风险";
      case "skill_income" -> "投入增加但进账未提高的风险";
      case "project_income" -> "到账延后或金额变化的风险";
      case "cooperation_income" -> "额外责任和开销增加的风险";
      case "retention" -> "新增支出压低结余的风险";
      default -> throw new IllegalArgumentException("unknown wealth comparison path: " + path);
    };
  }

  private String action(String path) {
    return switch (path) {
      case "stable_income" -> "按进账是否持续决定长期支出";
      case "skill_income" -> "比较投入增加前后的实际进账";
      case "project_income" -> "按实际到账时间安排支出";
      case "cooperation_income" -> "在共同用钱前写清金额、用途和各自责任";
      case "retention" -> "按实际结余决定可以增加多少开销";
      default -> throw new IllegalArgumentException("unknown wealth comparison path: " + path);
    };
  }

  private WealthAssessment.Decision decision(WealthAssessment year, String path) {
    return year.decisions().stream()
        .filter(decision -> decision.path().equals(path))
        .findFirst()
        .orElseThrow();
  }

  private int strengthRank(String strength) {
    return switch (strength) {
      case "none" -> 0;
      case "limited" -> 1;
      case "supported" -> 2;
      case "pronounced" -> 3;
      default -> throw new IllegalArgumentException("unknown wealth expression strength");
    };
  }
}
