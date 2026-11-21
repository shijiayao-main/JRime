## 2026-11-21

- 新模块 `:extensions:sample`（Kotlin JVM，依赖 `:core:engine`），`SampleExtension` 示例：
  - actions：`SampleInsertSignature`、`SampleOpenSymbols`；
  - keyboard：`sample.symbols`（10 列符号板，含「签名 / 返回键盘 / 符号面板」）；
  - toolbar：`sample.board`（图标 `Add`，SwitchKeyboard）；
  - inputRules：`sample_wide_punct`（descriptor；宿主展示后续接入）。
- 扩展 API：`JrimeExtension.toolbarActions()`、`ExtensionToolbarAction`；`ImeCommand.Typed(ImeAction)` +
  reducer 对 SwitchKeyboard/OpenPanel/ClosePanel 的状态归约。
- Registry：`resolvedActions`（声明动作 + 键盘槽位合成 token）、`keyboardBoards`（typed→TextKeyboard）、
  `toolbarActions`（重复 id 报错）；`KeyboardCatalog.shipped(extensions)` 合并并拒绝与内置键盘冲突。
- 宿主：`ComposeInputView` 注册 SampleExtension；controller/executor 带 `extraActions`；工具栏 append 扩展入口；
  `ImeCommand.Typed` 在 executor 直接交给 `RimeActionDispatcher`。
- 测试：`ExtensionRegistryTest` 增加键盘/工具栏/冲突；`SampleExtensionWiringTest` 覆盖扩展→catalog→resolver；
  模拟器验证示例键盘切换与符号上屏（截图归档）。
- 说明：扩展面板目前只注册 descriptor（宿主按 PanelId 渲染内置内容）；自定义面板内容渲染器是后续工作。
