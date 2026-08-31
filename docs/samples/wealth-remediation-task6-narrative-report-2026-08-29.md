# 财富整改任务 6：正文与样本交付记录

- 日期：2026-08-29。
- 状态：任务 6 开发与定向验证完成，实际生成样本待用户审核；任务 7–10 未开始。
- 计算基线：`95223a4` 的财富 v2；新增文案 `wealth-plain-v3`，没有修改原始排盘或计分权重。
- 当前页面和保存流程仍使用旧版；本轮未改 `ReportService`、前端、数据库、价格或额度，未启动/重启服务、提交、推送或部署。

## 先看这两份样本

1. [完整出生链路生成样本](wealth-v3-task6-birth-samples-2026-08-29.md)：R01、R05、R09，三份三年正文，分别可看年度关注变化、并列与混合限制、主方向延续。
2. [合成边界样本](wealth-v3-task6-boundary-samples-2026-08-29.md)：S02 弱支持、S03 并列、S05 无风险、S07 年度延续、S08 受限。它们用于检验边界，不能冒充真实命盘或正常消费者经历。

两份文档均由当前 Java 生成器直接渲染，未逐篇手写理想文章。完整出生样本重新运行了排盘、事实提取、计分、表达判断、比较和正文生成；“真实生成”不意味着现实预测已验证。

## 这轮具体改了什么

- 新增内部 `WealthNarrativeV3`、`WealthNarrativeWriter`、`WealthPlainCopyV3`，不把草稿直接接到正式报告接口。
- 总论、五路径概览、逐年收入与留钱、风险、现实观察、行动建议、跨年说明与总结均从已有判断生成，文字块保留适用年份和依据引用。
- 不强造唯一收入主线；并列明确展示；弱支持不过度表述；有利和限制同时保留；无负向时不生成风险和风险摘要。
- 跨年直接比较实际依据；同年处于不同报告位置时内容不变，相同条件明确延续，不写固定三阶段剧情。
- 把命盘判断、观察例子和一般建议分别标记；全零且只剩通用建议时拒绝生成，必要来源未核实也拒绝。
- 实现当前草稿的闭合文案许可校验；未知模板、错误作用域和借一般建议隐藏的强预测不能放行。规则详见 [正文许可表](../design/wealth-v3-copy-license.md)。

## 测试与实际输出核对

| 检查 | 结果 | 不能据此宣称什么 |
|---|---|---|
| 定向后端回归 | 159 项通过，0 失败、0 错误、0 跳过；包含此前 103 项、28 条整改规范及 28 项新增正文/样本检查 | 不是全量后端、前端或上线验收 |
| 任务 3 的独立整改规范 | 28/28 通过；任务 5 留下的 12 条正文相关失败已接到实际 v3 正文后通过 | 不代表现实准确率或付费价值过关 |
| 完整出生链路 | 15 组、46 个年度；每年事实、依据、判断、候选及原权重保留 | 不代表穷尽全部合法输入 |
| 合成边界 | 7 组、21 个年度生成内部草稿；S01 全零另行拒绝 | 不能把刻意构造的分数当作真实出生样本 |
| 实际正文结构与引用 | 共 22 份草稿；20 份三年 `Content` 通过草案与引用检查；另 2 份二/五年留在内部 | 没有伪造报告编号或 `ready` 外层来表示保存已经实现 |
| 原判断/年度比较字段 | 15 组、46 年、230 条判断、31 组相邻比较继续通过检查 | 不等于正式接口校验已接入 |
| 原契约测试 | 22 项通过；公开草案继续拒绝五年产品响应 | 不是浏览器验收 |
| 旧版基线 | SHA-256 未变，旧版正文模型回归继续通过 | 不等于已测 MySQL、旧报告页面读取或远程 CI |

冻结基线 SHA-256：`37a756756434ef3ca9115b4812006dbf3d98489047dce78a6f79c8d422a62a2a`。

三年草稿的紧凑 JSON 在本样本集里最大为 **51,559 UTF-8 字节**，包括正文与审计来源。这只是抽样测量，不是存储上限证明；任务 7–9 仍须验证实际持久化格式、最大案例及 MySQL 保存读取，不能以当前样本或 H2 结果替代。

### 测试先行与人工审读

先复跑任务 5 后的 12 条正文失败，再增加原生正文测试；首批 17 项在未实现时全部断言失败，随后实现并转绿。样本审读中又分两批补了 3 项和 5 项会失败的回归，再修正文案：

- “最后能留下的钱有可以考虑的部分”这类不顺的拼句；
- 留钱与风险段的同句重复，以及建议全部用于收入而漏掉所选风险；
- 跨年总论错误使用“今年”；把结余说成增收来源；
- 某路径仅相对名次变化，却让读者误以为其本身改善；
- 同分但来源独立性改变了表达强度，却写成强度延续。

最终通读三份完整出生样本和选出的边界样本。仍保留相同条件下的相同文字，不通过换同义词掩盖缺乏年度差异。

## 内容层仍然有的限制

1. **弱信号不一定值得收费。** S02 如实展示了倾向较轻，但内容价值仍弱。没有把它标成付费合格样本；不能为了“每个人都有明确答案”提高措辞强度。
2. **来源变化不总能转成具体生活事件。** 原算法只给某些抽象条件时，正文只能说方向或限制改变，不能自行补成涨薪、获利、损失或客户数量。
3. **相同年度仍有重复。** R09 的主方向、部分建议延续，这是当前计算的真实输出。是否在页面合并展示，需要后续阅读体验评审；不能用假变化解决。
4. **原计算合理性没有在本轮得到证明。** 本轮整改的是“计算支持什么，文字就说到什么程度”，不是证明传统命理能准确预测现实收入。原始旺衰、权重敏感性另批评审。

## 复现入口

在仓库 `apps/server` 目录运行定向回归并重新生成样本：

```sh
mvn -o -Dtest=WealthNarrativeV3Test,WealthNarrativeSampleTest,WealthRemediationSpec,WealthAnnualComparatorTest,WealthComparisonIntegrationTest,WealthExpressionPolicyTest,WealthProvenanceResolverTest,WealthRemediationFixtureTest,WealthV2BaselineTest,WealthFactExtractorTest,WealthPathEvaluatorTest,WealthArbitratorTest,WealthNarrativePlannerTest,WealthGoldenCaseTest,ReportHorizonTest -Dwealth.captureAssessments=true -Dwealth.captureComparisons=true -Dwealth.captureNarratives=true test
```

在仓库根目录检查实际输出和草案：

```sh
node contracts/tests/check-wealth-v3-narratives.cjs apps/server/target/wealth-v3-narratives.json
node contracts/tests/check-wealth-v3-assessments.cjs apps/server/target/wealth-v3-assessments.json
node contracts/tests/check-wealth-v3-comparisons.cjs apps/server/target/wealth-v3-comparisons.json
node --test contracts/tests/wealth-v3-contract.test.cjs
```

`WealthRemediationSpec` 仍须显式执行；默认命名扫描不自动包含 `Spec`。本次没有删除、跳过断言或修改默认构建以隐藏失败；任务 9 要把该规范纳入自动回归。

## 审核点与下一步

请先审正文是否说清楚具体事情、有无不通顺或无意义的重复，以及有限判断的深度能否接受。确认正文后再授权任务 7：接入新版生成、错误处理、版本化保存与旧快照读取；任务 8 才接页面。

当前草稿重建校验只适用于新生成的当前文案版本，**不能用于历史报告读取或重写**。任务 6 的完成不代表用户已确认文案，也不自动启动后续任务。
