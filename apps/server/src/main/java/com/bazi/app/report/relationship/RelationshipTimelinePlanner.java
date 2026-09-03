package com.bazi.app.report.relationship;

import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.NarrativeTimelinePlanner;
import com.bazi.app.report.NarrativeTimelineValidator;
import com.bazi.app.report.ReportTopic;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class RelationshipTimelinePlanner
    implements NarrativeTimelinePlanner<RelationshipPeriodEvaluation.Year, RelationshipStatus> {

  @Override
  public NarrativeTimeline plan(Input<RelationshipPeriodEvaluation.Year, RelationshipStatus> input) {
    Objects.requireNonNull(input, "input");
    if (!ReportTopic.RELATIONSHIP.code().equals(input.topicCode())) {
      throw new IllegalArgumentException("relationship timeline requires the relationship topic");
    }
    RelationshipStatus status = input.userState()
        .orElseThrow(() -> new IllegalArgumentException("relationship timeline requires status"));
    if (status == RelationshipStatus.SINGLE) {
      throw new IllegalArgumentException("single relationship uses its own timeline planner");
    }
    if (input.productEvaluations().size() != 3) {
      throw new IllegalArgumentException("partnered relationship timeline requires three years");
    }

    RelationshipPeriodEvaluation.Year previous = input.previousEvaluation();
    List<RelationshipPeriodEvaluation.Year> product = input.productEvaluations();
    RelationshipPeriodEvaluation.Year present = product.get(0);
    List<NarrativeTimeline.FutureStep> future = new ArrayList<>();
    ActionCopy previousAction = null;
    for (int index = 1; index < product.size(); index++) {
      RelationshipPeriodEvaluation.Year year = product.get(index);
      RelationshipDimension actionDimension = year.focus().primaryDimension();
      ActionCopy action = action(status, actionDimension, index - 1);
      if (previousAction != null
          && (previousAction.verb().equals(action.verb())
              || previousAction.object().equals(action.object()))) {
        actionDimension = year.focus().secondaryDimension();
        action = action(status, actionDimension, index - 1);
      }
      if (previousAction != null
          && (previousAction.verb().equals(action.verb())
              || previousAction.object().equals(action.object()))) {
        throw new IllegalArgumentException("relationship future actions must use distinct verbs and objects");
      }
      future.add(new NarrativeTimeline.FutureStep(
          year.year(),
          futureHeadline(year.year(), status, actionDimension, index - 1),
          action.text(),
          evidenceKeys(year, actionDimension)));
      previousAction = action;
    }

    RelationshipDimension priorityDimension = present.mainRisk() == null
        ? present.focus().secondaryDimension()
        : present.mainRisk().dimension();
    NarrativeTimeline timeline = new NarrativeTimeline(
        new NarrativeTimeline.PastReview(
            previous.year(),
            pastHeadline(previous.year(), status),
            List.of(
                pastCheckpoint(previous.year(), status, previous.focus().primaryDimension()),
                pastCheckpoint(previous.year(), status, previous.focus().secondaryDimension())),
            pastBridge(status),
            focusEvidenceKeys(previous)),
        new NarrativeTimeline.PresentReading(
            present.year(),
            presentHeadline(status, present.focus().primaryDimension()),
            presentJudgment(status, present),
            presentPriority(status, priorityDimension),
            presentEvidenceKeys(present, priorityDimension)),
        future);
    return new NarrativeTimelineValidator(corePhrases(status)).validate(timeline);
  }

  private String pastHeadline(int year, RelationshipStatus status) {
    return year + "年" + switch (status) {
      case DATING -> "约会回应与关系确认回看";
      case MARRIED -> "共同生活与责任分配回看";
      case SINGLE -> throw new IllegalArgumentException("single relationship uses its own timeline planner");
    };
  }

  private String pastCheckpoint(
      int year,
      RelationshipStatus status,
      RelationshipDimension dimension) {
    if (status == RelationshipStatus.DATING) {
      return switch (dimension) {
        case CONNECTION -> "回看" + year + "年约会之后的联系是否自然延续";
        case RESPONSE -> "回看" + year + "年约会或聊天后，对方的回应是否稳定";
        case DAILY_COOPERATION -> "如果" + year + "年见面次数增加，双方时间是否能够配合";
        case BOUNDARIES -> "回看" + year + "年是否说清彼此不能接受的相处方式";
        case STABILITY -> "如果" + year + "年谈过彼此期待，双方是否把关系确认清楚";
      };
    }
    return switch (dimension) {
      case CONNECTION -> "回看" + year + "年共同生活中，两个人是否仍会留出相处时间";
      case RESPONSE -> "回看" + year + "年共同生活中遇到事情时，双方是否会及时回应对方";
      case DAILY_COOPERATION -> "如果" + year + "年家务或日常安排增加，双方是否能够配合";
      case BOUNDARIES -> "回看" + year + "年双方与家人的边界是否说清";
      case STABILITY -> "如果" + year + "年有重要家庭安排，责任分配是否事先说清";
    };
  }

  private String pastBridge(RelationshipStatus status) {
    return switch (status) {
      case DATING -> "去年只用来核对互动和关系期待，再看今年先处理什么。";
      case MARRIED -> "去年只用来核对日常配合和生活责任，再看今年先处理什么。";
      case SINGLE -> throw new IllegalArgumentException("single relationship uses its own timeline planner");
    };
  }

  private String presentHeadline(RelationshipStatus status, RelationshipDimension dimension) {
    return "今年先看" + topic(status, dimension) + "是否顺畅";
  }

  private String presentJudgment(
      RelationshipStatus status,
      RelationshipPeriodEvaluation.Year year) {
    RelationshipDimension dimension = year.focus().primaryDimension();
    RelationshipTone tone = year.dimensions().get(dimension).tone();
    String subject = topic(status, dimension);
    return switch (tone) {
      case SUPPORTIVE -> subject + "更容易顺一些，但仍要看双方是否持续投入。";
      case MIXED -> subject + "有顺利的时候，也会遇到分歧，不能只看一时感受。";
      case PRESSURED -> subject + "容易遇到不一致，先把具体问题说清。";
      case QUIET -> subject + "没有明显变化，继续看双方实际怎么做。";
    };
  }

  private String presentPriority(
      RelationshipStatus status,
      RelationshipDimension dimension) {
    String issue = switch (status) {
      case DATING -> switch (dimension) {
        case CONNECTION -> "见面和联系能否持续";
        case RESPONSE -> "重要事情能否得到明确回应";
        case DAILY_COOPERATION -> "双方时间能否真正排得开";
        case BOUNDARIES -> "彼此边界以及不能接受的相处方式是否说清";
        case STABILITY -> "对关系下一步是否有一致期待";
      };
      case MARRIED -> switch (dimension) {
        case CONNECTION -> "两个人是否还留有相处时间";
        case RESPONSE -> "重要事情能否商量后再决定";
        case DAILY_COOPERATION -> "家务和日常安排是否长期偏向一方";
        case BOUNDARIES -> "双方与家人的边界是否说清";
        case STABILITY -> "长期支出和生活安排是否有共同计划";
      };
      case SINGLE -> throw new IllegalArgumentException("single relationship uses its own timeline planner");
    };
    return "现实中先确认" + issue + "。";
  }

  private String futureHeadline(
      int year,
      RelationshipStatus status,
      RelationshipDimension dimension,
      int index) {
    String direction = index == 0 ? "先落实" : "再调整";
    return year + "年" + direction + futureObject(status, dimension, index);
  }

  private String futureObject(
      RelationshipStatus status,
      RelationshipDimension dimension,
      int index) {
    if (status == RelationshipStatus.DATING) {
      return switch (dimension) {
        case CONNECTION -> index == 0 ? "固定相处时间" : "联系节奏";
        case RESPONSE -> index == 0 ? "重要消息的回应方式" : "分歧后的沟通办法";
        case DAILY_COOPERATION -> index == 0 ? "双方可以见面的日期" : "临时改约的处理办法";
        case BOUNDARIES -> index == 0 ? "不能接受的相处方式" : "个人时间界限";
        case STABILITY -> index == 0 ? "对关系下一步的期待" : "近期关系安排";
      };
    }
    return switch (dimension) {
      case CONNECTION -> index == 0 ? "两个人的相处时间" : "每周交流安排";
      case RESPONSE -> index == 0 ? "重要事情的商量顺序" : "没有说清的决定";
      case DAILY_COOPERATION -> index == 0 ? "家务和固定安排" : "长期由一人承担的事务";
      case BOUNDARIES -> index == 0 ? "双方家人介入的范围" : "个人休息空间";
      case STABILITY -> index == 0 ? "未来一年的共同计划" : "长期生活安排";
    };
  }

  private ActionCopy action(
      RelationshipStatus status,
      RelationshipDimension dimension,
      int index) {
    String object = futureObject(status, dimension, index);
    String verb = index == 0 ? switch (dimension) {
      case CONNECTION -> "安排";
      case RESPONSE -> "约定";
      case DAILY_COOPERATION -> "列出";
      case BOUNDARIES -> "说明";
      case STABILITY -> "记录";
    } : switch (dimension) {
      case CONNECTION -> "确认";
      case RESPONSE -> "回顾";
      case DAILY_COOPERATION -> "调整";
      case BOUNDARIES -> "划定";
      case STABILITY -> "商量";
    };
    String text = switch (status) {
      case DATING -> verb + object + (index == 0
          ? "，观察双方能否按约定做到。"
          : "，根据实际相处结果决定下一步。");
      case MARRIED -> verb + object + (index == 0
          ? "，接下来一个月按这个安排执行。"
          : "，执行一段时间后再一起检查。");
      case SINGLE -> throw new IllegalArgumentException("single relationship uses its own timeline planner");
    };
    return new ActionCopy(verb, object, text);
  }

  private String topic(RelationshipStatus status, RelationshipDimension dimension) {
    if (status == RelationshipStatus.DATING) {
      return switch (dimension) {
        case CONNECTION -> "相处联系";
        case RESPONSE -> "回应和表达";
        case DAILY_COOPERATION -> "约会安排";
        case BOUNDARIES -> "彼此边界";
        case STABILITY -> "关系稳定";
      };
    }
    return switch (dimension) {
      case CONNECTION -> "两个人的联系";
      case RESPONSE -> "商量和回应";
      case DAILY_COOPERATION -> "共同生活配合";
      case BOUNDARIES -> "彼此边界";
      case STABILITY -> "长期生活稳定";
    };
  }

  private List<String> focusEvidenceKeys(RelationshipPeriodEvaluation.Year year) {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    keys.addAll(year.focus().primaryEvidenceKeys());
    keys.addAll(year.focus().secondaryEvidenceKeys());
    return requiredEvidence(keys);
  }

  private List<String> presentEvidenceKeys(
      RelationshipPeriodEvaluation.Year year,
      RelationshipDimension priorityDimension) {
    LinkedHashSet<String> keys = new LinkedHashSet<>(year.focus().primaryEvidenceKeys());
    if (year.mainRisk() != null && year.mainRisk().dimension() == priorityDimension) {
      keys.addAll(year.mainRisk().evidenceKeys());
    } else {
      keys.addAll(year.focus().secondaryEvidenceKeys());
    }
    return requiredEvidence(keys);
  }

  private List<String> evidenceKeys(
      RelationshipPeriodEvaluation.Year year,
      RelationshipDimension dimension) {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    year.dimensions().get(dimension).evidence().stream()
        .map(RelationshipEvidence::key)
        .forEach(keys::add);
    return requiredEvidence(keys);
  }

  private List<String> requiredEvidence(LinkedHashSet<String> keys) {
    keys.removeIf(key -> key == null || key.isBlank());
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("relationship timeline section requires evidence");
    }
    return List.copyOf(keys);
  }

  private List<String> corePhrases(RelationshipStatus status) {
    return switch (status) {
      case DATING -> List.of(
          "约会回应情况", "关系确认方式", "固定相处时间", "分歧沟通结果", "个人时间界限");
      case MARRIED -> List.of(
          "共同生活配合", "家庭责任分配", "两人相处时间", "每周交流安排", "个人休息空间");
      case SINGLE -> throw new IllegalArgumentException("single relationship uses its own timeline planner");
    };
  }

  private record ActionCopy(String verb, String object, String text) {}
}
