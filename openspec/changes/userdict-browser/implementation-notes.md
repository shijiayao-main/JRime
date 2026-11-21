# Implementation Notes — userdict-browser

## 2026-11-21

- 用户词库文本格式（librime `UserDb::Export`）：`code<TAB>phrase<TAB>c=<commits> d=<dee> t=<tick>`，
  value 明文可改；import 合并规则：正 commits 取 max，负 commits 标记删除（`UserDbImporter::Put`）。
- `UserDictText` 负责解析/渲染与三种编辑（+1、删除、添加），全部通过
  `UserDictManager.importUserDict(bytes)` 走引擎通道；UI 每次编辑后重新 export 刷新列表。
- 修复：`SettingsSpecsFactory` 的 dataGroup 只加了「导入字体」，漏了「导出/恢复备份」「复制诊断」，
  导致 M4 的备份 UI 实际不可见；现补全并新增测试断言 system 组包含全部动作 key。
- 测试：`UserDictTextTest` 6 例 + `SettingsSpecsFactoryTest` 新增可见性断言，app 单测全绿；
  编译/assemble/spotless 绿。
