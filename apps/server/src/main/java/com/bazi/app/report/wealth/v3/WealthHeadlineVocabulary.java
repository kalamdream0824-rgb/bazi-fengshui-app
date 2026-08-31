package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.WealthPath;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Closed semantic and copy catalog for deterministic annual wealth headlines. */
public final class WealthHeadlineVocabulary {
  public static final String VERSION = "wealth-headline-v1";

  private static final List<Entry> ENTRIES = List.of(
      entry(10, "stable_receipt_support", "stable_income", "stable_receipt", "support",
          "monthly_receipt", "每月到账", "stable_monthly_receipt", "supportive",
          "固定收入更有支撑，先确认每月到账是否稳定。"),
      entry(11, "stable_continuity_support", "stable_income", "stable_continuity", "support",
          "income_interruption", "收入中断的可能", "stable_income_interruption", "supportive",
          "收入能否持续值得关注，也要确认收入中断的可能。"),
      entry(12, "stable_coverage_support", "stable_income", "stable_coverage", "support",
          "living_expense_coverage", "日常开支", "stable_living_expense_coverage", "supportive",
          "先看这笔钱能覆盖多少日常开支，再安排新增花费。"),
      entry(20, "stable_source_balance", "stable_income", "stable_dependency", "balance",
          "single_source", "单一来源", "stable_single_source", "mixed",
          "固定收入虽有支撑，也要防止过度依赖单一来源。"),
      entry(21, "stable_buffer_balance", "stable_income", "stable_buffer", "balance",
          "income_buffer", "收入缓冲", "stable_income_buffer", "mixed",
          "进账有支撑也有变数，先留出收入缓冲。"),
      entry(22, "stable_client_balance", "stable_income", "stable_client", "balance",
          "main_client", "主要客户", "stable_main_client", "mixed",
          "来源能否持续仍有变数，先确认是否过度依赖主要客户。"),
      entry(30, "stable_expense_control", "stable_income", "stable_expense", "control",
          "new_fixed_expense", "新增固定开支", "stable_fixed_expense", "restricted",
          "固定收入存在变数，新增固定开支要更谨慎。"),
      entry(31, "stable_commitment_control", "stable_income", "stable_commitment", "control",
          "long_term_spending", "长期支出承诺", "stable_long_term_spending", "restricted",
          "进账尚有变数，暂缓增加长期支出承诺。"),
      entry(32, "stable_reserve_control", "stable_income", "stable_reserve", "control",
          "reserve_fund", "备用资金", "stable_reserve_fund", "restricted",
          "收入不够稳时，先准备备用资金。"),

      entry(40, "skill_repeat_payment", "skill_income", "skill_demand", "support",
          "repeat_payment", "重复付费", "skill_repeat_payment", "supportive",
          "靠手艺或服务获得收入的条件更清楚，可以先看重复付费。"),
      entry(41, "skill_pricing_support", "skill_income", "skill_pricing", "support",
          "service_pricing", "服务定价", "skill_service_pricing", "supportive",
          "有人愿意为本事付钱时，先把服务定价说清楚。"),
      entry(42, "skill_returning_customer", "skill_income", "skill_customer", "support",
          "returning_customer", "老客户再次购买", "skill_returning_customer", "supportive",
          "这类收入可以关注，重点核对老客户再次购买。"),
      entry(50, "skill_time_balance", "skill_income", "skill_time", "balance",
          "time_cost", "投入的时间", "skill_time_cost", "mixed",
          "靠本事赚钱有机会，但要先算清投入的时间。"),
      entry(51, "skill_capacity_balance", "skill_income", "skill_capacity", "balance",
          "available_time", "可投入时间", "skill_available_time", "mixed",
          "服务需求和时间压力同时存在，先算清可投入时间。"),
      entry(52, "skill_margin_balance", "skill_income", "skill_margin", "balance",
          "service_margin", "实际结余", "skill_service_margin", "mixed",
          "增加服务可能带来进账，也要核对扣除成本后的实际结余。"),
      entry(60, "skill_service_cost", "skill_income", "skill_cost", "control",
          "service_cost", "时间和成本", "skill_service_cost", "restricted",
          "增加服务未必增加结余，先控制时间和成本。"),
      entry(61, "skill_workload_control", "skill_income", "skill_workload", "control",
          "workload_limit", "接活数量", "skill_workload_limit", "restricted",
          "时间投入受到限制，先控制接活数量。"),
      entry(62, "skill_equipment_control", "skill_income", "skill_equipment", "control",
          "equipment_spending", "新增设备开支", "skill_equipment_spending", "restricted",
          "靠本事增收仍有限制，暂缓扩大新增设备开支。"),

      entry(70, "project_payment_timing", "project_income", "project_terms", "support",
          "payment_timing", "付款时间", "project_payment_timing", "supportive",
          "额外项目有进账机会，先确认付款时间。"),
      entry(71, "project_actual_receipt", "project_income", "project_receipt", "support",
          "actual_receipt", "实际到账", "project_actual_receipt", "supportive",
          "有项目可做时，先核对实际到账。"),
      entry(72, "project_net_receipt", "project_income", "project_net", "support",
          "net_project_income", "扣除成本后的结余", "project_net_receipt", "supportive",
          "接单金额之外，还要看扣除成本后的结余。"),
      entry(80, "project_small_trial", "project_income", "project_cost", "balance",
          "small_investment", "小额投入", "project_small_trial", "mixed",
          "项目机会和限制同时出现，先用小额投入验证。"),
      entry(81, "project_terms_balance", "project_income", "project_collection", "balance",
          "collection_terms", "收款条件", "project_collection_terms", "mixed",
          "有接单空间也有不确定性，先写清收款条件。"),
      entry(82, "project_budget_balance", "project_income", "project_budget", "balance",
          "project_cost_ceiling", "成本上限", "project_cost_ceiling", "mixed",
          "报价和支出需要一起看，先设定成本上限。"),
      entry(90, "project_pending_cash", "project_income", "project_advance", "control",
          "pending_cash", "尚未到账的钱", "project_pending_cash", "restricted",
          "接额外项目前先控制投入，别预支尚未到账的钱。"),
      entry(91, "project_deposit_control", "project_income", "project_deposit", "control",
          "deposit_ratio", "预付款比例", "project_deposit_ratio", "restricted",
          "收款存在限制时，先约定预付款比例。"),
      entry(92, "project_scope_control", "project_income", "project_scope", "control",
          "added_requirements", "追加要求", "project_added_requirements", "restricted",
          "接单前先写清追加要求，避免成本继续增加。"),

      entry(100, "cooperation_profit_split", "cooperation_income", "cooperation_split", "support",
          "profit_split", "分账方式", "cooperation_profit_split", "supportive",
          "合作进账有机会，先把分账方式说清楚。"),
      entry(101, "cooperation_agreement_support", "cooperation_income", "cooperation_agreement", "support",
          "cooperation_agreement", "合作约定", "cooperation_agreement_support", "supportive",
          "一起做事可能带来收入，先确认合作约定。"),
      entry(102, "cooperation_actual_split", "cooperation_income", "cooperation_actual", "support",
          "actual_profit_split", "实际分账", "cooperation_actual_split", "supportive",
          "合作收入可以关注，重点核对实际分账。"),
      entry(110, "cooperation_shared_expense", "cooperation_income", "cooperation_expense", "balance",
          "shared_expenses", "共同开销", "cooperation_shared_expense", "mixed",
          "合作能带来进账，也要先约定共同开销。"),
      entry(111, "cooperation_responsibility", "cooperation_income", "cooperation_roles", "balance",
          "shared_responsibilities", "各自责任", "cooperation_responsibility", "mixed",
          "收入机会和合作限制并存，先写清各自责任。"),
      entry(112, "cooperation_advance_cost", "cooperation_income", "cooperation_advance", "balance",
          "advance_expenses", "垫付开销", "cooperation_advance_cost", "mixed",
          "一起接单前，先确认谁承担垫付开销。"),
      entry(120, "cooperation_cost_risk", "cooperation_income", "cooperation_risk", "control",
          "split_and_costs", "分账和额外开销", "cooperation_split_cost", "restricted",
          "合作收入存在变数，别忽略分账和额外开销。"),
      entry(121, "cooperation_written_split", "cooperation_income", "cooperation_written", "control",
          "written_split_agreement", "书面分账约定", "cooperation_written_split", "restricted",
          "与人一起赚钱受到限制，先留下书面分账约定。"),
      entry(122, "cooperation_exit_cost", "cooperation_income", "cooperation_exit", "control",
          "exit_cost", "退出成本", "cooperation_exit_cost", "restricted",
          "合作条件不够稳时，也要提前算清退出成本。"),

      entry(130, "retention_savings_goal", "retention", "retention_goal", "support",
          "savings_goal", "存钱目标", "retention_savings_goal", "supportive",
          "结余条件有所改善，可以单独设定存钱目标。"),
      entry(131, "retention_fixed_saving", "retention", "retention_fixed", "support",
          "fixed_saving", "固定存下的钱", "retention_fixed_saving", "supportive",
          "有把钱留下的条件，可以先确定每次固定存下的钱。"),
      entry(132, "retention_saving_ratio", "retention", "retention_ratio", "support",
          "saving_ratio", "可存比例", "retention_saving_ratio", "supportive",
          "结余可以作为目标，先按实际收支计算可存比例。"),
      entry(140, "retention_expense_limit", "retention", "retention_budget", "balance",
          "expense_limit", "日常开销", "retention_expense_limit", "mixed",
          "有进账也有支出压力，先给日常开销设上限。"),
      entry(141, "retention_variable_spending", "retention", "retention_variable", "balance",
          "variable_spending", "临时开销", "retention_variable_spending", "mixed",
          "钱能否留下仍有变数，先为临时开销留出余地。"),
      entry(142, "retention_cashflow_record", "retention", "retention_record", "balance",
          "cashflow_record", "收支记录", "retention_cashflow_record", "mixed",
          "进账和结余不能混在一起，先核对收支记录。"),
      entry(150, "retention_fixed_spending", "retention", "retention_spending", "control",
          "fixed_spending", "固定支出", "retention_fixed_spending", "restricted",
          "钱进来不等于能留下，先控制固定支出。"),
      entry(151, "retention_essential_spending", "retention", "retention_essential", "control",
          "essential_spending", "必需开支", "retention_essential_spending", "restricted",
          "结余受到限制时，先保证必需开支。"),
      entry(152, "retention_emergency_reserve", "retention", "retention_emergency", "control",
          "emergency_reserve", "应急余钱", "retention_emergency_reserve", "restricted",
          "支出压力较明显，先留出应急余钱。"));

  private static final Set<String> PATH_KEYS = Arrays.stream(WealthPath.values())
      .map(WealthPath::code).collect(Collectors.toUnmodifiableSet());
  private static final Set<String> CORE_PHRASE_KEYS = ENTRIES.stream()
      .flatMap(entry -> entry.corePhraseKeys().stream()).collect(Collectors.toUnmodifiableSet());

  private WealthHeadlineVocabulary() {}

  public static List<Entry> entries() {
    return ENTRIES;
  }

  public static Set<String> pathKeys() {
    return PATH_KEYS;
  }

  public static Set<String> corePhraseKeys() {
    return CORE_PHRASE_KEYS;
  }

  public static List<Entry> forDecision(String pathKey, String stance) {
    return ENTRIES.stream().filter(entry -> entry.pathKey().equals(pathKey) && entry.stance().equals(stance)).toList();
  }

  private static Entry entry(int order, String themeKey, String pathKey, String subjectKey,
      String angleKey, String objectKey, String objectText, String corePhraseKey,
      String stance, String text) {
    return new Entry(order, themeKey, pathKey, subjectKey, angleKey, objectKey,
        objectText, List.of(corePhraseKey), stance, text);
  }

  public record Entry(int catalogOrder, String themeKey, String pathKey, String subjectKey,
      String angleKey, String objectKey, String objectText, List<String> corePhraseKeys,
      String stance, String text) {
    public Entry {
      corePhraseKeys = List.copyOf(corePhraseKeys);
    }
  }
}
