# Tasks

> 状态（2026-11-21）：阶段 1 实施完成；阶段 2（Compose IA + SettingSpec）留待后续批次。

## 1. 死设置清理

- [x] 1.1 删除 7 个零消费者偏好与 4 个死常量
- [x] 1.2 全仓编译/测试通过，确认无消费者遗漏

## 2. 外观组

- [x] 2.1 新增 `AppPrefs.Appearance`（深浅模式/强调色/候选单页/候选字号/键面提示），移出虚拟键盘页的三项
- [x] 2.2 新增 `AppearanceSettingsFragment` + 导航目的地 + 首页入口 + 中英文案
- [x] 2.3 DE 存储同步纳入外观组

## 3. 表面接入

- [x] 3.1 引擎 `AccentPreset` + `SetAccent` + 状态单测
- [x] 3.2 `ui:design` 的 `ImeAccent`/`withAccent`；`ImeSurface` 应用 accent
- [x] 3.3 controller 首帧读取外观偏好；`ComposeInputView` listener 实时派发变更
- [x] 3.4 模拟器冒烟（外观页可见、键盘强调色/深浅生效）+ 截图

## 4. 阶段 2（未开始）

- [ ] 4.1 `:ui:settings` Compose 六组页面
- [ ] 4.2 `SettingSpec` 注册表 + 无孤儿双向断言
- [ ] 4.3 设置搜索；删除无效可见项与两套深色概念
