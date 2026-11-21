# Implementation Notes — settings-v2（阶段 1）

## 2026-11-21

- `AppPrefs`：删除 7 个零消费者偏好（vibrate_on_key_release / vibrate_on_key_repeat /
  swipe_velocity / double_tap_timeout / slide_step_size / hook_shift_arrow /
  clipboard_mask_sensitive）；新增 `Appearance` 组（深浅模式/强调色/键面提示/候选单页/候选字号），
  原虚拟键盘页的三项迁入；DE 存储同步纳入外观组。
- 新页面：`AppearanceSettingsFragment` + `NavigationRoute.Appearance` + 首页「外观」入口
  （palette 图标）+ 中英文案。
- 表面：引擎 `AccentPreset` + `ImeCommand.SetAccent`（含单测）；`ui:design` 新增
  `ImeAccent`（蓝/绿/紫 × 浅/深）与 `ImePalette.withAccent`；`ImeSurface` 按状态选色板并覆盖
  `enter`/`accent`/`candidateHighlighted`；controller 首帧读取外观偏好；`ComposeInputView`
  的偏好 listener 实时派发 `SetPaletteMode`/`SetAccent`/`SetCandidatePageSize`，
  候选字号/提示开关直接驱动 config 重组。
- 模拟器验证：外观页五项默认值正确；强调色改为绿色后回车键/强调色即时变绿
  （`screenshots/emulator-2026-11-21-appearance-page.png`、`...-green-accent-keyboard.png`）。
- 全仓闸门：`test + :app:assembleDebug + spotlessCheck` 绿。

## 阶段 2（未开始）

`:ui:settings` Compose 六组页面、`SettingSpec` 注册表与无孤儿断言、设置搜索、
删除 14 项无效可见项与两套深色概念。
