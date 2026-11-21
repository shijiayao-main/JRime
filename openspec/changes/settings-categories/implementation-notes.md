# Implementation Notes — settings-categories

## 2026-11-21

- `SettingsScreen`：`openedGroupId` 状态切换分类首页/详情；首页无搜索词显示分类卡片，
  有搜索词直接显示跨分类结果；详情页保留「返回+标题+项数」和局部搜索。
- 组件拆分：`SearchField`、`CategoryList`、`CategoryCard`、`CategoryHeader`、`SettingsRowList`
  （搜索与详情复用同一行渲染，保留 `key(revision)` 强制刷新）。
- `SettingGroup` 增加 `summary`（默认空，向后兼容）；`SettingsSpecsFactory` 的 `GroupDef`
  增加 `summaryRes`，六组分别配中/英/繁说明；桌面设置窗口补英文 summary。
- 模拟器（emulator-5554, arm64）：分类首页显示 6 个分类卡片（输入 24 项 / 键盘 14 / 候选 2 /
  外观 5 / 剪贴板 / 系统），进入「输入」显示 `‹ 返回 | 输入 | 24 项设置 | 搜索设置` + 行，
  返回回首页；截图 `screenshots/2026-11-21-category-landing.png`、`...-category-detail.png`。
- 桌面 `:desktop:settings` 冒烟仍通过（window up, rows=6）。
