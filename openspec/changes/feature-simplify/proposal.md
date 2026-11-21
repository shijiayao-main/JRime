# Proposal: feature-simplify（P4 第一批）

## Why

目标要求「功能精简、去掉为自定义而生的冗余」。自定义按键音效是典型的 YAML 自定义面：
`SoundEffectManager` 解析用户目录 `soundeffect/*sound.yaml`，配套偏好、选择弹窗、SoundPool 缓存与
加载逻辑——全部只为「用户自带音效包」服务，且是应用层最后一个 YAML 解析器。

## What Changes

- 删除自定义音效特性：`SoundEffect`/`SoundEffectManager`/`SoundEffectPickerDialog`、两个偏好
  （`custom_sound_effect_enabled`/`custom_sound_effect_name`）、设置中心「选择音效」动作、legacy listener
  的音效弹窗分支、`InputFeedbackManager` 的 SoundPool 缓存/播放路径；保留内置系统按键音
  （`AudioManager.playSoundEffect`）与振动/音量偏好。
- 候选长按菜单补图标（置顶/删词/复制/查编码）——UI 图标系统继续收口。
- 文档：验收手册注明「自定义音效已移除，仅内置按键音」；变更记录/路线图更新。

## 影响

- 应用层不再读取任何 YAML（`SoundEffectManager` 是最后一个）；随包 Rime YAML 仅作引擎数据。
- 删除约 400 行音效自定义代码与相关资源字符串；行为更可预测。
- 不再有音效选择入口/自定义音效包；需要使用系统或第三方音效的用户可在系统层处理。
