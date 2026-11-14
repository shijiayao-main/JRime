# 主题面裁剪保留清单（任务 4.1 产出）

> 机械推导：只走引用关系，不做"有没有用"的品味判断。方法先例同
> `openspec/changes/specialize-flypy/built-in-manifest.md`（期 1 内置集闭包推导）。
> 推导对象：`app/src/main/assets/shared/flyhe.trime.yaml`（9412 行，300,932 B = 293.9 KiB，
> 实测 `wc -l` + 字节求和，PyYAML 6.0.3 解析核对）。
> 用户实际使用：flypy_xhfast（飞鹤快拼，`default.custom.yaml` schema_list），中英混输（easy_en），简拼 flypy_fuzzy 开启。
> **本清单只推导、不裁剪；4.2 待主人过目本清单后再动。**

## 1. 推导方法与依据（运行时机制，本仓库 jrime 代码实测）

### 1.1 键盘切换机制（键盘闭包的"边"）

| 边类型 | 运行时依据 | 说明 |
|---|---|---|
| EISU 切换键 | `app/src/main/java/com/jiaoay/jrime/ime/keyboard/CommonKeyboardActionListener.kt:130`（`KEYCODE_EISU -> keyboardWindow.switchKeyboard(action.select)`）；生成映射表 `app/build/generated/ksp/debug/kotlin/com/jiaoay/jrime/core/RimeKeyMapping.kt:672`（`"Eisu_toggle" -> KeyEvent.KEYCODE_EISU`，全表仅此名映射 EISU） | preset_key `send` 含 `Eisu_toggle` 且其 `select` 为具体键盘名 → 该键盘可达 |
| 动态 select | `app/src/main/java/com/jiaoay/jrime/ime/keyboard/KeyboardWindow.kt:246-283`（`evalKeyboard`：`.default` → smartMatch；`.prior/.next` → 列表顺序导航；`.last/.last_lock`；`.ascii`；空串 → 上次锁定键盘） | `.default/.prior/.next/.last/.last_lock/空` 不是静态引用：导航遍历的是**裁剪后剩余**的 `presetKeyboards` 键序（`KeyboardWindow.kt:83` 直接取 `theme.presetKeyboards.keys`），裁剪后自动适配，无需为它们保留键盘 |
| 键盘属性 | `import_preset` / `ascii_keyboard` / `landscape_keyboard`（`KeyboardWindow.kt:172-179,260-266,277-281`；诊断器 `ThemeDiagnostics.kt:374-394`） | **实测：23 个键盘全部没有这三个属性**（PyYAML 解析核对），此三类边为空 |
| 硬编码入口 | `KeyboardWindow.kt:346-350`：数字/电话/日期输入类型 → `number` 键盘；`:343,359` email/password → `.ascii`（无 ascii_keyboard 时落 last_lock，运行时状态，非静态边） | `number` 必须保留 |
| 默认键盘 | `KeyboardWindow.kt:223-244`（`smartMatchKeyboard`）：方案 id 非键盘名 → 按 alphabet 试 qwerty/qwerty_/qwerty0 → else `"default"`。实测 flypy_xhfast：schema_id `flypy_xhfast` 非键盘名（`flypy_xhfast.schema.yaml:7`）；alphabet 含 `&_-/` 非字母字符（`flypy_xhfast.schema.yaml:356`）→ 三个分支皆不中；qwerty/qwerty_/qwerty0 均不在 23 键盘名中 → **默认键盘 = `default`** | `default` 为闭包起点 |
| rime 选项通道 | `KeyboardWindow.kt:427-432`（`_keyboard_<name>` 选项切键盘）、`:434-441`（`_key_<name>` 触发任意 preset_key） | 实测 grep shared/ 全部 yaml+lua：**无任何 `_keyboard_`/`_key_` 静态引用**，此通道不引入边 |
| 悬空引用 | `KeyboardWindow.kt:172-173`：select 的键盘名查不到 → fallback `default` | `select` 大小写须与 `preset_keyboards` 键名完全一致（YAML 大小写敏感） |

注：任务描述中的 "Locked_view/Return_to_*" 在本仓库代码与主题资产中均无此字面量（全仓库 grep 无命中）；按运行时实际机制对应为：锁定键盘（`TextKeyboard.lock`，`KeyboardWindow.kt:199` 记 last_lock）与"返回上次锁定键盘"（`evalKeyboard` 空串/`.last_lock` 分支，`KeyboardWindow.kt:258,268-272`）——两者都解析为运行时状态，不构成静态键盘引用边。

### 1.2 preset_keys 引用机制

- 键盘按键 13 个行为槽位（click / double_click / lazy_double_click / long_click / swipe_up / swipe_down / swipe_left / swipe_right / composing / has_menu / paging / combo / ascii，`TextKeyboard.kt:79-91`）+ `popup` 列表（`PopupKeyboardUi.kt:198` 对每个条目 `theme.resolveAction`）中，值为 preset_keys 名的即引用（`KeyAction.kt:180`：`presetKeys[token]` 命中才按 preset 解析，未命中当文本/键名）。
- preset_key 定义本身无行为槽位（`PresetKey.kt:11-29`），不产生 preset→preset 二级引用，闭包单层收敛。
- `Keyboard_emoji/kaomoji/symbol/greek_font/clipboard` 等名字带 Keyboard_ 前缀的键**不是键盘切换键**：`send: FUNCTION, command: liquid_keyboard`（`flyhe.trime.yaml:1619-1625`），打开 liquid 符号面板，不引入键盘边。4.2 执行时勿按名字误判。

### 1.3 调色板机制

- 用户默认选中 scheme = `"default"`（`ThemePrefs.kt:26-32`，默认值 `"default"`）；无 `default` 时取第一个（`ColorSchemeResolver.kt:41`；诊断器 `ThemeDiagnostics.kt:250-259`）。
- 跟随系统日夜间时沿 `dark_scheme`/`light_scheme` 链切换（`ColorSchemeResolver.kt:38-61`）；`flyhe.trime.yaml` 的 style 段无 `color_scheme` 键（jrime 也不读，`GeneralStyle.KNOWN_KEYS` 无此项）。
- 任务口径（tasks.md 4.1）："default + dark_scheme 链" → 实测链：`default --dark--> bg_centaurea`（`flyhe.trime.yaml:594`），bg_centaurea 无 dark_scheme，链终止。

### 1.4 style 键机制

- 运行时只读 `GeneralStyle.KNOWN_KEYS`（`GeneralStyle.kt:96-157`，56 键）；之外的键被诊断器报 `UNKNOWN_STYLE_KEY`（"the runtime ignores it"，`ThemeDiagnostics.kt:170-178`）。style 段实测 49 键 → 32 个在 KNOWN_KEYS，**17 个旧生态键**。

## 2. 键盘闭包（保留 7 / 裁剪 16，原 23）

### 2.1 保留清单及引用依据

| # | 键盘 | keys 数 | 保留依据（引用链） |
|---|---|---|---|
| 1 | `default` | 36 | 默认键盘入口：`KeyboardWindow.kt:243` else 分支（§1.1 默认键盘推导） |
| 2 | `number` | 26 | 系统输入类型硬编码 `KeyboardWindow.kt:346-350`；EISU 键 `Keyboard_number`（select: number，:1539）被 default/flyhe36/basic_symbol/math_symbol/t9_symbol 引用；tool_bar 数字按钮（:1761） |
| 3 | `flyhe36` | 46 | default 引用 `Keyboard_flyhe36`（select: flyhe36，:1537） |
| 4 | `editor` | 19 | default/flyhe36 引用 `Keyboard_editor`（select: editor，:1540）；tool_bar 编辑按钮（:1705） |
| 5 | `basic_symbol` | 36 | default/flyhe36 引用 `Keyboard_basic_symbol`（select: basic_symbol，:1543） |
| 6 | `math_symbol` | 36 | basic_symbol/number 引用 `Keyboard_math_symbol`（select: math_symbol，:1542） |
| 7 | `t9_symbol` | 35 | math_symbol 引用 `Keyboard_t9_symbol`（select: t9_symbol，:1541） |

闭包迭代过程（每轮引入新键盘的边，均经 §1.1/§1.2 机制验证）：
`{default, number}` →（default 引用 Keyboard_flyhe36/Keyboard_editor/Keyboard_basic_symbol）→ `+flyhe36, editor, basic_symbol` →（basic_symbol 引用 Keyboard_math_symbol）→ `+math_symbol` →（math_symbol 引用 Keyboard_t9_symbol）→ `+t9_symbol` → 收敛。
editor/number 虽也由 tool_bar 的 action 引入（:1705/:1761），与键盘链结果一致。

### 2.2 裁剪清单（16 个，闭包外）

| 键盘 | keys 数 | 原可达路径（为何闭包外） |
|---|---|---|
| `Greek` | 43 | 仅 EISU 键 `Keyboard_Greek`（:1547）指向，该键仅被 Greek/Greek_symbol 等被裁键盘引用 |
| `Greek_symbol` | 42 | 仅 `Keyboard_Greek_symbol`（:1545），引用者均为被裁键盘 |
| `Cyrillic` | 50 | 仅 `Keyboard_Cyrillic`（:1548），引用者均为被裁键盘 |
| `Cyrillic_symbol` | 42 | 仅 `Keyboard_Cyrillic_symbol`（:1546），同上 |
| `IPA1` | 74 | 仅 `Keyboard_IPA`/`Keyboard_IPA1`（:1551-1552），引用者均为被裁键盘 |
| `IPA2` | 72 | 仅 `Keyboard_IPA2`（:1553），同上 |
| `IPA3` | 74 | 仅 `Keyboard_IPA3`（:1554），同上 |
| `IPA_symbol_1` | 42 | 仅 `Keyboard_IPA_symbol_1`（:1556），同上 |
| `IPA_symbol_2` | 42 | 仅 `Keyboard_IPA_symbol_2`（:1557），同上 |
| `Hiragana` | 28 | 仅 `Keyboard_Hiragana`（:1559；select 与键盘名大小写一致，非悬空），引用者均为被裁键盘 |
| `Katakana` | 28 | 仅 `Keyboard_Katakana`（:1560），同上 |
| `Hangul` | 43 | 仅 `Keyboard_Hangul`（:1558），同上 |
| `bopomofo` | 46 | **无任何 EISU 键指向**（24 个 EISU 键 select 无 bopomofo）；`liquid_keyboard` 段 1826 行的 `bopomofo` 是 liquid 子键盘 tag 名，与该键盘无关 |
| `t9` | 22 | 无 EISU 键指向（t9_symbol 只指 t9_symbol 自身链路；`Keyboard_letter` select: default 不指 t9） |
| `English` | 46 | 仅 `Keyboard_English`（:1538），引用者均为被裁键盘。用户中英混输走 flypy_xhfast 的 easy_en（default 键盘 `EasyEnMode`），非 English 键盘 |
| `English_ASCII` | 46 | 仅 `Keyboard_English_ASCII`（:1544），同上 |

- `.prior/.next` 导航无静态保留需求：唯一 select 为 `.next` 的 EISU 键 `Keyboard_switch`（:1485）**无任何引用者**（grep 全文仅定义行），是死键，一并裁剪；裁剪后键盘顺序导航自动限于剩余 7 个。
- 悬空引用核查：24 个 EISU 键的 select 全部命中 23 键盘名（无 missing，无大小写错配）。

## 3. 调色板闭包（保留 2 / 裁剪 16，原 18）

### 3.1 保留清单

| # | 调色板 | 依据 |
|---|---|---|
| 1 | `default` | 用户默认选中（`ThemePrefs.kt:30` 默认值 "default"；`flyhe.trime.yaml:591`，名"紫陽 / Hydrangea"） |
| 2 | `bg_centaurea` | `default` 的 dark_scheme 链目标（`flyhe.trime.yaml:594`；夜间跟随系统时 `ColorSchemeResolver.kt:59-61` 切入），自身无 dark_scheme，链终止 |

### 3.2 裁剪清单（16 个）

`bk_flyhe`、`ba_vinho`、`bb_sakura`、`bc_pumpkin`、`bd_calendula`、`be_pine`、`bf_bamboo`、`bh_hydrangea`、`bi_lavender`、`bj_wisteria`、`by_astralwelkin`、`bz_light`、`aa_sinti`、`ab_coconut`、`ac_coal`、`ad_costa`

- 均不在 default 的 dark_scheme 链上；运行时仅在用户手动选色（ColorPickerDialog / `set_color_scheme` 命令，`CommonKeyboardActionListener.kt:174,216-220`）时可达，属用户选择范围而非主题内引用。
- 已发现的既有断链（保留与否不影响运行，仅记录）：`by_astralwelkin.light_scheme: bz_white_night` 指向不存在的 scheme（18 个中无此名），诊断器会报 `MISSING_SCHEME_LINK`（`ThemeDiagnostics.kt:261-274`）；by_astralwelkin 本身在裁剪清单内。
- `bh_hydrangea` 是 `bg_centaurea` 的 **light_scheme** 目标（:1007 段），按任务口径（dark_scheme 链）列入裁剪；差异见 §7 开放问题 1。

## 4. preset_keys 闭包（保留 71 / 裁剪 144，原 215）

### 4.1 保留清单（71 个）

**A. 保留键盘引用闭包（64 个，任务规则 3 字面口径）**——default/flyhe36/number/editor/basic_symbol/math_symbol/t9_symbol 七键盘的 13 槽位 + popup 引用并集：

`Alt_space`、`BackSpace`、`BackToPreviousSyllable`、`Calc_Lua`、`Clear`、`Color_switch`、`Commit_Caret`、`Commit_Dollar`、`Commit_Dot`、`Date_Lua`、`DelCandFunc`、`Delete`、`Down`、`EasyEnMode`、`End`、`Escape`、`Flyhe_readme`、`Google`、`HideCandFunc`、`History_Lua`、`Home`、`IME_switch`、`Keyboard_basic_symbol`、`Keyboard_default`、`Keyboard_editor`、`Keyboard_emoji`、`Keyboard_flyhe36`、`Keyboard_math_symbol`、`Keyboard_number`、`Keyboard_symbol`、`Keyboard_t9_symbol`、`Left`、`Liquid_fixed_down`、`Liquid_fixed_left`、`Liquid_fixed_right`、`Liquid_fixed_tab`、`Liquid_fixed_up`、`Lunar_Lua`、`Mandarin_rl`、`Menu`、`Number_Lua`、`Page_Down`、`Page_Up`、`PinCandFunc`、`ResetCandWeight`、`Return`、`Right`、`Schema_switch`、`Settings`、`Shift_L`、`Shift_L2`、`Theme_settings`、`Up`、`clipboard_window`、`copy`、`copy_all`、`cut`、`cut_all`、`paste`、`redo`、`select_all`、`set_theme`、`space`、`undo`

（EISU 切换键 7 个全在保留集：`Keyboard_default`→.default、`Keyboard_editor`→editor、`Keyboard_flyhe36`→flyhe36、`Keyboard_number`→number、`Keyboard_basic_symbol`→basic_symbol、`Keyboard_math_symbol`→math_symbol、`Keyboard_t9_symbol`→t9_symbol；`Keyboard_emoji`/`Keyboard_symbol` 是 liquid 面板命令键，见 §1.2 注。）

**B. 不裁剪段引用补集（7 个）**——tool_bar / liquid_keyboard 段不在本次四类裁剪对象内、保留原样，其按钮/固定栏引用的 preset_keys（引用完备性并入，见 §7 开放问题 2）：

| preset_key | 引用位置 |
|---|---|
| `menu_keyboard` | tool_bar 主按钮 `flyhe.trime.yaml:1692` |
| `Hide` | tool_bar 隐藏按钮 `:1741` |
| `liquid_keyboard_emoji` | tool_bar 表情按钮 `:1753` |
| `Liquid_fixed_exit` | liquid 固定栏 `:1788` |
| `Liquid_fixed_backspace` | `:1789` |
| `Liquid_fixed_space` | `:1792` |
| `Liquid_fixed_return` | `:1793` |

（tool_bar 其余 action 引用 `Keyboard_editor`/`redo`/`undo`/`Keyboard_number`/`clipboard_window` 与 liquid 固定栏 `Liquid_fixed_left/right`，已在 A 集内。）

### 4.2 裁剪清单（144 个 = 215 − 71，完整列示供 4.2 执行）

按运行时语义分组（组间互斥，脚本集合运算复核 15+2+6+4+52+65 = 144）：

- **EISU 切换键 · 被裁键盘专属（15 个）**：`Keyboard_English`、`Keyboard_English_ASCII`、`Keyboard_Greek`、`Keyboard_Greek_symbol`、`Keyboard_Cyrillic`、`Keyboard_Cyrillic_symbol`、`Keyboard_IPA`、`Keyboard_IPA1`、`Keyboard_IPA2`、`Keyboard_IPA3`、`Keyboard_IPA_symbol_1`、`Keyboard_IPA_symbol_2`、`Keyboard_Hangul`、`Keyboard_Hiragana`、`Keyboard_Katakana`
- **EISU 死键（2 个，定义后无任何引用者，grep 全文核对）**：`Keyboard_letter`（select: default，仅定义行 :1483）、`Keyboard_switch`（select: .next，仅定义行 :1485）
- **\*_Selected 标签占位键（6 个，`send` 为空、无动作，仅作显示）**：`Keyboard_Greek_Selected`、`Keyboard_Cyrillic_Selected`、`Keyboard_IPA_Selected`、`Keyboard_Hangul_Selected`、`Keyboard_Hiragana_Selected`、`Keyboard_Katakana_Selected`——引用者均在被裁键盘内（其中 Hiragana/Katakana 两个 _Selected 无引用者）
- **liquid 面板/命令键（4 个）**：`Keyboard_kaomoji`、`Keyboard_greek_font`、`Keyboard_fancy`（引用者均为被裁键盘）、`Keyboard_clipboard`（无引用者，死键）
- **IPA 符号上屏键（52 个）**：`IPA_advanced_tongue_root`、`IPA_aspirated`、`IPA_breathy_voiced`、`IPA_centralized`、`IPA_creaky_voiced`、`IPA_dental_above`、`IPA_dental_below`、`IPA_ejective`、`IPA_exhigh`、`IPA_exlow`、`IPA_extra_short`、`IPA_falling`、`IPA_fronted`、`IPA_fronted_side`、`IPA_glottalized`、`IPA_high`、`IPA_high_rising`、`IPA_labialized`、`IPA_lateral_release`、`IPA_linguolabial`、`IPA_low`、`IPA_low_rising`、`IPA_mid`、`IPA_mid-centralized`、`IPA_mid_central_vowel_release`、`IPA_more_rounded_above`、`IPA_more_rounded_below`、`IPA_nasal_release`、`IPA_nasalized`、`IPA_nasalized_glottalized`、`IPA_no_audible_release`、`IPA_non_syllabic_above`、`IPA_non_syllabic_below`、`IPA_palatalized`、`IPA_pharyngealized`、`IPA_raised`、`IPA_raised_side`、`IPA_retracted_tongue_root`、`IPA_rhoticity`、`IPA_rising`、`IPA_rising_falling`、`IPA_syllabic_above`、`IPA_syllabic_below`、`IPA_tie_bar_above`、`IPA_tie_bar_below`、`IPA_velarized`、`IPA_velarized_or_pharyngealized`、`IPA_voiced`、`IPA_voiceless_above`、`IPA_voiceless_below`、`IPA_voiceless_dental_fricative_release`、`IPA_voiceless_velar_fricative_release`
- **其余系统/功能/上屏/网址类（65 个）**：`BRIGHTNESS_DOWN`、`BRIGHTNESS_UP`、`Baidu`、`CALCULATOR`、`CALENDAR`、`CONTACTS`、`Candidate_switch`、`Charset_switch`、`ChineseDate`、`Color_settings`、`Comment_switch`、`CommitComment`、`CommitHomepage`、`CommitRawInput`、`CommitScriptText`、`Commit_Ampersand`、`Commit_Asterisk`、`Commit_At`、`Commit_Brace_L`、`Commit_Brace_R`、`Commit_Equal`、`Commit_Hash`、`Commit_Minus`、`Commit_Percent`、`Commit_Plus`、`Commit_Slash`、`Date`、`Deploy`、`ENVELOPE`、`EXPLORER`、`F4`、`Help`、`Henkan`、`Hint_switch`、`Homepage`、`IME_last`、`IME_next`、`Info`、`Insert`、`Mode_switch`、`MoeDict`、`MUSIC`、`Nothing`、`POWER`、`Punct_switch`、`SEARCH`、`SLEEP`、`Schema_settings`、`Search`、`Share`、`Sync`、`Time`、`TrimeApp`、`TrimeCmp`、`VOICE_ASSIST`、`VOLUME_DOWN`、`VOLUME_MUTE`、`VOLUME_UP`、`WebSearch`、`Wiki`、`Zdict`、`Zenkaku_Hankaku`、`paste_clip`、`paste_text`、`share_text`

> 4.2 执行时以脚本集合运算（215 − 保留 71）为准生成本节清单的机器可读版，分组仅辅助人读。

## 5. 旧生态 style 键裁剪单（17 个，全列 + 逐个引用确认）

判定标准：不在 `GeneralStyle.KNOWN_KEYS`（`GeneralStyle.kt:96-157`）→ 运行时忽略（`ThemeDiagnostics.kt:170-178` UNKNOWN_STYLE_KEY）。任务描述"约 15 个"，实测 17 个。

| # | style 键 | 行数/字节 | 原语义 | 被保留内容引用？ |
|---|---|---|---|---|
| 1 | `horizontal` | 1 / 43 B | 旧悬浮窗水平模式 | 否 |
| 2 | `comment_on_top` | 1 / 59 B | 旧编码提示位置（新版用 `comment_position`） | 否 |
| 3 | `reset_ascii_mode` | 1 / 49 B | style 级 ascii 重置（新版为每键盘属性 `TextKeyboard.resetAsciiMode`） | 否（名字碰撞见下） |
| 4 | `candidate_use_cursor` | 1 / 46 B | 旧候选项高亮开关 | 否 |
| 5 | `proximity_correction` | 1 / 49 B | 旧触摸修正开关 | 否 |
| 6 | `keyboards` | 24 / 393 B | 旧启用键盘列表（jrime 直接用 `presetKeyboards.keys`，`KeyboardWindow.kt:83`） | 否（名字碰撞见下） |
| 7 | `locale` | 1 / 40 B | 旧 locale 声明 | 否 |
| 8 | `latin_locale` | 1 / 37 B | 旧拉丁 locale | 否 |
| 9 | `speech_opencc_config` | 2 / 50 B | 旧语音 opencc 配置 | 否 |
| 10 | `preview_font` | 2 / 82 B | 旧气泡字体（新版为 `popup_font`） | 否 |
| 11 | `text_size` | 1 / 48 B | 旧编码字号 | 否 |
| 12 | `preview_height` | 1 / 54 B | 旧气泡高度（新版 `popup_height`） | 否 |
| 13 | `preview_offset` | 1 / 78 B | 旧气泡偏移 | 否 |
| 14 | `preview_text_size` | 1 / 48 B | 旧气泡字号（新版 `popup_text_size`） | 否 |
| 15 | `vertical_correction` | 1 / 72 B | 旧触摸位置修正 | 否 |
| 16 | `layout` | 18 / 896 B | 旧悬浮窗布局 | 否 |
| 17 | `window` | 12 / 274 B | 旧悬浮窗格式串 | 否 |

**名字碰撞说明（非引用）**：程序化引用检查曾命中 `reset_ascii_mode` 与 `keyboards` 两个名字，经核对均为**同名属性碰撞**而非 style 键引用——前者来自各键盘自身的 `reset_ascii_mode:` 属性（`TextKeyboard.resetAsciiMode`，4693 等行），后者来自 `liquid_keyboard` 段的 `keyboards:` tag 列表属性（`LiquidKeyboard.kt:17`，1794 行）。二者与 style 段的同名键无引用关系，17 键全部确认可裁。
另有一行被注释的死配置 `#latin_font:`（87 B，77 行）随段清理一并计入体积，不计入 17 键数。
特例确认：preset_key `label: enter_labels`（:1437）引用的是 **`style/enter_labels`**（KNOWN_KEYS 内，保留），由 `KeyView.kt:280` 解析，不受本裁剪影响。

## 6. 预计体积（实测）

实测方法：Python 行扫描器按段内条目（缩进定界）统计行数与 UTF-8 字节；文件基线 9412 行 / 300,932 B。

| 裁剪对象 | 数量 | 行数 | 字节 |
|---|---|---|---|
| preset_keyboards（16 键盘） | 16 / 23 | 3,493 | 102,494 B |
| preset_color_schemes（16 调色板） | 16 / 18 | 731 | 32,614 B |
| preset_keys（144 真键 + 3 注释行条目） | 147 条目 / 215 | 169 | 11,219 B |
| style 旧生态（17 真键 + 1 注释行） | 18 条目 | 71 | 2,405 B |
| **合计** | | **4,464** | **148,732 B ≈ 145.2 KiB** |

- **裁剪后估算：300,932 B → 约 152,200 B（148.6 KiB），缩减 49.4%**。
- 规则 3 字面口径（preset_keys 仅保留键盘闭包 64 个、裁掉 B 集 7 键）为 11,792 B，比完整口径多裁 573 B——见开放问题 2。
- 最大单项：被裁 16 键盘占裁剪量 68.9%（102,494 B）。

**附带发现（不在任务四类对象内，供主人决定是否纳入 4.2）**：`colors:` 段（181-589 行，23,013 B）是 390 个 YAML anchor 定义库；保留的 default + bg_centaurea 两 scheme 仅引用 48 个 anchor（hydrangea_* / centaurea_* / pure_black / pure_white 系），其余 **342 个 anchor（19,831 B ≈ 19.4 KiB）在调色板裁剪后成为死配置**（dark_* / light_* 系只被被裁的 by_astralwelkin/bz_light 引用，vinho/sakura/pumpkin/… 系只被被裁调色板引用）。YAML 未引用的 anchor 对解析无影响，裁剪安全；若纳入，合计缩减 168,563 B ≈ 164.6 KiB（56.0%），裁剪后约 132 KB。

## 7. 需要主人拍板的开放问题

1. **`bh_hydrangea`（bg_centaurea 的 light_scheme 目标，46 行 / 2,110 B）**：任务口径"default + dark_scheme 链"不含它，本清单列入裁剪。但它是保留调色板 bg_centaurea 声明的日间链接：若用户手动选中 bg_centaurea（暗色系）且系统处于日间+跟随模式，运行时会沿该链接切换（`ColorSchemeResolver.kt:55-57`）；删除后回退 bg_centaurea 自身（`:57` 的 `?: selected` 兜底），不崩溃、仅少一次切换。留或裁请主人定。
2. **preset_keys B 集 7 键（menu_keyboard / Hide / liquid_keyboard_emoji / Liquid_fixed_exit / backspace / space / return，合计 573 B）**：任务规则 3 字面是"保留键盘引用闭包"，不含 tool_bar / liquid_keyboard 的引用；本清单按引用完备性并入保留（它们被不在裁剪范围的保留段引用，裁掉会使工具栏按钮与 liquid 固定栏失效）。请主人确认口径，或明确 tool_bar/liquid_fixed_bar 也在 4.2 一并裁剪。
3. **colors 段死 anchor（342 个，19,831 B）**：调色板裁剪的连带死配置，不属于任务四类对象。是否纳入 4.2 裁剪范围，请主人定（纳入则总缩减 145.2 → 164.6 KiB）。
4. **`style/keyboards` 键的跨前端兼容性**：jrime 完全忽略该键（直接用 presetKeyboards.keys），本仓库内裁剪无影响；但该主题文件若被原版 Trime 使用，原版按此列表启用键盘，裁剪后原版下键盘集会变。默认按本仓库运行时事实裁剪，请主人知悉。

## 8. 复现记录

- 解析核对：`python3 -I`（隔离模式）+ PyYAML 6.0.3；脚本逻辑 = §1 机制的直译（EISU 键识别为 `send` 含 `Eisu_toggle`；键盘闭包从 {default, number} 起沿 select/import_preset/ascii_keyboard/landscape_keyboard 边迭代至不动点；preset 闭包 = 保留键盘 13 槽位 + popup 引用并集 ∪ tool_bar/liquid 引用；调色板 = default + dark_scheme 链；style = KNOWN_KEYS 差集；体积 = 段内条目行范围字节和）。
- 脚本与输出存于 /tmp/theme_closure/（dump_structure.py / closure.py / size.py），未写入仓库（本任务仅归档本文档）。
- 已跑检查：23 键盘/18 调色板/215 preset_keys/49 style 键计数核对全部吻合任务描述；闭包一致性检查（保留键盘全部槽位引用均可解析，无 unresolved）通过；EISU select 悬空引用检查无命中。
- 未跑（超出 4.1 范围）：golden 测试数量断言更新与 `:app:testDebugUnitTest`/spotless/assembleDebug 属任务 4.2。
