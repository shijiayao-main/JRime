# Spec Delta

## Purpose

定义 default.yaml 与 default.custom.yaml 在共享目录与用户目录之间的生效优先级：用户定制基座开箱生效、用户目录配置优先、系统兜底生成不遮蔽用户与预置配置。

## ADDED Requirements

### Requirement: default.yaml 基座开箱生效

应用预置的 default.yaml SHALL 为用户定制版基座；全新安装后生效的 default.yaml 即为该定制版，其未被补丁覆盖的定制项 SHALL 全部生效。用户目录经同步引入同名文件时，用户目录版本 SHALL 按目录优先级整文件覆盖共享基座。

#### Scenario: 开箱基座定制项生效

- **WHEN** 全新安装完成首启，用户目录无 default.yaml
- **THEN** 生效的 default.yaml 为预置的用户定制版，未被 default.custom.yaml 补丁覆盖的定制项（如候选标注、切换器文案、中英文切换、导航键绑定、标点配置等）生效

#### Scenario: 用户目录覆盖基座

- **WHEN** 用户经同步通道将自定义 default.yaml 放入用户目录并重启部署
- **THEN** 用户目录版本整文件覆盖共享基座生效，后续基座演进走同步通道闭环

### Requirement: default.custom.yaml 三源优先级

default.custom.yaml 的生效优先级 SHALL 为：用户目录版本 > 共享目录预置版本 > 系统兜底生成版本。

#### Scenario: 预置补丁开箱生效

- **WHEN** 全新安装完成首启，用户目录无 default.custom.yaml
- **THEN** 共享目录预置的四方案补丁生效，四个内置方案出现在方案切换器中

#### Scenario: 用户补丁优先于预置补丁

- **WHEN** 用户目录存在经同步或手工修改的 default.custom.yaml
- **THEN** 用户目录版本优先于共享目录预置版本生效，预置版本不遮蔽用户版本

### Requirement: 兜底补丁仅在双缺失时生成

系统 SHALL 仅在用户目录与共享目录均无 default.custom.yaml 时，才在用户目录生成兜底补丁；正常路径 SHALL NOT 向用户目录写入系统生成的补丁文件。兜底补丁内容 SHALL 为内置四方案。

#### Scenario: 正常路径不生成兜底文件

- **WHEN** 共享目录已预置 default.custom.yaml 而用户目录没有
- **THEN** 系统不在用户目录生成兜底文件，预置版本生效

#### Scenario: 双缺失时兜底可用

- **WHEN** 用户目录与共享目录均无 default.custom.yaml
- **THEN** 系统在用户目录生成四方案兜底补丁，输入法完成部署可用

### Requirement: 升级不静默遮蔽预置补丁

从旧版本升级且用户目录存在旧版系统生成的 default.custom.yaml 时，系统 SHALL NOT 在无提示的情况下让该文件持续遮蔽预置补丁；SHALL 检测系统生成文件的特征并处理（清理或明确告知用户），使预置四方案补丁或用户主动配置生效。

#### Scenario: 旧版兜底版被识别处理

- **WHEN** 用户从预置旧版兜底补丁（通用双方案）的版本升级到本版本
- **THEN** 用户目录中旧系统生成的 default.custom.yaml 被检测并处理（清理或明确告知），预置四方案补丁不再被静默遮蔽
