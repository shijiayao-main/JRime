# Spec Delta

## Purpose

发布前稳定性：引擎启动失败可恢复、用户数据可整机备份恢复、问题可诊断、安装可一键复现。

## ADDED Requirements

### Requirement: 启动失败可恢复

引擎启动抛出异常时，应用 SHALL NOT 崩溃或挂起就绪等待；SHALL 记录失败原因并在主界面给出可重试的失败提示；重试 SHALL 能让引擎重新启动。

#### Scenario: 启动失败不挂起

- **WHEN** 引擎启动阶段抛出异常
- **THEN** 应用继续运行，主界面在等待超时后显示「输入引擎启动失败」提示
- **AND** 设置页/主界面不会永久停在就绪等待

#### Scenario: 重试

- **WHEN** 用户在失败提示上点击「重试」
- **THEN** 引擎重新启动；成功则提示消失，失败则提示再次出现

### Requirement: 整机备份与恢复

设置 SHALL 提供用户数据整机备份与恢复：备份为 zip，包含常用语、用户自定义 patch 与各用户词典的文本导出；恢复 SHALL 合并回用户数据并重启引擎。

#### Scenario: 导出备份

- **WHEN** 用户选择「导出用户数据备份」并选定保存位置
- **THEN** 生成 zip，包含 `custom_phrase.txt`、`default.custom.yaml`（存在时）与 `userdict/*.txt`

#### Scenario: 恢复备份

- **WHEN** 用户选择备份 zip 并确认恢复
- **THEN** 各词典条目合并进用户数据，引擎重启，提示恢复的词典数量

### Requirement: 诊断可复制

设置 SHALL 提供「复制诊断信息」动作，复制版本、构建、设备、引擎错误与当前输入法信息到剪贴板。

#### Scenario: 复制诊断

- **WHEN** 用户点击「复制诊断信息」
- **THEN** 剪贴板获得一段含版本/设备/引擎状态的可粘贴文本

### Requirement: 一键安装

仓库 SHALL 提供把 debug 构建安装到连接设备并启用/选中 IME 的脚本。

#### Scenario: 安装脚本

- **WHEN** 开发者运行 `script/install-debug.sh`
- **THEN** 构建并安装匹配设备 ABI 的 debug APK，启用并选中 JRime 输入法
