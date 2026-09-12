package com.bazi.app.report.overall.v3;

import com.bazi.app.report.AnnualActionGuide;
import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.ReportContent;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Strict saved-content contract for the decision-led overall report. */
public record OverallV3NarrativePlan(
    int horizonYears,
    String thesis,
    String summary,
    List<YearNarrative> years,
    String readingNote,
    List<String> evidenceKeys,
    @JsonInclude(JsonInclude.Include.NON_NULL) NarrativeTimeline timeline) implements ReportContent {

  public OverallV3NarrativePlan {
    if (horizonYears < 2 || horizonYears > 5) {
      throw new IllegalArgumentException("overall v3 horizon must be between two and five years");
    }
    thesis = text(thesis, "overall v3 thesis");
    summary = text(summary, "overall v3 summary");
    readingNote = text(readingNote, "overall v3 reading note");
    years = requiredList(years, "overall v3 years");
    evidenceKeys = evidence(evidenceKeys, "overall v3 evidence");
    if (years.size() != horizonYears) {
      throw new IllegalArgumentException("overall v3 requires every configured year");
    }
    for (int index = 1; index < years.size(); index++) {
      if (years.get(index).year() != years.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("overall v3 years must be consecutive");
      }
    }
    if (timeline != null
        && (timeline.present().year() != years.get(0).year()
            || !timeline.future().stream().map(NarrativeTimeline.FutureStep::year).toList()
                .equals(years.subList(1, years.size()).stream().map(YearNarrative::year).toList()))) {
      throw new IllegalArgumentException("overall v3 timeline does not match product years");
    }
  }

  public record YearNarrative(
      int year,
      String primaryCode,
      String primaryLabel,
      String secondaryCode,
      String secondaryLabel,
      String decisionKey,
      String conflictKey,
      String headline,
      String linkage,
      AnnualActionGuide actionGuide,
      List<Observation> observations,
      String transition,
      List<String> evidenceKeys) {

    public YearNarrative {
      if (year <= 0) throw new IllegalArgumentException("overall v3 year must be positive");
      primaryCode = text(primaryCode, "overall v3 primary code");
      primaryLabel = text(primaryLabel, "overall v3 primary label");
      secondaryCode = text(secondaryCode, "overall v3 secondary code");
      secondaryLabel = text(secondaryLabel, "overall v3 secondary label");
      if (primaryCode.equals(secondaryCode)) {
        throw new IllegalArgumentException("overall v3 primary and secondary must differ");
      }
      decisionKey = text(decisionKey, "overall v3 decision key");
      conflictKey = text(conflictKey, "overall v3 conflict key");
      headline = text(headline, "overall v3 headline");
      linkage = text(linkage, "overall v3 linkage");
      Objects.requireNonNull(actionGuide, "overall v3 action guide");
      transition = text(transition, "overall v3 transition");
      observations = requiredList(observations, "overall v3 observations");
      evidenceKeys = evidence(evidenceKeys, "overall v3 annual evidence");
      String auditedPrimaryCode = primaryCode;
      String auditedSecondaryCode = secondaryCode;
      if (observations.size() != 2
          || observations.stream().map(Observation::topicCode).distinct().count() != 2
          || observations.stream().anyMatch(item ->
              item.topicCode().equals(auditedPrimaryCode)
                  || item.topicCode().equals(auditedSecondaryCode))) {
        throw new IllegalArgumentException("overall v3 requires the two remaining topic observations");
      }
    }
  }

  public record Observation(
      String topicCode,
      String topicLabel,
      String stance,
      String note,
      List<String> evidenceKeys) {

    public Observation {
      topicCode = text(topicCode, "overall v3 observation topic");
      topicLabel = text(topicLabel, "overall v3 observation label");
      stance = text(stance, "overall v3 observation stance");
      note = text(note, "overall v3 observation note");
      evidenceKeys = evidence(evidenceKeys, "overall v3 observation evidence");
    }
  }

  private static String text(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    return value.strip();
  }

  private static <T> List<T> requiredList(List<T> values, String label) {
    if (values == null || values.isEmpty() || values.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(label + " must not be empty");
    }
    return List.copyOf(values);
  }

  private static List<String> evidence(List<String> values, String label) {
    List<String> result = requiredList(values, label).stream()
        .map(value -> text(value, label))
        .toList();
    if (new LinkedHashSet<>(result).size() != result.size()) {
      throw new IllegalArgumentException(label + " must not contain duplicates");
    }
    return result;
  }
}
