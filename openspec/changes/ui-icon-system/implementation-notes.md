# Implementation Notes — ui-icon-system

## 2026-11-21

- `ImeIcon` 放在 `:ui:design`（KMP），枚举项 = `vector` + `key`；底层为 outlined Material Symbols
  （`material-icons-extended`，Apache-2.0，AboutLibraries 自动收录）。
- 布局接线：`ToolbarAction.icon`/`PanelSlot.icon`/`SettingGroup.icon`；顶栏 chip 有 icon 时渲染
  `Icon(tint=…)`，否则渲染原文字（值 chip 保持文字）；候选翻页/面板返回/收起改为图标。
- 设置分类卡：40dp 圆角底座（primary 12%）+ 22dp 图标 + 标题/说明/项数；Android 与桌面共用。
- 设计 token：`ImeDimens.IconSmall=18dp`、`IconCategory=22dp`（SurfaceGuard 禁止裸 dp）。
- 测试：`ImeIconSetTest`（唯一/无方案词/核心面覆盖）、`SettingsSpecsFactoryTest`（每组必须有图标）、
  既有 SurfaceGuard 通过。
- 模拟器（emulator-5554）：idle 顶栏显示 [⚙][，。][123-grid][📋][😊][简][半][书签] + 中/⌄；
  候选行右侧 chevron + 1/N；设置分类首页 6 张卡片均带图标。截图归档。
- 已知：debug APK 83MB（图标库 +6MB）；release 后续验证 R8 裁剪。候选长按菜单/键盘功能键图标后续。
