# 单份命书购买与解锁 Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** 为非会员及会员每日三份额度用完后的用户提供单份命书购买闭环，确保生成失败不进入支付、重复支付不重复扣款、未支付内容不可读取。

**Architecture:** 采用“先生成锁定报告，再支付解锁”的顺序。报告生成和待支付订单在同一事务内创建；支付确认只把所属用户的锁定报告改为可读状态。会员额度内仍走现有直接生成，单份购买不计入会员每日三份。

**Tech Stack:** Java 17、Spring Boot、MyBatis Plus、H2/MySQL、JUnit 5、React、TypeScript、Vitest、OpenAPI。

---

## 产品边界

- 当前支付渠道只有本地模拟支付，不能宣传为微信、支付宝或银行卡真实扣款。
- 本期可销售：事业通俗版、财富通俗版、感情通俗版，均为 690 分。
- 综合主题尚未开放；专业版内容尚未形成独立价值，继续禁售，不能只因页面曾显示 12.9 元就收费。
- 会员每日前三份成功报告直接生成；第四份及以后可单独购买。
- 非会员每份单独购买，不限制购买次数。
- 已支付解锁的命书永久保存在“我的命书”。

## 状态流

```text
生成与质量校验失败 → 无报告、无订单、无支付
生成成功 → locked 报告 + pending 订单
模拟/渠道支付成功 → paid 订单 + ready 报告
重复支付确认 → 返回同一 ready 报告，不重复改变账务
```

---

### Task 1: 建立报告商品目录与订单关联

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/domain/constants/ReportProducts.java`
- Create: `apps/server/src/main/java/com/bazi/app/domain/ReportOrderLink.java`
- Create: `apps/server/src/main/java/com/bazi/app/mapper/ReportOrderLinkMapper.java`
- Modify: `apps/server/src/main/resources/schema.sql`
- Test: `apps/server/src/test/java/com/bazi/app/ReportProductsTest.java`

**Step 1: 写失败测试**

断言三个已开放通俗版主题均为 690 分；综合主题和所有专业版抛出稳定业务错误 `REPORT_PRODUCT_UNAVAILABLE`。

**Step 2: 运行 RED**

```bash
cd apps/server
./mvnw -q -Dtest=ReportProductsTest test
```

预期：因 `ReportProducts` 尚不存在而编译失败。

**Step 3: 实现最小商品目录**

`ReportProducts` 只接受 `career/plain`、`wealth/plain`、`relationship/plain`，输出不可由客户端覆盖的商品编码和 690 分价格。新增 `bazi_report_order_link`：

```sql
CREATE TABLE IF NOT EXISTS bazi_report_order_link (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  report_id BIGINT NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Step 4: 运行 GREEN**

重新运行 `ReportProductsTest`，预期通过。

---

### Task 2: 生成锁定报告与待支付订单

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/dto/ReportCheckoutDto.java`
- Create: `apps/server/src/main/java/com/bazi/app/service/ReportCheckoutService.java`
- Modify: `apps/server/src/main/java/com/bazi/app/service/ReportService.java`
- Modify: `apps/server/src/main/java/com/bazi/app/controller/ReportController.java`
- Test: `apps/server/src/test/java/com/bazi/app/ReportCheckoutIntegrationTest.java`

**Step 1: 写失败测试**

调用 `POST /api/v1/reports/checkout`，断言：

- 返回服务器定价 690 分、订单 `pending`、报告 ID；
- 报告已生成但状态为 `locked`；
- `GET /api/v1/reports` 不返回锁定报告；
- `GET /api/v1/reports/{id}` 返回 404；
- 非订单所有者也无法读取；
- 生成器失败时既没有报告也没有订单。

**Step 2: 运行 RED**

```bash
cd apps/server
./mvnw -q -Dtest=ReportCheckoutIntegrationTest test
```

预期：checkout 路由不存在。

**Step 3: 重构报告生成但不复制算法**

将 `ReportService.create` 内的内容生成抽成私有构建方法。新增 `createLocked`，复用同一生成和质量校验，只把状态写为 `locked`，且不调用会员次数检查。`list/get` 只查询 `ready`。

`ReportCheckoutService.prepare` 在同一事务中：

1. 服务端校验商品与价格；
2. 生成并保存 `locked` 报告；
3. 创建 `pending` 订单，`plan` 使用报告商品编码；
4. 写入订单与报告关联；
5. 返回 checkout DTO，不返回正文。

**Step 4: 运行 GREEN**

重新运行 `ReportCheckoutIntegrationTest`，预期全部通过。

---

### Task 3: 支付确认后幂等解锁

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/service/ReportCheckoutService.java`
- Modify: `apps/server/src/main/java/com/bazi/app/controller/ReportController.java`
- Modify: `apps/server/src/test/java/com/bazi/app/ReportCheckoutIntegrationTest.java`

**Step 1: 写失败测试**

调用 `POST /api/v1/reports/checkout/{orderId}/mock-pay`，断言：

- 订单从 `pending` 变为 `paid`；
- 对应报告从 `locked` 变为 `ready` 并返回正文；
- “我的命书”出现该报告；
- 重复调用返回同一报告，不重复创建报告或订单；
- 其他用户支付返回 `ORDER_NOT_FOUND`；
- 会员单独购买的报告不会占用每日三份额度。

**Step 2: 运行 RED**

预期 mock-pay 路由不存在。

**Step 3: 实现事务内 CAS 解锁**

支付确认校验订单、关联和所有者。以 `pending → paid` 条件更新实现幂等；同一事务内以 `locked → ready` 更新报告。若订单已 paid 且报告已 ready，直接返回同一报告；其他状态返回 `ORDER_STATUS_INVALID`。

**Step 4: 运行 GREEN**

```bash
cd apps/server
./mvnw -q -Dtest=ReportCheckoutIntegrationTest,ReportEntitlementIntegrationTest,PayIntegrationTest test
```

预期全部通过。

---

### Task 4: 非会员必须购买，会员额度内直接生成

**Files:**
- Modify: `apps/server/src/main/java/com/bazi/app/service/ReportEntitlementService.java`
- Modify: `apps/server/src/test/java/com/bazi/app/ReportEntitlementIntegrationTest.java`
- Modify: `apps/web/src/services/payApi.ts`
- Modify: `apps/web/src/pages/ReportPage.tsx`
- Modify: `apps/web/src/pages/ReportPage.test.tsx`

**Step 1: 写失败测试**

- 非会员直接调用 `POST /reports` 返回 `REPORT_PAYMENT_REQUIRED`；
- 会员前三份继续直接生成；
- 前端非会员走 prepare → mock-pay → 打开同一份报告；
- 会员超额后显示“单独购买”，确认后走相同 checkout；
- 前端不展示技术错误，支付失败不假装成功。

**Step 2: 运行 RED**

运行后端权益测试和 `ReportPage.test.tsx`，确认因购买分流尚未实现而失败。

**Step 3: 实现前后端分流**

前端读取 `/me`：有效会员先直生成；非会员直接创建 checkout。会员遇到 `MEMBER_DAILY_REPORT_LIMIT` 后显示独立的单份购买操作，不自动扣款。模拟支付成功后跳转到返回的报告 ID。

**Step 4: 运行 GREEN**

运行相关后端与前端测试，预期通过。

---

### Task 5A: 接口契约与全主题销售矩阵

**Files:**
- Modify: `contracts/openapi.yaml`
- Create: `contracts/tests/report-checkout-contract.test.cjs`
- Modify: `docs/design/README.md`

**Step 1: 补充契约**

记录 checkout 和 mock-pay 路由、DTO、商品价格由服务端决定、锁定报告不可读。

**Step 2: 跑销售矩阵**

| 主题 | 通俗版 | 专业版 |
|---|---:|---:|
| 事业 | 允许 690 分 | 禁售 |
| 财富 | 允许 690 分 | 禁售 |
| 感情 | 允许 690 分 | 禁售 |
| 综合 | 禁售 | 禁售 |

**Step 3: 小批次验证**

只运行报告商品目录、checkout 接口和契约测试。通过后暂停并汇报，不进入全量回归。

---

### Task 5B: 端到端适配与全量验收

**Files:**
- Modify: `e2e/run_wealth_v3_e2e.py`
- Modify as needed: 本批次发现的回归文件

**Step 1: 适配端到端流程**

将非会员报告生成改为 checkout → mock-pay → 读取同一份 ready 报告；会员额度内继续验证直接生成。

**Step 2: 全量自动验证**

```bash
cd apps/server && ./mvnw -q test
cd ../web && npm test -- --run && npm run build
cd ../../contracts && node --test tests/*.test.cjs
```

**Step 3: 人工验收**

- 非会员购买并打开命书；
- 会员前三份免费生成；
- 第四份明确询问是否单独购买；
- 取消购买不产生 paid 订单；
- 重复支付确认只解锁一次；
- “我的命书”只显示 ready 报告。

---

## 后续真实渠道接入

本计划不伪造支付渠道。正式接微信/支付宝时，保留商品目录、锁定报告、订单关联与幂等解锁，只替换模拟支付确认：渠道回调验签成功后执行同一解锁服务。若渠道要求先实扣，必须补充退款单、退款状态和异步对账，不能只在页面显示“已退款”。
