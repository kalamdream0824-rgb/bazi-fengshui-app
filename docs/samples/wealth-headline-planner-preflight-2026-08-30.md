# 财富年度标题规划器全量预演报告（3A 修订版，2026-08-30）

## 结论

小批次 3A 已解决“为了去重而降低标题质量层级”的问题。

- **固定输入可行性：通过。** 15 个完整固定测试输入全部可以组成不同年度主题。
- **去重质量闸门：通过。** 46 条完整输入标题的 `diversityDowngrade` 为 0；没有任何标题仅因去重而降低 annual 直接依据、年度变化、表达强度或证据显著度。
- **具体对象与文本审计：通过。** 缺少对象 0 条，四至八字跨年重复片段告警 0 份。
- **真实线上失败率：仍未测量。** 当前样本是开发固定输入和合成边界，不能代表真实用户分布。

从技术和固定语料结果看，可以进入 v3.3 契约扩展；进入正文接线后仍需逐字人工验收 R04、R08、R12，防止“规则正确但中文不自然”。

## 为什么上一版结论需要纠正

上一版用“所选路径显著度是否低于该年最高显著度”估算去重降级，这个指标不够准确：

- 年度标题优先表达当年直接依据和年度变化；
- 三年总分最高但主要来自原局的路径，可能更适合放在共同主线，而不是重复写进每个年度标题；
- 因此，选择一个显著度较低但有当年直接依据的谨慎主题，不等于为了去重牺牲准确性。

3A 改为比较完整质量元组：

```text
当年直接依据 → 年度变化 → 表达强度 → 证据显著度
```

同一质量元组内的不同对象角度都记为 `qualityRank = 1`。只有跨年去重迫使标题进入更低质量元组时，才记为 `diversityDowngrade`。这一口径更接近真实问题。

## 样本范围

| 类型 | 数量 | 成功 | 失败 |
|---|---:|---:|---:|
| 完整固定测试输入（R01–R12、H02、H05、Y27） | 15 | 15 | 0 |
| 合成边界（S01–S08） | 8 | 7 | 1 |
| 合计 | 23 | 22 | 1 |

唯一失败为全零依据边界 `S01`：三个年份候选数量均为 0，原因码为 `INSUFFICIENT_DIVERSITY`。这是预期拒绝。

## 3A 改造内容

### 1. 增加质量层级

每个候选同时记录：

- 是否由当年 annual 事实直接支撑；
- 是否包含实际年度变化；
- 判断强度 `pronounced / supported / limited`；
- 支持与限制证据的总显著度。

候选顺位不再等同于质量层级。两个证据质量完全相同、只有对象不同的标题，可以分别是候选第 1、2 顺位，但两者的 `qualityRank` 都是 1。

### 2. 报告级组合先保质量，再去重

组合比较顺序调整为：

1. 最差 `qualityRank` 越小越优先；
2. 全报告质量降级总量越小越优先；
3. 再比较对象多样性、annual 依据数量和总显著度；
4. 最后使用稳定签名解决并列。

因此，规划器不能再为了凑三个主题而切换到更低质量候选。

### 3. 扩充同质量层级的具体对象

每条财富路径在 supportive、mixed、restricted 三种判断下均提供三个不同对象角度。例如：

- 稳定收入：每月到账、收入中断的可能、日常开支覆盖；
- 靠能力赚钱：重复付费、服务定价、老客户再次购买；
- 项目收入：付款时间、实际到账、扣除成本后的结余；
- 合作收入：分账方式、合作约定、实际分账；
- 结余：存钱目标、固定存下的钱、可存比例。

这些角度只改变标题的核对对象，不改变原始事实、证据、判断强度或年度变化。

## 量化结果

### 完整固定测试输入

共 46 条年度标题：

- `diversityDowngrade`：0 条；
- 使用第二顺位或更后候选：7 条，占 15.2%；
- 使用第三顺位或更后候选：1 条，占 2.2%；
- 缺少具体对象：0 条；
- 文本重复片段告警：0 份。

第二、第三顺位均处于同一 `qualityRank = 1`，只是采用不同对象角度，不再代表弱主题上浮。

### 全部成功样本

22 份成功样本共 67 条标题：

- `diversityDowngrade`：0 条；
- 使用第二顺位或更后候选：21 条；
- 使用第三顺位或更后候选：8 条；
- 缺少具体对象：0 条；
- 文本重复片段告警：0 份。

## 完整输入逐案结果

| 样本 | 主题序列 | 候选顺位 | 质量层级 |
|---|---|---|---|
| R01 | project_payment_timing / stable_receipt_support / project_small_trial | 1 / 1 / 1 | 1 / 1 / 1 |
| R02 | project_payment_timing / stable_receipt_support / retention_expense_limit | 1 / 1 / 1 | 1 / 1 / 1 |
| R03 | retention_expense_limit / retention_variable_spending / cooperation_shared_expense | 1 / 2 / 1 | 1 / 1 / 1 |
| R04 | project_payment_timing / retention_expense_limit / retention_variable_spending | 1 / 1 / 2 | 1 / 1 / 1 |
| R05 | cooperation_shared_expense / skill_repeat_payment / stable_receipt_support | 1 / 1 / 1 | 1 / 1 / 1 |
| R06 | project_payment_timing / stable_receipt_support / cooperation_profit_split | 1 / 1 / 1 | 1 / 1 / 1 |
| R07 | project_payment_timing / stable_receipt_support / project_small_trial | 1 / 1 / 1 | 1 / 1 / 1 |
| R08 | project_payment_timing / skill_repeat_payment / project_actual_receipt | 1 / 1 / 2 | 1 / 1 / 1 |
| R09 | cooperation_cost_risk / cooperation_shared_expense / cooperation_responsibility | 1 / 1 / 2 | 1 / 1 / 1 |
| R10 | project_small_trial / cooperation_shared_expense / cooperation_cost_risk | 1 / 1 / 1 | 1 / 1 / 1 |
| R11 | retention_expense_limit / retention_variable_spending / retention_cashflow_record | 1 / 2 / 3 | 1 / 1 / 1 |
| R12 | project_small_trial / cooperation_shared_expense / project_payment_timing | 1 / 1 / 1 | 1 / 1 / 1 |
| H02 | project_payment_timing / stable_receipt_support | 1 / 1 | 1 / 1 |
| H05 | project_payment_timing / stable_receipt_support / project_small_trial / retention_expense_limit / project_actual_receipt | 1 / 1 / 1 / 1 / 2 | 1 / 1 / 1 / 1 / 1 |
| Y27 | stable_receipt_support / project_small_trial / retention_expense_limit | 1 / 1 / 1 | 1 / 1 / 1 |

## 仍需保留的风险说明

1. 部分标题引用 `limited` 判断，但这不是去重造成的降级；通常因为它有更直接的当年依据或年度变化。正文接线后仍要人工检查这些谨慎主题是否值得占据顶部标题。
2. 多个年份可能使用同一路径的不同对象角度。文案不会声称趋势上升或下降，但消费者是否觉得三年差异足够，需要页面样本验收。
3. 没有经授权的脱敏真实样本，所以不能声明线上失败率或主题分布已经稳定。

## 发布检查点

固定语料的生产接线前置条件现已满足：

- 15/15 完整固定输入可规划；
- `diversityDowngrade = 0`；
- 缺少对象 = 0；
- 文本重复片段告警 = 0；
- 同输入连续运行 100 次结果一致。

下一阶段可以进入 Task 4：扩展 v3.3 输出契约。Task 5 接入正文后必须再次运行同一预演，并补充 R04、R08、R12 的完整中文样本验收。

## 复现命令

```bash
cd apps/server
./mvnw -q -Dtest=WealthHeadlineVocabularyTest,WealthAnnualHeadlinePlannerTest,WealthHeadlinePlannerPreflightTest test
```

机器可读汇总输出到 `apps/server/target/wealth-headline-preflight.json`，属于构建产物，不提交源码仓库。
