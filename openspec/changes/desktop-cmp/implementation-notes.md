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

## 2026-11-21 · D2 第一批：KMP 化 + 桌面候选窗渲染

- KMP 化：`:core:model`/`:core:engine`/`:ui:design`/`:ui:ime`/`:ui:settings` 改为
  `kotlin("multiplatform")` + AGP 9 的 `com.android.kotlin.multiplatform.library`
  （androidLibrary target + `jvm("desktop")`）；源码迁 commonMain/kotlin，测试迁 jvmTest/commonTest。
  Android app 构建与单测不受影响；KMP jvm/desktop 单测全绿（Core 20 + ui:ime 7）。
- 共享桌面表面：新增 `DesktopCandidateSurface`（编码回显 + `CandidateStripView`，共用 palette/token，
  不含软键盘）；`:desktop:ui`（CMP JVM 模块）以无边框、置顶、`focusableWindowState=false`
  的窗口渲染它——`JRIME_UI_SMOKE=1 ./gradlew :desktop:ui:run` 输出
  `desktop-ui: shared ImeSurface up (candidates=3)` 并干净退出。
- 结构证据：desktop:ui 直接依赖 `:ui:ime`/`:ui:design` 的 desktop 变体并成功渲染同一份代码；
  D0 的非激活窗断言（focusOwner 不抢占）继续成立。
- 下一批：`:desktop:ui` 接行协议（EngineClient）+ 候选点击回传 + 光标矩形；
  macOS Swift 壳改为「IMK 接管 + 向 CMP UI 进程转发」；随后 Linux ibus / Windows TSF。

## 2026-11-21 · D2 第二批：desktop:app（引擎+协议+CMP 候选窗一体）

- 协议处理抽出 `DesktopEngineSession`（`:desktop:engine`）：headless `DesktopServer` 与
  `:desktop:app` 共用同一份请求处理；`EngineSnapshotSampler` 支持 commit override
  （app 先读 commit 再交给采样器，从而同时把 commit 文本推给壳）。
- 新增 `:desktop:app`（Kotlin JVM + CMP）：`java -cp` 单进程内启动 librime + 行协议
  （stdin/stdout）+ 无边框置顶 `focusableWindowState=false` 的候选窗（`DesktopCandidateSurface`）。
  - 协议新增：`rect x y w h` 把候选窗移到光标附近并回 `{"ok":true}`；
  - App 内候选点击：选中后异步输出 `{"event":"update","preedit":...,"composing":...,"commit":...}`，
    由壳负责上屏/刷新 marked text（Swift 接入下一批）。
- 冒烟：`printf 'ready\nk n\nk i\n' | ./gradlew :desktop:app:run --args="--shared ... --user ... --dylib harness/build/librime_jni.dylib"`
  → ready 含 proto/engine，`k n`/`k i` 返回组合态与候选，stdin EOF 后干净退出（BUILD SUCCESSFUL）。
  （rime 日志中的 predict.db / gram 缺失为此前已知的可选件缺失，不影响输入。）
- 未做：Swift 壳接入、真实窗口定位/多屏、Linux ibus、Windows TSF。
