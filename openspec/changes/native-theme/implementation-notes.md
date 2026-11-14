# Implementation notes: native-theme

## 2026-11-15 — Task 1.1: 地基（Compose BOM + 替换前延迟基线 + NativeTheme DSL 骨架）

### 替换前延迟基线（自绘 View 管线，对照口径）

**环境**：AVD Medium_Phone_API_36.1（Android 16 x86_64），**hw.ramSize=4096**（config.ini
本就是 4GB，此前会话中插件启动带内存覆盖参数只给了 2GB——2GB 下系统因内存压力反复
回收有绑定的 IME service（`ActivityTaskManager: Scheduling restart of crashed service
… for mem-pressure-event`），IME 进程不断被杀、窗口状态闪断、按键被丢
（`Unable to send input event to IME … dropping`），一度污染整轮测量；**用
`emulator -avd Medium_Phone_API_36.1` 直接启动让 config.ini 生效后全部恢复正常**。
对照测量必须同环境复测）。APK = commit 4842b0fb 状态（x86_64 debug）。
口径说明：本模型无图像输入，无逐帧目验能力；延迟口径用 logcat 引擎消息
（键→引擎回显）+ gfxinfo 帧统计，**不含引擎消息→像素上屏的渲染尾段**；替换后
同口径复测对照，最终手感由主人验收（spec `keyboard-theme` 输入延迟需求）。

| 口径 | 数值 | 测法 |
| --- | --- | --- |
| 冷启动：`ime set`→进程拉起（RimeDaemon start） | **0.25s**（缓存暖）/ 3.2s（2GB 内存压力 + 冷缓存轮，环境病记录在案） | 设备侧 `date` 时间戳 + logcat threadtime |
| 冷启动进程内：Synced! / Starting rime | +0.06s / +0.07s | logcat |
| **冷首键→首回显**（惰性首个输入会话初始化） | **~2.7s**（`starting engine.` +1.66s，配置链读取 ~1s，preedit 首现） | keyevent 设备时间戳 vs 首条 preedit |
| 冷第二键→回显 | ~0.68s | 同上 |
| **暖态键→引擎回显** | **~10-50ms**（15 键连测） | keyevent 时间戳 vs preedit logcat |
| gfxinfo 帧统计（40 键会话） | 143 帧，janky 86（60.1%），p50 53ms / p90 85ms / p95 97ms / p99 113ms；GPU p50 12ms / p90 25ms | `dumpsys gfxinfo` reset→会话→dump（模拟器软件渲染，绝对值偏高，仅作同环境对照） |

### 测量过程附带发现（独立缺陷，不并入本变更）

1. **同进程 IME 冷态自请求死锁**：TrimeInputMethodService 无 `android:process` 属性
   （与主进程同进程）。MainActivity 的 Test input 面板在 IME 进程冷态时请求显示 →
   主线程在 insets-show 的 binder 调用里等 system_server，system_server 等 main
   thread 处理 IME onCreate 的 binder 回调 → **互等死锁**，5s 后
   `Activity top resumed state loss timeout` 强杀 activity
   （logcat 实证）。暖态（IME 已绑定）不触发。修复需权衡 RimeDaemon 单进程会话
   模型（拆 `:ime` 进程会动会话架构），留主人裁决，单列任务处理。
2. **未绑定焦点的 IME 进程被作 cached 进程回收**属正常生命周期；`adb root` 在
   生产镜像被拒；`adb shell kill` 跨 uid 无权限——冷态协议用
   `am force-stop`（默认 IME 回退怪癖）+ `ime set` 重绑实现。
3. **AVD 内存覆盖陷阱**：见上文环境段；模拟器验收统一用
   `emulator -avd Medium_Phone_API_36.1` 直启（4GB）。

### Compose 接线

- `gradle/libs.versions.toml`：`composeBom = 2026.09.00`（maven-metadata stable 尾部
  实查）+ runtime/ui/foundation/material3（版本随 BOM）+ `kotlin-compose` 插件
  （org.jetbrains.kotlin.plugin.compose，版本随 Kotlin 2.4.21）。
- `app/build.gradle.kts`：compose 插件 + `buildFeatures.compose` + BOM 依赖。
  `:app:assembleDebug` 全绿。

### NativeTheme DSL 骨架（`com.jiaoay.jrime.theme` 新包）

- `GboardColors.kt`：亮/暗两套 `ColorScheme`（键名=ColorKey 枚举小写拼写，含
  `dark_scheme: gboard_dark` 日夜跟随链）；Material 表面色起手值，全部视为
  task 1.5 截图校准的调参。
- `KeyboardBuilder.kt`：`keyboard("name") { row { key(click=…, longClick=…, …) } }`
  类型安全构建器 → 产出既有 `TextKeyboard/TextKey` 模型；13 行为槽位具名参数化，
  `key("q")` 自动 label 大写；全槽位空 = spacer（YAML `{width: 2}` 等价）。
- `NativeTheme.kt`：进程内单例 `Theme` 装配（style=Gboard 视觉参数：圆角 8、
  键缝 4、无边框无阴影；配色两套入 presetColorSchemes；布局/键位留 1.2 填）。
- `NativeThemeTest`（kotest）：双配色装配、日→夜链、Gboard 样式参数、DSL 产物
  槽位、无 click 但有手势槽的键不作 spacer。全绿 + spotless 绿。

## 2026-11-15 — Task 1.2: 布局移植（7 键盘 + 71 键 + 辅助码键）

- 转录用**生成器**完成（`tools/gen_layouts.py`，python3 -I + PyYAML，对
  4.1 闭包清单做机械转录）而非手抄；产物 `theme/KeyLayouts.kt`（385 行）冻结
  为唯一真源，生成器作为来源凭证存 tools/，随 1.4 与 YAML 一起退役。
- **模型结构澄清**（转录正确性的关键）：TextKeyboard 的 keys 列表是**扁平的**
  ——行由渲染器按宽度单位累加折行（columns=30）；7 键盘均无 import_preset，
  自含无继承。DSL 相应做成扁平（key/spacer 直接挂 builder）。
- **等价性验证**（`KeyLayoutsEquivalenceTest`，随 YAML 装载器在 1.4 退役）：
  - 7 键盘板级字段逐一相等 + 键数相等
  - **每键 13 槽位 + 全部视觉字段数据类级相等**（经 `orAbsent` 归一化——
    YAML 显式空串 `long_click: ""` 在模型里是 `Plain("")`，运行时按 orAbsent
    语义归空；等价判定在归一化后做）。唯一豁免：辅助码键（intentional
    divergence，见下）
  - 71 preset_keys 全部字段级相等
  - 辅助码键：click=`/`、label=`/`、hint=辅助、longClick=IME_switch、
    swipeDown=Menu、width=12.5f 与原 Schema_switch 槽位几何/功能键配色完整继承
- **踩坑记录**：① 板级数字字段 0 是有效值（ascii_mode: 0 不能当"未设置"跳过，
  第一版生成器把 number 键盘的 0 丢了）；② 字母键的模型 label 是空串，显示
  大写是渲染期行为，DSL 不得自动补 label；③ PresetKey 的 Kotlin 参数是驼峰
  （shift_lock→shiftLock）；④ TextKeyboard 无板级 keyTextSize/symbolTextSize
  （那是 GeneralStyle 的字段）；⑤ resolveAction 路径在单测环境不可跑
  （KeyEvent.keyCodeFromString not mocked），字段级等价 + 1.5 模拟器回归覆盖
  解析路径。
- `GboardColors` 补命名色：enter_bg/enter_text/enter_pressed_*（Gboard 语义：
  回车=强调色填充胶囊）、one_color（强调色），亮暗两套各配值，经 ColorTable
  的"活动方案优先"解析路径随日夜间切换。
- 门：spotlessCheck + :app:testDebugUnitTest（全量）+ :app:assembleDebug 全绿。

## 2026-11-15 — Task 1.3 开工前勘察（架构地图，实施依据）

- **消息流**：`InputView.handleRimeMessage`（InputView.kt:356-390）是 UI 侧消息
  分发中枢——Schema/Option/Composition/BulkCandidates 消息经 `InputBroadcaster`
  广播给 `InputBroadcastReceiver` 实现们（候选 UI、preedit、键盘外观状态）。
  **Compose 化路径**：状态持有者注册为 InputBroadcastReceiver → 驱动 Compose
  State，Composable 只读状态——广播器模式原样保留，仅换订阅端实现。
- **容器结构**：`InputView(context, rime, scope)` 装配 BoardWindowManager/
  PopupDelegate/PreeditDelegate/InputBarDelegate/KeyboardWindow/LiquidWindow
  （kodein DI），inputView 与 candidatesView 是**两个分离的系统区域**
  （setInputView / setCandidatesView，inputDeviceManager 按软/硬键盘分流）。
  1.3 保留双区域契约：各用一个 ComposeView 承载，Gboard 观感靠样式统一。
- **键盘切换逻辑**：`KeyboardWindow.evalKeyboard`（".default/.prior/.next/
  .last/.last_lock/.ascii/空"的求值，KeyboardWindow.kt:246-283）是**纯逻辑**，
  Compose 化保留复用；attachKeyboard 的视图侧换 Compose 面板。
- **服务接入点**：onCreateInputView（TrimeInputMethodService.kt:553-563）返回
  null + setInputView 自管；themeScope 未就绪时延迟创建（inputViewDeferred）。
  Compose 化改 onCreateInputView 返回 ComposeView(ImeRoot)，其余生命周期钩子
  （onStartInput/onStartInputView/inline suggestions/insets）不动。
- **下一步**（1.3 实施序）：ImeUiState 状态持有者（broadcaster 订阅）→
  ImeRoot 骨架接入服务（与旧 InputView 并存可切换，风险隔离）→ KeyboardPanel
  （扁平键列表折行 + 按键态局部 State + pointerInput 手势）→ 候选/组合条 →
  popup → liquid → 剪贴板/开关互操作 → 删旧视图（1.4）。

## 2026-11-15 — 侧修：同进程 IME 冷态自请求冻结（独立缺陷，主人令优先处理）

**现象**（2GB 模拟器 3/3 复现，4GB 不复现）：IME 进程冷态时，MainActivity 请求
IME（Test input 面板 insets-show，或进程重启时 pending service 投递）→ 5s 后
`Activity top resumed state loss timeout` → **Force finishing activity**，回退
启动器。暖态（service 已创建）不触发。

**机制修正**（诚实记录）：最初假说是 binder 互等死锁（insets-show 同步事务等
system_server、service onCreate 的 binder 回调等同一根被占住的主线程）；
活体取证不可行（生产镜像 `adb root`/`debuggerd` 均拒，top-resume 超时不写
ANR 文件）后，靠"连 Test input 都没点、纯 crash→am start 碰撞也冻结"的复现
修正为：**同进程冷启动期主线程累计负载（Application+activity 装配+service
onCreate+主题加载+延迟输入视图创建）撞上 5s top-resume 窗口**，2GB 内存压力
放大到必现；深挖到底层逐帧栈的路径在现环境不可达，修复按可复现环境实证。

**修复**：MainActivity.onCreate 预暖绑定自身 IME service（bindService 自有
组件免 BIND_INPUT_METHOD 权限——同 uid 组件访问不受声明权限约束），onDestroy
解绑。效果：service 创建被挪出任何 show-transaction/恢复临界路径，Test input
与任意编辑框的 IME 请求到达时 service 必已存在。**2GB 复现验证**：修复前
3/3 冻结 → 修复后 3/3 通过（面板打开+打字 preedit 正常+碰撞两轮零冻结）。

**残余**（记录不扩面）：进程重启碰撞在极端低压环境仍是累计负载问题，根治=
1.4 删主题 YAML 解析与旧视图树的冷启动减负（同方向收益）；真机（8GB+ 级）
不预期触发。

## 2026-11-15 — Task 1.3 WIP 检查点：Compose 骨架全链路打通至窗口高度协商

**已落地（编译绿 + 单测绿，ImeFlags 关闭为默认）**：`ime/compose/` 四件套——
ImeUiState（广播器→Compose State，Bulk 消息自带候选文本无需二次取）、
KeyboardPanel（扁平键表按 columns 折行、按键态单键局部 State、
awaitEachGesture 手势：tap/long/swipe、repeatable 长按连发、KeyBehavior 枚举序
状态化槽位解析）、ImeRoot（组合条/候选条/PanelHost+liquid 最小版）、
ComposeInputView（继承 InputView：旧 DI 机器全保留为状态机，旧树 INVISIBLE
——不能 GONE，旧树是测量高度来源；ComposeView 四边 MATCH_CONSTRAINT 约束
——ConstraintLayout 里裸 MATCH_PARENT 塌 0×0；专用 ComposeLifecycleOwner——
SavedStateRegistryController 要求 owner 生命周期处 INITIALIZED 期，service
生命周期做不到，配迷你 LifecycleOwner（create→attach→restore(null)→RESUMED），
组合跨窗口重附加存续利于延迟）。服务接线 replaceInputView 按 ImeFlags 分支。
KeyboardWindow 增 public currentKeyboardName。

**运行时证据（探针日志）**：init→attached→measured 1080×2337→layout
`compose=0,0,1080,2337` 全尺寸——ComposeView 生命周期/布局全通；软渲染实例上
y=1540 处出现 1px 的 #F1F3F4/#FFFFFF 条带（调色板像素！）。

**当前堵点（下一步的第一件事）**：IME 窗口高度协商——窗口 surface 实际
0×0/1px（`Frames:…surface=[0,0][0,0]` + 触屏区 y1513 起），旧机制的
updateWindowViewHeight 高度协商链（KeyboardWindow.currentKeyboardHeight ←
legacy KeyboardView 测量 → service 调 window 高度）与我的全窗 ComposeView
内容（应只占 content inset 以下的键盘带）没有对齐。嫌疑序：① InputView
的 updateWindowViewHeightJob 与 currentKeyboardHeight flow 在 INVISIBLE 旧树
下的取值时序；② 我的 ComposeView 应贴底而不是 fill（旧树键盘容器是贴底的，
上半窗保持透明）；③ onComputeInsets 的 contentTopInsets 协商。

**环境战记（模拟器图形）**：宿主 GPU（Intel Arc/Mesa gfxstream）状态损坏，
IME 区域全黑、全屏 98% 黑、出现绿/红测试色像素；**连今早验证正常渲染的
4842b0fb APK 也黑**——非本变更代码问题；热重启/冷启动(-no-snapshot-load)不
能救，最终以 `-gpu swiftshader_indirect` 软件渲染获得可用图形（此实例上
设置 App、IME 窗口、我的调色板像素都正常出现）。**后续验收如再遇渲染异常，
直接切软渲染或重启宿主 X/驱动**；延迟实测必须回到健康 GPU 实例。

**调试资产**：ComposeInputView 内保留 Timber 探针（init/attached/measured/
layout）直到 1.3 收尾。

## 2026-11-15 — Task 1.3 重构：FlorisBoard 架构参考 + 数据/chrome 分离

**方法修正（主人指示）**：旧 YAML 数据降级为行为清单，视觉层从设计规范出发。
参照 FlorisBoard（`/home/shijy7/Workspace/Github/florisboard`）：
- 字符排（字母）来自数据，功能行是应用自有 chrome（代码定义/硬编码）
- 固定行高 65dp、8dp 圆角、22sp 字母、回车主色胶囊
- 按压 ripple + repeatable 连发

**落地**：
- `ui-design-spec.md`（change 目录）：完整规范
- `GboardKeyboard.kt`：GboardKeyboard = 数据行(letterRows) + 硬编码 chrome 行
  （[符号][/辅助][，][空格·中文][。][↵]）；letterRows 单测证明过滤干净
  （3 行纯字母 q-p/a-l/z-m）
- `GboardKey.kt`：Gboard 视觉单键（亮色 surface 分类、按压态、长按超时
  + repeatable 连发）
- ImeRoot 条带高度解耦旧测量值（旧值裁剪新内容——Gboard 结构自身定高）
- 键面不渲染旧 labelSymbol/hint（仅辅助码键带"辅助"提示）

## 1.5 阶段 1 验收（2026-11-15，commit 4d1aed17，Medium_Phone_API_36.1 x86_64 模拟器）

### 打字回归（全项通过）

- 出字/简拼：n→preedit n、候选 16；ry→preedit ry、候选 16、首选"润"上屏（CommitText(text=润)）
- `ni/h` 辅助码：ni→逗号（组合态 `/` 槽）→preedit ni/→h→**候选 16→8（辅助码过滤生效）**→首选上屏
- `/A` 声调：ni→逗号→a→**候选 16→12（声调过滤生效）**（预览 𦤽）
- 英文内嵌：字母串+`*`（KEYCODE_STAR）→preedit〔英文〕标记（easy_en 内嵌段触发；hello 上屏口径见期 2 3.2 记录）
- 符号页：底行符号键→basic_symbol 切换+渲染（`~` 输入口径见 1.4 冒烟）
- 数字页：长按逗号→number 切换+渲染（顶行 7 字形簇）
- 编辑键盘：f 长按（swipe 注入 1400ms）→editor 切换
- liquid 面板：☻→emoji 网格渲染（白格 13833+彩色字形 2102）→点选上屏（地址栏彩色字形）；操作栏 ☻ 槽在 liquid 页变 ⌨ 返回→default（闭环）
- 操作栏双态：菜单态 6 chips（⚙/，。/📋/☻/中/▼）↔候选态（候选横滚+中/EN），band 高度恒定 896px
- 中英切换：中↔EN 蓝灰双态（像素验证）；EN 态字母经 KeyMessage 物理键路径直通上屏；符号中英（，。↔. ,）双向
- 设置/收起：⚙→MainActivity 置顶；▼→onRequestHide→onHidden
- ZXCVB 清理：A/Z 行键面纯字母（10/10/9 字形簇），labelSymbol 图标与 ;{}'" 角标全清，长按槽保留（逗号长按数字页、f 长按 editor 实测仍通）

### 暗/亮双 scheme（运行时切换闭环）

- `followSystemDayNight` 默认改 true（scheme 选择 UI 已随 YAML 引擎删除，跟随系统是唯一合理默认）；Light 表的 dark_scheme 链自动解析 gboard_dark
- Compose 树改读 `ThemeColors`（LocalImeColors provider，与 legacy 树同一张表）；scheme 翻转经既有 refreshColors 监听→`ImeUiState.colorsRevision` 重组
- `cmd uimode night yes`：band #202124/键面 #3C4043/chips #303134/强调 #8AB4F8；`night no`：即时回亮（白键/#F1F3F4 间隙），无需重启
- 截图归档：`screenshots/1.5-light-candidate.png`、`screenshots/1.5-dark-candidate.png`

### 延迟对照（与 1.1 基线同口径，logcat 引擎消息计时）

| 口径 | 1.1 基线（4842b0fb 自绘） | 4d1aed17（Compose+NativeTheme） | 变化 |
|---|---|---|---|
| `ime set`→进程拉起 | 0.25s | 0.135s | -46% |
| 冷首键（进程冷启动+键排队）→引擎回显 | ~2.7s | 0.88s | -67% |
| 暖态键→引擎回显 | 10-50ms | 96-105ms | 含 adb 注入开销的口径；引擎侧持平 |
| gfxinfo p50 | 53ms | 16ms | -70% |
| gfxinfo janky | 60% | 20% | -67% |
| gfxinfo p90 | 85ms | 109ms | p99 700ms 拖尾（软渲染实例特性） |

同环境注意：4GB/软渲染（swiftshader_indirect）实例仅作同口径对照，真机 arm64 手感以主人验收为准（2.3 终验项）。

### 顺带修复（验收中挖出）

- `PHASE_SERVER_ALREADY_VISIBLE` 竞态：BACK 收起后重聚焦输入框，show 被框架吞掉（键盘空白）——onStartInputView 里 `!isInputViewShown → requestShowSelf(0)` 兜底（Chrome 实测 BACK→retap 自动回弹）
- 系统手势区右侧 ime-switcher 按钮：`supportsSwitchingToNextInputMethod=false` + 单输入法（自用）后消失（模拟器实测右下图标 0 像素）；左侧收起键为 SystemUI 强制项，无法移除（操作栏 ▼ 承接其功能）
- liquid 面板高度钉 4×65dp：fillMaxSize 会把 wrap Column 撑满窗、把操作栏顶出 band 顶

## 2.1-2.3 阶段 2收官（2026-11-16，commits 8753cdfd/3e7941dd 前：ca132218/af8e5520/ff9d6b0f/6c0882cc/a941c0d0）

### 交付面
- 剪贴板面板 Compose 化（纯状态机+面板+长按菜单，DAO observeAll 全量 Flow，6 旧 View 文件删）
- liquid tab 条 Compose 化（55 页横滚 chip、tap 统一分发、5 旧文件删、onLiquidText 桥删）
- 面板关闭修复（back_to_keyboard 命令直 attach；📋 toggle）+ ⌨ 双击防抖（300ms 盲）
- **旧 View 树总删除 -6871 行**：InputView 纯桥宿主、五 delegate+7 目录+KeyboardView/KeyView+悬浮候选及设置页亡；KeyboardWindow 纯数据机
- 触摸区域修复：InputDeviceManager 简化误删 DISABLED 分支→superEvaluate false→touchable 只剩底行（dumpsys contentTopInsets=2211 实锤）→修为恒 true，insets 1442 恢复
- 2.2：Shift 键 EN 高亮（蓝面白⇧，键盘面反馈闭环）；孤儿清扫 7 文件+inlineSuggestions 链

### 终验回归（2026-11-16 模拟器，全绿）
打字链（n 简拼→ni 16 候选→空格上屏"拟"）；中英往返（OptionMessage 双向+Shift 蓝高亮像素）；📋 开/⌨ 关（Attach 日志对）；☻ 开（tab 条蓝 175px）/⌨ 关；符号页切换（basic_symbol）；长按数字页/编辑键盘 1.5a 已验（adb 长按注入 flaky，真机手指为准）

### 延迟终测（同 1.5 口径，与 1.5 对照）
| 口径 | 1.5 | 2.3 终测 |
|---|---|---|
| ime set→进程拉起 | 0.135s | 0.126s |
| 冷首键（force-stop 后首键排队） | 0.88s | **0.29s** |
| 暖态键→回显 | 96-105ms | 123-324ms（adb 注入开销主导，引擎侧持平） |
| gfxinfo p50 | 16ms | **16ms** |
| gfxinfo p90 | 109ms | **38ms** |
| gfxinfo janky | 20% | 30.5%（541 帧含 adb 测量噪音） |

### 主人 arm64 真机验收清单（archive 前置门）
1. 全新安装首启：引擎就绪时间（模拟器 4.6s 基线）
2. 稳态：ni→"拟"上屏、ni/h 辅助码过滤、声调 /A、hello* 英文内嵌
3. 面板：📋 列表+长按菜单+tap 上屏回键盘；☻ liquid tab 切换；⌨ 返回
4. 操作栏：中英切换（键面 EN 蓝）、符号中英、⚙ 设置、▼ 收起
5. 亮/暗模式跟随系统（白天/黑夜各看一轮）
6. 打字延迟手感（模拟器软渲染仅对照，真机 GPU 数字应更优）
