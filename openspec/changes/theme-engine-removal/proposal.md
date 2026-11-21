# Proposal: theme-engine-removal（P4.3）

## Why

主题/颜色 YAML 引擎是「任意配置化」的最后一块：`Theme`/`ThemeManager`/`ColorScheme`/`ColorTable`/
`ThemeColors`/`NativeTheme`/`GboardColors` 等把一套 YAML 主题模型留着只为生成旧的 `ImeColors` 面板配色；
主题本身早已硬编码（NativeTheme），切换能力不存在，这些代码只有成本没有行为。

## What Changes

- 删除主题/颜色引擎：Theme、ThemeManager、ThemePrefs、ThemeScope、ColorManager、ColorScheme、
  ColorSchemeResolver、ColorTable、ColorKey、ThemeColors、ThemeFonts、BitmapSampling、NativeTheme、
  GboardColors 及主题模型（GeneralStyle/Preedit/ToolBar/Window/MaybeStringList）与相关单测。
- 面板配色改用 `:ui:design` 的 `ImePalette`/`LocalImePalette`（剪贴板/符号/表情/剪贴板 chip），删除
  `ImeColors`/`GboardStyle`。
- 输入视图不再需要 `ThemeScope`：`ComposeInputView(service, rime)`；符号页数据来自
  `LiquidAssets.liquidKeyboard`（JSON 数据，主题无关）。
- 服务去掉主题初始化/颜色监听/延迟创建视图路径：`onCreateInputView` 直接创建；导航栏管理器改为
  palette light/dark 驱动，删除 navbar 主题偏好。
- `ComposeInputView` 补齐 `StatusMessage` 广播（此前 ascii/选项状态可能停留在初值，语言 chip 会显示错）。

## 验证

- 键盘/候选/中英切换、剪贴板/符号/表情面板、设置页正常；面板配色与键盘一致。
- 全仓闸门；随包 YAML 仅引擎数据，应用层无主题/颜色解析。
