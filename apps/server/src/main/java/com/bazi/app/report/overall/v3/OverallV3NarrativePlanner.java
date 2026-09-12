package com.bazi.app.report.overall.v3;

import com.bazi.app.report.AnnualActionGuide;
import com.bazi.app.report.AnnualActionGuidePolicy;
import com.bazi.app.report.NarrativeTimeline;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds a compact report around one calculated cross-topic decision per year. */
public final class OverallV3NarrativePlanner {

  private final OverallActionGuideWriter actionWriter = new OverallActionGuideWriter();
  private final OverallV3CopyCatalog copy = new OverallV3CopyCatalog();

  public OverallV3NarrativePlan plan(
      List<OverallTopicSnapshot> productSnapshots,
      List<OverallAnnualDecision> productDecisions) {
    return plan(productSnapshots, productDecisions, null, null);
  }

  public OverallV3NarrativePlan plan(
      List<OverallTopicSnapshot> productSnapshots,
      List<OverallAnnualDecision> productDecisions,
      List<OverallTopicSnapshot> previousSnapshots,
      OverallAnnualDecision previousDecision) {
    Map<Integer, List<OverallTopicSnapshot>> snapshotsByYear = group(productSnapshots);
    validateProduct(snapshotsByYear, productDecisions);
    if ((previousSnapshots == null) != (previousDecision == null)) {
      throw new IllegalArgumentException("overall v3 previous snapshots and decision are paired");
    }

    List<OverallV3NarrativePlan.YearNarrative> years = new ArrayList<>();
    List<AnnualActionGuide> guides = new ArrayList<>();
    for (int index = 0; index < productDecisions.size(); index++) {
      OverallAnnualDecision decision = productDecisions.get(index);
      List<OverallTopicSnapshot> annualSnapshots = snapshotsByYear.get(decision.year());
      AnnualActionGuide guide = actionWriter.write(decision);
      guides.add(guide);
      years.add(new OverallV3NarrativePlan.YearNarrative(
          decision.year(),
          code(decision.primary().topic()),
          label(decision.primary().topic()),
          code(decision.secondary().topic()),
          label(decision.secondary().topic()),
          decision.decisionKey(),
          decision.conflictKey(),
          headline(productDecisions, index),
          linkage(decision),
          guide,
          observations(annualSnapshots, decision),
          transition(productDecisions, index),
          evidence(annualSnapshots)));
    }
    AnnualActionGuidePolicy.validate(guides);
    OverallV3NarrativePolicy.validate(guides);

    NarrativeTimeline timeline = previousDecision == null ? null : timeline(
        previousSnapshots, previousDecision, productDecisions, years);
    return new OverallV3NarrativePlan(
        years.size(),
        thesis(productDecisions),
        summary(productDecisions),
        years,
        "这份综合命书只回答每年先处理什么，以及怎样确认行动是否有效；四个主题不会平均用力。",
        years.stream().flatMap(year -> year.evidenceKeys().stream()).distinct().toList(),
        timeline);
  }

  private Map<Integer, List<OverallTopicSnapshot>> group(List<OverallTopicSnapshot> snapshots) {
    if (snapshots == null || snapshots.isEmpty()) {
      throw new IllegalArgumentException("overall v3 product snapshots are required");
    }
    Map<Integer, List<OverallTopicSnapshot>> result = new LinkedHashMap<>();
    snapshots.stream().sorted(java.util.Comparator
            .comparingInt(OverallTopicSnapshot::year)
            .thenComparingInt(item -> item.topic().ordinal()))
        .forEach(snapshot -> result.computeIfAbsent(snapshot.year(), ignored -> new ArrayList<>())
            .add(snapshot));
    result.replaceAll((year, values) -> List.copyOf(values));
    return Map.copyOf(result);
  }

  private void validateProduct(
      Map<Integer, List<OverallTopicSnapshot>> snapshotsByYear,
      List<OverallAnnualDecision> decisions) {
    if (decisions == null || decisions.size() < 2 || decisions.size() > 5) {
      throw new IllegalArgumentException("overall v3 requires two to five annual decisions");
    }
    if (snapshotsByYear.size() != decisions.size()) {
      throw new IllegalArgumentException("overall v3 decisions must match snapshot years");
    }
    for (int index = 0; index < decisions.size(); index++) {
      OverallAnnualDecision decision = decisions.get(index);
      if (index > 0 && decision.year() != decisions.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("overall v3 decisions must be consecutive");
      }
      List<OverallTopicSnapshot> annual = snapshotsByYear.get(decision.year());
      if (annual == null || annual.size() != OverallTopicSnapshot.Topic.values().length
          || !annual.stream().map(OverallTopicSnapshot::topic)
              .collect(java.util.stream.Collectors.toSet())
              .equals(EnumSet.allOf(OverallTopicSnapshot.Topic.class))) {
        throw new IllegalArgumentException("overall v3 requires four topic snapshots per year");
      }
      if (!annual.contains(decision.primary()) || !annual.contains(decision.secondary())) {
        throw new IllegalArgumentException("overall v3 decision must belong to its annual snapshots");
      }
    }
  }

  private List<OverallV3NarrativePlan.Observation> observations(
      List<OverallTopicSnapshot> annual,
      OverallAnnualDecision decision) {
    return annual.stream()
        .filter(snapshot -> snapshot.topic() != decision.primary().topic())
        .filter(snapshot -> snapshot.topic() != decision.secondary().topic())
        .map(snapshot -> new OverallV3NarrativePlan.Observation(
            code(snapshot.topic()),
            label(snapshot.topic()),
            stance(snapshot.stance()),
            observation(snapshot),
            snapshot.evidenceKeys()))
        .toList();
  }

  private String headline(List<OverallAnnualDecision> decisions, int index) {
    OverallAnnualDecision current = decisions.get(index);
    String focus = copy.focusObject(current.primary());
    if (index == 0) {
      return current.year() + "年先处理" + focus;
    }
    OverallAnnualDecision previous = decisions.get(index - 1);
    if (previous.primary().topic() != current.primary().topic()) {
      return current.year() + "年重点转到" + focus;
    }
    if (!previous.primary().focusKey().equals(current.primary().focusKey())) {
      return current.year() + "年同一主题改看" + focus;
    }
    long run = decisions.subList(0, index).stream()
        .map(OverallAnnualDecision::primary)
        .filter(snapshot -> snapshot.focusKey().equals(current.primary().focusKey()))
        .count();
    return current.year() + "年继续处理" + focus + "，这是连续第" + chinese(run + 1) + "年";
  }

  private String linkage(OverallAnnualDecision decision) {
    String primary = copy.focusObject(decision.primary());
    String secondary = copy.focusObject(decision.secondary());
    return switch (decision.conflictKey()) {
      case "capacity_before_career_expansion" ->
          "先稳住" + primary + "，否则" + secondary + "越推进越容易挤占休息。";
      case "cash_buffer_before_growth" ->
          "先算清" + primary + "，再判断为" + secondary + "增加投入是否承受得住。";
      case "relationship_stability_before_growth" ->
          "先让" + primary + "得到回应，避免它持续影响" + secondary + "。";
      default -> "先把" + primary + "处理清楚，因为它会直接影响" + secondary + "能否稳定。";
    };
  }

  private String observation(OverallTopicSnapshot snapshot) {
    String tail = switch (snapshot.stance()) {
      case SUPPORTIVE -> "有向前推进的条件，但只核对真实结果。";
      case BALANCED -> "变化不突出，维持现有边界即可。";
      case MIXED -> "有帮助也有牵制，每月核对一次实际变化。";
      case PRESSURED -> "容易增加负担，不要让它占用主行动所需资源。";
    };
    return "关于“" + copy.focusObject(snapshot) + "”，当前" + tail;
  }

  private String transition(List<OverallAnnualDecision> decisions, int index) {
    OverallAnnualDecision current = decisions.get(index);
    if (index == decisions.size() - 1) {
      return "这是本次判断的最后一年，之后要根据真实变化重新计算。";
    }
    OverallAnnualDecision next = decisions.get(index + 1);
    if (current.primary().topic() != next.primary().topic()) {
      return "下一年优先顺序会从" + label(current.primary().topic()) + "转到"
          + label(next.primary().topic()) + "。";
    }
    if (!current.primary().focusKey().equals(next.primary().focusKey())) {
      return "下一年仍以" + label(current.primary().topic()) + "为主，但具体要改看"
          + copy.focusObject(next.primary()) + "。";
    }
    if (current.primary().stance() != next.primary().stance()) {
      return "下一年仍处理同一件事，但状态会从" + stance(current.primary().stance())
          + "变为" + stance(next.primary().stance()) + "。";
    }
    return "下一年计算重点保持不变，继续按同一成功信号核对实际结果。";
  }

  private String thesis(List<OverallAnnualDecision> decisions) {
    List<OverallTopicSnapshot.Topic> path = decisions.stream()
        .map(decision -> decision.primary().topic())
        .toList();
    if (path.stream().distinct().count() == 1) {
      return "未来三年的首要顺序都落在" + label(path.get(0))
          + "，这是真实计算结果，不会为了文案变化强行更换主题。";
    }
    return "未来三年的优先顺序依次是"
        + path.stream().map(this::label).collect(java.util.stream.Collectors.joining("、"))
        + "；每年只先解决一个主问题。";
  }

  private String summary(List<OverallAnnualDecision> decisions) {
    OverallAnnualDecision first = decisions.get(0);
    return "第一步先处理" + copy.focusObject(first.primary()) + "，同时守住"
        + copy.focusObject(first.secondary()) + "；行动是否有效，以四周后的真实变化为准。";
  }

  private NarrativeTimeline timeline(
      List<OverallTopicSnapshot> previousSnapshots,
      OverallAnnualDecision previous,
      List<OverallAnnualDecision> product,
      List<OverallV3NarrativePlan.YearNarrative> years) {
    Map<Integer, List<OverallTopicSnapshot>> previousByYear = group(previousSnapshots);
    if (previousByYear.size() != 1
        || previous.year() != product.get(0).year() - 1
        || !previousByYear.get(previous.year()).contains(previous.primary())
        || !previousByYear.get(previous.year()).contains(previous.secondary())) {
      throw new IllegalArgumentException("overall v3 previous decision must immediately precede product");
    }
    String pastPrimary = copy.focusObject(previous.primary());
    String pastSecondary = copy.focusObject(previous.secondary());
    OverallV3NarrativePlan.YearNarrative present = years.get(0);
    List<NarrativeTimeline.FutureStep> future = new ArrayList<>();
    for (int index = 1; index < years.size(); index++) {
      OverallV3NarrativePlan.YearNarrative year = years.get(index);
      future.add(new NarrativeTimeline.FutureStep(
          year.year(),
          "到" + year.year() + "年，优先顺序落在" + year.primaryLabel() + "。",
          "届时先核对" + copy.focusObject(product.get(index).primary())
              + "的真实变化，再决定是否扩大行动。",
          product.get(index).evidenceKeys()));
    }
    return new NarrativeTimeline(
        new NarrativeTimeline.PastReview(
            previous.year(),
            previous.year() + "年回看：" + pastPrimary,
            List.of(
                "回看" + previous.year() + "年，" + pastPrimary + "是否出现过明显变化。",
                "如果" + pastPrimary + "发生变化，核对" + pastSecondary + "是否同时受到影响。"),
            "去年的记录只用于确认两件事是否一起变化，再决定今年的处理顺序。",
            previous.evidenceKeys()),
        new NarrativeTimeline.PresentReading(
            present.year(),
            "当下先处理" + present.primaryLabel() + "。",
            "今年主问题是" + copy.focusObject(product.get(0).primary()) + "，并会牵动"
                + copy.focusObject(product.get(0).secondary()) + "。",
            "本月先做一件四周内能核对结果的事。",
            product.get(0).evidenceKeys()),
        future);
  }

  private List<String> evidence(List<OverallTopicSnapshot> snapshots) {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    snapshots.forEach(snapshot -> result.addAll(snapshot.evidenceKeys()));
    return List.copyOf(result);
  }

  private String code(OverallTopicSnapshot.Topic topic) {
    return topic.name().toLowerCase(Locale.ROOT);
  }

  private String label(OverallTopicSnapshot.Topic topic) {
    return switch (topic) {
      case RHYTHM -> "生活节奏";
      case CAREER -> "事业进展";
      case WEALTH -> "钱财安排";
      case RELATIONSHIP -> "关系沟通";
    };
  }

  private String stance(OverallTopicSnapshot.Stance stance) {
    return switch (stance) {
      case SUPPORTIVE -> "可以推进";
      case BALANCED -> "相对平稳";
      case MIXED -> "机会与牵制并存";
      case PRESSURED -> "压力偏重";
    };
  }

  private String chinese(long value) {
    return switch ((int) value) {
      case 2 -> "二";
      case 3 -> "三";
      case 4 -> "四";
      case 5 -> "五";
      default -> Long.toString(value);
    };
  }
}
