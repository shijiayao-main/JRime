# Tasks

> 状态(2026-11-17):任务 1.1-4.2 已实施并验证(见 implementation-notes.md);4.3 真机项待设备验收后勾验并 archive。

## 1. Kotlin 侧删除

- [x] 1.1 `core/Rime.kt`:删除 external fun `deployRimeSchemaFile` / `deployRimeConfigFile` / `syncRimeUserData` 与 `getAvailableRimeSchemaList` / `getSelectedRimeSchemaList` / `selectRimeSchemas`,以及三个 override 实现(`availableSchemata` / `enabledSchemata` / `setEnabledSchemata`);`core/RimeApi.kt` 删除对应三条接口声明。验证:`./gradlew :app:testDebugUnitTest` 全绿(基线 175 用例)。

## 2. C++ 侧删除

- [x] 2.1 `librime_jni/rime_jni.cc`:删除 `// deployment` 段三个函数。验证:harness 桌面冒烟编译通过且 SMOKE TEST PASS。
- [x] 2.2 `librime_jni/levers.cc`:删除三个 JNI 函数及 SwitcherSettings 的 `availableSchemas` / `selectedSchemas` / `selectSchemas` 类方法。验证:同上。

## 3. 死资产删除

- [x] 3.1 删除 `assets/shared/lua/revert_commit_word.lua` 与 `assets/shared/custom_predict_rules.yaml`。验证:全仓 grep 无引用;应用构建通过。

## 4. 回归与收口

- [x] 4.1 JNI 面审计:external fun 47 → 41,零调用入口清零(逐个 grep 调用点,口径见 specs/jni-surface)。验证:审计结论记录进 implementation-notes.md。
- [x] 4.2 `./gradlew :app:externalNativeBuildDebug` 构建通过(NDK 侧编译验证,增量 10s)。
- [ ] 4.3 真机冒烟:部署 → 输入出字 → 方案选择对话框可用 → 用户词典导出/导入可用。

## Workflow follow-up

- 全部勾验后 archive 本 change(`openspec-archive-change` 流程)。
