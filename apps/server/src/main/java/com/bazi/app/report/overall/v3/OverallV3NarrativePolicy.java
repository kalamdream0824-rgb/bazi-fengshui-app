package com.bazi.app.report.overall.v3;

import com.bazi.app.report.AnnualActionGuide;
import com.bazi.app.report.AnnualActionGuidePolicy;
import java.util.List;

/** Additional release gate for identity-neutral, complete Chinese overall copy. */
final class OverallV3NarrativePolicy {

  private static final List<String> UNCONFIRMED_IDENTITIES = List.of(
      "老板", "客户", "负责人", "职位", "配偶", "对象", "求职", "创业");
  private static final List<String> NO_ANSWER_COPY = List.of(
      "卡点", "卡住", "现有证据不足", "先观察再判断", "视情况",
      "可以作为关注", "可能作为关注");

  private OverallV3NarrativePolicy() {}

  static void validate(List<AnnualActionGuide> guides) {
    AnnualActionGuidePolicy.validate(guides);
    for (AnnualActionGuide guide : guides) {
      if (guide.focusKey().matches(".*20[0-9]{2}.*")
          || guide.focusKey().contains(".first")
          || guide.focusKey().contains(".second")
          || guide.focusKey().contains(".third")) {
        throw new IllegalArgumentException("overall focus must not encode calendar position");
      }
      for (String line : guide.lines()) {
        if (!line.matches(".*[。！？]$")) {
          throw new IllegalArgumentException("overall copy must be a complete Chinese sentence");
        }
        reject(line, UNCONFIRMED_IDENTITIES, "unconfirmed identity");
        reject(line, NO_ANSWER_COPY, "no-answer copy");
      }
    }
  }

  private static void reject(String line, List<String> fragments, String label) {
    for (String fragment : fragments) {
      if (line.contains(fragment)) {
        throw new IllegalArgumentException(label + ": " + fragment);
      }
    }
  }
}
