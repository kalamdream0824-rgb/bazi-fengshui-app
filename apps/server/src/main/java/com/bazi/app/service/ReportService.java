package com.bazi.app.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.config.BusinessException;
import com.bazi.app.domain.BaziReport;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.ReportDto;
import com.bazi.app.dto.ReportPreviewRequest;
import com.bazi.app.mapper.BaziReportMapper;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.CareerContext;
import com.bazi.app.report.CareerNarrativePlan;
import com.bazi.app.report.CareerNarrativePlanner;
import com.bazi.app.report.ReportContent;
import com.bazi.app.report.ReportEdition;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.ReportTopic;
import com.bazi.app.report.ThreeYearAssessment;
import com.bazi.app.report.ThreeYearAssessor;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.bazi.app.report.wealth.WealthFactExtractor;
import com.bazi.app.report.wealth.WealthNarrativePlan;
import com.bazi.app.report.wealth.WealthNarrativePlanner;
import com.bazi.app.report.wealth.WealthPathEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

  public static final String CAREER_CONTENT_VERSION = "career-narrative-v3";
  public static final String WEALTH_CONTENT_VERSION = "wealth-narrative-v2";

  private final BaziService baziService;
  private final BaziReportMapper mapper;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final ThreeYearAssessor assessor;
  private final CareerNarrativePlanner careerPlanner = new CareerNarrativePlanner();
  private final WealthNarrativePlanner wealthPlanner = new WealthNarrativePlanner();
  private final WealthFactExtractor wealthFactExtractor = new WealthFactExtractor();
  private final WealthPathEvaluator wealthPathEvaluator = new WealthPathEvaluator();

  public ReportService(BaziService baziService, BaziReportMapper mapper, ObjectMapper objectMapper) {
    this.baziService = baziService;
    this.mapper = mapper;
    this.objectMapper = objectMapper;
    this.clock = Clock.systemDefaultZone();
    this.assessor = new ThreeYearAssessor(clock, new AnnualRuleCatalog());
  }

  @Transactional
  public ReportDto create(Long userId, ReportPreviewRequest request) throws Exception {
    ReportTopic topic = ReportTopic.fromCode(request.topic());
    ReportEdition edition = ReportEdition.fromCode(request.edition());
    if (topic != ReportTopic.CAREER && topic != ReportTopic.WEALTH) {
      throw new BusinessException("REPORT_PAGE_TOPIC_UNSUPPORTED", "当前页面阅读版先开放事业与财富主题");
    }
    if (topic == ReportTopic.CAREER && request.careerContext() == null) {
      throw new BusinessException("CAREER_CONTEXT_REQUIRED", "生成事业命书前，请完成事业状态问卷");
    }
    if (topic == ReportTopic.WEALTH && edition == ReportEdition.PROFESSIONAL) {
      throw new BusinessException(
          "WEALTH_PROFESSIONAL_NOT_AVAILABLE", "财富专业版仍在设计中，当前只开放通俗版");
    }
    PaipanResultDto chart = baziService.paipan(request.request());
    ReportContent content;
    String contentVersion;
    Object contextRequest;
    if (topic == ReportTopic.CAREER) {
      CareerContext context = request.careerContext().toDomain();
      ThreeYearAssessment assessment = assessor.assess(request.request(), chart, topic, context);
      content = careerPlanner.plan(assessment, context);
      contentVersion = CAREER_CONTENT_VERSION;
      contextRequest = request.careerContext();
    } else {
      content = wealthPlanner.plan(wealthPathEvaluator.evaluate(wealthFactExtractor.extract(
          chart,
          new AnnualContextFactory(clock)
              .create(request.request(), chart, ReportHorizon.WEALTH_PRODUCT))));
      contentVersion = WEALTH_CONTENT_VERSION;
      contextRequest = Map.of(
          "source", "system",
          "horizonYears", ReportHorizon.WEALTH_PRODUCT.years());
    }
    LocalDateTime now = LocalDateTime.now();

    BaziReport report = new BaziReport();
    report.setUserId(userId);
    report.setSubject(subject(request.request().name()));
    report.setTopic(topic.code());
    report.setEdition(edition.code());
    report.setStatus("ready");
    report.setContentVersion(contentVersion);
    report.setRequestJson(objectMapper.writeValueAsString(request.request()));
    report.setContextJson(objectMapper.writeValueAsString(contextRequest));
    report.setContentJson(objectMapper.writeValueAsString(content));
    report.setCreatedAt(now);
    report.setGeneratedAt(now);
    mapper.insert(report);
    return toDto(report);
  }

  public List<ReportDto> list(Long userId) throws Exception {
    List<BaziReport> reports = mapper.selectList(new QueryWrapper<BaziReport>()
        .eq("user_id", userId)
        .orderByDesc("created_at"));
    List<ReportDto> result = new ArrayList<>();
    for (BaziReport report : reports) result.add(toDto(report));
    return result;
  }

  public Optional<ReportDto> get(Long userId, Long id) throws Exception {
    BaziReport report = mapper.selectOne(new QueryWrapper<BaziReport>()
        .eq("id", id)
        .eq("user_id", userId));
    return report == null ? Optional.empty() : Optional.of(toDto(report));
  }

  private ReportDto toDto(BaziReport report) throws Exception {
    ReportContent content = WEALTH_CONTENT_VERSION.equals(report.getContentVersion())
        ? objectMapper.readValue(report.getContentJson(), WealthNarrativePlan.class)
        : objectMapper.readValue(report.getContentJson(), CareerNarrativePlan.class);
    return new ReportDto(
        report.getId(),
        report.getSubject(),
        report.getTopic(),
        report.getEdition(),
        report.getStatus(),
        report.getContentVersion(),
        content,
        report.getCreatedAt(),
        report.getGeneratedAt());
  }

  private String subject(String name) {
    return name == null || name.isBlank() ? "命主" : name.trim();
  }
}
