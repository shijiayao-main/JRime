# Proposal: surface-foundation

## Why

键盘表面层（键盘、候选条、顶栏、面板、气泡）在过去三周里由一轮轮「发现一个问题修一个」堆出来，已经形成结构性债务，并直接对应主人的四类日常痛点（键盘手感/布局、扩展功能、设置乱、视觉不统一；见 `doc/master-plan.md` §0/§2.2）。只读盘点（82 个表面文件逐文件核对）确认：

- **状态双真相**：`ime/compose/ImeUiState.kt` 同时承担引擎镜像、候选命令、窗口状态、配色版本、IME inset 回传（`:30-99,115-128`），且含死字段/死方法；键盘 id 同时存在于静态字段 `KeyboardWindow.currentKeyboard`（`ime/keyboard/KeyboardWindow.kt:52-56`）与渲染源 `NativeTheme.presetKeyboards`（`ime/compose/ImeRoot.kt:444-454`）。
- **两套渲染模型**：遗留 `Keyboard`（500 行）+ `Key`（361 行）仍被构造与读取状态，但几何已无人渲染；Compose 层改用 71 处硬编码 `.dp` 字面量。两套模型让每次键盘切换仍构造 861 行遗留对象。
- **上屏与反馈路径发散**：至少 5 条上屏路径绕过统一派发（`CommonKeyboardActionListener.kt:108-116`、`LiquidWindow.kt:95-103`、`ClipboardWindow.kt:61-66`、`ClipboardChip.kt:163`、`CandidateStrip` 直调引擎）；按下反馈 4 处、连发 2 套、振动 `keyPressVibrate` 全仓无调用者（`InputFeedbackManager.kt:174-181`），5 个振动偏好全部失效。
- **无设计 token**：尺寸/字号/颜色散落在各组件，硬编码中文与硬编码红色（`ClipboardPanel.kt:277`）；`material3` 依赖引入但零使用。
- **无扩展点**：面板由 Android 窗口栈 + DI + `when` 硬编码（`BoardWindow.kt:17-66`、`ImeRoot.kt:410-440`），入口靠字符串匹配（`CommonKeyboardActionListener.kt:182-213`）；新增一个面板要动 3 处平台/UI 文件。
- **无法跨平台**：43/82 个表面文件是平台面；UI 层直接 import `android.*`/`androidx.*`。目标是 CMP 一套 UI 跨 Android/桌面（`doc/master-plan.md` §3.5），现状无法直接迁移。

本文档对应 `doc/master-plan.md` 路线图 M1：在**功能对等**前提下把表面层重构成五层模型（设计 token / 布局模型 / 交互 / 表面 / 会话），并建立 CMP-clean 边界。它是后续 `surface-p0`、`settings-v2`、`desktop-cmp` 的共同地基；不做这一步，后续每个需求都会继续加在补丁堆上。

## What Changes

1. **模块拆分（只拆不改行为）**：新建 `:core:engine`（键模型/布局模型/手势解析/动作路由/会话状态机，纯 Kotlin）、`:core:model`（引擎响应 DTO）、`:ui:design`（设计 token/调色板/字体/动效）、`:ui:ime`（全部键盘表面 Composable）。`:app` 收窄为 Android 宿主与装配；其余模块暂留 `:app`，不一次拆完。
2. **单一会话状态与单向数据流**：`ImeSessionState`（不可变快照）+ `ImeController`（只暴露 `ImeCommand`）；引擎事件 → 状态机 → 状态 → Compose，Composable 只读状态、只发命令。删除 `ImeUiState` 及其直调引擎的 5 条旁路。
3. **数据驱动布局模型**：`KeyboardLayout/RowSpec/KeySpec/KeyActionToken` 落地，26 键/36 键/数字/电话/编辑键盘由类型安全数据定义；删除遗留 `Keyboard`/`Key` 像素模型与 `BoardWindow` View 工厂；切换键盘不再构造无人渲染的对象。
4. **设计 token 单一源**：`ImeDimens`/`ImeTypography`/`ImePalette`/`ImeMotion`；Surface 内禁止 `.dp`/`.sp`/`Color(0x…)` 字面量（守护测试）；浅/深两套语义色 + 跟随系统；视觉与现有 Gboard 风格对齐（不做新视觉方向）。
5. **手势与反馈各一套**：单点手势识别器 + 单点阈值 `CompositionLocal`；单击/长按/长按重复/四向滑动/空格滑光标/双空格按统一规则解析；反馈统一走 `FeedbackSink`（触感/音效/TTS），**接通振动**（现状缺陷修复，属唯一的可观察行为变化）。
6. **可扩展面板与工具栏注册表**：`ImePanel` 接口 + `PanelRegistry` + `SurfaceState.activePanel`；剪贴板面板、符号/Emoji 面板迁入注册表；工具栏动作注册表替代字符串路由。新增面板 = 注册一行，不改宿主。
7. **删除旧模型与死代码**：`Keyboard`/`Key`/`BoardWindow`/`ImeUiState` 在本变更内一次性删除；`InlineSuggestions`（88 行无调用）、`EnabledSchemaPickerDialog`（只 import）、`ProgressFragment`（无子类）同批清理或显式登记保留理由。
8. **测试与守护补齐**：布局模型、手势解析、状态机、命令执行纯逻辑测试；关键表面 Compose 结构测试；模块边界/字面量/旧模型引用 grep 守护纳入验收。

**不做**（显式排除）：任何新功能（候选翻页/键面提示/顶栏开关等进 `surface-p0`）；设置 IA 与设置项（进 `settings-v2`）；CMP 第二 target 与桌面端（进 D1/D2）；引擎、协议、JNI、资源/词典改动；视觉重设计（只做 token 化，不改视觉方向）。

## Capabilities

### New Capabilities

- `keyboard-surface`：键盘表面层的结构契约——单一会话状态与单向数据流、数据驱动布局、设计 token 单一源、统一手势/反馈、可扩展面板注册表、平台无关（CMP-clean）边界、功能对等与性能不劣化。

### Modified Capabilities

（无。`input-schemas` 与 `profile-storage` 的需求不受影响；本变更不改变任何输入方案的部署与配置行为。）

## Impact

- **代码结构**：新增 4 个 Gradle 模块与约 30 个新文件；迁移 `ime/compose/`（8 文件 2586 行）、`theme/`（5 文件 977 行）、`ime/keyboard/` 的纯逻辑部分；删除 `Keyboard`/`Key`/`BoardWindow`/`ImeUiState` 等约 1500–2000 行。
- **行为**：以功能对等为硬约束；唯一有意的可观察变化是「振动反馈恢复工作」。候选条/键盘/面板的既有交互保持不变（新交互全部推迟）。
- **测试**：现有 199 个单测保持全绿，其中受影响用例随文件迁移；新增布局/手势/状态机测试。
- **构建**：新增模块后 `./gradlew test :app:assembleDebug spotlessCheck` 为聚合闸门；不引入 CMP 依赖、不新增 target。
- **风险**：迁移面大（表面层 82 文件中的 39 个可移植文件 + 依赖它们的平台文件），可能在「功能对等」上漏项；缓解 = 对等清单逐项勾验、旧模型引用 grep 归零、真机冒烟、非作者跑闸门。
- **回滚**：单 commit 序列；若中途失败，可回退到 M1 起点（旧模型删除放在最后一批）。
