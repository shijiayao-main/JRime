# Proposal

## Why

Trime 现以「通用输入法」形态分发：预置的是 luna_pinyin 全家、stroke 等通用方案资产，而用户实际使用的小鹤系配置（`/home/shijy7/Workspace/Personal/rime`，飞鹤快拼为主方案）需经 SAF 手工同步才能生效；同时配置基座的两源三源优先级未理顺——用户定制版 default.yaml 因同名整文件覆盖机制进不了生效链、未被 custom patch 覆盖的定制项静默丢失，代码兜底生成的 default.custom.yaml 会永久遮蔽用户版（`app/src/main/java/com/jiaoay/jrime/core/data/UserDataManager.kt:135-140`），「开箱即用」在配置层面落空。本 change 是六期改造路线图（`doc/refactor/roadmap.md`）的第一期：先把配置集定型为小鹤系内置集、理顺配置优先级、建立双口径性能基线，后续启动链优化（期 2）、Room 配置库（期 3）、模块化（期 4）、Compose 化（期 5/6）才有稳定的对象与可对照的基线。

## What Changes

- 预置资产整体替换：`app/src/main/assets/shared/` 移入小鹤系内置集（约 62M，含 ecdict.dict.yaml 27M——easy_en 方案的声明依赖与英文反查中文释义的核心功能件）、用户版 default.yaml（374 行定制版，作基座替换 prelude 版）、根级 schema/custom、`cn_dicts/`、`en_dicts/`、反查/部件词库、`lua/`（41 个 .lua 脚本全量，剔除仅被未启用脚本依赖的桌面平台二进制）、`opencc/`、`flyhe.trime.yaml`（默认键盘主题）。
- 内置集清单按 schema `dependencies:`/`import`/词典引用闭包机械推导，不手抄（手抄已证实易漏：melt_eng、ecdict 声明依赖等）。
- **BREAKING**：移除通用方案资产——luna_pinyin 全家（7 个符号链接）、stroke 族（2 个）、essay.txt、tongwenfeng.trime.yaml；升级安装后通用方案不再可用（自用 sideload 场景为设计内行为）。prelude 其余三件（key_bindings/punctuation/symbols）保留（引擎运行必需）。
- 配置基座两源三源落地（路线图补充决策 11）：assets 基座 = 用户版 default.yaml；`ensureDefaultCustomConfig` 写入条件从「user 缺失即写」收紧为「user 与 shared 均缺失才写」，兜底 patch 内容改为 flypy 四方案；存量 user 目录中代码生成版 default.custom.yaml 的升级清理路径显式处理。
- gram 与 predict.db 不内置（主方案引用的 huayu-v7 gram 不在仓库、predict.db 20.8M 二进制不可压）；真机验证两者缺失时主方案部署与运行不被阻塞，补件走 SAF 导入/资产包通道。
- 方案启用沿用现有方案列表的「可用 → 已启用」机制（`SchemaListFragment` 的添加/停用/排序，已核实仓库不存在「方案市场」类分发路径）：内置四方案（easy_en、flyhe_fast、flypy_xhfast、flypy_reverse）默认启用，现有添加入口即自定义方案入口，非预置方案经数据目录同步部署后经同一入口启用。
- 建立全路线图双口径性能基线：首启口径（安装后首次启动，含完整部署链）与稳态口径（部署完成后二次冷启动）分别实测部署耗时与冷启动到首字，记录进路线图附录；首启变慢是设计内现象（语料 62M 远大于原通用语料），它是期 2 的直接优化对象。

## Capabilities

### New Capabilities

- `preset-assets`: 预置 Rime 资产集的构成与首启释放——系统内置哪些资产、以何种清单校验与释放、通用资产不再预置、gram/predict.db 缺件时部署与运行不阻塞。
- `config-precedence`: 配置文件生效优先级——default.yaml 基座两源（shared 基座 + user 经 SAF 覆盖）、default.custom.yaml 三源（user > shared > 代码兜底）、代码兜底的触发条件与内容、存量代码生成版的清理。
- `schema-set`: 内置方案集与方案功能——四方案可切换出字、自定义方案入口、easy_en 英文反查中文释义链路（ecdict→reverse_lookup_filter→lua 注释）、lua 脚本功能（calculator 等）生效。

### Modified Capabilities

（无——`openspec/specs/` 当前为空，本 change 为项目首个 change。）

## Impact

- **资产与构建**：`app/src/main/assets/shared/` 内容整体替换（悬空符号链接改为真实文件，构建行为需实测）；`build-logic` DataChecksumsPlugin 对新资产集重新生成 checksums 清单；APK 体积增约 62M（词典为文本 YAML，压缩后实际增量以构建实测为准）。
- **代码**：`core/data/UserDataManager.kt`（`ensureDefaultCustomConfig` 写入条件与 patch 内容）；方案列表（`ui/main/settings/schema/`）以验证现有启用机制与新资产协同为主，无方案市场类路径的剔除工作（已核实不存在，见 design 决策 6）。
- **外部依赖**：librime submodule 当前未 checkout（assets 符号链接悬空），实施第一步必须先恢复 submodule 并确认 librime 版本对 lua 扩展（≥1.16）与 predict/octagram 模块匹配——全路线图最大外部依赖。
- **升级兼容**：user 目录已有代码生成版 default.custom.yaml 的存量用户会被 user 版继续遮蔽，升级清理路径需显式处理，不静默吞掉。
- **后续期依赖**：期 2（启动链优化）以本期的双口径基线与定型资产集为前提；期 3（Room 配置库）以本期定型的配置集为建模对象。
- **验收基建**：无 instrumented test，性能基线以 logcat 时间戳（`rime_jni.cc:248-265` → `core/Rime.kt:594-603`）+ adb 手工计时实测，需人工执行并记录。
