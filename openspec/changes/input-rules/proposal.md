# Proposal: input-rules

## Why

M2 把输入法的「手感/候选/外观/设置」做到可用后，M3 要补的是**输入规则**本身：简繁、全角、中文标点、表情候选、联想、单字优先、全拼显示、码区提示、候选注解类型这些引擎开关此前只能靠 `/so`、`8`、硬件快捷键或背命令触达；候选注解也无法关闭。它们是飞鹤快拼日常输入最常调的一组规则，却完全没有 UI。

本变更同时为词库增强（词库浏览/备份恢复增强、常用语）打底：输入规则走「运行时选项」通道，词库走「custom_phrase/用户词典」通道，二者都不依赖 YAML 用户面。

## What Changes

- 设置中心「输入」组新增 **输入规则**行（来自引擎 schema switches）：
  简繁字型（简/繁/港繁/台繁）、全角字符、使用英文标点、表情候选、联想输入、单字优先、
  预编辑显示全拼、显示码区提示、增广字集、候选注解类型（辅码/声调/空白）。
- 这些行通过 `RuntimeOptionSource` 直接读写当前 Rime 会话的运行时选项；**无会话时显示为不可用**，不写任何配置文件。
- 新增 `show_candidate_comment` 偏好（默认开）并接入候选条渲染与设置中心外观组。
- 引擎选项行与偏好行统一由 `:ui:settings` 渲染（新增 `ToggleOption`/`OptionChoice` 控件）。

**不做**：自定义中英切换手势（下一批）；模糊音/简拼的独立配置页（引擎侧为 8/Ctrl+q 触发，非开关）；词库逐词编辑（JNI 无逐词 API，下一批评估导出-过滤-导入方案）。

## Capabilities

### New Capabilities

- `input-rules`：输入规则的设置入口与引擎开关映射契约。

### Modified Capabilities

（无。）

## Impact

- `:ui:settings`：新增两种 option 控件与 `RuntimeOptionSource` 异步接缝；设置屏幕支持异步加载的 option 行。
- app：`RimeRuntimeOptionSource`（RimeDaemon 会话）、`InputRuleSpecs`、`AppPrefs` 新增候选注解开关、`ComposeInputView` 接入。
- 风险：运行时选项是会话级的；设置页在输入法未启动时不可用（已显式降级）。选项名与 schema 耦合，schema 变更需同步 `InputRuleSpecs`。
