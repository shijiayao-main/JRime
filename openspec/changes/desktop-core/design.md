# Design: desktop-core（D1a）

## 1. Gradle 模块（不搬目录）

```kotlin
// desktop/engine/build.gradle.kts
plugins { alias(libs.plugins.kotlin.jvm) }
kotlin {
    sourceSets["main"].kotlin.srcDirs("java", "../protocol-common")
}
```

- 保持 `desktop/engine/java` 与 `desktop/protocol-common` 为唯一源码位置：`harness/smoke.sh`、
  `desktop/test_server.py`、`macos/JRimeIME/build.sh`、CI 两个 leg 仍按目录收集源码，零回归。
- `./gradlew :desktop:engine:jar` 产出 `engine.jar`，作为 D1b（协议拆分/`:core:model` 汇合）的构建事实源。
- D1b 将做依赖倒置（协议不再直接 import `Rime` stub）后再拆 `:desktop:protocol`，避免循环依赖。

## 2. macOS 修饰键

- `KeyCodeMapper.mapModifier`：keyCode 56/60→`Shift_L`，57→`Caps_Lock`。
- `InputController.handle` 处理 `.flagsChanged`：无 Cmd/Ctrl/Option 冲突时把修饰键作为 `k <repr>` 发给引擎
  （按下/抬起成对），`apply` 上屏/组合态；返回引擎 `handled`。
- 语义：单独 Shift 点击切中英（ascii_composer），Shift+字母因中间有其它键不触发切换；
  CapsLock 走 `commit_code`。Cmd/Ctrl/Option 快捷键继续直通宿主应用。

## 3. 安装

`script/install-macos-ime.sh`：`macos/JRimeIME/build.sh` → 复制 `.app` 到 `~/Library/Input Methods/`
→ `pkill` 旧实例。IME 由系统在启用输入源时加载，脚本不 `open`（LSUIElement 程序直接启动无意义）。

## 4. 验证

- `./gradlew :desktop:engine:jar` PASS；
- `python3 desktop/test_server.py` → `DESKTOP SERVER TEST PASS`；
- `macos/JRimeIME/build.sh` → `codesign OK`；`.app` 已复制安装（141MB，含 jlink runtime/engine/dylib/rime 资产）。
- 真机验收（主人）：系统设置添加输入源 → 选 JRimeIME → 打字/候选/Shift 切中英。

## 5. 风险与后续

- 每宿主应用一个 JVM 实例（IMK controller 粒度）——D3 再评估 broker。
- 部署/配置热更新缺失：改 YAML 不生效，需后续加 deploy 请求。
- 候选窗仍是系统 `IMKCandidates`（D2 换 CMP 或自绘）。
