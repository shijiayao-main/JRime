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
