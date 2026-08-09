# 设计文档索引

未来会话先读本文件，按需加载对应文档，避免整篇读取浪费上下文。

| 文档 | 内容 | 谁该读 |
|---|---|---|
| `frontend-design.md` | 前端技术设计（当前状态/架构/路由/领域逻辑/待办） | 前端开发会话 |
| `backend-design.md` | 后端技术设计（技术栈/模块/API/表结构/一致性策略） | 后端开发会话 |
| `database-design.md` | 数据库设计（表/字段/索引/迁移/约定） | 后端开发会话 |
| `../mockups/bazi-app-mockups.md` | 产品图、交互原型、设计系统规范 | 产品/UI 迭代会话 |
| `../mockups/design-philosophy.md` | 视觉哲学「朱墨星图」 | UI 相关会话 |
| `../../contracts/openapi.yaml` | 前后端共享 API 契约（**唯一真源**） | 前后端联调 |
| `../../contracts/fixtures/bazi-cases.json` | 排盘边界用例（一致性回归基准） | 前后端算法联调 |

## 使用约定

1. **按需读取**：前端会话只读 `frontend-design.md`，后端会话只读 `backend-design.md`；跨端问题先查 `contracts/`。
2. **用 `rg` 定位**：找关键词（如"真太阳时""神煞"）用 `rg -n 关键词 docs/`，不要整篇通读。
3. **已交付历史看 git log**：设计文档只保留"当前状态 + 决策 + 待办"，不堆叠迭代过程。
4. **变更日志从简**：每个文档只留最近一条版本记录，历史由 git 负责。

## 前后端分离红线

- `apps/web` 与 `apps/server` **互不 import 源码**；共享信息只通过 `contracts/`（openapi + fixtures）。
- 接口字段以 `contracts/openapi.yaml` 为准；各端内部类型自行定义，但字段名与契约一致（camelCase）。
- 排盘正确性以 `contracts/fixtures/bazi-cases.json` 为共同基准，前后端各跑一份一致性测试。
- 前端第一版保留神煞/合婚/运势规则（`lib/`）；后端第一版不重复实现，只做排盘核心 + 记录存储。
