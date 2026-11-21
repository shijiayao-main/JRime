# Tasks

> **实施进度（2026-11-21）**：候选分页（引擎状态+reducer+UI 页码/箭头）、候选长按菜单
> （置顶/删词/复制）、键面提示开关、简繁/全角顶栏开关已落地；4 个偏好（单页数量/
> 候选字号/提示开关）已入 `AppPrefs`，触感复用既有 `vibrate_on_key_press`。
> 模拟器已验证分页渲染（7 条/页 + 1/3 + 箭头）。长按菜单/触感/开关的真机走查留待最终。

> 状态（2026-11-21）：规划完成，随 M2 实施。范围外：设置 IA（`settings-v2`）、主题外观、词库、桌面。

## 1. 引擎状态与命令

- [x] 1.1 `ImeSessionState` 增加 `candidatePageSize`/`candidatePageIndex`/`candidatePageCount`；`ImeCommand` 增加 `ChangeCandidatePage`/`SetCandidatePageSize`/`CandidateAction`
- [x] 1.2 reducer 实现分页计算、翻页 clamp、改页大小保持位置、候选事件重置；单测覆盖
- [x] 1.3 `ImeCommandExecutor` 处理 `CandidateAction`（置顶/删词/复制）与分页命令（UI-only no-op）

## 2. 候选条

- [x] 2.1 `CandidateStripView` 按页切片渲染，点选使用全局 index
- [x] 2.2 页码与左右箭头（单页隐藏），点击派发翻页
- [x] 2.3 长按菜单（置顶/删词/复制/查编码占位），菜单项派发 `CandidateAction`
- [x] 2.4 单页数量/字号从 `ImeSurfaceConfig`（app 从偏好读取）生效
- [ ] 验收：分页翻页正确、跨页点选正确、长按菜单可用

## 3. 键面提示

- [x] 3.1 `ImeSurfaceConfig.showKeyHints` + `KeySurface` 开关渲染
- [x] 3.2 提示优先级 labelSymbol > hint；关闭后不渲染角标/气泡 hint
- [ ] 验收：开关即时生效、提示不遮挡键面字

## 4. 简繁/全角开关

- [x] 4.1 顶栏新增「简/繁」「全/半」action（动态 label + highlighted）
- [x] 4.2 executor toggle → 引擎 option → 状态回写点亮
- [ ] 验收：真机/模拟器切换后候选与标点行为变化，按钮状态一致

## 5. 触感

- [ ] 5.1 偏好 `keyboard_vibrate_enabled` 默认开，host 读取
- [ ] 5.2 tap/long/slide 三档语义统一；关闭后无触感
- [ ] 验收：真机走查

## 6. 偏好与收尾

- [x] 6.1 `AppPrefs` 新增 4 个偏好（值 + key 常量），默认值与 design 一致
- [ ] 6.2 模拟器冒烟 + 截图；全仓闸门（test/assembleDebug/spotless）
- [ ] 6.3 实现记录 + `doc/master-plan.md` M2 状态回写
