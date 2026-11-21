# 模型化扩展架构（去随意配置化）

> 2026-11-21。方向来自主人：**去掉 Rime 的「随意配置性」**，精简功能、抽取模块；
> 以后扩展按 **Kotlin model** 接入，而不是再写一套 YAML。

## 1. 问题与判据

现状的冗余几乎都来自「可任意配置」的历史：

| 历史来源 | 现状 | 冗余表现 |
|---|---|---|
| Rime/Trime YAML 键盘、主题、预设 | 已硬编码为 Kotlin（`KeyLayouts`/`TextKeyboard`/设计 token） | 但行为仍走 YAML 时代的字符串管线：`KeyActionToken` → `PresetKey` 字符串字段（send/command/select/toggle/commit/text）→ `KeyAction`（code/modifier/text/command/option/select/toggle/commit/shiftLock…）→ `CommonKeyboardActionListener` 大 `when` |
| 自定义键盘/主题/面板 | 旧 `Keyboard`/`Key`/`KeyboardWindow`/`BoardWindow`/主题 YAML 引擎残留 | 约 4.7k 行 legacy 代码仍在（键盘视图、窗口宿主、颜色表、NativeTheme、Liquid 配置等） |
| 自定义音效 | `SoundEffectManager` 仍解析用户 YAML | 应用层最后一个 YAML 解析器 |
| 偏好项膨胀 | `AppPrefs` + `SettingsSpecsFactory` 从 delegate 反射派生 | 为旧键盘/主题/YAML 服务、隐藏的 legacy 配置项 |
| 面板宿主 | 面板内容已是 Compose，但仍挂在 `BoardWindow` 视图宿主上 | 只为历史接口保留的 View/DI 生命周期 |

**判据**：应用层不再解析任何用户配置格式（YAML/脚本/任意字符串命令）；每项行为都有类型化 model；
新增能力 = 新增 model/扩展实现 + 注册，不改字符串解释器。

## 2. 目标模型

### 2.1 类型化行为（已落地第一批）

`core/engine` 的 `com.jiaoay.jrime.action`：

```kotlin
sealed interface ImeAction {
  data class Key(name: String)            // Rime 按键（字符/keysym/Modifier+key）
  data class Commit(text: String)         // 直接上屏
  data class Sequence(steps: List<ImeAction>)
  data class SwitchKeyboard(id: KeyboardId)
  data class OpenPanel(id: PanelId)
  object ClosePanel / HideKeyboard / OpenSettings / ShowImePicker / VoiceInput
  data class ToggleOption(name: String)
  data class PageCandidates(forward: Boolean)
  data class SelectCandidate(index: Int)
  data class DeleteCandidate(index: Int)
  data class TextEdit(op: TextEditOp)     // Copy/Cut/Paste/Undo/Redo/SelectAll/Clear
  data class Maintenance(op: MaintenanceOp)
  data class OpenUrl(url: String)
  data class Unsupported(token: String, reason: String) // 只允许出现在清理白名单，测试钉死
}
```

`ActionResolver` 是**过渡桥**：把现有布局里的字符串槽位解析成 `ImeAction`；测试保证**随包布局
全部槽位都有类型化动作、Unsupported 为空**（当前已达成）。下一阶段布局直接以 model 编写，桥和
`PresetKey` 字符串字段一起删除。

### 2.2 扩展契约（已落地第一批）

`com.jiaoay.jrime.extension`：

```kotlin
interface JrimeExtension {
  val id: String
  fun actions(): Map<String, ImeAction> = emptyMap()
  fun keyboards(): List<KeyboardExtension> = emptyList()
  fun panels(): List<PanelDescriptor> = emptyList()
  fun inputRules(): List<InputRuleDescriptor> = emptyMap()
}
class ExtensionRegistry(extensions: List<JrimeExtension>) // 冲突显式报错，可单测
```

应用自身将成为第一个扩展（`JrimeCoreExtension`：随包键盘/动作/面板/输入规则），第三方模块只需
实现接口并在启动时注册；**没有 YAML、没有字符串命令解释器**。

### 2.3 模块抽取目标

```
:core:engine       动作/键盘/面板/扩展 model + 后端无关状态（已有 action/extension 包）
:ui:design|ime|settings  Compose 表面（已 KMP）
:app               宿主：Rime JNI、Compose 输入视图、执行器、面板内容、设置存储
:extension:sample  示例扩展（下一个批次）：注册一个自定义键/面板/输入规则，证明 model-first 可扩展
```

清理目标：`:app` 不再有 `ime/keyboard/{Keyboard,Key,KeyCode,KeyAction,CommonKeyboardActionListener,KeyboardWindow}`、
`ime/window/{BoardWindow,BoardWindowManager}`、`ime/{clipboard,symbol,phrase}/*Window`、`data/theme/*` 中
YAML/主题时代类；面板内容迁移为 Compose state holder + `PanelSlot`。预计删除 ~5–6k 行。

## 3. YAML 处置边界

- **引擎数据**（`assets/shared`、`assets/rime/build`、schema/dict/OpenCC 配置）：保留 YAML，
  由 `precompileRimeData` 预编译，应用只当作不变的打包数据；不是扩展格式。
- **应用层**：不读取任何 YAML。唯一残留 `SoundEffectManager` 的用户 YAML 音效配置
  → 下一批删除（简化为内置音效 + 系统音量/振动）。
- 用户仍然可以按 Rime 惯例改引擎数据文件（高级用法），但 JRime 不以此作为产品扩展面，
  也不为它维护 UI/校验/兼容代码。

## 4. 路线图（按批次）

- **P1（本批，已完成）**：typed `ImeAction` + `ActionResolver` + extension 契约/registry + 覆盖测试；
  全随包布局槽位类型化（Unsupported 空）。
- **P2（已完成）**：应用执行器改用 typed action 执行（`RimeActionDispatcher`：Rime key / commit /
  panel / keyboard / text edit / maintenance），Compose 路径去掉 `CommonKeyboardActionListener` 字符串命令分支；
  模拟器验证按键/上屏/双空格/剪贴板面板/长按空格语音。
- **P3**：面板去 BoardWindow 化（Clipboard/Symbols/Phrase → Compose state holder + `PanelSlot`），
  删除 `BoardWindow*`、`KeyboardWindow`、`Keyboard`、`Key`、`InputView` 等视图层。
- **P4**：颜色/主题去 legacy（`ColorManager/ColorTable/Theme/NativeTheme/ThemePrefs/GboardColors`），
  统一 design token；删除自定义音效 YAML；清理死偏好与 `SettingsSpecsFactory` 派生冗余。
- **P5**：扩展模块示例 + 文档 + 注册入口（ServiceLoader 或显式 registry）；README/开发说明改为
  「写 Kotlin model 扩展」而不是「改 YAML」。
- 每批：单测 + assembleDebug + spotless 全绿；真机验收留最后。

## 5. 验收方式

- `ActionResolverTest`：随包布局全部槽位 → typed action，Unsupported 必须为空。
- `ExtensionRegistryTest`：合并/冲突/重复 id/重复面板/重复动作均可测。
- 后续批次：删除代码后 `grep` 确认无 YAML 读取入口（引擎数据加载除外）、无字符串命令解释器；
  UI 冒烟与既有单测继续绿。
