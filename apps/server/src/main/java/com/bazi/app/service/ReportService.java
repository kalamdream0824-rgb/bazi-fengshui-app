package com.bazi.app.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.config.BusinessException;
import com.bazi.app.domain.BaziReport;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.ReportDto;
import com.bazi.app.dto.ReportPreviewRequest;
import com.bazi.app.mapper.BaziReportMapper;
import com.bazi.app.report.CareerContext;
import com.bazi.app.report.CareerNarrativePlan;
import com.bazi.app.report.CareerNarrativePlanner;
import com.bazi.app.report.ReportEdition;
import com.bazi.app.report.ReportTopic;
import com.bazi.app.report.ThreeYearAssessment;
import com.bazi.app.report.ThreeYearAssessor;
import com.bazi.app.report.WealthContext;
import com.bazi.app.report.WealthNarrativePlanner;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

  public static final String CAREER_CONTENT_VERSION = "career-narrative-v3";
  public static final String WEALTH_CONTENT_VERSION = "wealth-narrative-v1";

  private final BaziService baziService;
  private final BaziReportMapper mapper;
  private final ObjectMapper objectMapper;
  private final ThreeYearAssessor assessor;
  private final CareerNarrativePlanner careerPlanner = new CareerNarrativePlanner();
  private final WealthNarrativePlanner wealthPlanner = new WealthNarrativePlanner();

  public ReportService(BaziService baziService, BaziReportMapper mapper, ObjectMapper objectMapper) {
    this.baziService = baziService;
    this.mapper = mapper;
    this.objectMapper = objectMapper;
    this.assessor = new ThreeYearAssessor(Clock.systemDefaultZone(), new AnnualRuleCatalog());
  }

  public ReportDto create(Long userId, ReportPreviewRequest request) throws Exception {
    ReportTopic topic = ReportTopic.fromCode(request.topic());
    ReportEdition edition = ReportEdition.fromCode(request.edition());
    if (topic != ReportTopic.CAREER && topic != ReportTopic.WEALTH) {
      throw new BusinessException("REPORT_PAGE_TOPIC_UNSUPPORTED", "当前页面阅读版先开放事业与财富主题");
    }
    if (topic == ReportTopic.CAREER && request.careerContext() == null) {
      throw new BusinessException("CAREER_CONTEXT_REQUIRED", "生成事业命书前，请完成事业状态问卷");
    }
    if (topic == ReportTopic.WEALTH && request.wealthContext() == null) {
      throw new BusinessException("WEALTH_CONTEXT_REQUIRED", "生成财富命书前，请完成财富状态问卷");
    }
    PaipanResultDto chart = baziService.paipan(request.request());
    CareerNarrativePlan content;
    String contentVersion;
    Object contextRequest;
    if (topic == ReportTopic.CAREER) {
      CareerContext context = request.careerContext().toDomain();
      ThreeYearAssessment assessment = assessor.assess(request.request(), chart, topic, context);
      content = careerPlanner.plan(assessment, context);
      contentVersion = CAREER_CONTENT_VERSION;
      contextRequest = request.careerContext();
    } else {
      WealthContext context = request.wealthContext().toDomain();
      ThreeYearAssessment assessment = assessor.assess(request.request(), chart, topic);
      content = wealthPlanner.plan(assessment, context);
      contentVersion = WEALTH_CONTENT_VERSION;
      contextRequest = request.wealthContext();
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
    return new ReportDto(
        report.getId(),
        report.getSubject(),
        report.getTopic(),
        report.getEdition(),
        report.getStatus(),
        report.getContentVersion(),
        objectMapper.readValue(report.getContentJson(), CareerNarrativePlan.class),
        report.getCreatedAt(),
        report.getGeneratedAt());
  }

  private String subject(String name) {
    return name == null || name.isBlank() ? "命主" : name.trim();
  }
}
