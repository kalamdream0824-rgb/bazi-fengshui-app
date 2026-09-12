package com.bazi.app.report;

import java.util.List;

/** Writes career-specific outcomes; it never substitutes generic wealth or relationship copy. */
final class CareerActionGuideWriter {

  AnnualActionGuide write(
      CareerContext context,
      int yearIndex,
      String ruleKey,
      AnnualStage stage,
      String problem,
      String action,
      String adjustmentCondition,
      String fallbackAction,
      List<String> evidenceKeys) {
    return new AnnualActionGuide(
        focusKey(ruleKey, stage),
        complete(problem),
        complete(action),
        expectedChange(context.status(), yearIndex, ruleKey),
        checkTiming(context.status(), yearIndex),
        successSignal(context.status(), yearIndex, ruleKey),
        complete(adjustmentCondition),
        complete(fallbackAction),
        evidenceKeys);
  }

  private String focusKey(String ruleKey, AnnualStage stage) {
    return ruleKey == null || ruleKey.isBlank()
        ? "career.stage." + stage.name().toLowerCase()
        : ruleKey;
  }

  private String expectedChange(
      CareerContext.Status status, int yearIndex, String ruleKey) {
    String focused = switch (ruleKey) {
      case "career.role_transition" -> switch (status) {
        case EMPLOYED -> "这样做能先弄清工作内容和分工怎么变，再决定是否接受。";
        case SELF_EMPLOYED -> "这样做能先验证客户或经营做法的变化，再决定是否增加投入。";
        case JOB_SEEKING -> "这样做能缩小求职方向，避免在不合适的岗位上反复尝试。";
        case STUDYING -> "这样做能验证新的学习方向是否适合，再决定是否继续投入。";
      };
      case "career.coordination_window" -> switch (status) {
        case EMPLOYED -> "这样做能把需要谁配合、各自做什么说清楚，让工作继续推进。";
        case SELF_EMPLOYED -> "这样做能确认客户或伙伴是否愿意配合，让生意继续推进。";
        case JOB_SEEKING -> "这样做能借助真实介绍和反馈，更快接触合适的岗位。";
        case STUDYING -> "这样做能得到具体指导，少走弯路并完成一件作品。";
      };
      case "career.responsibility_upgrade" -> switch (status) {
        case EMPLOYED -> "这样做能确认更重要的工作是否带来职位、收入或分工变化。";
        case SELF_EMPLOYED -> "这样做能确认重要客户是否带来合理收入，而不只是更忙。";
        case JOB_SEEKING -> "这样做能确认要求更高的岗位是否也给出清楚的条件。";
        case STUDYING -> "这样做能通过真实项目证明能力，并留下可以展示的结果。";
      };
      case "career.visibility" -> switch (status) {
        case EMPLOYED -> "这样做能让负责人看见你解决了什么问题，而不只知道你很忙。";
        case SELF_EMPLOYED -> "这样做能让客户看懂你的实际能力，更容易决定是否合作。";
        case JOB_SEEKING -> "这样做能让招聘方看清你会做什么，提高获得面试的可能。";
        case STUDYING -> "这样做能让别人看见完整作品，再判断能力还缺少什么。";
      };
      case "career.preparation" -> switch (status) {
        case EMPLOYED -> "这样做能补上下一步最需要的能力，再争取职位变化。";
        case SELF_EMPLOYED -> "这样做能先理顺价格、成本和做事方法，再考虑扩大。";
        case JOB_SEEKING -> "这样做能先补好简历、作品或面试准备，再增加投递。";
        case STUDYING -> "这样做能先学会一项真正用得上的本领，再寻找机会。";
      };
      case "career.resource_delivery" -> switch (status) {
        case EMPLOYED -> "这样做能把工作成绩变成谈加薪或升职时可以核对的依据。";
        case SELF_EMPLOYED -> "这样做能看清项目是否真正带来收入，并减少回款拖延。";
        case JOB_SEEKING -> "这样做能用作品和面试表现换来条件明确的工作机会。";
        case STUDYING -> "这样做能把学到的内容变成作品，再换来实习或工作回应。";
      };
      case "career.overextension" -> switch (status) {
        case EMPLOYED -> "这样做能减少同时进行的工作，先保住最重要的结果。";
        case SELF_EMPLOYED -> "这样做能减少低回报项目，避免时间和成本一起失控。";
        case JOB_SEEKING -> "这样做能减少无效投递，把精力留给更合适的岗位。";
        case STUDYING -> "这样做能减少同时学习的方向，先完整掌握一项能力。";
      };
      default -> null;
    };
    return focused == null ? defaultExpectedChange(status, yearIndex) : focused;
  }

  private String defaultExpectedChange(CareerContext.Status status, int yearIndex) {
    return switch (status) {
      case EMPLOYED -> yearIndex == 0
          ? "这样做能让工作结果和职责更清楚，之后谈升职、加薪或调整分工时有具体依据。"
          : "这样做能确认已有成绩是否真的带来职位或收入变化，避免长期只增加工作量。";
      case SELF_EMPLOYED -> yearIndex == 0
          ? "这样做能看清哪些客户和项目真正带来收入，减少只有忙碌却没有回报的安排。"
          : "这样做能留下更省心、回款更稳定的客户，让经营重点比上一年更清楚。";
      case JOB_SEEKING -> yearIndex == 0
          ? "这样做能看清哪些投递可以换来面试，把时间留给回应更明确的岗位。"
          : "这样做能根据实际面试结果缩小范围，提高找到合适工作的可能。";
      case STUDYING -> yearIndex == 0
          ? "这样做能把学习变成一件可以展示的作品，再用真实反馈判断方向是否合适。"
          : "这样做能确认作品是否得到外部认可，再决定继续学习还是开始寻找机会。";
    };
  }

  private String checkTiming(CareerContext.Status status, int yearIndex) {
    return switch (status) {
      case EMPLOYED -> yearIndex == 0
          ? "开始执行后，每周记录一次工作结果和明确反馈；四周后统一检查。"
          : "进入这一年后，每两周核对一次职责、工作量和反馈；六周后统一检查。";
      case SELF_EMPLOYED -> yearIndex == 0
          ? "开始执行后，每周核对客户、项目进度和实际收入；四周后统一检查。"
          : "进入这一年后，每两周核对客户质量、成本和回款；六周后统一检查。";
      case JOB_SEEKING -> yearIndex == 0
          ? "开始执行后，每周核对投递、回复和面试结果；四周后统一检查。"
          : "进入这一年后，每两周整理一次面试反馈；参加三次面试后统一检查。";
      case STUDYING -> yearIndex == 0
          ? "开始执行后，每周核对作品完成进度；四周后请一位外部人士评价。"
          : "进入这一年后，每两周核对作品和实际机会；六周后统一检查。";
    };
  }

  private String successSignal(
      CareerContext.Status status, int yearIndex, String ruleKey) {
    String focused = switch (ruleKey) {
      case "career.role_transition" -> switch (status) {
        case EMPLOYED -> "有效的信号是：新的工作内容、负责人和完成标准已经说清楚。";
        case SELF_EMPLOYED -> "有效的信号是：新的客户或做法带来一笔可以核对的收入。";
        case JOB_SEEKING -> "有效的信号是：缩小方向后，目标岗位开始带来实际回复。";
        case STUDYING -> "有效的信号是：新方向完成一次真实尝试，并得到具体反馈。";
      };
      case "career.coordination_window" -> switch (status) {
        case EMPLOYED -> "有效的信号是：同事或合作方答应承担一项具体工作。";
        case SELF_EMPLOYED -> "有效的信号是：客户或伙伴确认下一步，并按约定完成配合。";
        case JOB_SEEKING -> "有效的信号是：介绍或沟通带来一次真实面试。";
        case STUDYING -> "有效的信号是：指导意见帮助你完成一处明确修改。";
      };
      case "career.responsibility_upgrade" -> switch (status) {
        case EMPLOYED -> "有效的信号是：新增工作同时写清职责、职位或收入中的一项变化。";
        case SELF_EMPLOYED -> "有效的信号是：重要客户增加后，收入提高且额外成本没有失控。";
        case JOB_SEEKING -> "有效的信号是：岗位要求提高时，工资和职责也说得清楚。";
        case STUDYING -> "有效的信号是：真实项目留下可以展示的成果和外部评价。";
      };
      case "career.visibility" -> switch (status) {
        case EMPLOYED -> "有效的信号是：负责人明确说出哪项成绩值得继续。";
        case SELF_EMPLOYED -> "有效的信号是：展示案例后，客户主动询问价格或下一步。";
        case JOB_SEEKING -> "有效的信号是：简历或作品带来招聘方的具体回复。";
        case STUDYING -> "有效的信号是：作品收到一条可以马上修改的意见。";
      };
      case "career.preparation" -> switch (status) {
        case EMPLOYED -> "有效的信号是：补上的能力已经用在一项真实工作里。";
        case SELF_EMPLOYED -> "有效的信号是：价格、成本和做事步骤已经能够逐项核对。";
        case JOB_SEEKING -> "有效的信号是：修改后开始收到更多有效回复或面试。";
        case STUDYING -> "有效的信号是：已经独立完成一件能展示所学能力的作品。";
      };
      case "career.resource_delivery" -> switch (status) {
        case EMPLOYED -> "有效的信号是：工作成绩被记录，并进入加薪或升职讨论。";
        case SELF_EMPLOYED -> "有效的信号是：项目完成后按约定收款，实际收入可以核对。";
        case JOB_SEEKING -> "有效的信号是：作品或面试带来一份条件明确的机会。";
        case STUDYING -> "有效的信号是：作品带来实习、合作或工作的真实回应。";
      };
      case "career.overextension" -> switch (status) {
        case EMPLOYED -> "有效的信号是：减少次要工作后，重要工作按时完成且返工变少。";
        case SELF_EMPLOYED -> "有效的信号是：减少低回报项目后，时间和实际收入都更稳定。";
        case JOB_SEEKING -> "有效的信号是：投递减少后，有效回复和面试比例反而提高。";
        case STUDYING -> "有效的信号是：减少学习方向后，一件作品按计划完成。";
      };
      default -> null;
    };
    return focused == null ? defaultSuccessSignal(status, yearIndex) : focused;
  }

  private String defaultSuccessSignal(CareerContext.Status status, int yearIndex) {
    return switch (status) {
      case EMPLOYED -> yearIndex == 0
          ? "有效的信号是：至少一项工作留下可核对的结果，并得到负责人明确反馈。"
          : "有效的信号是：工作量增加时，职位、收入或职责范围至少有一项同步变化。";
      case SELF_EMPLOYED -> yearIndex == 0
          ? "有效的信号是：至少一个客户或项目进入明确的下一步，并能说清成本和收入。"
          : "有效的信号是：按时回款的客户增加，临时返工或额外开销没有跟着增加。";
      case JOB_SEEKING -> yearIndex == 0
          ? "有效的信号是：目标岗位开始带来实际回复或面试，而不是只有投递数量增加。"
          : "有效的信号是：面试不再反复停在同一环节，并出现条件说得清楚的机会。";
      case STUDYING -> yearIndex == 0
          ? "有效的信号是：完成一件可以展示的作品，并收到一条可以继续修改的意见。"
          : "有效的信号是：作品带来实习、合作或工作回应，而不是只增加学习时间。";
    };
  }

  private String complete(String text) {
    return text.matches(".*[。！？]$") ? text : text + "。";
  }
}
