# Design

## Context

现状与约束（详见 proposal.md - Why 与事实包）：

- `app/src/main/assets/shared/` 预置的是通用方案：14 个指向未 checkout submodule（`app/data/rime/*`）的悬空符号链接 + 2 个真实文件（trime.yaml、tongwenfeng.trime.yaml）；释放通道是 `DataManager.sync()` 的 checksums diff 拷贝（`app/src/main/java/com/jiaoay/jrime/core/data/DataManager.kt:66-93`），校验清单由 `build-logic/.../DataChecksumsPlugin.kt` 构建期生成——「全量进 assets」与「首启从打包资源释放」在本仓库现有机制下是同一件事。
- `ensureDefaultCustomConfig()` 在 user 目录缺失 default.custom.yaml 时写入写死 luna_pinyin 双方案的 patch 版（`app/src/main/java/com/jiaoay/jrime/core/data/UserDataManager.kt:43-48,135-140`），而 librime 查找 user 优先于 shared，代码生成版会永久遮蔽预置版。
- librime 对非 `.custom` 同名文件按 user > shared 目录优先**整文件覆盖**；default.yaml 基座因此存在「用户定制版因同名进不了生效链」问题（用户仓库 `/home/shijy7/Workspace/Personal/rime/default.yaml` 为 374 行定制版，文件头自述须比共享目录同名文件的 config_version 大才生效）。
- 硬约束：librime 无内存挂载 API（JNI 仅传 shared/user 目录路径，`app/src/main/jni/librime_jni/rime_jni.cc:42-48`），引擎只认文件系统；无 instrumented test 基建，性能验收靠 logcat + 手工计时。
- native 侧 lua/predict/octagram 模块链接开关已核实为开（`rime_jni.cc:18-26`），但 librime submodule 未 checkout，版本匹配待实施期确认。

## Goals / Non-Goals

**Goals:**

- 预置资产整体替换为小鹤系内置集（约 62M，含 ecdict），四方案开箱可切换出字。
- 配置基座两源（default.yaml）与 custom 三源（default.custom.yaml）优先级理顺，用户定制开箱生效。
- 建立全路线图双口径性能基线（首启/稳态），成为期 2 的对照对象。

**Non-Goals:**

- 不做启动链优化（指纹短路、预置部署产物——期 2）；本期首启变慢是设计内现象，只记录不优化。
- 不做 Room / 配置元数据建模（期 3）；不动 `checksums.json` / `.sync-manifest.json` 机制。
- **不做任何 KMP 动作**（见决策 8）：不拆模块、不建 KMP 壳、不写守护测试，纯 `:app` 内改造。
- 不动键盘自绘 View、IME service、主题解析链。
- 不内置 gram 与 predict.db（见决策 2）。

## Decisions

### 决策 1：内置集全量进 assets，ecdict 内置

内置集（约 62M：`cn_dicts/` 26M + `en_dicts/` 32M 含 ecdict.dict.yaml 27M + `lua/` 532K + `opencc/` 516K + 根级 schema/custom 与 flyhe.trime.yaml 约 1M）全量进 `app/src/main/assets/shared/`，复用现有 checksums 首启释放机制，零新机制。

ecdict 内置的理由：它是 easy_en 方案的**声明依赖**（`easy_en.schema.yaml:14-15` 的 `dependencies: - ecdict`，缺件时方案部署不可用）与**核心功能件**（`:46` 挂 `reverse_lookup_filter@easy_en-ecdict`，英文反查中文释义；`lua/easy_en.lua:2`、`lua/flypy_switcher.lua` 围绕其注释开关工作）。剔除它，「四方案可切换出字 + easy_en 反查释义」验收自相矛盾。自用 sideload 无 Play 体积上限，APK 增大是最廉价可靠的分发方式；词典为文本 YAML，压缩后实际增量以构建实测为准。

**备选**（ecdict 不内置、easy_en 降级为仅主输入可用）：被否——缺声明依赖的部署行为未经验证，且反查释义正是 easy_en 的存在理由；仅当体积/部署耗时实测不可接受时启用，启用前提 = 真机验证 librime 对缺失声明依赖不崩溃。

### 决策 2：gram 与 predict.db 不内置，验证缺件行为

主方案 `flypy_xhfast.schema.yaml:129` 实际启用 `language: zh-hans-t-huayu-v7-bgw`，而用户仓库仅有 `zh-hans-t-essay-bgw.gram`（10.4M）——**方案引用的 gram 与仓库一致地缺失**，内置 essay-bgw 对主方案不生效。predict.db（20.8M 二进制，不可压）被 `flypy_xhfast.custom.yaml:317` 硬引用。

决策：维持缺件现状不内置。期 1 验证项 = 主方案缺 gram / 缺 predict.db 时的部署与运行行为（预期：octagram 检测不到语言模型即禁用语法功能、predictor 组件失效，均不阻塞部署与主输入链路；需真机确认）。验证通过后补件走 SAF 导入（用户目录）——predict.db 放入用户目录即被 `:317` 硬引用自动启用；资产包导入通道在期 4 落地（路线图决策 8）。若真机行为不符预期（阻塞部署），回退处理：从方案配置中移除对应引用或改为内置，以真机证据为准。

### 决策 3：内置集清单按依赖闭包机械生成（含 lua 引用闭包）

清单不手抄：按 schema 的 `dependencies:`/`import` 声明、词典引用、**lua 挂载引用与脚本间依赖**从配置目录机械推导。lua 引用是 `lua_processor@`/`lua_translator@`/`lua_filter@` 等 `@*name` 形式的运行时挂载，不出现在 dependencies/import——本轮实测 `flypy_xhfast.schema.yaml` 含 lua 引用 39 行、`easy_en.schema.yaml` 8 行，仅按 dependencies/import/词典引用推导会全漏 lua（而四方案出字依赖 lua）；脚本间依赖（如 `revert_commit_word.lua` 的 `require("lib/rime_helper")`）按 require 传递闭包纳入。手抄已证实易漏：`melt_eng.schema.yaml` 与 `flyhe_fast.schema.yaml:97-98` 的 melt_eng 词典挂载、easy_en 对 ecdict 的声明依赖均系本轮调查新发现。以生成的清单为准准备资产并纳入 DataChecksumsPlugin。

lua 目录搬运口径（本轮实测：顶层 28 个、递归 41 个 .lua）：**41 个 .lua 脚本全量内置，桌面平台二进制 `lua/lib/sendKeyCode.dll`（112K）与 `sendKeyCode.so`（34K）剔除**——两者仅被 `lua/revert_commit_word.lua` 硬依赖（脚本头注释自述「文件名不能变，只能是sendKeyCode.so或dll」且依赖桌面 cpath），而该脚本的挂载行在 `flypy_xhfast.schema.yaml:99` 处于注释状态、未启用；剔除二进制后该脚本维持仓库现状的未启用状态。已注释脚本本身随目录全量保留（与用户仓库口径一致，避免两边漂移），不需要按「启用闭包」反向裁剪脚本。

### 决策 4：default.yaml 基座 = 用户定制版替换 prelude 版

librime 对非 `.custom` 同名文件按 user > shared 整文件覆盖。APP_STORAGE 模式（Trime 现状默认，用户目录初始无 default.yaml）下生效的是 assets 物化的 shared 版——用户定制版直接进 assets 作基座，开箱即用户定制；EXTERNAL_SYNC 模式下用户仓库的 default.yaml 经 SAF 同步进 user 目录覆盖 shared 版，用户后续演进走同步通道生效（与该文件头注释的 config_version 语义一致）。

prelude 其余三件（key_bindings/punctuation/symbols）保留——仍被引用，引擎运行必需。

**备选**（用户定制转写为 default.custom.yaml patch）：被否——改变用户配置管理方式，用户仓库继续演进时两边漂移；（维持 prelude 版内置）：被否——APP_STORAGE 模式下定制静默丢失。

### 决策 5：`ensureDefaultCustomConfig` 写入条件收紧 + 兜底内容改四方案

写入条件从「user 缺失即写」收紧为「**user 与 shared 均缺失才写**」：正常路径不再往 user 目录写代码生成版，消除对预置版的永久遮蔽；兜底 patch 内容从 luna_pinyin 双方案改为 flypy 四方案（仅作极端兜底）。user 目录存在用户自己的 default.custom.yaml 时优先生效——这是用户主动行为，符合 librime 目录语义，不视为缺陷。

配套：恢复 submodule 后 diff 用户版 default.yaml vs prelude 版，确认引擎必需键无缺失（用户版缺键则从 prelude 回补后再内置）；基座生效判据从「未被 default.custom.yaml patch 覆盖且两版取值不同」的键中圈定——注意 `menu/page_size`、`switcher/hotkeys`、`switcher/fold_options` 恰被 custom patch 覆盖（patch 值 7、[F4, Alt+grave]、false），无论基座取值如何最终生效值相同，**无判别力，不作基座判据**；判据从 `menu/alternative_select_labels`、`switcher/caption`、`ascii_composer`、`navigator/editor`、`punctuator` 等 patch 未覆盖键中抽查。

### 决策 6：方案启用沿用现有机制 + 升级不静默遮蔽

本轮核实（评审修正）：`ui/main/settings/schema/SchemaListFragment.kt:26-33` 的现状即「可用方案（availableSchemata）→ 已启用方案（enabledSchemata）」的添加/停用/排序机制，全仓库不存在「方案市场」或通用方案描述路径（grep 零命中）——规划期所述「方案市场/通用方案描述路径剔除」无对象。因此本期方案列表的工作是**验证而非改造**：现有添加入口即自定义方案入口（非预置方案经 SAF 同步进入数据目录、部署后出现在可添加列表），资产替换后四方案默认启用、可添加列表无通用方案残留即为收敛达成；如验证中发现与通用方案相关的残留文案或路径，再按实际代码清理。

升级场景：user 目录已有旧代码生成版 default.custom.yaml 的存量用户会被 user 版继续遮蔽——显式处理（检测系统生成版特征并清理，或升级说明明确告知），不静默吞掉；具体手段见 Open Questions。

### 决策 7：双口径性能基线，先锁口径再测数值

- **首启口径** = 安装后首次启动（含完整部署链）；**稳态口径** = 部署完成后的二次冷启动。两个口径不允许混比。
- 测量：部署耗时取 logcat 中 deploy Start→Success 通知时间戳（`rime_jni.cc:248-265` → `core/Rime.kt:594-603` 路径）；冷启动到首字 adb 手工计时（无 instrumented test 基建，不虚构自动化指标）。
- **基线面 = 路线图附录表全部「期 1 待实测」行**（`doc/refactor/roadmap.md:344-351`，共 6 项）：两口径部署耗时、两口径冷启动到首字、主题列表加载文件解析次数（日志/profiler 证据）、APK 体积（构建产物实测）；另记录构建/CI 时长（ecdict 降级判据的数据来源，见决策 1 备选与风险栏）。
- 数值记录进路线图附录，后续每期同口径对照。首启部署耗时较现状显著上升是**设计内现象**（62M 语料 vs 原通用语料），它是期 2 的直接优化对象，本期如实记录。

### 决策 8：KMP 预备在本期的落点 = 零动作

按路线图决策 6 的节奏：**期 1-2 不做任何 KMP 动作**。本期是纯 `:app` 内的资产与配置路径改造，不拆模块、不建 KMP 壳（`kotlin("multiplatform")` 声明在期 4）、不引入 commonMain/androidMain 分层、不写守护测试（KMP 纪律从期 3 新写数据层才生效）。

理由：「预备」的完成标准是结构与纪律就位（可机械验证），任何提前动作在本期都没有验收路径，且会推迟专精化主线（用户痛点）的解决。本期改动的文件（`core/data/UserDataManager.kt`、方案 UI 路径）均位于期 4 规划的 androidMain 保留清单内（路线图决策 5/6），本期不新增跨面耦合，不会给后续拆分制造返工——这就是本期对 KMP 预备的全部贡献：**不添新债**。

## Risks / Trade-offs

- [首启部署耗时显著上升（flypy_base 25M + ecdict 27M 远大于原语料）] → 如实记录为基线；ecdict 若令部署/体积实测不可接受，按决策 1 备选降级为可选导入。
- [悬空符号链接改真实文件的构建行为未验证] → 实施期以构建实测为准（AGP 对 assets 符号链接的处理不以规划预设）。
- [62M 资产对构建/CI 时长与 release 产物体积的影响] → 实测记录，规划不预设数字。
- [default.yaml 基座完整性：submodule 未 checkout，用户版 vs prelude 版 diff 无法进行] → 实施第一步恢复 submodule 并 diff；用户版缺引擎必需键则从 prelude 回补后再内置；基座回归用例（patch 未覆盖键抽查）纳入验收。
- [librime 版本能力匹配（lua ≥1.16、predict/octagram 与 predict.db/gram 版本）] → 恢复 submodule 后确认；native 链接开关已核实为开（`rime_jni.cc:18-26`），降低不确定性。
- [gram/predict.db 缺件行为预期未验证] → 真机验证是本期验收项；若阻塞部署，按决策 2 回退处理。
- [存量代码生成版遮蔽升级用户] → 决策 6 显式处理，验收含升级场景。
- [升级安装时物化目录残留已移除的通用方案资产] → 见 Open Questions 1，tasks 含确认/清理任务。

## Migration Plan

实施顺序（tasks.md 展开）：恢复 submodule → 清单机械生成 → 资产替换与 DataChecksums 重新生成 → 两源三源代码改动（条件收紧 + 四方案 patch）→ 方案 UI 收敛 → 构建验证 → 真机验收 → 基线记录归档。

回滚：代码与资产均在 git 内，revert 即回滚；本期无一次性数据迁移、无新持久化格式。设备侧卸载重装即回到干净物化目录；升级回滚场景下物化目录孤儿文件处理随 Open Questions 1 的答案一并落地。

## Open Questions

1. **升级安装时物化目录孤儿文件的清理**：`DataManager.sync()` 的 checksums diff 拷贝是否会移除校验清单之外的文件，实施期需确认；若不清理，已安装设备上 luna_pinyin 等已释放资产会残留物化目录，需补孤儿清理逻辑（tasks 已含对应任务，答案只影响执行内容，不改变行为需求——全新安装场景不受影响）。
   **✅ 已回答（2026-11-14，实施期）**：现有机制已覆盖，无需补逻辑。`DataDiff.diff()`（`core/data/DataDiff.kt:58-63`）对「old checksums 有而 new checksums 无」的路径产出 `DeleteFile`/`DeleteDir` 指令，`DataManager.sync()`（`core/data/DataManager.kt:75-86`）按 ordinal 降序执行并 `removePath`。升级安装时旧记录含 luna_pinyin 族/stroke 族/essay.txt/tongwenfeng.trime.yaml，而新 checksums 不含，sync 自动删除物化目录中的对应孤儿文件。
2. **存量代码生成 default.custom.yaml 的处理手段**：检测特征并自动清理 vs 升级文档明确告知——行为需求（不静默遮蔽）已定，手段按实施期工作量裁量，两种手段均满足验收。
   **✅ 已回答（2026-11-14，实施期）**：选**自动清理**。`ensureDefaultCustomConfig()`（`core/data/UserDataManager.kt`）在 user 目录的 default.custom.yaml 内容与旧代码生成模板逐字一致（trim 后比对）时删除该文件，让预置四方案补丁生效；用户自己编辑过的文件（内容不等于旧模板）绝不触碰。
