# Implementation Notes — feature-simplify

## 2026-11-21

- 删除：`data/soundeffect/{SoundEffect,SoundEffectManager}.kt`、`ui/main/settings/SoundEffectPickerDialog.kt`；
  `AppPrefs.Keyboard` 的 `USE_CUSTOM_SOUND_EFFECT`/`CUSTOM_SOUND_EFFECT` 与对应偏好；
  `InputFeedbackManager` 的 `sounds` SoundPool、`cacheSoundId`/`prepareSoundPool`/`startInput`/
  `reloadSoundEffects`/`querySoundIndex` 与自定义播放分支；legacy listener 的音效弹窗与 `"sound"` 命令分支；
  设置中心 `ACTION_PICK_SOUND` 与字符串（三语言）。
- 保留：`sound_on_keypress`/`sound_volume` 与 `AudioManager.playSoundEffect` 内置音；振动/TTS 不动。
- 候选长按菜单：`MenuItem(label, icon, color)`，使用 `ImeIcon.Pin/Delete/Copy/Code`，图标 18dp + 文案。
- 结果：应用层不再解析任何 YAML；`grep` 无 `SoundEffect` 残留（除公开文档说明）。
- 验证：`:app:compileDebugKotlin`、`:ui:ime:desktopTest`、spotless 绿（完整闸门下一批统一跑）。

## 追加：死偏好清理（P4.2）

- 删除已无生产消费者的偏好：`Keyboard.landscapeMode/splitSpacePercent/expandKeypressArea/
  swipeTravel/longPressTimeout/repeatInterval/hookShiftSpace/hookShiftNum/hookShiftSymbol/
  hideInputBar/hideKeySymbol/hideKeyHint`，`Advanced.showAppIcon`，连同常量/枚举/字符串引用；
  `recreateInputViewPrefs` 只保留 `ignoreSystemGestureInsets`；`SettingsSpecsFactory` 移除 legacy-hidden 集合。
- 结果：设置分类行数收敛（键盘 14→9、输入 24→21、外观 5→4 等）；运行时行为不变（这些配置已不再被
  任何代码读取）。模拟器打开应用/设置正常，截图归档。
