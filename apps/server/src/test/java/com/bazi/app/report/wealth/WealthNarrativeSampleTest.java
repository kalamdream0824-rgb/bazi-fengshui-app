package com.bazi.app.report.wealth;

import static com.bazi.app.report.wealth.WealthRemediationFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthNarrativeV3;
import com.bazi.app.report.wealth.v3.WealthNarrativeWriter;
import com.bazi.app.report.wealth.v3.DefaultWealthReportGenerator;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.ZoneId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WealthNarrativeSampleTest {
  private final WealthNarrativeWriter writer = new WealthNarrativeWriter();
  record Sample(String id, String kind, WealthNarrativeV3 content) {}

  @Test
  void completeBirthAndSyntheticSamplesAreRealEngineOutputWithoutChangingAnyJudgment() throws Exception {
    List<Sample> samples = new ArrayList<>();
    int fullYears = 0;
    for (var source : read("wealth-v2-baseline-20260829.json").get("cases")) {
      if (!source.get("kind").asText().equals("full_birth_input")) continue;
      String id = source.get("id").asText();
      var input = fullBirthAssessments(id);
      var content = writer.plan(input, LocalDate.parse(source.get("asOf").asText()));
      assertNotNull(content);
      assertDoesNotThrow(() -> writer.validateDraft(content), id);
      assertEquals(input.size(), content.years().size());
      fullYears += input.size();
      for (int i = 0; i < input.size(); i++) {
        var original = input.get(i);
        var year = content.years().get(i);
        assertEquals(original.facts(), year.facts());
        assertEquals(original.evidence(), year.evidence());
        assertEquals(original.decisions(), year.decisions());
        assertEquals(original.focus(), year.focus());
        assertEquals(original.risk() == null, year.risk() == null);
      }
      samples.add(new Sample(id, "full_birth_input", content));
    }
    assertEquals(15, samples.size());
    assertEquals(46, fullYears);
    for (String id : List.of("S02", "S03", "S04", "S05", "S06", "S07", "S08")) {
      var content = writer.plan(WealthNarrativeV3Test.assessments(scoredCase(id)), LocalDate.of(2026, 8, 29));
      assertDoesNotThrow(() -> writer.validateDraft(content));
      samples.add(new Sample(id, "synthetic_scored_input", content));
    }
    assertEquals(22, samples.size());
    if (Boolean.getBoolean("wealth.captureNarratives")) {
      var output = JSON.createObjectNode();
      output.put("scope", "internal-narrative-review-not-a-saved-report");
      output.set("cases", JSON.valueToTree(samples));
      output.putArray("rejectedCases").add("S01: all-zero evidence; general-advice-only content is not accepted");
      Files.writeString(Path.of("target/wealth-v3.4-money-state-narratives.json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(output) + "\n");
      List<Sample> birthReviewSamples = new ArrayList<>();
      for (String id : List.of("R01", "R05", "R09")) {
        birthReviewSamples.add(new Sample(id, "full_birth_input", generatedDraft(id)));
      }
      Files.writeString(Path.of("../../docs/samples/wealth-v3.4-money-state-birth-samples-2026-09-04.md"),
          review(birthReviewSamples, false));
      Files.writeString(Path.of("../../docs/samples/wealth-v3.4-money-state-boundary-samples-2026-09-04.md"),
          review(samples.stream().filter(s -> Set.of("S02", "S03", "S05", "S07", "S08").contains(s.id())).toList(), true));
    }
  }

  @Test
  void sameActualYearIsIdenticalInThreeFiveAndShiftedReports() throws Exception {
    var three = draft("R01");
    var shifted = draft("Y27");
    assertSameJudgment(three.years().get(1), shifted.years().get(0));
    for (var source : read("wealth-v2-baseline-20260829.json").get("cases")) {
      if (!source.get("kind").asText().equals("full_birth_input") || source.get("horizonYears").asInt() != 5) continue;
      var five = draft(source.get("id").asText());
      assertSameJudgment(three.years().get(0), five.years().get(0));
      assertSameJudgment(three.years().get(1), five.years().get(1));
    }
  }

  private void assertSameJudgment(WealthNarrativeV3.Year expected, WealthNarrativeV3.Year actual) {
    assertEquals(expected.year(), actual.year());
    assertEquals(expected.ganZhi(), actual.ganZhi());
    assertEquals(expected.facts(), actual.facts());
    assertEquals(expected.evidence(), actual.evidence());
    assertEquals(expected.decisions(), actual.decisions());
    assertEquals(expected.focus(), actual.focus());
    assertEquals(expected.risk() == null ? null : expected.risk().path(),
        actual.risk() == null ? null : actual.risk().path());
  }

  @Test
  void draftRoundTripAndAllReadingReferencesRemainValid() throws Exception {
    var content = draft("R01");
    assertEquals(content, JSON.readValue(JSON.writeValueAsString(content), WealthNarrativeV3.class));
    var decisions = content.years().stream().flatMap(y -> y.decisions().stream()).collect(Collectors.toMap(d -> d.id(), d -> d));
    for (var block : content.blocks()) for (var id : block.decisionIds()) {
      assertTrue(decisions.containsKey(id));
      assertTrue(block.years().contains(decisions.get(id).year()));
    }
    for (var year : content.years()) if (year.risk() != null) {
      var negativeIds = year.evidence().stream().filter(e -> e.path().equals(year.risk().path()) && e.weight() < 0)
          .map(e -> e.id()).collect(Collectors.toSet());
      assertEquals(negativeIds, Set.copyOf(year.risk().limitingEvidenceIds()));
    }
  }

  private WealthNarrativeV3 draft(String id) throws Exception {
    var source = find(read("wealth-v2-baseline-20260829.json").get("cases"), id);
    return writer.plan(fullBirthAssessments(id), LocalDate.parse(source.get("asOf").asText()));
  }

  private WealthNarrativeV3 generatedDraft(String id) throws Exception {
    var source = find(read("wealth-v2-baseline-20260829.json").get("cases"), id);
    var request = JSON.treeToValue(source.get("request"), PaipanRequest.class);
    var chart = new BaziService().paipan(request);
    LocalDate asOf = LocalDate.parse(source.get("asOf").asText());
    ZoneId zone = ZoneId.of("Asia/Shanghai");
    Clock clock = Clock.fixed(asOf.atStartOfDay(zone).toInstant(), zone);
    var factory = new AnnualContextFactory(clock);
    return new DefaultWealthReportGenerator().generate(
        chart,
        factory.createYear(request, chart, asOf.getYear() - 1),
        factory.create(request, chart, ReportHorizon.WEALTH_PRODUCT),
        asOf);
  }

  @Test
  void changedYearsDoNotRepeatTheSameAnnualOverviewWhenTheirEvidenceActuallyDiffers() throws Exception {
    var content = draft("R12");
    for (int i = 1; i < content.years().size(); i++) {
      var previous = content.years().get(i - 1);
      var current = content.years().get(i);
      assertEquals("changed", previous.comparison().direction());
      assertNotEquals(previous.overview().text(), current.overview().text());
      assertEquals(List.of(current.year()), current.overview().years());
      assertFalse(current.overview().text().contains("上一年"), current.overview().text());
    }
  }

  @Test
  void periodFocusRemainsInTheThesisWithoutRestoringTheGenericConclusion() throws Exception {
    var content = draft("R12");

    assertTrue(content.thesis().text().contains("新增投入"));
    assertTrue(content.thesis().text().contains("实际进账"));
    assertFalse(content.thesis().text().contains("可以作为关注的一个方向"));
    assertTrue(content.years().stream().noneMatch(year -> year.overview().text()
        .equals(content.thesis().text())));
    assertEquals(3, content.years().stream().map(year -> year.overview().text()).distinct().count());
  }

  @Test
  void sameAnnualPathWithDifferentSupportStrengthGetsDifferentHeadline() throws Exception {
    var content = draft("R09");

    assertEquals(3, content.years().stream().map(year -> year.overview().text()).distinct().count());
    assertNotEquals(content.years().get(1).overview().text(), content.years().get(2).overview().text());
  }

  @Test
  void annualIncomeHeadlineUsesNaturalChineseWithoutConsecutivePossessives() throws Exception {
    var year = draft("R09").years().get(1);
    String headline = year.overview().text();

    assertEquals("cooperation_income", year.headlineMeta().pathKey());
    assertFalse(headline.contains("的的"), headline);
    assertFalse(headline.contains("收入的有利条件"), headline);
  }

  @Test
  void revisedAnnualOverviewHasANewCopyVersion() throws Exception {
    assertEquals("wealth-plain-v3.16", draft("R12").copyVersion());
    assertEquals("wealth-headline-v3", draft("R12").headlinePlannerVersion());
  }

  @Test
  void reportLevelHeadlinesDoNotRepeatProjectAndExtraIncomeWording() throws Exception {
    for (int caseNumber = 1; caseNumber <= 12; caseNumber++) {
      var content = draft("R%02d".formatted(caseNumber));
      assertTrue(content.years().stream().filter(year ->
          year.overview().text().contains("到账节奏")).count() <= 1, content.toString());
    }
  }

  @Test
  void fixedBirthSamplesHaveAuditableNonRepeatingTimelines() throws Exception {
    for (int caseNumber = 1; caseNumber <= 12; caseNumber++) {
      String id = "R%02d".formatted(caseNumber);
      var source = find(read("wealth-v2-baseline-20260829.json").get("cases"), id);
      var request = JSON.treeToValue(source.get("request"), PaipanRequest.class);
      var chart = new BaziService().paipan(request);
      LocalDate asOf = LocalDate.parse(source.get("asOf").asText());
      ZoneId zone = ZoneId.of("Asia/Shanghai");
      Clock clock = Clock.fixed(asOf.atStartOfDay(zone).toInstant(), zone);
      var factory = new AnnualContextFactory(clock);
      var content = new DefaultWealthReportGenerator().generate(
          chart,
          factory.createYear(request, chart, asOf.getYear() - 1),
          factory.create(request, chart, ReportHorizon.WEALTH_PRODUCT),
          asOf);

      assertNotNull(content.timeline(), id);
      assertEquals(asOf.getYear() - 1, content.timeline().past().year(), id);
      assertEquals(List.of(asOf.getYear() + 1, asOf.getYear() + 2), content.timeline().future().stream()
          .map(com.bazi.app.report.NarrativeTimeline.FutureStep::year)
          .toList(), id);
      List<String> copy = new ArrayList<>();
      copy.add(content.timeline().past().headline());
      copy.addAll(content.timeline().past().checkpoints());
      copy.add(content.timeline().past().bridge());
      copy.add(content.timeline().present().headline());
      copy.add(content.timeline().present().judgment());
      copy.add(content.timeline().present().priority());
      content.timeline().future().forEach(step -> {
        copy.add(step.headline());
        copy.add(step.action());
      });
      assertEquals(copy.size(), new java.util.LinkedHashSet<>(copy).size(), id + copy);
      for (String text : copy) {
        for (String forbidden : List.of(
            "你去年已经", "你去年一定", "去年必然", "事实证明", "准确命中",
            "职责", "成果", "岗位", "升职", "具体金额")) {
          assertFalse(text.contains(forbidden), id + ": " + text);
        }
      }
      assertFalse(content.timeline().past().evidenceKeys().isEmpty(), id);
      assertFalse(content.timeline().present().evidenceKeys().isEmpty(), id);
      assertTrue(content.timeline().future().stream().allMatch(step -> !step.evidenceKeys().isEmpty()), id);
    }
  }

  @Test
  void annualOverviewDoesNotRepeatTheSameMoneyPathName() throws Exception {
    for (int caseNumber = 1; caseNumber <= 12; caseNumber++) {
      for (var year : draft("R%02d".formatted(caseNumber)).years()) {
        String text = year.overview().text();
        for (String label : List.of("进账稳定", "投入回报", "到账节奏", "资金责任", "收支结余")) {
          assertEquals(text.indexOf(label), text.lastIndexOf(label), text);
        }
      }
    }
  }

  @Test
  void retentionUsesNaturalChineseAndDoesNotDuplicateTheDetailedRiskParagraph() throws Exception {
    var year = draft("R01").years().get(0);
    assertFalse(year.retention().text().contains("钱有可以考虑的部分"));
    assertFalse(year.retention().text().endsWith(year.risk().reading().text()));
    assertTrue(year.comparison().reading().text().startsWith("与2026年相比，2027年"));
  }

  @Test
  void aTiedIncomeFocusStillLeavesAnActionForTheSelectedRisk() throws Exception {
    var year = draft("R05").years().get(0);
    assertEquals("tied", year.focus().state());
    assertNotNull(year.risk());
    assertTrue(year.actions().stream().anyMatch(b -> b.decisionIds().contains(year.year() + ".decision." + year.risk().path())));
  }

  @ParameterizedTest
  @ValueSource(strings = {"R05", "R09"})
  void sharedLimitsAreExplainedOnceAcrossRetentionAndCooperationRisk(String id) throws Exception {
    for (var year : draft(id).years()) {
      for (var sentence : year.risk().reading().text().split("(?<=。)")) {
        assertFalse(year.retention().text().contains(sentence), year.retention().text());
      }
    }
  }

  private String review(List<Sample> samples, boolean synthetic) {
    var text = new StringBuilder(synthetic ? "# 财富 v3.4：资金状态边界样本\n\n" : "# 财富 v3.4：资金状态完整出生样本\n\n");
    text.append("本文由测试程序直接渲染当前生成器的原文，未逐篇手工改写。内部仍保留五条可审计路径，用户可见文案只描述资金状态，不推断职业或收入来源。\n\n");
    text.append(synthetic ? "这些是刻意构造的计分边界，不代表正常出生条件必然产生这些结果。S01 全零已拒绝生成，不以通用建议凑成命书。\n\n"
        : "以下使用完整出生测试输入重跑排盘和内容生成，不代表真实消费者经历或现实预测验证。\n\n");
    for (var sample : samples) {
      var c = sample.content();
      text.append("## 样本 ").append(sample.id()).append("\n\n分析日期：").append(c.asOf()).append("；年限：").append(c.horizonYears()).append(" 年。\n\n");
      section(text, "总重点", List.of(c.thesis().text()));
      section(text, "五项资金状态", c.pathSummaries().stream().map(p -> p.reading().text()).toList());
      if (c.riskSummary() != null) section(text, "需要留意的年份与事项", List.of(c.riskSummary().text()));
      if (c.timeline() != null) {
        var past = new ArrayList<String>();
        past.add(c.timeline().past().headline());
        past.addAll(c.timeline().past().checkpoints());
        past.add(c.timeline().past().bridge());
        section(text, "去年回看：主判断、次判断与隐性影响", past);
      }
      for (var y : c.years()) {
        text.append("### ").append(y.year()).append(" 年\n\n").append(y.overview().text()).append("\n\n");
        section(text, "资金变化", y.income().stream().map(b -> b.text()).toList());
        section(text, "收支结余", List.of(y.retention().text()));
        if (y.risk() != null) section(text, "需要留意的一项", List.of(y.risk().reading().text()));
        section(text, "可以留意的现实情况（不是预测验证）", y.observations().stream().map(b -> b.text()).toList());
        section(text, "可以着手做的事（一般建议）", y.actions().stream().map(b -> b.text()).toList());
        if (y.comparison() != null) section(text, "下一年的变化", List.of(y.comparison().reading().text()));
      }
      section(text, "最后看能留下多少", List.of(c.summary().text()));
      section(text, "行动整理（一般建议）", c.route().stream().map(b -> b.text()).toList());
      text.append(c.readingNote().text()).append("\n\n---\n\n");
    }
    return text.toString();
  }

  private void section(StringBuilder text, String heading, List<String> paragraphs) {
    if (paragraphs.isEmpty()) return;
    text.append("#### ").append(heading).append("\n\n");
    paragraphs.forEach(p -> text.append(p).append("\n\n"));
  }
}
