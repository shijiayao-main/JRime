<!--
SPDX-FileCopyrightText: 2015 - 2026 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# JRime

**JRime 是一个专注日常输入的 Android Rime 输入法**：共享 Compose 键盘与界面、精简的分类设置、
用户词典工具，以及桌面端配套。它派生自 [Trime](https://github.com/osfans/trime)，通过 JNI 运行
[RIME](https://rime.im/) 引擎（`librime`）。

默认方案是 **小鹤双拼 / 飞鹤快拼**（`flypy_xhfast`）——这只是默认值，不是绑定：JRime 是通用
Rime 前端，其他方案可以像任何 Rime 应用一样导入并切换。

[English](README.md) | [繁體中文](README_tc.md)

## 功能

- **一套 Compose 界面**：键盘、顶栏、候选条、面板与设置共用同一份 Compose 代码，并已 KMP 化
  到桌面；统一的 design token / 配色层。
- **日常可用的候选**：候选分页、长按菜单（置顶/删词/复制/查编码）、小鹤韵母键面提示、
  候选单页数量/字号/注解可调。
- **输入规则放进应用**：简繁、全角、英文标点、表情候选、联想、单字优先、全拼预编辑、码区提示、
  增广字集。
- **分类设置 + 搜索**：输入/键盘/候选/外观/剪贴板/系统与数据六类，不再是一条超长列表。
- **词库工具**：浏览/搜索/加词/调频/删除用户词典；常用语面板（`custom_phrase.txt`）；
  整机用户数据备份/恢复；复制诊断信息。
- **长按输入**：字母行长按数字/大写/符号；空格长按语音输入法、回车长按换行、逗号长按顿号。
- **桌面配套（进行中）**：共享 JVM 引擎/协议、Compose 候选窗应用与设置应用、macOS IMK 壳、
  Linux IBus 引擎壳、Windows TSF 脚手架。
- **稳定性**：启动失败降级、设置崩溃修复、设置无孤儿测试、表面守护测试、引擎 golden 回归。

## 环境要求

- Android 8.0（API 26）及以上。
- 构建：JDK 20、Android SDK 37、Gradle wrapper（`./gradlew`）。引擎源码已随仓库；桌面构建另需
  宿主 librime（见 `harness/README.md`）。
- 可选语音输入：安装任意系统语音输入法（如 Google 语音输入）后可使用长按空格。

## 构建与安装

```sh
# 全仓闸门（单测 + assembleDebug + spotless）
./gradlew test :core:model:jvmTest :core:engine:jvmTest :ui:ime:desktopTest \
  :app:assembleDebug spotlessCheck

# 构建 + 安装到已连接设备并启用输入法
./script/install-debug.sh
```

release 默认未签名；配置 `SIGNING_KEY`、`KEY_STORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`
（见 `app/build.gradle.kts`）后签名。

## 自己验收

- [doc/verification-guide.md](doc/verification-guide.md)：手机/macOS/Linux 逐项验收、已知限制、
  回滚与反馈清单。
- [doc/changelog.md](doc/changelog.md)：每批交付与验证方式。
- [doc/perf-baseline.md](doc/perf-baseline.md)：APK 体积与冷启动基线。

## 文档

| 文档 | 用途 |
|---|---|
| [doc/master-plan.md](doc/master-plan.md) | 总规划（产品原则、架构、里程碑、风险） |
| [doc/verification-guide.md](doc/verification-guide.md) | 主人验收清单 |
| [doc/changelog.md](doc/changelog.md) | 交付记录与已修问题 |
| [doc/voice-input-notes.md](doc/voice-input-notes.md) | 长按空格语音的路线对比与结论 |
| [doc/perf-baseline.md](doc/perf-baseline.md) | 体积/启动基线 |
| [doc/screenshots/README.md](doc/screenshots/README.md) | 界面证据索引 |
| [openspec/changes/](openspec/changes/) | 各期 change 提案、任务与实现说明 |

桌面壳见 [desktop/](desktop/README.md)，macOS 壳见 [macos/JRimeIME/](macos/JRimeIME/README.md)。

## 许可证与致谢

JRime 以 **GPL-3.0-or-later** 授权——见 [LICENSE](LICENSE)。

- 完整第三方许可证清单：[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
  （由 `script/gen-third-party-notices.py` 生成），应用内「关于 → 开源说明」同样可查。
- 上游致谢：[Rime 社区](https://rime.im/) 的 `librime`、[OpenCC](https://github.com/BYVoid/OpenCC)，
  以及 JRime 派生自的 Android 前端 [Trime](https://github.com/osfans/trime)；Rime 词典/方案
  版权归各自作者。
- JRime 自身新增的 Compose 界面、设置、词典工具、桌面配套与修复均为本项目内容；与上游应用无隶属关系。

## 隐私

JRime 在本机运行 Rime 引擎，不收集、不上传输入内容。可选语音输入由你切换到的系统语音输入法负责
（JRime 本身不申请麦克风权限）。见 [PRIVACY.md](PRIVACY.md)。
