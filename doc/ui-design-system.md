# JRime UI 设计系统（整体定调）

> 2026-11-21。依据 UI 设计库的检索/规范（`ui_design`：图标切换、按钮组、分类导航等模式），
> 结合输入法的约束落地到 Compose。**核心口径：JRime 是产品主体；飞鹤快拼只是当前默认方案。**

## 1. 品牌与命名口径

| 项 | 规则 |
|---|---|
| 产品名 | **JRime**（固定拼写；中文「JRime 输入法」） |
| 方案 | 方案是**数据**：界面只显示「当前方案：<方案名>」这类值，不把方案名写进图标名/组件名/文案模板。 |
| 方案演进 | 现默认 `flypy_xhfast`（小鹤双拼/飞鹤快拼）。后续会用 AI 优化、微调并可能改名；新方案**继承其规则**但作为独立方案接入，UI 不需要改图标或品牌。 |
| 图标名 | 语义命名（`Settings`/`Clipboard`/`Phrase`…），**禁止**方案名、上游名（flypy/trime/同文）出现在图标 key 中。 |
| 文案 | 动词/名词短语，简洁；数值 chip 用短标签（简/繁、全/半、中/EN、，。/. ,）。 |

## 2. 设计 token（来自 `:ui:design`）

- 颜色：`ImePalette`（light/dark） + 强调色 `Blue/Green/Purple`；语义色 `danger`；候选高亮/注解/提示各有 token。
  品牌默认强调色为蓝（浅色 `#1A73E8` 系 / 深色 `#8AB4F8` 系）。
- 字体：`ImeTypography`：候选/键面/顶栏/注解各自 style；不出现系统默认大小混用。
- 尺寸：`ImeDimens`：键 gap、候选高度、顶栏高度、触控目标 ≥ 48dp 区域；页面 padding 16dp。
- 形状：`ImeShapes`（键/芯片/面板圆角），设置分类卡 16dp 圆角、图标底座 12dp。
- 动效：`ImeMotion`（长按 500ms、按压反馈、候选切换）；克制，不做装饰动画；功能不受动画依赖。

## 3. 图标系统

**风格**：outlined、24dp 画布、2dp 视觉笔画、圆角端点；在 18dp（顶栏/候选箭头）与 22dp（设置分类卡）
两档使用；颜色一律由 tint 控制（`accentText`/`keyText`/`primary`/`danger`），不在图标里写死颜色。

| 语义 key | 名称 | 用途 | 尺寸 |
|---|---|---|---|
| `Settings` | 设置（齿轮） | 顶栏设置入口；系统与数据分类 | 18/22 |
| `InputRules` | 输入规则（调节滑杆） | 输入分类 | 22 |
| `Keyboard` | 键盘 | 键盘分类 | 22 |
| `NumberPad` | 数字键盘 | 顶栏「123」入口 | 18 |
| `Candidates` | 候选（列表） | 候选分类 | 22 |
| `Appearance` | 外观（调色板） | 外观分类；桌面设置窗口 | 22 |
| `Clipboard` | 剪贴板 | 顶栏/面板/分类 | 18/22 |
| `Emoji` | 表情（笑脸） | 顶栏/面板 | 18 |
| `Phrase` | 常用语（书签） | 顶栏/面板 | 18 |
| `Language` | 语言（翻译） | 中英切换（备选；当前用文字 中/EN） | 18 |
| `Hide` | 收起键盘（下箭头） | 顶栏右侧 | 18 |
| `Back` | 返回 | 面板顶栏 | 18 |
| `Close` | 关闭 | 弹层/面板 | 18 |
| `Search` | 搜索 | 设置搜索 | 18 |
| `ChevronLeft/Right` | 候选/分页箭头 | 候选翻页、详情返回 | 18 |
| `More` | 更多 | 候选菜单/溢出 | 18 |
| `Pin` | 置顶 | 候选长按菜单 | 18 |
| `Delete` | 删除 | 候选菜单/编辑 | 18 |
| `Copy` | 复制 | 候选菜单/剪贴板 | 18 |
| `Code` | 编码 | 查编码 | 18 |
| `Export` | 导出备份 | 系统与数据 | 22 |
| `Restore` | 恢复备份 | 系统与数据 | 22 |
| `Diagnostics` | 诊断信息 | 系统与数据 | 22 |
| `Add` | 添加 | 词典/常用语 | 18 |
| `TextFields` | 文本 | 输入/编辑 | 18 |

规则：
1. **图标只表达动作/类别**，不表达方案/语言品牌；「中/EN」「简/繁」「全/半」「，。/. ,」是**值**，继续用文字 chip。
2. 带图标的按钮必须有 `contentDescription`（等于标签），无文字 chip 时也是。
3. 同一动作跨界面复用同一 key；新增图标先进 `ImeIcon` 枚举（语义 key + `key` 字符串），扩展模块复用。
4. 图标来源：outlined Material Symbols（Apache-2.0，开源页已列）；未来可整体替换为自有品牌图标，不改调用点。
5. 禁止 emoji 与矢量图标混用在同一组按钮（旧 `⚙📋☻` 已替换）。

## 4. 组件与布局

| 组件 | 规格 |
|---|---|
| `SurfaceTopBar` | 高 `ImeDimens.TopBarHeight`；chip 高 36dp、圆角 `ImeShapes.Key`、图标 18dp；值 chip 文字 style `topBarStyle`；右侧固定「语言/收起」。 |
| `KeyboardSurface` | 5 行内容带（数字/字母×3/功能行）；字母行左右留白按 `rowInsetUnits`；键面角标 10–11sp。 |
| `CandidateStrip` | 候选高度 40dp；翻页箭头 18dp 图标；页码 `x/y` 使用注解 style；长按菜单带 Pin/Delete/Copy/Code 图标。 |
| 面板（剪贴板/符号/常用语） | 面板顶栏「返回 + 标题」；内容沿用 4–5 行带高度；图标与工具条一致。 |
| 设置分类首页 | 6 张卡片：40dp 圆角图标底座（primary 12% 底 + 22dp 图标）、标题 titleMedium、说明 bodySmall、项数 labelSmall、右侧 chevron；页面 16dp padding、卡片间 4dp。 |
| 设置分类详情 | 顶栏「‹ 返回 + 标题 + N 项设置」；搜索框 16dp 边距；行高一致、分隔线 `outlineVariant`。 |

## 5. 与旧版差异（本次落地）

- 顶栏：删除 emoji（⚙/📋/☻），改用语义图标（Settings/Clipboard/Emoji/Phrase/NumberPad）+ 值 chip 文字；
  收起与面板返回用图标；候选翻页 `‹ ›` 改为 Chevron 图标。
- 设置：分类卡新增语义图标；图标色彩随主题/强调色。
- 未做/后续：候选长按菜单加入对应图标（Pin/Delete/Copy/Code）、键盘功能键矢量图标（返回/空格等仍用键面字符，
  属键位图例而非工具栏图标）、应用启动图标重绘、自绘品牌字体。

## 6. 扩展与实现约定

- `ImeIcon` 位于 `:ui:design`（KMP commonMain），扩展模块可直接引用；新图标只加枚举项，不改渲染代码。
- `ToolbarAction.icon` / `PanelSlot.icon` / `SettingGroup.icon` 都是 `ImeIcon?`：扩展接入时按语义选图标，
  不引入字符串图标名；文本标签仍是无障碍与回退。
- 渲染统一 `Icon(imageVector = icon.vector, tint = …)`；禁止在调用点放 emoji/自定义 Path。
- 设计检查：`ImeIconSetTest` 保证 key 唯一且方案中立；`SettingsSpecsFactoryTest` 保证每个分类有图标。
