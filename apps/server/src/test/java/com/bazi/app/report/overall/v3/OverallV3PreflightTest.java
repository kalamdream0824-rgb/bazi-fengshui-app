package com.bazi.app.report.overall.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.domain.constants.WuXingConstants;
import com.bazi.app.domain.constants.ZiZuoConstants;
import com.bazi.app.dto.HideGanDto;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.PillarDto;
import com.bazi.app.report.AnnualActionGuide;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nlf.calendar.util.LunarUtil;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Engineering preflight for overall v3. This is not evidence of real-world accuracy. */
class OverallV3PreflightTest {

  private static final String CORPUS = "/report/overall-v3-engineering-corpus.json";
  private static final Path OUTPUT = Path.of("target/overall-v3-preflight.json");
  private static final ObjectMapper JSON = new ObjectMapper()
      .findAndRegisterModules()
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
  private static final List<String> FORBIDDEN = List.of(
      "老板", "客户", "负责人", "职位", "配偶", "对象", "求职", "创业",
      "卡点", "卡住", "先观察再判断", "可以作为关注", "现有证据不足",
      "视情况而定", "可能可以", "更值得关注", "抓手", "赋能", "闭环",
      "眼前的目前", "有进展也有牵制", "推动预计进账与实际到账的差异产生结果",
      "每天可用的时间和精力留下结果", "实际进账的稳定程度能否稳定",
      "差异的基本需要", "差异的实际状态", "回应的实际状态",
      "时间和精力中最明确的一项", "能改善预计进账与实际到账的差异的小事");
  private static final List<String> DANGLING = List.of(
      "的", "和", "与", "或", "以及", "因为", "所以", "如果", "否则", "把", "让", "为");
  private static final List<String> PREDICATES = List.of(
      "处理", "核对", "完成", "记录", "保留", "暂停", "缩小", "停止", "判断", "确认",
      "推进", "守住", "算清", "说清", "留下", "撤回", "减少", "增加", "改用", "延后",
      "执行", "影响", "得到", "存在", "下降", "变差", "恢复", "有效", "改善", "形成",
      "推动", "避免", "产生", "受损");
  private static final Pattern PLACEHOLDER = Pattern.compile(
      ".*(\\$\\{|\\{\\{|\\}\\}|<[A-Za-z][^>]*>|\\{[A-Z][A-Z0-9_]*}).*");
  private static final List<String> BAD_PUNCTUATION = List.of(
      "，，", "。。", "；；", "：：", "！？！？", ",。", "。；", "；。");

  private static JsonNode corpus;
  private static List<PreflightResult> results;
  private static Map<String, List<String>> crossFocusTupleCollisions;
  private static Map<String, List<String>> evidenceDistribution;

  @BeforeAll
  static void runPreflight() throws Exception {
    corpus = read(CORPUS);
    ZoneId zone = ZoneId.of(corpus.path("zoneId").asText());
    LocalDate defaultAsOf = LocalDate.parse(corpus.path("asOf").asText());
    List<CaseInput> inputs = new ArrayList<>();
    inputs.addAll(fixedBirthCases(corpus));
    inputs.addAll(classicCases(corpus, defaultAsOf));
    inputs.addAll(boundaryCases(corpus, defaultAsOf));
    results = inputs.stream().map(input -> run(input, zone)).toList();
    crossFocusTupleCollisions = tupleCollisions(results);
    evidenceDistribution = evidenceDistribution(results);

    Map<String, Object> output = new LinkedHashMap<>();
    output.put("notice", corpus.path("notice").asText());
    output.put("sampleCount", results.size());
    output.put("sourceDistribution", results.stream().collect(Collectors.groupingBy(
        PreflightResult::source, LinkedHashMap::new, Collectors.counting())));
    output.put("successfulCaseCount", results.stream().filter(item -> item.failures().isEmpty()).count());
    output.put("snapshotFingerprintCount", results.stream()
        .map(PreflightResult::snapshotFingerprint).filter(value -> !value.isBlank()).distinct().count());
    output.put("snapshotFingerprintCollisionGroups", fingerprintCollisions(results));
    output.put("primaryTopicDistribution", distribution(results.stream()
        .filter(item -> item.report() != null)
        .flatMap(item -> item.report().years().stream())
        .map(OverallV3NarrativePlan.YearNarrative::primaryCode).toList()));
    output.put("conflictKeyDistribution", distribution(results.stream()
        .flatMap(item -> item.conflictKeys().stream()).toList()));
    output.put("actionFocusDistribution", distribution(results.stream()
        .flatMap(item -> item.actionTuples().stream())
        .map(ActionTuple::focusKey).toList()));
    output.put("actionTupleCount", results.stream().mapToLong(item -> item.actionTuples().size()).sum());
    output.put("distinctActionTupleCount", results.stream()
        .flatMap(item -> item.actionTuples().stream()).map(OverallV3PreflightTest::tupleKey)
        .distinct().count());
    output.put("crossFocusActionTupleCollisions", crossFocusTupleCollisions);
    output.put("sameReportExactRepeatCaseCounts", sameReportRepeatCounts(results));
    output.put("languageFailureCaseCount", results.stream()
        .filter(item -> item.failures().stream().anyMatch(value -> value.startsWith("language:")))
        .count());
    output.put("maximumAdviceChineseLength", results.stream()
        .filter(item -> item.report() != null)
        .flatMap(item -> adviceCopy(item.report()).stream())
        .mapToInt(OverallV3PreflightTest::chineseLength).max().orElse(0));
    output.put("evidenceKeyDistribution", evidenceDistribution);
    output.put("manualReviewCaseIds", corpus.path("manualReviewCaseIds"));
    output.put("cases", results);
    Files.createDirectories(OUTPUT.getParent());
    Files.writeString(OUTPUT,
        JSON.writerWithDefaultPrettyPrinter().writeValueAsString(output) + "\n");
  }

  @Test
  void corpusCoversFixedClassicAndBoundaryCases() {
    assertEquals(1, corpus.path("schemaVersion").asInt());
    assertTrue(corpus.path("notice").asText().contains("不代表真人经历或预测准确率"));
    assertEquals(12, count("fixed"));
    assertEquals(8, count("classic"));
    assertEquals(12, count("boundary"));
    assertEquals(32, results.size());
    assertEquals(6, corpus.path("manualReviewCaseIds").size());
    Set<String> generatedIds = results.stream().map(PreflightResult::id).collect(Collectors.toSet());
    corpus.path("manualReviewCaseIds").forEach(id ->
        assertTrue(generatedIds.contains(id.asText()), "manual review case missing: " + id));
  }

  @Test
  void everyCaseProducesACompleteDeterministicFourYearAnalysis() {
    List<String> failures = results.stream()
        .filter(item -> !item.failures().isEmpty() || !item.deterministic()
            || item.snapshotCount() != 16 || item.report() == null
            || item.report().years().size() != 3 || item.report().timeline() == null)
        .map(item -> item.id() + ": " + item.failures()).toList();
    assertEquals(List.of(), failures, () -> "generation failures; inspect " + OUTPUT);
  }

  @Test
  void differentActionFocusesNeverShareTheSameActionResultSignalTuple() {
    assertEquals(Map.of(), crossFocusTupleCollisions,
        () -> "different focus keys share one action tuple: " + crossFocusTupleCollisions
            + "; inspect " + OUTPUT);
  }

  @Test
  void oneReportNeverRepeatsItsCoreAnnualCopy() {
    Map<String, List<String>> failures = failuresByPrefix("duplicate:");
    assertEquals(Map.of(), failures,
        () -> "same-report annual copy repeats: " + failures + "; inspect " + OUTPUT);
  }

  @Test
  void visibleChinesePassesTheMechanicalReleaseGate() {
    Map<String, List<String>> failures = failuresByPrefix("language:");
    assertEquals(Map.of(), failures,
        () -> "visible Chinese failed the mechanical gate: " + failures + "; inspect " + OUTPUT);
  }

  private static long count(String source) {
    return results.stream().filter(item -> item.source().equals(source)).count();
  }

  private static Map<String, List<String>> failuresByPrefix(String prefix) {
    Map<String, List<String>> failures = new LinkedHashMap<>();
    results.forEach(item -> {
      List<String> selected = item.failures().stream().filter(value -> value.startsWith(prefix)).toList();
      if (!selected.isEmpty()) failures.put(item.id(), selected);
    });
    return failures;
  }

  private static List<CaseInput> fixedBirthCases(JsonNode root) throws Exception {
    JsonNode source = read(root.path("fixedBirthResource").asText());
    Set<String> selected = new LinkedHashSet<>();
    root.path("fixedBirthCaseIds").forEach(item -> selected.add(item.asText()));
    List<CaseInput> result = new ArrayList<>();
    for (JsonNode fixture : source.path("fullBirthCases")) {
      if (!selected.contains(fixture.path("id").asText())) continue;
      result.add(new CaseInput(
          fixture.path("id").asText(),
          "fixed",
          LocalDate.parse(fixture.path("asOf").asText()),
          JSON.treeToValue(fixture.path("request"), PaipanRequest.class),
          null,
          List.of("R01-R12")));
    }
    assertEquals(selected, result.stream().map(CaseInput::id)
        .collect(Collectors.toCollection(LinkedHashSet::new)));
    return result;
  }

  private static List<CaseInput> classicCases(JsonNode root, LocalDate asOf) throws Exception {
    Map<String, CaseInput> unique = new LinkedHashMap<>();
    for (JsonNode resource : root.path("classicResources")) {
      JsonNode source = read(resource.asText());
      for (JsonNode fixture : source.path("cases")) {
        String id = fixture.path("id").asText();
        JsonNode canonical = fixture.path("canonicalInput");
        List<String> pillars = new ArrayList<>();
        canonical.path("pillars").forEach(item -> pillars.add(item.asText()));
        unique.putIfAbsent(id, new CaseInput(
            id,
            "classic",
            asOf,
            new PaipanRequest(id, canonical.path("gender").asText(),
                "1900-01-01T12:00:00", "古籍四柱直录", false),
            List.copyOf(pillars),
            List.of(fixture.path("partition").asText("LEGACY"), "STRUCTURAL_ONLY")));
      }
    }
    return List.copyOf(unique.values());
  }

  private static List<CaseInput> boundaryCases(JsonNode root, LocalDate asOf) throws Exception {
    List<CaseInput> result = new ArrayList<>();
    for (JsonNode fixture : root.path("boundaryCases")) {
      List<String> tags = new ArrayList<>();
      fixture.path("tags").forEach(item -> tags.add(item.asText()));
      result.add(new CaseInput(
          fixture.path("id").asText(),
          "boundary",
          asOf,
          JSON.treeToValue(fixture.path("request"), PaipanRequest.class),
          null,
          List.copyOf(tags)));
    }
    return result;
  }

  private static PreflightResult run(CaseInput input, ZoneId zone) {
    List<String> failures = new ArrayList<>();
    try {
      Clock clock = Clock.fixed(input.asOf().atStartOfDay(zone).toInstant(), zone);
      PaipanResultDto chart = input.classicPillars() == null
          ? new BaziService().paipan(input.request())
          : chartFromCanonicalPillars(input.classicPillars());
      AnnualContextFactory contextFactory = new AnnualContextFactory(clock);
      List<AnnualContext> product = contextFactory.create(
          input.request(), chart, ReportHorizon.of(3));
      AnnualContext previous = contextFactory.createYear(
          input.request(), chart, product.get(0).year() - 1);
      List<AnnualContext> fullWindow = new ArrayList<>();
      fullWindow.add(previous);
      fullWindow.addAll(product);

      List<OverallTopicSnapshot> snapshots = new OverallSnapshotFactory(clock)
          .create(input.request(), chart, fullWindow);
      List<OverallAnnualDecision> decisions = new OverallDecisionArbitrator()
          .arbitratePeriod(snapshots);
      List<OverallTopicSnapshot> previousSnapshots = snapshots.stream()
          .filter(item -> item.year() == previous.year()).toList();
      List<OverallTopicSnapshot> productSnapshots = snapshots.stream()
          .filter(item -> item.year() != previous.year()).toList();
      OverallV3NarrativePlan report = new OverallV3NarrativePlanner().plan(
          productSnapshots,
          decisions.subList(1, decisions.size()),
          previousSnapshots,
          decisions.get(0));
      OverallV3NarrativePlan repeated = new OverallV3NarrativePlanner().plan(
          productSnapshots,
          decisions.subList(1, decisions.size()),
          previousSnapshots,
          decisions.get(0));
      boolean deterministic = JSON.valueToTree(report).equals(JSON.valueToTree(repeated));
      inspect(report, failures);
      List<ActionTuple> tuples = report.years().stream().map(year -> new ActionTuple(
          year.actionGuide().focusKey(),
          year.actionGuide().action(),
          year.actionGuide().expectedChange(),
          year.actionGuide().successSignal())).toList();
      return new PreflightResult(
          input.id(),
          input.source(),
          input.tags(),
          pillars(chart),
          snapshots.size(),
          digest(JSON.writeValueAsString(snapshotProjection(snapshots))),
          annualFingerprints(snapshots),
          decisions.subList(1, decisions.size()).stream().map(decision ->
              decision.primary().focusKey() + " -> " + decision.secondary().focusKey()).toList(),
          decisions.subList(1, decisions.size()).stream()
              .map(OverallAnnualDecision::conflictKey).toList(),
          tuples,
          snapshots.stream().flatMap(item -> item.evidenceKeys().stream()).distinct().toList(),
          deterministic,
          report,
          List.copyOf(failures));
    } catch (Exception error) {
      failures.add("generation: " + error.getClass().getSimpleName() + " - " + error.getMessage());
      return new PreflightResult(
          input.id(), input.source(), input.tags(), "", 0, "", Map.of(), List.of(), List.of(),
          List.of(), List.of(), false, null, List.copyOf(failures));
    }
  }

  private static void inspect(OverallV3NarrativePlan report, List<String> failures) {
    duplicate(report.years().stream().map(OverallV3NarrativePlan.YearNarrative::headline).toList(),
        "headline", failures);
    duplicate(report.years().stream().map(OverallV3NarrativePlan.YearNarrative::linkage).toList(),
        "linkage", failures);
    duplicate(report.years().stream().map(year -> year.actionGuide().action()).toList(),
        "action", failures);
    duplicate(report.years().stream().map(year -> year.actionGuide().expectedChange()).toList(),
        "expectedChange", failures);
    duplicate(report.years().stream().map(year -> year.actionGuide().successSignal()).toList(),
        "successSignal", failures);

    visibleCopy(report).forEach(line -> inspectLine(line, failures));
    adviceCopy(report).forEach(line -> {
      if (chineseLength(line) > 48) {
        failures.add("language: advice exceeds 48 Chinese characters: " + line);
      }
      String body = line.replaceAll("[。！？]+$", "");
      if (DANGLING.stream().anyMatch(body::endsWith)) {
        failures.add("language: sentence ends without an object or predicate: " + line);
      }
      if (PREDICATES.stream().noneMatch(body::contains)) {
        failures.add("language: advice lacks an observable predicate: " + line);
      }
    });
  }

  private static void inspectLine(String line, List<String> failures) {
    if (line == null || line.isBlank()) {
      failures.add("language: blank visible copy");
      return;
    }
    FORBIDDEN.stream().filter(line::contains)
        .forEach(value -> failures.add("language: forbidden phrase " + value + ": " + line));
    if (PLACEHOLDER.matcher(line).matches()) {
      failures.add("language: unresolved placeholder: " + line);
    }
    BAD_PUNCTUATION.stream().filter(line::contains)
        .forEach(value -> failures.add("language: abnormal punctuation " + value + ": " + line));
  }

  private static void duplicate(List<String> values, String field, List<String> failures) {
    if (new LinkedHashSet<>(values).size() != values.size()) {
      failures.add("duplicate: " + field + " " + values);
    }
  }

  private static List<String> visibleCopy(OverallV3NarrativePlan report) {
    List<String> values = new ArrayList<>(List.of(
        report.thesis(), report.summary(), report.readingNote()));
    for (OverallV3NarrativePlan.YearNarrative year : report.years()) {
      values.add(year.headline());
      values.add(year.linkage());
      values.add(year.transition());
      values.addAll(year.actionGuide().lines());
      year.observations().forEach(item -> values.add(item.note()));
    }
    NarrativeTimeline timeline = report.timeline();
    values.add(timeline.past().headline());
    values.addAll(timeline.past().checkpoints());
    values.add(timeline.past().bridge());
    values.add(timeline.present().headline());
    values.add(timeline.present().judgment());
    values.add(timeline.present().priority());
    timeline.future().forEach(item -> {
      values.add(item.headline());
      values.add(item.action());
    });
    return List.copyOf(values);
  }

  private static List<String> adviceCopy(OverallV3NarrativePlan report) {
    List<String> result = new ArrayList<>();
    report.years().forEach(year -> {
      AnnualActionGuide guide = year.actionGuide();
      result.add(guide.action());
      result.add(guide.expectedChange());
      result.add(guide.checkTiming());
      result.add(guide.successSignal());
      result.add(guide.adjustmentCondition());
      result.add(guide.fallbackAction());
    });
    return List.copyOf(result);
  }

  private static Map<String, List<String>> tupleCollisions(List<PreflightResult> samples) {
    Map<String, Set<String>> focusesByTuple = new LinkedHashMap<>();
    Map<String, List<String>> locationsByTuple = new LinkedHashMap<>();
    samples.forEach(sample -> sample.actionTuples().forEach(tuple -> {
      String key = tupleKey(tuple);
      focusesByTuple.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(tuple.focusKey());
      locationsByTuple.computeIfAbsent(key, ignored -> new ArrayList<>())
          .add(sample.id() + ":" + tuple.focusKey());
    }));
    Map<String, List<String>> collisions = new LinkedHashMap<>();
    focusesByTuple.forEach((tuple, focuses) -> {
      if (focuses.size() > 1) collisions.put(tuple, locationsByTuple.get(tuple));
    });
    return collisions;
  }

  private static String tupleKey(ActionTuple tuple) {
    return tuple.action() + "|" + tuple.expectedChange() + "|" + tuple.successSignal();
  }

  private static Map<String, Long> distribution(List<String> values) {
    return values.stream().collect(Collectors.groupingBy(
        value -> value, LinkedHashMap::new, Collectors.counting()));
  }

  private static List<List<String>> fingerprintCollisions(List<PreflightResult> samples) {
    Map<String, List<String>> idsByFingerprint = new LinkedHashMap<>();
    samples.stream().filter(item -> !item.snapshotFingerprint().isBlank()).forEach(item ->
        idsByFingerprint.computeIfAbsent(item.snapshotFingerprint(), ignored -> new ArrayList<>())
            .add(item.id()));
    return idsByFingerprint.values().stream().filter(ids -> ids.size() > 1).toList();
  }

  private static Map<String, Long> sameReportRepeatCounts(List<PreflightResult> samples) {
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put("headline", repeatCaseCount(samples,
        year -> year.headline()));
    counts.put("linkage", repeatCaseCount(samples,
        year -> year.linkage()));
    counts.put("action", repeatCaseCount(samples,
        year -> year.actionGuide().action()));
    counts.put("expectedChange", repeatCaseCount(samples,
        year -> year.actionGuide().expectedChange()));
    counts.put("successSignal", repeatCaseCount(samples,
        year -> year.actionGuide().successSignal()));
    return Map.copyOf(counts);
  }

  private static long repeatCaseCount(
      List<PreflightResult> samples,
      java.util.function.Function<OverallV3NarrativePlan.YearNarrative, String> field) {
    return samples.stream().filter(item -> item.report() != null).filter(item -> {
      List<String> values = item.report().years().stream().map(field).toList();
      return new LinkedHashSet<>(values).size() != values.size();
    }).count();
  }

  private static Map<String, List<String>> evidenceDistribution(List<PreflightResult> samples) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    samples.forEach(sample -> sample.evidenceKeys().forEach(key ->
        result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(sample.id())));
    result.replaceAll((key, ids) -> ids.stream().distinct().toList());
    return result;
  }

  private static List<Map<String, Object>> snapshotProjection(List<OverallTopicSnapshot> snapshots) {
    return snapshots.stream().map(item -> {
      Map<String, Object> value = new LinkedHashMap<>();
      value.put("year", item.year());
      value.put("topic", item.topic());
      value.put("focusKey", item.focusKey());
      value.put("stance", item.stance());
      value.put("urgency", item.urgency());
      value.put("confidence", item.confidence());
      value.put("directEvidenceCount", item.directEvidenceCount());
      value.put("opportunityKey", item.opportunityKey());
      value.put("riskKey", item.riskKey());
      value.put("actionCandidateKeys", item.actionCandidateKeys());
      value.put("evidenceKeys", item.evidenceKeys());
      return value;
    }).toList();
  }

  private static Map<Integer, String> annualFingerprints(
      List<OverallTopicSnapshot> snapshots) throws Exception {
    Map<Integer, List<OverallTopicSnapshot>> byYear = snapshots.stream().collect(Collectors.groupingBy(
        OverallTopicSnapshot::year, LinkedHashMap::new, Collectors.toList()));
    Map<Integer, String> result = new LinkedHashMap<>();
    for (Map.Entry<Integer, List<OverallTopicSnapshot>> entry : byYear.entrySet()) {
      result.put(entry.getKey(), digest(JSON.writeValueAsString(snapshotProjection(entry.getValue()))));
    }
    return Map.copyOf(result);
  }

  private static int chineseLength(String value) {
    return (int) value.codePoints()
        .filter(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN)
        .count();
  }

  private static String digest(String value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(value.getBytes(StandardCharsets.UTF_8)));
  }

  private static String pillars(PaipanResultDto chart) {
    return List.of("year", "month", "day", "time").stream()
        .map(key -> chart.pillars().get(key).gan() + chart.pillars().get(key).zhi())
        .collect(Collectors.joining("·"));
  }

  private static JsonNode read(String resource) throws Exception {
    try (InputStream input = OverallV3PreflightTest.class.getResourceAsStream(resource)) {
      if (input == null) throw new IllegalArgumentException("missing resource: " + resource);
      return JSON.readTree(input);
    }
  }

  private static PaipanResultDto chartFromCanonicalPillars(List<String> ganZhi) {
    String dayGan = ganZhi.get(2).substring(0, 1);
    Map<String, PillarDto> pillars = new LinkedHashMap<>();
    List<String> keys = List.of("year", "month", "day", "time");
    Map<String, Integer> wuXing = new LinkedHashMap<>(Map.of(
        "jin", 0, "mu", 0, "shui", 0, "huo", 0, "tu", 0));
    for (int index = 0; index < keys.size(); index++) {
      String value = ganZhi.get(index);
      String gan = value.substring(0, 1);
      String zhi = value.substring(1, 2);
      List<HideGanDto> hidden = LunarUtil.ZHI_HIDE_GAN.getOrDefault(zhi, List.of()).stream()
          .map(hiddenGan -> new HideGanDto(
              hiddenGan,
              LunarUtil.SHI_SHEN.get(dayGan + hiddenGan),
              WuXingConstants.GAN_WUXING.get(hiddenGan)))
          .toList();
      pillars.put(keys.get(index), new PillarDto(
          keys.get(index), gan, zhi,
          index == 2 ? "日主" : LunarUtil.SHI_SHEN.get(dayGan + gan),
          ZiZuoConstants.ziZuo(gan, zhi), hidden,
          LunarUtil.NAYIN.getOrDefault(value, ""), "", LunarUtil.getXunKong(value), List.of()));
      wuXing.merge(WuXingConstants.GAN_WUXING.get(gan), 1, Integer::sum);
      wuXing.merge(WuXingConstants.ZHI_WUXING.get(zhi), 1, Integer::sum);
    }
    return new PaipanResultDto(
        "古籍四柱直录", "古籍四柱直录", "", ganZhi.get(3).substring(1, 2), pillars,
        "", "", "", "", "", "", wuXing,
        List.of(), null, List.of(), List.of(), List.of(), "", null, null);
  }

  private record CaseInput(
      String id,
      String source,
      LocalDate asOf,
      PaipanRequest request,
      List<String> classicPillars,
      List<String> tags) {}

  private record ActionTuple(
      String focusKey,
      String action,
      String expectedChange,
      String successSignal) {}

  private record PreflightResult(
      String id,
      String source,
      List<String> tags,
      String pillars,
      int snapshotCount,
      String snapshotFingerprint,
      Map<Integer, String> annualSnapshotFingerprints,
      List<String> primarySecondaryPath,
      List<String> conflictKeys,
      List<ActionTuple> actionTuples,
      List<String> evidenceKeys,
      boolean deterministic,
      OverallV3NarrativePlan report,
      List<String> failures) {}
}
