package com.bazi.app.report.overall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class OverallNarrativePreflightTest {

  private static final Path INPUTS = Path.of(
      "src/test/resources/report/wealth-v2-baseline-inputs.json");
  private static final Path OUTPUT = Path.of("target/overall-v1-preflight.json");
  private static final ObjectMapper JSON = new ObjectMapper()
      .findAndRegisterModules()
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
  private static final Set<String> EXPECTED_DIMENSIONS = Set.of(
      "rhythm", "career", "wealth", "relationship");
  private static final List<String> FORBIDDEN = List.of(
      "卡点", "抓手", "赋能", "赛道", "闭环", "现有证据不足", "先观察再判断",
      "可能可以", "视情况而定", "整体安排的几件事", "日常安排的几件事",
      "的直接变化会增多", "里容易积累小问题", "，但不要因为有余力");

  @Test
  void preflightsTwelveFixedBirthInputsBeforeCommercialOpening() throws Exception {
    JsonNode inputs = JSON.readTree(INPUTS.toFile());
    ZoneId zone = ZoneId.of(inputs.get("zoneId").asText());
    List<CaseResult> results = new ArrayList<>();

    for (JsonNode fixture : inputs.get("fullBirthCases")) {
      results.add(run(fixture, zone));
    }

    assertEquals(12, results.size());
    List<String> failures = results.stream()
        .flatMap(result -> result.failures().stream().map(failure -> result.id() + ": " + failure))
        .toList();
    assertTrue(failures.isEmpty(), String.join("\n", failures));

    ObjectNode summary = summary(results);
    assertEquals(0, summary.get("timelineDuplicateCaseCount").asInt());
    assertEquals(0, summary.get("timelineEmptyCopyCaseCount").asInt());
    assertEquals(0, summary.get("timelineMissingEvidenceCaseCount").asInt());
    Files.writeString(OUTPUT, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(summary) + "\n");
    results.forEach(result -> System.out.printf(
        "OVERALL_PREFLIGHT case=%s focuses=%s headlines=%s failures=%s%n",
        result.id(), result.primaryCodes(), result.headlines(), result.failures()));
    System.out.println("OVERALL_PREFLIGHT_SUMMARY " + summary);
  }

  private CaseResult run(JsonNode fixture, ZoneId zone) {
    String id = fixture.get("id").asText();
    List<String> failures = new ArrayList<>();
    try {
      PaipanRequest request = JSON.treeToValue(fixture.get("request"), PaipanRequest.class);
      Clock clock = Clock.fixed(
          LocalDate.parse(fixture.get("asOf").asText()).atStartOfDay(zone).toInstant(), zone);
      var chart = new BaziService().paipan(request);
      var factory = new AnnualContextFactory(clock);
      var contexts = factory.create(
          request, chart, ReportHorizon.of(fixture.get("horizonYears").asInt()));
      var analysisContexts = new ArrayList<com.bazi.app.report.AnnualContext>();
      analysisContexts.add(factory.createYear(request, chart, contexts.get(0).year() - 1));
      analysisContexts.addAll(contexts);
      OverallPeriodArbitrator arbitrator = new OverallPeriodArbitrator();
      OverallYearEvaluation previous = arbitrator.arbitrate(analysisContexts).years().get(0);
      OverallPeriodEvaluation period = arbitrator.arbitrate(contexts);
      OverallNarrativePlan content = new OverallNarrativePlanner().plan(period, previous);

      inspect(content, failures);
      return new CaseResult(
          id,
          content.years().stream().map(OverallNarrativePlan.YearNarrative::primaryCode).toList(),
          content.years().stream().map(OverallNarrativePlan.YearNarrative::secondaryCode).toList(),
          content.years().stream().map(OverallNarrativePlan.YearNarrative::headline).toList(),
          content.years().stream().flatMap(year -> year.actions().stream()).toList(),
          content,
          failures);
    } catch (RuntimeException error) {
      failures.add("生成失败：" + error.getClass().getSimpleName() + " - " + error.getMessage());
      return new CaseResult(id, List.of(), List.of(), List.of(), List.of(), null, failures);
    } catch (Exception error) {
      failures.add("读取输入失败：" + error.getClass().getSimpleName() + " - " + error.getMessage());
      return new CaseResult(id, List.of(), List.of(), List.of(), List.of(), null, failures);
    }
  }

  private void inspect(OverallNarrativePlan content, List<String> failures) {
    if (content.horizonYears() != 3 || content.years().size() != 3) {
      failures.add("不是完整三年输出");
    }
    if (content.timeline() == null) {
      failures.add("缺少过去、当下、未来时间线");
    } else {
      List<String> timelineCopy = timelineCopy(content);
      if (timelineCopy.stream().anyMatch(value -> value == null || value.isBlank())) {
        failures.add("时间线存在空内容");
      }
      if (new LinkedHashSet<>(timelineCopy).size() != timelineCopy.size()) {
        failures.add("时间线跨区段出现完全重复文案");
      }
      if (content.timeline().past().evidenceKeys().isEmpty()
          || content.timeline().present().evidenceKeys().isEmpty()
          || content.timeline().future().stream().anyMatch(step -> step.evidenceKeys().isEmpty())) {
        failures.add("时间线存在无依据区段");
      }
      if (content.timeline().future().size() != 2
          || content.timeline().future().stream()
              .map(com.bazi.app.report.NarrativeTimeline.FutureStep::headline)
              .distinct().count() != 2) {
        failures.add("未来两年没有使用不同重点");
      }
      for (String emptyPhrase : List.of("整体情况", "综合层面")) {
        if (String.join("", timelineCopy).contains(emptyPhrase)) {
          failures.add("时间线出现空泛表达：" + emptyPhrase);
        }
      }
    }
    required("三年总论", content.thesis(), failures);
    required("三年摘要", content.summary(), failures);
    required("阅读说明", content.readingNote(), failures);
    if (content.route().isEmpty() || content.route().stream().anyMatch(String::isBlank)) {
      failures.add("三年行动路线为空");
    }

    Set<String> headlines = new LinkedHashSet<>();
    List<String> actions = new ArrayList<>();
    Map<String, List<String>> judgmentsByDimension = new LinkedHashMap<>();
    List<String> priorityIssues = new ArrayList<>();
    List<String> changeConditions = new ArrayList<>();
    for (OverallNarrativePlan.YearNarrative year : content.years()) {
      required(year.year() + "年标题", year.headline(), failures);
      required(year.year() + "年总判断", year.verdict(), failures);
      required(year.year() + "年跨维联动", year.linkage(), failures);
      if (!year.linkage().isBlank()
          && (!year.linkage().contains(year.primaryLabel())
              || !year.linkage().contains(year.secondaryLabel()))) {
        failures.add(year.year() + "年跨维联动没有同时说明主次焦点：" + year.linkage());
      }
      required(year.year() + "年优先问题", year.priorityIssue(), failures);
      required(year.year() + "年调整条件", year.changeCondition(), failures);
      required(year.year() + "年衔接", year.transition(), failures);
      headlines.add(year.headline());
      actions.addAll(year.actions());
      priorityIssues.add(year.priorityIssue());
      changeConditions.add(year.changeCondition());
      if (year.actions().size() != 2 || year.actions().stream().anyMatch(String::isBlank)) {
        failures.add(year.year() + "年行动项不是两条完整句子");
      }
      Set<String> dimensions = new LinkedHashSet<>();
      for (OverallNarrativePlan.DimensionReading dimension : year.dimensions()) {
        dimensions.add(dimension.code());
        judgmentsByDimension.computeIfAbsent(dimension.code(), ignored -> new ArrayList<>())
            .add(dimension.judgment());
        required(year.year() + "年" + dimension.code() + "判断", dimension.judgment(), failures);
        if (dimension.evidenceKeys().isEmpty()
            || dimension.evidenceKeys().stream().anyMatch(String::isBlank)) {
          failures.add(year.year() + "年" + dimension.code() + "没有可追溯依据");
        }
      }
      if (!dimensions.equals(EXPECTED_DIMENSIONS)) {
        failures.add(year.year() + "年四维不完整：" + dimensions);
      }
    }
    if (headlines.size() != content.years().size()) {
      failures.add("同一报告出现重复年度标题：" + headlines);
    }
    if (new LinkedHashSet<>(actions).size() != actions.size()) {
      failures.add("同一报告出现完全重复行动句：" + actions);
    }
    judgmentsByDimension.forEach((dimension, judgments) -> {
      if (new LinkedHashSet<>(judgments).size() != judgments.size()) {
        failures.add("同一维度跨年出现完全重复判断：" + dimension + " " + judgments);
      }
    });
    if (new LinkedHashSet<>(priorityIssues).size() != priorityIssues.size()) {
      failures.add("跨年出现完全重复优先问题：" + priorityIssues);
    }
    if (new LinkedHashSet<>(changeConditions).size() != changeConditions.size()) {
      failures.add("跨年出现完全重复调整条件：" + changeConditions);
    }
    String text = flatten(content);
    FORBIDDEN.stream().filter(text::contains)
        .forEach(word -> failures.add("出现禁用表达：" + word));
  }

  private void required(String label, String value, List<String> failures) {
    if (value == null || value.isBlank()) failures.add(label + "为空");
  }

  private String flatten(OverallNarrativePlan content) {
    return JSON.valueToTree(content).toString();
  }

  private ObjectNode summary(List<CaseResult> results) {
    ObjectNode output = JSON.createObjectNode();
    output.put("sampleCount", results.size());
    output.put("successCount", results.stream().filter(result -> result.failures().isEmpty()).count());
    output.put("failureCount", results.stream().filter(result -> !result.failures().isEmpty()).count());
    output.put("yearCount", results.stream().mapToInt(result -> result.primaryCodes().size()).sum());
    output.set("primaryFocusDistribution", distribution(results, true));
    output.set("secondaryFocusDistribution", distribution(results, false));
    output.set("primarySequenceDistribution", sequenceDistribution(results));
    output.put("duplicateHeadlineCaseCount", results.stream()
        .filter(result -> new LinkedHashSet<>(result.headlines()).size() != result.headlines().size())
        .count());
    output.put("duplicateActionCaseCount", results.stream()
        .filter(result -> new LinkedHashSet<>(result.actions()).size() != result.actions().size())
        .count());
    output.put("duplicateDimensionJudgmentCaseCount", results.stream()
        .filter(this::hasDuplicateDimensionJudgment)
        .count());
    output.put("duplicatePriorityIssueCaseCount", results.stream()
        .filter(result -> hasDuplicate(result, OverallNarrativePlan.YearNarrative::priorityIssue))
        .count());
    output.put("duplicateChangeConditionCaseCount", results.stream()
        .filter(result -> hasDuplicate(result, OverallNarrativePlan.YearNarrative::changeCondition))
        .count());
    output.put("emptyLinkageCount", results.stream()
        .filter(result -> result.content() != null)
        .flatMap(result -> result.content().years().stream())
        .filter(year -> year.linkage().isBlank())
        .count());
    output.put("emptyOrInvalidCaseCount", results.stream()
        .filter(result -> !result.failures().isEmpty()).count());
    long timelineDuplicateCases = results.stream()
        .filter(result -> result.content() != null && result.content().timeline() != null)
        .filter(result -> {
          List<String> copy = timelineCopy(result.content());
          return new LinkedHashSet<>(copy).size() != copy.size();
        })
        .count();
    long timelineEmptyCases = results.stream()
        .filter(result -> result.content() == null
            || result.content().timeline() == null
            || timelineCopy(result.content()).stream().anyMatch(value -> value == null || value.isBlank()))
        .count();
    long timelineMissingEvidenceCases = results.stream()
        .filter(result -> result.content() == null
            || result.content().timeline() == null
            || result.content().timeline().past().evidenceKeys().isEmpty()
            || result.content().timeline().present().evidenceKeys().isEmpty()
            || result.content().timeline().future().stream()
                .anyMatch(step -> step.evidenceKeys().isEmpty()))
        .count();
    output.put("timelineDuplicateCaseCount", timelineDuplicateCases);
    output.put("timelineEmptyCopyCaseCount", timelineEmptyCases);
    output.put("timelineMissingEvidenceCaseCount", timelineMissingEvidenceCases);
    output.put("timelineDuplicateRate", timelineDuplicateCases / (double) results.size());
    output.put("timelineEmptyCopyRate", timelineEmptyCases / (double) results.size());
    output.put("timelineMissingEvidenceRate", timelineMissingEvidenceCases / (double) results.size());
    ArrayNode cases = output.putArray("cases");
    results.forEach(result -> {
      ObjectNode item = cases.addObject();
      item.put("id", result.id());
      item.set("primaryCodes", JSON.valueToTree(result.primaryCodes()));
      item.set("secondaryCodes", JSON.valueToTree(result.secondaryCodes()));
      item.set("headlines", JSON.valueToTree(result.headlines()));
      item.set("actions", JSON.valueToTree(result.actions()));
      item.set("failures", JSON.valueToTree(result.failures()));
      if (result.content() != null) item.set("content", JSON.valueToTree(result.content()));
    });
    return output;
  }

  private List<String> timelineCopy(OverallNarrativePlan content) {
    var timeline = content.timeline();
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

  private boolean hasDuplicateDimensionJudgment(CaseResult result) {
    if (result.content() == null) return false;
    Map<String, List<String>> judgments = new LinkedHashMap<>();
    result.content().years().stream().flatMap(year -> year.dimensions().stream())
        .forEach(dimension -> judgments
            .computeIfAbsent(dimension.code(), ignored -> new ArrayList<>())
            .add(dimension.judgment()));
    return judgments.values().stream()
        .anyMatch(items -> new LinkedHashSet<>(items).size() != items.size());
  }

  private boolean hasDuplicate(
      CaseResult result,
      java.util.function.Function<OverallNarrativePlan.YearNarrative, String> value) {
    if (result.content() == null) return false;
    List<String> values = result.content().years().stream().map(value).toList();
    return new LinkedHashSet<>(values).size() != values.size();
  }

  private ObjectNode distribution(List<CaseResult> results, boolean primary) {
    Map<String, Integer> counts = new TreeMap<>();
    results.stream()
        .flatMap(result -> (primary ? result.primaryCodes() : result.secondaryCodes()).stream())
        .forEach(code -> counts.merge(code, 1, Integer::sum));
    ObjectNode output = JSON.createObjectNode();
    counts.forEach(output::put);
    return output;
  }

  private ObjectNode sequenceDistribution(List<CaseResult> results) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    results.stream().map(result -> String.join(" > ", result.primaryCodes()))
        .forEach(sequence -> counts.merge(sequence, 1, Integer::sum));
    ObjectNode output = JSON.createObjectNode();
    counts.forEach(output::put);
    return output;
  }

  private record CaseResult(
      String id,
      List<String> primaryCodes,
      List<String> secondaryCodes,
      List<String> headlines,
      List<String> actions,
      OverallNarrativePlan content,
      List<String> failures) {

    private CaseResult {
      primaryCodes = List.copyOf(primaryCodes);
      secondaryCodes = List.copyOf(secondaryCodes);
      headlines = List.copyOf(headlines);
      actions = List.copyOf(actions);
      failures = List.copyOf(failures);
    }
  }
}
