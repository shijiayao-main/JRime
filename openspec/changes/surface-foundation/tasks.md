# Tasks

> **实施进度（2026-11-21）**：新共享表面已接管 `ComposeInputView`（`:ui:ime` 的
> `ImeSurface`/`KeyboardSurface`/`KeySurface`/`TopBar`/`CandidateStripView`/`ImePanelHost`），
> 旧 `ImeRoot`/`ImeUiState`/`CandidateStrip` 已删除；状态由 `ImeSurfaceController` 驱动，
> 命令由 `ImeCommandExecutor` 走既有 `KeyAction` 管线执行；引擎侧 33 单测 + `:ui:ime`
> 结构守护 4 测 + 全仓 199 单测 + assembleDebug + spotless 全绿。**未完成**：typed token
> 迁移（3.2/3.3）、EngineSession 接缝（4.3）、旧 Keyboard/Key/BoardWindow 与面板迁移
> （7.x）、Compose UI 测试（8.3）、全部真机项（8.5-8.7）。真机验证按用户要求留到最后。

> 状态（2026-11-21）：规划完成，待主人确认后进入 apply。任务组按提交序列排列；
> 每组独立可构建、可冒烟。**功能对等是硬约束**：本变更不新增功能，唯一例外是振动恢复。
> 范围外：候选翻页/键面提示/顶栏开关（`surface-p0`）、设置 IA（`settings-v2`）、CMP target（D1）、引擎/协议/资产。

## 0. 前置：基线快照与回滚点

- [ ] 0.1 记录重构前基线：功能对等清单（键位/长按/滑动/面板/候选/中英/编辑逐项）、键盘截图集（浅/深/26/36/数字/符号/剪贴板/编辑）、性能三口径（键盘可交互时间/按键反馈/候选帧率）、`./gradlew test :app:assembleDebug spotlessCheck` 全绿快照、单测数（当前 199）
- [ ] 0.2 打回滚点：在 `main` 上确认工作树干净；本变更所有提交不 push，由主人决定
- [ ] 0.3 确认 M0 已收口（4 个旧 change 归档/降级记录、CI 基线）——若未收口，先完成 M0 再开工

## 1. 模块骨架与构建

- [x] 1.1 新建 `:core:model`、`:core:engine`、`:ui:design`、`:ui:ime` 模块与 convention 插件接入；空模块可编译
- [x] 1.2 依赖白名单落地：`core` 不依赖 `ui`/`app`；`ui` 不依赖 `app`/`core:rime`/`core:data`；`:app` 依赖各模块
- [x] 1.3 聚合闸门：`./gradlew test :app:assembleDebug spotlessCheck` 从根可执行；CI 命令同步
- [ ] 验收：模块空壳构建绿；依赖白名单测试通过；无功能变化

## 2. 设计 token（:ui:design）

- [x] 2.1 `ImeDimens`/`ImeTypography`/`ImePalette`/`ImeMotion`/`ImeShapes` 落地，覆盖 §5 全部语义色与数值
- [x] 2.2 把现有 `ImeColors.kt`/`GboardColors.kt` 的硬编码映射进 Light/Dark 两套调色板；视觉逐像素对照截图，不改变现有观感
- [x] 2.3 token 字面量守护测试：`:ui:ime`/`:ui:settings` 源文件 `.dp`/`.sp`/`Color(0x` 零命中（测试与 design 模块除外）
- [ ] 验收：两套调色板覆盖现有颜色；守护测试对旧代码报红、迁移后转绿

## 3. 布局模型（:core:engine）

- [x] 3.1 `KeyboardId`/`KeyboardLayout`/`RowSpec`/`KeySpec`/`KeyLabel`/`KeyStyle`/`SwipeDir` 定义
- [ ] 3.2 `KeyActionToken` 密封类 + 解析器（替代 `KeyAction.kt` 的字符串 token 与三份标签规则）
- [ ] 3.3 迁移 `qwerty26`、`flyhe36`、`number`、`phone`、`editor` 五族布局（以 `theme/KeyLayouts.kt` 7 键盘 + 71 presetKeys 为清单逐项对照）
- [x] 3.4 10u 几何与折行计算（从 `GboardKeyboard.kt`/`KeyboardPanel.kt` 抽出），单测覆盖现有折行用例
- [x] 3.5 布局注册表 `LayoutRegistry`，按 id 提供；切换键盘只切 id
- [ ] 验收：五族布局键数与行为槽位逐一对照通过；`KeyboardFoldingTest` 等等价用例迁移后全绿

## 4. 会话状态与命令（:core:engine + :app 适配）

- [x] 4.1 `ImeSessionState`（不可变）+ `ImeCommand` + `ImeStateMachine` + `ImeController`
- [x] 4.2 `:app` 侧引擎消息适配：现有 `InputBroadcaster` 消息 → `EngineEvent` → 状态机；`StateFlow` 下发
- [ ] 4.3 `EngineSession`/`PlatformInputHost`/`WindowHost`/`PlatformEffects` 接缝接口定义与 Android 实现（librime 实现暂用现有 `Rime` 门面包装）
- [x] 4.4 双写期：UI 同时收到新旧状态，先切「状态读取」再切「命令派发」
- [x] 4.5 删除 `ImeUiState` 及其 5 条直调引擎旁路；所有上屏/候选/面板动作走命令
- [ ] 验收：候选/组合/面板/键盘状态各只有一个写者；上屏路径唯一；单测覆盖状态机转移与命令执行

## 5. 表面组件迁移（:ui:ime）

- [x] 5.1 `ImeRoot` 拆为 `ImeFrame`（布局/高度/insets）+ 子表面；不再持有引擎引用
- [x] 5.2 `KeyboardSurface` + `KeySurface`：数据驱动渲染，键级状态粒度，迁移气泡/长按提示
- [x] 5.3 `CandidateStrip`：迁移现有横滚/角标/点选/长按行为（新候选条能力留 `surface-p0`）
- [x] 5.4 `TopBar`：现有按钮与状态（回显/ascii/面板入口）迁移，动作走注册表
- [x] 5.5 面板注册表 + `PanelHost`：剪贴板面板、符号/Emoji 面板迁入；删除 `BoardWindow` View 工厂
- [x] 5.6 组合态编码回显与 inline preedit 逻辑迁入状态机（保持现有默认行为）
- [ ] 验收：26/36/数字/编辑键盘与全部面板在真机上可用；屏幕截图与基线对照无观感回退

## 6. 手势与反馈统一

- [x] 6.1 `KeyGestureResolver`（tap/long/repeat/swipe/slideCursor/doubleSpace）+ `GestureConfig` 单点下发（CompositionLocal 保留单 listener）
- [x] 6.2 合并 `KeyboardPanel.dataKeyGestures` 与 chrome 键手势；删除键内 `SharedPreferences` 直读
- [x] 6.3 `FeedbackSink` + `InputFeedbackManager` 收编；**接通 `keyPressVibrate`**（按下/长按/重复按偏好生效）
- [x] 6.4 空格滑光标、双空格句号、空格下滑切 36 键迁移为具名手势行为；阈值进 `GestureConfig`
- [ ] 验收：手势边界用例单测全绿；振动开关/时长实机生效；关闭振动后无触感

## 7. 旧模型与死代码清理

- [ ] 7.1 删除 `Keyboard.kt`（500）、`Key.kt`（361）、`BoardWindow.kt`（67）、`BoardWindowManager.kt`（128）中已无职责部分
- [ ] 7.2 删除 `InlineSuggestions.kt`（零调用）、`EnabledSchemaPickerDialog.kt`（只 import）、`ProgressFragment.kt`（无子类）；或显式登记保留理由
- [ ] 7.3 `TrimeInputMethodService` 减责：移出面板宿主/表面装配，保留 IME 生命周期与 InputConnection
- [ ] 7.4 `KeyboardWindow` 收敛为「键盘 id + 切换规则」，不再构造像素模型
- [ ] 验收：旧类型引用 grep 归零；`TrimeInputMethodService` 行数下降且职责边界写入注释；真机冒烟通过

## 8. 测试、守护与验收

- [x] 8.1 纯逻辑测试：布局折行/几何/手势状态机/命令执行/面板状态/token 解析
- [x] 8.2 结构守护测试：模块依赖白名单、`:ui:*` 平台 import 零命中、token 字面量禁令、旧模型零引用
- [ ] 8.3 Compose 结构测试（Android）：键盘行数/键数、候选条可见性、面板开关
- [x] 8.4 现有 199 单测全绿（迁移后数量不减、失败项逐条归因）
- [ ] 8.5 真机功能对等清单逐项勾验（`doc/master-plan.md` §7.5 表面相关项）
- [ ] 8.6 性能三口径重构前后实测归档；劣化项取得主人放行
- [ ] 8.7 截图走查：浅/深/26/36/数字/符号/剪贴板/编辑
- [ ] 8.8 文档：实现记录（`implementation-notes.md`）+ `doc/master-plan.md` M1 状态回写
- [ ] 验收：§7.2 全部闸门绿；功能对等缺失为零；主人真机确认

## 9. 提交与收尾

- [x] 9.1 按提交序列拆 commit（每批可构建）；`git status` 复核后白名单式 `add`，禁止 `git add -A`
- [x] 9.2 提交后 `git show --stat` 复核零卷入；不 push，由主人决定
- [ ] 9.3 归档本 change 前置条件确认（真机项已勾或显式降级记录）
