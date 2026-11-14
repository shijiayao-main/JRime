# Spec Delta

## Purpose

定义应用预置的 Rime 资产集行为：内置小鹤系配置资产并经校验清单首启释放，通用方案资产不再预置，可选增强件缺失时部署与主输入链路不被阻塞。

## ADDED Requirements

### Requirement: 预置小鹤系资产集

应用 SHALL 预置小鹤系 Rime 配置资产集：小鹤系键盘主题（作默认主题）、内置方案的 schema 与词典（含英文反查词典 ecdict）、lua 扩展脚本集、opencc 词典，以及用户定制版 default.yaml 基座与四方案 default.custom.yaml 补丁。

#### Scenario: 首启释放完整内置集

- **WHEN** 应用全新安装后首次启动，共享数据目录为空
- **THEN** 内置集完整释放到共享数据目录，部署后四个内置方案均可出字

#### Scenario: 默认键盘主题为小鹤系主题

- **WHEN** 全新安装完成首启
- **THEN** 默认生效的键盘主题为内置的小鹤系主题（flyhe），而非通用主题

### Requirement: 内置集清单按依赖闭包机械生成

内置资产清单 SHALL 按方案配置声明的依赖闭包机械推导生成，覆盖 schema 的 `dependencies:`/`import` 声明、词典引用、lua 处理器/翻译器/过滤器的挂载引用及 lua 脚本间 require 传递依赖，不以手工抄写的清单为准。

#### Scenario: 声明依赖暴露为清单缺口

- **WHEN** 方案声明的词典依赖（如英文方案声明的 ecdict 反查词典、melt_eng 词典挂载）未随资产准备
- **THEN** 清单推导过程将其暴露为缺口，缺件资产不得进入构建产物

#### Scenario: lua 脚本引用进入清单

- **WHEN** 方案以 lua 处理器/翻译器/过滤器形式挂载脚本，且该脚本 require 其他脚本
- **THEN** 被挂载脚本与其 require 依赖全部进入内置清单，lua 功能不因清单缺件失效

### Requirement: 通用方案资产不预置

应用 SHALL NOT 预置通用方案资产：luna_pinyin 族、stroke 族方案、essay 语料与通用键盘主题 SHALL 从预置资产中移除。引擎运行必需的 prelude 基础件（按键绑定、标点、符号表）SHALL 保留。

#### Scenario: 全新安装无通用方案

- **WHEN** 全新安装并完成首启部署
- **THEN** 方案列表不含 luna_pinyin、stroke 等通用方案，预置资产中无 essay 语料与通用主题

#### Scenario: 引擎必需基件保留

- **WHEN** 内置集替换后执行部署与输入
- **THEN** 标点、符号、按键绑定基础配置生效，基础输入行为不受资产替换影响

### Requirement: 可选增强件缺件不阻塞

主方案引用但未随内置集预置的可选增强件（语言模型 gram、词频预测库 predict.db）缺失时，系统 SHALL 完成部署且主方案可出字；对应增强功能 SHALL 降级为禁用，不报错、不阻塞部署与主输入链路。

#### Scenario: 缺语言模型部署运行

- **WHEN** 主方案引用的语言模型文件（gram）不在数据目录
- **THEN** 部署正常完成，主方案可出字，语法模型功能禁用

#### Scenario: 缺词频预测库部署运行

- **WHEN** 主方案引用的词频预测库文件（predict.db）不在数据目录
- **THEN** 部署正常完成，主方案可出字，词频预测功能禁用

#### Scenario: 增强件补件后自动生效

- **WHEN** 缺失的增强件文件经用户目录导入到位并重新部署
- **THEN** 对应增强功能自动生效，无需重装应用

### Requirement: 预置资产随校验清单增量释放

预置资产 SHALL 附带构建期生成的校验清单；启动释放 SHALL 仅对校验不一致的文件执行拷贝，已一致的文件不重复拷贝。

#### Scenario: 资产集更新后的增量释放

- **WHEN** 内置资产集变更后的首次启动
- **THEN** 仅变更文件被释放到共享数据目录，未变更文件不重复拷贝
