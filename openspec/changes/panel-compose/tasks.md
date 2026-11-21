# Tasks

- [x] 1.1 `ClipboardPanelState`（flow 收集/动作/关闭回调）
- [x] 1.2 `SymbolsPanelState`（tab/键数据/历史/符号 engine round-trip）
- [x] 1.3 面板 composable 改接收状态；`ComposeInputView` 用 remember/DisposableEffect 管理
- [x] 1.4 模拟器验证剪贴板/符号面板
- [ ] P3b：面板状态直接来自 `ImeSessionState.activePanel`；删除 BoardWindow/KeyboardWindow/Keyboard/Key/
      CommonKeyboardActionListener/ClipboardWindow/LiquidWindow/PhraseWindow/InputView 等 legacy 层
- [ ] P3c：删除相关 DI 绑定、偏好与死代码，跑完整闸门
