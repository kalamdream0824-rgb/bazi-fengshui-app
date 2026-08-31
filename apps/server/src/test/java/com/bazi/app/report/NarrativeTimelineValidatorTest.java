package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NarrativeTimelineValidatorTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void rejectsTheSameCompleteSentenceAcrossTimelineSectionsDespitePunctuation() {
    NarrativeTimeline timeline = timeline(
        "先把收入来源理清。",
        "先把收入来源理清",
        "明年再确认新的收入方向");

    assertThrows(IllegalArgumentException.class,
        () -> new NarrativeTimelineValidator(List.of("收入来源")).validate(timeline));
  }

  @Test
  void rejectsAnExplicitFourCharacterCorePhraseSharedByDifferentSections() {
    NarrativeTimeline timeline = new NarrativeTimeline(
        past(List.of("回看去年是否调整收入来源", "如果回款变慢，是否影响日常开销"), "今年先看结余"),
        present("今年收入来源需要先稳定", "把每月必要开销列清楚"),
        List.of(future(2027, "再考虑增加额外进账")));

    assertThrows(IllegalArgumentException.class,
        () -> new NarrativeTimelineValidator(List.of("收入来源")).validate(timeline));
  }

  @Test
  void acceptsDistinctCopyAndPreservesDeclaredPhraseOrder() {
    NarrativeTimeline timeline = timeline(
        "今年先核对固定开销",
        "先留出三个月日常支出",
        "明年再考虑新的赚钱方式");
    NarrativeTimelineValidator validator = new NarrativeTimelineValidator(
        List.of("收入来源", "日常支出", "收入来源"));

    assertEquals(timeline, validator.validate(timeline));
    assertEquals(List.of("收入来源", "日常支出"), validator.corePhrases());
  }

  @Test
  void requiresAuditedCorePhrasesRatherThanFunctionalTimelineLabels() {
    assertThrows(IllegalArgumentException.class,
        () -> new NarrativeTimelineValidator(List.of("回看去年")));
    assertThrows(IllegalArgumentException.class,
        () -> new NarrativeTimelineValidator(List.of("收入")));
  }

  @Test
  void plannerInputKeepsTopicPreviousResultsProductOrderAndOptionalState() {
    NarrativeTimelinePlanner.Input<String, String> input = new NarrativeTimelinePlanner.Input<>(
        "wealth",
        "previous-evaluation",
        List.of("present-evaluation", "future-evaluation"),
        Optional.of("user-state"));

    assertEquals("wealth", input.topicCode());
    assertEquals("previous-evaluation", input.previousEvaluation());
    assertEquals(List.of("present-evaluation", "future-evaluation"), input.productEvaluations());
    assertEquals(Optional.of("user-state"), input.userState());
    assertThrows(UnsupportedOperationException.class,
        () -> input.productEvaluations().add("another-year"));
  }

  @Test
  void samePlannerInputProducesByteIdenticalJson() throws JsonProcessingException {
    NarrativeTimeline expected = timeline(
        "今年先核对固定开销",
        "先留出三个月日常支出",
        "明年再考虑新的赚钱方式");
    NarrativeTimelinePlanner<String, String> planner = input -> expected;
    NarrativeTimelinePlanner.Input<String, String> input = new NarrativeTimelinePlanner.Input<>(
        "wealth", "previous", List.of("present", "future"), Optional.empty());

    String first = objectMapper.writeValueAsString(planner.plan(input));
    String second = objectMapper.writeValueAsString(planner.plan(input));

    assertEquals(first, second);
  }

  private static NarrativeTimeline timeline(String bridge, String priority, String futureAction) {
    return new NarrativeTimeline(
        past(List.of("回看去年收入是否稳定", "如果开销增加，是否留下足够余钱"), bridge),
        present("今年先稳住日常收支", priority),
        List.of(future(2027, futureAction)));
  }

  private static NarrativeTimeline.PastReview past(List<String> checkpoints, String bridge) {
    return new NarrativeTimeline.PastReview(
        2025, "去年收支回看", checkpoints, bridge, List.of("past.income"));
  }

  private static NarrativeTimeline.PresentReading present(String judgment, String priority) {
    return new NarrativeTimeline.PresentReading(
        2026, "当下收支判断", judgment, priority, List.of("present.income"));
  }

  private static NarrativeTimeline.FutureStep future(int year, String action) {
    return new NarrativeTimeline.FutureStep(
        year, year + "年钱财行动", action, List.of("future.income." + year));
  }
}
