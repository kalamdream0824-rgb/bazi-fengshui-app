# 八字排盘 App · 后端技术设计（v0.1）

| 项目 | 内容 |
|---|---|
| 版本 | v0.7（模拟支付全流程已交付） |
| 日期 | 2026-08-09 |
| 关联 | 契约 `contracts/openapi.yaml`；前端 `frontend-design.md`；文档索引 `README.md` |

## 1. 定位与范围（第一版）

- ✅ 排盘 API：lunar-java 出盘，响应与前端 `PaipanResult` 字段对应（camelCase）；**fixtures 一致性测试通过**（lunar-java vs lunar-javascript 关键字段一致）。
- ✅ 历史记录存储：`bazi_record` 表 + POST/GET/GET-by-id 接口。
- ✅ 账号体系：注册/登录（BCrypt 密码哈希 + JWT），记录按用户隔离（`user_id`），创建去重，支持 DELETE。
- ✅ **P0 加固**：配置分层（dev/prod profile，JWT secret 环境变量注入，prod 必填）；CORS 可配置；登录失败限流（5 次锁 5 分钟，429）；MockMvc 集成测试覆盖认证/去重/隔离/删除全链路。
- ✅ **P1**：MySQL 正式化——`docker-compose.yml`（mysql:8.4，自动建表）+ `application-mysql.yml` 本地 MySQL profile（prod 用 `application-prod.yml` + 环境变量）；统一请求日志过滤器（方法/URI/状态/耗时/用户，慢请求 >500ms 单独 WARN）。
- ✅ **P2-会员权益 + 兑换码已交付**：`bazi_redeem_code` 兑换码表 + `bazi_user` 增加 `plan/member_expire_at`；`GET /api/v1/me` 会员状态、`POST /api/v1/redeem` 兑换（大小写不敏感、顺延到期、记录订单 `provider=redeem`）；前端"我的-会员状态"卡接入。
- ✅ **P2-模拟支付打通全流程已交付**：`POST /api/v1/orders` 创建 pending 订单（套餐校验 + 金额来自 `MemberPlans` 常量，业务侧示例价 30 天 ¥29.9 / 90 天 ¥68）→ `POST /api/v1/pay/mock-success/{orderId}` 模拟支付回调（仅非生产环境，`app.pay.mock-enabled` 控制；**幂等**：订单已 paid 不重复顺延到期）→ 会员即时开通。`POST /api/v1/pay/callback` 为真实渠道回调占位（当前明确拒绝：`PAY_CALLBACK_NOT_READY`），资质就绪后接入微信/支付宝，订单与会员逻辑无需改动。
- ✅ 环境：Maven 3.9.11 已装；开发库 H2（MySQL 切换仅改 datasource 配置）。
- ⚠️ 神煞 / 真太阳时：**后端第一版不实现**——前端 `HttpBaziApi` 用与 Mock 同一套规则补充（`enrichResult` / `adjustRequestForTrueSolar`），保持结果一致；后续需要可下沉。
- ⏸️ 合婚 / 运势文案：前端保留，不在本端实现。
- ✅ 账号 / 云同步：已交付（v0.3）。
- ⏸️ **真实支付通道：待办**——需企业主体 + 商户号 + 备案域名才能接微信/支付宝；当前用模拟支付打通全流程，资质就绪后接 `POST /api/v1/pay/callback`（订单表已含 `provider/trade_no/paid_at`，无表结构变更）。
- ⏸️ 部署域名 / 会员权益点：M1.5 后续阶段。

## 2. 技术栈

| 项 | 选择 | 说明 |
|---|---|---|
| 语言/运行时 | Java 17 LTS | 本机已装 |
| 框架 | Spring Boot 3.x | 稳定版即可 |
| 构建 | Maven | 需先安装（当前机器无 mvn） |
| ORM | MyBatis-Plus | 用户已确认 |
| 数据库 | MySQL（开发用 Docker；H2 备选起步） | 生产 MySQL |
| 排盘库 | `com.github.6tail:lunar`（lunar-java） | 与前端 lunar-javascript 同源算法 |

## 3. 目录结构（apps/server）

```
apps/server/
├─ pom.xml
└─ src/main/java/com/bazi/app/
   ├─ BaziApplication.java
   ├─ controller/   # BaziRecordController（/api/v1/records）
   ├─ service/      # 排盘计算 + 记录服务
   ├─ mapper/       # MyBatis-Plus Mapper
   ├─ domain/       # 实体（BaziRecord）
   ├─ dto/          # 请求/响应 DTO（字段对齐契约）
   └─ config/       # CORS、异常处理
```

## 4. API（契约为准）

- `POST /api/v1/auth/register`、`POST /api/v1/auth/login`：返回 `{ token, username }`。
- `POST /api/v1/records`：请求 `PaipanRequest` → 排盘并保存 → 返回 `PaipanResult`。
- `GET /api/v1/records`：当前用户记录列表（新→旧）。
- `GET /api/v1/records/{id}`：单条记录。
- `DELETE /api/v1/records/{id}`：删除本人记录。
- `GET /api/v1/me`、`POST /api/v1/redeem`：会员状态 / 兑换码开通（`Bearer`）。
- `POST /api/v1/orders`：创建会员套餐订单（请求 `{ plan }`，套餐码 `member_1m` / `member_3m`）→ 返回 `OrderDto`（pending）。
- `POST /api/v1/pay/mock-success/{orderId}`：模拟支付成功回调（需 `Bearer`，仅非生产环境）→ 幂等开通会员，返回 `MembershipInfo`。
- `POST /api/v1/pay/callback`：真实支付渠道回调占位（无需 token，当前返回 `PAY_CALLBACK_NOT_READY`）。
- `/records/**` 需 `Authorization: Bearer <token>`；401 返回 `{code:"UNAUTHORIZED"}`。
- 错误响应统一 `{ "code": string, "message": string }`；入参 `@Valid` 校验。
- 开发期 CORS 允许 `http://localhost:5173`。

## 4.5 套餐与订单状态机

- 套餐常量集中在 `domain/constants/MemberPlans`（`code / days / amountCents`）；金额由后端定价，前端只展示，不做价格输入。
- 订单状态：`pending → paid`（模拟回调或兑换落账）；`provider`：`mock` / `redeem` / 未来 `wechat` / `alipay`。
- 幂等规则：同一笔订单重复回调只开通一次；同一 `provider + provider_trade_no` 已落账不重复顺延。
- 会员开通逻辑统一走 `MembershipService`（`redeem` / `payOrder` / `grantMembership`），到期时间取「当前时间与已有到期时间较大者」顺延。

## 5. 数据模型

详细字段、索引与迁移策略见 [database-design.md](./database-design.md)。

```sql
CREATE TABLE bazi_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,          -- 预留：账号体系接入后使用
  request_json TEXT NOT NULL,   -- PaipanRequest
  result_json TEXT NOT NULL,    -- PaipanResult
  created_at DATETIME NOT NULL
);

CREATE TABLE bazi_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL
);
```

前端登录后自动将本地 IndexedDB 历史批量上传（后端按 `request_json` 去重），实现"本地 → 云端"迁移。

## 6. 一致性策略（命根子）

- 读取 `contracts/fixtures/bazi-cases.json`（6 组：普通男/女、立春前后、闰二月、晚子时）。
- Java 侧断言关键字段：`lunarText / shengXiao / pillars.*.ganZhi / shiShen / naYin / wuXing / daYun[*].ganZhi`。
- 真太阳时：与前端同一公式（经度差 + NOAA 均时差），出生地经度表口径需对齐（前端内置 356 城，后端可复用同一数据集或等价来源）。
- 任一侧跑挂即阻断合并（CI 两侧各跑 fixtures 测试）。

## 7. 合规

- 接口只输出数据；免责声明与"温和参考"文案由前端呈现层负责。
- 不输出医疗、投资、婚姻等确定性建议。

## 7.5 分层与耦合规范（后续开发强制约定）

- 依赖方向单向：`Controller → Service → Mapper`；Controller 只做 HTTP 参数接收与响应，**业务规则（去重/校验/状态流转）不进 Controller**。
- 业务错误统一抛 `BusinessException(code, message)`，由 `GlobalExceptionHandler` 映射；**禁止用通用异常（如 IllegalArgumentException）表达业务语义**。
- 领域常量（五行表等）放 `domain/constants`；日期/字符串解析等工具独立成 `util`，不散落在 Service。
- 纯领域层（如 BaziCalculator / Assembler 拆分）：**等出现第二个消费方或 Service 明显膨胀（>200 行）再拆**，避免过早抽象（Rule of Three）。
- 新增接口时对照本规范自查：Controller 是否超过 10 行业务逻辑、是否用了通用异常、业务常量是否留在 Service。

## 8. 变更日志

- v0.7（2026-08-11）：模拟支付打通全流程交付——订单创建 + 模拟支付回调 + 幂等开通会员（30/90 天套餐常量、`/orders`、`/pay/mock-success/{orderId}`、`/pay/callback` 占位；后端测试 19/19）。
- v0.6（2026-08-10）：会员权益 + 兑换码交付——兑换码表/用户会员字段、/me 与 /redeem 接口、前端会员卡与兑换输入（后端测试 12/12，前端 77/77）。
- v0.5（2026-08-09）：P1 交付——MySQL（Docker Compose + mysql profile）、请求日志过滤器（后端测试 8/8）。
- v0.4.1（2026-08-09）：解耦最小修正——新增 `BusinessException` 业务异常体系、去重/保存下沉 `RecordService`（Controller 瘦身）、五行常量归入 `domain/constants`；立"分层与耦合规范"为后续强制约定。
- v0.4（2026-08-09）：P0 交付——配置分层与安全底线（secret 环境变量、CORS 可配置、MySQL 驱动）、登录防爆破、控制器集成测试（后端测试 8/8）。
- v0.3.1（2026-08-09）：标记会员/支付为待办（资质前置），当前不实现支付通道。
- v0.3（2026-08-09）：账号 + 云同步交付——注册/登录（JWT + BCrypt）、记录按用户隔离与去重、DELETE 接口；前端登录页与历史云同步（后端测试 5/5，前端 64/64）。
- v0.2（2026-08-09）：骨架交付——排盘服务 + 记录接口 + fixtures 一致性测试通过；明确神煞/真太阳时由前端补充。
- v0.1（2026-08-09）：建立后端技术设计基线（范围收敛、技术栈、目录、API、表结构、一致性策略）。
