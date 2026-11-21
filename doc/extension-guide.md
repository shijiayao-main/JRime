# JRime 扩展开发指南（model-first）

JRime 没有 YAML/主题/布局配置层。要新增能力，写一个 **Kotlin 模块**实现 `JrimeExtension`，启动时注册到
`ExtensionRegistry`。所有贡献都是 typed model：动作是 `ImeAction`，键盘槽位是 `KeySlot`，工具栏是
`ExtensionToolbarAction`，没有任何字符串命令或 YAML。

参考实现：`extensions/sample/`（`SampleExtension`，含 4 行符号板、动作、工具栏与输入规则），本指南的代码都来自它。

## 1. 新建模块

```
extensions/my-extension/
  build.gradle.kts        # kotlin JVM，依赖 :core:engine
  src/main/kotlin/.../MyExtension.kt
```

```kotlin
// settings.gradle.kts
include(":extensions:my-extension")
// app/build.gradle.kts
implementation(project(":extensions:my-extension"))
```

## 2. 实现扩展

```kotlin
object MyExtension : JrimeExtension {
    override val id = "my"                 // 全局唯一、小写

    override fun actions(): Map<String, ImeAction> = mapOf(
        "MyInsertDate" to ImeAction.Commit("2026-11-21"),
    )

    override fun keyboards(): List<KeyboardExtension> = listOf(
        KeyboardExtension(
            id = KeyboardId("my.symbols"),
            columns = 10,                   // 布局单位网格
            keys = listOf(
                KeySlot(label = "★", width = 1f, click = ImeAction.Commit("★")),
                KeySlot(label = "返回", width = 4f,
                    click = ImeAction.SwitchKeyboard(KeyboardId.Qwerty26)),
            ),
        ),
    )

    override fun toolbarActions(): List<ExtensionToolbarAction> = listOf(
        ExtensionToolbarAction(
            id = "my.board", label = "我的键盘", icon = "Add",   // icon 是 ImeIcon 的语义名
            action = ImeAction.SwitchKeyboard(KeyboardId("my.symbols")),
        ),
    )

    override fun inputRules(): List<InputRuleDescriptor> = listOf(
        InputRuleDescriptor(option = "my_option", title = "我的开关", labels = listOf("关", "开")),
    )
}
```

`KeySlot.width` 以 `columns` 网格为单位（0 表示 1 格）；长按/滑动同样用 typed action：

```kotlin
KeySlot(label = "语", longPress = ImeAction.VoiceInput,
        swipeUp = ImeAction.OpenPanel(PanelId.Symbols))
```

常用 `ImeAction`：`Key` / `Commit` / `Sequence` / `SwitchKeyboard` / `OpenPanel` / `ClosePanel` /
`HideKeyboard` / `OpenSettings` / `ShowImePicker` / `VoiceInput` / `ToggleOption` / `PageCandidates` /
`SelectCandidate` / `DeleteCandidate` / `TextEdit` / `Maintenance` / `OpenUrl`。

## 3. 注册（宿主侧）

应用只在启动时做一次注册；扩展之间 id/动作 token/键盘 id/工具栏 id 冲突会在构造时直接抛错：

```kotlin
private val extensions = ExtensionRegistry(listOf(MyExtension))
private val catalog = KeyboardCatalog.shipped(extensions)
private val controller = ImeSurfaceController(..., catalog.presetKeys, extensions.resolvedActions)
private val executor = ImeCommandExecutor(..., catalog.presetKeys,
    extraActions = extensions.resolvedActions, ...)
```

扩展键盘槽位会被适配为渲染器模型；`resolvedActions` 包含声明的动作和槽位合成动作，`ActionResolver`
因此可以把任何槽位解析成 typed action。工具栏项由 `extensions.toolbarActions` 追加到顶栏，点击直接下发
`ImeCommand.Typed(action)`。

## 4. 约束

- **不写 YAML**、不改 `assets/rime/*.yaml`；随包 YAML 只是引擎数据。
- 图标用 `:ui:design` 的语义名（`ImeIcon` 枚举名，如 `Settings`/`Clipboard`/`Add`），不要用 emoji/图片路径。
- 命名保持方案中立：不要出现某个输入方案名（扩展属于产品能力，不属于方案）。
- 键盘/动作/面板 id 全局唯一；面板目前需宿主有对应渲染器，自定义面板内容渲染器是后续 API。
- 外观用设计 token（`ImePalette`/`ImeDimens`/`ImeShapes`），不要硬编码颜色/尺寸。

## 5. 测试

```kotlin
val registry = ExtensionRegistry(listOf(MyExtension))
val catalog = KeyboardCatalog.shipped(registry)
catalog.find(KeyboardId("my.symbols")).shouldNotBeNull()
val resolver = ActionResolver(catalog.presetKeys, registry.resolvedActions)
resolver.resolve(board.keys.first().click) shouldBe ImeAction.Commit("★")
```

参考：`core/engine/src/jvmTest/.../ExtensionRegistryTest.kt`、
`app/src/test/.../SampleExtensionWiringTest.kt`。
