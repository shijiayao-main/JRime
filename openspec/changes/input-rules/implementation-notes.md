# Implementation Notes — input-rules（第一批）

## 2026-11-21

- `:ui:settings`：新增 `SettingControl.ToggleOption` / `OptionChoice` 与 `RuntimeOptionSource`
  异步接缝；`SettingsScreen` 的 option 行用 `LaunchedEffect` 加载、`rememberCoroutineScope` 写入，
  无会话/加载失败显示「—」；互斥组全 false 时默认显示第一项（简繁默认「简体」）。
- app：`RimeRuntimeOptionSource`（`RimeDaemon.getFirstSessionOrNull()` 的当前会话）、
  `InputRuleSpecs`（10 行：简繁四选一/全角/英文标点/表情候选/联想/单字优先/全拼显示/
  码区提示/增广字集/注解三选一），插在设置中心「输入」组顶部。
- 候选注解开关：`AppPrefs.Appearance.showCandidateComment`（默认开）→ `ComposeInputView`
  config + listener → `CandidateStripView` 既有读取点；由自动派生的设置行 + 无孤儿测试覆盖。
- 模拟器：输入组渲染 10 条规则，会话值正确读取（英文标点/表情候选默认开，简繁显示「简体」）；
  截图 `screenshots/emulator-2026-11-21-input-rules.png`。全仓闸门绿。
- 后续批次：中英切换方式偏好、模糊音/简拼入口、词库逐词编辑、常用语面板。
