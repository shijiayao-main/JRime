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

## 2026-11-21 · 模拟器验证与 slider 崩溃修复

- 模拟器（emulator-5554，arm64，2076x2152）验证：设置中心滚动到「系统与数据」不再崩溃，
  可见 管理用户词典/导出用户数据备份/恢复用户数据备份/复制诊断信息 四行；
  「管理用户词典」页正常（4 个词典 chip、搜索、加词行、共 0 条）。
- **顺带修复真机级崩溃**：设置页滑到部分滑块行时 `NegativeArraySizeException:
  -2147483648`（`EditTextInt` 类大范围整数偏好被映射成 Material3 Slider，steps 溢出）。
  修复：大范围整数改为文本编辑行；Slider 的 steps 计算加防御（范围>1000 步则连续滑块）；
  新增「sliders always have a bounded tick range」回归测试。
- 证据截图：`screenshots/settings-system-group.png`、`screenshots/userdict-browser.png`。
