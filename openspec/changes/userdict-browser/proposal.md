# Proposal: userdict-browser（M3.3）

## Why

用户词库此前只有「备份/恢复/导入/导出」四个整库动作，无法查看学到了哪些词、无法加词/删词/调频。
本轮发现导出格式其实是纯文本（`code<TAB>phrase<TAB>c=<commits> d=.. t=..`），于是可以完全走
引擎自带的 import 通道实现逐词管理，不需要新的 JNI/LevelDB 操作。

顺带修了一个 M4 遗留 bug：设置中心「系统与数据」组此前只加了「导入字体」一行，
导出/恢复备份、复制诊断信息三个动作的 spec 行漏加（常量与 handler 都在），界面上根本点不到。

## What Changes

- 新增 `UserDictText`（导出文本解析/渲染、+1 调频、负 commits 标记删除、添加词条）与 6 个单测。
- 新增「管理用户词典」页面：词典选择、搜索、词条列表（词条/编码/频次）、+1、删除/恢复、添加词条；
  编辑后经 `UserDictManager.importUserDict` 合并回引擎并重新导出刷新。
- 设置中心系统组补齐动作行：管理用户词典、导出备份、恢复备份、复制诊断信息（+ 回归测试断言可见性）。
- 未做：批量删除/多选、词条排序切换、编辑编码本身（删除后重加即可）。

## Impact

- app：`data/userdict/UserDictText.kt`、`UserDictBrowserFragment`、导航路由、设置 action、文案。
- 引擎/JNI：无改动，复用已有 import/export。
- 风险：删除是「负 commits 标记」语义，导出里仍可见（标为已删除），可恢复；Rime 重新部署后生效。
