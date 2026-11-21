# Implementation Notes — desktop-cmp（D0）

## 2026-11-21

- 环境：Compose Multiplatform 1.12.2 / skiko 0.150.2 已在 Gradle 缓存；Maven Central 可访问，
  material3 1.9.0 拉取成功；`screencapture` 无权限（无法截图验收）。
- `:desktop:ui-spike` 构建并运行：`Window(undecorated=true, alwaysOnTop=true)` +
  AWT `focusableWindowState=false`；焦点断言通过——候选窗 `requestFocus()` 后
  `KeyboardFocusManager.focusOwner` 仍为宿主 `spike-app` 窗口；窗口 `alive=true` 后干净退出。
- 结论：macOS 上 CMP 候选窗技术路径成立；Linux 原生 Wayland 需降级系统 panel/原生面板；
  Windows 需 `WS_EX_NOACTIVATE` 互操作补洞。跨进程焦点与透明/点击穿透留待真机/Windows 验证。
- 下一步：`:ui:*` KMP 化 + `:desktop:ui` 宿主（tasks §2）。
