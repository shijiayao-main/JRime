# Proposal: native-theme

## Why

主人对现主题的两点判定：视觉上太丑（对标 Gboard 的现代风格），辅助码单字输入在
手机上没有直达按键（`/` 分隔符藏在 `o` 键长按里）。而 trime 的主题引擎本身是为
"用户可配置"服务的 YAML 生态——解析层 `data/theme` 31 文件 3,260 行、两份主题
YAML 资产约 300KB、18 套调色板、主题切换 UI、诊断器——对一个单用户专精输入法
全是冗余。与其继续在 YAML 里裁剪/重设计（期 2 任务 4.2 的原方向），不如把整个
可配置主题引擎舍弃，直接原生开发一套硬编码主题。

## What Changes

- **删除 YAML 主题引擎**：`data/theme` 解析层（YAML 解析、preset 模型、调色板
  解析、GeneralStyle 解析、ThemeDiagnostics）、`trime.yaml` + `flyhe.trime.yaml`
  主题资产、主题/配色切换 UI（ThemeSettings/ColorPicker 相关）、对应 golden 主题
  测试，全部移除（**BREAKING**：不再支持任何用户主题与配色切换）。
- **新增原生主题**：硬编码 Kotlin 主题模块——键盘布局用类型安全 DSL 定义、
  Gboard 风格配色（亮/暗两套常量、跟随系统日夜间）、按键定义内置；主题模型
  （`Theme` 数据类及其子模型）保留为消费契约。
- **渲染层换血为 Jetpack Compose**（主人拍板）：IME 窗口整体重写为 Compose
  UI——键盘键阵、按键长按弹窗、候选条、组合区、符号面板全部 Compose 化，
  消费同一 NativeTheme 模型；旧的自绘 View 层（`ime/keyboard / bar /
  candidates / popup / symbol / composition / window` 等约 10.5k 行）随各
  阶段移植进度删除。输入延迟以实测口径立验收基线（见 `keyboard-theme` spec）。
- **辅助码专属按键**：默认键盘底行新增发送 `/`（KEYCODE_SLASH）的按键，占
  单方案化后已无意义的方案切换键位；一键进入辅助码/声调过滤模式（`ni/h`、
  `/A` 声调），长按保留 IME 切换。
- **布局功能面以 4.1 闭包分析为基线**：期 2 任务 4.1 已推导的键盘/按键引用闭包
  （7 键盘 / 71 preset_keys 的行为面）作为原生布局必须携带的功能清单；
  任务 4.2（YAML 裁剪施工）被本变更取代作废。
- **分阶段交付**：阶段 1 = 核心打字环路（默认键盘 + 候选条 + 组合区 + 符号页），
  阶段 2 = 其余面板（liquid 符号面板、剪贴板、开关、弹窗余项），各阶段独立
  过门并出模拟器截图给主人过目迭代。

## Capabilities

### New Capabilities

- `keyboard-theme`: 原生硬编码主题系统的契约——布局与配色的唯一来源是代码
  （无任何运行时主题解析/切换）、Gboard 风格视觉、亮暗跟随系统、辅助码按键
  在默认布局可达、渲染性能不劣于现自绘管线。

### Modified Capabilities

（无——`input-schemas` 与 `profile-storage` 的需求不受影响。）

## Impact

- **删除面**：`data/theme/`（31 文件 3,260 行）、`assets/shared/trime.yaml` +
  `flyhe.trime.yaml`（~300KB，连带 checksums 表缩减）、主题切换/配色 UI 及其
  资源字符串、ThemeDiagnostics 与相关单测、期 2 任务 4.2；以及被 Compose 取代
  的旧自绘 View 层（约 10.5k 行，随各阶段移植进度分批删除）。
- **新建面**：原生主题模块（布局 DSL + Gboard 配色常量 + 按键定义，约 1.5-2k
  行）；Compose IME UI 层（键阵/候选条/组合区/弹窗/面板各 Composable）；
  Compose BOM 依赖引入（路线图期 5/6 的 Compose 方向提前落到 IME 窗口）。
- **适配面**：`ime/` 内 55 个引用主题数据的文件——模型保留使其接口稳定，
  改动集中在渲染消费端（View 类换 Composable）；`ime/core` 的 service 层
  管道（TrimeInputMethodService/InlineSuggestions 等）不动。
- **性能**：IME 窗口为延迟敏感面，Compose 重写以状态粒度设计（按键态单键
  粒度重组）压延迟；以"窗口显示到可交互、按键到视觉反馈"实测口径立基线
  验收（见 spec），启动路径另少掉主题 YAML 解析（几十 ms 级，与期 2 同向）。
- **上游同步**：与 vendored librime 同理，主题与渲染层从此不追 trime 上游
  （专精 fork 的既定路线）。
