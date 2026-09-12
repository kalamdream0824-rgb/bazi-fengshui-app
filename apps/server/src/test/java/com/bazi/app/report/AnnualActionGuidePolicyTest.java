package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class AnnualActionGuidePolicyTest {

  @Test
  void rejectsTheSameDecisionForDifferentCalculationFocuses() {
    AnnualActionGuide first = guide(
        "career.visibility", "先整理一项已经做成的工作。", "让负责人看见你的能力。", "收到负责人明确反馈。");
    AnnualActionGuide second = guide(
        "career.coordination_window", "先整理一项已经做成的工作。", "让负责人看见你的能力。", "收到负责人明确反馈。");

    assertThrows(IllegalArgumentException.class,
        () -> AnnualActionGuidePolicy.validate(List.of(first, second)));
  }

  @Test
  void acceptsDifferentDecisionsForDifferentCalculationFocuses() {
    AnnualActionGuide first = guide(
        "career.visibility", "先整理一项已经做成的工作。", "让负责人看见你的能力。", "收到负责人明确反馈。");
    AnnualActionGuide second = guide(
        "career.coordination_window", "先找一位能参与此事的人。", "让合作更容易继续推进。", "对方答应承担一项具体工作。");

    assertDoesNotThrow(() -> AnnualActionGuidePolicy.validate(List.of(first, second)));
  }

  @Test
  void rejectsRepeatedSentencesAcrossProductYears() {
    AnnualActionGuide first = guide(
        "career.visibility", "先整理一项已经做成的工作。", "让负责人看见你的能力。", "收到负责人明确反馈。");
    AnnualActionGuide second = guide(
        "career.coordination_window", "先找一位能参与此事的人。", "让合作更容易继续推进。", "收到负责人明确反馈。");

    assertThrows(IllegalArgumentException.class,
        () -> AnnualActionGuidePolicy.validate(List.of(first, second)));
  }

  @Test
  void rejectsVagueOrUnfinishedReaderCopy() {
    AnnualActionGuide vague = guide(
        "career.visibility", "先处理现在卡住的地方。", "情况会有所改善。", "后续再看。");

    assertThrows(IllegalArgumentException.class,
        () -> AnnualActionGuidePolicy.validate(List.of(vague)));
  }

  private AnnualActionGuide guide(
      String focusKey,
      String action,
      String expectedChange,
      String successSignal) {
    return new AnnualActionGuide(
        focusKey,
        focusKey.contains("visibility")
            ? "当前的工作结果还没有被看见。"
            : "当前的工作需要别人配合才能继续。",
        action,
        expectedChange,
        focusKey.contains("visibility")
            ? "开始执行后，每周检查一次；四周后统一判断。"
            : "开始执行后，每两周核对一次；六周后统一判断。",
        successSignal,
        focusKey.contains("visibility")
            ? "如果四周后没有明确反馈，就调整展示方法。"
            : "如果六周后仍无人配合，就重新划分任务。",
        focusKey.contains("visibility")
            ? "请负责人指出还缺少哪项结果。"
            : "把事情拆小，再确认一项明确分工。",
        List.of("annual.rule", "natal.fact"));
  }
}
