package com.bazi.app.report.overall;

import java.util.List;

final class OverallPlainCopy {

  String linkage(
      OverallDimension primary, OverallDimension secondary, OverallStance primaryStance) {
    String lead = primaryStance == OverallStance.PRESSURED
        ? "先稳住" + primary.label()
        : "先看清" + primary.label();
    String impact = switch (primary) {
      case RHYTHM -> switch (secondary) {
        case CAREER -> "时间和精力是否够用，会直接影响事业责任能否按时完成并留下成果";
        case WEALTH -> "人在忙乱时更容易忽略账目或用钱换时间，这会影响钱财安排能否留出余量";
        case RELATIONSHIP -> "忙乱会挤占沟通和相处时间，关系支持也会随之变弱";
        case RHYTHM -> throw sameDimension(primary);
      };
      case CAREER -> switch (secondary) {
        case RHYTHM -> "工作量和责任边界会决定生活节奏是否还能留出休息时间";
        case WEALTH -> "工作成果能否落实到回报，会直接影响钱财安排的实际余量";
        case RELATIONSHIP -> "工作占用的时间和情绪会带回日常相处，进而影响关系支持";
        case CAREER -> throw sameDimension(primary);
      };
      case WEALTH -> switch (secondary) {
        case RHYTHM -> "钱是否留有余量，会决定生活节奏遇到变化时有没有调整空间";
        case CAREER -> "钱安排得太紧会限制工作选择，进而影响事业责任如何取舍";
        case RELATIONSHIP -> "共同开支和现实分工说不清，关系支持就容易被钱的问题消耗";
        case WEALTH -> throw sameDimension(primary);
      };
      case RELATIONSHIP -> switch (secondary) {
        case RHYTHM -> "沟通和分工是否稳定，会直接影响生活节奏能不能按计划进行";
        case CAREER -> "相处中的支持或消耗会带到工作状态，进而影响事业责任的完成质量";
        case WEALTH -> "共同开支和责任边界会影响钱财安排，口头约定还要落实到账目";
        case RELATIONSHIP -> throw sameDimension(primary);
      };
    };
    return lead + "，因为" + impact + "。";
  }

  private IllegalArgumentException sameDimension(OverallDimension dimension) {
    return new IllegalArgumentException("overall linkage requires different dimensions: " + dimension);
  }

  String headline(OverallDimension dimension, OverallStance stance, int index) {
    return stanceLead(stance) + headlineObjects(dimension).get(index % 3);
  }

  String judgment(
      OverallDimension dimension, OverallStance stance, OverallEvidenceAngle angle) {
    return driverSentence(dimension, angle.primaryKey()) + "；"
        + stanceOutcome(dimension, stance) + qualifier(angle.secondaryKey());
  }

  String priorityIssue(
      OverallDimension dimension, OverallStance stance, OverallEvidenceAngle angle) {
    String reason = shortReason(angle.primaryKey());
    return switch (dimension) {
      case RHYTHM -> stance == OverallStance.PRESSURED
          ? "最先要处理的是安排过满：" + reason + "，每天必须留出恢复时间。"
          : "最先要处理的是精力分散：" + reason + "，不要同时增加太多目标。";
      case CAREER -> stance == OverallStance.PRESSURED
          ? "最先要处理的是工作责任不清：" + reason + "，新增任务要先说清权限和期限。"
          : "最先要处理的是成果兑现：" + reason + "，要留下别人能看见的完成结果。";
      case WEALTH -> stance == OverallStance.PRESSURED
          ? "最先要处理的是钱的余量不足：" + reason + "，先保住必要开支。"
          : "最先要处理的是收支顺序：" + reason + "，钱到账后先留出备用金。";
      case RELATIONSHIP -> stance == OverallStance.PRESSURED
          ? "最先要处理的是反复出现的分歧：" + reason + "，把具体事情说清楚。"
          : "最先要处理的是沟通能否落实：" + reason + "，要看时间和行动是否配合。";
    };
  }

  String continuedPriorityIssue(
      OverallDimension dimension, OverallEvidenceAngle angle, OverallTransition transition) {
    String reason = shortReason(angle.primaryKey());
    return switch (dimension) {
      case RHYTHM -> switch (transition.relation()) {
        case "延续" -> "生活节奏的问题延续到" + transition.toYear() + "年：" + reason
            + "，今年要确认固定休息是否真正保留下来。";
        case "加强" -> "生活节奏受到的影响正在加强：" + reason
            + "，先删减安排，再考虑增加目标。";
        case "缓和" -> "生活节奏受到的影响有所缓和：" + reason
            + "，先恢复稳定作息，再逐步增加安排。";
        default -> throw unsupportedTransition(transition);
      };
      case CAREER -> switch (transition.relation()) {
        case "延续" -> "事业责任延续到" + transition.toYear() + "年：" + reason
            + "，今年要确认已有投入是否形成可核对的成果和条件。";
        case "加强" -> "事业责任带来的影响正在加强：" + reason
            + "，先明确责任、期限和对应条件。";
        case "缓和" -> "事业责任带来的压力有所缓和：" + reason
            + "，先把已经完成的成果落实下来。";
        default -> throw unsupportedTransition(transition);
      };
      case WEALTH -> switch (transition.relation()) {
        case "延续" -> "钱财安排延续到" + transition.toYear() + "年：" + reason
            + "，今年要核对备用金是否真正留下。";
        case "加强" -> "钱财压力正在加强：" + reason
            + "，先保住必要开支和现金余量。";
        case "缓和" -> "钱财压力有所缓和：" + reason
            + "，先确认实际余钱增加，再考虑新增支出。";
        default -> throw unsupportedTransition(transition);
      };
      case RELATIONSHIP -> switch (transition.relation()) {
        case "延续" -> "关系里的同一问题延续到" + transition.toYear() + "年：" + reason
            + "，今年要核对沟通是否变成实际行动。";
        case "加强" -> "关系问题带来的影响正在加强：" + reason
            + "，先把分歧和彼此边界说清楚。";
        case "缓和" -> "关系问题带来的影响有所缓和：" + reason
            + "，先确认沟通和实际配合能否稳定。";
        default -> throw unsupportedTransition(transition);
      };
    };
  }

  String action(OverallDimension dimension, OverallStance stance, int index) {
    boolean pressured = stance == OverallStance.PRESSURED;
    List<String> variants = switch (dimension) {
      case RHYTHM -> pressured
          ? List.of(
              "删掉一个可以延后的安排，给睡眠和休息留下固定时间。",
              "每周留出两个不接临时事情的时段，让精力恢复下来。",
              "新目标开始前先结束一件旧事，避免安排继续堆积。")
          : List.of(
              "只设一个年度重点，并在每周安排里为它保留固定时间。",
              "每周只写三件必须完成的事，其他安排按余力处理。",
              "先完成已经开始的重点，再决定是否增加新的目标。");
      case CAREER -> pressured
          ? List.of(
              "接下新任务前，确认负责人、截止时间和可以暂停的旧任务。",
              "把现有工作量和缺少的支持写清楚，再与负责人确认安排。",
              "对职责、时间和回报都说不清的临时任务，先不要继续增加。")
          : List.of(
              "选一项最重要的工作成果，写清完成标准和交付日期。",
              "每周记录一次已经完成的成果，并主动取得具体反馈。",
              "用已经完成的结果争取更清楚的权限、职位或回报。");
      case WEALTH -> pressured
          ? List.of(
              "列出必要支出和可延后支出，先保留三个月的日常用钱。",
              "每次大额付款前重新核对余额、付款期限和后续必要开支。",
              "回款没有按时到账时，暂停新的大额支出和借款安排。")
          : List.of(
              "每笔新增收入到账后，先留下必要开支和备用金。",
              "分别记录收入、成本和税费，按实际剩下的钱判断结果。",
              "把一部分实际结余单独留下，不让新增收入全部变成消费。");
      case RELATIONSHIP -> pressured
          ? List.of(
              "针对反复出现的一件事，说清各自需要什么、能做到什么。",
              "确认双方能稳定拿出的时间，不用临时承诺代替长期安排。",
              "沟通以后观察实际行动，再决定是否继续推进关系安排。")
          : List.of(
              "安排一次不被工作打断的相处或沟通，把下一步计划说具体。",
              "共同完成一件现实小事，看看时间、分工和回应能否配合。",
              "把未来一段时间的见面、分工或家庭安排确认下来。");
    };
    return variants.get(index % variants.size());
  }

  String changeCondition(
      OverallDimension dimension, OverallStance stance, OverallEvidenceAngle angle) {
    String reason = shortReason(angle.primaryKey());
    return switch (dimension) {
      case RHYTHM -> stance == OverallStance.PRESSURED
          ? "如果" + reason + "，同时睡眠或办事效率连续变差，就要继续减少安排。"
          : "如果" + reason + "，重点事情却没有进展，就要重新排优先顺序。";
      case CAREER -> stance == OverallStance.PRESSURED
          ? "如果" + reason + "，责任增加却没有相应权限和支持，就不要继续无条件承接。"
          : "如果" + reason + "，投入仍没有形成成果、授权或收入，就要调整工作方向。";
      case WEALTH -> stance == OverallStance.PRESSURED
          ? "如果" + reason + "，又连续出现回款延迟或预算超支，就要暂停大额安排。"
          : "如果" + reason + "，实际余钱却没有增加，就要重新检查支出和回款。";
      case RELATIONSHIP -> stance == OverallStance.PRESSURED
          ? "如果" + reason + "，同一分歧多次沟通仍没有变化，就要重新确认双方边界。"
          : "如果" + reason + "，口头承诺仍没有变成实际行动，就要放慢关系推进。";
    };
  }

  String continuedChangeCondition(OverallDimension dimension, OverallTransition transition) {
    return switch (dimension) {
      case RHYTHM -> switch (transition.relation()) {
        case "延续" -> "如果生活节奏的问题延续到" + transition.toYear()
            + "年，固定休息仍不能保留，就要继续减少安排。";
        case "加强" -> "如果生活节奏受到的影响继续加强，睡眠或办事效率也在变差，就要减少目标。";
        case "缓和" -> "即使生活节奏有所缓和，如果固定休息仍不能保留，也不要马上增加安排。";
        default -> throw unsupportedTransition(transition);
      };
      case CAREER -> switch (transition.relation()) {
        case "延续" -> "如果事业责任延续到" + transition.toYear()
            + "年，投入仍没有形成成果或明确条件，就要调整推进方向。";
        case "加强" -> "如果事业责任带来的影响继续加强，却没有相应支持和结果，就要缩小承担范围。";
        case "缓和" -> "即使事业压力有所缓和，如果成果仍没有落实，也不要马上增加新的责任。";
        default -> throw unsupportedTransition(transition);
      };
      case WEALTH -> switch (transition.relation()) {
        case "延续" -> "如果钱财安排延续到" + transition.toYear()
            + "年，备用金仍没有真正留下，就要暂停新增支出并重新核对固定开支和回款。";
        case "加强" -> "如果钱财压力继续加强，实际余钱还在减少，就要暂停大额支出并重新核对回款。";
        case "缓和" -> "即使钱财压力有所缓和，如果实际余钱没有增加，也不要马上扩大支出。";
        default -> throw unsupportedTransition(transition);
      };
      case RELATIONSHIP -> switch (transition.relation()) {
        case "延续" -> "如果关系问题延续到" + transition.toYear()
            + "年，沟通仍没有变成实际行动，就要重新确认彼此边界。";
        case "加强" -> "如果关系问题带来的影响继续加强，多次沟通仍没有变化，就要减少单方面投入。";
        case "缓和" -> "即使关系问题有所缓和，如果实际配合仍不稳定，也不要急着增加共同安排。";
        default -> throw unsupportedTransition(transition);
      };
    };
  }

  private IllegalArgumentException unsupportedTransition(OverallTransition transition) {
    return new IllegalArgumentException("unsupported continued overall transition: "
        + transition.relation());
  }

  String evidenceChange(String key) {
    if (key.startsWith("annual.branch.clash.")) return scope(key) + "的直接变动增多";
    if (key.startsWith("annual.branch.punishment.")) return scopeItems(key) + "更容易互相牵扯";
    if (key.startsWith("annual.branch.harm.")) return scope(key) + "更容易积累误解和消耗";
    if (key.startsWith("annual.branch.break.")) return scope(key) + "更容易被临时事情打断";
    if (key.startsWith("annual.branch.harmony.")) return scope(key) + "比之前更容易配合";
    if (key.contains("group.authority")) return "责任、规则和期限的影响更明显";
    if (key.contains("group.wealth")) return "收入、花费和现实安排更受关注";
    if (key.contains("group.output")) return "表达、交付和对外行动更集中";
    if (key.contains("group.peer")) return "协作、竞争和他人安排更受关注";
    if (key.contains("group.resource")) return "准备、学习和支持条件更受关注";
    return "现实负担出现变化";
  }

  private String driverSentence(OverallDimension dimension, String key) {
    if (key.startsWith("annual.branch.")) return relationDriver(dimension, key);
    if (key.contains("group.resource")) return switch (dimension) {
      case RHYTHM -> "准备、学习和整理会占用较多时间";
      case CAREER -> "工作更适合先补齐能力和条件，再推进重要任务";
      case WEALTH -> "钱更适合用在必要准备和长期需要上";
      case RELATIONSHIP -> "耐心沟通和实际照顾更容易得到回应";
    };
    if (key.contains("group.peer")) return switch (dimension) {
      case RHYTHM -> "他人的节奏和共同安排会影响自己的时间";
      case CAREER -> "合作和竞争会同时增加，需要把分工说清楚";
      case WEALTH -> "共同用钱、分成和人情支出需要算得更清楚";
      case RELATIONSHIP -> "相处机会会增加，但各自立场也会更明显";
    };
    if (key.contains("group.output")) return switch (dimension) {
      case RHYTHM -> "表达、交付和对外行动会让日程变得更满";
      case CAREER -> "想法更容易变成作品、方案或看得见的成果";
      case WEALTH -> "收入更依赖实际交付，成本也会跟着增加";
      case RELATIONSHIP -> "把真实想法说出来，更容易推动关系变化";
    };
    if (key.contains("group.wealth")) return switch (dimension) {
      case RHYTHM -> "收入、花费和现实事务会占用更多精力";
      case CAREER -> "工作成果需要进一步落实到回报和现实条件";
      case WEALTH -> "收入和资源安排会成为这一年的现实重点";
      case RELATIONSHIP -> "钱和现实分工会影响双方的相处感受";
    };
    if (key.contains("group.authority")) return switch (dimension) {
      case RHYTHM -> "责任、规则和期限会压缩可自由安排的时间";
      case CAREER -> "更容易接到责任明确、要求更高的工作";
      case WEALTH -> "固定责任和必要开支会影响钱的余量";
      case RELATIONSHIP -> "责任和现实安排会挤占相处与沟通时间";
    };
    if (key.endsWith("balance.weak")) return "自身承受余量偏少，事情一多就容易累积压力";
    if (key.endsWith("balance.strong")) return "自身推动事情的力量较足，但容易一次承担过多";
    return "自身状态大体平稳，关键在于安排是否有先后";
  }

  private String relationDriver(OverallDimension dimension, String key) {
    String area = scope(key);
    if (key.contains(".harmony.")) return switch (dimension) {
      case RHYTHM -> area + "较容易衔接，原有计划有机会顺着推进";
      case CAREER -> area + "较容易形成配合，工作推进阻力相对少";
      case WEALTH -> area + "较容易协调，收入和支出更便于安排";
      case RELATIONSHIP -> area + "更容易达成配合，沟通能落到实际安排";
    };
    if (key.contains(".clash.")) return switch (dimension) {
      case RHYTHM -> area + "容易出现直接变动，原定日程可能需要重排";
      case CAREER -> area + "容易出现正面调整，工作责任或方向需要重新确认";
      case WEALTH -> area + "容易出现金额或时间上的变动，钱不能安排得太满";
      case RELATIONSHIP -> area + "容易出现意见正面碰撞，回避只会推迟问题";
    };
    if (key.contains(".punishment.")) return switch (dimension) {
      case RHYTHM -> area + "容易互相牵扯，几件事会同时消耗精力";
      case CAREER -> area + "容易出现反复要求，做事前要先确认标准";
      case WEALTH -> area + "容易出现多笔支出互相挤压，需要提前留余量";
      case RELATIONSHIP -> area + "容易反复纠结同一件事，需要把边界说具体";
    };
    if (key.contains(".harm.")) return switch (dimension) {
      case RHYTHM -> area + "容易形成持续消耗，小问题拖久会影响状态";
      case CAREER -> area + "容易出现不易察觉的阻力，责任和支持要说清楚";
      case WEALTH -> area + "容易出现零散损耗，不能只看账面收入";
      case RELATIONSHIP -> area + "容易积累误解，不能等到情绪变重才沟通";
    };
    return switch (dimension) {
      case RHYTHM -> area + "容易被临时事情打断，需要给日程留出余地";
      case CAREER -> area + "原有分工容易被打断，需要准备替代安排";
      case WEALTH -> area + "原有收支计划容易被打断，备用金要留足";
      case RELATIONSHIP -> area + "原有相处安排容易被打断，需要重新确认时间";
    };
  }

  private String stanceOutcome(OverallDimension dimension, OverallStance stance) {
    return switch (dimension) {
      case RHYTHM -> switch (stance) {
        case SUPPORTIVE -> "整体还有余力，可以把时间集中到一个重点上";
        case BALANCED -> "生活变化不算大，按轻重安排就能保持稳定";
        case PRESSURED -> "不主动删减的话，休息和办事效率会先受影响";
        case MIXED -> "事情能推进，但必须给休息和临时变化留出空间";
      };
      case CAREER -> switch (stance) {
        case SUPPORTIVE -> "推进条件相对较好，关键是形成可确认的成果";
        case BALANCED -> "变化不算明显，把已有任务做好比频繁换方向更重要";
        case PRESSURED -> "责任和条件不说清，容易只增加工作量却没有结果";
        case MIXED -> "既有向前一步的空间，也要先确认责任、时间和回报";
      };
      case WEALTH -> switch (stance) {
        case SUPPORTIVE -> "安排条件相对有利，但仍要以实际到账和结余为准";
        case BALANCED -> "收支大体平稳，先守住日常余量，不必扩大开销";
        case PRESSURED -> "现金余量要留得更足，不能提前花掉还没到手的钱";
        case MIXED -> "收入和支出都可能增加，最后要看实际留下多少钱";
      };
      case RELATIONSHIP -> switch (stance) {
        case SUPPORTIVE -> "主动沟通更容易得到回应，可以把共同安排说具体";
        case BALANCED -> "关系变化不大，稳定相处比追求表面热闹更有用";
        case PRESSURED -> "回避问题会让同一分歧反复出现，需要尽早说清楚";
        case MIXED -> "既有支持也有分歧，判断要看实际相处和行动";
      };
    };
  }

  private String qualifier(String key) {
    if (key == null || key.isBlank()) return "。";
    if (key.startsWith("annual.branch.clash.")) return "，同时还要给" + scope(key) + "可能出现的变动留余地。";
    if (key.startsWith("annual.branch.punishment.")) return "，同时别让" + scopeItems(key) + "互相挤压。";
    if (key.startsWith("annual.branch.harm.")) return "，同时要及时处理" + scope(key) + "里积累的小问题。";
    if (key.startsWith("annual.branch.break.")) return "，同时要为" + scope(key) + "里的变化准备替代安排。";
    if (key.startsWith("annual.branch.harmony.")) return "，而" + scope(key) + "更容易得到配合。";
    if (key.contains("group.authority")) return "，同时要把责任和期限说清楚。";
    if (key.contains("group.wealth")) return "，同时要核对实际收入和花费。";
    if (key.contains("group.output")) return "，同时要把表达落实成完成结果。";
    if (key.contains("group.peer")) return "，同时要确认合作中的分工和边界。";
    if (key.contains("group.resource")) return "，同时要先补齐必要准备。";
    if (key.endsWith("balance.weak")) return "，自身余量不足时还要及时减量。";
    if (key.endsWith("balance.strong")) return "，也不要因为有余力就一次承担过多。";
    return "，同时按现实承受范围安排。";
  }

  private String shortReason(String key) {
    if (key.startsWith("annual.branch.clash.")) return scope(key) + "持续出现直接变化";
    if (key.startsWith("annual.branch.punishment.")) return scopeItems(key) + "容易互相牵扯";
    if (key.startsWith("annual.branch.harm.")) return scope(key) + "里的小问题持续积累";
    if (key.startsWith("annual.branch.break.")) return scope(key) + "容易被临时事情打断";
    if (key.startsWith("annual.branch.harmony.")) return scope(key) + "更容易得到配合";
    if (key.contains("group.authority")) return "责任和期限会变得更重要";
    if (key.contains("group.wealth")) return "收入和现实事务会更受关注";
    if (key.contains("group.output")) return "表达和交付会明显增多";
    if (key.contains("group.peer")) return "合作和他人安排会增多";
    if (key.contains("group.resource")) return "准备和学习会占用更多时间";
    if (key.endsWith("balance.weak")) return "自身承受余量偏少";
    if (key.endsWith("balance.strong")) return "自身容易一次承担过多";
    return "现实安排需要重新分轻重";
  }

  private String scope(String key) {
    if (key.endsWith(".dayun")) return "接下来一段时间的整体安排";
    if (key.endsWith(".day")) return "个人决定和亲近关系";
    if (key.endsWith(".month")) return "工作和家庭事务";
    if (key.endsWith(".time")) return "临时计划和后续安排";
    if (key.endsWith(".year")) return "原有环境和长期安排";
    return "现实安排";
  }

  private String scopeItems(String key) {
    if (key.endsWith(".dayun")) return "长期任务和整体安排";
    if (key.endsWith(".day")) return "个人事务和亲近关系";
    if (key.endsWith(".month")) return "工作和家庭事务";
    if (key.endsWith(".time")) return "临时计划和后续安排";
    if (key.endsWith(".year")) return "原有事务和长期安排";
    return "眼前的几件事";
  }

  private String stanceLead(OverallStance stance) {
    return switch (stance) {
      case SUPPORTIVE -> "条件较顺，";
      case BALANCED -> "变化不大，";
      case PRESSURED -> "压力偏重，";
      case MIXED -> "机会与牵制并存，";
    };
  }

  private List<String> headlineObjects(OverallDimension dimension) {
    return switch (dimension) {
      case RHYTHM -> List.of(
          "先把精力放在一件要紧事上",
          "每天的安排要给休息留位置",
          "先守住正常节奏，再增加目标");
      case CAREER -> List.of(
          "工作先看责任是否说清",
          "把成果做实，别让忙碌代替进展",
          "责任增加时，也要争取相应条件");
      case WEALTH -> List.of(
          "钱要按轻重安排，不急着增加支出",
          "先看实际到手，再决定钱怎么用",
          "收入与花费一起看，重点是留下余量");
      case RELATIONSHIP -> List.of(
          "把话说清楚，比一味迁就更有用",
          "关系要靠实际相处判断",
          "先处理反复出现的问题，再谈下一步");
    };
  }
}
