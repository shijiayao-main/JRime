# Proposal: surface-p0

## Why

M1（`surface-foundation`）已把表面层换成共享架构并在模拟器跑通，但「手机先好用」的 P0 能力仍有缺口（`doc/master-plan.md` §2.3）。本变更补齐其中不依赖设置页重做的四项：

1. **候选条分页**：现全量横滚 ≤16 条、无页码/单页数量/字号控制，长候选列表只能盲滑。
2. **候选长按菜单**：现长按=静默删词、无确认无撤销，也拿不到置顶/查编码/复制。
3. **键面提示**：韵母/形码/长按提示只有零散角标，没有统一的提示开关与展示规则（`/ok` 仍要背）。
4. **高频开关与触感**：简繁/全角只能靠 `/so` 或硬件快捷键；振动虽在 M1 接通但尚未走查强度与长按语义。

目标是在不重做设置 IA（`settings-v2`）的前提下，把这些能力做成可验证的 P0 体验；新增偏好先落默认值与内部读写，设置 UI 随 `settings-v2` 暴露。

## What Changes

- **候选分页**：`ImeSessionState` 增加单页数量与当前页；`ImeCommand` 增加翻页/设单页数量；候选条按页渲染、显示页码与左右箭头；点选仍用全局候选 index。
- **候选长按菜单**：长按候选弹出菜单（置顶 / 删词 / 复制 / 查编码占位），删词带 3s 撤销；菜单项通过 `ImeCommand.CandidateAction` 执行。
- **键面提示**：字母键角标显示长按/滑动产出（沿用数据）；新增「提示开关」偏好（默认开）；空格/退格/回车等 chrome 键不再挤占提示位。
- **简繁/全角开关**：顶栏新增「简/繁」「全/半」双态按钮，直接 toggle 引擎 option（`simplification`/`full_shape`），状态由 option/status 事件驱动。
- **触感走查**：按压/长按/滑光标三档触感语义统一（复用 M1 FeedbackSink），新增偏好开关默认开。

**不做**：设置页 IA 重做（`settings-v2`）；主题/外观（M2 外观批次）；词库（M3）；桌面。

## Capabilities

### New Capabilities

- `candidate-experience`：候选条分页与长按菜单、键面提示、输入模式开关与触感反馈的 P0 行为契约。

### Modified Capabilities

（无。`input-schemas`/`profile-storage` 不受影响。）

## Impact

- 引擎：`ImeSessionState`/`ImeCommand`/`ImeStateReducer` 扩展与单测；无 JNI/协议变更。
- UI：`:ui:ime` 的 `CandidateStripView`、`KeySurface`、`TopBar` 增量；`KeySurface` 手势/触感复用现有 host 接口。
- App：`ImeSurfaceController` 映射新事件；`ImeCommandExecutor` 处理新命令；`AppPrefs` 新增 3 个偏好（候选单页数量、候选字号、键面提示开关）。
- 风险：分页与全量候选的 index 语义要对齐（点选必须用全局 index）；长按菜单要保持「长按删词」旧手感但加撤销，需真机验收。
