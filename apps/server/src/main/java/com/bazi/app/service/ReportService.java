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
import com.bazi.app.report.overall.OverallNarrativePlan;
import com.bazi.app.report.overall.OverallNarrativePlanner;
import com.bazi.app.report.overall.OverallPeriodArbitrator;
import com.bazi.app.report.relationship.RelationshipDimensionEvaluator;
import com.bazi.app.report.relationship.RelationshipFactExtractor;
import com.bazi.app.report.relationship.RelationshipNarrativePlan;
import com.bazi.app.report.relationship.RelationshipNarrativePlanner;
import com.bazi.app.report.relationship.RelationshipPeriodArbitrator;
import com.bazi.app.report.relationship.RelationshipStatus;
import com.bazi.app.report.relationship.RelationshipSingleNarrativePlan;
import com.bazi.app.report.relationship.RelationshipSingleNarrativePlanner;
import com.bazi.app.report.wealth.WealthNarrativePlan;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3;
import com.bazi.app.report.wealth.v3.WealthReportGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

  private static final ZoneId WEALTH_ZONE = ZoneId.of("Asia/Shanghai");
  public static final String CAREER_CONTENT_VERSION = "career-narrative-v3";
  public static final String OVERALL_CONTENT_VERSION = "overall-narrative-v1.1";
  public static final String OVERALL_V1_CONTENT_VERSION = "overall-narrative-v1";
  public static final String WEALTH_CONTENT_VERSION = "wealth-narrative-v3";
  public static final String WEALTH_V2_CONTENT_VERSION = "wealth-narrative-v2";
  public static final String RELATIONSHIP_CONTENT_VERSION = "relationship-narrative-v1";
  public static final String RELATIONSHIP_SINGLE_CONTENT_VERSION = "relationship-single-v1";

  private final BaziService baziService;
  private final BaziReportMapper mapper;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final ThreeYearAssessor assessor;
  private final WealthReportGenerator wealthReportGenerator;
  private final ReportEntitlementService reportEntitlementService;
  private final CareerNarrativePlanner careerPlanner = new CareerNarrativePlanner();
  private final RelationshipFactExtractor relationshipFactExtractor = new RelationshipFactExtractor();
  private final RelationshipDimensionEvaluator relationshipDimensionEvaluator =
      new RelationshipDimensionEvaluator();
  private final RelationshipPeriodArbitrator relationshipPeriodArbitrator =
      new RelationshipPeriodArbitrator();
  private final RelationshipNarrativePlanner relationshipPlanner =
      new RelationshipNarrativePlanner();

  public ReportService(BaziService baziService, BaziReportMapper mapper, ObjectMapper objectMapper,
      WealthReportGenerator wealthReportGenerator, ReportEntitlementService reportEntitlementService) {
    this.baziService = baziService;
    this.mapper = mapper;
    this.objectMapper = objectMapper;
    this.clock = Clock.systemDefaultZone();
    this.assessor = new ThreeYearAssessor(clock, new AnnualRuleCatalog());
    this.wealthReportGenerator = wealthReportGenerator;
    this.reportEntitlementService = reportEntitlementService;
  }

  @Transactional
  public ReportDto create(Long userId, ReportPreviewRequest request) throws Exception {
    return toDto(createStored(userId, request, "ready", true));
  }

  @Transactional
  public BaziReport createLocked(Long userId, ReportPreviewRequest request) throws Exception {
    return createStored(userId, request, "locked", false);
  }

  private BaziReport createStored(
      Long userId,
      ReportPreviewRequest request,
      String status,
      boolean requireMemberSlot) throws Exception {
    ReportTopic topic = ReportTopic.fromCode(request.topic());
    ReportEdition edition = ReportEdition.fromCode(request.edition());
    if (topic == ReportTopic.OVERALL && edition == ReportEdition.PROFESSIONAL) {
      throw new BusinessException(
          "OVERALL_PROFESSIONAL_NOT_AVAILABLE", "综合专业版仍在设计中，当前只开放通俗版验收");
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
    if (requireMemberSlot) {
      reportEntitlementService.requireDirectGenerationAccess(userId);
    }
    PaipanResultDto chart = baziService.paipan(request.request());
    ReportContent content;
    String contentVersion;
    Object contextRequest;
    if (topic == ReportTopic.OVERALL) {
      var contexts = new AnnualContextFactory(clock.withZone(WEALTH_ZONE))
          .create(request.request(), chart, ReportHorizon.of(3));
      content = new OverallNarrativePlanner().plan(new OverallPeriodArbitrator().arbitrate(contexts));
      contentVersion = OVERALL_CONTENT_VERSION;
      contextRequest = Map.of("source", "system", "horizonYears", 3);
    } else if (topic == ReportTopic.CAREER) {
      CareerContext context = request.careerContext().toDomain();
      ThreeYearAssessment assessment = assessor.assess(request.request(), chart, topic, context);
      content = careerPlanner.plan(assessment, context);
      contentVersion = CAREER_CONTENT_VERSION;
      contextRequest = request.careerContext();
    } else if (topic == ReportTopic.WEALTH) {
      Clock wealthClock = clock.withZone(WEALTH_ZONE);
      var contexts = new AnnualContextFactory(wealthClock)
          .create(request.request(), chart, ReportHorizon.WEALTH_PRODUCT);
      LocalDate asOf = LocalDate.now(wealthClock);
      WealthNarrativeV3 wealthContent;
      try {
        wealthContent = wealthReportGenerator.generate(chart, contexts, asOf);
      } catch (IllegalArgumentException error) {
        throw new BusinessException("REPORT_GENERATION_UNAVAILABLE",
            "本次命书暂未生成成功，未扣除费用或使用次数，请稍后再试");
      }
      content = wealthContent;
      contentVersion = WEALTH_CONTENT_VERSION;
      contextRequest = Map.of(
          "source", "system",
          "horizonYears", ReportHorizon.WEALTH_PRODUCT.years(),
          "asOf", asOf.toString(),
          "calculationVersion", wealthContent.calculationVersion(),
          "policyVersion", wealthContent.policyVersion(),
          "copyVersion", wealthContent.copyVersion());
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
    if (requireMemberSlot) {
      // The ready report is the usage record. Lock and check immediately before insert so any
      // generation failure consumes nothing, while concurrent requests for one member stay serial.
      reportEntitlementService.requireSuccessfulReportSlot(userId);
    }
    LocalDateTime now = LocalDateTime.now();

    BaziReport report = new BaziReport();
    report.setUserId(userId);
    report.setSubject(subject(request.request().name()));
    report.setTopic(topic.code());
    report.setEdition(edition.code());
    report.setStatus(status);
    report.setContentVersion(contentVersion);
    report.setRequestJson(objectMapper.writeValueAsString(request.request()));
    report.setContextJson(objectMapper.writeValueAsString(contextRequest));
    report.setContentJson(objectMapper.writeValueAsString(content));
    report.setCreatedAt(now);
    report.setGeneratedAt(now);
    mapper.insert(report);
    return report;
  }

  public List<ReportDto> list(Long userId) throws Exception {
    List<BaziReport> reports = mapper.selectList(new QueryWrapper<BaziReport>()
        .eq("user_id", userId)
        .eq("status", "ready")
        .orderByDesc("created_at"));
    List<ReportDto> result = new ArrayList<>();
    for (BaziReport report : reports) result.add(toDto(report));
    return result;
  }

  public Optional<ReportDto> get(Long userId, Long id) throws Exception {
    BaziReport report = mapper.selectOne(new QueryWrapper<BaziReport>()
        .eq("id", id)
        .eq("user_id", userId)
        .eq("status", "ready"));
    return report == null ? Optional.empty() : Optional.of(toDto(report));
  }

  private ReportDto toDto(BaziReport report) throws Exception {
    ReportContent content;
    if (Set.of(OVERALL_CONTENT_VERSION, OVERALL_V1_CONTENT_VERSION)
        .contains(report.getContentVersion())) {
      content = objectMapper.readValue(report.getContentJson(), OverallNarrativePlan.class);
    } else if (WEALTH_CONTENT_VERSION.equals(report.getContentVersion())) {
      content = objectMapper.readValue(report.getContentJson(), WealthNarrativeV3.class);
    } else if (WEALTH_V2_CONTENT_VERSION.equals(report.getContentVersion())) {
      content = objectMapper.readValue(report.getContentJson(), WealthNarrativePlan.class);
    } else if (RELATIONSHIP_SINGLE_CONTENT_VERSION.equals(report.getContentVersion())) {
      content = objectMapper.readValue(report.getContentJson(), RelationshipSingleNarrativePlan.class);
    } else if (RELATIONSHIP_CONTENT_VERSION.equals(report.getContentVersion())) {
      content = objectMapper.readValue(report.getContentJson(), RelationshipNarrativePlan.class);
    } else if (Set.of("career-narrative-v1", "career-narrative-v2", CAREER_CONTENT_VERSION,
        "wealth-narrative-v1").contains(report.getContentVersion())) {
      content = objectMapper.readValue(report.getContentJson(), CareerNarrativePlan.class);
    } else {
      throw new BusinessException("REPORT_CONTENT_VERSION_UNSUPPORTED", "暂不支持读取这个命书版本");
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
