# Spec Delta

## Purpose

常用语（Rime `custom_phrase`）的面板与管理契约：可见、可点选上屏、可新增/删除，并与引擎用户词典即时一致。

## ADDED Requirements

### Requirement: 常用语面板

输入表面 SHALL 提供「常用语」面板入口；面板 SHALL 列出当前 `custom_phrase` 条目，点选 SHALL 上屏该文本并返回键盘。

#### Scenario: 打开并点选

- **WHEN** 用户点击顶栏「语」
- **THEN** 显示常用语面板与当前条目列表
- **AND** 点击任一条目后该文本上屏、面板关闭

### Requirement: 首次播种随包常用语

用户目录缺少 `custom_phrase.txt` 时，应用 SHALL 从随包 shared 目录播种到用户目录，使面板显示随包置顶词。

#### Scenario: 全新安装可见置顶词

- **WHEN** 全新安装后首次打开常用语面板
- **THEN** 面板显示随包分发的置顶词（约 115 条），而不是空列表

### Requirement: 条目增删与格式保持

面板 SHALL 支持新增（文本+编码）与删除条目；写回文件时 SHALL 保留原文件头注释与 `文本<TAB>编码<TAB>权重` 格式；坏行 SHALL NOT 被写入。

#### Scenario: 新增立即持久

- **WHEN** 用户输入文本与编码并点击「＋」
- **THEN** 条目出现在列表且写入 `custom_phrase.txt`，引擎会话重建后候选生效

#### Scenario: 删除

- **WHEN** 用户点击条目右侧「×」
- **THEN** 该条目从列表与文件中移除
