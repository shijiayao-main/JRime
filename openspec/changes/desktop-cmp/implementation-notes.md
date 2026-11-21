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

## 2026-11-21 · D2 第三批：桌面设置窗口 + 设置贯通

- 新增 `:desktop:settings`：共享 `:ui:settings` 的 `SettingsScreen` 跑在桌面窗口（CMP），
  后端是 `DesktopSettings`（`~/.config/jrime/settings.properties`，可用 `JRIME_SETTINGS_FILE` 覆盖）
  与 `DesktopSettingsStore`；spec 直接按 `SettingSpec` 契约 authored（外观/候选 6 项）。
- `:desktop:app` 读取同一配置文件：palette_mode/accent → `ImeSessionState.paletteMode/accent`；
  candidate_text_size/show_key_hints/show_candidate_comment → `ImeSurfaceConfig`。
- 冒烟：settings 窗口 `window up (rows=6)` 并打印默认值；写入
  `palette_mode=dark, accent=green` 后候选窗 app 打印 `settings palette=dark accent=green`，
  协议 ready 正常。
- macOS 候选窗定位结论：IMK 公开 API 无插入点坐标，自绘窗需辅助功能权限；默认系统
  `IMKCandidates`，CMP 候选窗用于 Linux X11/Windows，状态/设置窗保持共享 CMP（待主人最终裁决）。

## 2026-11-21 · D2 第四批：Linux IBus 薄壳 + Windows 契约

- `desktop/linux/`：`jrime_engine_client.py`（行协议客户端，支持异步 event 行）、
  `jrime_ibus_engine.py`（IBus engine：键映射、preedit、lookup table、commit、
  候选点击、Shift/大小写切换按键对与 Release 语义）、`jrime.xml` + `ibus-engine-jrime` +
  `install.sh` + README。协议客户端用假引擎跑 6 个单测（ready/key/select+event/rect/stop/错误路径）全绿。
  候选 UI 走 IBus 面板（Wayland 安全路径）。
- `desktop/README.md`：三壳状态与协议扩展（rect / event update）契约。
- `desktop/windows/README.md`：TSF 脚手架与协议契约（in-proc DLL + 单 broker + CMP 候选窗 +
  `ITfContextView::GetTextExt` 光标矩形 + `WS_EX_NOACTIVATE` 补洞 + 安装/CI 待办）。
- 修正 Shift 切换的 Release 语义：macOS Swift `mapModifier` 区分按下/抬起（`Release+Shift_L`），
  Linux engine 同样发 press/release 对（librime ascii_composer 只在完整 tap 后切换）。
