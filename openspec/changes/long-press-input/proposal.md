# Proposal: long-press-input

## Why

用户反馈希望补全长按输入能力，并明确要求调研「长按空格语音输入」。现状：字母/数字行已有长按（数字、大写、符号），但功能行的空格/回车/逗号没有长按行为；语音入口只存在于旧键盘数据与 legacy 监听器中，Compose 键盘没有暴露。

## What Changes

- 键盘预设新增 `VoiceInput`（`VOICE_ASSIST`）、`Newline`（commit `\n`）、`EnumerationComma`（commit `、`）。
- 功能行补默认长按：空格→语音输入（可开关）、回车→换行、逗号→顿号；按键自身已有长按时不覆盖。
- 新增偏好 **长按空格语音输入**（键盘组，默认开），复用既有「首选语音输入法」选择。
- 长按空格语音走**路线 A：切换到系统语音输入法**，不申请麦克风权限、不内嵌识别；完整可行性对比与限制见 `doc/voice-input-notes.md`。
- 单元测试覆盖预设与绑定解析；更新验收手册。

**不做**：IME 内嵌 SpeechRecognizer（需 RECORD_AUDIO 与 `<queries>`，兼容/权限成本，列为后续备选）。

## Impact

- `core/engine`：预设表 + 测试。
- `ui:ime`：功能行长按覆盖（配置驱动）。
- app：偏好/监听/配置注入/文案。
- 风险：设备无语音 IME 时只提示 toast；切换 IME 可能丢失未上屏编码（系统行为，文档记录）。
