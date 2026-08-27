package com.bazi.app.report.relationship;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class RelationshipSingleNarrativePlanner {
  public RelationshipSingleNarrativePlan plan(RelationshipPeriodEvaluation period) {
    Objects.requireNonNull(period, "period");
    if (period.years().size() != 2) {
      throw new IllegalArgumentException("single current-year reading requires exactly two years");
    }
    var current = period.years().get(0);
    var next = period.years().get(1);
    var primary = current.focus().primaryDimension();
    List<RelationshipSingleNarrativePlan.Section> sections = new ArrayList<>();
    sections.add(new RelationshipSingleNarrativePlan.Section(
        "opportunities", copy("title.opportunities"),
        List.of(dimensionCopy("detail", current, RelationshipDimension.CONNECTION),
            copy("scene.no_contact"), copy("scene.has_contact")),
        List.of(), evidence(current, RelationshipDimension.CONNECTION)));

    var development = List.of(
        dimensionCopy("detail", current, RelationshipDimension.RESPONSE),
        dimensionCopy("detail", current, RelationshipDimension.DAILY_COOPERATION),
        dimensionCopy("detail", current, RelationshipDimension.STABILITY));
    sections.add(new RelationshipSingleNarrativePlan.Section(
        "development", copy("title.development"), development,
        List.of(copy("observe.response"), copy("observe.daily_cooperation")),
        evidence(current, RelationshipDimension.RESPONSE, RelationshipDimension.DAILY_COOPERATION,
            RelationshipDimension.STABILITY)));

    if (current.mainRisk() != null) {
      var risk = current.mainRisk().dimension();
      sections.add(new RelationshipSingleNarrativePlan.Section(
          "attention", copy("title.attention"),
          List.of(copy("risk." + risk.code()), copy("example." + risk.code())),
          List.of(), current.mainRisk().evidenceKeys()));
    }

    return new RelationshipSingleNarrativePlan(
        "single", 2, dimensionCopy("thesis", current, primary),
        copy("summary." + (current.mainRisk() == null ? "clear" : current.mainRisk().dimension().code())),
        current.year(), next.year(), sections, outlook(current, next), copy("reading_note"),
        period.years().stream().map(RelationshipPeriodEvaluation.Year::evaluation).toList(),
        period.years().stream().flatMap(year -> evidence(year, RelationshipDimension.values()).stream())
            .distinct().sorted().toList());
  }

  private List<String> outlook(RelationshipPeriodEvaluation.Year current, RelationshipPeriodEvaluation.Year next) {
    var comparison = RelationshipYearComparison.between(current, next);
    String lead = comparison.focusChanged()
        ? "明年的重点转向" + copy("topic." + next.focus().primaryDimension().code()) + "。"
        : "明年延续今年的重点，仍要看" + copy("topic." + next.focus().primaryDimension().code()) + "。";
    if (comparison.changes().isEmpty()) {
      return List.of(lead, "两年的相关判断没有明显变化，不必只因为换了一年，就改变相处的标准。");
    }
    List<String> details = new ArrayList<>();
    // Keep the strongest relevant changes short, but never split an opposing support/limit pair.
    for (var change : comparison.changes().stream().limit(2).toList()) {
      if (change.supportDelta() != 0) {
        details.add(delta("support", change.supportDelta(), change.dimension()) + "。");
      }
      if (change.limitDelta() != 0) {
        details.add((change.supportDelta() * change.limitDelta() > 0 ? "但" : "")
            + delta("limit", change.limitDelta(), change.dimension()) + "。");
      }
      if (change.magnitude() == 0) {
        String before = cue(change.removed().get(0));
        String after = cue(change.added().get(0));
        details.add(before.equals(after)
            ? "关于" + before + "，判断强弱相同，但年度依据不同。"
            : "年度依据的区别在于：今年侧重" + before + "，明年侧重" + after + "。");
      }
    }
    return List.of(lead, String.join("", details));
  }

  private String cue(RelationshipEvidence evidence) {
    String key = evidence.key();
    if (key.startsWith("annual.branch.harmony.")) return "见面和相处安排";
    if (key.startsWith("annual.branch.")) return "分歧对相处的影响";
    if (key.startsWith("annual.stem.group.output.")) return "主动说出想法";
    if (key.startsWith("annual.stem.group.resource.")) return "耐心听懂彼此的需要";
    if (key.startsWith("dayun.")) return "长期形成的相处习惯";
    return copy("topic." + evidence.dimension().code());
  }

  private String delta(String kind, int value, RelationshipDimension dimension) {
    return copy("delta." + kind + "." + (value > 0 ? "up" : "down") + "." + dimension.code());
  }

  private String dimensionCopy(String kind, RelationshipPeriodEvaluation.Year year, RelationshipDimension dimension) {
    return copy(kind + "." + dimension.code() + "."
        + year.dimensions().get(dimension).tone().name().toLowerCase(Locale.ROOT));
  }

  private List<String> evidence(RelationshipPeriodEvaluation.Year year, RelationshipDimension... dimensions) {
    return Arrays.stream(dimensions).flatMap(dimension -> year.dimensions().get(dimension).evidence().stream())
        .map(RelationshipEvidence::key).distinct().sorted().toList();
  }

  private String copy(String key) {
    return RelationshipPlainCopy.get("single.current." + key);
  }
}
