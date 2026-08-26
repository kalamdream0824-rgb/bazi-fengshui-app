# 八字命理 · 排盘 App

一个基于 Web 的八字（四柱）排盘应用，前后端分离架构。前端采用 React 19 + TypeScript，设计语言「朱墨星图」，信息架构参考问真八字（非品牌抄袭）。

当前页面命书已开放事业、财富与感情主题。感情通俗版按单身、交往中、已婚三种阅读角度生成固定三年内容，生成后保存到“我的命书”；当前不提供感情专业版和 PDF 导出。

## 仓库结构

```
bazi-fengshui-app/
├─ apps/
│  ├─ web/          # 前端应用（Vite + React 19 + TS）
│  └─ server/       # 后端服务（Spring Boot 3 + Java 17 + MyBatis-Plus + H2）
├─ contracts/       # 前后端共享契约：OpenAPI + 测试夹具
├─ docs/            # 设计文档（产品图、前端技术设计、设计哲学）
└─ README.md
```

## 快速开始（前端）

```bash
cd apps/web
npm install
npm run dev
```

打开 http://localhost:5173 即可使用。默认 `VITE_API_MODE=mock`，排盘数据由 lunar-javascript 本地真算；后端就绪后切换为 `http` 模式（见前端技术设计文档）。

## 常用命令（apps/web）

```bash
npm run dev       # 开发服务器
npm run build     # 类型检查 + 构建
npm test          # Vitest 单元测试
```

## 文档

- 产品图文档：`docs/mockups/bazi-app-mockups.md`
- 前端技术设计：`docs/design/bazi-frontend-design.md`
- 设计哲学：`docs/mockups/design-philosophy.md`
- 感情命书 v1 算法与验收：`docs/design/relationship-report-v1.md`

## 网络说明

如 npm 安装缓慢，可临时使用本地代理：

```bash
npm config set proxy http://127.0.0.1:7890
npm config set https-proxy http://127.0.0.1:7890
```

## 快速开始（后端）

```bash
cd apps/server
mvn spring-boot:run    # http://localhost:8080
```

前端联调：`apps/web` 下设置 `VITE_API_MODE=http` 后 `npm run dev`（`/api` 已代理到 8080）。

可选 MySQL（Docker）：

```bash
cd apps/server
docker compose up -d
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run
```
