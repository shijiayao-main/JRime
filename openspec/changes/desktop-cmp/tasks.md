# Tasks

> 状态（2026-11-21）：D0 spike 完成（macOS CMP 候选窗可行性成立）；D2 实施拆到后续批次。

## 1. D0 spike（已完成）

- [x] 1.1 接线 Compose Multiplatform 插件（1.12.2）+ `:desktop:ui-spike` 模块
- [x] 1.2 无边框/置顶/非激活窗口创建并打印 AWT 事实
- [x] 1.3 焦点不抢占断言（focusOwner 保持宿主窗口）
- [x] 1.4 记录平台约束（Wayland 降级、Windows 需补洞、无截图权限）

## 2. D2 实施（KMP 化与桌面候选窗已验证）

- [x] 2.1 `:core:model`/`:core:engine`/`:ui:design`/`:ui:ime`/`:ui:settings` KMP 化（androidLibrary + jvm(desktop)；AGP 9 用 `com.android.kotlin.multiplatform.library`）
- [x] 2.2 `:ui:ime` desktop target 编译通过；共享 `DesktopCandidateSurface`（候选条+编码回显）落地
- [x] 2.3 `:ui:settings` desktop target + `:desktop:settings` 设置窗口（共享 SettingsScreen + properties store）；候选窗 app 读取同一配置文件
- [x] 2.4 `:desktop:app`：引擎 + 行协议 + CMP 候选窗一体（stdin/stdout 协议、`rect` 定位请求、候选点击回推 `event:update`）；`desktop:ui` demo 宿主保留为无引擎预览
- [~] 2.5 协议扩展：候选点击回推 `{"event":"update",preedit,composing,commit}` + `rect` 请求已定义；highlight 同步与 Swift 侧处理下一批
- [ ] 2.6 macOS 壳接入：EngineClient 起动 `desktop:app`、event 处理、打包 CMP 运行时
      ——候选窗定位**结论（待裁决）**：macOS 公开 IMK API 不提供插入点屏幕坐标，自绘候选窗需要辅助功能权限
      或 accessibility API；系统 `IMKCandidates` 无此成本。默认继续用系统候选窗，CMP 候选窗用于
      Linux X11/Windows（可由 X11/TSF 拿到光标矩形），状态窗/设置窗仍共享 CMP。
- [ ] 2.7 Linux ibus 壳 + X11 候选窗 + Wayland 降级路径
- [ ] 2.8 Windows TSF 薄壳 + CMP 候选窗（WS_EX_NOACTIVATE 补洞）+ CI 首次实跑
