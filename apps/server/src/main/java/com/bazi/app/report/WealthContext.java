package com.bazi.app.report;

import com.bazi.app.config.BusinessException;
import java.util.Locale;
import java.util.Objects;

public record WealthContext(
    IncomeSource incomeSource,
    Goal goal,
    Pace pace) {

  public WealthContext {
    Objects.requireNonNull(incomeSource, "incomeSource");
    Objects.requireNonNull(goal, "goal");
    Objects.requireNonNull(pace, "pace");
  }

  public static WealthContext fromCodes(String incomeSource, String goal, String pace) {
    try {
      return new WealthContext(
          IncomeSource.valueOf(normalize(incomeSource)),
          Goal.valueOf(normalize(goal)),
          Pace.valueOf(normalize(pace)));
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new BusinessException("WEALTH_CONTEXT_INVALID", "财富状态问卷包含无效选项");
    }
  }

  private static String normalize(String value) {
    return Objects.requireNonNull(value).trim().toUpperCase(Locale.ROOT);
  }

  public enum IncomeSource {
    SALARY("固定工资为主"),
    SELF_EMPLOYED("自营或项目收入为主"),
    MIXED("工资和副业都有"),
    UNSTABLE("暂时没有稳定收入");

    private final String label;

    IncomeSource(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  public enum Goal {
    INCREASE_INCOME("希望增加收入"),
    STABILIZE_CASHFLOW("希望稳定收支"),
    REDUCE_PRESSURE("希望减轻支出压力"),
    NEW_INCOME_SOURCE("准备尝试新的收入来源");

    private final String label;

    Goal(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  public enum Pace {
    STABLE("目前收支稳定"),
    INCOME_FLUCTUATING("近期收入有波动"),
    SPENDING_PRESSURE("近期支出压力较大"),
    PREPARING_ADJUSTMENT("正在准备调整收支");

    private final String label;

    Pace(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }
}
