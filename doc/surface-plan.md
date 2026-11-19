# 键盘表面层规划（surface track）

> 版本：2026-11-19 r4（长任务队列 T1-T4 执行完毕，待真机验收）。
> 定位：与 `doc/refactor/roadmap.md`（内核/配置/模块化六期主线，kernel 会话推进）划界——本文只管 IME 表面：键盘 UI、交互、候选体验，以及不进主线的小工程项。
> 节奏：**长任务队列顺序执行**（用户指令）——每项独立提交、双门禁、推送，完成一项再开下一项；标 ◆ 的项含设计决策，实现时先出方案再动手。

## 长任务队列（顺序执行，2026-11-19 定稿并执行完毕）

- **T1 ✅ 度量收紧**：行高默认 52→46、字母键字 22sp→19sp、ActionBar 44→40、气泡 44dp/24sp→40dp/20sp（f930edd）；真机微调以「键盘行高」偏好兜底
- **T2 ✅ 面板高度统一**：liquid/editor/clipboard 面板与数字板等内容带高统一为键盘内容带高（6×行高），随「键盘行高」偏好联动，切换零跳变（f35a522）
- **T3 ✅ 键面净化**：字母键手势只留输入类——长按=大写字母（无符号长按的键补齐）/既有正常符号（j/k/l/n/m）/数字（q-p 保留）；select_all、undo/cut/copy/paste、clipboard_window、Date_Lua、Google、History_Lua、Keyboard_editor、Keyboard_symbol、Keyboard_emoji 及 Liquid_fixed_*/ResetCandWeight/HideCandFunc/DelCandFunc 等拓展/面板切换手势全部撤除（4180280）
- **T4 ✅ 功能上收**：承载核实——面板切换类已有菜单/面板入口（📋/☻/123）覆盖；编辑操作类由系统文本选择工具条承载；候选删词由候选条长按承担；Date_Lua/Google/History_Lua 遗产入口裁除并记录（可回滚）（4180280）
- **T5 ⏳ 真机验收**（待主人装机）：①新度量手感（46dp/19sp 或行高滑杆现场调）②面板切换零跳变 ③字母键长按=大写/符号生效 ④菜单栏与系统文本工具条功能可用 ⑤回归（数字行/菜单栏两态、aux 次选、shift、剪贴板 chip、气泡）

## 已完成（2026-11-16 ~ 11-19）

- 数字行 + 菜单栏：组合态编码串回显挪上菜单栏、数字槽位切换候选栏（88076d7）
- Q 行数字角标摘除，长按数字保留为盲操作（bb778ce）
- CI：clang-format 钉 22.1.8、Boost tarball 缓存、Apple ld Darwin 兼容（b9b5006 / 1f0c163 / 0a07b72）
- 顶槽渲染与数字行解耦：`hasLeadingDigitRow` 成为「槽位交换」与「操作栏让位」的共同判据（审查遗留项）
- 候选长按删词：`deleteCandidate(global)` 直达候选条（长按触觉，Rime 流自动刷新）
- CI paths-ignore 增补 `desktop/**` / `harness/**` / `macos/**`
- 组合态回显点击清空编码（Esc 语义）
- 键盘行高设置：`keyboard_row_height` 偏好（36-80dp）接入 Compose 键盘，设置变更实时生效（同文对齐项）
- 长按/连发/滑动阈值走偏好：`LONG_PRESS_TIMEOUT`（0=跟随系统）/ `REPEAT_INTERVAL`（默认 80ms）/ `SWIPE_TRAVEL`（0=禁用滑动）接入 dataKeyGestures 并统一 GboardChromeKey 节奏，设置变更实时生效（72ac5a0；返工 9d400e9 修钳制下限错位）
- 按键气泡预览：`POPUP_ON_KEY_PRESS`（默认开启）接入 GboardKeyFace——按压浮出键面字符气泡，Popup 浮层避行高裁切，CompositionLocal 单点供值零每键 listener（2220ee7）
- 剪贴板快捷粘贴 chip：`clipboard_suggestion`/`clipboard_suggestion_timeout` 双键门控的菜单栏预览 chip——最新剪贴文本点击即 Inline commit 直分发粘贴，敏感剪贴不显示、消费即隐、面板打开态让位，预览截断为 24 字符+省略号、timeout 0=不自动隐藏（3aef2bd+6619719）
- 启动器图标重设计：「快」印章（Noto Sans SC Bold 轮廓 + 应用主色，矢量自适应图标，PNG 密度资产退役）（01e0180）
- T1 度量收紧：行高默认 46dp、字母键字 19sp、ActionBar 40dp/30dp chips、气泡 40dp/20sp（f930edd）
- T2 面板高度统一：KEYBOARD_CONTENT_ROWS=6×行高成为所有 surface 的统一带高（liquid/clipboard/非 qwerty 板随「键盘行高」联动，切换零跳变）（f35a522）
- T3+T4 键面净化与功能上收：字母键手势只留输入类（长按=大写/既有符号、滑动=符号/形码），复制粘贴/面板切换/lua 拓展全部撤除——编辑操作由系统文本工具条承载、剪贴板走 chip+面板、面板切换走菜单栏既有入口；候选删词由候选条长按承担（4180280）

## 已排除的伪需求 / 划界

- 候选翻页：`Candidates.Paged`（hasPrev/hasNext）是 Trime 遗产的休眠分支——本应用 native 侧永远发 `Bulk` 全量候选，LazyRow 横滚已覆盖全部候选，翻页无意义；`changeCandidatePage` 保留但无消费方
- 深色模式：`ColorSchemeResolver` 已支持跟随系统（含单测），无需再动
- 撤销上屏：z 长按的 "undo" 是发给应用的 Control+z（应用层撤销）；引擎侧 revert 只在组合态由 BackSpace 承担，符合 Rime 语义
- 内核裁剪、Kotlin 运行时、desktop harness：归 kernel 会话与 `doc/refactor/roadmap.md`，本文不重复
- 数字行符号角标：去杂讯方向已定（bb778ce），不再加回
