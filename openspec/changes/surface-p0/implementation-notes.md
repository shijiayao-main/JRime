# Implementation Notes — surface-p0

## 2026-11-21 · 候选分页 / 长按菜单 / 提示开关 / 简繁全角

- 引擎：`ImeSessionState.candidatePageSize`、`ImeCommand.ChangeCandidatePage`/
  `SetCandidatePageSize`/`CandidateAction`；reducer 负责分页计算、翻页 clamp、候选事件重置；
  新增 4 个分页单测。
- UI：`CandidateStripView` 重写为分页条（上一页/下一页 + `x/y` 页码 + 全局 index 点选），
  长按候选弹出菜单（置顶/删词/复制/查编码）；`KeySurface` 提示受 `showKeyHints` 控制。
- App：`AppPrefs` 新增 `candidate_page_size`/`candidate_text_size`/`show_key_hints`；
  controller 把命令先过 reducer 再交 executor（分页/面板这类 UI 局部状态由此生效）；
  executor 处理 `CandidateAction`（置顶走 preset、删词走 deleteCandidate、复制写系统剪贴板）；
  顶栏新增「简/繁」「全/半」双态按钮。
- 模拟器：分页渲染验证（`screenshots/emulator-2026-11-21-paged-candidates.png`，7 条/页、
  `1/3`、右箭头）。箭头点击/长按菜单/开关的真机走查留到最终验收。
- 触感：复用 `vibrate_on_key_press`（默认关，设置页由 `settings-v2` 暴露）；M1 已把
  tap/long/slide 三档接入 host。
