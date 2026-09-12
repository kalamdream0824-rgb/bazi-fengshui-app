package com.bazi.app.report;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Mechanical release gate shared by every topic that publishes an annual action guide. */
public final class AnnualActionGuidePolicy {

  private static final List<String> VAGUE_FRAGMENTS = List.of(
      "卡点", "卡住", "有所改善", "后续再看", "视情况", "现有证据不足",
      "先观察再判断", "可以作为关注", "可能作为关注");

  private AnnualActionGuidePolicy() {}

  public static void validate(List<AnnualActionGuide> guides) {
    if (guides == null || guides.isEmpty()) {
      throw new IllegalArgumentException("annual action guides must not be empty");
    }

    Set<String> sentences = new HashSet<>();
    Map<String, String> decisionFocuses = new HashMap<>();
    for (AnnualActionGuide guide : guides) {
      validateGuide(guide);
      for (String line : guide.lines()) {
        for (String sentence : line.split("(?<=[。！？])")) {
          String normalized = normalize(sentence);
          if (!normalized.isEmpty() && !sentences.add(normalized)) {
            throw new IllegalArgumentException(
                "annual action guide repeats a visible sentence: " + sentence.trim());
          }
        }
      }
      String decision = normalize(guide.action()) + "|"
          + normalize(guide.expectedChange()) + "|" + normalize(guide.successSignal());
      String existingFocus = decisionFocuses.putIfAbsent(decision, guide.focusKey());
      if (existingFocus != null && !existingFocus.equals(guide.focusKey())) {
        throw new IllegalArgumentException("different calculation focuses share one decision");
      }
    }
  }

  private static void validateGuide(AnnualActionGuide guide) {
    if (guide == null) throw new IllegalArgumentException("annual action guide must not be null");
    if (guide.focusKey().isBlank() || guide.focusKey().equals("legacy.unspecified")) {
      throw new IllegalArgumentException("generated annual action guide requires a calculation focus");
    }
    if (!guide.checkTiming().matches(".*([0-9一二三四五六七八九十]+[天周月次]|每天|每周|每月|年中|年底).*")) {
      throw new IllegalArgumentException("annual action guide requires an observable check timing");
    }
    if (normalize(guide.action()).equals(normalize(guide.fallbackAction()))) {
      throw new IllegalArgumentException("primary and fallback actions must differ");
    }
    for (String line : guide.lines()) {
      if (line.contains("{{") || line.contains("}}")) {
        throw new IllegalArgumentException("annual action guide contains an unresolved token");
      }
      for (String fragment : VAGUE_FRAGMENTS) {
        if (line.contains(fragment)) {
          throw new IllegalArgumentException("annual action guide contains vague copy: " + fragment);
        }
      }
    }
  }

  private static String normalize(String text) {
    return text.replaceAll("[\\s，。；：、！？,.!?;:]", "").trim();
  }
}
