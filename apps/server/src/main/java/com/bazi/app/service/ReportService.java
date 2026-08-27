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
import com.bazi.app.report.relationship.RelationshipDimensionEvaluator;
import com.bazi.app.report.relationship.RelationshipFactExtractor;
import com.bazi.app.report.relationship.RelationshipNarrativePlan;
import com.bazi.app.report.relationship.RelationshipNarrativePlanner;
import com.bazi.app.report.relationship.RelationshipPeriodArbitrator;
import com.bazi.app.report.relationship.RelationshipStatus;
import com.bazi.app.report.relationship.RelationshipSingleNarrativePlan;
import com.bazi.app.report.relationship.RelationshipSingleNarrativePlanner;
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
  public static final String RELATIONSHIP_CONTENT_VERSION = "relationship-narrative-v1";
  public static final String RELATIONSHIP_SINGLE_CONTENT_VERSION = "relationship-single-v1";

  private final BaziService baziService;
  private final BaziReportMapper mapper;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final ThreeYearAssessor assessor;
  private final CareerNarrativePlanner careerPlanner = new CareerNarrativePlanner();
  private final WealthNarrativePlanner wealthPlanner = new WealthNarrativePlanner();
  private final WealthFactExtractor wealthFactExtractor = new WealthFactExtractor();
  private final WealthPathEvaluator wealthPathEvaluator = new WealthPathEvaluator();
  private final RelationshipFactExtractor relationshipFactExtractor = new RelationshipFactExtractor();
  private final RelationshipDimensionEvaluator relationshipDimensionEvaluator =
      new RelationshipDimensionEvaluator();
  private final RelationshipPeriodArbitrator relationshipPeriodArbitrator =
      new RelationshipPeriodArbitrator();
  private final RelationshipNarrativePlanner relationshipPlanner =
      new RelationshipNarrativePlanner();

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
    if (topic != ReportTopic.CAREER
        && topic != ReportTopic.WEALTH
        && topic != ReportTopic.RELATIONSHIP) {
      throw new BusinessException("REPORT_PAGE_TOPIC_UNSUPPORTED", "当前页面阅读版尚未开放这个主题");
    }
    if (topic == ReportTopic.CAREER && request.careerContext() == null) {
      throw new BusinessException("CAREER_CONTEXT_REQUIRED", "生成事业命书前，请完成事业状态问卷");
    }
    if (topic == ReportTopic.WEALTH && edition == ReportEdition.PROFESSIONAL) {
      throw new BusinessException(
          "WEALTH_PROFESSIONAL_NOT_AVAILABLE", "财富专业版仍在设计中，当前只开放通俗版");
    }
    RelationshipStatus relationshipStatus = null;
    if (topic == ReportTopic.RELATIONSHIP) {
      if (edition == ReportEdition.PROFESSIONAL) {
        throw new BusinessException(
            "RELATIONSHIP_PROFESSIONAL_NOT_AVAILABLE", "感情专业版仍在设计中，当前只开放通俗版");
      }
      if (request.relationshipContext() == null) {
        throw new BusinessException(
            "RELATIONSHIP_CONTEXT_REQUIRED", "生成感情命书前，请选择当前关系状态");
      }
      try {
        relationshipStatus = request.relationshipContext().toDomain();
      } catch (IllegalArgumentException error) {
        throw new BusinessException(
            "RELATIONSHIP_STATUS_INVALID", "当前关系状态不在支持范围内");
      }
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
    } else if (topic == ReportTopic.WEALTH) {
      content = wealthPlanner.plan(wealthPathEvaluator.evaluate(wealthFactExtractor.extract(
          chart,
          new AnnualContextFactory(clock)
              .create(request.request(), chart, ReportHorizon.WEALTH_PRODUCT))));
      contentVersion = WEALTH_CONTENT_VERSION;
      contextRequest = Map.of(
          "source", "system",
          "horizonYears", ReportHorizon.WEALTH_PRODUCT.years());
    } else {
      boolean single = relationshipStatus == RelationshipStatus.SINGLE;
      var period = relationshipPeriodArbitrator.arbitrate(
              relationshipDimensionEvaluator.evaluate(
                  relationshipFactExtractor.extract(
                      request.request(),
                      chart,
                      new AnnualContextFactory(clock).create(
                          request.request(), chart, single
                              ? ReportHorizon.RELATIONSHIP_SINGLE_PRODUCT : ReportHorizon.RELATIONSHIP_PRODUCT))));
      if (single) {
        content = new RelationshipSingleNarrativePlanner().plan(period);
        contentVersion = RELATIONSHIP_SINGLE_CONTENT_VERSION;
      } else {
        RelationshipNarrativePlan relationshipContent = relationshipPlanner.plan(period, relationshipStatus);
        validateRelationshipContent(relationshipContent);
        content = relationshipContent;
        contentVersion = RELATIONSHIP_CONTENT_VERSION;
      }
      contextRequest = request.relationshipContext();
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
    ReportContent content;
    if (WEALTH_CONTENT_VERSION.equals(report.getContentVersion())) {
      content = objectMapper.readValue(report.getContentJson(), WealthNarrativePlan.class);
    } else if (RELATIONSHIP_SINGLE_CONTENT_VERSION.equals(report.getContentVersion())) {
      content = objectMapper.readValue(report.getContentJson(), RelationshipSingleNarrativePlan.class);
    } else if (RELATIONSHIP_CONTENT_VERSION.equals(report.getContentVersion())) {
      content = objectMapper.readValue(report.getContentJson(), RelationshipNarrativePlan.class);
    } else {
      content = objectMapper.readValue(report.getContentJson(), CareerNarrativePlan.class);
    }
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

  private void validateRelationshipContent(RelationshipNarrativePlan content) {
    boolean primaryHasEvidence = content.dimensions().stream()
        .filter(dimension -> dimension.code().equals(content.primaryDimensionCode()))
        .anyMatch(dimension -> !dimension.supportingEvidenceKeys().isEmpty()
            || !dimension.limitingEvidenceKeys().isEmpty());
    if (content.horizonYears() != ReportHorizon.RELATIONSHIP_PRODUCT.years()
        || content.years().size() != ReportHorizon.RELATIONSHIP_PRODUCT.years()
        || content.evidenceKeys().isEmpty()
        || !primaryHasEvidence) {
      throw new BusinessException(
          "RELATIONSHIP_CONTENT_INVALID", "感情命书内容校验失败，请稍后重试");
    }
  }
}
