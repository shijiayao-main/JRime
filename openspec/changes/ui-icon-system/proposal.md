# Proposal: ui-icon-system

## Why

UI 一直是 emoji/文字混搭（⚙/📋/☻/语…），图标没有语义命名；分类设置卡片无图标。主人要求整体 UI 设计，
包括图标名称，并明确：**JRime 是主体，飞鹤快拼只是默认方案，后续 AI 调整出的方案继承它但独立**——
图标/命名不能绑定方案。

## What Changes

- 引入 **语义图标系统** `ImeIcon`（`:ui:design`，KMP commonMain）：26 个语义 key（Settings/InputRules/
  Keyboard/NumberPad/Candidates/Appearance/Clipboard/Emoji/Phrase/Language/Hide/Back/Close/Search/
  ChevronLeft/Right/More/Pin/Delete/Copy/Code/Export/Restore/Diagnostics/Add/TextFields），
  outlined 风格、24dp 画布、18/22dp 两档；key 方案中立（测试禁止 flypy/flyhe/trime/rime 等字样）。
- 落地：顶栏工具动作改图标 + 值 chip 文字（简/繁、全/半、中/EN、，。/. , 仍为文字）；面板返回/收起/
  候选翻页箭头改图标；设置分类卡加图标底座（primary 12% 底 + 22dp 图标）。
- 新增 `doc/ui-design-system.md`：品牌与命名口径（方案是数据、可被 AI 优化/改名而不改图标）、
  token、图标表（key/名称/用途/尺寸）、组件规格、与 UI 设计库模式的取舍。
- 测试：`ImeIconSetTest`（key 唯一/方案中立/核心面覆盖）；设置分类必须有图标断言；SurfaceGuard
  继续限制 dp 字面量（图标尺寸进 token）。

## Impact

- `:ui:design` 新增 `ImeIcon` + material-icons（outlined, Apache-2.0，开源页自动收录）；
  `:ui:ime`/`:ui:settings`/app/desktop 设置接线。
- debug APK 因图标库 +6MB（release R8 会裁剪未用图标，后续验证）。
- 不改行为：仍是同一批命令/面板；只是图标与命名统一（旧 emoji 去除）。

## 验证

- `ImeIconSetTest`、`SettingsSpecsFactoryTest`、SurfaceGuard、assembleDebug、spotless 全绿。
- 模拟器实拍：顶栏图标（idle）、候选翻页 chevron、设置分类图标（截图随 change 归档）。
