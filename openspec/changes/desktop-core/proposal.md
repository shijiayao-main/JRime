# Proposal: desktop-core（D1）

## Why

手机端 M1–M4 已把输入体验做成日常可用形态；`doc/master-plan.md` D1 要把桌面线从「macOS 半成品 + 零散 kotlinc 脚本」推进到「共享内核 + 可安装壳」：桌面引擎没有 Gradle 模块、协议源集靠脚本拼目录、macOS 壳缺中英切换且只能手工构建安装。

D1 先做**可验证的结构与壳**，不做 Path B、不做 CMP 桌面 UI（D2）：

- 桌面引擎 + 协议源集落 `:desktop:engine` Gradle 模块（源码目录暂不动，脚本/Swift 打包继续消费同一批目录，先立构建事实源）；
- macOS 壳补齐 Shift 中英切换（此前 `flagsChanged` 未转发，`Shift_L: commit_code` 收不到）与一键安装脚本；
- 产物 `.app` 重新构建并安装到 `~/Library/Input Methods/`，供最终真机验收。

## What Changes

- 新增 `:desktop:engine`（Kotlin JVM）Gradle 模块，source 指向 `desktop/engine/java` + `desktop/protocol-common`，产出 `engine.jar`。
- macOS `KeyCodeMapper` 新增修饰键映射（Shift/CapsLock），`InputController` 处理 `flagsChanged`：单独 Shift 点击=引擎中英切换，CapsLock=commit_code；Cmd/Ctrl/Option 组合仍直通系统。
- 新增 `script/install-macos-ime.sh`：构建 `.app` 并安装到当前用户的 Input Methods 目录（不含直接 `open`，IME 由系统加载）。
- `.app` 重新构建（含 Swift 变更）并已拷贝安装；`desktop/test_server.py` 协议端到端仍 PASS。

**不做**：`:desktop:protocol` 物理拆分与 `:core:model` 汇合（D1b）；候选窗自绘/CMP（D2）；Windows/Linux 壳（D2/D3）；部署/配置热更新（后续）。

## Capabilities

### New Capabilities

- `desktop-core`：桌面内核的 Gradle 构建事实源与 macOS 壳的可安装、可切换中英的契约。

### Modified Capabilities

（无。）

## Impact

- 构建：新增 Gradle 模块；五处 kotlinc 入口暂不变（源码目录未移动，D1b 再收敛）。
- macOS 壳：`flagsChanged` 转发 + 修饰键映射；安装脚本。
- 风险：IME 注册/启用仍需用户在系统设置里手动确认一次；点击 Shift 切中英依赖 ascii_composer 的按下/抬起配对语义，需真机验收。
