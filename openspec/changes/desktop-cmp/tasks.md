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
- [x] 2.3 `:ui:settings` desktop target 编译通过（桌面设置窗口宿主下一批）
- [~] 2.4 `:desktop:ui` 候选窗宿主（CMP）已跑通（共享 ImeSurface 渲染 + demo host）；协议客户端/真实 Shell 接入下一批
- [ ] 2.5 协议扩展：highlight/cursor/光标矩形/候选点击
- [ ] 2.6 macOS 壳改为「IMK 接管 + CMP 候选窗」；真机验收
- [ ] 2.7 Linux ibus 壳 + X11 候选窗 + Wayland 降级路径
- [ ] 2.8 Windows TSF 薄壳 + CMP 候选窗（WS_EX_NOACTIVATE 补洞）+ CI 首次实跑
