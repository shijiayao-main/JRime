# 键盘表面层规划（surface track）

> 版本：2026-11-18 r2。
> 定位：与 `doc/refactor/roadmap.md`（内核/配置/模块化六期主线，kernel 会话推进）划界——本文只管 IME 表面：键盘 UI、交互、候选体验，以及不进主线的小工程项。
> 节奏：小步快走，每项独立提交独立验收；标 ◆ 的项启动时走 openspec-propose。

## 已完成（2026-11-16 ~ 11-18）

- 数字行 + 菜单栏：组合态编码串回显挪上菜单栏、数字槽位切换候选栏（88076d7）
- Q 行数字角标摘除，长按数字保留为盲操作（bb778ce）
- CI：clang-format 钉 22.1.8、Boost tarball 缓存、Apple ld Darwin 兼容（b9b5006 / 1f0c163 / 0a07b72）
- 顶槽渲染与数字行解耦：`hasLeadingDigitRow` 成为「槽位交换」与「操作栏让位」的共同判据（审查遗留项）
- 候选长按删词：`deleteCandidate(global)` 直达候选条（长按触觉，Rime 流自动刷新）
- CI paths-ignore 增补 `desktop/**` / `harness/**` / `macos/**`
- 组合态回显点击清空编码（Esc 语义，1e 系 commit）
- 键盘行高设置：`keyboard_row_height` 偏好（36-80dp）接入 Compose 键盘，设置变更实时生效（同文对齐项）
- 长按/连发/滑动阈值走偏好：`LONG_PRESS_TIMEOUT`（0=跟随系统）/ `REPEAT_INTERVAL`（默认 80ms）/ `SWIPE_TRAVEL`（0=禁用滑动）接入 dataKeyGestures 并统一 GboardChromeKey 节奏，设置变更实时生效（72ac5a0；最初记录的 225d61a 为 amend 前哈希，已修正）
- 按键气泡预览：`POPUP_ON_KEY_PRESS`（默认开启）接入 GboardKeyFace——按压浮出键面字符气泡，Popup 浮层避行高裁切，CompositionLocal 单点供值零每键 listener（9bc45b3）

## 行动项（按价值排序）

1. ◆ 候选序号上屏：组合态按数字选首屏候选（需先设计数字入口——数字行此时已是候选栏；备选方案：字母行上滑出数字）
2. 剪贴板快捷粘贴 chip：菜单栏在有新剪贴板文本时出预览 chip（Gboard 形态）
3. 键盘行高自适应细节：行高变化时字体是否随行高缩放的实测调参
4. 真机回归清单维护：数字行/菜单栏两态切换、aux 次选、shift 行为、悬浮窗遮挡——随每项改动跑
5. 工程面：gh auth login 后拉历史失败日志，把 11-15 的 :app:check 失败与 Nightly build 失败逐个定案

## 已排除的伪需求 / 划界

- 候选翻页：`Candidates.Paged`（hasPrev/hasNext）是 Trime 遗产的休眠分支——本应用 native 侧永远发 `Bulk` 全量候选，LazyRow 横滚已覆盖全部候选，翻页无意义；`changeCandidatePage` 保留但无消费方
- 深色模式：`ColorSchemeResolver` 已支持跟随系统（含单测），无需再动
- 撤销上屏：z 长按的 "undo" 是发给应用的 Control+z（应用层撤销）；引擎侧 revert 只在组合态由 BackSpace 承担，符合 Rime 语义
- 内核裁剪、Kotlin 运行时、desktop harness：归 kernel 会话与 `doc/refactor/roadmap.md`，本文不重复
- 数字行符号角标：去杂讯方向已定（bb778ce），不再加回
