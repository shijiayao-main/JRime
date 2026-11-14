# Tasks

> 状态（2026-11-15）：阶段 1 已完成 1.1-1.4（1.4 换源提交 514a5449，YAML 主题
> 引擎全灭，NativeTheme 唯一生产者）；当前待办 1.5 阶段验收，随后进入阶段 2。

## 1. 阶段 1：核心打字环路换血（NativeTheme + Compose）

- [x] 1.1 地基：引入 Compose BOM 依赖；**替换前延迟基线实测**（自绘管线现行
  窗口：显示到可交互、按键到视觉反馈两口径，录屏逐帧或 systrace，归档
  implementation-notes）；新建 `com.jiaoay.jrime.theme` 包——类型安全
  keyboard/key/row DSL（13 行为槽位具名参数）、Gboard 亮/暗配色常量与
  GeneralStyle 视觉参数（Material 表面色起手）。验证：builder 可产出完整
  `Theme` 模型实例；基线数字归档。
- [x] 1.2 布局移植：以 4.1 闭包清单（`startup-fastpath/theme-trim-list.md`
  §2/§4）为基线，7 个保留键盘（default/flyhe36/number/editor/basic_symbol/
  math_symbol/t9_symbol）与 71 个 preset_keys 全部行为槽位转录为 DSL；默认
  键盘底行第 2 槽以辅助码键（click=`/`、long=IME_switch）替换方案切换键。
  验证：DSL 产物与 YAML 逐键对照抽样单测（键数/槽位/label/hint）；旧
  ThemeDiagnostics 跑新模型零告警（删除诊断器前的等价性证据）。
- [x] 1.3 Compose IME 窗口：`ImeRoot`（单 ComposeView 承载）+ CompositionBar +
  CandidateBar + KeyboardPanel（行×键网格，普通 Row/Column 不用 Lazy 容器，
  按键态单键粒度局部 State）+ KeyPopup（长按预览/符号选择，Compose Popup）；
  `Modifier.pointerInput` 实现 press/long-press/swipe 槽位并复用现行
  `KeyAction` 派发；liquid 符号面板（tab 条 + 网格）Compose 化；剪贴板/开关
  面板以 AndroidView 互操作过渡保入口可用。验证：模拟器手工全槽位操作路径
  走查（每个手势槽至少一次触发）+ 按键事件单测。
- [x] 1.4 换源与删除：`ThemeManager` 数据源改指 NativeTheme 单例（删文件监听/
  重载/主题选择持久化）；删除 YAML 解析机器（ThemeLoader/ThemeYaml/
  ThemeDslExpander/ThemeFilesManager/ThemeDiagnostics/ColorTable 等）、
  `trime.yaml`+`flyhe.trime.yaml` 资产、主题/配色设置页与按键字符串、被
  Compose 取代的核心环路旧 View 类（keyboard/candidates/popup/composition/
  bar/window 中已无引用者）、golden 主题测试 YAML 装载路径（改 NativeTheme
  模型断言）；checksums 强制全量重生成（`rm app/src/main/assets/checksums.json`
  老陷阱处置）；`startup-fastpath/tasks.md` 4.2 标注被本变更取代。验证：
  `:app:assembleDebug` + `:app:testDebugUnitTest` + spotless + `make check`
  全绿；APK 内无主题 YAML、无主题入口。
- [ ] 1.5 阶段 1 验收：模拟器全新安装——打字回归逐项（出字/简拼/`ni/h`
  辅助码/`/A` 声调/英文内嵌 `hello*`/符号页/数字页/编辑键盘/liquid 面板），
  亮/暗各一轮截图归档；**替换后延迟对照实测**（与 1.1 基线同口径）归档并
  交主人验收手感。验证：logcat+落框+延迟数字+截图全部归档，主人放行或
  提迭代意见。

## 2. 阶段 2：面板收尾与视觉精修

- [ ] 2.1 面板 Compose 化收尾：剪贴板/开关面板移植为 Composable，删 AndroidView
  互操作与全部旧 View 层残余（`ime/symbol / clipboard / switches` 旧适配器等）；
  死代码/字符串/测试清理。验证：全仓 grep 无旧渲染层与 YAML 主题残留引用；
  门全绿。
- [ ] 2.2 视觉精修迭代：按主人对截图的意见校准色值/圆角/间距/字号（必要时
  多轮）。验证：每轮截图归档，主人放行为准。
- [ ] 2.3 终验与归档准备：5.2 口径全项回归 + 延迟终测 + `implementation-notes.md`
  完整证据链；真机项（arm64 首启/稳态/打字回归/延迟手感）列为主人验收清单。
  验证：门全绿、证据归档、待主人真机验收后 archive。

## Workflow follow-up

- 本 change 吸收并取代 `startup-fastpath` 任务 4.2（YAML 裁剪），4.1 的闭包分析
  作为移植基线继续生效；`startup-fastpath` 的 archive 仍按其自身真机验收门走。
- 延迟基线实测（1.1）必须在换源动工前完成，替换后对照口径才有效。
