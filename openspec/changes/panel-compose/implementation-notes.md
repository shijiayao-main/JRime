# Implementation Notes — panel-compose（P3a）

## 2026-11-21

- 新状态类（`app/ime/compose/panels/`）：`ClipboardPanelState`（原 ClipboardWindow 的业务方法迁移，
  去掉 BoardWindow/DI/windowManager；`paste` 后按偏好回调关闭面板）、`SymbolsPanelState`（原 LiquidWindow 的
  tab/键/历史/符号 engine round-trip 迁移，`onClosePanel` 替代 attachWindow(KeyboardWindow)）。
- 面板 composable：`ClipboardPanel(state)`、`LiquidPanel(state)`；`ClipBoardList/ClipItemMenu`
  改为接收状态；删掉窗口类型引用。
- `ComposeInputView.panelSlots`：`remember` 创建状态、`DisposableEffect` 启停 flow 收集；
  `closePanel()` 统一发 `ClosePanel`（controller + executor）。
- 模拟器验证：剪贴板面板（标签/清空/空态）与符号面板（常用/emoji/标点…标签，标点网格正常）均工作；
  截图见 `screenshots/`。
- 未做：删除 legacy 窗口层（P3b）；面板打开仍由 BoardWindow attach 事件映射（P3b 改为纯 state）。
