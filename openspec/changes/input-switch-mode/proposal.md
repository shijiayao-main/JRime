# Proposal: input-switch-mode（M3 剩余）

## Why

M3 只剩「中英切换方式」这一项偏好：目前 Shift 单击与顶栏 中/EN 按钮都固定可用，用户无法把
误触 Shift 导致的模式翻转关掉，也无法在只想用 Shift 的人那里隐藏顶栏按钮。

## What Changes

- 新增偏好 **中英切换方式**（输入组）：`Shift 或顶栏按钮`（默认）/`仅顶栏按钮`/`仅 Shift`。
- 顶栏 中/EN 按钮按模式显示/隐藏；执行器在「仅顶栏按钮」模式下拦截 `Shift_L` 按键（不触发 ascii_composer 翻转）。
- 顺带修复：`ImeSurfaceConfig` 之前把候选字号硬编码为 18、未传键面提示/候选注解开关，
  导致「设置 → 外观」的候选字号/提示/注解开关对键盘不生效；现在全部从偏好注入。

## Impact

- `AppPrefs.General` 新枚举 + key；设置中心输入组自动出现该行（沿用 spec 派生）。
- `:ui:ime`：`ImeSurfaceConfig` 增加 `showLanguageToggle`/`shiftTogglesLanguage`，顶栏按配置渲染。
- app：`ComposeInputView` 监听偏好并注入 config；`ImeCommandExecutor` 拦截 Shift。
- 风险：`仅顶栏按钮` 模式下 Shift 不再参与引擎模式切换（ASCII 大写依赖 Shift 的旧手感会受影响），
  属用户显式选择的取舍，默认模式不变。
