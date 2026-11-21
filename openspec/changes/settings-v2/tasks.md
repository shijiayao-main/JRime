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

## 4. 阶段 2（进行中：Compose 设置中心已落地）

- [x] 4.1a `:ui:settings` 模块 + Compose 设置中心（搜索 + 分组行 + 开关/滑杆/选项/文本编辑器），替换首页五个设置入口
- [ ] 4.1b 六组信息架构重排（当前沿用 provider 的 5 组 + 数据组）
- [x] 4.2a `SettingSpec` 从既有偏好声明自动派生（单一事实源，未覆盖类型隐藏而非丢失）+ 运行期 fail-fast
- [ ] 4.2b 无孤儿双向断言（需要可注入的 provider 构建，待补）
- [x] 4.3a 设置搜索
- [ ] 4.3b 删除无效可见项（横屏/分屏等，依赖遗留 Keyboard/Key 模型）与两套深色概念
