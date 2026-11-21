# Spec Delta

## Purpose

键盘表面层（键盘、候选条、顶栏、面板）的结构契约：单一会话状态与单向数据流、
数据驱动布局、设计 token 单一源、统一手势与反馈、可扩展面板注册表、平台无关边界，
并以功能对等与性能不劣化为硬约束。

## ADDED Requirements

### Requirement: 单一会话状态与单向数据流

表面层 SHALL 只在 `ImeSessionState` 中保存 UI 所需状态（组合态、候选、高亮、ascii、
面板、键盘 id、行高、配色模式），该状态为不可变快照；UI SHALL 只通过 `ImeCommand`
表达用户动作，不得直接调用引擎或输入连接。引擎事件 SHALL 经状态机产出新状态，
Composable 只读状态。

#### Scenario: 按键只发命令

- **WHEN** 用户点击字母键、候选或面板按钮
- **THEN** 对应 Composable 只派发 `ImeCommand`，由命令执行器调用引擎/输入连接
- **AND** 表面层不存在第二条直调引擎的上屏路径

#### Scenario: 状态只有一个写者

- **WHEN** 追踪候选、组合态、面板、键盘 id 的写入点
- **THEN** 均只由状态机/命令执行器写入，不存在静态字段或第二份可变副本与之并行

### Requirement: 数据驱动布局与功能对等

键盘布局 SHALL 由类型安全的数据模型（`KeyboardLayout`/`RowSpec`/`KeySpec`/`KeyActionToken`）
定义并被注册表按 id 提供；布局数据 SHALL 携带现有全部键盘族的行为槽位（单击、长按、
长按重复、四向滑动、键内提示），替换后用户可达功能不减少。

#### Scenario: 键盘族完整可达

- **WHEN** 依次切换 26 键、36 键、数字、电话、编辑键盘
- **THEN** 每个键盘的行列、按键、长按/滑动行为与替换前一致，切换无跳变

#### Scenario: 遗留像素模型退役

- **WHEN** 在代码库检索 `Keyboard`/`Key` 旧渲染类型的实例化与引用
- **THEN** 引用为零；键盘切换不再构造任何遗留像素对象

### Requirement: 设计 token 单一源

`:ui:*` 的尺寸、间距、圆角、字号、颜色与动效 SHALL 只来自 `:ui:design` 的 token；
组件内不得出现 `.dp`/`.sp`/`Color(0x…)` 字面量（token 文件与测试除外）。浅色/深色
两套语义色 SHALL 完整覆盖现有视觉，默认跟随系统。

#### Scenario: 组件无字面量

- **WHEN** 扫描 `:ui:ime` 与 `:ui:settings` 的源文件
- **THEN** `.dp`/`.sp`/`Color(0x` 字面量零命中（测试文件除外）

#### Scenario: 深浅色由 token 切换

- **WHEN** 系统在日间/夜间模式之间切换
- **THEN** 键盘、候选条、顶栏、面板整体切换到对应语义色，无需组件内单独判断

### Requirement: 统一手势与反馈

表面层 SHALL 使用单点手势识别器与单点阈值配置解析单击、长按、长按重复、四向滑动、
空格滑光标、双空格；按键反馈 SHALL 经统一 `FeedbackSink`（触感/音效/TTS）发出，
同一事件只触发一次。触感反馈 SHALL 在按下/长按时实际生效，并受现有振动偏好控制。

#### Scenario: 手势阈值一致

- **WHEN** 修改长按/滑动阈值偏好后重新按键
- **THEN** 所有键面与 chrome 键立即按新阈值解释手势，不存在某类键使用旧阈值

#### Scenario: 振动可用

- **WHEN** 开启振动并按任意字母键
- **THEN** 设备产生触感反馈；关闭后不再产生
- **AND** 长按/长按重复按各自偏好生效

### Requirement: 可扩展面板与工具栏注册表

面板 SHALL 通过 `ImePanel` + `PanelRegistry` 注册，宿主只负责动画、高度与生命周期；
工具栏动作 SHALL 通过注册表声明，不得以字符串匹配或 `when` 硬编码面板分支。
新增一个面板 SHALL 只需新增注册项与实现，不改宿主与 DI 装配。

#### Scenario: 面板可插拔

- **WHEN** 打开剪贴板、符号/Emoji、编辑入口
- **THEN** 由注册表解析到对应面板并渲染，宿主代码无面板专属分支

#### Scenario: 未知面板不崩溃

- **WHEN** 请求一个未注册的面板 id
- **THEN** 表面层保持当前状态并记录错误，不崩溃

### Requirement: 平台无关边界（CMP-clean）

`:ui:design`、`:ui:ime`、`:ui:settings` 的 commonMain SHALL 不 import 任何平台 API
（`android.*`、`androidx.*`、`java.*` 及 Android 专属库）；键盘输入、上屏、窗口、触感、
剪贴板、存储等能力 SHALL 经接缝接口（`EngineSession`/`PlatformInputHost`/`WindowHost`/
`PlatformEffects`/`SettingsStore`）注入。模块依赖 SHALL 满足：core 不依赖 ui/app，
ui 不依赖 app/core:rime/core:data。

#### Scenario: UI 层平台 import 零命中

- **WHEN** 对 `:ui:*` 的 commonMain 做平台 import 扫描
- **THEN** 零命中；平台能力只出现在接缝接口的实现与 `:app` 装配层

#### Scenario: 接缝可替换

- **WHEN** 为 `EngineSession` 提供一个测试替身
- **THEN** 表面层可在无 Android 运行时的环境下渲染/测试状态与交互

### Requirement: 行为对等与性能不劣化

重构 SHALL 保持用户可见行为与重构前一致（唯一例外：振动反馈从失效恢复为可用）；
键盘可见到可交互时间、按键按下到视觉反馈、候选滚动帧率三个口径 SHALL 在重构前后
各实测一轮并归档，劣化需主人明确放行。

#### Scenario: 功能对等清单逐项通过

- **WHEN** 按对等清单执行真机冒烟（出字/候选/长按/滑动/空格/面板/中英/编辑）
- **THEN** 每项行为与重构前一致，缺失项为零

#### Scenario: 性能实测归档

- **WHEN** 重构前后分别测量三个性能口径
- **THEN** 数字写入实施记录，劣化项须有主人放行结论
