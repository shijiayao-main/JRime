# Design: native-theme

> 修订（2026-11-15）：主人拍板渲染层用 **Jetpack Compose**（原草案的"保留
> Canvas 自绘管线"假设作废）。理由（主人原话）：自绘管线长期价值有限，
> Compose 对后续多平台（KMP/Compose Multiplatform，参考 EasyDevToolbox 方向）
> 支持更有优势。落地纪律随之明确：**Composable 保持纯 Compose**——除阶段 1
> 的互操作过渡外不引入 Android View 依赖，保 UI 代码的多平台可迁移性；
> IME 窗口本体是 Android 平台件（InputMethodService），多平台收益体现在
> UI 范式/组件/技能的跨面复用。本版按 Compose 重写设计。

## Context

`data/theme` 的 `Theme` 数据类（`Theme.kt:22-32`）是纯 Kotlin 模型：`style /
preedit / window / liquidKeyboard / presetKeys / presetKeyboards /
presetColorSchemes / fallbackColors / toolBar` + `resolveAction`，`ime/` 内 55
个消费方持有的都是它；YAML 生态只是这个模型的**生产者**。因此本变更的形状是
"模型保留、生产者换血、渲染层重建"：`Theme` 模型与按键行为解析（`KeyAction`）
保留，YAML 解析与切换机器删除，NativeTheme 硬编码构建器取而代之；IME 窗口的
渲染从自绘 View 层整体重写为 Compose，消费同一模型。`ime/core` 的 service
管道（`TrimeInputMethodService` / `InlineSuggestions` / `NavigationBarManager`
等）不动。

## Goals / Non-Goals

**Goals**
- 主题唯一来源 = 代码：类型安全 DSL 构建 `Theme` 模型，编译期校验。
- **渲染层 Compose 化**（主人拍板）：IME 窗口整体为 Compose UI——键阵、长按
  弹窗、候选条、组合区、符号/liquid 面板、剪贴板、开关全部 Composable 化；
  技术栈与路线图期 5/6 的 Compose 方向统一。
- Gboard 风格视觉：亮/暗两套硬编码配色（跟随系统日夜间），Material 语言的
  圆角/留缝/明度分层。
- 辅助码 `/` 专属按键进默认布局底行（占已死的方案切换键位）。
- 行为面等价：4.1 闭包分析（7 键盘 / 71 preset_keys 行为槽位清单）为移植基线。
- **输入延迟有实测验收基线**：窗口显示到可交互、按键到视觉反馈两个口径
  实测归档，不显著劣于替换前（显著与否由主人验收判定）。

**Non-Goals**
- 不做任何形式的用户主题/配色切换（删除即终态，不留兼容层）。
- 不动 `ime/core` service 管道与 Rime 桥（输入事件→`KeyAction`→`Rime` 的
  路径仅换 UI 载体，逻辑不动）。
- 不动 `input-schemas` / `profile-storage` 的任何需求。

## 1. NativeTheme 构建器（模型层，与渲染无关，设计不变）

新包 `com.jiaoay.jrime.theme`，`NativeTheme` 按需构建并持有单例 `Theme`：

- **布局 DSL**：`keyboard("default") { row { key("q") { click="q"; longClick="`" } } }`
  类型安全构建器，产物为现行 `TextKeyboard / Key / PresetKey` 模型对象；每键
  13 个行为槽位（`TextKeyboard.kt:79-91`）具名参数化。
- **配色**：`GboardColors.Light / Dark` 常量表（Material 表面色起手：亮色键面
  `#F1F3F4` 系/背景 `#FFFFFF` 系、暗色键面 `#3C4043` 系/背景 `#202124` 系、
  强调 `#1A73E8`/`#8AB4F8`；对照 Gboard 截图校准），装配为 default + dark_scheme
  两套走现行跟随链；多主题选择/切换 UI 删除。
- **GeneralStyle**：Gboard 视觉参数（圆角 ≈8dp、键缝 ≈4-5dp、无边框、轻微或
  无阴影、字号/键高校准），硬编码进 builder。

## 2. Compose IME UI 层

### 2.1 窗口与组合树

`TrimeInputMethodService.onCreateInputView` 返回单个 `ComposeView`，承载整个
IME 窗口组合树：

```
ImeRoot
├─ CompositionBar（preedit/编码区，读 preedit 模型 + GeneralStyle）
├─ CandidateBar（候选列表/翻页/高亮）
├─ PanelHost（按窗口状态切换）
│  ├─ KeyboardPanel（7 键盘共用：行×键网格，读 TextKeyboard 模型）
│  ├─ LiquidSymbolPanel（符号/表情 liquid 面板：tab 条 + 网格）
│  ├─ ClipboardPanel / SwitchesPanel（阶段 2 移植，期初 AndroidView 互操作过渡）
└─ KeyPopup（长按预览/符号选择弹层，Compose Popup）
```

- 布局数据（`Theme` 模型）在组合外持有（进程单例），Composable 只读；
  `remember` 缓存行/键派生结构，避免每帧重建。
- **状态粒度**：按键按下态为每键局部 `mutableStateOf`，按一个键只重组该键；
  窗口级状态（当前键盘、面板、候选、preedit）各自独立 `State` 流。
- 输入事件：`Modifier.pointerInput` 采集 press/long-press/swipe（13 槽位中的
  手势槽），解析复用现行 `KeyAction` / `resolveAction`，派发路径不变。
- 主题色/样式以 `CompositionLocal`（`LocalImeStyle`）下发，亮暗切换 = 换
  `CompositionLocal` 提供值。

### 2.2 性能设计（延迟敏感面的纪律）

- 键阵用普通 `Column/Row` 组合（按键数固定 ≤35，无列表惰性开销），每键
  `Box + Text` 最小组件树；不用 `LazyGrid`。
- 按键反馈（按压色变化）只依赖该键局部状态；避免窗口级重组。
- 首帧：IME 窗口视图每个 service 生命周期创建一次（非每输入框），Compose
  首帧成本一次性；release 构建开 R8；实测口径见验收。
- **验收基线**：替换前后各实测一轮——窗口显示到首个按键可响应、按键按下到
  视觉反馈（录屏逐帧或 systrace），数字归档 `implementation-notes.md`，
  劣化是否可接受由主人判定（spec `keyboard-theme` 的延迟需求）。

### 2.3 辅助码按键

默认键盘底行第 2 槽位（现 `Schema_switch` 键位）：`click = "/"`（KEYCODE_SLASH，
`/` 已在 speller alphabet 内），label `/`、hint 标注辅助码；`long_click =
IME_switch` 保留。组合中按它即进入 `ni/h` 辅助码与 `/A` 声调过滤
（`cand_selector.lua:131-141` 既有机制，零引擎改动）。

## 3. 删除清单（随阶段推进）

- **模型/解析层**：`data/theme` 的 `ThemeLoader / ThemeYaml / ThemeDslExpander /
  ThemeFilesManager / ThemeDiagnostics / ColorTable / BitmapSampling`；保留
  `Theme` 模型、`ColorSchemeResolver` 跟随逻辑、字体加载。
- **资产**：`assets/shared/trime.yaml` + `flyhe.trime.yaml`（~300KB，checksums
  连带缩减）。
- **渲染层（被 Compose 取代）**：`ime/keyboard`（KeyboardView/KeyView 等）、
  `ime/bar`、`ime/candidates`、`ime/popup`、`ime/composition`、`ime/window`
  的 View 实现，及 `ime/symbol / clipboard / switches` 的 RecyclerView 适配器
  ——随各阶段移植进度分批删除，期初未移植面板经 `AndroidView` 互操作过渡。
- **UI/按键**：主题/配色设置页、`Theme_settings / Color_switch / set_theme`
  键与字符串；`Schema_switch` 键（位置让给辅助码键；F4 硬件键不受影响）。
- **测试**：golden 主题测试的 YAML 装载路径删除，改为 NativeTheme 模型断言
  （布局/键位/配色/行为槽位）+ Composable 渲染的 Robolectric/单测抽样。
- 期 2 `startup-fastpath` 任务 4.2 标注被本变更取代。

## 4. 交付阶段

- **阶段 1（核心打字环路）**：Compose BOM 依赖 + NativeTheme DSL/配色 +
  7 键盘移植（4.1 闭包为清单）+ 辅助码键 + `ImeRoot`（CompositionBar/
  CandidateBar/KeyboardPanel/KeyPopup）+ liquid 符号面板 + 未移植面板
  AndroidView 互操作 + 换源删除 YAML 机器；延迟基线实测（替换前后各一轮）。
  出打字回归 + 亮暗截图。
- **阶段 2（面板收尾与精修）**：剪贴板/开关面板 Compose 化并删互操作与全部
  旧 View 层；视觉精修迭代（截图→主人意见→校准）；终验。

## Risks / Trade-offs

- **输入延迟**（Compose 重写的核心风险）：以 2.2 的状态粒度纪律压重组范围，
  以实测基线兜底；主人已知情选择 Compose，劣化判定权在主人。
- **移植面宽**（~10.5k 行 View 层 + 55 消费方）：分阶段 + 互操作过渡保行为
  等价；4.1 闭包清单 + 模拟器回归兜底行为面。
- **手势槽位复杂**（swipe/long-press/popup）：`pointerInput` 实现滑动手势
  与现行 `KeyAction` 语义对齐是阶段 1 的技术难点，单测+真机回归覆盖。
- **视觉主观性**：色值/圆角以截图迭代为准。
- **上游割裂**：主题与渲染层从此不追 trime 上游（既定路线）。

## Open Questions

1. Gboard 配色基准参照（默认：主人常用机型当前版 Gboard，阶段 1 截图时参照）。
2. 4.1 的四个开放问题（hydrangea/死 anchor/B 集）随 YAML 主题消亡作废（记录）。
3. 延迟劣化的可接受阈值不预设数字，以"替换前后实测对比 + 主人验收"为准。
