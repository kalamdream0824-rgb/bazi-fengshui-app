package com.bazi.app.report;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class CareerTimelinePlanner
    implements NarrativeTimelinePlanner<YearAssessment, CareerContext> {

  @Override
  public NarrativeTimeline plan(Input<YearAssessment, CareerContext> input) {
    Objects.requireNonNull(input, "input");
    if (!ReportTopic.CAREER.code().equals(input.topicCode())) {
      throw new IllegalArgumentException("career timeline requires the career topic");
    }
    CareerContext context = input.userState()
        .orElseThrow(() -> new IllegalArgumentException("career timeline requires user state"));
    if (input.productEvaluations().size() != 2) {
      throw new IllegalArgumentException("career timeline requires current and next year evaluations");
    }

    YearAssessment previous = input.previousEvaluation();
    YearAssessment present = input.productEvaluations().get(0);
    YearAssessment future = input.productEvaluations().get(1);
    NarrativeTimeline timeline = new NarrativeTimeline(
        past(previous, context.status()),
        present(present, context),
        List.of(future(future, context)));
    return new NarrativeTimelineValidator(corePhrases(context.status())).validate(timeline);
  }

  private NarrativeTimeline.PastReview past(
      YearAssessment previous,
      CareerContext.Status status) {
    return new NarrativeTimeline.PastReview(
        previous.year(),
        previous.year() + "年" + pastObject(status) + "回看",
        pastCheckpoints(previous.year(), status),
        pastBridge(status),
        evidenceKeys(previous));
  }

  private NarrativeTimeline.PresentReading present(
      YearAssessment present,
      CareerContext context) {
    return new NarrativeTimeline.PresentReading(
        present.year(),
        presentHeadline(context.status()),
        presentJudgment(present.stage(), context.status()),
        presentPriority(context),
        evidenceKeys(present));
  }

  private NarrativeTimeline.FutureStep future(
      YearAssessment future,
      CareerContext context) {
    return new NarrativeTimeline.FutureStep(
        future.year(),
        futureHeadline(future.year(), future.stage(), context.status()),
        futureAction(context.status(), context.goal()),
        evidenceKeys(future));
  }

  private List<String> pastCheckpoints(int year, CareerContext.Status status) {
    return switch (status) {
      case EMPLOYED -> List.of(
          "回看" + year + "年职责是否增加，做成的成果有没有被明确看见",
          "如果" + year + "年接了更多工作，工资或职位是否同步变化");
      case SELF_EMPLOYED -> List.of(
          "回看" + year + "年客户是否更稳定，主要项目有没有持续下来",
          "如果" + year + "年订单增加，回款是否按原计划收到");
      case JOB_SEEKING -> List.of(
          "回看" + year + "年投递是否带来稳定的面试机会",
          "如果" + year + "年参加过面试，回应是否总停在相同环节");
      case STUDYING -> List.of(
          "回看" + year + "年学习计划是否持续完成",
          "如果" + year + "年投入了更多时间，作品是否真正做完并得到反馈");
    };
  }

  private String pastObject(CareerContext.Status status) {
    return switch (status) {
      case EMPLOYED -> "职责与成果";
      case SELF_EMPLOYED -> "客户与回款";
      case JOB_SEEKING -> "投递与面试";
      case STUDYING -> "学习与作品";
    };
  }

  private String pastBridge(CareerContext.Status status) {
    return switch (status) {
      case EMPLOYED -> "去年只用来核对职责和成果，再看今年该先做好哪件事。";
      case SELF_EMPLOYED -> "去年只用来核对客户和收款，再看今年该先稳住什么。";
      case JOB_SEEKING -> "去年只用来核对求职回应，再看今年该先改哪一步。";
      case STUDYING -> "去年只用来核对学习结果，再看今年该先完成什么。";
    };
  }

  private String presentHeadline(CareerContext.Status status) {
    return switch (status) {
      case EMPLOYED -> "今年先看工作成果能否带来实际变化";
      case SELF_EMPLOYED -> "今年先看客户和回款能否稳定";
      case JOB_SEEKING -> "今年先看投递能否带来有效面试";
      case STUDYING -> "今年先看学习能否变成完整作品";
    };
  }

  private String presentJudgment(AnnualStage stage, CareerContext.Status status) {
    return switch (status) {
      case EMPLOYED -> switch (stage) {
        case ADVANCE -> "今年可以主动争取职位变化，但先用做成的事说话。";
        case PREPARE -> "今年更适合补齐工作能力，暂时不用急着换职位。";
        case CAUTION -> "今年额外工作容易变多，先守住质量和休息。";
        case TRANSITION -> "今年分工可能调整，先确认具体职责再答应。";
        case STABLE -> "今年变化不大，先完成最重要的工作。";
      };
      case SELF_EMPLOYED -> switch (stage) {
        case ADVANCE -> "今年可以争取更多生意，但只扩大已经做顺的项目。";
        case PREPARE -> "今年先理顺价格、流程和收款，不急着增加客户。";
        case CAUTION -> "今年先少接新项目，保证手里的钱和老客户稳定。";
        case TRANSITION -> "今年生意做法需要调整，先小范围验证再投入。";
        case STABLE -> "今年先服务好现有客户，不同时增加太多项目。";
      };
      case JOB_SEEKING -> switch (stage) {
        case ADVANCE -> "今年可以增加投递，但只选真正适合的工作。";
        case PREPARE -> "今年先补好简历、作品和面试准备，再增加投递。";
        case CAUTION -> "今年不要只靠海投，先缩小岗位范围。";
        case TRANSITION -> "今年求职方向可能改变，先确定两类目标工作。";
        case STABLE -> "今年求职机会较平稳，投得准比投得多更重要。";
      };
      case STUDYING -> switch (stage) {
        case ADVANCE -> "今年可以开始用作品寻找实习或入行机会。";
        case PREPARE -> "今年先学好一项关键能力，再做出能展示的作品。";
        case CAUTION -> "今年不要同时学太多方向，先完成一件作品。";
        case TRANSITION -> "今年学习方向可能调整，先用短项目试一次。";
        case STABLE -> "今年保持学习，同时把学到的内容变成作品。";
      };
    };
  }

  private String presentPriority(CareerContext context) {
    return paceLead(context) + goalPriority(context.status(), context.goal());
  }

  private String paceLead(CareerContext context) {
    return switch (context.pace()) {
      case SMOOTH -> switch (context.status()) {
        case EMPLOYED -> "工作进展顺利时，";
        case SELF_EMPLOYED -> "生意进展顺利时，";
        case JOB_SEEKING -> "求职进展顺利时，";
        case STUDYING -> "学习进展顺利时，";
      };
      case STALLED -> switch (context.status()) {
        case EMPLOYED -> "工作推进不顺时，";
        case SELF_EMPLOYED -> "生意进展不顺时，";
        case JOB_SEEKING -> "求职进展不顺时，";
        case STUDYING -> "学习进展不顺时，";
      };
      case HIGH_PRESSURE -> switch (context.status()) {
        case EMPLOYED -> "工作压力较大时，";
        case SELF_EMPLOYED -> "经营压力较大时，";
        case JOB_SEEKING -> "求职压力较大时，";
        case STUDYING -> "学习压力较大时，";
      };
      case PREPARING_CHANGE -> switch (context.goal()) {
        case PROMOTION -> "准备升职时，";
        case JOB_CHANGE -> "准备换工作时，";
        case STABILITY -> "准备调整工作状态时，";
        case TRANSITION -> "准备转换方向时，";
      };
    };
  }

  private String goalPriority(CareerContext.Status status, CareerContext.Goal goal) {
    return switch (status) {
      case EMPLOYED -> switch (goal) {
        case PROMOTION -> "先整理能证明成绩的材料，再找机会谈升职。";
        case JOB_CHANGE -> "先写清下份工作的必要条件，再安排面试。";
        case STABILITY -> "先完成关键工作，再减少长期消耗精力的事。";
        case TRANSITION -> "先做一个小项目，再决定是否真的转行。";
      };
      case SELF_EMPLOYED -> switch (goal) {
        case PROMOTION -> "先稳住已经赚钱的项目，再考虑扩大生意。";
        case JOB_CHANGE -> "先确定想回到哪类工作，再准备简历。";
        case STABILITY -> "先保证老客户和回款正常，再把生意稳定下来。";
        case TRANSITION -> "先小范围试新项目，再决定是否增加投入。";
      };
      case JOB_SEEKING -> switch (goal) {
        case PROMOTION -> "先争取进入合适岗位，再考虑更高的职位。";
        case JOB_CHANGE -> "先检查目标岗位、简历和面试表现，再集中寻找下一份工作。";
        case STABILITY -> "先排除条件说不清的机会，再集中寻找稳定工作。";
        case TRANSITION -> "先用作品证明新能力，再寻找相应工作。";
      };
      case STUDYING -> switch (goal) {
        case PROMOTION -> "先补好入行需要的能力，再争取更好的机会。";
        case JOB_CHANGE -> "先确定目标行业和岗位，再准备求职材料。";
        case STABILITY -> "先学好最重要的能力，再完成一件完整作品。";
        case TRANSITION -> "先完成一个短项目，再决定是否继续投入新方向。";
      };
    };
  }

  private String futureHeadline(
      int year,
      AnnualStage stage,
      CareerContext.Status status) {
    String object = switch (status) {
      case EMPLOYED -> "工作";
      case SELF_EMPLOYED -> "生意";
      case JOB_SEEKING -> "求职";
      case STUDYING -> "学习与入行";
    };
    String direction = switch (stage) {
      case ADVANCE -> "可以主动争取下一步";
      case PREPARE -> "要先准备，再寻找机会";
      case CAUTION -> "要收紧安排，避免分散精力";
      case TRANSITION -> "要先试清变化，再作决定";
      case STABLE -> "要保持节奏，完成既定目标";
    };
    return year + "年" + object + direction;
  }

  private String futureAction(CareerContext.Status status, CareerContext.Goal goal) {
    return switch (status) {
      case EMPLOYED -> switch (goal) {
        case PROMOTION -> "把今年积累的成绩整理成材料，再正式争取升职或加薪。";
        case JOB_CHANGE -> "用今年明确的条件筛选岗位，再安排实际面试。";
        case STABILITY -> "保留最重要的工作，减少长期消耗却没有结果的任务。";
        case TRANSITION -> "用已完成的小项目验证新方向，再决定是否转行。";
      };
      case SELF_EMPLOYED -> switch (goal) {
        case PROMOTION -> "只扩大已经稳定赚钱的项目，不同时增加新方向。";
        case JOB_CHANGE -> "用已经理清的岗位方向准备简历，开始实际求职。";
        case STABILITY -> "留下按时付钱的客户，减少回款慢、反复修改的项目。";
        case TRANSITION -> "根据小项目的实际收入和反馈，决定是否扩大新生意。";
      };
      case JOB_SEEKING -> switch (goal) {
        case PROMOTION -> "进入合适岗位后，先做出能被看见的工作结果。";
        case JOB_CHANGE -> "根据今年的面试反馈缩小岗位范围，集中争取合适机会。";
        case STABILITY -> "优先选择工作内容、收入和作息都说得清的机会。";
        case TRANSITION -> "用作品和短项目的反馈，集中寻找真正需要新能力的岗位。";
      };
      case STUDYING -> switch (goal) {
        case PROMOTION -> "用完整作品争取实习或入行机会，不再只停在学习阶段。";
        case JOB_CHANGE -> "根据目标岗位补齐作品和简历，开始参加实际面试。";
        case STABILITY -> "围绕同一项能力继续做作品，用外部反馈修正学习方法。";
        case TRANSITION -> "用完成的短项目去找一次真实机会，再决定是否继续新方向。";
      };
    };
  }

  private List<String> evidenceKeys(YearAssessment year) {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    addEvidence(keys, year.evidence());
    addEvidence(keys, year.counterEvidence());
    addEvidence(keys, year.contextEvidence());
    year.ruleKeys().stream().map(key -> "rule." + key).forEach(keys::add);
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("career timeline section requires evidence");
    }
    return List.copyOf(keys);
  }

  private void addEvidence(LinkedHashSet<String> keys, List<ReportEvidence> evidence) {
    evidence.stream()
        .map(ReportEvidence::key)
        .filter(Objects::nonNull)
        .filter(key -> !key.isBlank())
        .forEach(keys::add);
  }

  private List<String> corePhrases(CareerContext.Status status) {
    List<String> common = List.of("事业优先顺序", "年度事业行动");
    List<String> result = new ArrayList<>(common);
    result.addAll(switch (status) {
      case EMPLOYED -> List.of("工作成果", "职责变化", "升职准备");
      case SELF_EMPLOYED -> List.of("客户回款", "项目收入", "经营安排");
      case JOB_SEEKING -> List.of("简历投递", "面试反馈", "目标岗位");
      case STUDYING -> List.of("学习作品", "入行准备", "项目反馈");
    });
    return List.copyOf(result);
  }
}
