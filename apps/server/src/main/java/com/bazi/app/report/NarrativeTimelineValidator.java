package com.bazi.app.report;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class NarrativeTimelineValidator {

  private static final Pattern COPY_SEPARATORS = Pattern.compile("[\\p{P}\\p{Z}\\s]+");
  private static final Set<String> FUNCTIONAL_PHRASES = Set.of(
      "回看去年",
      "去年回看",
      "当下判断",
      "未来行动",
      "今年先做",
      "明年再做");

  private final List<String> corePhrases;

  public NarrativeTimelineValidator(List<String> corePhrases) {
    Objects.requireNonNull(corePhrases, "corePhrases");
    LinkedHashSet<String> audited = new LinkedHashSet<>();
    for (String phrase : corePhrases) {
      String normalized = normalize(phrase);
      long hanCharacters = normalized.codePoints()
          .filter(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN)
          .count();
      if (hanCharacters < 4) {
        throw new IllegalArgumentException("core phrase must contain at least four Chinese characters");
      }
      if (FUNCTIONAL_PHRASES.contains(normalized)) {
        throw new IllegalArgumentException("functional timeline label must not be a core phrase");
      }
      audited.add(normalized);
    }
    this.corePhrases = List.copyOf(audited);
  }

  public List<String> corePhrases() {
    return corePhrases;
  }

  public NarrativeTimeline validate(NarrativeTimeline timeline) {
    Objects.requireNonNull(timeline, "timeline");
    List<List<String>> sections = sections(timeline);
    rejectRepeatedSentences(sections);
    rejectRepeatedCorePhrases(sections);
    return timeline;
  }

  private void rejectRepeatedSentences(List<List<String>> sections) {
    Map<String, Integer> ownerByCopy = new LinkedHashMap<>();
    for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
      for (String copy : sections.get(sectionIndex)) {
        String normalized = normalize(copy);
        Integer owner = ownerByCopy.putIfAbsent(normalized, sectionIndex);
        if (owner != null && owner != sectionIndex) {
          throw new IllegalArgumentException("complete timeline sentence must not repeat across sections");
        }
      }
    }
  }

  private void rejectRepeatedCorePhrases(List<List<String>> sections) {
    for (String phrase : corePhrases) {
      int matchingSections = 0;
      for (List<String> section : sections) {
        boolean present = section.stream().map(NarrativeTimelineValidator::normalize)
            .anyMatch(copy -> copy.contains(phrase));
        if (present && ++matchingSections > 1) {
          throw new IllegalArgumentException("core timeline phrase must not repeat across sections: " + phrase);
        }
      }
    }
  }

  private static List<List<String>> sections(NarrativeTimeline timeline) {
    List<List<String>> sections = new ArrayList<>();
    List<String> past = new ArrayList<>();
    past.add(timeline.past().headline());
    past.addAll(timeline.past().checkpoints());
    past.add(timeline.past().bridge());
    sections.add(List.copyOf(past));
    sections.add(List.of(
        timeline.present().headline(),
        timeline.present().judgment(),
        timeline.present().priority()));
    for (NarrativeTimeline.FutureStep step : timeline.future()) {
      sections.add(List.of(step.headline(), step.action()));
    }
    return List.copyOf(sections);
  }

  private static String normalize(String value) {
    Objects.requireNonNull(value, "copy");
    String normalized = COPY_SEPARATORS.matcher(
        Normalizer.normalize(value, Normalizer.Form.NFKC)).replaceAll("");
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("timeline copy must not be blank");
    }
    return normalized;
  }
}
