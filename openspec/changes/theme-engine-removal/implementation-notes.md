## 2026-11-21

- 删除约 4k 行主题/颜色代码：`data/theme/{Theme,ThemeManager,ThemePrefs,ThemeScope,ColorManager,ColorScheme,
  ColorSchemeResolver,ColorTable,ColorKey,ThemeColors,ThemeFonts,BitmapSampling}`、`theme/{NativeTheme,GboardColors}`、
  `data/theme/model/{GeneralStyle,MaybeStringList,Preedit,ToolBar,Window}`、`ime/compose/ImeColors`、
  `ime/core/InlineSuggestions` 及 8 个主题单测。
- 面板统一读 `:ui:design` 的 `LocalImePalette`；剪贴板删除操作色改 `palette.danger`。
- `SymbolsPanelState` 改接收 `LiquidKeyboard`（`LiquidAssets.liquidKeyboard` 一次性加载）。
- Service：删除 ThemeManager/ColorManager 初始化与监听、`inputViewDeferred`/`pendingEditorInfo` 主题延迟创建，
  `onCreateInputView` 直接建视图；`NavigationBarManager` 只按 palette 明暗切换系统图标。
- `ComposeInputView.handleRimeMessage` 增加 `StatusMessage -> broadcaster.onInputStatusUpdate`，修复语言 chip
  停在 EN 的旧问题（模拟器验证显示 中）。
- 模拟器：键盘/候选/中文输入正常；剪贴板面板打开且配色为设计 token；截图归档。
- 剩余 UI：键盘功能键矢量图、启动图标；P5 示例扩展与开发说明。
