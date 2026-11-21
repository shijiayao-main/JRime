# Implementation Notes — long-press-input

## 2026-11-21

- 预设：`KeyLayouts.presetKeys` 增加 `VoiceInput`（send `VOICE_ASSIST`，走既有
  `CommonKeyboardActionListener.switchToVoiceInputMethod()`）、`Newline`（commit `\n`）、
  `EnumerationComma`（commit `、`）。
- 功能行：`KeyboardSurface.ChromeSlot` 在按键没有自身 longClick 时套默认长按——
  空格（受 `ImeSurfaceConfig.spaceLongPressVoice` 控制）、回车、逗号；已有长按的键不覆盖。
- 偏好：`AppPrefs.Keyboard.SPACE_LONG_PRESS_VOICE`（默认 true）+ 中英文案 + `ComposeInputView` 注入。
- 单测：`KeyLayoutsPresetTest` 校验预设值、绑定可解析、默认板空格/回车留给功能行覆盖。
- 说明：`doc/voice-input-notes.md` 给出四条路线对比与选择理由（A：切语音 IME；B：内嵌
  SpeechRecognizer 需 RECORD_AUDIO + `<queries>` 且兼容风险高，列为备选；C/D 不采用）。
