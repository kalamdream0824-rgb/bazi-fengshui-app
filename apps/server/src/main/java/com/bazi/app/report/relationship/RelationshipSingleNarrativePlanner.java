package com.bazi.app.report.relationship;

import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.NarrativeTimelineValidator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class RelationshipSingleNarrativePlanner {
  public RelationshipSingleNarrativePlan plan(RelationshipPeriodEvaluation period) {
    return plan(period, null);
  }

  public RelationshipSingleNarrativePlan plan(
      RelationshipPeriodEvaluation period,
      RelationshipPeriodEvaluation.Year previous) {
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

    NarrativeTimeline timeline = previous == null ? null : timeline(previous, current, next);
    return new RelationshipSingleNarrativePlan(
        "single", 2, dimensionCopy("thesis", current, primary),
        copy("summary." + (current.mainRisk() == null ? "clear" : current.mainRisk().dimension().code())),
        current.year(), next.year(), sections, outlook(current, next), copy("reading_note"),
        period.years().stream().map(RelationshipPeriodEvaluation.Year::evaluation).toList(),
        period.years().stream().flatMap(year -> evidence(year, RelationshipDimension.values()).stream())
            .distinct().sorted().toList(),
        timeline);
  }

  private NarrativeTimeline timeline(
      RelationshipPeriodEvaluation.Year previous,
      RelationshipPeriodEvaluation.Year current,
      RelationshipPeriodEvaluation.Year next) {
    if (previous.year() != current.year() - 1) {
      throw new IllegalArgumentException("single previous year must precede the current year");
    }
    RelationshipDimension priority = current.mainRisk() == null
        ? current.focus().secondaryDimension()
        : current.mainRisk().dimension();
    NarrativeTimeline result = new NarrativeTimeline(
        new NarrativeTimeline.PastReview(
            previous.year(),
            previous.year() + "年认识机会与相处边界回看",
            List.of(
                "回看" + previous.year() + "年是否有接触新人的机会，联系后的回应能否持续",
                "如果" + previous.year() + "年认识过新的人，是否始终守住自己的相处边界"),
            "去年只用来核对认识新人的机会和后续回应，再看今年适合把注意力放在哪里。",
            pastEvidence(previous)),
        new NarrativeTimeline.PresentReading(
            current.year(),
            presentHeadline(current.focus().primaryDimension()),
            presentJudgment(current),
            presentPriority(priority),
            currentEvidence(current, priority)),
        List.of(new NarrativeTimeline.FutureStep(
            next.year(),
            futureHeadline(next.year(), next.focus().primaryDimension()),
            futureAction(next.focus().primaryDimension()),
            requiredEvidence(evidence(next, next.focus().primaryDimension())))));
    return new NarrativeTimelineValidator(List.of(
        "接触新人机会", "联系回应情况", "自己的相处边界",
        "值得认识的新机会", "下一年认识行动")).validate(result);
  }

  private String presentHeadline(RelationshipDimension dimension) {
    return switch (dimension) {
      case CONNECTION -> "今年先看有没有值得认识的新机会";
      case RESPONSE -> "今年先看新的联系能否得到认真回应";
      case DAILY_COOPERATION -> "今年先看线上联系能否变成实际见面";
      case BOUNDARIES -> "今年先看认识别人时能否守住个人边界";
      case STABILITY -> "今年先看新的联系能否稳定延续";
    };
  }

  private String presentJudgment(RelationshipPeriodEvaluation.Year current) {
    RelationshipDimension primary = current.focus().primaryDimension();
    RelationshipTone tone = current.dimensions().get(primary).tone();
    String subject = switch (primary) {
      case CONNECTION -> "认识新的人";
      case RESPONSE -> "和新认识的人继续交流";
      case DAILY_COOPERATION -> "从聊天走到实际见面";
      case BOUNDARIES -> "在相处中表达真实想法";
      case STABILITY -> "让一段新联系稳定下来";
    };
    return switch (tone) {
      case SUPPORTIVE -> subject + "相对顺利，可以主动一点，但仍要看对方实际怎么做。";
      case MIXED -> subject + "有机会，也会遇到犹豫或反复，不要只凭一时热情。";
      case PRESSURED -> subject + "不太顺利，先减少单方面投入，再看是否值得继续。";
      case QUIET -> subject + "不是今年最明显的变化，不必为了恋爱勉强自己。";
    };
  }

  private String presentPriority(RelationshipDimension dimension) {
    String issue = switch (dimension) {
      case CONNECTION -> "联系是否一直只有你主动";
      case RESPONSE -> "重要问题是否得到认真回应";
      case DAILY_COOPERATION -> "见面安排是否总由同一个人迁就";
      case BOUNDARIES -> "自己的不舒服和不同意见是否说清";
      case STABILITY -> "答应的小事是否能够持续做到";
    };
    return "现实中先确认" + issue + "。";
  }

  private String futureHeadline(int year, RelationshipDimension dimension) {
    String object = switch (dimension) {
      case CONNECTION -> "把认识机会变成一次真实接触";
      case RESPONSE -> "用一次明确回应判断是否继续";
      case DAILY_COOPERATION -> "把线上聊天变成一次实际见面";
      case BOUNDARIES -> "在开始了解时先说清个人边界";
      case STABILITY -> "先观察新的联系能否保持稳定";
    };
    return year + "年" + object;
  }

  private String futureAction(RelationshipDimension dimension) {
    return switch (dimension) {
      case CONNECTION -> "参加一次自己真正感兴趣的活动，或接受可信朋友的一次介绍。";
      case RESPONSE -> "出现新的联系时，提出一个具体问题，再看回应是否认真。";
      case DAILY_COOPERATION -> "聊得来时，提出一次时间明确的见面，再看双方能否配合。";
      case BOUNDARIES -> "开始了解一个人时，先说清一件自己不能接受的事。";
      case STABILITY -> "有了新的联系后，先观察约定能否连续做到，再决定是否投入更多。";
    };
  }

  private List<String> pastEvidence(RelationshipPeriodEvaluation.Year previous) {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    for (RelationshipDimension dimension : List.of(
        RelationshipDimension.CONNECTION,
        RelationshipDimension.RESPONSE,
        RelationshipDimension.BOUNDARIES)) {
      keys.addAll(evidence(previous, dimension));
    }
    if (keys.isEmpty()) {
      keys.addAll(previous.focus().primaryEvidenceKeys());
      keys.addAll(previous.focus().secondaryEvidenceKeys());
    }
    return requiredEvidence(List.copyOf(keys));
  }

  private List<String> currentEvidence(
      RelationshipPeriodEvaluation.Year current,
      RelationshipDimension priority) {
    LinkedHashSet<String> keys = new LinkedHashSet<>(current.focus().primaryEvidenceKeys());
    if (current.mainRisk() != null && current.mainRisk().dimension() == priority) {
      keys.addAll(current.mainRisk().evidenceKeys());
    } else {
      keys.addAll(current.focus().secondaryEvidenceKeys());
    }
    return requiredEvidence(List.copyOf(keys));
  }

  private List<String> requiredEvidence(List<String> keys) {
    List<String> result = keys.stream()
        .filter(Objects::nonNull)
        .filter(key -> !key.isBlank())
        .distinct()
        .toList();
    if (result.isEmpty()) {
      throw new IllegalArgumentException("single timeline section requires evidence");
    }
    return result;
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
