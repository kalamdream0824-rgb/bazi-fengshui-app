package com.bazi.app.report;

import com.bazi.app.report.ReportDocument.ReportAppendix;
import com.bazi.app.report.ReportDocument.ReportChapter;
import com.bazi.app.report.ReportDocument.ReportPoint;
import com.bazi.app.report.ReportDocument.ReportProfile;
import com.bazi.app.report.ReportDocument.ReportSection;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

abstract class BaseReportPresenter implements ReportPresenter {

  protected final AnnualRuleCatalog catalog;
  private final ReportEdition edition;

  BaseReportPresenter(AnnualRuleCatalog catalog, ReportEdition edition) {
    this.catalog = Objects.requireNonNull(catalog);
    this.edition = Objects.requireNonNull(edition);
  }

  @Override
  public ReportDocument present(
      ThreeYearAssessment assessment,
      ReportProfile profile,
      String subject) {
    Objects.requireNonNull(assessment);
    Objects.requireNonNull(profile);
    String resolvedSubject = subject == null || subject.isBlank() ? "命主" : subject;
    ReportTopic topic = assessment.topic();
    List<YearAssessment> years = assessment.years();
    List<ReportChapter> chapters = List.of(
        chapter("壹", "未来三年" + topic.label() + "总览", overviewLead(assessment), topic,
            List.of(overviewPoint(assessment))),
        annualChapter("贰", years.get(0), topic),
        annualChapter("叁", years.get(1), topic),
        annualChapter("肆", years.get(2), topic),
        chapter("伍", "三年" + topic.label().replace("运势", "") + "行动路线",
            routeLead(assessment), topic, List.of(routePoint(assessment))));

    return new ReportDocument(
        ReportCopy.get("content.version"),
        resolvedSubject + " · " + edition.label() + "命书",
        resolvedSubject,
        topic.code(),
        topic.label(),
        edition.code(),
        edition.label(),
        profile,
        chapters,
        appendix(profile),
        ReportCopy.get("disclaimer"));
  }

  protected abstract String overviewInterpretation(ThreeYearAssessment assessment);

  protected abstract String annualInterpretation(YearAssessment year, ReportTopic topic);

  protected abstract String annualMethodNote(YearAssessment year);

  protected abstract String routeInterpretation(ThreeYearAssessment assessment);

  protected String topicCopy(ReportTopic topic, String editionKey) {
    return catalog.copy("copy." + topic.code() + "." + editionKey);
  }

  protected String findings(List<AnnualFinding> findings) {
    if (findings.isEmpty()) return "暂无达到证据门槛的独立结论";
    return findings.stream()
        .map(finding -> finding.title() + "：" + finding.explanation())
        .collect(Collectors.joining("；"));
  }

  protected String joined(List<String> values, String empty) {
    return values.isEmpty() ? empty : String.join("；", values);
  }

  private ReportChapter annualChapter(String number, YearAssessment year, ReportTopic topic) {
    return chapter(
        number,
        year.year() + "年" + topic.label() + "详解",
        year.ganZhi() + " · " + year.stage().label() + " · " + year.headline(),
        topic,
        List.of(annualPoint(year, topic)));
  }

  private ReportChapter chapter(
      String number,
      String title,
      String lead,
      ReportTopic topic,
      List<ReportPoint> points) {
    return new ReportChapter(
        number,
        title,
        lead,
        List.of(new ReportSection(title, topic.code(), points)));
  }

  private ReportPoint overviewPoint(ThreeYearAssessment assessment) {
    return new ReportPoint(
        "overview." + assessment.topic().code(),
        assessment.trajectory(),
        assessment.years().stream()
            .map(year -> year.year() + "年：" + year.headline())
            .toList(),
        assessment.years().stream()
            .flatMap(year -> year.evidence().stream().map(ReportEvidence::key))
            .distinct()
            .toList(),
        overviewInterpretation(assessment),
        edition == ReportEdition.PROFESSIONAL ? ReportCopy.get("presenter.overview.method") : "",
        assessment.years().get(0).realitySignals().stream().findFirst()
            .orElse(ReportCopy.get("presenter.signal.default")));
  }

  private ReportPoint annualPoint(YearAssessment year, ReportTopic topic) {
    return new ReportPoint(
        year.ruleKeys().isEmpty() ? "insufficient." + year.year() : String.join(" + ", year.ruleKeys()),
        year.headline() + "：" + year.conclusion(),
        year.evidence().stream().map(ReportEvidence::display).toList(),
        year.evidence().stream().map(ReportEvidence::key).toList(),
        annualInterpretation(year, topic),
        annualMethodNote(year),
        year.realitySignals().stream().findFirst().orElse(ReportCopy.get("presenter.signal.default")));
  }

  private ReportPoint routePoint(ThreeYearAssessment assessment) {
    return new ReportPoint(
        "route." + assessment.topic().code(),
        "三年行动优先级",
        assessment.priorities(),
        assessment.years().stream().flatMap(year -> year.ruleKeys().stream()).distinct().toList(),
        routeInterpretation(assessment),
        edition == ReportEdition.PROFESSIONAL ? ReportCopy.get("presenter.route.method") : "",
        ReportCopy.get("presenter.route.prompt"));
  }

  private ReportAppendix appendix(ReportProfile profile) {
    ReportPoint basis = new ReportPoint(
        "appendix.chart",
        "命盘基础资料",
        List.of(
            "四柱：" + profile.pillarsText(),
            "日主：" + profile.dayMaster(),
            "当前大运：" + profile.currentDaYun(),
            "当前流年：" + profile.currentLiuNian()),
        List.of("profile.pillars", "profile.day_master", "profile.dayun", "profile.annual"),
        ReportCopy.get("presenter.appendix.interpretation"),
        edition == ReportEdition.PROFESSIONAL ? ReportCopy.get("presenter.appendix.method") : "",
        ReportCopy.get("presenter.appendix.prompt"));
    return new ReportAppendix(
        "命盘与计算依据",
        List.of(new ReportSection("基础排盘", "appendix", List.of(basis))));
  }

  private String overviewLead(ThreeYearAssessment assessment) {
    return assessment.topic().label() + "以" + assessment.trajectory()
        + "为三年主线，以下结论按年度证据分别展开。";
  }

  private String routeLead(ThreeYearAssessment assessment) {
    return "行动路线只收录三年规则共同筛选后的优先事项："
        + joined(assessment.priorities(), "继续记录现实信号");
  }
}
