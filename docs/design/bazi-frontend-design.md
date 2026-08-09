# 八字排盘 App · 前端技术设计文档（概要设计）

| 项目 | 内容 |
|---|---|
| 文档版本 | v0.11（已实现：M0.5 + M0.8 + 表单组件 + 真太阳时 + A2 神煞 + A1 命书 PDF + A3 合婚规则完善 + A4a 本地历史 + B0 工程加固 + C1 + C2 真实黄历/运势文案） |
| 日期 | 2026-08-08 |
| 适用范围 | 前端（apps/web）开发 |
| 关联文档 | [产品图文档](/Users/lijialin/Documents/Codex/2026-08-07/wo/outputs/bazi-app-mockups.md)、[PRD](/Users/lijialin/Documents/Codex/2026-08-07/wo/outputs/bazi-fengshui-app-PRD.md)、[设计哲学](/Users/lijialin/Documents/Codex/2026-08-07/wo/outputs/design-philosophy.md) |

---

## 1. 背景与目标

- 产品形态：Web 起步的八字排盘 App，后续可迁移微信小程序；前后端分离。
- 当前阶段：前端已完成 M0.5（工程化 + 排盘输入/基本排盘）与 M0.8（其余模块补齐），**8 个模块全部可走通**；后端（Spring Boot + Java 17 + MyBatis-Plus + MySQL）待启动。
- 前端设计原则（已落实）：
  1. 设计系统基于「朱墨星图」哲学与产品图文档令牌实现；
  2. **契约先行**：排盘数据层抽象为 `BaziApi`，开发期 `MockBaziApi`（lunar-javascript 真算），后端就绪后切 `HttpBaziApi`，页面零改动；
  3. 排盘正确性前置：测试夹具 + 单测基线（当前 15 用例全绿）。

## 2. 总体架构

```mermaid
flowchart LR
  subgraph repo[仓库根目录]
    W[apps/web —— 已完成 M0.5/M0.8]
    C[contracts —— OpenAPI 契约 + 测试夹具]
    S[apps/server —— 待启动 Java 后端]
    D[docs —— 设计文档]
  end
  W -->|开发期 MockBaziApi| B[lunar-javascript]
  W -->|上线 HttpBaziApi| API[REST /api/v1]
  API --> S
  C -->|夹具| W
  C -->|契约| S
```

要点：
- `apps/web` 纯前端，不依赖后端即可运行；
- 排盘结果形状（`PaipanResult`）由 contracts 定义，Mock 与 Http 实现输出同一形状；
- 夹具（`contracts/fixtures/bazi-cases.json`，6 组边界用例）为语言无关 JSON，未来用于前后端一致性回归。

## 3. 技术选型（实际版本）

| 领域 | 选择 | 状态 |
|---|---|---|
| 构建 | Vite 8.2 + React 19.2 | ✅ 已装 |
| 语言 | TypeScript strict（tsconfig 已移除 baseUrl，paths 用相对路径） | ✅ |
| 路由 | react-router-dom 7.18 | ✅ |
| 服务端状态 | @tanstack/react-query 5.101 | ✅（排盘输入/合婚乙方用 useMutation） |
| 客户端状态 | zustand 5.0.14 | ✅（bazi / toast / settings 三个 store） |
| 样式 | CSS Variables（tokens.css）+ 全局 app.css | ✅（见 7 节偏差记录） |
| 测试 | vitest 4.1 + jsdom + @testing-library/jest-dom 7 | ✅ 15 用例 |
| 排盘计算（开发期） | lunar-javascript 1.7.7 | ✅ 仅存在于 MockBaziApi/lib |
| Node 类型 | @types/node | ✅ |

## 4. 仓库布局（实际）

```
bazi-fengshui-app/
├─ apps/web/
│  ├─ index.html / vite.config.ts / package.json
│  └─ src/
│     ├─ main.tsx                    # QueryClientProvider + 样式入口
│     ├─ app/App.tsx                 # BrowserRouter + 路由 + TabBar/Toast
│     ├─ pages/                      # 8 个页面（全部实现）
│     ├─ components/                 # 11 个设计系统组件
│     ├─ services/                   # baziApi 接口 + Mock/Http 实现
│     ├─ lib/                        # baziMapper / compRules / wuxing
│     ├─ store/                      # useBaziStore / useToastStore / useSettingsStore
│     ├─ types/                      # bazi.ts + lunar-javascript.d.ts + env.d.ts
│     ├─ styles/                     # tokens.css + app.css
│     └─ tests/setup.ts
├─ contracts/
│  ├─ openapi.yaml                   # 排盘契约草案
│  └─ fixtures/bazi-cases.json       # 6 组夹具（立春/闰月/子时等）
└─ docs/
```

## 5. 路由与导航（实现状态）

| 路径 | 页面 | 底部导航 | 状态 |
|---|---|---|---|
| `/` | 首页（今日/明日切换、快捷入口、最近排盘） | 显示 | ✅ M0.8 |
| `/input` | 排盘输入（表单 + 真太阳时开关） | 显示 | ✅ M0.5 |
| `/chart` | 基本排盘（四柱表 + 释义 + 五行） | 隐藏 | ✅ M0.5 |
| `/chart/pro` | 专业细盘（大运/流年/十神/神煞页签） | 隐藏 | ✅ M0.8 |
| `/comp` | 八字合婚（双人排盘 + Mock 规则） | 隐藏 | ✅ M0.8 |
| `/daily` | 每日运势（今日/明日） | 隐藏 | ✅ M0.8 |
| `/report` | 命书报告（封面/目录/样张） | 隐藏 | ✅ M0.8 |
| `/profile` | 我的（命盘/设置/占位项） | 显示 | ✅ M0.8 |

规则：顶层三页显示底部导航；子页面返回键 `navigate(-1)`；无命盘时跳转 `/input`。

## 6. 数据契约与领域逻辑（实现）

- `PaipanRequest` / `PaipanResult` / `Pillar` / `DaYun` 与 `contracts/openapi.yaml` 一致；`lib/baziMapper.ts` 负责 lunar-javascript → 契约形状映射。
- 开发期 Mock 行为：真实排盘 + 220ms 模拟延迟；`VITE_API_MODE=http` 切换 `HttpBaziApi`（POST `/api/v1/records`）。
- 新增领域逻辑：
  - `lib/compRules.ts`：生肖六合（鼠牛/虎猪/兔狗/龙鸡/蛇猴/马羊）+ 日主五行生克 + 五行互补 → 婚配指数（示例规则，60 基础分 ± 加分，0-100 截断）；
  - `lib/baziMapper.getGanZhiFor(offset)`：今日/明日干支真实推算；
  - `store/useSettingsStore`：真太阳时默认开关，localStorage 持久化，排盘页联动；
  - `components/RegionSelect`：出生地省→市联动下拉（`china-division` 民政部数据，34 省级 + 400+ 地级市，含港澳台）；
  - `components/DateTimePicker`：出生时间底部滚轮选择（年/月/日/时/分五列 + 时辰提示，无手动输入）；
  - `lib/trueSolarTime.ts`：真太阳时真实校正——内置 356 城市经度（省会兜底）+ 经度差 + NOAA 均时差，结果记入 `PaipanResult.trueSolar` 并在结果页展示。
- 待补：流年解读与运势文案（当前为示例占位）；神煞当前覆盖四柱 20 个高频，**大运流年神煞待扩展**；A4a 本地历史 + 导出/导入备份待实现；择日/分享/会员为占位；路由级代码分割与 CI 待收尾。
- **待办：时辰边界提示**——当真太阳时校正跨越时辰/节气边界时，结果页高亮提示"该时间经校正后时辰发生变化"，避免用户困惑。

## 7. 设计系统落地（含偏差记录）

- 令牌：`styles/tokens.css`（色板/五行色/圆角/间距/字体），与产品图文档一致。
- 组件（11 个）：Button(+ButtonRow)、Card(+CardTitle)、SegControl、Switch、TopBar、TabBar、BaziTable(内嵌释义 Sheet)、WuxingBar、DaYunList、Toast。
- 交互规范已落实：按压回弹、页签/分段切换、开关滑动、动效 < 300ms 尊重 reduced-motion、居中文本 `letter-spacing` 等量 `text-indent` 补偿。
- **偏差记录**：v0.1 计划 CSS Modules，实际采用"全局 app.css + 前缀类名"以加速 MVP；后续如需隔离可迁移，组件接口不变。

## 8. 测试与验证（当前状态）

- **30/30 用例通过**：
  - `baziMapper`：6 组夹具（普通男/女、立春前后年柱切换、闰二月、晚子时）+ 结构完整性（五行合计 8、大运排序与当前标记、男女大运顺逆不同）；
  - `compRules`：五行生克/比和、合婚评分与生肖六合识别；
  - `datePicker`：闰年天数、时辰映射、解析往返、年份范围；
  - `DateTimePicker`：面板开合、确定/取消回传；
  - `trueSolarTime`：城市经度解析与省会兜底、均时差近似值、校正数学；
  - 真太阳时端到端：深圳 13:05 校正后由未时（丁未）变午时（丙午）。
- `npm run build`（tsc strict + vite build）通过；存在主包 >500KB 警告（lunar-javascript + 路由未分割），列入剩余工作。
- 浏览器走查：沙箱无法启动浏览器，由用户在 `npm run dev` 验收；后续可接入 `webapp-testing` 技能（Playwright，需沙箱外）。

## 9. 里程碑与剩余工作

### 方案 A 执行计划（2026-08-09 决议）

- **A2 神煞 ✅ 已交付**：前端内置约 20 个高频神煞（桃花/驿马/华盖/羊刃/禄神/天乙贵人/文昌/劫煞/亡神/将星/空亡/孤辰寡宿/红艳/流霞/金舆/魁罡/十恶大败/阴阳差错/孤鸾煞等），以年支/日支 + 日干 + 日柱查法实现，四柱覆盖；**查法来源在代码注释中注明（《三命通会》《渊海子平》惯例）**——"算法透明"差异化的落地；已知命例断言测试。
- **A1 命书 PDF ✅ 已交付**：技术选型 **jsPDF + html2canvas**——jsPDF 内置字体不支持中文、嵌入中文字体成本高，html2canvas 渲染设计稿 HTML 保证中文与版式保真；多页 A4（封面/命盘概览/大运与参考），含水印与免责声明。
- **A3 合婚规则完善 ✅ 已交付**：纳音五行生克、夫妻宫（日支）六合/三合/六冲、年支关系 + 年干五合、五行缺补权重（每项 3 上限 12）；结论标注"参考规则"；新增关系判定单元测试。
- **A4 收敛**：前端只做"本地历史（IndexedDB）+ 导出/导入 JSON 备份"；**部署平台、账号体系、云同步统一归入后端项目（M1.5）**，前端阶段不实施，避免返工。
- **A4a 本地历史 + 导出备份 ✅ 已交付**：IndexedDB（`idb`）存储排盘记录（`request + result + createdAt`）；`/history` 列表页（查看/删除/导出全部/导入备份）；排盘成功自动存档；首页"最近排盘"读取真实历史（前 3 条）；导入按"性别+出生时间+姓名"去重并校验备份格式；数据结构兼容未来云同步（L3）。测试覆盖增删查、导出→导入往返、去重、非法文件。

| 里程碑 | 状态 |
|---|---|
| M0.5 工程化 + 排盘输入/基本排盘 | ✅ 已交付（提交 db121ff） |
| M0.8 其余 6 模块 + 首页/细盘完善 | ✅ 已交付（提交 9c0de4a） |
| 表单组件 + 真太阳时（省市下拉/时间滚轮/经度均时差校正） | ✅ 已交付（提交 ed8991d） |
| M1.5 后端联调 | ⏳ 待启动：Spring Boot + lunar-java 出盘；HttpBaziApi 切换；夹具一致性回归 |
| 代码分割 | ⏳ 路由级 lazy 加载，消除 600KB 主包警告 |
| 神煞 / 流年文案 / PDF | ⏳ 后端实现或专项方案 |
| 时辰边界提示 | ⏳ 待办：校正跨边界时结果页高亮 |

## 10. 风险与决策记录（更新）

- 排盘：前端开发期用 lunar-javascript 真算模拟后端 lunar-java，一致性由 contracts/fixtures 回归保证；页面层不直接依赖 lunar-javascript（仅 lib/Mock 引用）。
- 合婚指数为示例规则，已标注 UI；后端可替换为更严谨算法。
- 真太阳时：前端已实现市级经度 + 均时差校正；后端联调时需对齐公式与经度数据口径。
- 沙箱网络授权会中途失效，GitHub 推送/依赖安装需即时执行；不影响用户本机开发。
- 提交署名：历史提交为占位身份，新提交使用 GitHub 身份 `kalamdream0824-rgb`。

## 11. 变更日志

## 12. 代码复用与优化原则（B0 起生效）

- **三次法则（Rule of Three）**：同一领域逻辑或 UI 结构出现 **≥3 处且形态稳定** 才抽取；1-2 处时重复往往比错误抽象便宜，避免提前抽象造成反复重构。
- **真重复 vs 假相似**：领域逻辑（五行映射、时辰、格式化）必须共享；演进方向不同的相似 UI（如首页与每日运势的"今日/明日"切换）不强行合并，避免 prop 膨胀与互相拖累。
- **优化纪律**：优化改动不得引入次要缺陷；每次改动必须伴随充分测试——抽取的组件要有组件测试，受影响页面保持既有用例通过，最终构建必须绿。
- **B0 范围 ✅ 已完成**：抽取真实重复组件（FooterNote / EmptyState / RecordRow，含组件测试）；路由懒加载（React.lazy + Suspense，主包拆分生效）；GitHub Actions CI（push 自动 npm ci + test + build）。**CSS 全量作用域化（CSS Modules）暂缓**——当前无法在沙箱目视验证，纯重构回归风险大于收益，待有浏览器验证环境后另行处理。

## 13. C1 大运流年神煞 + 时辰边界提示 ✅ 已交付

- **时辰边界提示**：真太阳时校正前后时辰不同（如 13:05 → 12:54，未时变午时）时，结果页高亮提示"该时间经校正后时辰发生变化，排盘已按校正后时辰计算"。纯前端，`TrueSolarInfo` 增加 `originalShichen / adjustedShichen / boundaryChanged`。
- **大运流年神煞口径**（已定）：以大运/流年干支对照**命局原局**推算——
  - 三合局类（桃花/驿马/华盖/劫煞/亡神/将星）：以原局年支、日支查；
  - 日干类（羊刃/禄神/天乙贵人/文昌/红艳/流霞/金舆）：以原局日干查；
  - 空亡：以原局日柱旬空查；孤辰/寡宿：以原局年支查。
  - 来源延续 A2 通行口径（《三命通会》《渊海子平》），UI 公示。`DaYun` 增加 `shenSha`，结果增加 `currentLiuNian`（当年干支 + 神煞）；专业细盘"神煞"页签展示流年神煞与大运神煞。
- 实现：`computeExternalShenSha` 对照命局原局推算大运/流年神煞；`TrueSolarInfo` 增加时辰与边界标记，结果页跨边界时红色高亮提示（测试 55/55）。

## 14. C2 真实黄历 / 运势文案 ✅ 已交付（方案 A：温和参考型）

- **数据源**：lunar-javascript 自带黄历能力（`getDayYi/getDayJi/getDayChongDesc/getDaySha/getDayChongShengXiao/getZhiXing/getPengZuGan|Zhi/getDayPositionFu`），前端直接取用，零外部依赖。
- **运势规则引擎** `lib/dailyFortune.ts`：今日天干 vs 日主 → 十神主题文案；今日五行 vs 日主五行 → 基调（生/克/比和）；值星吉凶、冲命主生肖、彭祖百忌 → 星级与注意事项；幸运色取当日五行、方位取福神方位。
- **合规红线（方案 A）**：文案一律"温和参考"语气，不出现绝对化预测（不写"大凶/必发财"）；保留"仅供传统文化研究参考"；无命盘时首页只显示黄历、不做个性化运势；不做医疗/投资/婚姻确定性建议。
- 页面：首页宜忌 chips 换真实黄历（含冲煞）；每日运势页星级/宜忌/指引接真实数据，去掉"示例"标注。
- 实现：`lib/almanac.ts` 取 lunar 黄历（宜/忌/冲/煞/值星/彭祖/福神方位）；`lib/dailyFortune.ts` 规则引擎（十神主题 + 五行基调 + 值星 + 生肖冲 + 彭祖 → 星级/文案/幸运色/方位）；首页与运势页接入，无命盘时仅黄历参考（测试 60/60）。

- v0.1（2026-08-08）：建立前端技术设计基线。
- v0.2（2026-08-08）：整合 M0.5/M0.8 实现状态——8 模块全部可走通；记录实际技术版本、领域逻辑（compRules/settings/getGanZhiFor）、样式偏差、测试与里程碑状态、剩余工作。
- v0.3（2026-08-08）：记录表单组件与真太阳时实现（RegionSelect/DateTimePicker/trueSolarTime，30 用例）；新增"时辰边界提示"待办；更新里程碑与风险记录。
- v0.4（2026-08-09）：记录方案 A 执行计划与决策——A2 神煞（前端内置 20 高频神煞 + 透明公示）、A1 命书 PDF（jsPDF + html2canvas）、A3 合婚完善、A4 收敛为本地历史+备份；部署/账号/云同步归入后端项目。
- v0.5（2026-08-09）：A2 神煞与 A1 命书 PDF 交付（测试 36/36，构建绿）。
- v0.6（2026-08-09）：A3 合婚规则完善进行中（文档先行）。
- v0.7（2026-08-09）：A3 合婚规则完善交付（测试 39/39，构建绿）。
- v0.7.1（2026-08-09）：修正"仍待后端"口径——神煞（四柱）与命书 PDF 已实现；明确剩余待办清单。
- v0.8（2026-08-09）：A4a 本地历史 + 导出备份交付（测试 47/47，构建绿）。
- v0.9（2026-08-09）：B0 工程加固交付——抽取 FooterNote/EmptyState/RecordRow、路由懒加载、GitHub Actions CI（测试 51/51，构建绿）。
