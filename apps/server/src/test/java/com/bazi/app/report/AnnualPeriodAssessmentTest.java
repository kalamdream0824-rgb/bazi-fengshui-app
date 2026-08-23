package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnualPeriodAssessmentTest {

  @Test
  void labelsEverySupportedPeriodInChinese() {
    assertEquals("两年", assessmentWith(2).periodLabel());
    assertEquals("三年", assessmentWith(3).periodLabel());
    assertEquals("四年", assessmentWith(4).periodLabel());
    assertEquals("五年", assessmentWith(5).periodLabel());
  }

  @Test
  void rejectsMissingOrNonConsecutiveYears() {
    assertThrows(IllegalArgumentException.class, () -> new AnnualPeriodAssessment(
        ReportTopic.WEALTH,
        LocalDate.of(2026, 8, 23),
        ReportHorizon.of(3),
        List.of(year(2026), year(2028), year(2029)),
        List.of(
            new AnnualTransition(2026, 2028, "增强"),
            new AnnualTransition(2028, 2029, "延续")),
        List.of()));
  }

  @Test
  void requiresOneTransitionForEveryAdjacentPair() {
    assertThrows(IllegalArgumentException.class, () -> new AnnualPeriodAssessment(
        ReportTopic.WEALTH,
        LocalDate.of(2026, 8, 23),
        ReportHorizon.of(3),
        List.of(year(2026), year(2027), year(2028)),
        List.of(new AnnualTransition(2026, 2027, "增强")),
        List.of()));
  }

  private AnnualPeriodAssessment assessmentWith(int count) {
    List<YearAssessment> years = java.util.stream.IntStream.range(0, count)
        .mapToObj(index -> year(2026 + index))
        .toList();
    List<AnnualTransition> transitions = java.util.stream.IntStream.range(0, count - 1)
        .mapToObj(index -> new AnnualTransition(2026 + index, 2027 + index, "延续"))
        .toList();
    return new AnnualPeriodAssessment(
        ReportTopic.WEALTH,
        LocalDate.of(2026, 8, 23),
        ReportHorizon.of(count),
        years,
        transitions,
        List.of("记录收支"));
  }

  private YearAssessment year(int year) {
    return new YearAssessment(
        year,
        "丙午",
        AnnualStage.STABLE,
        "保持稳定",
        "先看实际变化",
        List.of(),
        List.of(),
        List.of("记录变化"),
        List.of("收入是否变化"),
        List.of(
            new ReportEvidence("annual.stem." + year, "流年天干", "可核验"),
            new ReportEvidence("annual.branch." + year, "流年地支", "可核验")),
        List.of(),
        ConfidenceLevel.MEDIUM,
        List.of("wealth.stable"),
        AssessmentBasis.REVIEWED_RULES);
  }
}
