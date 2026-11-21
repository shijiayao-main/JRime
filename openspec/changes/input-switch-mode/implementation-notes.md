# Implementation Notes — input-switch-mode

## 2026-11-21

- 偏好：`AppPrefs.General.ASCII_SWITCH_MODE`（enum `AsciiSwitchMode`），自动派生到设置中心输入组。
- `ImeSurfaceConfig` 新增 `showLanguageToggle`、`shiftTogglesLanguage`；`SurfaceTopBar` 三态都透传，
  `TopBarLanguageAndHide` 按开关渲染 中/EN 按钮。
- `ImeCommandExecutor`：仅顶栏模式下 `ImeCommand.Key(Plain("Shift_L"))` 直接吞掉；其他模式不变。
- 修复外观遗漏：`surfaceConfig` 之前写死 `candidateTextSizeSp = 18f` 且未传 `showKeyHints`/
  `showCandidateComment`，导致设置项对键盘无效；现按偏好注入（偏好监听已有）。
- 验证：`:app:compileDebugKotlin`、app 单测、`:ui:ime:desktopTest`、spotless 绿。

## 追加（同批）：深浅模式合并

- 移除 `AppPrefs.Advanced.UI_MODE`/`UiMode`：`MainActivity` 改为按 `Appearance.PALETTE_MODE`
  设置 `AppCompatDelegate` 夜间模式，并监听偏好变化即时重应用。
- 设置中心不再出现重复的「UI 模式」行，外观组的「深浅模式」同时决定应用主题与键盘配色。
