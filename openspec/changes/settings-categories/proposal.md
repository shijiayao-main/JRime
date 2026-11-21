# Proposal: settings-categories

## Why

用户反馈设置页内容太长。当前是「一个超长滚动页」：输入组 24 项 + 键盘 14 + 候选 2 + 外观 5 +
剪贴板 5 + 系统 5，共约 55 行，找设置靠滚动或全局搜索。

## What Changes

- 设置页改为**两层分类展示**（同一 Compose 屏幕，Android 与桌面共享）：
  - 分类首页：搜索框 + 六个分类卡片（标题、一句话说明、项数、`›`）；
  - 分类详情：`‹ 返回 | 分类标题 | N 项设置` + 该分类各自独立搜索 + 设置行；
  - 首页搜索仍跨全部分类（输入关键词直接给出分组结果）。
- `SettingGroup` 新增 `summary`；六个分类补一句说明（中/英/繁）。
- `SettingsStrings` 新增 `back`/`itemsCountFormat`/`categories`，Android 与桌面宿主提供本地化文案。
- 新增回归测试：每个分类必须有 landing summary；`SettingsSpecsFactoryTest` 继续保证无孤儿项。

## Impact

- `:ui:settings`：屏幕结构（分类首页/详情/搜索复用），无 API 破坏（新增默认参数）。
- app/desktop：分类 summary 与宿主文案。
- 验证：模拟器实拍分类首页与详情（见 screenshots）；桌面设置窗口仍正常渲染。
- 不做：每个设置项加图标/二级分组（目前分类已把长度降到可接受）。
