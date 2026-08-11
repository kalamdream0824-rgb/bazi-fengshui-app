package com.bazi.app.domain.constants;

import com.bazi.app.config.BusinessException;
import java.util.Map;

/** 会员套餐常量：代码 / 时长 / 金额（分）。金额为业务侧示例定价，正式定价待定。 */
public final class MemberPlans {

  public static final String MEMBER_1M = "member_1m";
  public static final String MEMBER_3M = "member_3m";

  private static final Map<String, MemberPlan> PLANS = Map.of(
      MEMBER_1M, new MemberPlan(MEMBER_1M, 30, 2990), // ¥29.9
      MEMBER_3M, new MemberPlan(MEMBER_3M, 90, 6800) // ¥68
  );

  private MemberPlans() {}

  public static MemberPlan of(String code) {
    MemberPlan plan = PLANS.get(code);
    if (plan == null) {
      throw new BusinessException("PLAN_INVALID", "套餐不存在");
    }
    return plan;
  }

  public record MemberPlan(String code, int days, int amountCents) {}
}
