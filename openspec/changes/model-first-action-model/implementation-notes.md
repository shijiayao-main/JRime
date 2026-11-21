# Implementation Notes — model-first-action-model (P1)

## 2026-11-21

- 新增 `com.jiaoay.jrime.action.ImeAction`：Key/Commit/Sequence/SwitchKeyboard/OpenPanel/ClosePanel/
  HideKeyboard/OpenSettings/ShowImePicker/VoiceInput/ToggleOption/PageCandidates/SelectCandidate/
  DeleteCandidate/TextEdit/Maintenance/OpenUrl/Unsupported。
- `ActionResolver`：预设字段（send/command/toggle/commit/text）→ typed action；兼容旧 keysym/字符/
  `Modifier+key` 表示；`{Control+a}{Control+c}` 等宏显式转 `TextEdit` 步骤；多字符字面量（"……"/"♠︎"）→ Commit。
  测试遍历 `KeyLayouts.keyboards` 的所有行为槽位，确认 **Unsupported 为空**（全部随包布局已覆盖）。
- `JrimeExtension`/`ExtensionRegistry`：model-first 扩展面；actions/keyboards/panels/inputRules，
  重复 id/token/panel 显式抛错，测试覆盖。
- `doc/model-first.md` 记录问题判据、目标模块、YAML 边界（引擎数据保留、应用层零 YAML 解析）、
  P1–P5 路线图。
- 下一批 P2：app 执行器直接消费 `ImeAction`，删掉 `CommonKeyboardActionListener` 字符串命令分支；
  随后 P3 面板迁移后删除 BoardWindow/Keyboard/Key/KeyboardWindow 等约 4.7k 行 legacy 代码。

## P2（2026-11-21）：Compose 路径切换到 typed action 执行

- 新增 `RimeActionDispatcher`（app/ime/compose）：直接执行 `ImeAction`：
  Key（含双击空格句号、Shift hook、修饰键状态、导航回退）、Commit、Sequence、SwitchKeyboard、
  OpenPanel/ClosePanel、隐藏/设置/输入法选择/语音、ToggleOption（含组合态 ascii 上屏）、候选翻页/选词/删词、
  TextEdit（复制/剪切/粘贴/全选/撤销/重做/清空）、Maintenance（deploy/updateConfig）、OpenUrl、Unsupported 日志。
- `ImeCommandExecutor`：`ImeCommand.Key` 用 `ActionResolver` 解析成 typed action 后直接 dispatch；
  删除 `CommonKeyboardActionListener` 依赖与字符串命令分支；Backspace/ToggleAscii/候选置顶/光标移动也改 typed；
  面板打开改为显式 `openPanel(PanelId)`（剪贴板窗口/符号/表情/常用语），面板宿主仍是 legacy（P3 移除）。
- `ClipboardChip` 改 `onCommit(text)`，不再构造 legacy `KeyAction`。
- 模拟器验证（emulator-5554）：字母键 → 组合态 + 候选（typed-composing 截图）；点候选 → 字段上屏
  （typed-commit）；双空格 → 句号；点剪贴板图标 → 面板打开（typed-clipboard-panel）；长按空格 →
  切到 Google 语音输入（typed-voice-input）。
- 仍待 P3：删除 `CommonKeyboardActionListener`/`KeyCode`/`Keyboard`/`Key`/`BoardWindow*`/legacy 面板窗口，
  面板改纯 Compose state；P4/P5 按路线继续。
