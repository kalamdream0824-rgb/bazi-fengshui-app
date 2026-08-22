package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.ReportDocument.ReportPoint;
import com.bazi.app.service.BaziService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ReportComposerTest {

  private PaipanRequest request;
  private PaipanResultDto result;
  private ReportComposer composer;

  @BeforeEach
  void setUp() {
    request = new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
    result = new BaziService().paipan(request);
    composer = new ReportComposer();
  }

  @Test
  void professionalCareerReportUsesFiveTopicFirstChapters() {
    ReportDocument report = composer.compose(request, result, ReportTopic.CAREER, ReportEdition.PROFESSIONAL);

    assertEquals(5, report.chapters().size());
    assertEquals("未来三年事业运势总览", report.chapters().get(0).title());
    assertEquals("2026年事业运势详解", report.chapters().get(1).title());
    assertEquals("三年事业行动路线", report.chapters().get(4).title());
    assertTrue(points(report).anyMatch(point -> point.methodNote() != null && !point.methodNote().isBlank()));
    assertTrue(report.profile().pillarsText().contains("乙亥 · 乙酉 · 壬申 · 丁未"));
  }

  @Test
  void reportAvoidsCertaintyAndUnsupportedYongshenClaims() {
    ReportDocument report = composer.compose(request, result, ReportTopic.OVERALL, ReportEdition.PROFESSIONAL);
    String allText = flatten(report);

    for (String forbidden : List.of("一定", "注定", "必发财", "必离婚", "改运", "消灾")) {
      assertFalse(allText.contains(forbidden), forbidden);
    }
    assertTrue(allText.contains("方法边界"));
  }

  @ParameterizedTest
  @EnumSource(ReportTopic.class)
  void everyTopicSharesThreeAnnualConclusionsAcrossEditions(ReportTopic topic) {
    ReportDocument plain = composer.compose(request, result, topic, ReportEdition.PLAIN);
    ReportDocument professional = composer.compose(request, result, topic, ReportEdition.PROFESSIONAL);

    List<ReportPoint> plainTopicPoints = annualPoints(plain);
    List<ReportPoint> professionalTopicPoints = annualPoints(professional);
    assertEquals(3, plainTopicPoints.size());
    assertEquals(plainTopicPoints.stream().map(ReportPoint::ruleKey).toList(),
        professionalTopicPoints.stream().map(ReportPoint::ruleKey).toList());
    assertEquals(plainTopicPoints.stream().map(ReportPoint::conclusion).toList(),
        professionalTopicPoints.stream().map(ReportPoint::conclusion).toList());
    assertNotEquals(plainTopicPoints.stream().map(ReportPoint::interpretation).toList(),
        professionalTopicPoints.stream().map(ReportPoint::interpretation).toList());
    assertTrue(professionalTopicPoints.stream().allMatch(point -> !point.methodNote().isBlank()));
  }

  private Stream<ReportPoint> points(ReportDocument report) {
    return report.chapters().stream()
        .flatMap(chapter -> chapter.sections().stream())
        .flatMap(section -> section.points().stream());
  }

  private List<ReportPoint> annualPoints(ReportDocument report) {
    return report.chapters().stream().skip(1).limit(3)
        .flatMap(chapter -> chapter.sections().stream())
        .flatMap(section -> section.points().stream())
        .toList();
  }

  private String flatten(ReportDocument report) {
    return Stream.concat(
            Stream.of(report.title(), report.disclaimer()),
            points(report).flatMap(point -> Stream.concat(
                Stream.of(point.conclusion(), point.interpretation(), point.methodNote(), point.prompt()),
                point.evidence().stream())))
        .filter(value -> value != null)
        .reduce("", (left, right) -> left + right);
  }
}
