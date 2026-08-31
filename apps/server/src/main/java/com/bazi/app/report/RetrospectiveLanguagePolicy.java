package com.bazi.app.report;

import java.util.List;
import java.util.Objects;

public final class RetrospectiveLanguagePolicy {

  private static final List<String> REVIEW_MARKERS = List.of("如果", "是否", "可能", "回看");
  private static final List<String> FORBIDDEN_ASSERTIONS = List.of(
      "你去年已经",
      "你去年一定",
      "去年必然",
      "事实证明",
      "准确命中");

  private RetrospectiveLanguagePolicy() {}

  public static void validate(List<String> checkpoints) {
    Objects.requireNonNull(checkpoints, "checkpoints");
    for (String checkpoint : checkpoints) {
      String copy = requireText(checkpoint, "past checkpoint");
      if (FORBIDDEN_ASSERTIONS.stream().anyMatch(copy::contains)) {
        throw new IllegalArgumentException("past checkpoint must not assert an unverified event");
      }
      if (REVIEW_MARKERS.stream().noneMatch(copy::contains)) {
        throw new IllegalArgumentException("past checkpoint must use retrospective review language");
      }
    }
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
