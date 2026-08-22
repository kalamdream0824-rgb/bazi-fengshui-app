package com.bazi.app.report;

import java.util.List;

public record ReportDocument(
    String contentVersion,
    String title,
    String subject,
    String topicCode,
    String topicLabel,
    String editionCode,
    String editionLabel,
    ReportProfile profile,
    List<ReportChapter> chapters,
    ReportAppendix appendix,
    String disclaimer) {

  public record ReportProfile(
      String solarText,
      String lunarText,
      String genderLabel,
      String pillarsText,
      String dayMaster,
      String currentDaYun,
      String currentLiuNian) {}

  public record ReportChapter(String number, String title, String lead, List<ReportSection> sections) {}

  public record ReportSection(String title, String sectionTopic, List<ReportPoint> points) {

    public ReportSection {
      points = points == null ? List.of() : List.copyOf(points);
    }

    public ReportSection(String title, List<ReportPoint> points) {
      this(title, "legacy", points);
    }
  }

  public record ReportAppendix(String title, List<ReportSection> sections) {

    public ReportAppendix {
      sections = sections == null ? List.of() : List.copyOf(sections);
    }
  }

  public record ReportPoint(
      String ruleKey,
      String conclusion,
      List<String> evidence,
      List<String> evidenceKeys,
      String interpretation,
      String methodNote,
      String prompt) {

    public ReportPoint {
      evidence = evidence == null ? List.of() : List.copyOf(evidence);
      evidenceKeys = evidenceKeys == null ? List.of() : List.copyOf(evidenceKeys);
    }

    public ReportPoint(
        String ruleKey,
        String conclusion,
        List<String> evidence,
        String interpretation,
        String methodNote,
        String prompt) {
      this(ruleKey, conclusion, evidence, List.of(), interpretation, methodNote, prompt);
    }
  }
}
