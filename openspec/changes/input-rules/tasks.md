# Tasks

> 状态（2026-11-21）：第一批（输入规则 + 候选注解开关）完成；中英切换方式、模糊音/简拼配置、词库逐词编辑、常用语面板列入后续批次。

## 1. 运行时选项接缝

- [x] 1.1 `:ui:settings` 新增 `ToggleOption`/`OptionChoice` 控件与 `RuntimeOptionSource` 异步接缝
- [x] 1.2 `SettingsScreen` 支持异步 option 行（加载失败/无会话显示 —）
- [x] 1.3 app `RimeRuntimeOptionSource`（RimeDaemon 当前会话）

## 2. 输入规则行

- [x] 2.1 简繁字型四选一（zh_simp/zh_trad/zh_hk/zh_tw）
- [x] 2.2 全角字符、英文标点、表情候选、联想输入、单字优先、全拼显示、码区提示、增广字集
- [x] 2.3 候选注解类型三选一（mask_hint/tone_hint/comment_off）
- [x] 2.4 插入设置中心「输入」组顶部；模拟器渲染验证

## 3. 候选注解开关

- [x] 3.1 `AppPrefs.Appearance.showCandidateComment`（默认开）+ 设置中心自动出现
- [x] 3.2 `ComposeInputView` 接入 `ImeSurfaceConfig.showCandidateComment`（实时 listener）

## 4. 后续批次

- [ ] 4.1 中英切换方式偏好（按钮/Shift/空格）与实现
- [ ] 4.2 模糊音/简拼配置入口（引擎侧 8 / Ctrl+q 触发）
- [ ] 4.3 用户词库浏览/逐词编辑（导出-过滤-导入方案评估）与备份恢复增强
- [x] 4.4 常用语面板（写 `custom_phrase.txt` + 用户词典重载）→ 独立 change `phrase-panel`
