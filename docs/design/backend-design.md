# 八字排盘 App · 后端技术设计（v0.1）

| 项目 | 内容 |
|---|---|
| 版本 | v0.3（账号 + 云同步已交付） |
| 日期 | 2026-08-09 |
| 关联 | 契约 `contracts/openapi.yaml`；前端 `frontend-design.md`；文档索引 `README.md` |

## 1. 定位与范围（第一版）

- ✅ 排盘 API：lunar-java 出盘，响应与前端 `PaipanResult` 字段对应（camelCase）；**fixtures 一致性测试通过**（lunar-java vs lunar-javascript 关键字段一致）。
- ✅ 历史记录存储：`bazi_record` 表 + POST/GET/GET-by-id 接口。
- ✅ 账号体系：注册/登录（BCrypt 密码哈希 + JWT），记录按用户隔离（`user_id`），创建去重，支持 DELETE。
- ✅ 环境：Maven 3.9.11 已装；开发库 H2（MySQL 切换仅改 datasource 配置）。
- ⚠️ 神煞 / 真太阳时：**后端第一版不实现**——前端 `HttpBaziApi` 用与 Mock 同一套规则补充（`enrichResult` / `adjustRequestForTrueSolar`），保持结果一致；后续需要可下沉。
- ⏸️ 合婚 / 运势文案：前端保留，不在本端实现。
- ✅ 账号 / 云同步：已交付（v0.3）。
- ⏸️ **会员 / 支付：待办（后续再做）**——需企业主体 + 商户号 + 备案域名才能接微信/支付宝；当前不实现支付通道。后续按「方案 3」先做会员权益 + 兑换码，资质就绪后接支付回调（订单表预留 `provider/trade_no` 字段）。
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
- `/records/**` 需 `Authorization: Bearer <token>`；401 返回 `{code:"UNAUTHORIZED"}`。
- 错误响应统一 `{ "code": string, "message": string }`；入参 `@Valid` 校验。
- 开发期 CORS 允许 `http://localhost:5173`。

## 5. 数据模型

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

## 8. 变更日志

- v0.3.1（2026-08-09）：标记会员/支付为待办（资质前置），当前不实现支付通道。
- v0.3（2026-08-09）：账号 + 云同步交付——注册/登录（JWT + BCrypt）、记录按用户隔离与去重、DELETE 接口；前端登录页与历史云同步（后端测试 5/5，前端 64/64）。
- v0.2（2026-08-09）：骨架交付——排盘服务 + 记录接口 + fixtures 一致性测试通过；明确神煞/真太阳时由前端补充。
- v0.1（2026-08-09）：建立后端技术设计基线（范围收敛、技术栈、目录、API、表结构、一致性策略）。
