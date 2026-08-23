package com.bazi.app.report;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class CareerNarrativePlanner {

  public CareerNarrativePlan plan(ThreeYearAssessment assessment, CareerContext context) {
    Objects.requireNonNull(assessment, "assessment");
    Objects.requireNonNull(context, "context");
    if (assessment.topic() != ReportTopic.CAREER) {
      throw new IllegalArgumentException("career narrative requires the career topic");
    }

    List<CareerNarrativePlan.YearNarrative> years = new ArrayList<>();
    for (int index = 0; index < assessment.years().size(); index++) {
      years.add(year(assessment.years().get(index), context, index));
    }
    LinkedHashSet<String> route = new LinkedHashSet<>();
    years.forEach(year -> route.addAll(year.actions()));
    return new CareerNarrativePlan(
        thesis(context, assessment.years()),
        contextSummary(context),
        years,
        route.stream().limit(3).toList());
  }

  private CareerNarrativePlan.YearNarrative year(
      YearAssessment year,
      CareerContext context,
      int index) {
    String ruleKey = year.ruleKeys().isEmpty() ? "" : year.ruleKeys().get(0);
    List<String> actions = List.of(
        goalAction(context.goal(), index),
        supportingAction(context.status(), index));

    return new CareerNarrativePlan.YearNarrative(
        year.year(),
        year.ganZhi(),
        year.stage().label(),
        plainHeadline(ruleKey, year.stage(), context.status()),
        verdict(year.stage(), context.status()),
        List.of(astrologyReason(ruleKey, year.stage(), context.status()), contextReason(context, index)),
        obstacle(context, index),
        actions,
        changeCondition(context, index),
        year.evidence().stream().map(ReportEvidence::key).distinct().toList(),
        year.evidence(),
        year.counterEvidence(),
        confidence(year.confidence()));
  }

  private String contextSummary(CareerContext context) {
    return context.status().label() + " · " + context.goal().label() + " · " + paceSummary(context);
  }

  private String paceSummary(CareerContext context) {
    return switch (context.pace()) {
      case SMOOTH -> switch (context.status()) {
        case EMPLOYED -> "工作进展顺利";
        case SELF_EMPLOYED -> "生意进展顺利";
        case JOB_SEEKING -> "求职进展顺利";
        case STUDYING -> "学习进展顺利";
      };
      case STALLED -> switch (context.status()) {
        case EMPLOYED -> "工作推进不顺";
        case SELF_EMPLOYED -> "生意进展不顺";
        case JOB_SEEKING -> "求职进展不顺";
        case STUDYING -> "学习进展不顺";
      };
      case HIGH_PRESSURE -> switch (context.status()) {
        case EMPLOYED -> "工作压力较大";
        case SELF_EMPLOYED -> "经营压力较大";
        case JOB_SEEKING -> "求职压力较大";
        case STUDYING -> "学习压力较大";
      };
      case PREPARING_CHANGE -> switch (context.goal()) {
        case PROMOTION -> "正在为升职做准备";
        case JOB_CHANGE -> "正在准备换工作";
        case STABILITY -> "正在调整工作状态";
        case TRANSITION -> "正在尝试新方向";
      };
    };
  }

  private String thesis(CareerContext context, List<YearAssessment> years) {
    boolean strengthens = momentum(years.get(1).stage()) > momentum(years.get(0).stage());
    return switch (context.status()) {
      case EMPLOYED -> switch (context.goal()) {
        case PROMOTION -> strengthens
            ? "今年先把手上的成绩做出来，明年再找机会谈晋升。"
            : "这两年先把手上的成绩做稳；工作加了不少却没加薪时，要早点说清楚。";
        case JOB_CHANGE -> strengthens
            ? "今年先明确下一份工作想要什么，明年再认真寻找新机会。"
            : "先把想找的工作缩小到两类，不要因为着急就到处投简历。";
        case STABILITY -> "这两年先把最重要的工作做好，不该你长期负责的事要及时说清楚。";
        case TRANSITION -> strengthens
            ? "先用一个小项目试试新方向，确定适合自己后再考虑转行。"
            : "先别急着辞职，做个小项目看看新方向是不是真的适合自己。";
      };
      case SELF_EMPLOYED -> switch (context.goal()) {
        case PROMOTION -> "今年先把最稳定的客户和项目做好，明年再考虑扩大生意。";
        case JOB_CHANGE -> "如果准备回到职场，今年先确定想找的工作，明年再认真求职。";
        case STABILITY -> "这两年先稳定客户和收入，不要同时开太多新项目。";
        case TRANSITION -> "先用一个小项目试试新的生意方向，确认可行后再增加投入。";
      };
      case JOB_SEEKING -> switch (context.goal()) {
        case PROMOTION -> "今年先争取进入合适的岗位，明年再考虑更高的职位。";
        case JOB_CHANGE -> "今年先明确想找哪类工作，明年争取进入合适的岗位。";
        case STABILITY -> "这两年先找一份工作内容和收入都比较稳定的机会。";
        case TRANSITION -> "先用作品或短项目验证新方向，再集中寻找相应的工作。";
      };
      case STUDYING -> switch (context.goal()) {
        case PROMOTION -> "今年先准备好入行需要的能力和作品，明年再争取更好的机会。";
        case JOB_CHANGE -> "今年先确定目标行业和岗位，明年再认真求职。";
        case STABILITY -> "这两年先学好最重要的一项能力，并完成一件完整作品。";
        case TRANSITION -> "先用一个小项目试试新方向，确定适合自己后再继续投入。";
      };
    };
  }

  private String verdict(AnnualStage stage, CareerContext.Status status) {
    return switch (status) {
      case EMPLOYED -> switch (stage) {
        case ADVANCE -> "可以主动争取更好的职位，但要拿已经做成的事来说话。";
        case PREPARE -> "先补上最影响下一步的一项能力，不用急着换职位。";
        case CAUTION -> "这段时间先少接一些额外工作，别让自己只变忙、没有收获。";
        case TRANSITION -> "工作内容可能会变，先问清楚要做什么，再决定接不接。";
        case STABLE -> "先把手上最重要的事做好，再找机会谈职位或分工。";
      };
      case SELF_EMPLOYED -> switch (stage) {
        case ADVANCE -> "可以多接一些生意，但只做自己已经做顺的项目。";
        case PREPARE -> "先把做事流程理顺，不要急着同时接很多客户。";
        case CAUTION -> "先少接项目，保证手里的钱和已有客户不出问题。";
        case TRANSITION -> "做生意的方法需要调整，先小范围试一试，别急着花大钱。";
        case STABLE -> "先把老客户服务好，不要一下子开太多新项目。";
      };
      case JOB_SEEKING -> switch (stage) {
        case ADVANCE -> "可以多投一些简历，但只投自己真正想去、也适合的工作。";
        case PREPARE -> "先补上目标工作最看重的能力和作品，再多投简历。";
        case CAUTION -> "不要用海投安慰自己，先改清楚求职方向和简历。";
        case TRANSITION -> "想找的工作可能需要调整，先缩小范围，再增加投递。";
        case STABLE -> "保持投递，但更重要的是投得准，不是投得多。";
      };
      case STUDYING -> switch (stage) {
        case ADVANCE -> "可以开始拿作品找实习或工作，不用一直只做准备。";
        case PREPARE -> "先学好最影响入行的一项能力，再做出能给别人看的作品。";
        case CAUTION -> "不要同时学太多方向，先完整做出一个作品。";
        case TRANSITION -> "学习方向可能要调整，先做个短项目试试，再决定要不要继续。";
        case STABLE -> "保持学习，也要把学到的东西变成作品或真实经历。";
      };
    };
  }

  private String plainHeadline(
      String ruleKey,
      AnnualStage stage,
      CareerContext.Status status) {
    return switch (ruleKey) {
      case "career.role_transition" -> switch (status) {
        case EMPLOYED -> "工作内容可能有变化";
        case SELF_EMPLOYED -> "生意做法可能要调整";
        case JOB_SEEKING -> "求职方向可能需要调整";
        case STUDYING -> "学习或入行方向可能需要调整";
      };
      case "career.coordination_window" -> switch (status) {
        case EMPLOYED -> "有人帮你，工作更容易做成";
        case SELF_EMPLOYED -> "有人帮你，生意更容易推进";
        case JOB_SEEKING -> "有人帮你，更容易获得面试机会";
        case STUDYING -> "有人指点，学习和入行会更顺";
      };
      case "career.responsibility_upgrade" -> switch (status) {
        case EMPLOYED -> "更重要的工作可能会找上你";
        case SELF_EMPLOYED -> "可能接到更重要的客户或项目";
        case JOB_SEEKING -> "更有机会接触要求较高的工作";
        case STUDYING -> "更有机会参与真实项目";
      };
      case "career.visibility" -> switch (status) {
        case EMPLOYED -> "更容易让别人看见你的能力";
        case SELF_EMPLOYED -> "更容易让客户看见你的能力";
        case JOB_SEEKING -> "更容易让招聘方看见你的能力";
        case STUDYING -> "更容易让别人看见你的作品";
      };
      case "career.preparation" -> switch (status) {
        case EMPLOYED -> "先补好能力，再争取下一步";
        case SELF_EMPLOYED -> "先把生意基础理顺，再扩大";
        case JOB_SEEKING -> "先准备好简历和面试，再多投";
        case STUDYING -> "先学会一项本领，再寻找机会";
      };
      case "career.resource_delivery" -> switch (status) {
        case EMPLOYED -> "先做出成绩，才更容易加薪";
        case SELF_EMPLOYED -> "先把项目做好，收入才会增加";
        case JOB_SEEKING -> "先拿出作品，才更容易得到机会";
        case STUDYING -> "先做出作品，才更容易获得认可";
      };
      case "career.overextension" -> switch (status) {
        case EMPLOYED -> "工作太多，容易顾不过来";
        case SELF_EMPLOYED -> "项目太多，容易顾不过来";
        case JOB_SEEKING -> "投得太多，容易失去重点";
        case STUDYING -> "学得太杂，反而不容易学成";
      };
      default -> defaultHeadline(stage, status);
    };
  }

  private String defaultHeadline(AnnualStage stage, CareerContext.Status status) {
    return switch (stage) {
      case ADVANCE -> switch (status) {
        case EMPLOYED -> "今年适合主动争取工作机会";
        case SELF_EMPLOYED -> "今年适合主动争取生意机会";
        case JOB_SEEKING -> "今年适合主动争取面试机会";
        case STUDYING -> "今年适合主动争取学习和入行机会";
      };
      case PREPARE -> "今年先准备充分，再寻找机会";
      case CAUTION -> "今年先减少压力，不要勉强自己";
      case TRANSITION -> "今年会有变化，先看清再决定";
      case STABLE -> switch (status) {
        case EMPLOYED -> "今年先把手上的工作做好";
        case SELF_EMPLOYED -> "今年先把现有生意做好";
        case JOB_SEEKING -> "今年先把求职方向定清楚";
        case STUDYING -> "今年先把正在学的内容学好";
      };
    };
  }

  private String astrologyReason(
      String ruleKey,
      AnnualStage stage,
      CareerContext.Status status) {
    return switch (ruleKey) {
      case "career.role_transition" -> switch (status) {
        case EMPLOYED -> "今年的工作容易出现变化，原来的分工或做事方式可能会调整。";
        case SELF_EMPLOYED -> "今年生意的客户、产品或做法容易变化，原来的经营方式可能要调整。";
        case JOB_SEEKING -> "今年求职方向或目标岗位容易变化，原来的投递方法可能要调整。";
        case STUDYING -> "今年学习或入行方向容易变化，原来的课程和计划可能要调整。";
      };
      case "career.coordination_window" -> switch (status) {
        case EMPLOYED -> "今年更容易得到同事或合作方的帮助，很多事不用一个人承担。";
        case SELF_EMPLOYED -> "今年更容易得到客户或合作伙伴的帮助，生意不用全靠自己推进。";
        case JOB_SEEKING -> "今年更容易得到熟人、同行或招聘方的帮助，面试机会可能增加。";
        case STUDYING -> "今年更容易遇到愿意指导你的人，学习和找机会会更顺一些。";
      };
      case "career.responsibility_upgrade" -> switch (status) {
        case EMPLOYED -> "你可能会被交给更重要的工作，也更容易被领导看见。";
        case SELF_EMPLOYED -> "你可能会接到更重要的客户或项目，也更容易被市场看见。";
        case JOB_SEEKING -> "你可能会接触要求更高的岗位，也更容易得到招聘方注意。";
        case STUDYING -> "你可能会参与更真实的项目，也更容易让别人看到你的能力。";
      };
      case "career.visibility" -> switch (status) {
        case EMPLOYED -> "今年适合主动说出自己的想法，让领导知道你能解决什么问题。";
        case SELF_EMPLOYED -> "今年适合主动展示案例和服务，让客户知道你能解决什么问题。";
        case JOB_SEEKING -> "今年适合把简历和作品写具体，让招聘方看清楚你会做什么。";
        case STUDYING -> "今年适合主动展示作品，让老师或从业者给出实际反馈。";
      };
      case "career.preparation" -> switch (status) {
        case EMPLOYED -> "今年更适合补好工作需要的能力，准备充分后再争取职位变化。";
        case SELF_EMPLOYED -> "今年更适合理顺产品、价格和流程，基础稳了再扩大生意。";
        case JOB_SEEKING -> "今年更适合准备简历、作品和面试，准备充分后再增加投递。";
        case STUDYING -> "今年更适合先学好关键能力，再用完整作品寻找入行机会。";
      };
      case "career.resource_delivery" -> switch (status) {
        case EMPLOYED -> "今年有机会加薪或升职，但前提是先拿出清楚的工作成绩。";
        case SELF_EMPLOYED -> "今年有机会增加收入，但前提是先把项目做好并按时收回款。";
        case JOB_SEEKING -> "今年的机会取决于简历、作品和面试表现，准备越具体越容易得到回应。";
        case STUDYING -> "今年能否获得机会，主要看学到的内容能不能变成完整作品。";
      };
      case "career.overextension" -> switch (status) {
        case EMPLOYED -> "今年容易一下子接太多工作，忙乱和返工会影响最后的成绩。";
        case SELF_EMPLOYED -> "今年容易一下子接太多项目，时间和成本可能顾不过来。";
        case JOB_SEEKING -> "今年容易为了增加机会到处投简历，反而没有时间认真准备面试。";
        case STUDYING -> "今年容易同时学习太多内容，最后每一项都难以真正学会。";
      };
      default -> defaultAstrologyReason(stage, status);
    };
  }

  private String defaultAstrologyReason(AnnualStage stage, CareerContext.Status status) {
    return switch (stage) {
      case ADVANCE -> switch (status) {
        case EMPLOYED -> "今年工作机会比平时多一些，主动开口和行动更容易有结果。";
        case SELF_EMPLOYED -> "今年生意机会比平时多一些，主动联系客户更容易有结果。";
        case JOB_SEEKING -> "今年面试机会比平时多一些，主动联系和投递更容易有回应。";
        case STUDYING -> "今年学习和入行机会较多，主动展示作品更容易得到回应。";
      };
      case PREPARE -> "今年先准备会更稳，急着改变反而容易多走弯路。";
      case CAUTION -> switch (status) {
        case EMPLOYED -> "今年工作容易变多，先顾好最重要的部分会更稳。";
        case SELF_EMPLOYED -> "今年项目容易变多，先顾好成本和已有客户会更稳。";
        case JOB_SEEKING -> "今年求职容易消耗精力，先把方向和简历准备清楚会更稳。";
        case STUDYING -> "今年学习任务容易变多，先学好最重要的一项会更稳。";
      };
      case TRANSITION -> "今年容易遇到变化，原来的做法可能需要跟着调整。";
      case STABLE -> switch (status) {
        case EMPLOYED -> "今年工作变化不算大，把手上的事做好比盲目改变更重要。";
        case SELF_EMPLOYED -> "今年生意变化不算大，服务好现有客户比盲目扩大更重要。";
        case JOB_SEEKING -> "今年求职机会比较平稳，投得准确比盲目增加数量更重要。";
        case STUDYING -> "今年学习节奏比较平稳，完成作品比盲目更换方向更重要。";
      };
    };
  }

  private String contextReason(CareerContext context, int index) {
    if (index == 0) {
      return switch (context.pace()) {
        case SMOOTH -> switch (context.status()) {
          case EMPLOYED -> "目前工作进展顺利，但还要看这些成绩能不能带来加薪或升职。";
          case SELF_EMPLOYED -> "目前生意进展顺利，但还要看客户和收入能不能稳定下来。";
          case JOB_SEEKING -> "目前求职进展顺利，但还要看面试能不能带来合适的工作机会。";
          case STUDYING -> "目前学习进展顺利，但还要看学到的内容能不能变成作品或实习机会。";
        };
        case STALLED -> switch (context.status()) {
          case EMPLOYED -> "目前工作推进不顺，先弄清楚是目标不明确、缺少支持，还是沟通不到位。";
          case SELF_EMPLOYED -> "目前生意进展不顺，先看看是客户太少、价格不合适，还是回款太慢。";
          case JOB_SEEKING -> "目前求职进展不顺，先看看问题出在求职方向、简历，还是面试表现。";
          case STUDYING -> "目前学习进展不顺，先看看是方向没选好、作品不够，还是实际尝试太少。";
        };
        case HIGH_PRESSURE -> switch (context.status()) {
          case EMPLOYED -> "目前工作压力较大，再出现新机会时，要先确认自己还有没有时间和精力。";
          case SELF_EMPLOYED -> "目前经营压力较大，接新项目之前，要先确认时间、成本和回款是否合适。";
          case JOB_SEEKING -> "目前求职压力较大，不要因为着急就接受工作内容和工资都说不清的机会。";
          case STUDYING -> "目前学习压力较大，先减少同时进行的课程和项目，避免每一项都做不完。";
        };
        case PREPARING_CHANGE -> switch (context.goal()) {
          case PROMOTION -> "你已经开始为升职做准备，下一步要用实际成绩证明自己能承担更重要的工作。";
          case JOB_CHANGE -> "你已经开始准备换工作，下一步要真正投递和面试，不能只比较职位信息。";
          case STABILITY -> "你正在调整工作状态，下一步要先减少最耗精力、又没有实际收获的事情。";
          case TRANSITION -> "你已经开始尝试新方向，下一步要用一个真实项目确认自己是否适合。";
        };
      };
    }
    return switch (context.status()) {
      case EMPLOYED -> "明年重点看：工作变多以后，工资和职位有没有跟着变。";
      case SELF_EMPLOYED -> "明年重点看：有没有稳定客户，项目做完能不能按时收到钱。";
      case JOB_SEEKING -> "明年重点看：投出的简历能不能带来稳定的面试。";
      case STUDYING -> "明年重点看：学到的东西有没有变成作品、实习或工作机会。";
    };
  }

  private String obstacle(CareerContext context, int index) {
    return switch (context.pace()) {
      case SMOOTH -> switch (context.status()) {
        case EMPLOYED -> index == 0
            ? "工作已经做出一些成绩，但加薪或升职还没有落实。"
            : "工作暂时顺利，容易让人忽略后面可能出现的变化。";
        case SELF_EMPLOYED -> index == 0
            ? "生意已经有些起色，但客户和收入还不够稳定。"
            : "眼前订单增加，容易忽略成本和回款是否正常。";
        case JOB_SEEKING -> index == 0
            ? "已经收到一些面试邀请，但合适的工作还没有确定。"
            : "几次面试进展顺利，容易因此放松后面的准备。";
        case STUDYING -> index == 0
            ? "学习进展不错，但还没有做出一件完整的作品。"
            : "目前学得顺利，容易忽略实际练习和外部反馈。";
      };
      case STALLED -> switch (context.status()) {
        case EMPLOYED -> index == 0
            ? "工作一直很忙，但还没弄清楚问题出在目标、支持还是沟通上。"
            : "原来的做法没有带来变化，却还在继续重复。";
        case SELF_EMPLOYED -> index == 0
            ? "生意没有起色，但还没分清是客户、价格还是回款的问题。"
            : "原来的获客方法没有效果，却还在继续投入。";
        case JOB_SEEKING -> index == 0
            ? "投了简历却很少收到面试，还没找出是方向还是简历的问题。"
            : "原来的投递方法没有回应，却还在继续增加数量。";
        case STUDYING -> index == 0
            ? "学了不少内容，却还没有形成一件可以展示的完整作品。"
            : "原来的学习方法效果不明显，却还在继续重复。";
      };
      case HIGH_PRESSURE -> switch (context.status()) {
        case EMPLOYED -> index == 0
            ? "工作已经让你很累，很难再想清楚下一步。"
            : "长时间没有休息，工作质量已经开始下降。";
        case SELF_EMPLOYED -> index == 0
            ? "经营压力占满了时间，容易顾不上成本和回款。"
            : "项目接得太多，已经开始影响服务质量。";
        case JOB_SEEKING -> index == 0
            ? "求职消耗了很多精力，容易为了尽快入职而降低判断。"
            : "长时间求职没有休息，面试状态开始受影响。";
        case STUDYING -> index == 0
            ? "学习任务安排得太多，每一项都难以完整做完。"
            : "长时间没有休息，理解和练习的效果开始下降。";
      };
      case PREPARING_CHANGE -> switch (context.goal()) {
        case PROMOTION -> index == 0
            ? "为升职做了不少准备，但还没有拿出能证明成绩的材料。"
            : "一直等待合适时机，却没有真正开口争取。";
        case JOB_CHANGE -> index == 0
            ? "已经想了很多换工作的事，但还没有认真投出简历。"
            : "一直比较不同机会，却迟迟没有参加实际面试。";
        case STABILITY -> index == 0
            ? "想让工作稳定下来，但还没有减少最消耗精力的事情。"
            : "已经发现问题，却迟迟没有调整每天的工作安排。";
        case TRANSITION -> index == 0
            ? "为新方向做了不少准备，但还没有完成一次真实尝试。"
            : "一直学习和比较，却没有做出能检验方向的作品。";
      };
    };
  }

  private String goalAction(CareerContext.Goal goal, int index) {
    return switch (goal) {
      case PROMOTION -> index == 0
          ? "整理三件最能说明自己成绩的事"
          : "拿已经做成的成绩去谈加薪或晋升";
      case JOB_CHANGE -> index == 0
          ? "只选两类最想找的工作，分别准备简历"
          : "只认真考虑工资、工作内容和发展都说得清楚的机会";
      case STABILITY -> index == 0
          ? "列清楚最重要的工作，以及可以暂时不做的事"
          : "把主要工作做好，不长期替别人收拾没说清楚的事";
      case TRANSITION -> index == 0
          ? "做一个小项目，试试新方向是否适合自己"
          : "试过确实可行，再考虑花更多时间、钱或辞职";
    };
  }

  private String supportingAction(CareerContext.Status status, int index) {
    return switch (status) {
      case EMPLOYED -> index == 0
          ? "和领导说清楚哪些事该你负责，哪些事暂时不要接"
          : "每隔一段时间看看，忙碌有没有换来加薪或职位变化";
      case SELF_EMPLOYED -> index == 0
          ? "接项目前先说清价格、时间和要做到什么程度"
          : "留下按时付钱、合作省心的客户，少接麻烦的生意";
      case JOB_SEEKING -> index == 0
          ? "每周记录投递、面试和回复，留下真正有效的方法"
          : "根据面试反馈调整简历，不要只增加投递数量";
      case STUDYING -> index == 0
          ? "选一个方向，做出可以直接给别人看的作品"
          : "拿作品找一次实习或工作，看看市场是否认可";
    };
  }

  private String changeCondition(CareerContext context, int index) {
    return switch (context.pace()) {
      case SMOOTH -> switch (context.status()) {
        case EMPLOYED -> index == 0
            ? "如果连续两个月只是更忙，却没有加薪或升职，就要和领导谈清楚。"
            : "如果工作一直增加，工资和职位却没变化，就不要再继续多接。";
        case SELF_EMPLOYED -> index == 0
            ? "如果订单增加，实际收入却没有提高，就要重新检查价格和成本。"
            : "如果客户越来越多，回款却越来越慢，就要减少风险较高的项目。";
        case JOB_SEEKING -> index == 0
            ? "如果面试增加，却始终没有合适的录用机会，就要重新检查求职方向。"
            : "如果收到的机会都不合适，就不要为了入职而降低最重要的要求。";
        case STUDYING -> index == 0
            ? "如果学习时间增加，却一直没有完整作品，就要减少同时学习的内容。"
            : "如果作品一直得不到实际反馈，就要主动找老师或从业者评价。";
      };
      case STALLED -> switch (context.status()) {
        case EMPLOYED -> index == 0
            ? "如果一个月后工作仍没有进展，就要重新确认目标，并找领导谈清楚。"
            : "如果换过做法后仍没有改善，就要考虑换岗位或换工作。";
        case SELF_EMPLOYED -> index == 0
            ? "如果一个月仍没有新客户或订单，就要重新检查客户来源、价格和产品。"
            : "如果换过销售方法仍没有起色，就减少投入，重新选择主要项目。";
        case JOB_SEEKING -> index == 0
            ? "如果一个月投出的简历仍换不来面试，就要调整求职方向和简历。"
            : "如果多次面试都停在同一环节，就针对这个环节重新准备。";
        case STUDYING -> index == 0
            ? "如果一个月后还没有完整作品，就要缩小题目，先完成一件。"
            : "如果作品一直得不到正面反馈，就要更换课程或练习方法。";
      };
      case HIGH_PRESSURE -> switch (context.status()) {
        case EMPLOYED -> index == 0
            ? "如果工作已经影响睡眠和身体，就要和领导商量减少任务。"
            : "如果休息后仍无法恢复，就要把健康放在职位变化之前。";
        case SELF_EMPLOYED -> index == 0
            ? "如果项目已经影响睡眠和身体，就要暂停接新项目。"
            : "如果减少项目后仍无法恢复，就要重新安排经营规模。";
        case JOB_SEEKING -> index == 0
            ? "如果求职已经影响睡眠和情绪，就要暂时减少投递，先恢复状态。"
            : "如果休息后面试状态仍没有改善，就要寻求具体反馈或帮助。";
        case STUDYING -> index == 0
            ? "如果学习已经影响睡眠和身体，就要减少课程或项目。"
            : "如果休息后仍学不进去，就要重新安排学习目标和进度。";
      };
      case PREPARING_CHANGE -> switch (context.goal()) {
        case PROMOTION -> index == 0
            ? "如果一个月后还没有整理好成绩，就先完成一份清楚的成绩清单。"
            : "如果准备充分后仍不敢开口，就约定一个具体时间和领导沟通。";
        case JOB_CHANGE -> index == 0
            ? "如果一个月后还没有投出简历，就把目标缩小到两类工作，马上开始。"
            : "如果参加几次面试后都不合适，就重新检查目标职位和要求。";
        case STABILITY -> index == 0
            ? "如果一个月后工作仍然混乱，就先停止最不重要的两件事。"
            : "如果调整安排后仍然疲惫，就要重新商量工作量或考虑换工作。";
        case TRANSITION -> index == 0
            ? "如果一个月后还没有真实作品，就把项目缩小，先完成一个版本。"
            : "如果几次真实尝试都不合适，就不要再继续投入更多时间和钱。";
      };
    };
  }

  private int momentum(AnnualStage stage) {
    return switch (stage) {
      case CAUTION -> 0;
      case PREPARE -> 1;
      case STABLE, TRANSITION -> 2;
      case ADVANCE -> 3;
    };
  }

  private String confidence(ConfidenceLevel confidence) {
    return switch (confidence) {
      case HIGH -> "高";
      case MEDIUM -> "中";
      case LOW -> "低";
    };
  }
}
