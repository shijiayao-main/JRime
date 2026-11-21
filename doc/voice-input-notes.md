# 长按空格语音输入：可行性调研与实现

> 2026-11-21。目标：不引入麦克风权限与自绘识别 UI 的前提下，让「长按空格」能进入语音输入。

## 1. 技术路线对比

| 路线 | 做法 | 权限/依赖 | 体验 | 结论 |
|---|---|---|---|---|
| **A. 切换到系统语音输入法**（本版采用） | 复用它力已有的 `KEYCODE_VOICE_ASSIST` 语义：长按空格 → 切到「首选语音输入法」（设置里可选，如 Google 语音输入） | 无新增权限；要求设备已安装语音输入法/子类型（`InputMethodSubtype.mode == "voice"`） | 由系统语音 IME 接管录音与识别，识别结果直接进入当前输入框；返回键盘由用户或系统完成 | ✅ 采用：零权限、实现成熟、与 Rime 组合态解耦 |
| B. IME 内嵌 SpeechRecognizer | 键盘内起 `SpeechRecognizer`，自绘聆听浮层，结果经 `commitText` 上屏 | 需 `RECORD_AUDIO` 运行时权限 + API 30 起 manifest `<queries>` 声明 `RecognitionService`；识别服务可用性因设备/ROM 差异大 | 可做「键区内聆听」（社区有先例），但权限申请不能直接在 IME 窗口完成，需引导到应用内；识别服务与厂商差异大 | ⏸ 备选：体验上限高、成本与兼容风险也高，后续单独立项 |
| C. 调起 `RecognizerIntent` Activity | `startActivity` 系统语音 Activity | 无权限，但会离开 IME、上下文切换重 | 明显打断输入，且无法拿到键区尺寸 | ❌ 不采用 |
| D. 自研/云语音 SDK | 接第三方识别 | 隐私、合规、体积与网络要求 | 不必要 | ❌ 不采用 |

外部参考：
- Android `SpeechRecognizer` 文档（`RECORD_AUDIO`、API 30+ `<queries>`、主线程 `setRecognitionListener`）：<https://developer.android.com/reference/kotlin/android/speech/SpeechRecognizer>
- Trime 上游同路线实现（`KEYCODE_VOICE_ASSIST` 切换语音 IME + 首选语音 IME 偏好）：<https://github.com/osfans/trime/pull/1847>
- 反向做法（IME 内嵌聆听面板 + 权限/Activity 兜底）的社区实现：<https://github.com/metasequoiaime/msime/blob/main/platforms/android/README.md>

## 2. 本版实现（路线 A）

- 键盘数据新增预设 `VoiceInput`（`send: VOICE_ASSIST`）、`Newline`（`commit: "\n"`）、`EnumerationComma`（`commit: "、"`）。
- 功能行（chrome row）在按键自身没有长按行为时补默认长按：
  - **空格长按 → 语音输入**（`ImeSurfaceConfig.spaceLongPressVoice`，可在 键盘 设置关闭）；
  - **回车长按 → 换行**；
  - **逗号长按 → 中文顿号「、」**。
- 语音目标选择复用既有设置「首选语音输入法」（`preferred_voice_input`），实现见 `CommonKeyboardActionListener.switchToVoiceInputMethod()`：
  - 有首选且已安装 → 切到该输入法子类型；
  - 未选择 → 取系统第一个 `mode == "voice"` 的子类型；
  - 都没有 → toast 提示「未安装语音输入法」。
- 关闭开关后长按空格不再有动作（不影响空格单击/滑动光标）。

## 3. 已知限制与验收

- 需要设备装好语音输入法（多数国行/国际版可通过 Google 语音输入、讯飞/搜狗语音等提供）。模拟器通常没有语音 IME → 只会看到提示 toast。
- 切换输入法属于系统行为：若当时有未上屏编码，可能随输入法切换丢失（与手动切换输入法一致）；后续可以在切换前先提交组合态（待评估）。
- 语音识别质量/网络/隐私由所切换到的语音输入法负责，JRime 不采集麦克风数据（无 `RECORD_AUDIO` 权限）。

验收：装好语音输入法 → 任意输入框长按空格 → 切到语音 IME；返回 JRime 后输入正常；设置关闭开关后长按空格不再切换。
