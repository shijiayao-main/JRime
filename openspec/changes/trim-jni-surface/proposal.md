# Proposal

## Why

JNI 面自 Trime 原样继承(47 个 external fun),其中 6 个入口在全仓为零调用:三个 on-device 逐文件部署/全量同步入口(deployRimeSchemaFile / deployRimeConfigFile / syncRimeUserData——字典已离线预编译进 assets/rime/build,部署链唯一入口是 startRimeMaintenance)与三个方案启用管理入口(availableSchemata / enabledSchemata / setEnabledSchemata——方案列表 UI 实际只用 selectedSchemata / selectedSchemaId / selectSchema)。死入口不只是死代码:它们把「运行时改配置/切方案」的能力面继续暴露在 ABI 上,是 Trime「配置面过广」病灶在代码层的残留。另有两件死资产仅被注释行引用。依据:doc/trim-plan.md(2026-11-17,含逐项 file:line 与全仓调用点核查)。

## What Changes

- 删除 6 个零调用 JNI 入口的三层实现:Kotlin external fun 声明(core/Rime.kt)→ RimeApi 接口声明与 Rime 实现(availableSchemata / enabledSchemata / setEnabledSchemata)→ C++ JNI 函数(librime_jni/rime_jni.cc 的 deployment 段三函数;librime_jni/levers.cc 的三函数及仅被其调用的 SwitcherSettings.availableSchemas / selectedSchemas / selectSchemas 类方法)。
- 删除两件死资产:assets/shared/lua/revert_commit_word.lua(唯一引用是 flypy_xhfast.schema.yaml:99 注释行)、assets/shared/custom_predict_rules.yaml(唯一引用是 flypy_xhfast.custom.yaml:327 注释行)。
- 保留面不动:EnabledSchemaPickerDialog 在用的 selectedSchemata / selectedSchemaId / selectSchema、符号面板在用的 simulateKeySequence、部署链在用的 startRimeMaintenance、用户词典五方法、config 读取方法、opencc 两方法、octagram / predict native 链接(roadmap 决策 2 的补件前提)。
- 无用户可见行为变化;验证为「删后全绿」:JVM 单测(基线 175 用例)+ harness 桌面冒烟(host 编译 librime_jni.dylib 并跑通加载/部署/回调)+ NDK 构建。

## Capabilities

### New Capabilities

- `jni-surface`:JNI 面的构成不变量——入口只随真实调用方存在;方案选择、部署、用户词典三条保留链路的行为边界;防回潮的审计口径。

### Modified Capabilities

(无——删除均为零调用入口,不改变任何既有规格行为。)

## Impact

- **代码**:`core/RimeApi.kt`(-3 接口声明)、`core/Rime.kt`(-3 实现、-6 external fun)、`librime_jni/rime_jni.cc`(-3 函数)、`librime_jni/levers.cc`(-3 函数、-3 类方法)。RimeDaemon 为 `by realRime` 委托,不受接口缩面影响。
- **资产**:删除两个死文件(KB 级)。
- **风险**:JNI 按名绑定,只删一侧不报编译错(删 C++ 留 Kotlin 声明会在运行时 UnsatisfiedLinkError,反之只是死符号),必须两层同 commit,并依赖 harness 冒烟 + 单测兜底。
- **不做**:librime 核心 C++ 与 gear 组件裁剪(路线已否决,doc/trim-plan.md §5);octagram / predict 链接移除(资产包补件通道的前提)。
