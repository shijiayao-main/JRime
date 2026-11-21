# JRime 总规划 v1

> 版本：2026-11-21 · 状态：**生效中（待主人确认后作为唯一权威）**
> 定位：一人自用、专精小鹤双拼（飞鹤快拼 `flypy_xhfast`）的输入法产品与架构总纲。
> 本文吸收并取代三份旧规划：`doc/refactor/roadmap.md`（六期主线）、`doc/multiplatform-design.md`（多平台设计）、`doc/surface-plan.md`（表面层任务队列）。旧文保留为历史证据，不再作为执行依据；`doc/architecture-review.md`、`doc/harness-design.md`、`doc/pathb-syllabifier-spec.md` 作为专项事实文档继续有效。
> 事实基线：2026-11-21 工作树（HEAD `28efcac`）+ 四路只读盘点（表面层 82 文件逐文件核对 / 引擎启动链与配置面 / 桌面线全资产 / 功能与设置 43 项逐项取证）。文中行号与数字均来自该基线，实施前以当时工作树复核为准。
> 工作方式：本文是「为什么 + 长什么样 + 什么顺序」；每一步的详细规格进 `openspec/changes/<change>/`。所有权衡默认值写在文中，显式分歧集中在 §9 待裁决。

---

## 0. 一页结论

**现状三句话**

1. **引擎与启动线是健康资产**：构建期预编译已落地，首装 4.6s、稳态就绪 <2s、首键 <0.6s，按键热路径不碰 YAML，运行时 YAML 面已收敛到 2 份编译产物（约 77KB）+ 条件性 user.yaml/音效。主题 YAML 引擎已彻底移除。**性能不是当前的主要矛盾**。
2. **表面层是最大债务**：键盘 UI 由一轮轮「发现一个问题修一个」堆出来——`ImeUiState` 五职责超载、两套键盘状态真源、两套渲染模型（遗留 `Keyboard/Key` 861 行 + Compose）、手势/反馈逻辑四份复制、无设计 token（71 处 `.dp` 字面量）、面板靠 Android 窗口栈 + 字符串路由、`TrimeInputMethodService` 1003 行。**这直接对应你选的五个痛点中的四个**（手感/布局、扩展功能、设置乱、视觉不统一）。
3. **桌面线只有 macOS 半成品**：Swift 壳 352 行可构建、协议端到端可跑，但未安装、无中英切换、候选窗是系统 `IMKCandidates`、改配置不生效；Linux/Windows **零壳代码**，只有引擎制品；CMP 当前使用量为 **0**（Android 侧是 AndroidX Compose，设置页还是 14 个 Preference Fragment）。**「一套 CMP UI」目前一行都还没有，且桌面候选窗有平台级硬约束（§5.5）**。

**目标一句话**：手机端先做成「开箱即用的专精双拼输入法」——手感、候选、扩展、外观、设置五项都达到日常无感使用；再把同一套 CMP UI + 共享内核推到 macOS/Linux/Windows，平台壳只做输入接管。

**路线一句话**：
`M0 收口冻结 → M1 表面层重启 → M2 手机好用 P0 → M3 输入规则与词库 → M4 稳定发布 → D1 桌面共享内核 → D2 CMP 桌面落地 → D3 桌面日常可用`；Path B（Kotlin 引擎）作为**并行研究线**，不占主线关键路径。

**首批变更**：`surface-foundation`（已创建，对应 M1）、`surface-p0`（M2）、`settings-v2`（M2/M3）。

**需要你拍板的只有 4 项**（§9），其余本文已给默认路径。

---

## 1. 产品定位与原则

### 1.1 定位

**一句话**：小鹤双拼用户的一人自用输入法——开箱即用、零手工配置、手机优先、桌面随后，视觉与手感对标 Gboard 而不是「可配置的框架」。

**目标用户画像**：只有一个人（你）。不需要多用户、不需要方案市场、不需要兼容他人配置。所有「为了通用性而存在」的设计默认砍掉。

**产品边界**：
- 输入法本体：26 键双拼 + 形码辅助 + 中英混输 + 候选 + 符号/表情/剪贴板/编辑 + 词库。
- 不做：云输入、账号同步、皮肤商店、九宫格/手写/笔画、内置语音（保留外跳）、表情包/GIF 商店、多方案管理、动态功能模块。

### 1.2 设计原则（冲突时按此排序）

| # | 原则 | 判据 |
|---|---|---|
| P1 | **输入优先** | 任何 UI 改动不得让「按键到上屏」变慢；体验预算：就绪 <2s、首键 <300ms、按键反馈 <16ms 一帧 |
| P2 | **默认即最优** | 用户不配置也能舒服使用；设置只服务「有明确偏好」的少数项，不做参数游乐场 |
| P3 | **一套语言** | 视觉、手势、文案、动效在所有键盘/面板/设置页保持同一套 token 与规则 |
| P4 | **零 YAML、零手工配置文件** | 用户面不存在需要编辑的文件；主题/布局/设置全部代码内数据 + App 内 UI |
| P5 | **平台壳薄** | 平台差异只存在于「输入接管/窗口宿主/系统能力」接缝，UI 与业务逻辑跨平台复用 |
| P6 | **可验证** | 每个里程碑有机器闸门 + 真机清单；不写「感觉变好了」的验收 |
| P7 | **一次一个变更** | 每期一个 OpenSpec change，一个写入范围，一次评审，一次提交序列；禁止跨范围顺手改 |
| P8 | **本地与隐私** | 无网络权限、数据全本地、剪贴板敏感内容可遮蔽 |

### 1.3 明确不做（防止再次发散）

- 不做主题编辑器/皮肤系统：只提供 2–3 套预设 + 跟随系统深浅 + 强调色。
- 不做多方案管理/方案市场：内置唯一方案，`flypy_radical` 作为依赖。
- 不做键盘布局自由拖拽定制：内置 26/36/数字/编辑/符号固定族，最多提供行高与按键提示开关。
- 不在手机上做云同步/账号；备份走本地导入导出。
- 不把 Path B 塞进手机/桌面关键路径：它先作为研究线，H4 门（golden L0≥95%）通过后才可能替换运行时。

---

## 2. 现状判定（证据化）

### 2.1 已经是资产的（不要重做）

| 资产 | 事实 | 出处 |
|---|---|---|
| 构建期预编译 | 17 件产物随 APK；设备端无 maintenance 调用；首装 4.6s（释放 1.9s + 初始化 1.0s）、稳态就绪 ≈1.0s、可打字 <2s、零 Pass1 | `startup-fastpath/implementation-notes.md:231-233,315-319` |
| 按键热路径 | `engine.cc:167-196` ProcessKey 无 config 调用；首键唯一 YAML = 2 份编译产物 ≈77KB | `doc/multiplatform-design.md:56-58`；引擎盘点 §1 |
| 主题单一源 | 主题 YAML 引擎已删；`NativeTheme` 唯一生产者，2 套硬编码配色 | `theme/NativeTheme.kt:27-49`；`ThemeManager.kt:15-27` |
| 协议层 | 行协议 + 版本握手 `proto:1` 已在 CI 制品实证；`protocol-common` 三件套 112 行 | `desktop/protocol-common/`、c571d1c |
| macOS 壳基线 | Swift 4 文件 352 行，spawn JVM 引擎 + ready 握手 + 候选/上屏通路可跑 | `macos/JRimeIME/`；平台盘点 §1 |
| 测试基线 | 199 个 JVM 单测（2026-11-20）覆盖候选角标/手势阈值/主题/反馈等纯逻辑 | `app/build/test-results` |

**结论**：不需要再「优化性能」「重做引擎启动」「重做主题解析」。这些已经解决了。

### 2.2 三项结构性债务

**债务 A：表面层无架构（最大）**

| 症状 | 证据 |
|---|---|
| `ImeUiState` 五职责超载：引擎镜像 + 候选命令 + 窗口状态 + 配色版本 + inset 回传；含死字段/死方法 | `ime/compose/ImeUiState.kt:30-99,115-128` |
| 两套键盘真相：`KeyboardWindow.currentKeyboard` 静态字段 vs `NativeTheme.presetKeyboards` 渲染源 | `ime/keyboard/KeyboardWindow.kt:52-56`；`ImeRoot.kt:444-454` |
| 两套渲染模型：遗留 `Keyboard/Key`（861 行）仍被构造/读状态但几何无人渲染；Compose 用硬编码 dp | `ime/keyboard/Keyboard.kt`、`Key.kt`；`GboardKeyboard.kt:48,66` |
| 五条上屏路径绕过统一派发 | `CommonKeyboardActionListener.kt:108-116`、`LiquidWindow.kt:95-103`、`ClipboardWindow.kt:61-66`、`ClipboardChip.kt:163`、`CandidateStrip` 直调引擎 |
| 手势/反馈复制：按下反馈 4 处、连发 2 套、阈值解释散落 | `KeyboardPanel.kt:341-347`、`GboardKey.kt:315-319`、`ImeRoot.kt:334-337`、`ClipboardChip.kt:161` |
| 无设计 token：71 处 `.dp` 字面量、字号散落、硬编码中文、硬编码红色 | `ImeColors.kt:105-137`、`ClipboardPanel.kt:277` 等 |
| 面板扩展点缺失：新增面板要改 Android 窗口栈 + DI + Compose `when`；入口靠字符串匹配 | `BoardWindow.kt:17-66`、`ImeRoot.kt:410-440`、`CommonKeyboardActionListener.kt:182-213` |
| 超大文件 | `TrimeInputMethodService` 1003、`CommonKeyboardActionListener` 467、`ImeRoot` 547、`KeyboardPanel` 529、`GboardKeyboard` 517 |
| 死代码/过期注释 | `InlineSuggestions` 88 行无调用、`EnabledSchemaPickerDialog` 只 import、`ProgressFragment` 无子类 |

**债务 B：设置体系是 Trime 通用骨架的残留**

- 53 个偏好（49 可见），其中 **7 个零消费者**（`doubleTapTimeout`/`slideStepSize`/`swipeVelocity`/`vibrateOnKeyRelease`/`vibrateOnKeyRepeat`/`clipboardMaskSensitive`/`hookShiftArrow`）、4 个死常量、3 个无 UI（`normalModeColor`/`navbarBackground`/`followSystemDayNight`）；另有 14 项可见但无效或只影响已不渲染的遗留模型（横屏/分屏/隐藏输入栏/隐藏键符号等）。振动功能整体失效：`keyPressVibrate` **全仓无调用者**。
- 6 个设置页，但缺主题、候选、方案入口；「配置」页名不副实（只有目录/重置两个动作）；无设置搜索。
- 两套深色模式概念（App `ui_mode` vs 键盘 `follow_system_day_night`）；无「键盘强制深色」开关。
- 设置项与引擎配置完全解耦：没有任何偏好写入 rime 配置，`updateConfig()` 无 UI 入口——用户改 YAML 后只能全量修复部署。

**债务 C：桌面线只有 macOS 半成品**

- macOS：`.app` 可构建（141MB），未安装到 `~/Library/Input Methods/`；**中英切换路径断**（`flagsChanged` 未转发，`Shift_L: commit_code` 收不到）；候选窗是系统 `IMKCandidates`（无高亮/定位、重复文本错选、`candidateSelectionChanged` 空实现）；改 schema/ YAML 不生效（服务端不跑 deployment，bundle 无 deployer）；`ascii` 字段错位（P1-1）。
- Linux/Windows：**零壳代码**；CI 只产引擎 zip；`librime_jni.so/.dll` 的 JVM 加载从未验证。
- CMP：零使用。设置页 14 个 PreferenceFragment（0 Compose）；键盘 Compose 2586 行但有 4 处 Android import；无 KMP 模块、无 desktop/jvm target。

### 2.3 产品缺口（43 项功能盘点结论：19 完整 / 20 部分 / 4 缺失）

**P0（手机想「先用得爽」必须补齐）**

| # | 缺口 | 现状证据 |
|---|---|---|
| 1 | 候选条翻页/页码/单页数量/字号 | 现为 LazyRow 横滚 ≤16 条、无翻页页码（`CandidateStrip.kt:29-118`；`rime_jni.cc:201-206`） |
| 2 | 双拼键位/韵母提示 + 音节切分展示 | 仅 `/ok` 查表；键面无韵母提示、preedit 无音节分隔（`flypy_help_menu.lua`） |
| 3 | 触感反馈接通 | `keyPressVibrate` 无调用者；5 个振动偏好全废（`InputFeedbackManager.kt:174-181`） |
| 4 | 高频开关上操作栏：简繁/全角/emoji | 简繁只能 `/so` 或硬件快捷键；全角无开关（`schema.yaml:23-31,280-300`） |
| 5 | 候选长按菜单（置顶/删词/查编码/撤销） | 现长按=静默删词，无确认无撤销（`CandidateStrip.kt:98-110`） |
| 6 | 设置 IA 重做 + 14 项死设置清理 + 缺页补齐 | §2.2-B |
| 7 | 主题/外观可调（预设 + 深浅 + 强调色） | 硬编码 2 套、无选择 UI（`NativeTheme.kt:27-49`） |
| 8 | 键盘手感与布局调优（高度预设/手势阈值/空格滑切） | 行高单一滑杆；空格下滑切 36 键易误触 |

**P1（增强）**：词库浏览/编辑与备份恢复；联想预测（导入 `predict.db` + 开关）；符号/Emoji 搜索与分类索引；常用语/快捷短语；候选编码提示开关；英文候选释义开关；剪贴板跨应用收紧与置顶管理。

**P2（可延后）**：智能纠错、双拼纠错；内置语音/手写；表情包/GIF；键位自定义 UI；多方案管理；云同步。

### 2.4 与目标的四个偏差

1. **CMP 偏差**：目标是「一套 CMP UI」，现状是 0 CMP + 设置未 Compose + 键盘 UI 带 Android 依赖。不能直接「迁移」，只能先重构出可移植边界，再加第二 target。
2. **设置偏差**：目标「默认即最优」，现状 53 项里 1/4 无效或不可达。
3. **桌面偏差**：目标「桌面开箱即用」，现状只有 macOS 半成品且未日常使用；Linux/Windows 从零。
4. **治理偏差**：4 个已完成的 change 未归档、真机验收项未勾；三份规划文档互相重叠；CI 只在手动触发时覆盖桌面，提交 CI 的 `paths-ignore` 排除了 `desktop/**`、`harness/**`、`macos/**`。

### 2.5 为什么必须「先表面层、后桌面」

- 手机是你的日常入口，也是唯一有真机反馈的界面；表面层不重构，后面每加一个桌面 UI 都会复制同一份混乱。
- CMP 共享的前提是 UI 层零平台依赖；当前 43/82 个表面文件是平台面，直接上 CMP 会迫使边迁移边重构。
- 桌面候选窗有平台硬约束（§5.5），过早投入可能做了会被 Wayland/焦点问题推翻的工作；先把可确定的共享内核（协议/模型/引擎）与 CMP-clean 的 UI 边界做好，桌面风险才可控。

---

## 3. 目标架构 v2

### 3.1 分层

```
┌── 平台宿主（每平台一套，薄） ──────────────────────────────────────┐
│ Android: InputMethodService + InputConnection + 窗口宿主            │
│ macOS:   Swift IMK 壳（进程内，不可避免）                           │
│ Linux:   ibus engine（D-Bus）                                       │
│ Windows: TSF DLL（COM in-proc）+ JVM 引擎进程                       │
└──────────────────────────────┬────────────────────────────────────┘
        PlatformInputHost │ WindowHost │ PlatformEffects
┌──────────────────────────────┴────────────────────────────────────┐
│ UI（Compose Multiplatform，commonMain）                             │
│  :ui:design   设计 token / 调色板 / 字体 / 动效                      │
│  :ui:ime      会话状态 → 键盘/候选/工具栏/面板 Composable            │
│  :ui:settings 设置 UI（全部平台共享）                                │
└──────────────────────────────┬────────────────────────────────────┘
        ImeSession（状态 + Command）│ EngineSession │ SettingSpec
┌──────────────────────────────┴────────────────────────────────────┐
│ 业务核心（纯 Kotlin，commonMain）                                    │
│  :core:engine  键模型/布局模型/手势解析/动作路由/会话状态机           │
│  :core:model   引擎响应 DTO / proto                                  │
│  :core:protocol 桌面行协议（编解码/采样/就绪探测）                    │
└──────────────────────────────┬────────────────────────────────────┘
┌──────────────────────────────┴────────────────────────────────────┐
│ 平台适配（actual / 进程边界）                                        │
│  :core:rime    librime JNI 适配（android/jvm 两套 actual）           │
│  :core:data    偏好/用户词库/剪贴板/备份（expect/actual 存储）        │
│  :desktop:engine 桌面 JVM 引擎进程（D1 起）                          │
└───────────────────────────────────────────────────────────────────┘
```

### 3.2 模块表（目标态，分期落地）

| 模块 | 内容 | 构建声明 | 落地期 |
|---|---|---|---|
| `:app` | Android application：IME service、InputConnection、Android 宿主装配、App 壳 | Android app | 现状保留，M1 收窄为「宿主 + 装配」 |
| `:ui:design` | `ImeDimens`/`ImeTypography`/`ImePalette`/动效常量/图标语义 | kotlin multiplatform（android + jvm） | M1 建 android；D1 加 jvm |
| `:ui:ime` | 键盘表面全部 Composable + 表面状态 + 面板注册表 | kotlin multiplatform（android + jvm） | M1 建 android；D1 加 jvm |
| `:ui:settings` | Compose 设置页 + `SettingSpec` 注册表 | kotlin multiplatform（android + jvm） | M2 建 android；D1 加 jvm |
| `:core:engine` | 纯逻辑：`KeyboardLayout`、`KeySpec`、手势解析、动作路由、会话状态机 | kotlin multiplatform | M1 |
| `:core:model` | `RimeProto`/`SchemaItem` 等纯 DTO（吸收桌面 8 DTO） | kotlin multiplatform | M1（与桌面汇合在 D1） |
| `:core:protocol` | 桌面行协议三件套 + 8 DTO 编解码 | kotlin multiplatform | D1（脚本源集退役） |
| `:core:rime` | librime JNI 门面、session、消息解码；androidMain actual | KMP（android + jvm） | M1 拆接口，D1 加 jvm actual |
| `:core:data` | 偏好存储、用户词库、剪贴板、备份 | KMP + expect/actual | M2/M3 渐进 |
| `:desktop:engine` | 桌面 JVM 引擎进程（DesktopServer 重写为 gradle 模块） | JVM | D1 |
| `:desktop:host-macos`/`linux`/`windows` | 平台壳（Swift/C/GTK/TSF） | 各平台原生 | D2/D3 |

**分期原则**：M1 只拆 `:core:engine`、`:core:model`、`:ui:design`、`:ui:ime` 四个模块，其余暂留 `:app`；`settings` 在 M2 拆出；桌面模块 D1 起建。**不为了模块化而模块化**。

### 3.3 六条架构铁律（写入每个 change 的验收）

1. **单向数据流**：引擎事件 → `ImeSessionState`（不可变快照）→ Compose；UI 只能发 `ImeCommand`。禁止 Composable 直调引擎、禁止第二份可变状态。
2. **UI 层零平台依赖**：`:ui:*` 的 commonMain 不得出现 `android.*`/`androidx.*`/`java.*`（例外：通过接缝接口注入）。守护测试 + 编译隔离双闸。
3. **设计 token 单一源**：`:ui:*` 内禁止 `.dp`/`.sp`/`Color(0x…)` 字面量（token 文件与测试除外）；所有尺寸/字号/颜色/圆角/时长来自 `:ui:design`。
4. **扩展点用注册表**：面板、工具栏动作、按键动作 token、候选装饰、设置项全部注册到显式注册表；禁止字符串路由与 `when (panel)` 硬编码。
5. **设置即数据**：每个偏好由一个 `SettingSpec` 声明（key/类型/默认/分组/校验/是否可见）；无声明的偏好在测试中失败；每个声明必须有消费者（测试断言无孤儿）。
6. **旧模型一次性退役**：遗留 `Keyboard`/`Key`/`BoardWindow`/`ImeUiState` 在 M1 内删除，不允许「新旧双轨」过夜。

### 3.4 平台接缝接口（跨平台边界清单）

| 接缝 | 职责 | Android 实现 | 桌面实现 |
|---|---|---|---|
| `EngineSession` | `processKey`/`selectCandidate`/`commit`/`setOption`/`stateFlow` | `:core:rime` JNI | `:desktop:engine` 行协议客户端 |
| `PlatformInputHost` | 上屏、删除、移动光标、取编辑器信息、选区、返回键语义 | InputConnection 封装 | 壳回调（insertText/deleteBackward/…） |
| `WindowHost` | 键盘窗口显隐、高度/insets、横竖屏、窗口定位（候选窗） | IME 窗口 + insets | 壳窗口管理器 |
| `PlatformEffects` | 振动、音效、TTS、剪贴板读写、导入导出、路由跳转 | Android API | 各平台 API（CMP 侧只调接口） |
| `SettingsStore` | 偏好读写 + Flow | DataStore/SharedPreferences actual | 文件 actual |
| `Clock`/`Scheduler` | 手势阈值、连发、超时 | 协程 | 协程 |

### 3.5 CMP 共享策略与平台硬约束

**目标**：UI 一套代码（`:ui:design`/`:ui:ime`/`:ui:settings`），平台壳只做输入接管与窗口宿主。

**现实约束（必须在 D0 用 spike 验证，不能假设）**：

| 平台 | 输入接管 | 候选窗/状态窗 | CMP 可行性 | 风险等级 |
|---|---|---|---|---|
| Android | InputMethodService 进程内 | 自绘 Compose（同进程） | **高**：现状已是 Compose，加 CMP target 只是依赖/源集迁移 | 低 |
| macOS | Swift IMK（IMK 要求进程内 Objective-C/Swift，CMP 不能替代壳） | 可由 JVM/Compose 独立窗口绘制，但需「不激活、不抢焦点」；CMP 该能力无官方正解（CMP-3471 open） | 中：候选窗需 spike（AWT `focusableWindowState=false` + 定位） | 中高 |
| Windows | 原生 TSF COM DLL（JVM 不能 in-proc）+ JVM 引擎进程 | JVM/Compose 无边框置顶窗口 + `WS_EX_NOACTIVATE` 需 Swing/原生互操作补洞；CMP-6639/8118/6036 有已知 bug | 中：候选窗可行但需补洞 | 中高 |
| Linux (X11) | ibus engine（任意进程，D-Bus） | JVM/Compose 窗口可定位、可置顶 | 中：X11 可用 | 中 |
| Linux (Wayland) | ibus engine | **客户端无法任意定位/置顶窗口**（Wayland 协议限制）；需 compositor 的 input-method/popup 协议或 layer-shell，JVM/CMP 无通道 | **低**：CMP 自绘候选窗在原生 Wayland 不可行 | 高 |

**默认策略（写进 D2 的验收）**：
- 目标仍是「一套 CMP UI」：键盘表面、设置、状态窗、以及 **macOS/Windows/X11 的候选窗**用 CMP。
- **Linux 原生 Wayland 的候选窗**按现实降级：优先用 ibus 自带 panel（系统绘制，零窗口代码），或一个极小的 GTK4 原生面板；其余 UI 仍 CMP。这不违背「一套 UI」——Wayland 的协议约束不是设计选择。
- **D0 spike 是 D2 的硬前置**：三个证明（macOS 非激活候选窗、Windows 无激活候选窗、Linux X11 定位 + Wayland 降级路径）。任一失败 → 该平台候选窗走原生，**只降级该平台，不推翻整体架构**。
- **不在 M1 引入 CMP 第二 target**：M1 只要求 UI「CMP-clean」（无平台 import、接缝注入）。D1 才加 `jvm()` target 并把依赖从 AndroidX Compose 切到 `org.jetbrains.compose`（版本以届时的 Compose BOM/ Kotlin 版本对齐实测为准）。理由：手机阶段加第二 target 只增加构建复杂度，不产生可验收价值。

### 3.6 引擎与配置策略

- **已达成，不再投入**：构建期预编译、设备端零编译、按键路径零 YAML、主题 YAML 移除。
- **剩余小项（随 M1/M2 顺手收）**：每次启动仍跑 checksums diff 释放链（0.87s，可做指纹短路）；设备 `.so` 仍含 deployer/dict_compiler（保留修复路径，暂不删）；`updateConfig()` 无入口（设置 UI 化后自然消失）；音效 kaml 是最后一份 Kotlin 侧 YAML（可改 Kotlin/JSON）。
- **Path B（Kotlin 引擎）定位**：**并行研究线，不占关键路径**。已有 syllabifier 规格（41 facts）、harness/golden（12/60 用例）、协议层；实现 0 行。触发条件：D1 完成后若仍需摆脱 librime 运行时，再按 `harness` H4 门（golden L0≥95%、语料 ≥60）立项；当前不投入编码。

---

## 4. 键盘表面层设计体系（本次规划的核心补课）

> 这一节回答「为什么之前的键盘是一堆补丁」：因为没有下面这五层。所有后续 change 按这五层归属，跨层混写即为违规。

### 4.1 五层模型

| 层 | 内容 | 归属模块 | 规则 |
|---|---|---|---|
| L0 设计 token | 尺寸/间距/圆角/字号/颜色/动效/触感 | `:ui:design` | 唯一事实源，禁止字面量 |
| L1 布局模型 | 键盘族 / 行 / 键 / 键动作 / 键提示 的数据模型 | `:core:engine` | 纯数据 + 纯函数，可单测 |
| L2 交互 | 手势识别、阈值、冲突裁决、反馈管线 | `:core:engine` + `:ui:ime` | 一套手势机，一套反馈 |
| L3 表面 | 键盘、候选条、顶栏、面板、气泡、弹窗 | `:ui:ime` | 只读状态，只发 Command |
| L4 会话 | 引擎状态 → 表面状态、命令执行 | `:core:engine` + `:ui:ime` | 不可变快照，单向流 |

### 4.2 设计 token 规范（首版数值，真机验收微调）

| token | 值 | 说明 |
|---|---|---|
| 间距 scale | 2 / 4 / 6 / 8 / 12 / 16 / 24 dp | 键盘内边距 6，行间距 6，面板内边距 12 |
| 圆角 | 4 / 8 / 12 / 16 / 24 dp | 键 8，候选 chip 16，面板 24 |
| 行高 | 44–64 dp，默认 50 | 沿用现有 `keyboard_row_height` 偏好范围，改为档位选择 |
| 字号 | 字母键 19 / 符号键 14 / 候选 18 / 候选注释 11 / 角标 9 / 顶栏 14 / 标题 16 | 候选字号可调 16–22 |
| 动效 | 80ms（按下）/ 120ms（候选高亮）/ 200ms（面板） | 尊重系统「减弱动态」 |
| 触感 | 按下 8ms light，长按进入 12ms medium，滑动光标 4ms tick | 可关；强度/时长留给系统设置，不做 App 内 0–255 滑杆 |
| 调色板 | `Light` / `Dark` 两套语义色 + 3 个强调色（品牌蓝/墨绿/紫） | 跟随系统 + 手动强制深浅 |
| 语义色 | `surface`/`key`/`keyPressed`/`keyHint`/`accent`/`text`/`textMuted`/`divider`/`danger`/`candidateHighlight` | 禁止组件内取色 |

### 4.3 布局模型与键盘族

```kotlin
// 目标模型（示意，实施时细化）
data class KeyboardLayout(val id: KeyboardId, val rows: List<RowSpec>)
data class RowSpec(val keys: List<KeySpec>, val heightUnits: Float = 1f)
data class KeySpec(
    val action: KeyActionToken,          // 类型安全 token，替代字符串
    val label: KeyLabel,                 // text/icon/空
    val hint: KeyLabel? = null,          // 角标/长按提示
    val longPress: KeyActionToken? = null,
    val swipes: Map<SwipeDir, KeyActionToken> = emptyMap(),
    val widthUnits: Float = 1f,
    val style: KeyStyle = KeyStyle.Char,
)
```

- **键盘族（首版固定）**：`qwerty26`（默认）、`flyhe36`（空格下滑切换，保留）、`number`、`phone`、`editor`（编辑键）、`symbol`/`emoji`（作为面板而非键盘）。
- **布局数据来源**：现有 `theme/KeyLayouts.kt`（433 行 DSL）迁移为 `:core:engine` 的类型安全数据，不再被 UI 直引；删除遗留 `Keyboard`/`Key` 像素模型。
- **几何**：qwerty 10u 网格，1u = `(宽度 - 2×6dp - 9×6dp) / 10`；行高由 token + 偏好决定；内容带高 = 6×行高（沿用现有统一带高，保证面板/键盘切换零跳变）。
- **横屏/分屏**：首版不做分屏；横屏保留「同一布局自适应宽度」，彻底移除 `keyboard_landscape_mode`/`split_space_percent` 死设置。

### 4.4 交互规范（手势表 + 冲突裁决）

| 手势 | 行为 | 阈值/条件 | 冲突裁决 |
|---|---|---|---|
| 点击 | 上屏/执行 | — | 最高优先 |
| 长按 | 键定义的长按动作；无定义 = 大写字母/符号 | `LONG_PRESS_TIMEOUT` 默认 500ms（可调 300–800） | 长按期间移动超过 8dp 则转为滑动 |
| 长按重复 | 退格/方向等 | 80ms 间隔 | 仅对允许按键 |
| 四向滑动 | 键定义的滑动手势（符号/删音节/切键盘） | `SWIPE_TRAVEL` 默认 32dp | 空格下滑切 36 键保留，但需 >48dp 防误触 |
| 空格长按滑 | 移光标（水平逐字/垂直逐行） | 长按 350ms 后进入，22dp/步 | 进入后不再上屏空格；松手结束 |
| 双空格 | 上屏「。」 | 120–350ms 窗口 + 光标前是空格 | 连发节奏 80ms 永不误触 |
| 页内滑动 | 候选条横向滚/翻页 | — | 与键滑动不冲突（不同组件） |
| 长按候选 | 候选菜单（置顶/删词/查编码） | 500ms | 取代静默删词 |

- **反馈优先级**：触感 > 音效 > TTS（可分别关闭）；同一事件只触发一次。
- **编辑操作**：复制/粘贴/撤销走系统文本工具条优先，键盘编辑键作为补充；不再把 Ctrl 组合塞进字母键长按。

### 4.5 候选条规范

- **取数**：引擎 `Candidates.Bulk` → `CandidatePage`；**分页展示**，单页数量偏好 5/7/9（默认 7），超过一页时显示左右翻页箭头与「当前页/总页」；保留横向滚动作为页内溢出兜底。
- **序号角标**：前 9 个显示，与长按数字选词一致；`CandidateProto.label` 作为可选覆盖。
- **长按菜单**：置顶 / 删词（带 3s 撤销）/ 查看编码 / 复制候选；不再静默删词。
- **注释**：编码注释、形码注释、英文释义开关；字号/是否显示由设置控制。
- **位置**：顶栏下方（默认）或键盘上方，二者不并存；组合态编码回显与 inline preedit 二选一默认（避免同一编码显示两处）。
- **视觉**：候选高度 36dp、chip 最小宽度 44dp、间距 6dp、高亮用 `candidateHighlight`。

### 4.6 顶栏/工具栏与面板注册表

- **顶栏**（40dp）：设置、标点、数字行切换、剪贴板、表情、中/EN、收起；语音保留外跳。
- **面板注册表**：

```kotlin
interface ImePanel {
    val id: PanelId
    val title: String
    val icon: ImageVector
    @Composable fun Content(state: ImeSessionState, controller: ImeController)
}
```

- `PanelHost` 只管动画与窄高；面板状态进 `SurfaceState.activePanel: PanelId?`；新增面板 = 注册一行，不改宿主。
- 首版面板：剪贴板（历史/收藏/置顶/编辑/清空/敏感遮挡）、符号与 Emoji（合一面板，分类 tab + 搜索）、编辑键面板。
- **快捷短语/常用语**：P1 面板，注册表天然支持。

### 4.7 设置信息架构 v2（6 组）

| 组 | 内容（目标） | 从现有迁移/新增 |
|---|---|---|
| 输入 | 双拼方案（只读展示）、模糊音/简拼、简繁、全角/半角、中英切换方式、标点方案、联想/预测开关、纠错（P2） | 新增；删除 `hook_ctrl_*` 等硬件键盘遗产 |
| 键盘 | 键盘族（26/36）、行高档位、数字行、按键提示（韵母/形码/长按）、长按/滑动阈值档位、空格手势 | 合并现有键盘页 35 项 → 约 15 项 |
| 候选 | 单页数量、候选字号、注释开关、候选位置、长按动作、上屏后行为 | 新增 |
| 外观 | 主题（浅/深/跟随系统）、强调色、候选字体（导入字体只影响候选/预编辑） | 新增；删除主题残留字符串 |
| 工具栏与面板 | 工具栏按钮与顺序、剪贴板设置、符号/Emoji 默认页、常用语 | 剪贴板页迁移 |
| 词库与数据 | 用户词库浏览/编辑、导入导出、备份恢复、数据目录、重置 | 增强现有词库页 + 配置页 |
| 关于 | 启用方法、隐私、日志、版本、开源许可 | 现有 |

规则：每个设置项一个 `SettingSpec`，默认值写进注册表；**测试断言每个 spec 有 UI 入口、有至少一个消费者**；删除的 14 项死设置不留迁移路径（本机自用，重置可接受）。

### 4.8 可访问性与国际化

- 触控目标 ≥40dp；对比度满足 WCAG AA；TalkBack 语义标签（键名/候选序号/面板名）；键盘焦点顺序。
- 所有用户可见文案进 `strings.xml`（现有 223 条复用），禁止 Composable 内硬编码中文；桌面端语言由 CMP resources 提供（D1 再引入）。

### 4.9 扩展点清单（首版）

1. `PanelRegistry`：面板。
2. `ToolbarActionRegistry`：顶栏动作与排序。
3. `KeyActionToken`：类型安全键动作（替代 `token: String`）。
4. `CandidateDecorator`：候选角标/注释/高亮。
5. `KeyHintProvider`：韵母/形码/长按提示。
6. `ThemePreset`：调色板。
7. `SettingSpec`：设置项。
8. `EngineSession` 后端（librime / 未来 KotlinEngine）。

---

## 5. 分期路线图

## 5. 分期路线图

> **实施状态（2026-11-21 r2）**
>
> | 期 | 状态 | 备注 |
> |---|---|---|
> | M0 收口 | ✅ | 文档权威链、基线、旧 change 真机项待统一验收后归档 |
> | M1 表面层 | ✅（遗留清理待办） | 共享 CMP-clean 表面接管；旧 Keyboard/Key/BoardWindow 模型与旧面板待最终清理 |
> | M2 手机 P0 | ✅ | 候选分页/长按菜单、键面韵母提示、简繁全角与 10 条输入规则、外观（深浅/强调色）、Compose 设置中心六组+搜索+无孤儿断言、死设置清理 |
> | M3 输入规则/词库 | 🟡 大部分 | 输入规则、常用语面板、词库备份/恢复已完成；中英切换方式偏好、词库逐词管理未做 |
> | M4 稳定发布 | 🟡 大部分 | 启动失败降级、整机备份恢复、诊断复制、一键安装脚本完成；真机矩阵/签名 secrets/7 天观察待主人 |
> | D1 桌面内核 | ✅ | `:desktop:engine`/`:desktop:protocol` Gradle 化；协议依赖倒置；macOS Shift 切中英、安装脚本 |
> | D2 共享桌面 UI | 🟡 | 五模块 KMP 化；`:desktop:app`（引擎+协议+CMP 候选窗）与 `:desktop:settings` 跑通；Linux IBus 薄壳（单测）与 Windows 契约就绪；Shell 集成/Windows 实机待办 |
> | D3 桌面日常 | 🟡 | macOS 可安装（系统候选窗）；`:desktop:app` 候选窗空闲隐藏 + 设置热重载；Linux/Windows 需对应平台实测；验收手册已出 |
>
> 完成/待验收的标准与操作见 `doc/verification-guide.md`。

> 工期为「一人 + AI」估算，单位：周。每期一个 OpenSpec change，独立可交付、可回滚；不追求并行抢进度。

### M0 · 收口与冻结（0.5–1 周）

**目标**：把上一轮散落工作收口，立基线，停止「发现问题改一处」。

- 归档 4 个已完成 change：`specialize-flypy`、`startup-fastpath`、`trim-jni-surface`、`native-theme`（真机验收项：能验则勾验，不能验则显式降级记录，不静默）。
- 建立基线快照：当前功能清单、设置清单、键盘截图集、性能数字（首装/稳态/首键/帧率）、APK 体积。
- 文档权威链落地：本文生效；三份旧规划加 `> 已被 doc/master-plan.md 取代` 注记（仅加头注，不重写内容）。
- CI：提交 CI 覆盖 Android（应已有）；为 `desktop/**`、`harness/**`、`macos/**` 增加 path-filtered job 或明确「本地闸门」名单；补齐 release 签名 secrets（需你操作）。
- **功能冻结**：M0 起至 M1 结束不再接受「单点功能补丁」，所有需求进对应 change 队列。
- 验收：文档 + 基线 + CI 绿 + 真机清单通过。

### M1 · 表面层重启（2–3 周）→ change `surface-foundation`（已创建）

**目标**：功能对等的前提下，把表面层从补丁堆重构成五层架构，建立 CMP-clean 边界。

- 模块拆分：`:core:engine`、`:core:model`、`:ui:design`、`:ui:ime`。
- 单一状态：`ImeSessionState` + `ImeController` 取代 `ImeUiState`；五条上屏路径收敛为 `ImeCommand`。
- 布局模型：`KeyboardLayout/KeySpec` 落地，26/36/数字/编辑键盘数据化；删除遗留 `Keyboard/Key`（861 行）与 `BoardWindow` View 工厂。
- 设计 token：尺寸/字号/色板/动效单一源；Surface 内禁止字面量（守护测试）。
- 手势与反馈各一套；接通振动。
- 面板注册表；剪贴板/符号面板迁移到注册表。
- 测试：布局/手势/状态机纯逻辑测试；Compose UI 结构测试；真机冒烟（功能对等清单）。
- **不做**：新功能、设置 IA、CMP 第二 target、引擎改动。
- 验收：功能对等清单逐项勾验；`grep` 守护（`:ui:*` 零 `android.*`）；按键延迟/帧率不劣化；`Keyboard/Key/ImeUiState` 零引用。

### M2 · 手机「好用」P0（3–4 周）→ change `surface-p0` + `settings-v2`

**目标**：把你选的四类痛点直接打平。

- 候选条 v2（翻页/页码/单页数量/字号/长按菜单）。
- 键面提示（韵母/形码/长按提示）+ 组合态编码回显收敛。
- 触感反馈接通 + 反馈设置。
- 顶栏高频开关：简繁/全角/Emoji/剪贴板。
- 设置 IA v2 + 删除 14 项死设置 + `SettingSpec` + 设置搜索。
- 外观：浅/深/跟随 + 3 强调色 + 候选字体；截图走查迭代。
- 键盘手感：行高档位、空格/滑动阈值调优、防误触。
- 验收：P0 清单逐项 + 连续 3 天日常使用反馈 + 设置无孤儿断言 + 截图评审通过。

### M3 · 输入规则与词库（2–3 周）→ change `input-rules` + `userdict-v2`

- 中英切换方式（顶栏/Shift/空格滑动三选一默认）、简繁/全角/标点 UI 化。
- 模糊音/简拼设置入口；编码提示开关；`/ok` 键位表 UI 化。
- 用户词库浏览/编辑/置顶/删词/调频；整机备份与恢复（导入导出）。
- 常用语/快捷短语（P1）；联想预测可选导入 `predict.db`（有开关，缺件不报错）。
- 验收：输入规则逐项对等 + 词库导入导出闭环 + 真机。

### M4 · 稳定与发布（1–2 周）→ change `release-hardening`

- 设备矩阵：API 26 / 33 / 36 + 至少一台折叠/大屏（如有）；启动/内存/帧率回归。
- 崩溃与日志：Rime 启动异常降级、部署失败可恢复 UI、日志导出。
- 数据安全：备份恢复、卸载重装、升级路径、隐私文案（去掉麦克风等过时描述）。
- 发布：签名 release APK/ AAB、版本号规则、CI 产物归档、本机安装脚本。
- 验收：连续 7 天日常使用无阻塞问题；性能预算不劣化；备份可恢复。

### D1 · 桌面共享内核（2–3 周）→ change `desktop-core`

- 桌面 gradle 模块：`:desktop:engine`（DesktopServer 重写）、`:desktop:protocol`；`protocol-common` 升 commonMain。
- 与 `:core:model` 汇合；解决 P1-1 字段错位（一次 golden 重录窗口）与协议词汇表二选一。
- macOS：安装到 `~/Library/Input Methods/`、中英切换（`flagsChanged`）、候选窗先用系统 `IMKCandidates` 但补齐高亮/定位/重复文本选择；改配置后能重载。
- 验收：macOS 本机连续 1 天日常输入；协议 golden 不回归。

### D2 · CMP 桌面落地（3–4 周）→ change `desktop-cmp`

- **前置 D0 spike**（1 周，独立）：macOS 非激活候选窗、Windows 无激活窗口、Linux X11 定位 + Wayland 降级路径；结论写进 change。
- `:ui:design`/`:ui:ime`/`:ui:settings` 加 `jvm()` target，依赖切 CMP，桌面设置 App + 候选窗/状态窗用共享 UI。
- macOS 壳接 CMP 窗口（Swift 只保留 IMK + 定位 + 输入上报）。
- Linux ibus 壳 + 候选窗（X11 CMP；Wayland 走 ibus panel 或 GTK4 面板）。
- Windows TSF 薄壳 + CMP 候选窗（`WS_EX_NOACTIVATE` 补洞）。
- 验收：每平台「能输入 + 候选可选 + 不抢焦点 + 切换中英」冒烟；降级路径有记录。

### D3 · 桌面日常可用（3–4 周）→ change `desktop-parity`

- 三平台安装/自启/更新脚本；设置 App 全量共享；零手工配置文件。
- 与手机对齐输入规则、词库导入导出、备份；桌面快捷键与状态栏。
- 验收：macOS + Linux 连续 7 天日常使用；Windows 至少 2 天；无 P0 缺陷。

### E · Path B（并行研究线，不占主线）

- 现状：规格齐（`doc/pathb-syllabifier-spec.md` 41 facts）、harness 12/60 golden、实现 0 行。
- 触发条件：D1 完成后评估「是否仍需摆脱 librime 运行时」；先补语料到 ≥60 并跑 H4（L0≥95%），未达标不进入编码。
- 不阻塞任何里程碑；无独立工期承诺。

### 依赖图

```
M0 → M1 → M2 ─┬→ M3 → M4
              └→ (settings-v2 可与 surface-p0 串行执行)
M4 → D1 → D0 spike → D2 → D3
E: 并行，D1 后评估
```

---

## 6. 首批 OpenSpec 变更拆分

### 6.1 `surface-foundation`（已创建，对应 M1）

- **目标**：功能对等重构表面层，建立五层架构与 CMP-clean 边界。
- **范围**：模块拆分、单一状态、布局模型、设计 token、手势/反馈统一、面板注册表、旧模型删除、测试补齐。
- **不在范围**：新功能、设置 IA、CMP 第二 target、桌面、引擎/协议。
- **验收**：`specs/keyboard-surface/spec.md` 的 7 条需求 + 真机功能对等清单。
- **位置**：`openspec/changes/surface-foundation/`。

### 6.2 `surface-p0`（M2，待 `surface-foundation` 合并后创建）

- **目标**：候选条 v2、键面提示、触感、顶栏开关、键盘手感调优。
- **依赖**：`surface-foundation` 的 token/布局/状态 API。
- **预估**：10–14 个任务。

### 6.3 `settings-v2`（M2/M3）

- **目标**：设置 IA 六组、`SettingSpec` 注册表、删除死设置、设置搜索、主题/候选/方案入口。
- **依赖**：`surface-foundation`（设计 token）；与 `surface-p0` 串行（共享 `:ui:design` 与 `:ui:ime` 少量文件）。
- **预估**：10–12 个任务。

### 6.4 后续预留（不在本批创建）

`input-rules`（M3）、`userdict-v2`（M3）、`release-hardening`（M4）、`desktop-core`（D1）、`desktop-cmp`（D2，含 D0 spike）、`desktop-parity`（D3）。

### 6.5 变更执行纪律

- 一个 change 一个主写入会话；`design.md` 定稿后再动代码。
- 任务单写清「可写路径白名单」；禁止 `git add -A`。
- 机器闸门由非作者执行/复核；真机项必须在任务单里列出验收人（你或代理）。
- 每个 change 完成后归档，并回写本文路线图的实际状态（避免文档漂移）。

---

## 7. 工程治理

### 7.1 文档权威链

| 层级 | 文档 | 职责 |
|---|---|---|
| L1 权威 | `doc/master-plan.md`（本文） | 产品定位、架构、路线、裁决 |
| L2 执行 | `openspec/changes/<change>/` | 单期 proposal/spec/design/tasks/实现记录 |
| L3 专项 | `doc/harness-design.md`、`doc/pathb-syllabifier-spec.md`、`doc/architecture-review.md` | 引擎 oracle / Path B / 历史评审证据 |
| L4 规格 | `openspec/specs/` | 已归档能力的长期契约（当前仅 input-schemas / profile-storage） |
| 历史 | `doc/refactor/roadmap.md`、`doc/multiplatform-design.md`、`doc/surface-plan.md` | 保留证据，不再执行 |

### 7.2 闸门（每个 change 必过）

1. **构建**：`./gradlew test :app:assembleDebug spotlessCheck` 全绿。
2. **结构**：`grep` 守护——`:ui:*` commonMain 零平台 import；设计 token 无字面量；`SettingSpec` 无孤儿；旧模型零引用。
3. **行为**：真机功能对等清单 + 关键路径冒烟（启用→出字→候选→上屏→切换面板→设置生效）。
4. **性能**：就绪 <2s、首键 <300ms、按键反馈一帧内、候选滚动无掉帧；记录口径与基线对比。
5. **视觉**：截图走查（浅/深/横竖屏/各面板）人工确认。
6. **文档**：change 实现记录 + 本文状态回写。

### 7.3 CI 与发布

- Android：提交/PR 自动构建 + 单测 + spotless（现状已有 `commit-ci`）。
- 桌面：`desktop-ci.yml` 增 path-filtered 触发（`desktop/**`、`harness/**`、`macos/**`），重活（native 构建）保留手动；windows leg 增加 DLL 加载冒烟（P2-9）。
- Release：补齐 `SIGNING_KEY`/`KEY_STORE_PASSWORD`/`ALIAS`/`KEY_PASSWORD`（你操作）；版本号 + 更新日志自动生成。
- 本机安装：一条 `script/install-debug.sh`（adb install + ime enable），减少手工步骤。

### 7.4 多 agent 协作纪律（沿用上一轮验证过的做法）

- 任务板 + 写入范围白名单；同一文件同一时间只有一个写者。
- 「设计/实现/验证」三角分离：规划产出 design；实现只动白名单；验证者独立跑闸门。
- 提交前 `git status` 复核、显式 `add`、commit 后 `show --stat` 复核；不 push 由你决定。
- 并发会话在途文件（本仓库历史上出现过）必须显式排除，不 `add -A`。
- 每波结束写台账（`expert_team_state.md` 模式可继续用，或改为 change 内 `implementation-notes.md`）。

### 7.5 真机验收清单（M1 起固定使用）

- 启用/切换默认输入法；锁屏可输入（direct boot）；冷启动就绪时间。
- 26 键/36 键/数字/编辑键盘切换；长按/滑动/空格滑光标/双空格句号。
- 候选：横向/翻页/长按菜单/删词撤销；中英混输；标点/符号/emoji；剪贴板粘贴与敏感遮挡。
- 设置：修改行高/深浅/强调色/候选数量后实时生效；导入字体；备份/恢复。
- 异常：部署失败提示可恢复；切换应用不丢候选；横竖屏切换不崩。

---

## 8. 风险清单

| # | 风险 | 影响 | 缓解 |
|---|---|---|---|
| R1 | **CMP 桌面候选窗被平台约束证伪**（macOS 不抢焦点、Wayland 定位） | 桌面 UI 一套代码目标部分落空 | D0 spike 前置；按平台降级原生候选窗，不推翻整体；本文 §3.5 已记录判据 |
| R2 | **M1 重构范围失控**（顺手加功能/顺手改引擎） | 里程碑延期、回归难归因 | 功能冻结 + 对等清单 + 白名单 + 非作者验闸 |
| R3 | **设置迁移回归**（14 死项删除 + 6 组重排） | 用户找不到原设置、行为变化 | `SettingSpec` 全量对照 + 逐项对等 + 真机 |
| R4 | **单人带宽/并发 agent 冲突** | 合并冲突、互相覆盖 | 一次一个 change；写入范围白名单；任务串行 |
| R5 | **Path B 黑洞** | 吞掉桌面/移动迭代时间 | 只做并行研究线，H4 门不过不编码 |
| R6 | **无 UI 自动化** | 视觉/交互回归靠人眼 | 引入 Compose UI 测试（结构与纯逻辑）+ 截图集；真机清单 |
| R7 | **主题审美主观** | 反复返工 | 每轮 2–3 个候选截图评审；token 化后调参成本低 |
| R8 | **桌面分发/签名** | 无法日常安装 | 自用 adhoc/自签 + 安装脚本；不追求商店分发 |
| R9 | **数据/词库丢失** | 用户词库不可恢复 | M3 先做导入导出/备份，再做词库编辑 |
| R10 | **文档再次漂移** | 回到「零零散散」 | 本文唯一权威 + 每期回写状态 + 旧文档头注 |

---

## 9. 待你拍板（4 项，均有默认路径）

| # | 事项 | 选项 | 默认（不回复则按此执行） |
|---|---|---|---|
| DEC-1 | Linux 原生 Wayland 的候选窗 | A 允许降级为 ibus panel/GTK4 原生面板，其余 UI 仍 CMP；B 坚持 CMP（大概率不可行）；C 放弃 Wayland 原生、只支持 X11 | **A** |
| DEC-2 | M1 是否引入 CMP 第二 target | A 不引入，M1 只做 CMP-clean，D1 再加 jvm target；B M1 就加 jvm target 并跑桌面预览 | **A** |
| DEC-3 | 词库/联想优先级 | A 放 M3（P1）；B 提前到 M2 | **A** |
| DEC-4 | 36 键/横屏分屏去留 | A 保留 36 键、横屏只做自适应宽度、删分屏死设置；B 删 36 键；C 现在做分屏 | **A** |

---

## 附录 A · 关键证据索引

| 事实 | 证据 |
|---|---|
| 表面层 82 文件、43 平台面/39 可移植 | 表面层盘点 §1/§4（基于 `app/src/main/java/com/jiaoay/jrime/` 逐文件 wc/grep） |
| `ImeUiState` 五职责 | `ime/compose/ImeUiState.kt:30-99,115-128` |
| 遗留 Keyboard/Key 861 行 | `ime/keyboard/Keyboard.kt`（500 行）、`Key.kt`（361 行） |
| 53 偏好/7 死项/14 无效可见项 | `data/prefs/AppPrefs.kt:76-427`；产品盘点 §2 |
| 振动无调用者 | `ime/keyboard/InputFeedbackManager.kt:174-181`；`grep keyPressVibrate` 零调用 |
| 运行时 YAML 仅 4 类 | 引擎盘点 §2（2 份 build YAML 77KB + user.yaml 条件 + 音效 kaml） |
| 首装 4.6s / 稳态 <2s | `openspec/changes/startup-fastpath/implementation-notes.md:231-233,315-319` |
| macOS 壳 352 行、中英切换断 | `macos/JRimeIME/`；`InputController.swift:45-55`；`default.yaml:6-13` |
| Linux/Windows 零壳代码 | 仓库 `find` 无 ibus/TSF 源；`doc/multiplatform-design.md:149,155` |
| CMP 使用量为 0 | `settings.gradle.kts:24-25`；全仓无 `multiplatform`/`jvm()`/`org.jetbrains.compose` |
| APK arm64 80.12MB | `app/build/outputs/apk/debug/...arm64-v8a-debug.apk`（2026-11-20） |
| Path B 实现 0 行、12/60 语料 | 引擎盘点 §4；`harness/corpora/baseline.jsonl` |

## 附录 B · 术语

- **表面层（surface）**：输入法 UI——键盘、候选条、顶栏、面板、气泡、弹窗。
- **CMP-clean**：UI 代码不 import 任何平台 API，平台能力全部经接缝接口注入；可被 Compose Multiplatform 直接编译到第二平台。
- **接缝（seam）**：跨平台边界接口，如 `EngineSession`、`PlatformInputHost`、`PlatformEffects`。
- **SettingSpec**：设置项的声明式单一事实源（key/类型/默认/分组/校验），UI 与消费者均由它派生并由测试约束。
- **Path B**：以 Kotlin 引擎替代 librime 运行时的中期方案（见 `doc/multiplatform-design.md` §2）。
- **D0 spike**：D2 前的桌面候选窗可行性验证（三平台）。
- **功能对等清单**：重构前后逐项勾验的用户可见行为列表，防止「重构顺手丢功能」。
