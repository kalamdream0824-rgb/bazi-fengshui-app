package com.bazi.app.report.relationship;

import com.bazi.app.report.ReportContent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Immutable current-year reading, with a separate next-year outlook and calculation audit. */
public record RelationshipSingleNarrativePlan(
    String relationshipStatus,
    int horizonYears,
    String thesis,
    String summary,
    int currentYear,
    int outlookYear,
    List<Section> sections,
    List<String> outlook,
    String readingNote,
    List<RelationshipYearEvaluation> evaluations,
    List<String> evidenceKeys) implements ReportContent {

  public RelationshipSingleNarrativePlan {
    if (!"single".equals(relationshipStatus) || horizonYears != 2 || outlookYear != currentYear + 1) {
      throw new IllegalArgumentException("single reading requires current year and next year only");
    }
    thesis = requireText(thesis);
    summary = requireText(summary);
    readingNote = requireText(readingNote);
    sections = List.copyOf(sections);
    outlook = lines(outlook);
    evaluations = List.copyOf(evaluations);
    evidenceKeys = lines(evidenceKeys);
    if (evaluations.size() != 2 || evaluations.get(0).year() != currentYear
        || evaluations.get(1).year() != outlookYear || outlook.isEmpty() || evidenceKeys.isEmpty()) {
      throw new IllegalArgumentException("single reading does not match its calculation horizon");
    }
    List<String> sectionIds = evaluations.get(0).totalLimitationWeight() > 0
        ? List.of("opportunities", "development", "attention")
        : List.of("opportunities", "development");
    if (!sections.stream().map(Section::id).toList().equals(sectionIds)) {
      throw new IllegalArgumentException("single reading sections do not match current year risks");
    }
    Set<String> currentKeys = keys(List.of(evaluations.get(0)));
    for (Section section : sections) {
      if (!currentKeys.containsAll(section.evidenceKeys())) {
        throw new IllegalArgumentException("current year section contains unrelated evidence");
      }
    }
    if (!Set.copyOf(evidenceKeys).equals(keys(evaluations))
        || evidenceKeys.stream().distinct().count() != evidenceKeys.size()) {
      throw new IllegalArgumentException("single reading requires all calculation evidence");
    }
  }

  public record Section(String id, String title, List<String> paragraphs,
                        List<String> signals, List<String> evidenceKeys) {
    public Section {
      id = requireText(id);
      title = requireText(title);
      paragraphs = lines(paragraphs);
      signals = lines(signals);
      evidenceKeys = lines(evidenceKeys);
      if (paragraphs.isEmpty()) throw new IllegalArgumentException("reading section requires paragraphs");
    }
  }

  private static Set<String> keys(List<RelationshipYearEvaluation> evaluations) {
    return evaluations.stream().flatMap(year -> year.dimensions().values().stream())
        .flatMap(dimension -> dimension.evidence().stream()).map(RelationshipEvidence::key)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static List<String> lines(List<String> values) {
    return values.stream().map(RelationshipSingleNarrativePlan::requireText).toList();
  }

  private static String requireText(String text) {
    if (text == null || text.isBlank() || text.contains("${") || text.contains("{{")
        || text.matches("(?s).*\\{[a-z_]+}.*")) {
      throw new IllegalArgumentException("single reading requires complete text");
    }
    return text;
  }
}
