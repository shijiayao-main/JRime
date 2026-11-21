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

## 2026-11-21 · D1b：协议依赖倒置 + `:desktop:protocol` 拆分

- 新增 `EngineDataAccess` 接缝（rawInput/context/status/commit/candidates）；
  `EngineSnapshotSampler`/`SchemaReadyProbe` 从「直接 import desktop `Rime` stub 的 object」
  改为构造注入 access 的 class；JNI 侧由 `RimeDataAccess` 实现（engine 模块）。
- 协议模块物理拆分：8 个桌面 DTO 从 `desktop/engine/java` 迁入
  `desktop/protocol-common/com/jiaoay/jrime/core/`；新增 `:desktop:protocol` JVM 模块，
  `:desktop:engine` 依赖它（源码目录仍与五处 kotlinc 入口一致，脚本零改动）。
- 验证：`desktop/test_server.py` → DESKTOP SERVER TEST PASS；
  `harness/run_cases.py verify` → 12/12 VERIFY PASS（golden 逐字节不变，协议倒置无行为变化）；
  `./gradlew :desktop:protocol:jar :desktop:engine:jar` 离线 1s 内通过。
- 未做：DTO 与 `:core:model` 汇合——桌面 `Candidates.Paged/Bulk` 等构造签名是 C++ 桥
  `FindClass` 绑定面，与 app `RimeProto` 语义不同；需要桥侧适配层 + golden 重录窗口，
  列入 D2（与候选窗/协议扩展一起做）。macOS `.app` 本轮未重建（引擎行为不变，最终验收前统一重建）。
