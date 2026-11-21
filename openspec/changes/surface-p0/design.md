# Design: surface-p0

依据：`doc/master-plan.md` M2；`surface-foundation` 的 `ImeSessionState`/`ImeCommand`/`ImeSurfaceHost` 契约。

## 1. 候选分页（UI 侧分页）

引擎仍发 `Candidates.Bulk` 全量候选（native 侧不翻页）。分页是纯 UI 状态：

```kotlin
// ImeSessionState 增加
val candidatePageSize: Int = 7          // 5/7/9
val candidatePageIndex: Int = 0
val candidatePageCount: Int = 1

// ImeCommand 增加
data class ChangeCandidatePage(val forward: Boolean) : ImeCommand
data class SetCandidatePageSize(val size: Int) : ImeCommand
```

- reducer：`Candidates` 事件按 `ceil(size/pageSize)` 重算页数并回到第 0 页；翻页 clamp；改单页数量保持当前首个候选可见（重算页并 clamp）。
- UI：`CandidateStripView` 只渲染 `candidates.drop(page*size).take(size)`；左侧箭头（上一页）/右侧箭头（下一页）+「x/y」页码；点选派发 `SelectCandidate(candidate.index)`（**全局 index**）。
- 空列表/单页时隐藏箭头与页码。

## 2. 候选长按菜单

- 长按候选打开 `Popup`（focusable=false 不行，需要点击菜单项 → focusable=true + dismiss on outside），菜单项：
  - 置顶：`ImeCommand.CandidateAction(index, Pin)` → executor 派发 preset `PinCandFunc`（Control+t）；
  - 删词：`Delete` → 现有 `deleteCandidate(index, global=true)`，UI 侧 3s 撤销条（撤销=重新提交候选文本？Rime 无 un-delete；撤销以「重新上屏文本」替代不成立——**降级**：删词后 3s 内显示提示，不提供撤销；撤销能力列入后续）；
  - 复制：`Copy` → executor 取候选文本写系统剪贴板；
  - 查编码：留占位（M3 编码提示批次）。
- 单击仍直接上屏；长按不再直接删词（菜单显式选择），与旧手感差异在真机验收时确认。

## 3. 键面提示

- 数据已声明 `labelSymbol`/`hint`；`KeyBinding` 增加 `showHints: Boolean` 由 config 提供（`ImeSurfaceConfig.showKeyHints`，默认 true）。
- 关闭时 `KeySurface` 不渲染角标，也不渲染长按气泡里的 hint；不影响长按行为。
- 提示内容优先级：`key.labelSymbol > key.hint > 无`。

## 4. 简繁/全角开关

- 顶栏新增两个 `ToolbarAction`：`简/繁`、`全/半`（label 由 state 决定），command=`ImeCommand.SetOption("simplification"|"full_shape", !current)`。
- executor 已有 `SetOption` → `setRuntimeOption`；状态经 option/status 事件回写 state。
- 顶栏空间不足时的顺序：⚙ / 标点 / 123 / 📋 / ☻ / 简繁 / 全半 / spacer / 中EN / ▼；窄屏允许横向滚动（TopBar 用 LazyRow 或压缩 chip）。

## 5. 触感

- 现有 host 已在 Tap/LongPress/SlideCursor 调用 `keyPressVibrate`；本变更补偏好开关（`keyboard_vibrate_enabled`，默认 true）并统一三档：tap=KEYBOARD_TAP、long/slide=LONG_PRESS。
- 音效路径不变。

## 6. 偏好（先落值，UI 在 settings-v2）

| key | 默认 | 说明 |
|---|---|---|
| `candidate_page_size` | 7 | 5/7/9 |
| `candidate_text_size` | 18 | 15–22 sp |
| `show_key_hints` | true | 键面角标 |
| `keyboard_vibrate_enabled` | true | 触感总开关 |

## 7. 测试

- 引擎 reducer：分页计算/翻页 clamp/改页大小保持位置/候选事件重置页。
- 纯函数：分页切片、页码文本、长按菜单项可见性。
- 结构守护：`CandidateStripView` 无平台 import。
- 模拟器冒烟：分页翻页、长按菜单、开关状态变化、按钮点亮/熄灭。

## 8. 风险

- 分页与全局 index 混用 → 全部路径统一用 `CandidateView.index`，测试覆盖。
- 长按菜单与「长按删词」旧习惯冲突 → 菜单默认焦点在「删词」项，真机验收确认。
- 顶栏拥挤 → 先压缩 chip，必要时把简繁/全角收进第二行（本变更不做第二行）。
