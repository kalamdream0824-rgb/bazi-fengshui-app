# 八字排盘 App · 前端技术设计文档（概要设计）

| 项目 | 内容 |
|---|---|
| 文档版本 | v0.2（已实现：M0.5 + M0.8） |
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
  - `store/useSettingsStore`：真太阳时默认开关，localStorage 持久化，排盘页联动。
- 仍待后端：神煞（`shenSha` 恒为空数组）、真太阳时校正、流年/运势文案、PDF 导出。

## 7. 设计系统落地（含偏差记录）

- 令牌：`styles/tokens.css`（色板/五行色/圆角/间距/字体），与产品图文档一致。
- 组件（11 个）：Button(+ButtonRow)、Card(+CardTitle)、SegControl、Switch、TopBar、TabBar、BaziTable(内嵌释义 Sheet)、WuxingBar、DaYunList、Toast。
- 交互规范已落实：按压回弹、页签/分段切换、开关滑动、动效 < 300ms 尊重 reduced-motion、居中文本 `letter-spacing` 等量 `text-indent` 补偿。
- **偏差记录**：v0.1 计划 CSS Modules，实际采用"全局 app.css + 前缀类名"以加速 MVP；后续如需隔离可迁移，组件接口不变。

## 8. 测试与验证（当前状态）

- **15/15 用例通过**：
  - `baziMapper`：6 组夹具（普通男/女、立春前后年柱切换、闰二月、晚子时）+ 结构完整性（五行合计 8、大运排序与当前标记、男女大运顺逆不同）；
  - `compRules`：五行生克/比和、合婚评分与生肖六合识别。
- `npm run build`（tsc strict + vite build）通过；存在主包 >500KB 警告（lunar-javascript + 路由未分割），列入剩余工作。
- 浏览器走查：沙箱无法启动浏览器，由用户在 `npm run dev` 验收；后续可接入 `webapp-testing` 技能（Playwright，需沙箱外）。

## 9. 里程碑与剩余工作

| 里程碑 | 状态 |
|---|---|
| M0.5 工程化 + 排盘输入/基本排盘 | ✅ 已交付（提交 db121ff） |
| M0.8 其余 6 模块 + 首页/细盘完善 | ✅ 已交付（提交 9c0de4a） |
| M1.5 后端联调 | ⏳ 待启动：Spring Boot + lunar-java 出盘；HttpBaziApi 切换；夹具一致性回归 |
| 代码分割 | ⏳ 路由级 lazy 加载，消除 600KB 主包警告 |
| 神煞 / 真太阳时 / 流年文案 / PDF | ⏳ 后端实现或专项方案 |

## 10. 风险与决策记录（更新）

- 排盘：前端开发期用 lunar-javascript 真算模拟后端 lunar-java，一致性由 contracts/fixtures 回归保证；页面层不直接依赖 lunar-javascript（仅 lib/Mock 引用）。
- 合婚指数为示例规则，已标注 UI；后端可替换为更严谨算法。
- 沙箱网络授权会中途失效，GitHub 推送/依赖安装需即时执行；不影响用户本机开发。
- 提交署名：历史提交为占位身份，新提交使用 GitHub 身份 `kalamdream0824-rgb`。

## 11. 变更日志

- v0.1（2026-08-08）：建立前端技术设计基线。
- v0.2（2026-08-08）：整合 M0.5/M0.8 实现状态——8 模块全部可走通；记录实际技术版本、领域逻辑（compRules/settings/getGanZhiFor）、样式偏差、测试与里程碑状态、剩余工作。
