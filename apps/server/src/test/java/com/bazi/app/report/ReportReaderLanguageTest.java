package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.relationship.RelationshipDimensionEvaluator;
import com.bazi.app.report.relationship.RelationshipFactExtractor;
import com.bazi.app.report.relationship.RelationshipNarrativePlan;
import com.bazi.app.report.relationship.RelationshipNarrativePlanner;
import com.bazi.app.report.relationship.RelationshipPeriodArbitrator;
import com.bazi.app.report.relationship.RelationshipPeriodEvaluation;
import com.bazi.app.report.relationship.RelationshipStatus;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ReportReaderLanguageTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

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
}
