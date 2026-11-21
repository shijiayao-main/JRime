# Design: settings-v2（阶段 1）

## 1. 信息架构（阶段 1 落地部分）

新增「外观」页（`AppearanceSettingsFragment` → `AppPrefs.appearance`）：

| 项 | key | 形态 | 默认 |
|---|---|---|---|
| 深浅模式 | `theme_palette_mode` | 列表（跟随/亮/暗） | 跟随系统 |
| 强调色 | `theme_accent` | 列表（蓝/绿/紫） | 蓝 |
| 候选单页数量 | `candidate_page_size` | 滑杆 5–9 | 7 |
| 候选字号 | `candidate_text_size` | 滑杆 15–22sp | 18sp |
| 键面提示 | `show_key_hints` | 开关 | 开 |

从虚拟键盘页移出后三个键面/候选项（它们属于外观与候选，不属于键盘布局）。虚拟键盘页保留布局、行高、音效、振动、手势阈值与 Ctrl/Shift hook（阶段 2 再按六组重排）。

## 2. 深浅与强调色

- 引擎状态新增 `AccentPreset`（Blue/Green/Purple）与 `ImeCommand.SetAccent`；`PaletteMode` 已存在。
- `ui:design` 新增 `ImeAccent`（每个 accent 给 light/dark 两组色值与 on 色）与 `ImePalette.withAccent(accent, onAccent)`，覆盖 `enter`/`accent`/`candidateHighlighted` 三个角色，其余语义色不变。
- `ImeSurface` 按 `state.paletteMode` 选浅/深，再按 `state.accent` 覆盖强调色；`isSystemInDarkTheme` 仍负责跟随系统。
- 控制器在 attach 时从 `AppPrefs.appearance` 读取并写入状态；`ComposeInputView` 的偏好 listener 在设置变更时派发 `SetPaletteMode`/`SetAccent`/`SetCandidatePageSize`，候选字号与提示开关直接驱动 config 重组。

## 3. 死设置清理

删除 7 个零消费者偏好与 4 个死常量。保留但不在本批处理的：14 项可见但无效的项中，`hide_*`/`expand_keypress_area`/横屏分屏依赖遗留 `Keyboard/Key` 模型，随该模型删除（M1 收尾后续批次）一并清；`hook_ctrl_*`/`hook_shift_*` 有真实硬件键盘消费者，保留；两套深色概念中的 App `ui_mode` 与键盘深浅，在阶段 2 合并为一个。

## 4. 测试

- 引擎：`SetAccent` 状态单测。
- 结构：外观页与导航编译即验；设置变更 → 表面重组冒烟（模拟器截图）。
- 手动：外观页五项可调；改强调色后键盘回车/候选高亮变色；改深浅模式后键盘即时切换。

## 5. 阶段 2 预告（不在本批）

`:ui:settings` Compose 六组（输入/键盘/候选/外观/工具栏/词库与数据）、`SettingSpec` 注册表与「无 UI 入口/无消费者」双向断言、设置搜索、删除 Preference 依赖。

## 风险

- 删除偏好无迁移：自用场景接受；被删 key 不再读取，不影响启动。
- 强调色只覆盖三个语义角色，避免一键换色导致对比度失衡；真机走查后再决定是否扩大。
