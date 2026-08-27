package com.bazi.app.report.relationship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Evidence-led annual copy. Calendar position is deliberately not a writing input. */
final class RelationshipAnnualNarrator {

  String judgment(String prefix, RelationshipPeriodEvaluation.Year previous,
      RelationshipPeriodEvaluation.Year current) {
    if (previous != null) return comparison(prefix, previous, current, "今年", "上一年");
    var primary = current.focus().primaryDimension();
    String base = RelationshipPlainCopy.get(prefix + ".year.judgment." + primary.code()
        + "." + current.dimensions().get(primary).tone().name().toLowerCase(Locale.ROOT));
    var annual = current.dimensions().values().stream()
        .flatMap(dimension -> dimension.evidence().stream())
        .filter(item -> item.family() != RelationshipEvidenceFamily.NATAL_STRUCTURE)
        .sorted(Comparator.comparingInt((RelationshipEvidence item) ->
                item.dimension() == primary ? 0 : 1)
            .thenComparing(Comparator.comparingInt((RelationshipEvidence item) ->
                Math.abs(item.weight())).reversed())
            .thenComparing(RelationshipEvidence::key))
        .findFirst();
    return annual.filter(item -> !cue(item).equals(
            RelationshipPlainCopy.get("annual.cue." + primary.code())))
        .map(item -> base + "尤其要看" + cue(item) + "。").orElse(base);
  }

  String focus(String prefix, RelationshipPeriodEvaluation.Year year) {
    return "重点看" + topic(prefix, year.focus().primaryDimension()) + "。";
  }

  List<String> signals(String prefix, RelationshipPeriodEvaluation.Year year) {
    return selectedDimensions(year).stream().map(dimension ->
        RelationshipPlainCopy.get(prefix + ".observe." + dimension.code()
            + (year.dimensions().get(dimension).limitationWeight() > 0 ? ".cautious" : ".normal")))
        .toList();
  }

  List<String> actions(String prefix, RelationshipPeriodEvaluation.Year year) {
    return selectedDimensions(year).stream().map(dimension ->
        RelationshipPlainCopy.get(prefix + ".action." + dimension.code()
            + (year.dimensions().get(dimension).limitationWeight() > 0 ? ".cautious" : ".normal")))
        .toList();
  }

  String transition(String prefix, RelationshipPeriodEvaluation.Year current,
      RelationshipPeriodEvaluation.Year next) {
    if (next == null) return "后续仍以实际相处为准，本报告不延伸判断未计算的年份。";
    var comparison = RelationshipYearComparison.between(current, next);
    String lead = comparison.focusChanged()
        ? next.year() + "年重点转向" + topic(prefix, next.focus().primaryDimension()) + "。"
        : next.year() + "年延续本年的重点。";
    if (comparison.changes().isEmpty()) return lead + "相关因素与本年接近，继续按相处事实判断。";
    var change = comparison.changes().get(0);
    List<String> parts = new ArrayList<>();
    if (change.supportDelta() != 0) parts.add(deltaCopy("support", change.supportDelta(), change.dimension()).split("，", 2)[0]);
    if (change.limitDelta() != 0) parts.add(deltaCopy("limit", change.limitDelta(), change.dimension()).split("，", 2)[0]);
    if (parts.isEmpty()) return lead + "形成这一判断的依据与本年不同，具体见下一年正文。";
    String connector = change.supportDelta() * change.limitDelta() > 0 ? "。但" : "。同时";
    return lead + "相较" + current.year() + "年，" + String.join(connector, parts) + "。";
  }

  private String comparison(String prefix, RelationshipPeriodEvaluation.Year previous,
      RelationshipPeriodEvaluation.Year current, String when, String comparedWith) {
    var comparison = RelationshipYearComparison.between(previous, current);
    String currentTopic = topic(prefix, current.focus().primaryDimension());
    if (!comparison.focusChanged() && comparison.changes().isEmpty()) {
      return when + "仍重点看" + currentTopic + "。与" + comparedWith
          + "相比，相关因素没有明显变化，不必仅因年份变化就改变判断标准。";
    }
    String lead = comparison.focusChanged()
        ? when + "的重点从" + topic(prefix, previous.focus().primaryDimension()) + "转向" + currentTopic + "。"
        : when + "仍重点看" + currentTopic + "。";
    // At most two change statements. Advice belongs in the actions, not in this short judgment.
    List<String> details = new ArrayList<>();
    for (var change : comparison.changes()) {
      List<String> parts = new ArrayList<>();
      if (change.supportDelta() != 0) parts.add(deltaCopy("support", change.supportDelta(), change.dimension()).split("，", 2)[0]);
      if (change.limitDelta() != 0) parts.add(
          (change.supportDelta() * change.limitDelta() > 0 ? "但" : "")
              + deltaCopy("limit", change.limitDelta(), change.dimension()).split("，", 2)[0]);
      if (!parts.isEmpty()) {
        // Never split a support/limitation pair into a one-sided conclusion.
        if (details.size() + parts.size() > 2) break;
        details.addAll(parts);
      } else {
        String before = cue(change.removed().get(0));
        String after = cue(change.added().get(0));
        if (before.equals(after)) {
          details.add("依据有所变化，但具体仍要看" + after);
        } else {
          if (!details.isEmpty()) break;
          details.add("变化在于：原先侧重看" + before);
          details.add("现在更侧重看" + after);
        }
      }
      if (details.size() == 2) break;
    }
    return lead + "相比" + comparedWith + "，" + String.join("。", details) + "。";
  }

  private String deltaCopy(String kind, int delta, RelationshipDimension dimension) {
    return RelationshipPlainCopy.get("annual.delta." + kind + "."
        + (delta > 0 ? "up" : "down") + "." + dimension.code());
  }

  private String topic(String prefix, RelationshipDimension dimension) {
    return RelationshipPlainCopy.get(prefix + ".topic." + dimension.code());
  }

  private List<RelationshipDimension> selectedDimensions(RelationshipPeriodEvaluation.Year year) {
    var primary = year.focus().primaryDimension();
    var other = year.mainRisk() != null && year.mainRisk().dimension() != primary
        ? year.mainRisk().dimension() : year.focus().secondaryDimension();
    return List.of(primary, other);
  }

  private String cue(RelationshipEvidence evidence) {
    String key = evidence.key();
    if (key.startsWith("annual.branch.harmony.")) return "相处安排能否互相配合";
    if (key.startsWith("annual.branch.")) return "分歧是否影响日常相处";
    if (key.startsWith("annual.stem.group.output.")) return "双方是否愿意主动表达想法";
    if (key.startsWith("annual.stem.group.resource.")) return "双方能否耐心听懂彼此的需要";
    if (key.startsWith("dayun.")) return "长期形成的相处习惯是否仍然合适";
    return RelationshipPlainCopy.get("annual.cue." + evidence.dimension().code());
  }
}
