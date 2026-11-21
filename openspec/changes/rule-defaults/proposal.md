# Proposal: rule-defaults

## Why

用户要求「飞鹤快拼只是默认方案，具体规则看看优化下」。审查（`doc/schema-rules-notes.md`）发现随包默认值与
中文输入习惯/设置一致性有三处问题：

1. `ascii_punct reset: 1` → 首次使用默认**英文标点**，中文输入法反直觉；
2. `charset reset: 1`（flypy）/ `extended_charset reset: 1`（flyhe）→ 默认**增广字集**，候选更长、生僻字干扰；
3. 方案候选单页（flypy custom 9 / build 9）与 App 候选条默认 7 不一致，产生「引擎 9 / 界面 7」双事实。

## What Changes

- 默认标点改为**中文标点**：`ascii_punct reset: 0`（flypy_xhfast 与 flyhe_fast 的 shared 源与预编译 build）。
- 默认字集改为**通用字集**：flypy `charset reset: 0`、flyhe `extended_charset reset: 0`。
- 统一 flypy 候选单页为 **7**（shared custom 与 precompiled build；flyhe 保留其原有 5，属不同键盘方案）。
- 不改 App 端规则 UI（仍按真实 runtime option 显示）；资产 release 链每次引擎启动做 checksum diff，
  升级安装会拿到新默认（实测：清数据后首次部署，设置里「使用英文标点」「增广字集」均为关）。
- 文档：`doc/schema-rules-notes.md` 标记已落地；`doc/style-guide.md` 去掉「待改」注记；
  验收手册补充新默认说明。

## Impact

- 仅资产 schema 默认值 + 文档；无 Java/JNI/协议改动。
- 旧安装：下次启动经 DataManager.sync 更新资产后生效（未清数据的用户也可拿到；若用户自建 custom 覆盖，
  以用户文件优先）。
- 风险：习惯英文标点/增广字集的用户需在输入规则里切换一次（App 内可切，属于显式偏好）。
