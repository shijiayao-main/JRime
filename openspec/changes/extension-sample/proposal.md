# Proposal: extension-sample（P5）

## Why

model-first 扩展接口（`JrimeExtension`/`ExtensionRegistry`）此前只有接口和单测，应用没有加载任何扩展，
「后来者按 model 接入」还停留在纸面。P5 要给出**可复制的示例扩展 + 开发说明**，并让宿主真正消费扩展贡献。

## What Changes

- 新增 Kotlin JVM 模块 `:extensions:sample`：`SampleExtension` 用纯 Kotlin 注册
  - 自定义动作（typed `ImeAction`）
  - 一个符号键盘（typed `KeySlot`，无 YAML/无 token）
  - 一个工具栏入口（`ExtensionToolbarAction`，语义图标名）
  - 一条输入规则描述（`InputRuleDescriptor`）
- 扩展 API：`JrimeExtension.toolbarActions()` + `ExtensionToolbarAction`；`ImeCommand.Typed(ImeAction)`。
- 宿主适配：
  - `ExtensionRegistry` 生成键盘槽位内部 token 并暴露 `resolvedActions`；`keyboardBoards` 把 typed
    `KeyboardExtension` 适配成当前渲染器的 `TextKeyboard`。
  - `KeyboardCatalog.shipped(extensions)` 合并扩展键盘（与内置 id 冲突即报错）。
  - `ActionResolver`/controller/executor 接收扩展动作；工具栏把 `ImeCommand.Typed` 直接下发。
  - `ComposeInputView` 启动时注册 `SampleExtension`；扩展工具栏入口出现在顶栏。
- 开发说明 `doc/extension-guide.md`；README 链接；`model-first` 路线图 P5 完成。

## 验证

- 单测：registry 键盘/工具栏/冲突、`SampleExtensionWiringTest`（扩展→catalog→resolver）。
- 模拟器：顶栏「扩展示例」→ 切换示例键盘；点符号上屏；返回内置键盘。
- 全仓闸门。
