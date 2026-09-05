package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.relationship.RelationshipDimensionEvaluator;
import com.bazi.app.report.relationship.RelationshipFactExtractor;
import com.bazi.app.report.relationship.RelationshipNarrativePlan;
import com.bazi.app.report.relationship.RelationshipNarrativePlanner;
import com.bazi.app.report.relationship.RelationshipSingleNarrativePlanner;
import com.bazi.app.report.relationship.RelationshipPeriodArbitrator;
import com.bazi.app.report.relationship.RelationshipPeriodEvaluation;
import com.bazi.app.report.relationship.RelationshipStatus;
import com.bazi.app.report.overall.OverallNarrativePlanner;
import com.bazi.app.report.overall.OverallPeriodArbitrator;
import com.bazi.app.report.wealth.v3.DefaultWealthReportGenerator;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ReportReaderLanguageTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));
  private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
  private static final Path FIXTURES = Path.of(
      "src/test/resources/report/wealth-v2-baseline-inputs.json");
  private static final List<String> RETROSPECTIVE_ASSERTIONS = List.of(
      "已发生", "你去年已经", "你去年一定", "去年必然", "事实证明", "准确命中");
  private static final List<String> PLAIN_LANGUAGE_FORBIDDEN = List.of(
      "卡点", "抓手", "承接", "赋能", "闭环", "赛道", "协作节点", "交付");

  @Test
  void relationshipPlainCopyUsesDirectNaturalChinese() {
    RelationshipPeriodEvaluation evaluation = relationshipEvaluation();
    RelationshipNarrativePlanner planner = new RelationshipNarrativePlanner();

    for (RelationshipStatus status : RelationshipStatus.values()) {
      RelationshipNarrativePlan plan = planner.plan(evaluation, status);
      List<String> lines = readerLines(plan);
      String text = String.join("", lines);
      for (String forbidden : List.of(
          "现有证据不足", "先观察再判断", "卡点", "现在卡住了",
          "正缘", "烂桃花", "第三者", "必定结婚", "一定分手", "不忠",
          "责任边界", "协作节点", "复盘", "交付", "赋能", "承接", "推进",
          "${", "{{")) {
        assertFalse(text.contains(forbidden), forbidden + " in " + text);
      }
      for (String line : lines) {
        assertFalse(line.isBlank());
        // Annual judgments now contain a focus and up to two comparisons, not one stock sentence.
        // Keep the original short-sentence gate; do not allow longer sentences to hide verbosity.
        for (String sentence : line.split("(?<=[。！？])")) {
          assertTrue(sentence.length() <= 48, sentence.length() + " chars: " + sentence);
        }
      }
      plan.years().forEach(year -> {
        assertTrue(year.judgment().length() <= 120, year.judgment());
        assertTrue(year.judgment().split("[。！？]").length <= 3, year.judgment());
      });
      plan.years().stream()
          .flatMap(year -> Stream.concat(
              year.realitySignals().stream(), year.actions().stream()))
          .forEach(line -> assertTrue(
              Stream.of("你", "对方", "两个人", "夫妻", "家人").anyMatch(line::contains),
              "missing explicit subject: " + line));
    }
  }

  @Test
  void fourTopicTimelinePreflightCoversRequiredSamplesWithoutAcceptanceViolations()
      throws Exception {
    List<TimelineCase> samples = new ArrayList<>();
    addCareerSamples(samples);
    addWealthAndOverallSamples(samples);
    addRelationshipSamples(samples);

    assertEquals(31, samples.size());
    assertEquals(4, samples.stream().filter(sample -> sample.topic().equals("career")).count());
    assertEquals(12, samples.stream().filter(sample -> sample.topic().equals("wealth")).count());
    assertEquals(3, samples.stream().filter(sample -> sample.topic().equals("relationship")).count());
    assertEquals(12, samples.stream().filter(sample -> sample.topic().equals("overall")).count());

    List<String> failures = samples.stream()
        .flatMap(sample -> inspectTimeline(sample).stream()
            .map(failure -> sample.id() + ": " + failure))
        .toList();
    assertTrue(failures.isEmpty(), String.join("\n", failures));
    writePreflightArtifact(samples);
    System.out.printf(
        "FOUR_TOPIC_TIMELINE_PREFLIGHT samples=%d retrospectiveAssertions=0 emptyEvidence=0 "
            + "crossSectionDuplicateCopy=0 duplicateFutureAction=0 topicContamination=0%n",
        samples.size());
  }

  private void writePreflightArtifact(List<TimelineCase> samples) throws Exception {
    var output = JSON.createObjectNode();
    output.put("sampleCount", samples.size());
    output.put("careerSampleCount", count(samples, "career"));
    output.put("wealthSampleCount", count(samples, "wealth"));
    output.put("relationshipSampleCount", count(samples, "relationship"));
    output.put("overallSampleCount", count(samples, "overall"));
    output.put("retrospectiveAssertionViolationCount", 0);
    output.put("emptyEvidenceCount", 0);
    output.put("crossSectionDuplicateCount", 0);
    output.put("duplicateFutureActionCount", 0);
    output.put("topicContaminationCount", 0);
    // Cross-section language checks do not establish cross-chart diversity. Record visible
    // repetitions separately; WealthRetrospectiveCollisionTest gates different-evidence collisions.
    Map<String, List<String>> wealthReviews = new LinkedHashMap<>();
    samples.stream().filter(sample -> sample.topic().equals("wealth"))
        .forEach(sample -> wealthReviews
            .computeIfAbsent(pastReviewCopy(sample.timeline().past()), key -> new ArrayList<>())
            .add(sample.id()));
    output.put("wealthUniqueVisiblePastReviewCount", wealthReviews.size());
    output.set("wealthRepeatedPastReviewGroups", JSON.valueToTree(wealthReviews.values().stream()
        .filter(group -> group.size() > 1).toList()));
    output.set("cases", JSON.valueToTree(samples));
    Files.writeString(
        Path.of("target/four-topic-timeline-preflight.json"),
        JSON.writerWithDefaultPrettyPrinter().writeValueAsString(output) + "\n");
  }

  private long count(List<TimelineCase> samples, String topic) {
    return samples.stream().filter(sample -> sample.topic().equals(topic)).count();
  }

  private void addCareerSamples(List<TimelineCase> samples) {
    PaipanRequest request = request();
    PaipanResultDto chart = new BaziService().paipan(request);
    AnnualContextFactory factory = new AnnualContextFactory(CLOCK);
    AnnualPeriodAssessor assessor = new AnnualPeriodAssessor(CLOCK, new AnnualRuleCatalog());
    List<CareerContext> contexts = List.of(
        CareerContext.fromCodes("employed", "promotion", "smooth"),
        CareerContext.fromCodes("self_employed", "stability", "high_pressure"),
        CareerContext.fromCodes("job_seeking", "job_change", "stalled"),
        CareerContext.fromCodes("studying", "transition", "preparing_change"));
    for (CareerContext context : contexts) {
      AnnualPeriodAssessment product = assessor.assess(
          request, chart, ReportTopic.CAREER, ReportHorizon.of(2), context);
      YearAssessment previous = assessor.assessYear(
          factory.createYear(request, chart, product.years().get(0).year() - 1),
          ReportTopic.CAREER,
          context);
      NarrativeTimeline timeline = new CareerNarrativePlanner()
          .plan(ThreeYearAssessment.from(product), context, previous)
          .timeline();
      samples.add(new TimelineCase(
          "career-" + context.status().name().toLowerCase(), "career", timeline, 1));
    }
  }

  private void addWealthAndOverallSamples(List<TimelineCase> samples) throws Exception {
    JsonNode root = JSON.readTree(FIXTURES.toFile());
    ZoneId zone = ZoneId.of(root.get("zoneId").asText());
    for (JsonNode fixture : root.get("fullBirthCases")) {
      String id = fixture.get("id").asText();
      LocalDate asOf = LocalDate.parse(fixture.get("asOf").asText());
      Clock clock = Clock.fixed(asOf.atStartOfDay(zone).toInstant(), zone);
      PaipanRequest request = JSON.treeToValue(fixture.get("request"), PaipanRequest.class);
      PaipanResultDto chart = new BaziService().paipan(request);
      AnnualContextFactory factory = new AnnualContextFactory(clock);
      List<AnnualContext> product = factory.create(request, chart, ReportHorizon.WEALTH_PRODUCT);
      AnnualContext previousContext = factory.createYear(
          request, chart, product.get(0).year() - 1);

      NarrativeTimeline wealthTimeline = new DefaultWealthReportGenerator()
          .generate(chart, previousContext, product, asOf)
          .timeline();
      samples.add(new TimelineCase(id, "wealth", wealthTimeline, 2));

      List<AnnualContext> analysis = new ArrayList<>();
      analysis.add(previousContext);
      analysis.addAll(product);
      OverallPeriodArbitrator arbitrator = new OverallPeriodArbitrator();
      var previous = arbitrator.arbitrate(analysis).years().get(0);
      var overall = new OverallNarrativePlanner().plan(arbitrator.arbitrate(product), previous);
      samples.add(new TimelineCase(id, "overall", overall.timeline(), 2));
    }
  }

  private void addRelationshipSamples(List<TimelineCase> samples) {
    PaipanRequest request = request();
    PaipanResultDto chart = new BaziService().paipan(request);
    AnnualContextFactory factory = new AnnualContextFactory(CLOCK);
    List<AnnualContext> product = factory.create(
        request, chart, ReportHorizon.RELATIONSHIP_PRODUCT);
    List<AnnualContext> analysis = new ArrayList<>();
    analysis.add(factory.createYear(request, chart, product.get(0).year() - 1));
    analysis.addAll(product);
    var evaluations = new RelationshipDimensionEvaluator().evaluate(
        new RelationshipFactExtractor().extract(request, chart, analysis));
    RelationshipPeriodArbitrator arbitrator = new RelationshipPeriodArbitrator();
    RelationshipPeriodEvaluation.Year previous = arbitrator.arbitrate(evaluations).years().get(0);
    RelationshipPeriodEvaluation partnered = arbitrator.arbitrate(
        evaluations.subList(1, evaluations.size()));
    for (RelationshipStatus status : List.of(RelationshipStatus.DATING, RelationshipStatus.MARRIED)) {
      NarrativeTimeline timeline = new RelationshipNarrativePlanner()
          .plan(partnered, status, previous)
          .timeline();
      samples.add(new TimelineCase(
          "relationship-" + status.code(), "relationship", timeline, 2));
    }
    RelationshipPeriodEvaluation single = arbitrator.arbitrate(evaluations.subList(1, 3));
    NarrativeTimeline singleTimeline = new RelationshipSingleNarrativePlanner()
        .plan(single, previous)
        .timeline();
    samples.add(new TimelineCase("relationship-single", "relationship", singleTimeline, 1));
  }

  private List<String> inspectTimeline(TimelineCase sample) {
    List<String> failures = new ArrayList<>();
    NarrativeTimeline timeline = sample.timeline();
    if (timeline == null) return List.of("缺少时间线");
    if (timeline.past().year() != timeline.present().year() - 1) {
      failures.add("过去年度不是当前年度的上一年");
    }
    if (timeline.past().checkpoints().size() != 2) {
      failures.add("过去核对点不是两条");
    }
    if (timeline.future().size() != sample.futureCount()) {
      failures.add("未来行动数量不符合产品年限");
    }
    List<String> copy = timelineCopy(timeline);
    if (copy.stream().anyMatch(value -> value == null || value.isBlank())) {
      failures.add("存在空文案");
    }
    if (new LinkedHashSet<>(copy).size() != copy.size()) {
      failures.add("跨区段出现完全重复文案");
    }
    if (timeline.past().evidenceKeys().isEmpty()
        || timeline.present().evidenceKeys().isEmpty()
        || timeline.future().stream().anyMatch(step -> step.evidenceKeys().isEmpty())) {
      failures.add("存在空依据");
    }
    String past = pastReviewCopy(timeline.past());
    RETROSPECTIVE_ASSERTIONS.stream().filter(past::contains)
        .forEach(word -> failures.add("过去回看出现确定断言：" + word));
    String all = String.join("", copy);
    PLAIN_LANGUAGE_FORBIDDEN.stream().filter(all::contains)
        .forEach(word -> failures.add("出现工作黑话：" + word));
    contamination(sample.topic()).stream().filter(all::contains)
        .forEach(word -> failures.add("出现主题串味：" + word));
    List<String> futureActions = timeline.future().stream()
        .map(NarrativeTimeline.FutureStep::action)
        .toList();
    if (new LinkedHashSet<>(futureActions).size() != futureActions.size()) {
      failures.add("未来行动出现重复");
    }
    return List.copyOf(failures);
  }

  private List<String> contamination(String topic) {
    return switch (topic) {
      case "wealth" -> List.of("职责", "成果", "岗位", "升职", "简历", "投递", "面试", "作品");
      case "relationship" -> List.of("岗位", "升职", "简历", "投递", "面试", "客户", "回款", "项目收入");
      case "career" -> List.of("约会", "家务", "夫妻", "结婚", "分手", "第三者");
      default -> List.of();
    };
  }

  private List<String> timelineCopy(NarrativeTimeline timeline) {
    List<String> copy = new ArrayList<>();
    copy.add(timeline.past().headline());
    copy.addAll(timeline.past().checkpoints());
    copy.add(timeline.past().bridge());
    copy.add(timeline.present().headline());
    copy.add(timeline.present().judgment());
    copy.add(timeline.present().priority());
    timeline.future().forEach(step -> {
      copy.add(step.headline());
      copy.add(step.action());
    });
    return List.copyOf(copy);
  }

  private String pastReviewCopy(NarrativeTimeline.PastReview past) {
    List<String> lines = new ArrayList<>();
    lines.add(past.headline());
    lines.addAll(past.checkpoints());
    lines.add(past.bridge());
    return String.join("\n", lines);
  }

  private PaipanRequest request() {
    return new PaipanRequest(
        "林先生", "male", "1995-10-08T14:30:00", "上海", false);
  }

  private RelationshipPeriodEvaluation relationshipEvaluation() {
    PaipanRequest request = new PaipanRequest(
        "林先生", "male", "1995-10-08T14:30:00", "上海", false);
    PaipanResultDto chart = new BaziService().paipan(request);
    return new RelationshipPeriodArbitrator().arbitrate(
        new RelationshipDimensionEvaluator().evaluate(
            new RelationshipFactExtractor().extract(
                request,
                chart,
                new AnnualContextFactory(CLOCK)
                    .create(request, chart, ReportHorizon.RELATIONSHIP_PRODUCT))));
  }

  private List<String> readerLines(RelationshipNarrativePlan plan) {
    List<String> lines = new ArrayList<>();
    lines.add(plan.thesis());
    lines.add(plan.summary());
    plan.dimensions().forEach(item -> {
      lines.add(item.status());
      lines.add(item.judgment());
    });
    if (plan.mainRisk() != null) lines.add(plan.mainRisk().judgment());
    plan.years().forEach(year -> {
      lines.add(year.focus());
      lines.add(year.judgment());
      if (year.mainLimit() != null) lines.add(year.mainLimit());
      lines.addAll(year.realitySignals());
      lines.addAll(year.actions());
      lines.add(year.transition());
    });
    return List.copyOf(lines);
  }

  private record TimelineCase(
      String id,
      String topic,
      NarrativeTimeline timeline,
      int futureCount) {}
}
