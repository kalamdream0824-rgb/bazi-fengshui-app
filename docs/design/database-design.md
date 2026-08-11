# 八字排盘 App · 数据库设计文档（v0.1）

| 项目 | 内容 |
|---|---|
| 版本 | v0.3（订单落账 + 模拟支付 · 与后端 v0.7 对齐） |
| 日期 | 2026-08-09 |
| 目标库 | MySQL 8.x（utf8mb4）；开发/测试用 H2（`MODE=MySQL` 兼容） |
| DDL 单一来源 | `apps/server/src/main/resources/schema.sql`（幂等，`IF NOT EXISTS`） |

## 1. 表清单

### 1.1 bazi_user（用户）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(64) | NOT NULL, UNIQUE | 登录名 |
| password_hash | VARCHAR(100) | NOT NULL | BCrypt 哈希，不存明文 |
| created_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 注册时间 |

预留（会员体系待办后加）：`member_expire_at TIMESTAMP NULL`（会员到期时间）、`plan VARCHAR(32) NULL`（当前套餐）。

### 1.2 bazi_record（排盘记录）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 所属用户（逻辑关联 bazi_user.id） |
| request_json | TEXT | NOT NULL | 排盘请求快照（`PaipanRequest` JSON） |
| result_json | TEXT | NOT NULL | 排盘结果快照（`PaipanResult` JSON） |
| created_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |

索引建议：

```sql
CREATE INDEX idx_record_user_created ON bazi_record (user_id, created_at DESC);
```

去重说明：当前按 `(user_id, request_json)` `selectCount` 查重；记录量上来后可加唯一索引，或对 `request_json` 做哈希列（`request_hash CHAR(64)` + 唯一索引）提升性能。

### 1.3 bazi_order（订单，已启用）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | 订单号（对外可加业务单号） |
| user_id | BIGINT NOT NULL | 下单用户 |
| plan | VARCHAR(32) NOT NULL | 套餐标识（`member_1m` / `member_3m`） |
| amount_cents | INT NOT NULL | 金额（分） |
| status | VARCHAR(16) NOT NULL | `pending → paid`（`refunded` 预留） |
| provider | VARCHAR(16) NULL | 支付渠道（`mock` / `redeem`，未来 `wechat` / `alipay`） |
| provider_trade_no | VARCHAR(64) NULL | 渠道交易号（回调对账与幂等判断用） |
| created_at / paid_at | TIMESTAMP | 创建/支付时间 |

状态机与幂等：

- 创建订单：`status=pending`、`provider=NULL`、`amount_cents` 由后端套餐常量定价。
- 模拟支付回调：校验订单归属与状态 → CAS 条件更新（`WHERE id=? AND status='pending'`）置为 `paid` + `provider='mock'` + `provider_trade_no='MOCK-<id>'` + `paid_at` → 开通会员。**重复回调不重复顺延**（状态已 paid 直接返回当前会员状态）。
- 兑换码：落账 `provider='redeem'`、`provider_trade_no=兑换码`、`amount=0`。
- 真实渠道接入后复用同一订单表：回调更新订单 → 调 `MembershipService` 开通逻辑，无需改表。

索引建议：

```sql
CREATE INDEX idx_order_user_created ON bazi_order (user_id, created_at DESC);
CREATE INDEX idx_order_paid_idem ON bazi_order (provider, provider_trade_no, status);
```

### 1.4 兑换码 bazi_redeem_code（会员权益 P2 已加）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| code | VARCHAR(32) NOT NULL UNIQUE | 兑换码（大写） |
| plan | VARCHAR(32) NOT NULL | 套餐标识（如 `member_3m`） |
| duration_days | INT NOT NULL | 时长（天） |
| used_by | BIGINT NULL | 使用用户（NULL=未用） |
| used_at / created_at | TIMESTAMP | 使用/创建时间 |

兑换逻辑：校验码有效且未用 → 会员到期时间在"当前与已有到期时间取大者"上顺延 duration_days → 写入 `bazi_order`（`provider=redeem, status=paid, amount=0`）→ 标记码已用。

## 2. 设计约定

- 命名：表 snake_case + `bazi_` 前缀；主键统一 `BIGINT AUTO_INCREMENT`。
- 字符集：`utf8mb4` + `utf8mb4_unicode_ci`（docker-compose 已配置）。
- **JSON 快照设计**：`request_json/result_json` 存契约快照，保证历史排盘不因算法升级而漂移——这是本项目的关键决策。
- 业务主数据放表字段；可变的计算/展示数据放 JSON 快照。
- 删除：当前业务"删除记录"即物理删除，不引入软删除。
- JSON 内字段 camelCase（与 openapi 契约一致），表列 snake_case，由 MyBatis-Plus `map-underscore-to-camel-case` 映射。

## 3. 迁移策略

- 开发：`schema.sql` 幂等 init（H2 启动即建；Docker MySQL 挂载到 `docker-entrypoint-initdb.d` 自动建表）。
- 生产：当前手动执行 `schema.sql`；表结构开始演进后引入 **Flyway** 版本化迁移（`V1__init.sql` 起步）。
- 变更纪律：加列必须带兼容默认值；禁止破坏性变更（删列/改类型/改唯一约束）不做评审直接上。

## 4. 安全

- 生产数据库凭据只通过环境变量注入（`DB_URL / DB_USER / DB_PASSWORD`），不进代码库。
- 生产账号最小权限（业务库仅 DML，不用 root）。
- 备份策略在部署阶段文档中补充。

## 5. 变更日志

- v0.3（2026-08-11）：订单表由预留转正式——明确状态机 `pending → paid`、模拟支付 CAS 幂等更新、`provider=mock/redeem` 与索引建议（与后端 v0.7 对齐）。
- v0.2（2026-08-10）：会员权益落地——bazi_user 增加 plan/member_expire_at；新增 bazi_redeem_code 与 bazi_order 表。
- v0.1（2026-08-09）：建立数据库设计文档（用户/记录表 + 订单预留 + 约定/迁移/安全）。
