# Proposal: model-first-action-model (P1)

## Why

主人提出：去掉 Rime 的随意配置性、精简功能、抽取模块；以后扩展按 **Kotlin model** 接入而不是 YAML。
现状 283 个键盘槽位 token、预设表字符串字段、`KeyAction` + `CommonKeyboardActionListener` 的
字符串命令解释器，是 YAML 时代留下的主要冗余源；应用层仍解析用户音效 YAML。

## What Changes（P1）

- `:core:engine` 新增 typed 行为模型 `ImeAction`（Key/Commit/Sequence/SwitchKeyboard/OpenPanel/
  ToggleOption/PageCandidates/SelectCandidate/TextEdit/Maintenance/OpenUrl/Unsupported…）。
- `ActionResolver`：把现有布局槽位解析成 typed action；文本宏（copy_all/cut_all/Clear）转成显式
  `Sequence(TextEdit)`；多字符字面量转 `Commit`。
- 新增 model-first 扩展契约 `JrimeExtension` + `ExtensionRegistry`（actions/keyboards/panels/inputRules，
  冲突显式报错）。
- 测试：随包布局**全部槽位**解析成功、Unsupported 为空；registry 合并/冲突/重复可测。
- 设计文档 `doc/model-first.md`：问题判据、目标模块、YAML 边界、P1–P5 路线图。

## 不做（后续批次）

- 还没把 app 执行器切到 typed action（P2）；还没迁移面板/删除 BoardWindow（P3）；
  还没删主题/音效 YAML/死偏好（P4）；示例扩展模块（P5）。

## Impact

- 新增 core model 与测试；不动运行时 UI（行为桥接仍在 app 旧执行器）。
- 为后续删除 ~5–6k 行 legacy 代码提供类型化基础。
