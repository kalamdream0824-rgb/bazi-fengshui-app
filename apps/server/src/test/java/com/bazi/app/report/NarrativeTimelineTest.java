package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class NarrativeTimelineTest {

  @Test
  void keepsAnImmutablePreviousPresentAndContinuousFutureTimeline() {
    NarrativeTimeline timeline = timeline(
        past(2025, List.of("回看去年是否换过工作重点", "如果去年职责增加，收入是否同步变化")),
        present(2026),
        List.of(future(2027, "先确认新的工作方向"), future(2028, "再决定是否扩大投入")));

    assertEquals(2025, timeline.past().year());
    assertEquals(2026, timeline.present().year());
    assertEquals(List.of(2027, 2028), timeline.future().stream()
        .map(NarrativeTimeline.FutureStep::year)
        .toList());
    assertThrows(UnsupportedOperationException.class,
        () -> timeline.past().checkpoints().add("回看其他事情"));
  }

  @Test
  void rejectsPastYearThatIsNotImmediatelyBeforePresent() {
    assertThrows(IllegalArgumentException.class,
        () -> timeline(past(2024, checkpoints()), present(2026), List.of(future(2027, "调整工作安排"))));
  }

  @Test
  void requiresExactlyTwoDistinctPastCheckpoints() {
    assertThrows(IllegalArgumentException.class,
        () -> timeline(past(2025, List.of("回看去年工作是否有变化")), present(2026), List.of()));
    assertThrows(IllegalArgumentException.class,
        () -> timeline(
            past(2025, List.of("回看去年工作是否有变化", "回看去年工作是否有变化")),
            present(2026),
            List.of()));
  }

  @Test
  void rejectsNonContinuousOrNonFutureYears() {
    assertThrows(IllegalArgumentException.class,
        () -> timeline(
            past(2025, checkpoints()),
            present(2026),
            List.of(future(2027, "先稳定工作"), future(2029, "再考虑变化"))));
    assertThrows(IllegalArgumentException.class,
        () -> timeline(past(2025, checkpoints()), present(2026), List.of(future(2026, "处理现在的问题"))));
  }

  @Test
  void requiresEvidenceForEveryTimelineSection() {
    assertThrows(IllegalArgumentException.class,
        () -> timeline(
            new NarrativeTimeline.PastReview(2025, "去年回看", checkpoints(), "再看今年", List.of()),
            present(2026),
            List.of()));
    assertThrows(IllegalArgumentException.class,
        () -> timeline(past(2025, checkpoints()),
            new NarrativeTimeline.PresentReading(2026, "当下判断", "先稳定工作", "先完成手头任务", List.of()),
            List.of()));
    assertThrows(IllegalArgumentException.class,
        () -> timeline(past(2025, checkpoints()), present(2026),
            List.of(new NarrativeTimeline.FutureStep(2027, "明年行动", "先确认方向", List.of()))));
  }

  @Test
  void rejectsDuplicateCopyAcrossTimelineFields() {
    NarrativeTimeline.PastReview past = new NarrativeTimeline.PastReview(
        2025,
        "去年工作有过调整",
        checkpoints(),
        "先完成手头任务",
        List.of("past.fact"));

    assertThrows(IllegalArgumentException.class,
        () -> timeline(past, present(2026), List.of()));
  }

  @Test
  void acceptsOnlyRetrospectiveLanguageAndRejectsAssertions() {
    for (String checkpoint : List.of(
        "你去年已经换过工作",
        "你去年一定遇到阻力",
        "去年必然出现收入变化",
        "事实证明工作方向不合适",
        "准确命中了你的离职经历")) {
      assertThrows(IllegalArgumentException.class,
          () -> RetrospectiveLanguagePolicy.validate(List.of(checkpoint, "回看去年收入是否稳定")));
    }

    RetrospectiveLanguagePolicy.validate(checkpoints());
  }

  @Test
  void requiresEachCheckpointToUseReviewLanguage() {
    assertThrows(IllegalArgumentException.class,
        () -> RetrospectiveLanguagePolicy.validate(List.of("去年工作发生变化", "回看去年收入是否稳定")));
  }

  private static NarrativeTimeline timeline(
      NarrativeTimeline.PastReview past,
      NarrativeTimeline.PresentReading present,
      List<NarrativeTimeline.FutureStep> future) {
    return new NarrativeTimeline(past, present, future);
  }

  private static NarrativeTimeline.PastReview past(int year, List<String> checkpoints) {
    return new NarrativeTimeline.PastReview(
        year,
        "去年回看",
        checkpoints,
        "今年先处理最重要的问题",
        List.of("past.fact"));
  }

  private static NarrativeTimeline.PresentReading present(int year) {
    return new NarrativeTimeline.PresentReading(
        year,
        "当下判断",
        "今年工作更看重稳定",
        "先完成手头任务",
        List.of("present.fact"));
  }

  private static NarrativeTimeline.FutureStep future(int year, String action) {
    return new NarrativeTimeline.FutureStep(year, "未来行动" + year, action, List.of("future.fact." + year));
  }

  private static List<String> checkpoints() {
    return List.of("回看去年工作是否有变化", "如果去年收入有波动，是否与工作变化有关");
  }
}
