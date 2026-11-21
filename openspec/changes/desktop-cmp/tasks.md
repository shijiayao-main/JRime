# Tasks

> 状态（2026-11-21）：D0 spike 完成（macOS CMP 候选窗可行性成立）；D2 实施拆到后续批次。

## 1. D0 spike（已完成）

- [x] 1.1 接线 Compose Multiplatform 插件（1.12.2）+ `:desktop:ui-spike` 模块
- [x] 1.2 无边框/置顶/非激活窗口创建并打印 AWT 事实
- [x] 1.3 焦点不抢占断言（focusOwner 保持宿主窗口）
- [x] 1.4 记录平台约束（Wayland 降级、Windows 需补洞、无截图权限）

## 2. D2 实施（下一批）

- [ ] 2.1 `:ui:design` KMP 化（android + desktop）
- [ ] 2.2 `:ui:ime` 加 desktop target，桌面宿主实现 `ImeSurfaceHost`
- [ ] 2.3 `:ui:settings` 加 desktop target + 桌面设置窗口
- [ ] 2.4 `:desktop:ui` 候选窗/状态窗宿主（CMP）+ 与引擎进程的协议客户端
- [ ] 2.5 协议扩展：highlight/cursor/光标矩形/候选点击
- [ ] 2.6 macOS 壳改为「IMK 接管 + CMP 候选窗」；真机验收
- [ ] 2.7 Linux ibus 壳 + X11 候选窗 + Wayland 降级路径
- [ ] 2.8 Windows TSF 薄壳 + CMP 候选窗（WS_EX_NOACTIVATE 补洞）+ CI 首次实跑
