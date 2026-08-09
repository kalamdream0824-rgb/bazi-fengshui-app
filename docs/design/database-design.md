# 八字排盘 App · 数据库设计文档（v0.1）

| 项目 | 内容 |
|---|---|
| 版本 | v0.1（与后端 v0.5 对齐） |
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

### 1.3 预留：orders（订单）

支付待办（需企业资质）后创建：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | 订单号（对外可加业务单号） |
| user_id | BIGINT NOT NULL | 下单用户 |
| plan | VARCHAR(32) NOT NULL | 套餐/商品标识（如 `report_full` / `member_3m`） |
| amount_cents | INT NOT NULL | 金额（分） |
| status | VARCHAR(16) NOT NULL | `pending / paid / refunded` |
| provider | VARCHAR(16) NULL | 支付渠道（wechat / alipay） |
| provider_trade_no | VARCHAR(64) NULL | 渠道交易号（回调对账用） |
| created_at / paid_at | TIMESTAMP | 创建/支付时间 |

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

- v0.1（2026-08-09）：建立数据库设计文档（用户/记录表 + 订单预留 + 约定/迁移/安全）。
