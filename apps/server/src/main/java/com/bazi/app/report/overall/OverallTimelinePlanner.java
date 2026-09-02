package com.bazi.app.report.overall;

import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.NarrativeTimelinePlanner;
import com.bazi.app.report.NarrativeTimelineValidator;
import com.bazi.app.report.ReportTopic;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class OverallTimelinePlanner
    implements NarrativeTimelinePlanner<OverallYearEvaluation, Void> {

  @Override
  public NarrativeTimeline plan(Input<OverallYearEvaluation, Void> input) {
    Objects.requireNonNull(input, "input");
    if (!ReportTopic.OVERALL.code().equals(input.topicCode())) {
      throw new IllegalArgumentException("overall timeline requires the overall topic");
    }
    if (input.productEvaluations().size() != 3) {
      throw new IllegalArgumentException("overall timeline requires the three-year product");
    }
    OverallYearEvaluation previous = input.previousEvaluation();
    List<OverallYearEvaluation> product = input.productEvaluations();
    OverallYearEvaluation present = product.get(0);

    List<NarrativeTimeline.FutureStep> future = new ArrayList<>();
    OverallDimension previousFocus = null;
    for (int index = 1; index < product.size(); index++) {
      OverallYearEvaluation year = product.get(index);
      OverallDimension focus = year.primaryDimension();
      if (focus == previousFocus) focus = year.secondaryDimension();
      future.add(new NarrativeTimeline.FutureStep(
          year.year(),
          futureHeadline(year.year(), focus, index - 1),
          futureAction(focus, index - 1),
          evidenceKeys(year.dimension(focus))));
      previousFocus = focus;
    }

    NarrativeTimeline timeline = new NarrativeTimeline(
        new NarrativeTimeline.PastReview(
            previous.year(),
            previous.year() + "年" + object(previous.primaryDimension()) + "与"
                + object(previous.secondaryDimension()) + "回看",
            List.of(
                primaryCheckpoint(previous.year(), previous.primaryDimension()),
                linkageCheckpoint(
                    previous.year(),
                    previous.primaryDimension(),
                    previous.secondaryDimension())),
            "去年只用来核对" + object(previous.primaryDimension()) + "怎样影响"
                + object(previous.secondaryDimension()) + "，再看今年先处理哪件事。",
            combinedEvidence(previous, previous.primaryDimension(), previous.secondaryDimension())),
        new NarrativeTimeline.PresentReading(
            present.year(),
            presentHeadline(present.primaryDimension()),
            presentJudgment(present.primary()),
            presentPriority(present.primaryDimension()),
            evidenceKeys(present.primary())),
        future);
    return new NarrativeTimelineValidator(List.of(
        "工作责任变化", "实际收支变化", "关系沟通变化", "生活节奏变化",
        "时间精力分配", "工作责任结果", "收支实际余量", "沟通实际变化"))
        .validate(timeline);
  }

  private String primaryCheckpoint(int year, OverallDimension dimension) {
    return switch (dimension) {
      case RHYTHM -> "回看" + year + "年休息和日程是否经常被临时事情打断";
      case CAREER -> "回看" + year + "年工作责任是否增加，完成的事情有没有得到明确反馈";
      case WEALTH -> "回看" + year + "年收入和支出是否同时变化，钱最后有没有留下";
      case RELATIONSHIP -> "回看" + year + "年重要关系里的沟通是否及时，实际配合有没有持续";
    };
  }

  private String linkageCheckpoint(
      int year,
      OverallDimension primary,
      OverallDimension secondary) {
    String condition = switch (primary) {
      case RHYTHM -> "日程经常变化";
      case CAREER -> "工作责任增加";
      case WEALTH -> "收入或必要支出变化";
      case RELATIONSHIP -> "重要关系需要更多时间";
    };
    String linked = switch (secondary) {
      case RHYTHM -> "休息和生活节奏是否受到影响";
      case CAREER -> "工作完成进度是否受到影响";
      case WEALTH -> "实际收支和余钱是否受到影响";
      case RELATIONSHIP -> "沟通和相处是否受到影响";
    };
    return "如果" + year + "年" + condition + "，" + linked;
  }

  private String presentHeadline(OverallDimension dimension) {
    return "今年先处理" + switch (dimension) {
      case RHYTHM -> "时间和精力怎么分配";
      case CAREER -> "工作责任和完成结果";
      case WEALTH -> "收入、支出和余钱";
      case RELATIONSHIP -> "沟通和实际配合";
    };
  }

  private String presentJudgment(OverallDimensionEvaluation evaluation) {
    return switch (evaluation.dimension()) {
      case RHYTHM -> switch (evaluation.stance()) {
        case SUPPORTIVE -> "今年还有精力推进事情，但一次只保留一个重点。";
        case BALANCED -> "今年日程变化不大，按照轻重安排即可。";
        case PRESSURED -> "今年事情容易挤占休息，需要主动减少安排。";
        case MIXED -> "今年事情能够推进，但日程必须为临时变化留出空间。";
      };
      case CAREER -> switch (evaluation.stance()) {
        case SUPPORTIVE -> "今年工作更容易向前推进，但要形成看得见的结果。";
        case BALANCED -> "今年工作变化不大，先把已经承担的事情做好。";
        case PRESSURED -> "今年工作责任容易增加，先问清期限和所需支持。";
        case MIXED -> "今年既有推进机会，也会增加责任，不要还没问清条件就答应。";
      };
      case WEALTH -> switch (evaluation.stance()) {
        case SUPPORTIVE -> "今年收支安排相对顺利，仍要以实际到账和余钱为准。";
        case BALANCED -> "今年收支变化不大，先守住日常需要的钱。";
        case PRESSURED -> "今年必要支出容易增加，不要提前花还没到账的钱。";
        case MIXED -> "今年收入和支出都可能增加，最后要看实际留下多少。";
      };
      case RELATIONSHIP -> switch (evaluation.stance()) {
        case SUPPORTIVE -> "今年沟通更容易得到回应，可以把共同安排说具体。";
        case BALANCED -> "今年关系变化不大，稳定相处比口头承诺更重要。";
        case PRESSURED -> "今年同一分歧容易反复，需要尽早把具体事情说清。";
        case MIXED -> "今年既有支持也有分歧，要看沟通以后实际有没有变化。";
      };
    };
  }

  private String presentPriority(OverallDimension dimension) {
    return switch (dimension) {
      case RHYTHM -> "先给睡眠和休息留出固定时间，再接受新的安排。";
      case CAREER -> "先说清最重要工作的负责人、期限和完成标准。";
      case WEALTH -> "先保留必要开支，再决定尚未到账的钱是否可以动用。";
      case RELATIONSHIP -> "先把反复出现的一件事说清，再讨论下一步。";
    };
  }

  private String futureHeadline(int year, OverallDimension dimension, int index) {
    String lead = index == 0 ? "先落实" : "再核对";
    return year + "年" + lead + switch (dimension) {
      case RHYTHM -> "休息与日程";
      case CAREER -> "工作责任与结果";
      case WEALTH -> "收支与余钱";
      case RELATIONSHIP -> "关系沟通与相处";
    };
  }

  private String futureAction(OverallDimension dimension, int index) {
    if (index == 0) {
      return switch (dimension) {
        case RHYTHM -> "给每周日程留出一段不接受临时事项的时间。";
        case CAREER -> "选一项最重要的工作，写清谁负责、何时完成。";
        case WEALTH -> "按照必要程度排列支出，再决定可以动用多少钱。";
        case RELATIONSHIP -> "选一件反复争论的事，约好时间把它说清。";
      };
    }
    return switch (dimension) {
      case RHYTHM -> "检查连续一个月的睡眠和精力，删掉长期占时间却没有结果的事。";
      case CAREER -> "核对已经完成的工作结果，用它争取清楚的权限或回报。";
      case WEALTH -> "核对每月实际到账和支出，把剩下的钱单独留下。";
      case RELATIONSHIP -> "查看沟通后的实际变化，没有改变时重新确认彼此边界。";
    };
  }

  private String object(OverallDimension dimension) {
    return switch (dimension) {
      case RHYTHM -> "生活节奏";
      case CAREER -> "工作";
      case WEALTH -> "钱财";
      case RELATIONSHIP -> "关系";
    };
  }

  private List<String> combinedEvidence(
      OverallYearEvaluation year,
      OverallDimension first,
      OverallDimension second) {
    LinkedHashSet<String> keys = new LinkedHashSet<>(evidenceKeys(year.dimension(first)));
    keys.addAll(evidenceKeys(year.dimension(second)));
    return List.copyOf(keys);
  }

  private List<String> evidenceKeys(OverallDimensionEvaluation evaluation) {
    List<String> keys = evaluation.allEvidenceKeys().stream()
        .filter(Objects::nonNull)
        .filter(key -> !key.isBlank())
        .distinct()
        .toList();
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("overall timeline section requires evidence");
    }
    return keys;
  }
}
