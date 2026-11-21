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

## P3b（2026-11-21）：legacy 视图层删除，Compose 输入视图自立

- 删除 ~5k 行 legacy：`ime/window/{BoardWindow,BoardWindowManager,ResidentWindow}`、
  `ime/keyboard/{KeyboardWindow,Keyboard,Key,CommonKeyboardActionListener,KeyboardActionListener,KeyAction,
  KeyboardPrefs,KeyboardResolve}`、`ime/clipboard/ClipboardWindow`、`ime/symbol/LiquidWindow`、
  `ime/phrase/PhraseWindow`、`core/{InputView,BaseInputView}`、`ime/compose/{GboardKeyboard,GboardKey,
  KeyboardPanel}` 及过时单测（AuxCode/KeyPopup/KeyboardFolding/KeyboardTopRow/DoubleSpaceTap/KeyAction/
  KeyboardResolve）。
- `ComposeInputView` 改为直接 `ConstraintLayout`：自有 Rime 消息泵（composition/options/schema/bulk
  candidates）、`handleMessages`、`startInput/updateSelection/refreshColors/trimMemory/bandTopY`；
  `InputBroadcaster`/controller/executor 本地创建（去掉 DI）；`InputDeviceManager`/`TrimeInputMethodService`
  类型收敛。
- 面板/键盘切换：controller 用 `ActionResolver` 把 key token 译为 `SwitchKeyboard`/`OpenPanel`/`ClosePanel`，
  纯状态驱动；`PanelSlot` 增加 Emoji 独立槽（SymbolsPanelState 初始 emoji tab）。
- Rime `_liquid_keyboard` 选项 → 打开 Symbols 面板；schema 切换 → 关闭面板。
- 模拟器验证：键盘渲染/中英切换/输入、数字键盘切换、剪贴板/表情/符号面板均正常（截图归档）。
- 仍待 P4：dead prefs（hookShift/landscape 等）、legacy 颜色/主题类（ImeColors/ColorManager/NativeTheme）、
  design token 统一。
