# JRime 重构最终验收（model-first / 去配置化 / UI）

> 2026-11-21。开发侧已完成并可重复验证；**真机手感与视觉 polish 留给用户验收**（清单见文末）。

## 1. 目标逐条对照

| 目标 | 交付 | 证据 |
|---|---|---|
| 1. 移除应用层 YAML/任意配置依赖 | 自定义音效 YAML、主题/颜色 YAML 引擎全部删除；应用源码无 YAML 解析器（仅引擎数据/备份文件路径字符串） | `grep -rn "kaml\\|Yaml" app/src/main/java` 仅剩注释与路径；提交 `1be10f1`、`e4e4962` |
| 2. 收敛功能与设置、删除死代码/旧宿主 | 删除 legacy BoardWindow/KeyboardWindow/Keyboard/Key/CommonKeyboardActionListener/InputView 等约 5.3k 行；删除 19 个死偏好；设置键盘组 14→9、输入 24→21；删除主题/颜色约 3k 行 | 提交 `2c31bcd`、`149839c`、`e4e4962`；设置截图 |
| 3. model-first 扩展接口 | `ImeAction`/`ActionResolver` typed 动作；`JrimeExtension`（动作/键盘/面板/工具栏/输入规则）+ `ExtensionRegistry`；`ImeCommand.Typed`；示例 `:extensions:sample` | `core/engine/.../action`、`.../extension`；提交 `5b35258`、`09c4579` |
| 4. UI 整体设计（品牌/命名/token/图标/统一） | `doc/ui-design-system.md`；`ImePalette`/`ImeDimens`/`ImeShapes`/`ImeTypography`；`ImeIcon` 29 个语义图标；顶栏/候选/设置分类/面板/键盘功能键全部图标化；启动图标改方案中立 J monogram | 提交 `f76143a`、`1be10f1`、`4f2f128`；`doc/ui-design-system.md` |
| 5. 示例扩展与开发说明 | `extensions/sample`（4 行符号板/动作/工具栏/输入规则）；`doc/extension-guide.md`；README 链接；`SampleExtensionWiringTest` | 提交 `09c4579`；`doc/extension-guide.md` |

## 2. 架构现状

- **输入表面**：`ComposeInputView` 直接持有 Compose 表面；`ImeSurfaceController`/`ImeCommandExecutor`/`RimeActionDispatcher`
  执行 typed `ImeAction`；面板为 Compose state（`ClipboardPanelState`/`SymbolsPanelState`）。
- **无主题引擎**：外观全部来自 `:ui:design` token（跟随深浅 + 强调色），面板/键盘/设置同一套语义色。
- **无 YAML 配置面**：随包 `assets/rime/*.yaml` 只作 librime 引擎数据；应用层不解析、不写用户配置，
  唯一用户数据是引擎数据目录与备份（导出/恢复）。
- **扩展**：新增能力 = 新 Kotlin 模块实现 `JrimeExtension`，注册到 `ExtensionRegistry`；
  冲突在构造期报错；不写 YAML、不动引擎资产。
- **品牌**：JRime 是产品；飞鹤快拼只是默认方案（`flypy_xhfast`）；未来 AI 调优方案继承其规则但可独立命名，
  图标/文案均不含方案名（图标 key 测试禁止 `flypy/flyhe/...`）。

## 3. 构建与测试证据

```sh
./gradlew test :core:model:jvmTest :core:engine:jvmTest :ui:ime:desktopTest \
  :app:assembleDebug :desktop:app:compileKotlin :desktop:settings:compileKotlin \
  :extensions:sample:compileKotlin spotlessCheck --offline
# BUILD SUCCESSFUL

./gradlew :app:assembleRelease --offline
# BUILD SUCCESSFUL（未签名：仓库不含签名密钥，发布前由用户注入）
```

Release 包体（R8/shrink 后、未签名）：每 ABI 约 **56–57 MB**（arm64-v8a/v7a/x86/x86_64）；
Debug 包约 83 MB（含调试符号与未压缩资源，属预期）。

## 4. 模拟器冒烟证据

截图归档于各 `openspec/changes/*/screenshots/`：

- 顶栏图标 / 候选翻页 / 设置分类图标：`ui-icon-system`
- 面板 Compose 化（剪贴板/符号/表情）：`panel-compose`
- 中文输入/数字键盘/语言状态：`theme-engine-removal`、`panel-compose`
- 示例扩展键盘与上屏：`extension-sample`
- 功能键矢量图标 / 示例键盘 band：`ui-polish`

## 5. 真机验收清单（用户）

Android（重点）：
1. 触控手感：键位命中、长按/滑动/连发、滑光标、双空格句号。
2. 候选：分页/长按菜单（置顶/删词/复制/查编码）、注解展示、每页数量与字号。
3. 面板：剪贴板（含 chip 一键粘贴）、表情/符号、常用语增删。
4. 语言/规则：中英切换（三种方式）、简繁/全半、10 条输入规则即时生效与默认值。
5. 设置：6 分类首页/搜索/项数；外观切换深浅+强调色；备份/恢复；用户词典；诊断复制；字体导入。
6. 稳定性：连续 3 天、切 App/失焦/旋转（无横屏专属项，行为应与竖屏一致）、低内存。
7. 扩展：顶栏「+」进入示例键盘、符号上屏、返回内置键盘（验证 model-first 路径）。

桌面（KMP 目标）：
- macOS：`install-macos-ime.sh` 安装、系统候选窗、Shift 中英、快捷键直通。
- Linux（IBus）：按 `desktop` 文档在 Linux 会话验证协议与候选窗。
- Windows（TSF）：待 Windows 工具链批次实现（本仓库当前为脚手架）。

## 6. 已知限制（非本次重构引入）

- 引擎联想/语法模型 `predict.db`/`gram` 未随包内置，界面开关可能无可感效果。
- 导入字体只影响候选/预编辑，不影响键面字母。
- 候选高亮由引擎决定（Bulk 候选通常无高亮）。
- Release 签名密钥不在仓库；发布包需用户注入。
