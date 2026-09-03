# 真太阳时后端权威链路验收记录

日期：2026-09-04  
范围：排盘记录、文字命书、单份命书结算、前端 HTTP 模式与 Mock 模式。

## 结论

`solarDateTime` 现在始终表示用户输入的原始民用钟表时间。HTTP 前端不再改写该字段，也不再关闭 `trueSolarTime`；后端在进入 lunar-java 前统一完成一次真太阳时校正。排盘、报告与结算复用同一 `BaziService`，保存的请求 JSON 保留原时间、地点和开关，响应 `trueSolar` 记录实际计算依据。

## 算法口径

- 时区基准：中国标准时间 UTC+8，标准经线东经 120°。
- 经度修正：`4 × (城市经度 - 120)` 分钟。
- 均时差：NOAA fractional-year 近似公式，闰年使用 366 天分母。
- 有效出生时间：原始民用时间 + 经度修正 + 均时差，合计秒数四舍五入。
- 地点：必须为完整的“省级行政区 城市”；四个直辖市按市中心经度。无法解析时失败关闭，不回退省会。

## 验收向量

| 场景 | 输入 | 预期 | 自动化证据 |
|---|---|---|---|
| 关闭校正 | 1995-10-08 13:05，深圳，关闭 | 有效时间等于原时间，`trueSolar=null` | `BirthTimeResolverTest.disabledCorrectionReturnsOriginalTimeWithoutMetadata`、`BaziServiceTest.disabledTrueSolarTimeKeepsOriginalTimeAndNoMetadata` |
| 开启但不跨时辰 | 1995-10-08 14:30，深圳，开启 | 原时辰与校正后均为未时 | `BirthTimeResolverTest.correctionKeepsHourBoundaryWhenBothTimesRemainWei` |
| 深圳跨时辰 | 1995-10-08 13:05，深圳，开启 | 1995-10-08 12:53:51；未时变午时；时柱丙午 | `BirthTimeResolverTest.shenzhenCorrectionCrossesFromWeiToWuHour`、`BaziApiIntegrationTest.recordUsesServerResolvedTrueSolarTimeAndReturnsAuditMetadata` |
| 跨公历日期 | 2024-02-29 00:05，乌鲁木齐，开启 | 2024-02-28 21:42:23 | `BirthTimeResolverTest.correctionCanCrossToPreviousCivilDate`、前端 `trueSolarTime.test.ts` 同向量 |
| 地点无法解析 | 广东省 火星市，开启 | HTTP 400 `BIRTH_PLACE_UNRESOLVED`；报告、订单、关联记录均为 0 | `ReportCheckoutFailureIntegrationTest.unresolvedTrueSolarPlaceCreatesNeitherReportNorOrder` |
| 报告一致性 | 深圳跨时辰原始输入 vs 显式有效时间 | 两份报告正文完全一致；保存的请求仍为 13:05 且开关为 true | `SavedReportIntegrationTest.reportUsesTheSameServerResolvedTimeWhilePreservingOriginalRequest` |
| 前端 HTTP 原样提交 | 深圳跨时辰原始输入 | `/records` 请求体保持 13:05、深圳、true | `HttpBaziApi.test.ts`，以及 `reportApi.test.ts`、`payApi.test.ts` |

## 失败与扣费边界

地点或出生时间无法解析时，异常发生在报告落库、会员成功次数记账、订单和报告订单关联创建之前。单份购买不会出现“先建订单、后发现算不了”的收费风险；会员直出也不会因失败消耗每日三次额度。

## 页面验收

- 开启真太阳时时，必须选完省、市两级后才能提交。
- 输入页明确当前支持中国大陆市级地点，不支持县区、乡镇、台湾和境外校正。
- 结果页展示原时间、校正后时间、采用经度、经度修正、均时差和合计修正。
- 跨时辰或日期时，用高亮文案明确命盘及命书已按校正后时间计算。

## 回归命令

```bash
cd apps/server && ./mvnw test
cd apps/web && npm test
cd apps/web && npm run lint
cd apps/web && npm run build
```

上述命令必须全部通过，才可视为本链路验收完成。

## 本次执行结果

- 后端：528 项测试通过，0 失败。
- 前端：45 个测试文件、212 项测试通过，0 失败。
- 前端 ESLint：0 错误、0 警告。
- 前端生产构建：通过。
- HTTP 浏览器联调：前端原样提交 `1995-10-08T13:05 / 广东省 深圳市 / true`；结果页显示校正后 `12:53`、未时变午时；命书封面时柱为丙午；财富命书完成模拟单份购买并进入已保存详情页。
