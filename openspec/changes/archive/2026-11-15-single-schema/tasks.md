# Tasks

> 状态（2026-11-15）：全部完成。验收记录：部署 `3 tasks ran: 3 success, 0 failure`（flypy_xhfast + flypy_radical + **flyhe_fast 底座**——实施中实测发现 flypy_fuzzy（简拼，用户 custom 中开启）与 cand_selector（单字表情上屏）构建在 flyhe 字表上，flyhe 改为经 dependencies 部署的隐藏底座、不进方案列表）；lua 零错误（tone_hint 加 nil 守卫）；中文候选/上屏与简拼候选实测可见；easy_en 独立方案/ecdict 释义/多方案管理 UI 全部移除；APK 46M→32M。候选首选顺序（如 ni+hk 首选）属码表权重配置，待主人日常使用复核。


## 1. 资产收缩

- [x] 1.1 删除独立方案与专属词典：`easy_en.schema.yaml`、`ecdict.schema.yaml`、`flyhe_fast.{schema,custom,dict}.yaml`、`cn_dicts/flyhe_chars.dict.yaml`、`flypy_reverse.{schema,dict}.yaml`、根级 `melt_eng.dict.yaml`、`ecdict.dict.yaml`。保留 `flypy_xhfast` 全家、`flypy_radical`（主方案声明依赖）、`easy_en.dict.yaml` + `en_dicts/*`（内嵌英文词库链）、`cn_dicts` 其余四个。验证：`rg` 无已删文件名残留引用（schema/lua/custom/default.custom）。
- [x] 1.2 `default.custom.yaml` 收敛单方案（schema_list 仅 flypy_xhfast，去 easy_en-ecdict 的 save_options 项）；`UserDataManager` 兜底 patch 单方案化。验证：编译 + 单测绿。

## 2. 配置与 lua 适配

- [x] 2.1 `flypy_xhfast.custom.yaml` 清理：删 `Control+Shift+E → select: easy_en` 键绑定与一切指向 ecdict / easy_en-as-schema 的 patch 键；内嵌 `easy_en/+:` 调优段保留。验证：`rg "select: easy_en|ecdict" custom` 清零。
- [x] 2.2 `lua/easy_en.lua` 去 ecdict 化：删除 ecdict comment 路径与 `Schema("easy_en")` 独立方案加载（改为当前方案配置的内嵌段或整体裁剪）；`lua/flypy_switcher.lua` 移除 `easy_en-ecdict` 开关接线；全量 `rg "easy_en-ecdict|Schema\(\"easy_en\"\)|Schema\(\"ecdict\"\)"` 清零。验证：模拟器全新部署 **2 个单元全 success**（lua 抛错会导致方案加载失败，部署冒烟即验证）。

## 3. 多方案管理 UI 移除

- [x] 3.1 删除 `ui/main/settings/schema/`（SchemaListFragment 等）与 Schemata 导航路由/主菜单入口；`RimeSchema` 运行时插桩保留。验证：编译 + 单测 + spotless 绿；设置页无 Schemata 项。

## 4. 部署与出字验收（模拟器）

- [x] 4.1 全新安装部署：`2 tasks ran: 2 success`（flypy_xhfast + flypy_radical），无 lua/schema 错误日志。验证：logcat 证据归档进本 change 目录。
- [x] 4.2 出字验证（Chrome 地址栏）：小鹤双拼键序出中文候选并上屏（如 `ni`+`hk` → 你好）；英文键序出英文候选上屏（内嵌 easy_en 生效）；F4 弹出的选单仅含方案内开关（无方案列表项即无碍——有当前方案项属 librime 内建行为，记录即可）。验证：上屏文本 + logcat CommitTextMessage 证据归档。
