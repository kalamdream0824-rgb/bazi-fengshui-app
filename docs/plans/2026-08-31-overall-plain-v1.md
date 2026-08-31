# 综合运势通俗版 v1 实施计划

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** 在不引入 LLM、不要求用户填写额外问卷的前提下，生成一份覆盖未来三年、每年同时检查生活节奏、事业责任、钱财安排和关系支持，并明确年度主次顺序的综合运势通俗版。

**Architecture:** 综合主题复用排盘与 `AnnualContextFactory` 的确定性事实，但不拼接现有事业、财富和感情正文。新增综合主题专用的维度评估器、三年裁决器和通俗文案规划器：评估器保证四个维度每年都有状态，裁决器选择年度主焦点并解释年度变化，规划器只把结构化判断翻译成日常中文。内容先进入保存与页面阅读链路，完成人工样本验收后才允许加入商品目录。

**Tech Stack:** Java 17、Spring Boot、Jackson、JUnit 5、React、TypeScript、Vitest。

---

## 产品边界

- 版本：仅 `overall / plain`，内容版本 `overall-narrative-v1`。
- 年限：对外固定三年；不提供年限选择。
- 输入：只使用命盘和当前日期，不增加现实状态问卷。
- 覆盖：每年都输出生活节奏、事业责任、钱财安排、关系支持四个维度，不能因为没有强信号而省略维度。
- 主线：每年只设一个“最值得关注”，其余维度作为次要提醒；连续三年标题不得相同。
- 边界：不预测具体事件，不输出分数，不把机会写成必然结果。
- 商业：内容验收前不加入 `ReportProducts`，不允许扣费生成。

### Task 1: 定义综合主题结构化计算契约

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallDimension.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallStance.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallDimensionEvaluation.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/OverallDimensionEvaluatorTest.java`

**Steps:**
1. 先写失败测试，固定两个命盘和三年上下文，要求每年恰好产生四个维度、维度代码不重复、依据键可追溯。
2. 运行 `cd apps/server && ./mvnw -Dtest=OverallDimensionEvaluatorTest test`，确认因类型或评估器尚不存在而失败。
3. 实现不可为空的结构化记录；状态只允许 `supportive / balanced / pressured / mixed`。
4. 重跑测试，确认基础契约通过。

### Task 2: 实现四维全量评估器

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallDimensionEvaluator.java`
- Modify: `apps/server/src/test/java/com/bazi/app/report/overall/OverallDimensionEvaluatorTest.java`

**Steps:**
1. 先增加失败测试，覆盖偏弱、承载适中、岁运合、岁运冲害刑以及五种流年功能组。
2. 每个维度分别维护支持事实和限制事实，不允许用同一条事实同时为同一维度加分和减分。
3. 分值仅用于内部排序；输出保存证据键和状态，不向用户展示数值。
4. 运行单测并检查同一输入重复执行的结果完全一致。

### Task 3: 实现三年主焦点与冲突裁决

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallYearEvaluation.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallPeriodEvaluation.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallPeriodArbitrator.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/OverallPeriodArbitratorTest.java`

**Steps:**
1. 先写失败测试：强限制优先于普通支持；同分按固定维度顺序裁决；同一输入输出稳定。
2. 每年选择一个主焦点和一个次焦点，保留四维完整评估。
3. 相邻年度主焦点相同时，仅在其他候选足够接近且有直接年度依据时调整；不得为了文案去重抬高明显较弱的判断。
4. 输出相邻年度的 `延续 / 转向 / 加强 / 缓和` 关系。
5. 运行单测，确认三年主标题所依赖的 `focusCode` 不做文本反推。

### Task 4: 生成日常中文正文并执行语言门槛

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallNarrativePlan.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallPlainCopy.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/overall/OverallNarrativePlanner.java`
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePlannerTest.java`

**Steps:**
1. 先写失败测试，要求三年标题不同、每年明确主问题、四维内容齐全、行动有对象。
2. 禁止“卡点、抓手、赋能、赛道、闭环、现有证据不足、先观察再判断”等机械或无答案表达。
3. 每年正文固定为：一句总判断、四维简述、最需要先处理的问题、两项行动、调整条件、与下一年关系。
4. 总论必须指出三年内哪一年适合推进、哪一年宜收紧；如果事实不支持强弱差异，明确写“节奏接近”，不得编造峰值。
5. 输出一份固定样本 Markdown 供人工审阅。

### Task 5: 接入后端保存与历史读取

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/service/ReportService.java`
- Modify: `apps/server/src/test/java/com/bazi/app/SavedReportIntegrationTest.java`
- Modify: `contracts/openapi.yaml`

**Steps:**
1. 先写集成失败测试，要求 `overall / plain` 能生成、保存、列表读取和详情读取，内容版本为 `overall-narrative-v1`。
2. 接入综合评估与规划器；专业版继续返回未开放错误。
3. 生成失败时不得插入报告、扣会员次数或推进支付订单。
4. 保留旧内容版本读取行为，新增版本按专用类型反序列化。

### Task 6: 增加综合命书阅读页

**Files:**
- Create: `apps/web/src/components/report/OverallReportReader.tsx`
- Modify: `apps/web/src/services/reportApi.ts`
- Modify: `apps/web/src/pages/ReportReaderPage.tsx`
- Modify: `apps/web/src/pages/ReportReaderPage.test.tsx`
- Modify: `apps/web/src/styles/app.css`

**Steps:**
1. 先写失败测试，验证总论、三年主焦点、四维内容和行动路线均可阅读。
2. 新增显式 `OverallSavedReport` 类型和类型守卫，不根据标题文字猜内容版本。
3. 复用现有命书纸张视觉，但用四维小节建立层级，不制作仪表盘或数值雷达图。
4. 完成桌面和移动端截图核对。

### Task 7: 开放免费验收入口，不开放收费

**Files:**
- Modify: `apps/web/src/pages/ReportPage.tsx`
- Modify: `apps/web/src/pages/ReportPage.test.tsx`
- Keep unchanged: `apps/server/src/main/java/com/bazi/app/domain/constants/ReportProducts.java`

**Steps:**
1. 先写失败测试，登录会员可从综合主题生成验收报告，无需问卷。
2. 非会员与会员超额时不进入购买流程，显示“综合版内测中，暂不单独售卖”。
3. 不修改商品目录，确保支付准备接口仍拒绝综合主题。
4. 验证已生成的综合报告可在“我的命书”长期读取。

### Task 8: 样本预演与商业开放决策

**Files:**
- Create: `apps/server/src/test/java/com/bazi/app/report/overall/OverallNarrativePreflightTest.java`
- Create: `docs/samples/overall-v1-preflight-2026-08-31.md`
- Modify only after approval: `apps/server/src/main/java/com/bazi/app/domain/constants/ReportProducts.java`

**Steps:**
1. 对至少 12 个固定命盘生成三年报告，统计主焦点分布、重复标题、空维度和禁用词。
2. 人工审阅至少 3 份差异明显的完整样本，重点检查是否像三个单主题的缩写拼接。
3. 任一样本出现空答案、同年自相矛盾或连续三年同标题，保持禁售并返回 Task 2–4 修正。
4. 仅在用户明确验收后，把 `overall / plain / 690` 加入商品目录并补支付集成测试。

## 全量验收

- 后端：`cd apps/server && ./mvnw test`
- 前端：`cd apps/web && npm test -- --run && npm run lint && npm run build`
- 差异：`git diff --check`
- 人工：至少一份综合报告逐年检查四维内容、主次关系、三年转折和中文流畅度。
