package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CareerNarrativePlannerTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"),
      ZoneId.of("Asia/Shanghai"));

  private PaipanRequest request;
  private PaipanResultDto chart;
  private ThreeYearAssessor assessor;
  private CareerNarrativePlanner planner;

  @BeforeEach
  void setUp() {
    request = new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
    chart = new BaziService().paipan(request);
    assessor = new ThreeYearAssessor(CLOCK, new AnnualRuleCatalog());
    planner = new CareerNarrativePlanner();
  }

  @Test
  void keepsOneFocusTwoReasonsAndTwoActionsPerYear() {
    CareerContext context = CareerContext.fromCodes("job_seeking", "job_change", "stalled");
    ThreeYearAssessment assessment = assessor.assess(request, chart, ReportTopic.CAREER, context);

    CareerNarrativePlan plan = planner.plan(assessment, context);

    assertEquals(2, plan.years().size());
    assertFalse(plan.thesis().isBlank());
    for (CareerNarrativePlan.YearNarrative year : plan.years()) {
      assertFalse(year.headline().isBlank());
      assertFalse(year.verdict().isBlank());
      assertEquals(2, year.reasons().size());
      assertEquals(2, year.actions().size());
      assertTrue(year.evidenceKeys().size() >= 2);
      assertTrue(year.verdict().length() <= 70, year.verdict());
    }
  }

  @Test
  void keepsLegacyPlannerCallsWithoutATimeline() {
    CareerContext context = CareerContext.fromCodes("job_seeking", "job_change", "stalled");

    CareerNarrativePlan plan = planner.plan(
        assessor.assess(request, chart, ReportTopic.CAREER, context), context);

    assertNull(plan.timeline());
  }

  @Test
  void attachesTimelineWhenPreviousYearAssessmentIsProvided() {
    CareerContext context = CareerContext.fromCodes("job_seeking", "job_change", "stalled");
    ThreeYearAssessment assessment = assessor.assess(request, chart, ReportTopic.CAREER, context);
    AnnualContext previousContext = new AnnualContextFactory(CLOCK)
        .createYear(request, chart, assessment.years().get(0).year() - 1);
    YearAssessment previous = new AnnualPeriodAssessor(CLOCK, new AnnualRuleCatalog())
        .assessYear(previousContext, ReportTopic.CAREER, context);

    CareerNarrativePlan plan = planner.plan(assessment, context, previous);

    assertNotNull(plan.timeline());
    assertEquals(assessment.years().get(0).year(), plan.timeline().present().year());
  }

  @Test
  void readsLegacyJsonThatDoesNotContainTimeline() throws Exception {
    CareerContext context = CareerContext.fromCodes("job_seeking", "job_change", "stalled");
    ObjectMapper mapper = new ObjectMapper();
    CareerNarrativePlan current = planner.plan(
        assessor.assess(request, chart, ReportTopic.CAREER, context), context);
    JsonNode legacy = mapper.valueToTree(current);
    ((com.fasterxml.jackson.databind.node.ObjectNode) legacy).remove("timeline");
    legacy.withArray("years").forEach(year ->
        ((com.fasterxml.jackson.databind.node.ObjectNode) year).remove("actionGuide"));

    CareerNarrativePlan restored = mapper.treeToValue(legacy, CareerNarrativePlan.class);

    assertNull(restored.timeline());
    assertTrue(restored.years().stream().allMatch(year -> year.actionGuide() == null));
    assertEquals(current.thesis(), restored.thesis());
    assertEquals(current.years().stream().map(CareerNarrativePlan.YearNarrative::headline).toList(),
        restored.years().stream().map(CareerNarrativePlan.YearNarrative::headline).toList());
  }

  @Test
  void readsEarlyV5JsonThatDoesNotContainActionGuideFocusKey() throws Exception {
    CareerContext context = CareerContext.fromCodes("job_seeking", "job_change", "stalled");
    ObjectMapper mapper = new ObjectMapper();
    CareerNarrativePlan current = planner.plan(
        assessor.assess(request, chart, ReportTopic.CAREER, context), context);
    JsonNode earlyV5 = mapper.valueToTree(current);
    earlyV5.withArray("years").forEach(year ->
        ((com.fasterxml.jackson.databind.node.ObjectNode) year.get("actionGuide")).remove("focusKey"));

    CareerNarrativePlan restored = mapper.treeToValue(earlyV5, CareerNarrativePlan.class);

    assertTrue(restored.years().stream()
        .allMatch(year -> year.actionGuide().focusKey().equals("legacy.unspecified")));
  }

  @Test
  void removesMechanicalMetaLanguageFromReaderCopy() {
    CareerContext context = CareerContext.fromCodes("job_seeking", "job_change", "stalled");
    CareerNarrativePlan plan = planner.plan(
        assessor.assess(request, chart, ReportTopic.CAREER, context), context);
    String text = flatten(plan);

    for (String forbidden : List.of(
        "结合你填写的", "优先解释为", "现实观察信号", "方法边界",
        "现有证据不足", "先观察再判断", "可能存在一定影响", "。。")) {
      assertFalse(text.contains(forbidden), forbidden + " in " + text);
    }
  }

  @Test
  void usesRealityContextToChangeTheConcreteVocabularyWithoutChangingEvidence() {
    CareerContext employed = CareerContext.fromCodes("employed", "promotion", "smooth");
    CareerContext seeking = CareerContext.fromCodes("job_seeking", "job_change", "stalled");

    CareerNarrativePlan employedPlan = planner.plan(
        assessor.assess(request, chart, ReportTopic.CAREER, employed), employed);
    CareerNarrativePlan seekingPlan = planner.plan(
        assessor.assess(request, chart, ReportTopic.CAREER, seeking), seeking);

    assertNotEquals(employedPlan.thesis(), seekingPlan.thesis());
    assertTrue(flatten(employedPlan).contains("加薪") || flatten(employedPlan).contains("晋升"));
    assertTrue(flatten(seekingPlan).contains("简历") || flatten(seekingPlan).contains("投递"));
    assertEquals(
        employedPlan.years().stream().map(CareerNarrativePlan.YearNarrative::evidenceKeys).toList(),
        seekingPlan.years().stream().map(CareerNarrativePlan.YearNarrative::evidenceKeys).toList());
  }

  @Test
  void writesEverydaySteadyChineseAcrossCareerSituations() {
    List<CareerContext> contexts = List.of(
        CareerContext.fromCodes("employed", "promotion", "smooth"),
        CareerContext.fromCodes("self_employed", "stability", "high_pressure"),
        CareerContext.fromCodes("job_seeking", "job_change", "stalled"),
        CareerContext.fromCodes("studying", "transition", "preparing_change"));
    List<CareerNarrativePlan> plans = contexts.stream()
        .map(context -> planner.plan(
            assessor.assess(request, chart, ReportTopic.CAREER, context), context))
        .toList();

    assertEquals("今年先把手上的成绩做出来，明年再找机会谈晋升。", plans.get(0).thesis());
    assertTrue(flatten(plans.get(1)).contains("客户"));
    assertTrue(flatten(plans.get(2)).contains("简历"));
    assertTrue(flatten(plans.get(3)).contains("作品"));

    for (CareerNarrativePlan plan : plans) {
      String text = flatten(plan);
      for (String formalTerm : List.of(
          "正式授权", "权责", "评价周期", "资源配置", "协作节点", "交付人",
          "交付", "兑现", "承接", "责任边界", "扩大承诺", "岗位匹配度",
          "负荷", "复盘", "核心成果", "推进强度")) {
        assertFalse(text.contains(formalTerm), formalTerm + " in " + text);
      }
      for (String line : readerLines(plan)) {
        assertTrue(line.length() <= 48, line.length() + " chars: " + line);
      }
    }
  }

  @Test
  void addsACompleteOutcomeDrivenActionGuideForEveryCareerSituation() {
    List<CareerContext> contexts = List.of(
        CareerContext.fromCodes("employed", "promotion", "smooth"),
        CareerContext.fromCodes("self_employed", "stability", "high_pressure"),
        CareerContext.fromCodes("job_seeking", "job_change", "stalled"),
        CareerContext.fromCodes("studying", "transition", "preparing_change"));

    List<CareerNarrativePlan> plans = contexts.stream()
        .map(context -> planner.plan(
            assessor.assess(request, chart, ReportTopic.CAREER, context), context))
        .toList();

    for (CareerNarrativePlan plan : plans) {
      for (CareerNarrativePlan.YearNarrative year : plan.years()) {
        AnnualActionGuide guide = year.actionGuide();
        assertNotNull(guide);
        assertFalse(guide.focusKey().isBlank());
        assertFalse(guide.problem().isBlank());
        assertFalse(guide.action().isBlank());
        assertFalse(guide.expectedChange().isBlank());
        assertFalse(guide.checkTiming().isBlank());
        assertFalse(guide.successSignal().isBlank());
        assertFalse(guide.adjustmentCondition().isBlank());
        assertFalse(guide.fallbackAction().isBlank());
        assertEquals(year.evidenceKeys(), guide.evidenceKeys());
      }
    }

    assertContainsAny(actionGuideText(plans.get(0)), "工作", "负责人", "职位", "收入");
    assertContainsAny(actionGuideText(plans.get(1)), "客户", "项目", "生意", "收入");
    assertContainsAny(actionGuideText(plans.get(2)), "岗位", "招聘", "面试", "投递");
    assertContainsAny(actionGuideText(plans.get(3)), "学习", "作品", "实习", "能力");
  }

  @Test
  void annualCalculationFocusChangesTheCareerActionGuide() {
    CareerContext context = CareerContext.fromCodes("employed", "promotion", "smooth");
    CareerActionGuideWriter writer = new CareerActionGuideWriter();

    AnnualActionGuide visibility = writer.write(
        context, 0, "career.visibility", AnnualStage.ADVANCE,
        "工作成绩还没有被看见。", "整理一项已经做成的工作。",
        "四周后仍没有明确反馈。", "请负责人指出还缺少什么。",
        List.of("career.visibility", "annual.stem.group.output"));
    AnnualActionGuide coordination = writer.write(
        context, 0, "career.coordination_window", AnnualStage.ADVANCE,
        "工作需要别人配合才能继续。", "找一位相关同事确认分工。",
        "四周后仍没有人参与。", "把工作拆小后重新确认分工。",
        List.of("career.coordination_window", "annual.branch.relation.harmony"));

    assertEquals("career.visibility", visibility.focusKey());
    assertEquals("career.coordination_window", coordination.focusKey());
    assertNotEquals(visibility.expectedChange(), coordination.expectedChange());
    assertNotEquals(visibility.successSignal(), coordination.successSignal());
  }

  @Test
  void everyCareerCalculationFocusOwnsADistinctDecisionInEverySituation() {
    List<String> focuses = List.of(
        "career.role_transition",
        "career.coordination_window",
        "career.responsibility_upgrade",
        "career.visibility",
        "career.preparation",
        "career.resource_delivery",
        "career.overextension");
    List<CareerContext> contexts = List.of(
        CareerContext.fromCodes("employed", "promotion", "smooth"),
        CareerContext.fromCodes("self_employed", "stability", "high_pressure"),
        CareerContext.fromCodes("job_seeking", "job_change", "stalled"),
        CareerContext.fromCodes("studying", "transition", "preparing_change"));
    CareerActionGuideWriter writer = new CareerActionGuideWriter();

    for (CareerContext context : contexts) {
      List<AnnualActionGuide> guides = focuses.stream()
          .map(focus -> writer.write(
              context, 0, focus, AnnualStage.ADVANCE,
              "当前有一件具体问题需要处理。", "先完成一项具体行动。",
              "四周后仍没有变化。", "把范围缩小后再试一次。",
              List.of(focus, "natal.fact")))
          .toList();

      assertEquals(focuses, guides.stream().map(AnnualActionGuide::focusKey).toList());
      assertEquals(focuses.size(), guides.stream()
          .map(guide -> guide.expectedChange() + guide.successSignal())
          .distinct()
          .count());
    }
  }

  @Test
  void careerActionGuideDoesNotRepeatSentencesAcrossProductYears() {
    CareerContext context = CareerContext.fromCodes("employed", "promotion", "smooth");
    CareerNarrativePlan plan = planner.plan(
        assessor.assess(request, chart, ReportTopic.CAREER, context), context);

    List<String> sentences = plan.years().stream()
        .flatMap(year -> year.actionGuide().lines().stream())
        .flatMap(line -> java.util.Arrays.stream(line.split("(?<=[。！？])")))
        .map(String::trim)
        .filter(line -> !line.isBlank())
        .toList();

    assertEquals(sentences.size(), sentences.stream().distinct().count(), sentences.toString());
  }

  @Test
  void namesTheSpecificProblemInsteadOfUsingVagueStatusFragments() {
    List<CareerContext> contexts = List.of(
        CareerContext.fromCodes("employed", "promotion", "stalled"),
        CareerContext.fromCodes("self_employed", "stability", "stalled"),
        CareerContext.fromCodes("job_seeking", "job_change", "stalled"),
        CareerContext.fromCodes("studying", "transition", "stalled"));
    List<CareerNarrativePlan> plans = contexts.stream()
        .map(context -> planner.plan(
            assessor.assess(request, chart, ReportTopic.CAREER, context), context))
        .toList();

    assertEquals("在职 · 希望升职 · 工作推进不顺", plans.get(0).contextSummary());
    assertEquals("自营或创业 · 希望工作稳定 · 生意进展不顺", plans.get(1).contextSummary());
    assertEquals("求职中 · 准备换工作 · 求职进展不顺", plans.get(2).contextSummary());
    assertEquals("学习或准备入行 · 准备转换方向 · 学习进展不顺", plans.get(3).contextSummary());
    assertEquals("今年先明确想找哪类工作，明年争取进入合适的岗位。", plans.get(2).thesis());

    assertTrue(plans.get(0).years().get(0).reasons().get(1).contains("工作推进不顺"));
    assertTrue(plans.get(1).years().get(0).reasons().get(1).contains("客户"));
    assertTrue(plans.get(2).years().get(0).reasons().get(1).contains("简历"));
    assertTrue(plans.get(3).years().get(0).reasons().get(1).contains("作品"));

    for (CareerNarrativePlan plan : plans) {
      String text = flatten(plan);
      for (String vagueFragment : List.of(
          "最近卡住了", "现在卡住了", "卡在哪里", "当前卡点", "再认真换")) {
        assertFalse(text.contains(vagueFragment), vagueFragment + " in " + text);
      }
      for (CareerNarrativePlan.YearNarrative year : plan.years()) {
        assertTrue(year.obstacle().endsWith("。"), year.obstacle());
      }
    }
  }

  @Test
  void describesTheSameAnnualEvidenceInTheReadersActualCareerSituation() {
    List<CareerContext> contexts = List.of(
        CareerContext.fromCodes("employed", "promotion", "stalled"),
        CareerContext.fromCodes("self_employed", "stability", "stalled"),
        CareerContext.fromCodes("job_seeking", "job_change", "stalled"),
        CareerContext.fromCodes("studying", "transition", "stalled"));
    List<CareerNarrativePlan> plans = contexts.stream()
        .map(context -> planner.plan(
            assessor.assess(request, chart, ReportTopic.CAREER, context), context))
        .toList();

    assertEquals("工作内容可能有变化", plans.get(0).years().get(0).headline());
    assertEquals("生意做法可能要调整", plans.get(1).years().get(0).headline());
    assertEquals("求职方向可能需要调整", plans.get(2).years().get(0).headline());
    assertEquals("学习或入行方向可能需要调整", plans.get(3).years().get(0).headline());

    assertTrue(plans.get(0).years().get(0).reasons().get(0).contains("工作"));
    assertTrue(plans.get(1).years().get(0).reasons().get(0).contains("生意"));
    assertTrue(plans.get(2).years().get(0).reasons().get(0).contains("求职"));
    assertTrue(plans.get(3).years().get(0).reasons().get(0).contains("学习"));

    assertEquals(
        plans.get(0).years().stream().map(CareerNarrativePlan.YearNarrative::evidenceKeys).toList(),
        plans.get(2).years().stream().map(CareerNarrativePlan.YearNarrative::evidenceKeys).toList());
  }

  private String flatten(CareerNarrativePlan plan) {
    return plan.thesis() + plan.contextSummary() + String.join("", plan.route()) + plan.years().stream()
        .map(year -> year.headline() + year.verdict()
            + String.join("", year.reasons())
            + year.obstacle()
            + String.join("", year.actions())
            + year.changeCondition())
        .collect(Collectors.joining());
  }

  private String actionGuideText(CareerNarrativePlan plan) {
    return plan.years().stream()
        .flatMap(year -> year.actionGuide().lines().stream())
        .collect(Collectors.joining());
  }

  private void assertContainsAny(String text, String... words) {
    assertTrue(java.util.Arrays.stream(words).anyMatch(text::contains), text);
  }

  private List<String> readerLines(CareerNarrativePlan plan) {
    return java.util.stream.Stream.concat(
            java.util.stream.Stream.of(plan.thesis()),
            plan.years().stream().flatMap(year -> java.util.stream.Stream.of(
                java.util.stream.Stream.of(
                    year.headline(), year.verdict(), year.obstacle(), year.changeCondition()),
                year.reasons().stream(),
                year.actions().stream()).flatMap(stream -> stream)))
        .toList();
  }
}
