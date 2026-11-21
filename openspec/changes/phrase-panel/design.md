# Design: phrase-panel

## 1. 数据面

`custom_phrase.txt` 是 Rime `stabledb`（`db_class: stabledb`，`user_dict: custom_phrase`），由 DB resolver 以「用户目录优先、shared 目录回退」解析：

- 首次加载：用户目录无文件 → 从 `DataManager.sharedDataDir/custom_phrase.txt` 播种到用户目录；
- 编辑：`CustomPhraseStore` 写用户目录文件，保留原文件头注释与 Tab/权重格式；
- 生效：写盘后 `RimeDaemon.restartRime(fullCheck = false)`，stabledb 在会话构建词典时读取。

## 2. 格式

`#` 注释与空行作为 header 原样保留；数据行 `文本<TAB>编码<TAB>权重`；权重缺省 1；字段不足/空文本/空编码的行丢弃（不让坏行污染文件）。解析/渲染为纯函数，JVM 单测覆盖。

## 3. UI

`PhrasePanel`（Compose，app 侧面板槽）：

- 顶部：文本输入、编码输入、「＋」新增（任一为空时不添加；重复 text+code 忽略）；
- 列表：`LazyColumn`，行内显示文本+编码，点击上屏（`ImeCommand.CommitText` + 关闭面板），右侧 × 删除；
- 颜色/尺寸使用 `ui:design` 的 `LocalImePalette`/token。

顶栏「语」→ `ImeCommand.OpenPanel(PanelId.Phrase)`；controller 把 `PhraseWindow` 映射为 `PanelId.Phrase`；executor 打开面板时 attach 该窗口。

## 4. 测试与验收

- `CustomPhraseCodecTest`：header 保留、解析、缺省权重、坏行丢弃、render-parse round-trip。
- 模拟器：面板显示播种的置顶词；点选上屏并回到键盘；新增/删除可写盘（真机最终验收）。
