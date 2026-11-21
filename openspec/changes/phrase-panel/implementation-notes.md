# Implementation Notes — phrase-panel

## 2026-11-21

- `CustomPhraseCodec`：保留 header 注释、解析 `文本\t编码\t权重`（权重缺省 1）、坏行丢弃；
  `CustomPhraseCodecTest` 3 例通过（含 render-parse round-trip）。
- `CustomPhraseStore`：用户目录 `custom_phrase.txt` 读写；用户目录缺失时从
  `DataManager.sharedDataDir/custom_phrase.txt` 播种（修复随包 115 条置顶词此前只存在于
  shared 只读回退、面板不可编辑的问题）；保存后 `RimeDaemon.restartRime(fullCheck=false)`。
- `PhraseWindow` + controller `PhraseWindow → PanelId.Phrase` + executor 打开面板；
  `PhrasePanel`（列表/点选上屏/新增/删除，使用 `LocalImePalette` 与 design token）。
- 顶栏：新增「语」动作；闲置动作行改为 `LazyRow` 横向滚动（修复动作增多后被挤出屏幕——
  此前简/繁、全/半、语三个动作实际不可见）。
- 简繁状态修正：schema 用 `zh_simp/zh_trad/zh_hk/zh_tw` 选项（无 `simplification` 布尔），
  首帧在四项都未设置时默认启用 `zh_simp`（简体）；顶栏按钮改走新的
  `ImeCommand.SetSimplified`（同时写 zh_simp/zh_trad），选项事件回写状态。
- 模拟器：面板显示播种置顶词（啊/不/吧/才…），点选「啊」后文本上屏并返回键盘
  （`screenshots/emulator-2026-11-21-phrase-panel.png`、`...-phrase-committed.png`）。
- 全仓闸门绿。
