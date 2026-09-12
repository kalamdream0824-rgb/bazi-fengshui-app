package com.bazi.app.report.wealth.v3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Renders one complete, concrete claim without inferring occupation or income source. */
public final class WealthSemanticClaimWriter {

  public String conclusion(WealthSemanticClaim claim) {
    return conclusion(claim, claim.semanticKey());
  }

  public String conclusion(WealthSemanticClaim claim, String styleSeed) {
    int variant = variant(claim, styleSeed, "conclusion");
    String subject = conciseSubject(claim.path());
    return switch (claim.stance()) {
      case "supportive" -> supportive(subject, claim.path(), claim.strength(), variant);
      case "mixed" -> mixed(subject, claim.path(), variant);
      case "restricted" -> restricted(subject, claim.path(), variant);
      case "quiet" -> "目前不能只凭命盘判断" + subject + "会怎样变化";
      default -> throw new IllegalArgumentException("unknown wealth claim stance");
    };
  }

  public String check(WealthSemanticClaim claim) {
    return check(claim, claim.semanticKey());
  }

  public String check(WealthSemanticClaim claim, String styleSeed) {
    String verification = verification(claim, variant(claim, styleSeed, "check"));
    return verification + evidenceTail(claim);
  }

  String briefCheck(WealthSemanticClaim claim, String styleSeed) {
    return verification(claim, variant(claim, styleSeed, "check"));
  }

  public String presentHeadline(WealthSemanticClaim claim) {
    return presentHeadline(claim, claim.semanticKey());
  }

  public String presentHeadline(WealthSemanticClaim claim, String styleSeed) {
    return headlinePrefix(variant(claim, styleSeed, "present-headline", 32), "今年")
        + claim.objectText();
  }

  public String futureHeadline(WealthSemanticClaim claim, String styleSeed) {
    return headlinePrefix(variant(claim, styleSeed, "future-headline", 32), claim.year() + "年")
        + claim.objectText();
  }

  public String action(WealthSemanticClaim claim) {
    return action(claim, 0, claim.semanticKey());
  }

  public String action(WealthSemanticClaim claim, int stepIndex) {
    return action(claim, stepIndex, claim.semanticKey());
  }

  public String action(WealthSemanticClaim claim, int stepIndex, String styleSeed) {
    String verification = verification(claim,
        variant(claim, styleSeed + "\u0000" + stepIndex, "action", 16));
    String next = switch (claim.path()) {
      case "stable_income" -> stepIndex == 0
          ? "确认连续性后，再决定是否增加固定支出"
          : "连续核对后，再按实际进账调整固定支出";
      case "skill_income" -> stepIndex == 0
          ? "只保留确实带来实际进账的部分"
          : "减少只增加时间和花费、没有带来进账的部分";
      case "project_income" -> stepIndex == 0
          ? "只使用已经实际收到的钱安排支出"
          : "连续核对后，只保留能按约定收到的安排";
      case "cooperation_income" -> stepIndex == 0
          ? "超出事前约定的支出先暂停"
          : "发生共同开销后，再核对是否超出约定";
      case "retention" -> stepIndex == 0
          ? "实际结余没有增加时，不提高固定花费"
          : "连续核对后，再按实际结余安排新增支出";
      default -> throw new IllegalArgumentException("unknown wealth claim path");
    };
    return verification + "，" + next + "。";
  }

  private String headlinePrefix(int variant, String yearText) {
    return switch (variant) {
      case 0 -> yearText + "先核对";
      case 1 -> yearText + "重点核对";
      case 2 -> yearText + "先看清";
      case 3 -> yearText + "先记录";
      case 4 -> yearText + "先确认";
      case 5 -> yearText + "要核对";
      case 6 -> yearText + "先记清";
      case 7 -> yearText + "先对照";
      case 8 -> yearText + "先查清";
      case 9 -> yearText + "先理清";
      case 10 -> yearText + "先核实";
      case 11 -> yearText + "着重核对";
      case 12 -> yearText + "优先核对";
      case 13 -> yearText + "先盘点";
      case 14 -> yearText + "先比较";
      case 15 -> yearText + "先弄清";
      case 16 -> yearText + "先查对";
      case 17 -> yearText + "先梳理";
      case 18 -> yearText + "先看明白";
      case 19 -> yearText + "先确认清楚";
      case 20 -> yearText + "先列清";
      case 21 -> yearText + "先核验";
      case 22 -> yearText + "先逐项核对";
      case 23 -> yearText + "先逐项确认";
      case 24 -> yearText + "优先确认";
      case 25 -> yearText + "着重确认";
      case 26 -> yearText + "重新核对";
      case 27 -> yearText + "认真核对";
      case 28 -> yearText + "先按实际结果核对";
      case 29 -> yearText + "先按记录核对";
      case 30 -> yearText + "先细看";
      default -> yearText + "先弄明白";
    };
  }

  private String supportive(String subject, String path, String strength, int variant) {
    String direction = switch (path) {
      case "stable_income" -> "保持连续";
      case "skill_income" -> "转成实际进账";
      case "project_income" -> "按原定时间收到";
      case "cooperation_income" -> "按事前约定完成分配";
      case "retention" -> "比之前增加";
      default -> throw new IllegalArgumentException("unknown wealth claim path");
    };
    String condition = conditional(subject, path);
    return switch (strength) {
      case "pronounced" -> switch (variant) {
        case 0 -> condition + direction + "的可能性较高";
        case 1 -> condition + "更可能" + direction;
        case 2 -> subject + direction + "的机会较明显";
        case 3 -> subject + "较有机会" + direction;
        case 4 -> subject + "更可能" + direction;
        case 5 -> "目前看，" + subject + "更可能" + direction;
        case 6 -> condition + direction + "的把握相对更大";
        default -> subject + "更有可能" + direction;
      };
      case "supported" -> switch (variant) {
        case 0 -> condition + direction + "有一定可能";
        case 1 -> condition + "可能会" + direction;
        case 2 -> subject + "有机会" + direction;
        case 3 -> subject + "有机会" + direction + "，但还不能提前当作结果";
        case 4 -> subject + direction + "有一定可能";
        case 5 -> "目前看，" + subject + "可能" + direction;
        case 6 -> condition + "有可能逐步" + direction;
        default -> subject + "也可能" + direction;
      };
      case "limited" -> switch (variant) {
        case 0 -> condition + direction + "的迹象较弱，不能提前据此安排支出";
        case 1 -> condition + "暂时只有少量" + direction + "的迹象，不能提前花这笔钱";
        case 2 -> subject + "能否" + direction + "仍不确定，要等实际记录确认";
        case 3 -> subject + direction + "的条件还不充分，不宜提前安排支出";
        case 4 -> "目前还不能确定" + subject + "会" + direction + "，先不要据此增加花费";
        case 5 -> "目前只看到少量" + subject + direction + "的迹象，仍需保留余地";
        case 6 -> condition + "即使出现" + direction + "的迹象，也要先核对实际结果";
        default -> subject + direction + "暂时只能作为待核对的变化";
      };
      default -> throw new IllegalArgumentException("unknown wealth claim strength");
    };
  }

  private String mixed(String subject, String path, int variant) {
    String[] outcomes = switch (path) {
      case "stable_income" -> new String[] {"保持连续", "中途发生变化"};
      case "skill_income" -> new String[] {"转成实际进账", "只增加时间和花费"};
      case "project_income" -> new String[] {"按期收到", "延期或金额发生变化"};
      case "cooperation_income" -> new String[] {"按约定处理", "增加额外责任和开销"};
      case "retention" -> new String[] {"实际留下的钱增加", "新增花费压低结余"};
      default -> throw new IllegalArgumentException("unknown wealth claim path");
    };
    String condition = conditional(subject, path);
    return switch (variant) {
      case 0 -> condition + "可能" + outcomes[0] + "，也可能" + outcomes[1];
      case 1 -> condition + "结果可能偏向" + outcomes[0] + "，也可能" + outcomes[1];
      case 2 -> subject + "有机会" + outcomes[0] + "，但也可能" + outcomes[1];
      case 3 -> subject + "可能" + outcomes[0] + "，也可能" + outcomes[1];
      case 4 -> condition + "既可能" + outcomes[0] + "，也可能" + outcomes[1];
      case 5 -> condition + "结果有两种可能：" + outcomes[0] + "，或者" + outcomes[1];
      case 6 -> subject + "接下来可能" + outcomes[0] + "，也可能" + outcomes[1];
      default -> condition + outcomes[0] + "与" + outcomes[1] + "都要考虑";
    };
  }

  private String restricted(String subject, String path, int variant) {
    String[] outcomeAndAdvice = switch (path) {
      case "stable_income" -> new String[] {"更容易中断", "长期支出不宜提前加重"};
      case "skill_income" -> new String[] {"未必能同步转成实际进账", "不要提前把投入当成收入"};
      case "project_income" -> new String[] {"更容易延期", "不能提前当作已经到账"};
      case "cooperation_income" -> new String[] {"更容易增加额外责任和开销", "先写清各自承担的范围"};
      case "retention" -> new String[] {"更容易被增加的支出压低", "新增花费要先看实际结余"};
      default -> throw new IllegalArgumentException("unknown wealth claim path");
    };
    String risk = switch (path) {
      case "stable_income" -> "进账中断";
      case "skill_income" -> "投入没有带来进账";
      case "project_income" -> "到账延期";
      case "cooperation_income" -> "额外责任和开销增加";
      case "retention" -> "支出压低结余";
      default -> throw new IllegalArgumentException("unknown wealth claim path");
    };
    String condition = conditional(subject, path);
    return switch (variant) {
      case 0 -> condition + outcomeAndAdvice[0] + "，" + outcomeAndAdvice[1];
      case 1 -> condition + outcomeAndAdvice[0] + "；" + outcomeAndAdvice[1];
      case 2 -> subject + "目前受到的限制较明显，" + outcomeAndAdvice[1];
      case 3 -> condition + "先防范" + risk + "，" + outcomeAndAdvice[1];
      case 4 -> subject + "不能按最顺利的结果预估，" + outcomeAndAdvice[1];
      case 5 -> subject + "暂时不宜按理想结果预估，" + outcomeAndAdvice[1];
      case 6 -> condition + outcomeAndAdvice[0] + "，所以" + outcomeAndAdvice[1];
      default -> "当前要先防范" + risk + "，" + outcomeAndAdvice[1];
    };
  }

  private String verification(WealthSemanticClaim claim, int variant) {
    String object = claim.objectText();
    return switch (variant) {
      case 0 -> WealthHeadlineVocabulary.entry(claim.themeKey()).sentenceSpec().verification();
      case 1 -> object.endsWith("记录") ? "整理" + object : "记录" + object;
      case 2 -> "核对" + object;
      case 3 -> "写清" + object;
      case 4 -> "按顺序记下" + object;
      case 5 -> "另记" + object;
      case 6 -> "用真实记录核对" + object;
      case 7 -> "把" + object + "和最终结果作比较";
      case 8 -> "查清" + object;
      case 9 -> "确认" + object;
      case 10 -> "比较" + object;
      case 11 -> "盘点" + object;
      case 12 -> object.endsWith("记录") ? "逐项整理" + object : "逐项记录" + object;
      case 13 -> "按实际结果核对" + object;
      case 14 -> "根据现有记录查清" + object;
      default -> "把" + object + "记清";
    };
  }

  private int variant(WealthSemanticClaim claim, String styleSeed, String channel) {
    return variant(claim, styleSeed, channel, 8);
  }

  private int variant(WealthSemanticClaim claim, String styleSeed, String channel, int bound) {
    if (styleSeed == null || styleSeed.isBlank()) {
      throw new IllegalArgumentException("wealth copy style seed is required");
    }
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(
          (claim.semanticKey() + "\u0000" + styleSeed + "\u0000" + channel)
              .getBytes(StandardCharsets.UTF_8));
      return Math.floorMod(java.nio.ByteBuffer.wrap(digest, 8, Integer.BYTES).getInt(), bound);
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  private String conditional(String subject, String path) {
    return switch (path) {
      case "stable_income" -> "已有" + subject + "时，";
      case "skill_income" -> "新增时间或资金后，";
      case "project_income" -> "如果有" + subject + "，";
      case "cooperation_income" -> "涉及" + subject + "时，";
      default -> subject + "，";
    };
  }

  private String conciseSubject(String path) {
    return switch (path) {
      case "stable_income" -> "持续进账";
      case "skill_income" -> "新增投入";
      case "project_income" -> "已约定的待到账款项";
      case "cooperation_income" -> "共同资金安排";
      case "retention" -> "实际结余";
      default -> throw new IllegalArgumentException("unknown wealth claim path");
    };
  }

  private String evidenceTail(WealthSemanticClaim claim) {
    return switch (claim.evidenceAngle()) {
      case "annual_direct" -> annualDirectTail(claim);
      case "annual_relation" -> "，同时检查既定安排是否改变了"
          + changeSubject(claim.path());
      case "long_term_context" -> "，对照前后阶段的" + contextSubject(claim.path());
      case "input_to_income" -> "，确认投入是否带来进账";
      case "income_to_retention" -> "，确认进账后实际留下多少";
      case "shared_money" -> "，写清与他人共同承担的金额和责任";
      case "income_capacity" -> "，核对现有收支能够承受多大变化";
      case "usual_pattern", "combined_pattern" -> "，对照以往的" + contextSubject(claim.path());
      default -> throw new IllegalArgumentException("unknown wealth claim evidence angle");
    };
  }

  private String annualDirectTail(WealthSemanticClaim claim) {
    return switch (claim.evidenceDetailKey()) {
      case "direct_income" -> "，以实际到账记录为准";
      case "input_return" -> "，确认投入是否形成进账";
      case "responsibility_limit" -> "，核对相关责任或限制是否增加";
      case "shared_competition" -> "，核对共同分配是否顺畅";
      case "preparation_support" -> "，核对事前准备是否充分";
      default -> "，以实际记录为准";
    };
  }

  private String changeSubject(String path) {
    return switch (path) {
      case "stable_income" -> "进账连续性";
      case "skill_income" -> "投入后的实际进账";
      case "project_income" -> "实际到账时间";
      case "cooperation_income" -> "共同承担的金额和责任";
      case "retention" -> "最终结余";
      default -> throw new IllegalArgumentException("unknown wealth claim path");
    };
  }

  private String contextSubject(String path) {
    return switch (path) {
      case "stable_income" -> "进账连续性";
      case "skill_income" -> "投入与进账关系";
      case "project_income" -> "到账时间和金额";
      case "cooperation_income" -> "共同资金约定";
      case "retention" -> "实际收支和结余";
      default -> throw new IllegalArgumentException("unknown wealth claim path");
    };
  }
}
