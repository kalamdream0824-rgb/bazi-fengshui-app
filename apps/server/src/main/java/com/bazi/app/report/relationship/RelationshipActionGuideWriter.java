package com.bazi.app.report.relationship;

import com.bazi.app.report.AnnualActionGuide;
import java.util.List;
import java.util.Locale;

/** Turns a calculated relationship focus into a status-specific, observable action loop. */
final class RelationshipActionGuideWriter {

  AnnualActionGuide write(
      RelationshipStatus status,
      RelationshipPeriodEvaluation.Year year,
      int yearIndex,
      String action,
      String fallbackAction,
      List<String> evidenceKeys) {
    RelationshipDimension dimension = year.focus().primaryDimension();
    RelationshipTone tone = year.dimensions().get(dimension).tone();
    String focusKey = "relationship." + status.code() + "." + dimension.code() + "."
        + tone.name().toLowerCase(Locale.ROOT);
    return new AnnualActionGuide(
        focusKey,
        yearLine(year.year(), yearIndex, problem(status, dimension, tone)),
        yearLine(year.year(), yearIndex, complete(action)),
        yearLine(year.year(), yearIndex, expectedChange(status, dimension, tone)),
        checkTiming(status, year.year(), yearIndex),
        yearLine(year.year(), yearIndex, successSignal(status, dimension)),
        yearLine(year.year(), yearIndex, adjustmentCondition(status, dimension, tone)),
        yearLine(year.year(), yearIndex, complete(fallbackAction)),
        evidenceKeys);
  }

  private String problem(
      RelationshipStatus status, RelationshipDimension dimension, RelationshipTone tone) {
    if (tone == RelationshipTone.QUIET) {
      return switch (status) {
        case SINGLE -> "今年这方面变化不明显，不必为了进入关系勉强自己。";
        case DATING -> "今年这方面变化不明显，不必把普通波动当成关系转折。";
        case MARRIED -> "今年这方面变化不明显，先维持共同生活的正常节奏。";
      };
    }
    return switch (status) {
      case SINGLE -> switch (dimension) {
        case CONNECTION -> "现在要看清认识新人的机会能否变成真实接触。";
        case RESPONSE -> "现在要看清新的联系能否得到认真回应。";
        case DAILY_COOPERATION -> "现在要看清线上交流能否变成实际见面。";
        case BOUNDARIES -> "现在要在了解别人时守住自己的相处边界。";
        case STABILITY -> "现在要看清新的联系能否稳定延续。";
      };
      case DATING -> switch (dimension) {
        case CONNECTION -> "两个人需要把联系和见面的期待说清楚。";
        case RESPONSE -> "两个人有些真实想法还没有认真说清。";
        case DAILY_COOPERATION -> "两个人的见面和日常安排需要更好地配合。";
        case BOUNDARIES -> "两个人出现分歧时，需要先守住彼此边界。";
        case STABILITY -> "两个人需要把能否继续走下去落到实际安排。";
      };
      case MARRIED -> switch (dimension) {
        case CONNECTION -> "共同生活容易只剩事务，需要重新留出相处时间。";
        case RESPONSE -> "共同生活里有些不满和需要还没有说清。";
        case DAILY_COOPERATION -> "家务、时间或固定开支需要重新分配。";
        case BOUNDARIES -> "夫妻出现分歧时，需要停止伤害关系的做法。";
        case STABILITY -> "共同生活中的重要决定需要排出先后。";
      };
    };
  }

  private String expectedChange(
      RelationshipStatus status, RelationshipDimension dimension, RelationshipTone tone) {
    String base = switch (status) {
      case SINGLE -> switch (dimension) {
        case CONNECTION -> "这样做能把泛泛的社交变成一次可以判断的真实接触。";
        case RESPONSE -> "这样做能看清对方是否愿意认真回应，减少单方面猜测。";
        case DAILY_COOPERATION -> "这样做能确认双方是否愿意为一次见面付出实际行动。";
        case BOUNDARIES -> "这样做能尽早看清对方是否尊重你的真实想法。";
        case STABILITY -> "这样做能判断新的联系是短暂热情还是可以继续了解。";
      };
      case DATING -> switch (dimension) {
        case CONNECTION -> "这样做能让联系和见面更稳定，减少一方独自维持关系。";
        case RESPONSE -> "这样做能减少互相猜测，让两个人知道问题究竟在哪里。";
        case DAILY_COOPERATION -> "这样做能让约会和日常安排更公平，减少临时迁就。";
        case BOUNDARIES -> "这样做能让分歧停在具体事情上，不继续伤害彼此。";
        case STABILITY -> "这样做能确认两个人对未来的想法是否真的一致。";
      };
      case MARRIED -> switch (dimension) {
        case CONNECTION -> "这样做能让共同生活保留相处感，不只剩下要完成的事务。";
        case RESPONSE -> "这样做能让不满和需要被听见，减少冷淡或反复争吵。";
        case DAILY_COOPERATION -> "这样做能让家务、时间和开支分配更清楚，减少长期疲惫。";
        case BOUNDARIES -> "这样做能减少冷战、翻旧账或互相指责带来的伤害。";
        case STABILITY -> "这样做能让重要决定更可执行，避免一方独自承担。";
      };
    };
    if (tone == RelationshipTone.PRESSURED) {
      return base.replace("这样做能", "先这样做，能");
    }
    return base;
  }

  private String successSignal(RelationshipStatus status, RelationshipDimension dimension) {
    return switch (status) {
      case SINGLE -> switch (dimension) {
        case CONNECTION -> "有效的信号是：出现一次双方都愿意参加的真实见面。";
        case RESPONSE -> "有效的信号是：提出具体问题后，对方给出清楚回答。";
        case DAILY_COOPERATION -> "有效的信号是：双方共同确定时间，并按约完成见面。";
        case BOUNDARIES -> "有效的信号是：你说出不舒服后，对方愿意尊重并调整。";
        case STABILITY -> "有效的信号是：答应的小事连续做到，联系没有只靠你维持。";
      };
      case DATING -> switch (dimension) {
        case CONNECTION -> "有效的信号是：联系和见面的频率由两个人共同维持。";
        case RESPONSE -> "有效的信号是：说出真实想法后，问题得到具体回应。";
        case DAILY_COOPERATION -> "有效的信号是：约会和日常安排不再总由同一个人迁就。";
        case BOUNDARIES -> "有效的信号是：有分歧时双方仍能停止指责并讨论事情。";
        case STABILITY -> "有效的信号是：双方对下一步给出一致且能做到的安排。";
      };
      case MARRIED -> switch (dimension) {
        case CONNECTION -> "有效的信号是：夫妻能固定留出不处理事务的相处时间。";
        case RESPONSE -> "有效的信号是：一方说出需要后，另一方有明确回应和行动。";
        case DAILY_COOPERATION -> "有效的信号是：共同事务重新分配后，双方都能持续做到。";
        case BOUNDARIES -> "有效的信号是：争执时不再冷战或翻旧账，问题能够当天收住。";
        case STABILITY -> "有效的信号是：重要决定写清时间和分工，并由夫妻共同推进。";
      };
    };
  }

  private String adjustmentCondition(
      RelationshipStatus status, RelationshipDimension dimension, RelationshipTone tone) {
    if (tone == RelationshipTone.PRESSURED) {
      return status == RelationshipStatus.SINGLE
          ? "如果连续两次表达后仍只有你投入，就不要继续增加付出。"
          : "如果连续两次沟通仍只有一方改变，就要重新确认彼此底线。";
    }
    if (tone == RelationshipTone.QUIET) {
      return "如果现实里一直没有对应变化，就维持原来的判断标准。";
    }
    return switch (dimension) {
      case CONNECTION -> "如果四周后仍只有计划没有行动，就减少没有结果的联系。";
      case RESPONSE -> "如果两个重要问题都没有得到回答，就不要再靠猜测维持。";
      case DAILY_COOPERATION -> "如果两次具体安排都无法共同做到，就重新商量相处方式。";
      case BOUNDARIES -> "如果同一边界再次被忽视，就停止迁就并拉开距离。";
      case STABILITY -> "如果约定连续两次没有做到，就先停止增加新的承诺。";
    };
  }

  private String checkTiming(RelationshipStatus status, int year, int yearIndex) {
    if (status == RelationshipStatus.SINGLE) {
      return switch (yearIndex) {
        case 0 -> "接下来四周，每周核对一次实际联系和对方回应。";
        case 1 -> "进入" + year + "年后，每两周核对一次实际联系；六周后统一判断。";
        default -> "进入" + year + "年后，每月核对一次实际回应；两个月后统一判断。";
      };
    }
    return switch (yearIndex) {
      case 0 -> "接下来四周，每周核对一次双方是否都有实际行动。";
      case 1 -> "进入" + year + "年后，每两周核对一次相处变化；六周后统一判断。";
      default -> "进入" + year + "年后，每月核对一次共同安排；两个月后统一判断。";
    };
  }

  private String yearLine(int year, int yearIndex, String text) {
    if (yearIndex == 0) return text;
    return year + "年，" + text;
  }

  private String complete(String text) {
    return text.matches(".*[。！？]$") ? text : text + "。";
  }
}
