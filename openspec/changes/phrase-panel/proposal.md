# Proposal: phrase-panel

## Why

飞鹤快拼自带 `custom_phrase` 用户词典（`custom_phrase.txt`，115 条置顶单字随 APK 分发），但过去只能靠编辑文件使用；而重装后用户目录没有该文件，实际只有 shared 只读回退。设置页也没有入口。M3 的词库/常用语方向需要一个可用的常用语管理面。

本变更把 `custom_phrase` 做成输入法内的「常用语」面板：点选即上屏、可直接新增/删除，写回 Rime 用户词典并即时生效。

## What Changes

- 新增 **常用语面板**（顶栏「语」进入）：列表展示当前 `custom_phrase` 条目，点选上屏；顶部输入行（文本 + 编码 + ＋）新增；每行 × 删除。
- 新增 `CustomPhraseCodec`（保留文件头注释、Tab 分隔、权重缺省、坏行丢弃）与 `CustomPhraseStore`（用户目录读写 + 公开流 + 保存后轻量重启引擎使 stabledb 生效）。
- **首次使用从 shared 目录播种**：用户目录缺 `custom_phrase.txt` 时把随包文件复制过去，面板能显示 115 条置顶词，编辑也从完整列表开始。
- 引擎状态新增 `PanelId.Phrase` 面板接线：`PhraseWindow`、controller 映射、executor 打开面板、`ComposeInputView` 面板槽 + 顶栏动作。

## Capabilities

### New Capabilities

- `phrase-panel`：常用语/自定义短语的面板与持久化契约。

### Modified Capabilities

（无。）

## Impact

- app：新 `ime/phrase/` 包（codec/store/window/panel）、controller/executor/host 接线、顶栏「语」。
- 引擎：无 JNI 改动；复用 Rime `custom_phrase` stabledb。
- 风险：保存后重启会话（轻量 `restartRime`）会短暂重建引擎；条目增多时面板列表用 LazyColumn 不影响输入延迟。
