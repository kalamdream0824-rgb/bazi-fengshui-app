package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ThreeYearAssessmentTest {

  @Test
  void requiresThreeConsecutiveYears() {
    assertThrows(IllegalArgumentException.class, () -> new ThreeYearAssessment(
        ReportTopic.CAREER,
        LocalDate.of(2026, 8, 23),
        List.of(year(2026), year(2028)),
        "先稳后进",
        List.of("核对职责")));
  }

  @Test
  void startsWithTheGenerationCalendarYear() {
    assertThrows(IllegalArgumentException.class, () -> new ThreeYearAssessment(
        ReportTopic.CAREER,
        LocalDate.of(2026, 8, 23),
        List.of(year(2025), year(2026), year(2027)),
        "先稳后进",
        List.of("核对职责")));
  }

  @Test
  void requiresTwoIndependentEvidenceItemsForAnAnnualConclusion() {
    assertThrows(IllegalArgumentException.class, () -> new YearAssessment(
        2026,
        "丙午",
        AnnualStage.ADVANCE,
        "推进期",
        "职责扩张",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(new ReportEvidence("same", "证据", "一")),
        List.of(),
        ConfidenceLevel.HIGH,
        List.of("career.responsibility"),
        AssessmentBasis.REVIEWED_RULES));
  }

  @Test
  void permitsAnExplicitLowConfidenceInsufficientEvidenceResult() {
    YearAssessment assessment = new YearAssessment(
        2026,
        "丙午",
        AnnualStage.STABLE,
        "依据不足，建议观察",
        "现有证据不足以形成稳定结论",
        List.of(),
        List.of(),
        List.of("记录现实变化"),
        List.of("职责范围是否持续变化"),
        List.of(),
        List.of(),
        ConfidenceLevel.LOW,
        List.of(),
        AssessmentBasis.INSUFFICIENT_EVIDENCE);

    assertEquals(AssessmentBasis.INSUFFICIENT_EVIDENCE, assessment.basis());
  }

  @Test
  void insufficientEvidenceCannotClaimHighConfidence() {
    assertThrows(IllegalArgumentException.class, () -> new YearAssessment(
        2026,
        "丙午",
        AnnualStage.STABLE,
        "依据不足，建议观察",
        "现有证据不足以形成稳定结论",
        List.of(),
        List.of(),
        List.of("记录现实变化"),
        List.of("职责范围是否持续变化"),
        List.of(),
        List.of(),
        ConfidenceLevel.HIGH,
        List.of(),
        AssessmentBasis.INSUFFICIENT_EVIDENCE));
  }

  @Test
  void copiesMutableCollectionsAtTheContractBoundary() {
    List<String> priorities = new ArrayList<>(List.of("核对职责"));
    ThreeYearAssessment assessment = new ThreeYearAssessment(
        ReportTopic.CAREER,
        LocalDate.of(2026, 8, 23),
        List.of(year(2026), year(2027), year(2028)),
        "先稳后进",
        priorities);

    priorities.clear();

    assertEquals(List.of("核对职责"), assessment.priorities());
    assertThrows(UnsupportedOperationException.class, () -> assessment.years().clear());
  }

  @Test
  void appendixCopiesItsSectionsAtTheContractBoundary() {
    List<ReportDocument.ReportSection> sections = new ArrayList<>(List.of(
        new ReportDocument.ReportSection("命盘依据", List.of())));

    ReportDocument.ReportAppendix appendix =
        new ReportDocument.ReportAppendix("附录", sections);
    sections.clear();

    assertEquals(1, appendix.sections().size());
    assertThrows(UnsupportedOperationException.class, () -> appendix.sections().clear());
  }

  private YearAssessment year(int year) {
    return new YearAssessment(
        year,
        switch (year) {
          case 2026 -> "丙午";
          case 2027 -> "丁未";
          default -> "戊申";
        },
        AnnualStage.STABLE,
        "稳步观察",
        "先核对现实信号",
        List.of(),
        List.of(),
        List.of("记录变化"),
        List.of("职责范围发生变化"),
        List.of(
            new ReportEvidence("annual.stem", "流年天干", "可核验"),
            new ReportEvidence("annual.branch", "流年地支", "可核验")),
        List.of(),
        ConfidenceLevel.MEDIUM,
        List.of("career.stable"),
        AssessmentBasis.REVIEWED_RULES);
  }
}
