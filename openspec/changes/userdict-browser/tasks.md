# Tasks

## 1. 文本层

- [x] 1.1 `UserDictText` 解析/渲染/调频/删除/添加
- [x] 1.2 单测（解析、round-trip、bump、delete、add 去重）

## 2. UI 与接线

- [x] 2.1 `UserDictBrowserFragment`（词典选择/搜索/列表/加词/调频/删除恢复）
- [x] 2.2 导航路由 `UserDictBrowser` + 设置中心动作入口
- [x] 2.3 设置中心系统组补齐四个数据动作行（修复 M4 遗漏）+ 回归断言
- [x] 2.4 中英文文案

## 3. 验证

- [x] 3.1 app 单测/编译/spotless 全绿
- [x] 3.2 模拟器验证：设置中心滚到「系统与数据」不崩、四个数据动作行可见（screenshots/settings-system-group.png）；
      「管理用户词典」页打开、词典 chip/搜索/加词 UI 正常（screenshots/userdict-browser.png）
- [ ] 3.3 加词→上屏→删除 端到端（模拟器词典为空，留真机验收）
