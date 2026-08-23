package com.bazi.app.report;

import com.bazi.app.config.BusinessException;
import java.util.Locale;
import java.util.Objects;

public record CareerContext(
    Status status,
    Goal goal,
    Pace pace) {

  public CareerContext {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(goal, "goal");
    Objects.requireNonNull(pace, "pace");
  }

  public static CareerContext fromCodes(String status, String goal, String pace) {
    try {
      return new CareerContext(
          Status.valueOf(normalize(status)),
          Goal.valueOf(normalize(goal)),
          Pace.valueOf(normalize(pace)));
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new BusinessException("CAREER_CONTEXT_INVALID", "事业状态问卷包含无效选项");
    }
  }

  private static String normalize(String value) {
    return Objects.requireNonNull(value).trim().toUpperCase(Locale.ROOT);
  }

  public enum Status {
    EMPLOYED("在职"),
    SELF_EMPLOYED("自营或创业"),
    JOB_SEEKING("求职中"),
    STUDYING("学习或准备入行");

    private final String label;

    Status(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  public enum Goal {
    PROMOTION("希望升职"),
    JOB_CHANGE("准备换工作"),
    STABILITY("希望工作稳定"),
    TRANSITION("准备转换方向");

    private final String label;

    Goal(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  public enum Pace {
    SMOOTH("目前进展顺利"),
    STALLED("目前进展不顺"),
    HIGH_PRESSURE("目前压力较大"),
    PREPARING_CHANGE("正在为改变做准备");

    private final String label;

    Pace(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }
}
