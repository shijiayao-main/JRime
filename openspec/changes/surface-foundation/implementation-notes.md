# Implementation Notes — surface-foundation (M1)

## 2026-11-21 · 第一批（模块地基）

- 新增 `:core:model`、`:core:engine`、`:ui:design`、`:ui:ime` 四个模块；`settings.gradle.kts` 与
  `app/build.gradle.kts` 接线。`RimeProto`/`SchemaItem` 与主题布局纯模型（`TextKeyboard`/
  `PresetKey`/`KeyActionToken`/`KeyLayouts`/`KeyboardBuilder`）迁入 `:core:engine`。
- `:ui:design`：`ImeDimens`/`ImeTypography`/`ImePalette`（浅/深语义色板）/`ImeMotion`/`ImeShapes`，
  数值由 `:core:engine` 的 `ImeMetrics` 单源派生。
- `:core:engine`：键盘模型、`KeyGestureResolver`、`ImeSessionState`/`ImeCommand`/`ImeStateReducer`、
  `KeyBinding`、`KeyboardCatalog`、`KeyboardLayoutOps`；新增 33 个单测。
- 提交：`afa0b56`、`985fe25`（未 push）。

## 2026-11-21 · 第二批（表面接管）

- `:ui:ime` 新表面：`ImeSurface`（带高/调色板/band top 上报）、`KeyboardSurface`（Gboard 5 行结构 +
  fallback 折行）、`KeySurface`（键面/气泡/手势/连发/滑光标）、`SurfaceTopBar`、`CandidateStripView`、
  `ImePanelHost`（面板注册表槽位）。
- app 侧：`ImeSurfaceController`（broadcaster → `ImeSessionState`）、`ImeCommandExecutor`
  （`ImeCommand` → 既有 `KeyAction` 管线）、`ComposeInputView` 切换到新表面；剪贴板 chip 与
  clipboard/liquid 面板以 slot 形式暂留旧实现（旧配色 `LocalImeColors` 由 slot 包装提供）。
- 旧文件删除：`ImeRoot`、`ImeUiState`、`CandidateStrip`；`isAuxActive`/`compositionEchoText`
  纯函数抽到 `LegacySurfaceHelpers` 供既有单测继续覆盖。
- 结构守护：`SurfaceGuardTest`（无平台 import、无 app import、无 dp/sp/Color 字面量）。
- 修正：内容带高改为 qwerty 5 行 / fallback 折行数（消掉旧键盘 5 行、面板 6 行的潜在跳变）。
- 行为口径：M1 行为仍由既有 `KeyAction` 管线执行；typed token 迁移推迟到 `surface-p0`。
