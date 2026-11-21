# Spec Delta

## Purpose

桌面内核的可构建事实源与 macOS 输入法壳的最小可用契约。

## ADDED Requirements

### Requirement: 桌面引擎是可构建的 Gradle 模块

仓库 SHALL 提供 `:desktop:engine` Gradle 模块，覆盖桌面协议源集与引擎 server 源码，产出可运行的 `engine.jar`；现有 kotlinc 脚本入口 SHALL 继续可运行。

#### Scenario: 构建 jar

- **WHEN** 运行 `./gradlew :desktop:engine:jar`
- **THEN** 构建成功并产出 `engine.jar`

#### Scenario: 协议端到端不回归

- **WHEN** 运行 `python3 desktop/test_server.py`
- **THEN** 输出 `DESKTOP SERVER TEST PASS`

### Requirement: macOS 壳支持中英切换

macOS 壳 SHALL 把单独的 Shift 点击转发给引擎作为中英切换，CapsLock 作为 commit_code；Cmd/Ctrl/Option 组合键 SHALL 直通宿主应用。

#### Scenario: Shift 切中英

- **WHEN** 在中文态单独按一下 Shift
- **THEN** 引擎切换到英文态（再按切回）

#### Scenario: 快捷键不受影响

- **WHEN** 按 Cmd+C / Ctrl+A 等组合键
- **THEN** 事件直通宿主应用，不被引擎吞掉

### Requirement: 一键安装 macOS 输入法

仓库 SHALL 提供构建并安装 `.app` 到当前用户 `~/Library/Input Methods/` 的脚本。

#### Scenario: 安装

- **WHEN** 运行 `script/install-macos-ime.sh`
- **THEN** 构建 `.app` 并复制到 `~/Library/Input Methods/JRimeIME.app`
