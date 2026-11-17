# 键盘表面层规划（surface track）

> 版本：2026-11-17 r1。
> 定位：与 `doc/refactor/roadmap.md`（内核/配置/模块化六期主线，kernel 会话推进）划界——本文只管 IME 表面：键盘 UI、交互、候选体验，以及不进主线的小工程项。
> 节奏：小步快走，每项独立提交独立验收；标 ◆ 的项启动时走 openspec-propose。

## 已完成（2026-11-16 ~ 11-17）

- 数字行 + 菜单栏：组合态编码串回显挪上菜单栏、数字槽位切换候选栏（88076d7）
- Q 行数字角标摘除，长按数字保留为盲操作（bb778ce）
- CI：clang-format 钉 22.1.8、Boost tarball 缓存、Apple ld Darwin 兼容（b9b5006 / 1f0c163 / 0a07b72）

## 本次落地（2026-11-17）

- 顶槽渲染与数字行解耦：`hasLeadingDigitRow` 成为「槽位交换」与「操作栏让位」的共同判据，任意板面两态带高恒定（独立审查遗留项）
- 候选长按删词：`deleteCandidate(global)` 直达候选条（快拼卫生动作，长按触觉 + 无对话框，Rime 流自动刷新）
- CI paths-ignore 增补 `desktop/**` / `harness/**` / `macos/**`（桌面轨道不再触发 Android CI）

## 行动项（按价值排序）

1. ◆ 候选分页接线：`ImeUiState.changeCandidatePage` 已就绪但无 UI 入口——先真机确认 LazyRow 横滚够不够用，不够再设计翻页手势/按键
2. ◆ 候选序号上屏：组合态按数字选首屏候选（需先设计数字入口——数字行此时已是候选栏；备选方案：字母行上滑出数字）
3. 键盘高度/行高自适应：小屏与大字号下的行高、字体缩放实测调参（52dp 现状为基准）
4. 组合态回显交互评估：长按菜单栏回显串 = 清空编码（ESC 等价物）是否值得做
5. 真机回归清单维护：数字行/菜单栏两态切换、aux 次选、shift 行为、悬浮窗遮挡——随每项改动跑
6. 工程面：gh auth login 后拉历史失败日志，把 11-15 的 :app:check 失败与 Nightly build 失败逐个定案

## 不做 / 划界

- 深色模式：`ColorSchemeResolver` 已支持跟随系统（含单测），无需再动
- 内核裁剪、Kotlin 运行时、desktop harness：归 kernel 会话与 `doc/refactor/roadmap.md`，本文不重复
- 数字行符号角标：去杂讯方向已定（bb778ce），不再加回
