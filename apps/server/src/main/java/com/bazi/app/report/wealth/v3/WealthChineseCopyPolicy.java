package com.bazi.app.report.wealth.v3;

import java.util.List;

/** Mechanical readability gate for customer-visible annual wealth headlines. */
public final class WealthChineseCopyPolicy {

  private static final List<String> FORBIDDEN = List.of(
      "有利条件",
      "好坏条件",
      "改善空间",
      "存在变数",
      "有变数",
      "值得重点留意",
      "同时记录",
      "这方面",
      "先观察再判断",
      "它主要关系到",
      "需要留意的限制",
      "条件的组合",
      "本身的倾向",
      "相对位置",
      "这一方向",
      "不必另起一套安排",
      "的支撑",
      "整体资金判断",
      "整体信号",
      "主要风险",
      "年的预计进账的");

  public void validateBodyParagraph(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("wealth body paragraph must not be blank");
    }
    for (String phrase : FORBIDDEN) {
      if (text.contains(phrase)) {
        throw new IllegalArgumentException(
            "wealth body paragraph contains abstract or unresolved copy: " + phrase);
      }
    }
  }

  public void validateAnnualHeadline(String text) {
    validateBodyParagraph(text);
    List<String> sentences = List.of(text.split("。")).stream()
        .filter(sentence -> !sentence.isBlank())
        .toList();
    if (sentences.size() != 2
        || !sentences.get(0).startsWith("今年")
        || !sentences.get(1).startsWith("先")) {
      throw new IllegalArgumentException(
          "wealth annual headline must state this year's judgment and one explicit check");
    }
  }
}
