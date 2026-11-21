# Proposal: desktop-cmp（D2）

## Why

用户目标是「一套 CMP UI 跨 Android/桌面」。D1 已让 macOS 壳可用、桌面引擎/协议 Gradle 化；D2 要回答并落地：**CMP 能否承担桌面输入法的候选窗/状态窗**，以及 `:ui:*` 如何加桌面 target。

本轮先做 **D0 可行性 spike**（master-plan §3.5 的硬前置），再按结果决定 D2 实施形态。

## D0 spike 结果（2026-11-21，macOS arm64，本机）

环境事实：

- Compose Multiplatform **1.12.2** Gradle 插件与 skiko **0.150.2** 在本机 Gradle 缓存中；Maven Central 可访问，`org.jetbrains.compose.material3:material3:1.9.0` 成功拉取。
- 新增 `:desktop:ui-spike`（Kotlin JVM + `org.jetbrains.compose`），构建与运行通过。

窗口行为事实（`./gradlew :desktop:ui-spike:run` 输出）：

```
spike: headless=false
spike: undecorated=true
spike: alwaysOnTop=true
spike: focusableWindowState=false
spike: type=NORMAL
spike: focusOwnerBefore=spike-app
spike: focusOwnerAfter=spike-app
spike: alive=true
```

即：**无边框置顶 CMP 窗口可创建；设置 AWT `focusableWindowState=false` 后，`requestFocus()` 不会把焦点从可聚焦的 app 窗口抢走**——macOS 上的候选窗技术路径成立。

**局限（如实记录）**：本环境无屏幕录制权限，无法做像素级截图验收；焦点判定为进程内断言（跨进程 app 焦点还需真机）。Linux 原生 Wayland 仍存在「客户端不能任意定位/置顶窗口」的协议约束（master-plan §3.5），候选窗需降级为 ibus panel/GTK4 面板。Windows 需 `WS_EX_NOACTIVATE` 互操作补洞，未在本机验证。

## What Changes（D2 实施，分步）

1. `:ui:design`/`:ui:ime`/`:ui:settings` 增加 `jvm("desktop")` target（KMP 化），共享 Compose 表面可编译到桌面。
2. 桌面 UI 进程：候选窗/状态窗用 CMP 渲染（D0 已验证），由壳提供光标矩形与按键流；设置界面复用 `:ui:settings`。
3. macOS 壳改造：Swift IMK 壳保留输入接管，候选窗改由 CMP 进程绘制（壳只传位置/文本），D0 的 AWT 非激活窗口即候选窗实现基础。
4. Linux ibus 壳（D-Bus engine + 行协议）与 Windows TSF 薄壳（COM DLL + JVM broker）按 master-plan §3.5 实施；Linux Wayland 候选窗降级为系统 panel/原生面板。
5. 协议扩展：highlight/cursor 同步、光标矩形、候选点击回传（P2-4 负载扩展）。

**本轮不做**：Linux/Windows 壳实现（需对应平台与 CI 矩阵，下一批）；Path B。

## Capabilities

### New Capabilities

- `desktop-cmp`：桌面共享 UI 与候选窗的可行性约束与实施契约。

### Modified Capabilities

（无。）

## Impact

- 新增 spike 模块与 Compose Multiplatform 插件接线（仅桌面目标，不影响 Android 构建）。
- 后续 D2 会把 `:ui:*` 改为 KMP 多 target；Android 产物不变。
- 风险：Linux Wayland 候选窗必须降级；Windows 需原生补丁；跨进程焦点需真机验证。
