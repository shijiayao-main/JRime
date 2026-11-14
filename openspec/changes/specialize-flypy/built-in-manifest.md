# 内置集文件清单（任务 2.1 产出）

> 机械推导：schema dependencies / dictionary / reverse_lookup / lua 挂载 / opencc 引用 /
> dict import_tables 递归（行级状态机，兼容行尾注释）/ lua require 传递。
> 入口 = default.custom.yaml 启用的四方案 + flypy_radical（flypy_xhfast 的部件反查依赖）。
> 生成脚本基于 /home/shijy7/Workspace/Personal/rime（2026-11-14 状态）。

## 方案 schema 与 custom patch（7 项）

- `easy_en.schema.yaml` — 4K
- `flyhe_fast.custom.yaml` — 5K
- `flyhe_fast.schema.yaml` — 11K
- `flypy_radical.schema.yaml` — 1K
- `flypy_reverse.schema.yaml` — 0K
- `flypy_xhfast.custom.yaml` — 16K
- `flypy_xhfast.schema.yaml` — 16K

## 词典（import_tables 闭包）（18 项）

- `cn_dicts/flyhe_chars.dict.yaml` — 883K
- `cn_dicts/flypy_base.dict.yaml` — 24.4M
- `cn_dicts/flypy_chars.dict.yaml` — 119K
- `cn_dicts/flypy_chars_ext.dict.yaml` — 511K
- `cn_dicts/flypy_melt_eng.dict.yaml` — 8K
- `easy_en.dict.yaml` — 0K
- `ecdict.dict.yaml` — 0K
- `en_dicts/cn_en_flypy.dict.yaml` — 14K
- `en_dicts/easy_en.dict.yaml` — 4.5M
- `en_dicts/ecdict.dict.yaml` — 26.1M
- `en_dicts/en.dict.yaml` — 360K
- `en_dicts/en_custom.dict.yaml` — 3K
- `en_dicts/en_ext.dict.yaml` — 51K
- `flyhe_fast.dict.yaml` — 0K
- `flypy_radical.dict.yaml` — 1.3M
- `flypy_reverse.dict.yaml` — 973K
- `flypy_xhfast.dict.yaml` — 1K
- `melt_eng.dict.yaml` — 0K

## lua 脚本（require 闭包）（33 项）

- `lua/calculator.lua` — 12K
- `lua/cand_selector.lua` — 17K
- `lua/charset.lua` — 5K
- `lua/chinese_lunar.lua` — 20K
- `lua/chinese_number.lua` — 11K
- `lua/cn_en_custom.lua` — 6K
- `lua/cold_word_drop.lua` — 7K
- `lua/commit_history.lua` — 6K
- `lua/date_time.lua` — 18K
- `lua/easy_en.lua` — 4K
- `lua/easy_navigator.lua` — 3K
- `lua/flypy_fixed.lua` — 3K
- `lua/flypy_fuzzy.lua` — 9K
- `lua/flypy_help_menu.lua` — 3K
- `lua/flypy_switcher.lua` — 20K
- `lua/keybinder.lua` — 4K
- `lua/laTex.lua` — 5K
- `lua/lib/config_api.lua` — 6K
- `lua/lib/env_api.lua` — 6K
- `lua/lib/list.lua` — 8K
- `lua/lib/metatable.lua` — 5K
- `lua/lib/number_to_cn.lua` — 2K
- `lua/lib/rime_helper.lua` — 3K
- `lua/lib/string.lua` — 2K
- `lua/pair_punct.lua` — 8K
- `lua/pin_word.lua` — 8K
- `lua/reduce_emoji.lua` — 2K
- `lua/revert_commit_word.lua` — 2K
- `lua/select_char.lua` — 2K
- `lua/shortcut.lua` — 17K
- `lua/symbol_menu.lua` — 2K
- `lua/word_append_space.lua` — 9K
- `lua/word_autocaps.lua` — 2K

> 任务 2.2 按 tasks.md 决策**全量**内置 lua/ 目录（磁盘共 41 个 .lua），本闭包清单为引用核对用；闭包之外的全量项：lua/cold_word_records/drop_words.lua、lua/cold_word_records/hide_words.lua、lua/cold_word_records/reduce_freq_words.lua、lua/lib/json.lua、lua/lib/librime.lua、lua/lib/logger.lua、lua/pin_word_record.lua、lua/shortcut_config.lua

## opencc 资产（5 项）

- `(构建期由 librime submodule 供给) opencc/s2hk.json` — 0K
- `(构建期由 librime submodule 供给) opencc/s2t.json` — 0K
- `(构建期由 librime submodule 供给) opencc/s2tw.json` — 0K
- `(构建期由 librime submodule 供给) opencc/t2s.json` — 0K
- `opencc/emoji.json` — 0K

## 全局资产（基座/键盘主题/字体等）（9 项）

- `calendar.yaml` — 2K
- `custom_phrase.txt` — 2K
- `custom_predict_rules.yaml` — 0K
- `default.custom.yaml` — 1K
- `default.yaml` — 12K
- `flyhe.trime.yaml` — 294K
- `flypy_chord_rule.yaml` — 2K
- `fonts/iconfont.ttf` — 708K
- `symbols.custom.yaml` — 43K

**合计：60.5M**（schema 54K + 词典 59.2M + lua 237K + opencc 0K + 全局 1.0M）

## 排除项（不内置）

- squirrel.yaml / squirrel.custom.yaml（macOS 前端）
- weasel.yaml / weasel.custom.yaml（Windows 前端）
- fcitx5-theme/（fcitx5 前端主题）
- trime-schema.json / rime-yaml-schema.json（开发用 JSON Schema）
- intro.md / README.md / LICENSE / dprint.json / stylua.toml（仓库文档与工具配置）
- user_custom_predict_rule.example.yaml（示例文件）
- predict.db / zh-hans-t-essay-bgw.gram（增强件：按 design 决策不内置，用户目录导入后启用）
- lua/lib/sendKeyCode.dll / sendKeyCode.so（桌面平台二进制，按 tasks.md 2.2 剔除）
- build/ / sync/ / .git/ / *.userdb/（运行时产物）
- xhup.png / flypy_keymap.txt（已核对：无任何配置引用——`user_dict: flypy_keymap` 是运行时 userdb 名恰好同名，schema/主题中的 "xhup" 仅为方案描述文字；两者均为 fcitx 用途的说明文件，不内置）

## ⚠ 缺失引用（2 项，需人工核对）

- en_dicts/en_ext_private.dict.yaml（词典文件缺失）——**已核对**：用户仓库 `.gitignore` 排除 `*private*`（私有自造词），日常使用中该文件一直缺失且 easy_en 正常工作，librime 对缺失的 import 词典容忍（警告不阻塞）。处理：不内置，用户目录存在时经 SAF 同步生效。

## 布局适配修正（模拟器部署实测发现，2026-11-15）

- **主词典必须在 shared 根目录**：librime 的 dict compiler 只在数据根查找 `<dictionary>.dict.yaml`（schema 的 `dictionary:` 键不接受子目录路径，子目录仅 `import_tables` 条目支持）。据此将 `easy_en.dict.yaml`、`ecdict.dict.yaml` 从 `en_dicts/` 移至 shared 根；`en_dicts/` 其余 5 个词典均只被 `import_tables` 引用，保持子目录布局。fcitx 环境无此问题的原因：其 `build/` 内已有编译好的 `*.table.bin` 兜底，全新安装没有。
- 依部署实测补充内置 `ecdict.schema.yaml`（easy_en 的 `dependencies: ecdict` + 反查词典指向的完整反查方案）与 `en_dicts/en_ext_private.dict.yaml` 空骨架（easy_en 的 import 引用，用户私有自造词扩展位，用户目录同名文件优先）。
- 清单推导脚本 v3 的 `dependencies:` 解析存在缩进匹配缺陷（未识别 schema 段内的缩进列表），ecdict 依赖由模拟器部署失败暴露后补入。
