# 命书样本验收说明

- 固定命例：林先生，男，1995-10-08 14:30，上海，未启用真太阳时。
- 主题：事业与职场。
- 内容版本：`mingshu-2026.08-v1`。
- 通俗版：12 页，保留关键证据、直白解释与反思问题。
- 专业版：17 页，展示完整证据链、规则键、方法注与章节复核。
- 两版共用同一证据引擎与结论，不共享对方的付费表达文本。
- PDF 由 Apache FOP 生成，页面不是截图；自动测试会用 PDFBox 提取中文并确认每页存在文字。

当前本地样本嵌入 macOS `Songti SC Regular`。生产部署前必须在 `apps/server/src/main/resources/report/fonts/` 中补充 OFL 授权的 `NotoSerifSC-Regular.ttf` 与 `OFL.txt`，避免依赖操作系统字体。
