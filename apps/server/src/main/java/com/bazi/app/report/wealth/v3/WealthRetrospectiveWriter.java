package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.wealth.v3.WealthRetrospectivePlan.Observation;
import java.util.List;
import java.util.Objects;

/** Turns a traceable retrospective plan into closed, plain-language review prompts. */
public final class WealthRetrospectiveWriter {

  public NarrativeTimeline.PastReview write(WealthRetrospectivePlan plan) {
    Objects.requireNonNull(plan, "retrospective plan");
    if (plan.primary() == null) {
      throw new IllegalArgumentException("wealth retrospective review requires primary evidence");
    }
    String secondary = plan.secondary() == null
        ? "次判断：本次回看不再加入另一项具体判断。"
        : line("次判断：再回看", plan.secondary());
    String hidden = plan.hidden() == null
        ? "隐性影响：本次回看不再加入另一项具体影响。"
        : hiddenLine(plan.hidden());
    return new NarrativeTimeline.PastReview(
        plan.year(),
        line("主判断：" + plan.year() + "年先回看", "命盘提示，", plan.primary()),
        List.of(secondary, hidden),
        WealthPlainCopyV3.retrospectiveBridge(
            plan.primary().subject(), plan.primary().direction()),
        plan.evidenceKeys());
  }

  private String line(String prefix, Observation observation) {
    return line(prefix, "同时，", observation);
  }

  private String line(String prefix, String transition, Observation observation) {
    return prefix + WealthPlainCopyV3.retrospectiveQuestion(observation.subject()) + "。"
        + transition + WealthPlainCopyV3.retrospectiveDirection(
            observation.direction(), observation.strength())
        + WealthPlainCopyV3.retrospectiveAngle(observation.angle());
  }

  private String hiddenLine(Observation observation) {
    return "隐性影响：还要回看"
        + WealthPlainCopyV3.retrospectiveAngleQuestion(observation.angle()) + "。"
        + "它主要关系到" + WealthPlainCopyV3.retrospectiveObject(observation.subject()) + "。"
        + "从整体收支看，" + WealthPlainCopyV3.retrospectiveDirection(
            observation.direction(), observation.strength());
  }
}
