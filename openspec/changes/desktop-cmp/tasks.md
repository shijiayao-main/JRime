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
- [~] 2.7 Linux ibus 薄壳已实现（`desktop/linux/`：协议客户端 + IBus engine + component/安装脚本；客户端 6 个单测通过）；候选窗用 IBus 面板（Wayland 安全路径）；需 Linux 会话实测
- [~] 2.8 Windows TSF 脚手架与协议契约已落 `desktop/windows/README.md`（DLL+broker+rect/event 形状、安装/签名/CI 待办）；本环境无 Windows 工具链，未编译
