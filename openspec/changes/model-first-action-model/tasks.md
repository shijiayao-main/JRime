# Tasks

- [x] 1.1 `ImeAction` typed model（覆盖按键/上屏/面板/键盘/开关/翻页/选词/文本编辑/维护/URL）
- [x] 1.2 `ActionResolver` 桥接现有布局 token（含 3 个文本宏与多字符字面量）
- [x] 1.3 `JrimeExtension` + `ExtensionRegistry`（actions/keyboards/panels/inputRules，冲突可测）
- [x] 1.4 测试：随包布局槽位全覆盖、Unsupported 空；registry 合并/冲突
- [x] 1.5 `doc/model-first.md` 架构与 P1–P5 路线图
- [ ] P2 应用执行器改用 typed action，删除字符串命令分支
- [ ] P3 面板去 BoardWindow 化，删除 legacy 视图层
- [ ] P4 删除主题/音效 YAML/死偏好，统一 design token
- [ ] P5 示例扩展模块 + 开发说明
