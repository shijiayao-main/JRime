# Design: desktop-cmp（D0 spike + D2 路线）

## 1. D0 spike 设计

- 模块：`:desktop:ui-spike`（Kotlin JVM 17 + `org.jetbrains.compose` 1.12.2 + material3）。
- 程序：创建 `Window(undecorated=true, alwaysOnTop=true)`；在 `FrameWindowScope` 内设置
  `window.focusableWindowState = false`；同时创建一个可聚焦的 AWT `Frame` 作为「宿主 app」。
- 断言：显示候选窗后读 `KeyboardFocusManager.focusOwner`，调用候选窗 `requestFocus()`
  后再读；焦点必须仍在宿主 Frame。
- 结果见 proposal：`focusOwnerBefore/After = spike-app`，窗口 `alive=true`，干净退出。

## 2. D2 目标形态

```
Swift IMK / ibus engine / TSF DLL   （薄壳：输入接管 + 光标矩形）
        │ 行协议（k/sel/commit/... + 新增 highlight/cursor/rect）
        ▼
桌面引擎 JVM 进程（DesktopServer，已有）
        ▲ 进程内/共享状态
        │
CMP UI 进程（候选窗/状态窗/设置）——:ui:* 的 desktop target
```

- macOS：候选窗用 CMP（D0 已验证非激活/置顶）；Swift 壳负责 IMK 接管、把 caret rect / 按键/
  候选点击经协议交给 UI 进程。
- Linux：ibus engine + 行协议；X11 下 CMP 候选窗可定位；**原生 Wayland 降级**为 ibus panel
  或 GTK4 小面板（协议约束，不是设计选择）。
- Windows：TSF DLL + JVM broker；候选窗 CMP + `WS_EX_NOACTIVATE` 互操作补洞。

## 3. KMP 化步骤（下一批）

1. `:ui:design` 改 `kotlin("multiplatform")`，commonMain 放 tokens/palette，android+desktop。
2. `:ui:ime` 同样加 desktop target；`ImeSurfaceHost` 由桌面宿主实现。
3. `:ui:settings` 加 desktop target，桌面设置窗口直接复用。
4. 新增 `:desktop:ui` 模块（JVM）：候选窗/状态窗/设置窗宿主 + 协议客户端。
5. Android 产物不变（android target 继续产出 AAR）。

## 4. 风险

- CMP 桌面窗口的透明/阴影/点击穿透在 Windows 有已知 issue（CMP-6639/8118/6036），需补洞或原生面板。
- 跨进程焦点：D0 只验证进程内；真实候选窗与宿主 app 跨进程，需 macOS 真机与 Windows 验证。
- Linux Wayland 候选窗不可客户端定位——必须降级；记录为验收前提。
