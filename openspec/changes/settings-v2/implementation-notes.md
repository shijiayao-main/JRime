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

## 2026-11-21 · 阶段 2 第一批：Compose 设置中心

- 新增 `:ui:settings` 模块：`SettingSpec`/`SettingGroup`/`SettingControl`/`SettingsStore` 契约 +
  `SettingsScreen`（搜索、分组、开关/滑杆/选项弹窗/文本弹窗/动作行），零平台依赖。
- app 侧：`AppSettingsStore`（SharedPreferences 原始读写 + 修订流）、`SettingsSpecsFactory`
  （从既有 `preferenceDelegatesUi` 自动派生 spec，单一事实源，未映射类型隐藏而非丢弃）、
  `SettingsFragment`（ComposeView 宿主 + 导入字体/音效选择动作）。
- 首页五个旧设置入口合并为「设置」；旧 Preference 页面路由保留但不再可达（阶段 2b 清理）。
- 主题：hub 使用品牌蓝（浅 `#1A73E8` / 深 `#8AB4F8`）而非 Material 默认紫。
- 模拟器验证：hub 渲染、分组、滑杆/开关可操作；开关写入 SharedPreferences 生效
  （run-as 读取验证）；截图 `screenshots/emulator-2026-11-21-compose-settings-hub.png`。
- 全仓闸门绿。

### 阶段 2b 待办
六组 IA 重排、无孤儿双向断言、删除旧 Preference 页面与依赖、删除横屏/分屏等无效可见项、
合并两套深色概念、hub 行的 enabled 依赖联动。

## 2026-11-21 · 阶段 2b：六组 IA、无孤儿断言、旧页面清理

- 六组信息架构：输入 / 键盘 / 候选 / 外观 / 剪贴板 / 系统与数据；`SettingsSpecsFactory`
  从偏好声明自动派生并按 key 归类；无法归类的新 key 落键盘组，未映射类型隐藏而非丢失。
- 无孤儿双向断言：`SettingsSpecsFactory` 支持注入 `providers` + `labeler`，配合
  `FakeSharedPreferences` 在 JVM 单测中验证「每个声明偏好都有行 / 每行都指向真实偏好 /
  key 唯一 / 六组齐全 / 外观与系统动作归类」；`AppSettingsStore` 读写与修订流同测。
  全量 203+ 单测通过。
- 删除旧 Preference 页面：General/Keyboard/Appearance/Clipboard/Advanced/Progress fragment
  与对应导航目的地移除；首页仅保留 用户词典 / 配置 / 设置。`launchMainToKeyboard` 改指设置页。
- 遗留无效可见项（横屏/分屏/隐藏工具栏/隐藏键符号/隐藏注解/扩大点击区）在 hub 中隐藏，
  等遗留 Keyboard/Key 模型删除时一并删偏好。
- 模拟器验证：六组顺序与行渲染正常（截图 `screenshots/emulator-2026-11-21-six-groups-*.png`）。
