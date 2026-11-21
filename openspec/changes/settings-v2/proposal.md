# Proposal: settings-v2

## Why

设置是主人点名的四类痛点之一：现状 6 个 Preference 页面、53 个偏好，其中 7 个零消费者、4 个死常量、3 个无 UI、14 项可见但无效或只影响已不渲染的遗留模型；缺主题、候选、方案入口；两套深色概念；「配置」页名不副实。M2 新增的 4 个偏好此前散落在 35 项的虚拟键盘页里，用户找不到。

`doc/master-plan.md` §4.7 的目标是六组信息架构 + `SettingSpec` 声明式单一事实源 + 无孤儿断言。完整 Compose 重写工作量大于一个批次，本变更分两阶段交付：

- **阶段 1（本批）**：清理死设置；把外观/候选/提示类偏好收进新的「外观」页（沿用现有 Preference 基础设施）；新增深浅模式与强调色并接到共享表面；`SettingSpec` 暂以 `AppPrefs` 既有声明式 UI 为等价物，孤儿断言随阶段 2 落地。
- **阶段 2（后续批次）**：`:ui:settings`（Compose）六组页面替换 Preference 页面；`SettingSpec` 注册表 + 无孤儿测试；设置搜索；删除 14 项无效可见项与两套深色概念。

## What Changes

阶段 1：

- 删除 7 个零消费者偏好（`vibrate_on_key_release`/`vibrate_on_key_repeat`/`swipe_velocity`/`double_tap_timeout`/`slide_step_size`/`hook_shift_arrow`/`clipboard_mask_sensitive`）与 4 个死常量。
- 新增 `Appearance` 偏好组与「外观」页面：深浅模式（跟随/亮/暗）、强调色（蓝/绿/紫）、候选单页数量、候选字号、键面提示开关。
- 共享表面接入 `PaletteMode` 与 `AccentPreset`：浅/深色板 + 强调色覆盖按键/候选/回车色；设置变更实时生效。
- 导航新增「外观」入口；`AppPrefs` 的 DE 存储同步纳入新组。

## Capabilities

### New Capabilities

- `settings-ia`：设置的信息架构与偏好生命周期契约（死设置清零、外观组可调、设置变更实时生效；阶段 2 的六组与无孤儿断言在此能力下继续演进）。

### Modified Capabilities

（无。）

## Impact

- `AppPrefs`：删除 7 项 + 新增 Appearance 组；`syncToDeviceEncryptedStorage` 纳入新组。
- UI：新增 `AppearanceSettingsFragment`、导航目的地与首页入口；`ImeSurface` 应用 accent；`ComposeInputView` 监听外观偏好并实时更新。
- 引擎：`AccentPreset` + `ImeCommand.SetAccent`（纯状态）。
- 风险：删除偏好后旧安装残留 key 无迁移（自用可接受，Key 不再被读取）；阶段 2 的 Compose 页面与 Preference 页并存期需要导航兼容。
