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

## 2026-11-21 · 模拟器冒烟（Android 16 arm64 模拟器，emulator-5554）

- 安装 arm64 debug APK、启用并选中 JRime 后打开 App「测试输入」面板：
  - 新共享表面正常渲染：顶栏（⚙ / 标点 / 123 / 📋 / ☻ / 中 / ▼）、数字行、QWERTY 三行、
    辅助/⌫ 行、符号/空格/回车行；Compose 布局无裁切、无崩溃。
  - 盲按键盘输入后进入组合态：顶栏编码回显、候选条占据数字行槽位、候选序号角标出现；
    点击候选后组合态结束、键盘回到空闲态。
  - `adb logcat` 无 FATAL/AndroidRuntime 异常。
- 截图存 `screenshots/emulator-2026-11-21-*.png`（idle / composing / after-select）。
- 已知未验证/差异（留给 M1 收尾与真机）：
  - 空格标签仍是「空格」，旧表面显示 schema 名（controller 尚未消费 schema 消息）；
  - ⌫ 图标回退成 ⇦（`TextKey.labelSymbol` 未进 `KeyBinding`）；
  - 键间距/字号按 token（旧 22sp→19sp、padding 2dp→6dp），手感需真机校准；
  - 剪贴板 chip / 剪贴板面板 / 符号面板仍走旧实现与旧配色。

## 2026-11-21 · 标签与 schema 收口

- controller 消费 `onRimeSchemaUpdated`/`onInputStatusUpdate`，首帧即带 schema 名与输入模式；
  空格键显示「飞鹤快拼」，与旧表面一致。
- 键面字优先级改为「布局声明 label > preset label > token」：逗号「，」、句号「。」、
  退格「⌫」恢复；空格 schema 名恢复。截图 `screenshots/emulator-2026-11-21-labels.png`。
