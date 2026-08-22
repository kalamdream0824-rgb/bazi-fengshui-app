package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.ReportDocument.ReportPoint;
import com.bazi.app.report.ReportDocument.ReportProfile;
import com.bazi.app.report.ReportDocument.ReportSection;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportPresenterTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"),
      ZoneId.of("Asia/Shanghai"));

  private ThreeYearAssessment assessment;
  private ReportProfile profile;
  private ReportPresenter plainPresenter;
  private ReportPresenter professionalPresenter;

  @BeforeEach
  void setUp() {
    PaipanRequest request = new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
    PaipanResultDto chart = new BaziService().paipan(request);
    AnnualRuleCatalog catalog = new AnnualRuleCatalog();
    assessment = new ThreeYearAssessor(CLOCK, catalog).assess(request, chart, ReportTopic.CAREER);
    ReportAnalysis analysis = ReportAnalysis.from(request, chart);
    profile = new ReportProfile(
        chart.solarText(), chart.lunarText(), "男", analysis.fact("pillars").value(),
        analysis.fact("dayMaster").value(), analysis.fact("currentDayun").value(),
        analysis.fact("currentYear").value());
    plainPresenter = new PlainReportPresenter(catalog);
    professionalPresenter = new ProfessionalReportPresenter(catalog);
  }

  @Test
  void editionsShareAnnualConclusionsAndEvidenceButNotPresentationDensity() {
    ReportDocument plain = plainPresenter.present(assessment, profile);
    ReportDocument professional = professionalPresenter.present(assessment, profile);

    assertEquals(annualPoints(plain).map(ReportPoint::ruleKey).toList(),
        annualPoints(professional).map(ReportPoint::ruleKey).toList());
    assertEquals(annualPoints(plain).map(ReportPoint::evidenceKeys).toList(),
        annualPoints(professional).map(ReportPoint::evidenceKeys).toList());
    assertTrue(flatten(professional).length() > flatten(plain).length() * 1.25,
        "plain=" + flatten(plain).length() + ", professional=" + flatten(professional).length());
  }

  @Test
  void plainEditionContainsNoUnexplainedSpecialistTerms() {
    String text = flatten(plainPresenter.present(assessment, profile));

    for (String term : List.of("透干", "通根", "制化", "月令", "食伤", "官杀", "比劫")) {
      assertFalse(text.contains(term), term);
    }
  }

  @Test
  void atLeastEightyPercentOfBodySectionsStayOnThePurchasedTopic() {
    for (ReportDocument document : List.of(
        plainPresenter.present(assessment, profile),
        professionalPresenter.present(assessment, profile))) {
      List<ReportSection> body = document.chapters().stream()
          .flatMap(chapter -> chapter.sections().stream())
          .toList();
      long focused = body.stream()
          .filter(section -> section.sectionTopic().equals(ReportTopic.CAREER.code()))
          .count();
      assertTrue((double) focused / body.size() >= 0.80, document.editionCode());
    }
  }

  @Test
  void presentsFiveTopicFirstChaptersAndASeparateBasisAppendix() {
    ReportDocument document = plainPresenter.present(assessment, profile);

    assertEquals(List.of(
        "未来三年事业运势总览",
        "2026年事业运势详解",
        "2027年事业运势详解",
        "2028年事业运势详解",
        "三年事业行动路线"),
        document.chapters().stream().map(ReportDocument.ReportChapter::title).toList());
    assertEquals("命盘与计算依据", document.appendix().title());
    assertFalse(document.appendix().sections().isEmpty());
  }

  @Test
  void professionalEditionDisclosesRulesConfidenceAndMethodBoundary() {
    String text = flatten(professionalPresenter.present(assessment, profile));

    assertTrue(text.contains("career."));
    assertTrue(text.contains("置信等级"));
    assertTrue(text.contains("方法边界"));
    assertTrue(text.contains("反向证据"));
  }

  private Stream<ReportPoint> annualPoints(ReportDocument document) {
    return document.chapters().stream().skip(1).limit(3)
        .flatMap(chapter -> chapter.sections().stream())
        .flatMap(section -> section.points().stream());
  }

  private String flatten(ReportDocument document) {
    Stream<ReportSection> sections = Stream.concat(
        document.chapters().stream().flatMap(chapter -> chapter.sections().stream()),
        document.appendix().sections().stream());
    return sections.flatMap(section -> section.points().stream())
        .flatMap(point -> Stream.concat(
            Stream.of(point.ruleKey(), point.conclusion(), point.interpretation(), point.methodNote(), point.prompt()),
            point.evidence().stream()))
        .filter(value -> value != null)
        .reduce(document.title() + document.disclaimer(), String::concat);
  }
}
