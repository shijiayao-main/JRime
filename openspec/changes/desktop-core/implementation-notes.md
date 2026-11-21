# Implementation Notes — desktop-core（D1a）

## 2026-11-21

- `:desktop:engine` Gradle 模块：`srcDirs("java", "../protocol-common")`，JVM 11；
  `./gradlew :desktop:engine:jar` 通过。源码目录不搬，五处 kotlinc 入口与 Swift 打包零改动。
- macOS：`KeyCodeMapper.mapModifier`（Shift/Caps）；`InputController.handle` 处理
  `.flagsChanged`，无 Cmd/Ctrl/Option 冲突时把修饰键作为 `k Shift_L`/`k Caps_Lock` 发给引擎；
  Cmd 组合仍直通。
- `script/install-macos-ime.sh`：构建 + 安装到 `~/Library/Input Methods/` + kill 旧实例；
  本机已安装 `JRimeIME.app`（141MB）。IME 由系统在启用输入源时加载，脚本不做 `open`。
- `desktop/test_server.py` → `DESKTOP SERVER TEST PASS`（协议端到端）。
- `macos/JRimeIME/build.sh` 全流程 `codesign OK`（jlink runtime + engine classes + dylib + rime 资产 + Swift）。
- D1b：协议依赖倒置与 `:desktop:protocol` 拆分、DTO 与 `:core:model` 汇合、部署热更新。
