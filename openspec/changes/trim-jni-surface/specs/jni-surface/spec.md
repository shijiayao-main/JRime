# Spec Delta

## Purpose

定义 JNI 面的构成不变量:入口只随真实调用方存在;方案选择、部署、用户词典三条保留链路的行为边界;防回潮的审计口径。

## ADDED Requirements

### Requirement: JNI 入口只随真实调用方存在

librime_jni 的每个 JNI 入口 SHALL 在 Kotlin 侧有声明且存在至少一条真实产品调用链(接口方法 → 调用方);零调用入口 SHALL NOT 保留。审计口径:对照 core/Rime.kt 的 external fun 清单逐个查调用点(越过 wrapper 层查到接口方法的最终调用方),零调用即裁。

#### Scenario: 审计无零调用入口

- **WHEN** 对 JNI 面执行调用点审计
- **THEN** 每个 external fun 都能给出至少一条产品调用链,无仅声明无调用的入口

### Requirement: 方案选择入口保留三方法

方案选择对话框(EnabledSchemaPickerDialog)依赖的 selectedSchemata / selectedSchemaId / selectSchema SHALL 保留可用;availableSchemata / enabledSchemata / setEnabledSchemata 等无调用方的方案管理入口 SHALL NOT 存在。

#### Scenario: 方案选择对话框可用

- **WHEN** 用户从键盘入口打开方案选择对话框并切换方案
- **THEN** 切换生效且输入随新方案出字

### Requirement: 部署链唯一入口

引擎部署 SHALL 仅经 startupRime(免部署拉起)+ startRimeMaintenance(显式部署)驱动;逐文件部署(deployRimeSchemaFile / deployRimeConfigFile)与用户数据全量同步(syncRimeUserData)入口 SHALL NOT 存在——字典离线预编译进 assets,用户目录同步走 git/WorkManager 通道。

#### Scenario: 部署后可出字

- **WHEN** 应用冷启完成部署并输入
- **THEN** 候选与上屏正常,部署不因逐文件部署入口缺失受影响

### Requirement: 用户词典管理入口保留

用户词典的列出 / 备份 / 恢复 / 导出 / 导入五入口 SHALL 经 UserDictManager 保留可用。

#### Scenario: 用户词典导出导入

- **WHEN** 用户执行词典导出后再导入
- **THEN** 用户词在导入后仍生效
