# Implementation Notes — trim-jni-surface

> 2026-11-17 实施完成(除真机项 4.3)。

## 审计(任务 4.1)

- JNI 声明口径修正:全仓 Kotlin `external fun` 声明为 **47**(此前文档误记 44,以 grep 行数为准);core/Rime.kt 裁剪前 29、裁剪后 23,其他文件(opencc、键码工具等)18,合计裁剪后 41。
- 已删 6 入口(裁剪前逐个全仓 grep,越过 Rime.kt wrapper 与 RimeApi 接口层查到最终调用方,均 0 调用):`deployRimeSchemaFile`、`deployRimeConfigFile`、`syncRimeUserData`、`availableSchemata`、`enabledSchemata`、`setEnabledSchemata`;裁剪后全仓 grep 无残留引用。
- 保留面复核:`selectedSchemata` / `selectedSchemaId` / `selectSchema`(EnabledSchemaPickerDialog.kt:22/25/39,对话框经 CommonKeyboardActionListener.kt:27 可达)、`simulateKeySequence`(LiquidWindow.kt:111)、`startRimeMaintenance`(Rime.kt:132/154 部署链)、用户词典五方法(UserDictManager)。

## 验证记录(任务 1.1 / 2.1 / 2.2 / 3.1 / 4.1 / 4.2)

- `./gradlew :app:testDebugUnitTest`:BUILD SUCCESSFUL,23 suites / **175 tests / 0 failures / 0 errors / 0 skipped**(与基线持平)。
- `./harness/smoke.sh`:裁剪后 C++ 桌面编译 + 完整会话冒烟 **SMOKE TEST PASS**——JNI 按名绑定、删一侧不报编译错的兜底闸。
- `./gradlew :app:externalNativeBuildDebug`:BUILD SUCCESSFUL in 10s(NDK 增量,裁剪后 JNI 源码过编;assembleDebug 的 APK 出包未跑)。
- 死资产:`revert_commit_word.lua`、`custom_predict_rules.yaml` 已 git rm,非注释引用复查 clean。

## 遗留

- 任务 4.3(真机冒烟:部署 → 出字 → 方案对话框 → 词典导出导入)待设备验收后勾验并 archive 本 change。
