# Design: surface-foundation

> 依据：`doc/master-plan.md` §3/§4；只读盘点（表面层/引擎/桌面/产品四份，2026-11-21）。
> 目标：功能对等前提下，把表面层重构为五层模型并建立 CMP-clean 边界。**本设计不改用户可见功能**（振动修复除外）。

## 1. 目标与非目标

**目标**
1. 表面层只有一个状态源、一个命令出口、一套手势、一套反馈、一套 token、一个面板注册表。
2. UI 层可被 Compose Multiplatform 直接编译到第二平台：`:ui:*` commonMain 零平台 import，平台能力全部经接缝接口注入。
3. 行为对等：现有全部键位、长按、滑动、面板、候选交互在重构后逐项可用。
4. 结构可守护：模块边界、字面量禁令、旧模型归零、设置无孤儿（后者在 `settings-v2`）由测试/grep 机械检查。

**非目标**
- 不新增功能（候选翻页、键面提示、顶栏开关 → `surface-p0`）。
- 不改视觉方向（只把现有视觉参数化；新主题在 `settings-v2`）。
- 不引入 CMP 依赖/第二 target（→ D1）。
- 不改设置项（→ `settings-v2`）。
- 不改引擎/JNI/协议/资产。

## 2. 模块与依赖

```
:app (Android app)
 ├─ :ui:ime ── :ui:design
 │    └────── :core:engine ── :core:model
 ├─ :core:rime ── :core:model
 └─ :core:data (本期不拆，暂留 :app；仅在 app 内按接口组织)
```

- `:core:model`：`RimeProto.kt`、`SchemaItem.kt` 等纯 DTO（零平台 import）。与桌面 8 DTO 的汇合在 D1，本期不搬桌面代码。
- `:core:engine`：`KeyboardLayout/RowSpec/KeySpec/KeyActionToken`、`KeyGestureResolver`、`ImeCommand`、`ImeSessionState`、`ImeStateMachine`、`ImeController`；纯 Kotlin + 协程，无 Android。
- `:ui:design`：`ImeDimens`、`ImeTypography`、`ImePalette`、`ImeMotion`、`ImeShapes`；Compose Multiplatform API（本期只 android target，但只用跨平台 API 子集）。
- `:ui:ime`：`KeyboardSurface`、`KeySurface`、`CandidateStrip`、`TopBar`、`PanelHost`、`ImeRoot`；只依赖 `:core:engine`、`:ui:design` 与接缝接口。
- `:app`：`TrimeInputMethodService`、InputConnection 封装、窗口/insets、DI 装配、`EngineSession` 的 librime 实现；平台面全部留在这里或 `:core:rime`。

**依赖规则（测试守护）**：`core` 不依赖 `ui`/`app`；`ui` 不依赖 `app`/`core:rime`/`core:data`；`core:engine` 不依赖 `core:model` 之外的任何模块（`core:model` 无依赖）。

## 3. 会话状态与单向数据流

```kotlin
// :core:engine
data class ImeSessionState(
    val composition: CompositionView? = null,      // 组合态编码/光标
    val candidates: List<CandidateView> = emptyList(),
    val highlightedIndex: Int = -1,
    val composing: Boolean = false,
    val asciiMode: Boolean = false,
    val asciiPunctuation: Boolean = false,
    val schemaLabel: String = "",
    val hasMenu: Boolean = false,
    val activePanel: PanelId? = null,
    val keyboardId: KeyboardId = KeyboardId.Qwerty26,
    val visibleRowHeightDp: Int = 50,
    val paletteMode: PaletteMode = PaletteMode.FollowSystem,
)

sealed interface ImeCommand {
    data class Key(val token: KeyActionToken) : ImeCommand
    data class SelectCandidate(val index: Int, val fromPage: Int = 0) : ImeCommand
    data class LongPressCandidate(val index: Int) : ImeCommand
    data class CommitText(val text: String) : ImeCommand
    data object Backspace : ImeCommand
    data object ToggleAscii : ImeCommand
    data class SetOption(val option: String, val value: Boolean) : ImeCommand
    data class OpenPanel(val id: PanelId) : ImeCommand
    data object ClosePanel : ImeCommand
    data class SwitchKeyboard(val id: KeyboardId) : ImeCommand
    data class MoveCursor(val chars: Int, val lines: Int) : ImeCommand
    data object HideKeyboard : ImeCommand
}
```

- 引擎消息（现有 `InputBroadcaster` 扇出）在 `:app` 侧被翻译为 `EngineEvent`，喂给 `ImeStateMachine`，产出新 `ImeSessionState`；Compose 通过 `StateFlow` 收集。
- 所有 UI 动作（键、候选、面板、工具栏）只调用 `controller.dispatch(ImeCommand)`；命令执行器在 `:app`/`:core:engine` 中落到 `EngineSession`/`PlatformInputHost`。
- 删除 `ImeUiState` 的可变字段与直调引擎方法；`bandTopY`/insets 回传改为 `PlatformInputHost` 上的独立回调，不进会话状态。

**迁移策略（strangler）**：先建 `ImeSessionState` 与命令，双写一段时间（新状态从旧广播推导，UI 读新状态）→ 切换 UI 读新状态 → 删除旧 `ImeUiState`。双写期不得超过本变更生命周期。

## 4. 布局模型

```kotlin
@JvmInline value class KeyboardId(val value: String) {
    companion object { val Qwerty26 = KeyboardId("qwerty26"); val Flyhe36 = KeyboardId("flyhe36") }
}

data class KeyboardLayout(val id: KeyboardId, val rows: List<RowSpec>)
data class RowSpec(val keys: List<KeySpec>, val heightUnits: Float = 1f)
data class KeySpec(
    val action: KeyActionToken,
    val label: KeyLabel = KeyLabel.None,
    val hint: KeyLabel? = null,
    val longPress: KeyActionToken? = null,
    val swipes: Map<SwipeDir, KeyActionToken> = emptyMap(),
    val widthUnits: Float = 1f,
    val style: KeyStyle = KeyStyle.Char,
)
data class KeyLabel(val text: String? = null, val icon: IconId? = null)
```

- `KeyActionToken` 是密封类（`Char`/`Text`/`Command`/`Modifier`/`SwitchKeyboard`/`OpenPanel`/`Option`/`MoveCursor`/`Hide`），替代现有字符串 token 与 `KeyAction.kt:134-171` 的三份标签规则。
- 布局数据从 `theme/KeyLayouts.kt` 迁移到 `:core:engine` 的 `layouts/`；`qwerty26`/`flyhe36`/`number`/`phone`/`editor` 逐键搬迁，行为槽位（长按/滑动）逐项对照原文件。
- **功能对等基线**：`theme/KeyLayouts.kt` 的 7 键盘 + 71 presetKeys 是必须携带的功能清单（`doc/refactor/roadmap.md` 期 1 的闭包结论），逐项列进任务清单勾验。
- 几何：以 10u 网格描述（`GboardKeyboard.kt` 现有逻辑），1u 由 `ImeDimens` 计算；行高取偏好档位；内容带高 = 6×行高（保留现有统一带高）。
- 遗留 `Keyboard`/`Key` 删除后，`KeyboardWindow` 只保留「键盘 id 与切换规则」，不再构造像素模型。

## 5. 设计 token

```kotlin
object ImeDimens {
    val KeyGap = 6.dp; val PagePadding = 6.dp; val PanelPadding = 12.dp
    val TopBarHeight = 40.dp; val CandidateHeight = 36.dp
    val KeyRadius = 8.dp; val PanelRadius = 24.dp; val ChipRadius = 16.dp
    val MinTouchTarget = 40.dp
}
object ImeTypography { val KeyLetter = 19.sp; val KeySymbol = 14.sp; val Candidate = 18.sp
    val CandidateComment = 11.sp; val Badge = 9.sp; val TopBar = 14.sp }
object ImeMotion { const val Press = 80; const val Candidate = 120; const val Panel = 200 }
```

- 调色板为语义色（`surface`/`key`/`keyPressed`/`keyHint`/`accent`/`text`/`textMuted`/`divider`/`danger`/`candidateHighlight`），Light/Dark 两套；`PaletteMode` = 跟随系统/强制浅/强制深（强制项在本变更加入状态与 token，但 UI 入口留给 `settings-v2`，本期默认跟随系统）。
- `ImeColors.kt` 的现有硬编码颜色映射进两套调色板；`data/theme/ColorManager` 的 int 色路径在 UI 层退役（`NavigationBarManager` 等平台面可继续用）。
- 守护测试：扫描 `:ui:*` 源文件，`.dp`/`.sp`/`Color(0x` 只允许出现在 `:ui:design` 与测试中。

## 6. 手势与反馈

- 单一 `KeyGestureResolver`（`:core:engine`）：输入 `PointerEvent` 序列（按下/移动/抬起/时间戳）+ `GestureConfig`（长按时长、滑动距离、连发间隔、双空格窗口），输出 `KeyGesture`（Tap/LongPress/Repeat/Swipe(dir)/SlideCursor/DoubleSpace）。现有 `KeyboardPanel.dataKeyGestures`（约 150 行）与 `GboardKey` 的 chrome 手势合并为一套。
- `GestureConfig` 由 `:app` 从偏好读取一次，经 `CompositionLocal` 下发（保留现有单 listener 机制），键内不再直读 `SharedPreferences`（现存 `KeyboardPanel.kt:333-334` 直读）。
- 反馈：`FeedbackSink` 接口（`haptic(pattern)`/`sound(id)`/`speak(text)`），`:app` 实现；`InputFeedbackManager` 保留平台实现但被收编。**振动接通**：`keyPressVibrate` 在 Tap/LongPress 路径调用，偏好开关/时长生效。
- 空格滑光标、双空格句号、空格下滑切 36 键：作为 `KeyGesture` 的具名行为保留，阈值进 `GestureConfig`，行为不进 UI 组件。

## 7. 面板与工具栏注册表

```kotlin
interface ImePanel {
    val id: PanelId
    val title: String
    val icon: IconId
    @Composable fun Content(state: ImeSessionState, controller: ImeController)
}
object PanelRegistry { fun register(panel: ImePanel); fun all(): List<ImePanel> }
```

- `SurfaceState.activePanel: PanelId?`（在 `ImeSessionState` 内），`PanelHost` 只做动画与高度；删除 `BoardWindow`/`BoardWindowManager` 的 View 工厂职责（窗口栈若仍需保留，仅保留 insets/attach 语义）。
- 首版注册：剪贴板面板、符号/Emoji 面板（合一面板，分类 tab）、编辑键盘入口；`LiquidWindow` 的符号数据加载保留，UI 迁移到注册表面板。
- 工具栏动作注册表：`ToolbarAction(id, icon, visibleWhen, onClick)`；本期只迁移现有按钮，不做排序配置（排序进 `settings-v2`/P1）。

## 8. 旧代码处置

| 文件/类型 | 处置 |
|---|---|
| `ime/keyboard/Keyboard.kt`（500）、`Key.kt`（361） | 删除；几何/状态迁移到 `:core:engine` 新模型 |
| `ime/compose/ImeUiState.kt`（175） | 删除，由 `ImeSessionState` + `ImeController` 替代 |
| `ime/window/BoardWindow.kt`（67）、`BoardWindowManager.kt`（128） | 删除 View 工厂；保留必要 insets/attach 逻辑到 `:app` 的 `WindowHost` |
| `ime/compose/KeyboardPanel.kt`、`GboardKeyboard.kt`、`GboardKey.kt` | 拆成 `KeyboardSurface`/`KeySurface`/`LayoutResolver`；手势/几何抽出 |
| `ime/keyboard/KeyAction.kt`（253） | 标签/键码映射抽到 `:core:engine` 的 token 解析；Android KeyEvent 部分留 `:app` |
| `ime/core/InlineSuggestions.kt`（88，零调用） | 删除（或登记保留理由） |
| `ime/dialog/EnabledSchemaPickerDialog.kt`（只 import） | 删除 |
| `ui/main/settings/ProgressFragment.kt`（无子类） | 删除 |
| `ime/core/TrimeInputMethodService.kt`（1003） | 本期只做「减责」：移出表面装配与面板宿主，保留 IME 生命周期/InputConnection；不追求一次拆小 |

## 9. 测试策略

1. **纯逻辑单测**（主力）：布局折行、10u 几何、手势状态机（tap/long/repeat/swipe/slide/double-space 边界）、命令执行、面板状态、token 解析。
2. **结构守护测试**：模块依赖白名单；`:ui:*` 平台 import 零命中；token 字面量禁令；旧类型引用零命中；`SettingSpec` 无孤儿（本期只建骨架，`settings-v2` 启用完整断言）。
3. **Compose 结构测试**（Android）：键盘行数/键数/候选条可见性/面板打开关闭；不做像素级断言。
4. **真机冒烟**：`doc/master-plan.md` §7.5 清单中与表面层相关的项 + 功能对等清单。
5. **性能口径**：替换前后各测一轮「键盘可见到可交互」「按键按下到视觉反馈」「候选滚动帧率」；劣化需主人放行。

## 10. 迁移顺序（建议提交序列）

1. 建模块骨架 + convention 插件（构建绿、空模块）。
2. `:ui:design` token + 现有颜色参数化（不改视觉）。
3. `:core:engine` 布局模型 + 迁移 qwerty26/flyhe36/number/phone/editor 数据；单测对照旧布局。
4. `ImeSessionState`/`ImeCommand`/`ImeController` + 引擎事件适配；双写。
5. UI 切新状态：`KeyboardSurface`/`KeySurface`/`CandidateStrip`/`TopBar`。
6. 手势/反馈统一 + 振动接通。
7. 面板注册表 + 剪贴板/符号面板迁移。
8. 删除旧模型/死代码；引用 grep 归零。
9. 测试补齐 + 真机验收 + 性能归档。

## 11. 风险与备选

| 风险 | 缓解 |
|---|---|
| 迁移漏功能（71 presetKeys/长按/滑动槽位） | 以 `KeyLayouts.kt` 为清单逐项勾验；旧文件删除前跑对照测试 |
| Compose 结构测试成本高、收益低 | 只做结构断言，不做像素；视觉靠截图走查 |
| 模块拆分引入构建摩擦（KSP/DI/ViewBinding） | 本期只拆纯 Kotlin/Compose 模块；`:app` 保留 DI 与 Android 依赖；convention 插件复用现有 7 个插件扩展 |
| 双写期状态不一致 | 双写不超过 2 个提交批次；每批次真机冒烟 |
| 界面延迟劣化 | 键级状态粒度、避免整树重组；性能口径实测归档 |
| 与 `surface-p0`/`settings-v2` 并发 | 变更串行；`surface-p0` 在本变更合并后开工 |

**备选**：只做「状态+token」轻量重构、保留 `Keyboard/Key`（被否：两套渲染模型是结构债根源，留任一套都会在 CMP 迁移时二次返工）；直接上 CMP 第二 target（被否：M1 目标是手机对等重构，第二 target 只增构建复杂度，见 `master-plan.md` DEC-2）。
