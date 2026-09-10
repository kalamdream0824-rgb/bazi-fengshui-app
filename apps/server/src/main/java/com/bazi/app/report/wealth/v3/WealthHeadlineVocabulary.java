package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.WealthPath;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Closed semantic and copy catalog for deterministic annual wealth headlines. */
public final class WealthHeadlineVocabulary {
  public static final String VERSION = "wealth-headline-v3";

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

  public static Entry entry(String themeKey) {
    return ENTRIES.stream().filter(entry -> entry.themeKey().equals(themeKey)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown wealth headline theme: " + themeKey));
  }

  private static Entry entry(int order, String themeKey, String pathKey, String subjectKey,
      String angleKey, String objectKey, String objectText, String corePhraseKey,
      String stance, String text) {
    String neutralObject = neutralObject(pathKey, order);
    WealthHeadlineSentenceSpec sentenceSpec = sentenceSpec(themeKey, stance, neutralObject);
    return new Entry(order, themeKey, pathKey, subjectKey, angleKey, objectKey,
        neutralObject, List.of(corePhraseKey), stance, sentenceSpec,
        new WealthHeadlineSentenceWriter().write(sentenceSpec));
  }

  private static String neutralObject(String pathKey, int order) {
    int variant = switch (pathKey) {
      case "stable_income" -> ((order - 10) / 10) * 3 + order % 10;
      case "skill_income" -> ((order - 40) / 10) * 3 + order % 10;
      case "project_income" -> ((order - 70) / 10) * 3 + order % 10;
      case "cooperation_income" -> ((order - 100) / 10) * 3 + order % 10;
      case "retention" -> ((order - 130) / 10) * 3 + order % 10;
      default -> throw new IllegalArgumentException("unknown wealth headline path: " + pathKey);
    };
    return switch (pathKey) {
      case "stable_income" -> List.of(
          "每笔钱实际到手的日期", "连续收钱有无中断", "现有收入可支付的日常开销",
          "主要进账所占比例", "进账暂停时的可用余钱", "进账是否过度集中",
          "新增固定支出金额", "长期支出的总额", "可用余钱能够维持多久").get(variant);
      case "skill_income" -> List.of(
          "相同投入能否再次产生进账", "每份投入对应的进账", "同类投入持续带来回报的次数",
          "新增投入占用的时间", "现在还能投入多少时间", "扣除花费后的实际结余",
          "时间和花费的总投入", "同时增加的投入数量", "新增物品花费").get(variant);
      case "project_income" -> List.of(
          "预计与实际到账日期", "账面金额最后能用的数额", "扣除相关花费后的结余",
          "首次投入的金额", "约定与实际到账的差距", "相关花费的最高金额",
          "尚未到账的金额", "首次到账所占比例", "后续增加的投入").get(variant);
      case "cooperation_income" -> List.of(
          "各自分得的金额", "共同用钱的事前约定", "最后各自收到的金额",
          "共同承担的开销", "各自负责的部分", "由谁先承担支出",
          "分配金额与额外开销", "写下来的资金约定", "停止共同安排的成本").get(variant);
      case "retention" -> List.of(
          "每月存下的目标金额", "每次进账后先留下多少", "实际可以留下的比例",
          "日常开销的最高金额", "临时开销预留的金额", "每月进账与支出记录",
          "每月固定支出的总额", "必需开支的实际金额", "应急余钱能维持多久").get(variant);
      default -> throw new IllegalArgumentException("unknown wealth headline path: " + pathKey);
    };
  }

  private static WealthHeadlineSentenceSpec sentenceSpec(
      String themeKey, String stance, String object) {
    return switch (themeKey) {
      case "stable_receipt_support" -> spec("今年收到钱的日期", stance, "较可能保持规律", "", "记下" + object);
      case "stable_continuity_support" -> spec("今年连续收钱的状态", stance, "较可能延续", "", "查看" + object);
      case "stable_coverage_support" -> spec("今年可用于日常开支的钱", stance, "较可能增多", "", "计算" + object);
      case "stable_source_balance" -> spec("今年持续进账", stance, "有保持稳定的机会",
          "过度依赖单一来源会放大中断影响", "核对" + object);
      case "stable_buffer_balance" -> spec("今年进账", stance, "有延续机会",
          "到账时间仍可能波动", "确认" + object);
      case "stable_client_balance" -> spec("今年长期进账", stance, "有保持稳定的机会",
          "集中在少数来源时更容易波动", "核对" + object);
      case "stable_expense_control" -> spec("今年持续进账", stance, "更容易出现中断", "", "核对" + object);
      case "stable_commitment_control" -> spec("今年长期进账", stance, "稳定性不足", "", "确认" + object);
      case "stable_reserve_control" -> spec("今年可用资金", stance, "需要更多缓冲", "", "评估" + object);

      case "skill_repeat_payment" -> spec("今年新增投入带来的收入", stance, "较可能同步提高", "", "比较" + object);
      case "skill_pricing_support" -> spec("今年时间与收入的对应关系", stance, "更容易看清", "", "记录" + object);
      case "skill_returning_customer" -> spec("今年同类付出得到的回报", stance, "更容易确认", "", "统计" + object);
      case "skill_time_balance" -> spec("今年增加投入", stance, "可能带来更多进账",
          "占用的时间也会增加", "记录" + object);
      case "skill_capacity_balance" -> spec("今年投入机会", stance, "可能增加",
          "可用时间可能不足", "核对" + object);
      case "skill_margin_balance" -> spec("今年实际进账", stance, "可能提高",
          "投入成本也会增加", "核对" + object);
      case "skill_service_cost" -> spec("今年增加投入后的实际结余", stance, "未必同步增加", "", "比较" + object);
      case "skill_workload_control" -> spec("今年可投入的时间", stance, "更容易吃紧", "", "记录" + object);
      case "skill_equipment_control" -> spec("今年新增投入", stance, "回收速度可能较慢", "", "核对" + object);

      case "project_payment_timing" -> spec("今年预计收到的钱", stance, "较可能按计划到手", "", "比较" + object);
      case "project_actual_receipt" -> spec("今年账面金额", stance, "更容易真正收到", "", "记录" + object);
      case "project_net_receipt" -> spec("今年到账后的实际结余", stance, "更容易看清", "", "计算" + object);
      case "project_small_trial" -> spec("今年首次投入", stance, "可能换来额外进账",
          "最后收到的金额仍可能波动", "用小额尝试核对" + object);
      case "project_terms_balance" -> spec("今年预计进账", stance, "有实现机会",
          "实际到账仍可能延后", "记录" + object);
      case "project_budget_balance" -> spec("今年额外进账", stance, "可能增加",
          "相关支出也会增加", "计算" + object);
      case "project_pending_cash" -> spec("今年预计进账", stance, "更容易延后", "", "核对" + object);
      case "project_deposit_control" -> spec("今年实际到账", stance, "稳定性不足", "", "记录" + object);
      case "project_scope_control" -> spec("今年新增投入", stance, "更容易超出原有安排", "", "核对" + object);

      case "cooperation_profit_split" -> spec("今年共同安排的钱", stance, "更容易形成清楚结果", "", "写清" + object);
      case "cooperation_agreement_support" -> spec("今年涉及他人的资金安排", stance, "更容易按约定执行", "", "核对" + object);
      case "cooperation_actual_split" -> spec("今年共同资金的结果", stance, "更容易完成分配", "", "记录" + object);
      case "cooperation_shared_expense" -> spec("今年一起安排资金", stance, "能让用钱更方便",
          "同时可能增加责任和花费", "写清" + object);
      case "cooperation_responsibility" -> spec("今年与他人有关的资金责任", stance, "可能增多",
          "负责范围容易说不清", "列出" + object);
      case "cooperation_advance_cost" -> spec("今年共同安排支出", stance, "可能更频繁",
          "一方先垫付会增加压力", "记录" + object);
      case "cooperation_cost_risk" -> spec("今年分配共同资金", stance, "较容易出现分歧", "", "逐项写清" + object);
      case "cooperation_written_split" -> spec("今年涉及他人的资金", stance, "更容易因约定不清而延后", "", "记录" + object);
      case "cooperation_exit_cost" -> spec("今年共同资金安排", stance, "稳定性不足", "", "算清" + object);

      case "retention_savings_goal" -> spec("今年实际结余", stance, "更容易增加", "", "设定" + object);
      case "retention_fixed_saving" -> spec("今年每次进账后留下的钱", stance, "更容易保持稳定", "", "记录" + object);
      case "retention_saving_ratio" -> spec("今年实际可存比例", stance, "更容易提高", "", "计算" + object);
      case "retention_expense_limit" -> spec("今年月底能留下的钱", stance, "可能增多",
          "新增花费也可能消耗结余", "设好" + object);
      case "retention_variable_spending" -> spec("今年存钱进度", stance, "有改善机会",
          "零散花费也会增加", "确认" + object);
      case "retention_cashflow_record" -> spec("今年收支差额", stance, "可能扩大",
          "花出去的钱也可能变多", "检查" + object);
      case "retention_fixed_spending" -> spec("今年实际结余", stance, "更容易被固定支出压缩", "", "重新核对" + object);
      case "retention_essential_spending" -> spec("今年每月可用的钱", stance, "受到必要开支限制", "", "保证" + object);
      case "retention_emergency_reserve" -> spec("今年支出压力", stance, "更容易增加", "", "按" + object + "留出应急余量");
      default -> throw new IllegalArgumentException("unknown wealth headline theme: " + themeKey);
    };
  }

  private static WealthHeadlineSentenceSpec spec(
      String subject,
      String tone,
      String judgment,
      String limitation,
      String verification) {
    return new WealthHeadlineSentenceSpec(subject, tone, judgment, limitation, verification);
  }

  public record Entry(int catalogOrder, String themeKey, String pathKey, String subjectKey,
      String angleKey, String objectKey, String objectText, List<String> corePhraseKeys,
      String stance, WealthHeadlineSentenceSpec sentenceSpec, String text) {
    public Entry {
      corePhraseKeys = List.copyOf(corePhraseKeys);
    }
  }
}
