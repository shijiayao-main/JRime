# Design: input-rules

## 1. 运行时选项 vs 偏好

Rime 的 `switches` 是**引擎会话状态**（`getRuntimeOption`/`setRuntimeOption`），不是应用偏好；因此：

- `SettingsStore` 仍负责持久偏好；新增的 `RuntimeOptionSource` 只负责会话选项：
  `suspend fun get(option): Boolean?`、`suspend fun set(option, value)`。
- 无会话（输入法未启动）时 `get` 返回 null，行渲染为不可用，不写任何文件。
- 选项名来自 `flypy_xhfast.schema.yaml` 的 `switches` 与 `flypy_xhfast.custom.yaml` 增加的 switches。

## 2. 控件

```kotlin
sealed interface SettingControl {
    ...
    data class ToggleOption(val option: String) : SettingControl
    data class OptionChoice(val optionNames: List<String>, val optionLabels: List<String>) : SettingControl
}
```

- `ToggleOption`：读单个 option，开关写回。
- `OptionChoice`：互斥组（简繁四选一、注解三选一）；读取时找第一个为 true 的成员，写入时选中项 true、其余 false；全为 false 时显示第一项作为默认。
- 行内用 `LaunchedEffect` 异步加载、`rememberCoroutineScope` 异步写入；加载失败/无会话显示「—」。

## 3. 行数据

`InputRuleSpecs.build(labeler, groupTitle)` 生成 10 行，插到设置中心「输入」组最前面；后续行为偏好行（预编辑模式、双空格、Ctrl/Shift hook 等）。

## 4. 候选注解开关

`show_candidate_comment`（默认 true）→ `ImeSurfaceConfig.showCandidateComment` → `CandidateStripView` 已有读取点；设置中心外观组自动出现（无孤儿测试覆盖）。

## 5. 测试

- 已有 no-orphan 测试继续保证新偏好有行（自动派生）。
- UI 模块编译期保证控件穷尽（`when` 无 else 分支需覆盖新类型）。
- 模拟器冒烟：输入组渲染 10 条规则、可读会话值（有会话时显示 ON/OFF，简繁默认「简体」）、候选注解开关存在。
- 真机/后续：切换规则后输入行为变化（最终用户验收）。

## 6. 风险

- 选项名和 schema 绑定：schema 换名时 `InputRuleSpecs` 需同步；行上不体现错误，最终以真机行为验收。
- 会话选项的生命周期：输入法重启后恢复 schema 默认/上次 schema 持久化状态，设置页显示以当前会话为准。
