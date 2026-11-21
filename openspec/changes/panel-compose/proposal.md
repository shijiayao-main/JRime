# Proposal: panel-compose（P3a）

## Why

面板内容早已是 Compose，但仍挂在 legacy `BoardWindow`/`WindowManager` 视图宿主上：窗口类继承
`BoardWindow.BarBoardWindow`、创建无用的 `View`、通过 DI/attach 广播驱动 `PanelId`。这是 YAML/主题时代
的宿主抽象，只为让面板"看起来像窗口"，实际窗口 View 并不渲染（ComposeView 才是输入视图）。

## What Changes（P3a）

- 新增纯 Compose 状态类：
  - `ClipboardPanelState`：历史/收藏两列 live list + paste/pin/unpin/favorite/edit/share/delete/clear；
    面板组合时启动 flow 收集，解散时停止。
  - `SymbolsPanelState`：liquid 页的 tab/键数据、历史页、符号页 engine round-trip；不再依赖窗口。
- `ClipboardPanel`/`LiquidPanel` 改为接收状态对象；`ComposeInputView` 的 `PanelSlot` 用
  `remember` + `DisposableEffect` 创建/销毁状态，关闭面板改为显式 `closePanel()`。
- 仍保留 `BoardWindowManager` 的窗口 attach 事件作为 `activePanel` 状态来源（P3b 再删）。

## 后续（P3b）

- 用 `ImeSessionState.activePanel` 直接驱动面板；删除 `BoardWindow*`、`KeyboardWindow`、`Keyboard/Key`、
  `CommonKeyboardActionListener`、`ClipboardWindow`/`LiquidWindow`/`PhraseWindow`、`InputView/BaseInputView`
  legacy 层（预计 4–5k 行），并移除相关 DI 绑定与偏好。

## 验证

- 模拟器：剪贴板面板（新状态空态/标签/清空）、符号面板（标签页切换后网格正常）均可用；截图归档。
- 构建/单测/spotless 闸门。
