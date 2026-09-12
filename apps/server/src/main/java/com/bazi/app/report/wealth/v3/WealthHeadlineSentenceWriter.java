package com.bazi.app.report.wealth.v3;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Writes deterministic complete sentences from an explicit headline meaning. */
public final class WealthHeadlineSentenceWriter {

  public String write(WealthHeadlineSentenceSpec spec) {
    return write(spec, 0, 0);
  }

  public String write(WealthHeadlineSentenceSpec spec, String styleSeed) {
    return write(spec, styleSeed, 0);
  }

  public String write(WealthHeadlineSentenceSpec spec, String styleSeed, int yearSlot) {
    if (styleSeed == null || styleSeed.isBlank()) {
      throw new IllegalArgumentException("wealth headline style seed is required");
    }
    if (yearSlot < 0 || yearSlot > 4) {
      throw new IllegalArgumentException("wealth headline year slot must be between 0 and 4");
    }
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(styleSeed.getBytes(StandardCharsets.UTF_8));
      return write(spec,
          Math.floorMod(ByteBuffer.wrap(digest, 8, Integer.BYTES).getInt(), 16), yearSlot);
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  private String write(WealthHeadlineSentenceSpec spec, int variant, int yearSlot) {
    String judgment = subject(spec.subject(), variant % 4, yearSlot) + spec.judgment();
    if (spec.tone().equals("mixed")) {
      judgment += "，但" + spec.limitation();
    }
    String instruction = instructionLead(variant / 4, yearSlot) + spec.verification();
    String result = judgment + "。" + instruction + "。";
    new WealthChineseCopyPolicy().validateAnnualHeadline(result);
    return result;
  }

  private String subject(String subject, int variant, int yearSlot) {
    String withoutYear = subject.startsWith("今年") ? subject.substring("今年".length()) : subject;
    if (yearSlot == 0) {
      return switch (variant) {
        case 0 -> subject;
        case 1 -> "今年看，" + withoutYear;
        case 2 -> "今年来说，" + withoutYear;
        default -> "今年可见，" + withoutYear;
      };
    }
    if (yearSlot == 1) {
      return switch (variant) {
        case 0 -> "今年的重点是，" + withoutYear;
        case 1 -> "今年主要看到，" + withoutYear;
        case 2 -> "今年更值得看的是，" + withoutYear;
        default -> "今年值得注意的是，" + withoutYear;
      };
    }
    if (yearSlot == 2) {
      return switch (variant) {
        case 0 -> "今年要注意，" + withoutYear;
        case 1 -> "今年接着要看，" + withoutYear;
        case 2 -> "今年再看，" + withoutYear;
        default -> "今年往后看，" + withoutYear;
      };
    }
    if (yearSlot == 3) {
      return switch (variant) {
        case 0 -> "今年还要留意，" + withoutYear;
        case 1 -> "今年需要关注，" + withoutYear;
        case 2 -> "今年另一个表现是，" + withoutYear;
        default -> "今年再往后看，" + withoutYear;
      };
    }
    return switch (variant) {
      case 0 -> "今年最后要看的是，" + withoutYear;
      case 1 -> "今年往后看，" + withoutYear;
      case 2 -> "今年还可以看到，" + withoutYear;
      default -> "今年后面的情况是，" + withoutYear;
    };
  }

  private String instructionLead(int variant, int yearSlot) {
    if (yearSlot == 0) {
      return switch (variant) {
        case 0 -> "先";
        case 1 -> "先从记录开始：";
        case 2 -> "先把这点查清：";
        default -> "先完成核验：";
      };
    }
    if (yearSlot == 1) {
      return switch (variant) {
        case 0 -> "先从这里入手：";
        case 1 -> "先做好记录：";
        case 2 -> "先核实这件事：";
        default -> "先逐项确认：";
      };
    }
    if (yearSlot == 2) {
      return switch (variant) {
        case 0 -> "先检查一遍：";
        case 1 -> "先对照结果：";
        case 2 -> "先理清记录：";
        default -> "先把情况弄清：";
      };
    }
    if (yearSlot == 3) {
      return switch (variant) {
        case 0 -> "先列出明细：";
        case 1 -> "先核对现况：";
        case 2 -> "先查明变化：";
        default -> "先记下实情：";
      };
    }
    return switch (variant) {
      case 0 -> "先从账面查起：";
      case 1 -> "先确认最终数目：";
      case 2 -> "先梳理已有信息：";
      default -> "先按结果判断：";
    };
  }
}
