# specialize-flypy 实施记录（2026-11-14）

> 实施期证据与验收状态归档。任务编号对齐 `tasks.md`；真机项（3.3、4.x）待设备验收后补录。

## 任务 1.1 submodule 恢复与版本核对 ✅

- `git submodule update --init --depth 1` 完成，10 个 submodule 全部 checkout（`git submodule status` 全部无 `-` 前缀）。
- librime 版本：**1.17.0**（CHANGELOG 顶部 2026-06-05；tag `latest`）≥ 用户配置 `installation.yaml` 声明的 1.16.1 → 模块化 lua（`lua_processor@*` 形式）与 librime-lua 配套版本满足。
- JNI 模块链接开关（`rime_jni.cc:18-26`）：lua / octagram / predict 三模块链接齐全，与 `predict.db` / `*.gram` 引用版本匹配（octagram/predict 子模块均已 checkout）。

## 任务 1.2 default.yaml 基座结论 ✅

- 结论与判据清单归档：`default-yaml-basis.md`（同目录）。
- 要点：用户版是 prelude 版的**超集**（prelude 8 个顶层键全覆盖，用户版另含 navigator/selector/editor），无缺键、无需回补；基座判据 87 键；page_size/switcher 热键等 6 个被 default.custom.yaml patch 覆盖的键已排除（无判别力，独立终审结论）。

## 任务 2.1 内置集清单 ✅

- 归档：`built-in-manifest.md`（同目录）：5 schema + 18 词典（60.5M，含 ecdict 硬依赖）+ 33 lua 引用闭包（执行按 tasks 2.2 全量 41 个 .lua）+ opencc + 全局资产。
- 逐方案验收核对：flypy_xhfast 24 个 lua 挂载 + custom patch 补挂 2 个（word_append_space/keybinder）全部在闭包内；flyhe_fast/easy_en 挂载全覆盖；reverse/radical 无 lua 挂载。
- 缺件核对：`en_dicts/en_ext_private.dict.yaml` 用户仓库本身缺失（`.gitignore` 排除 `*private*`），librime 容忍，不内置。

## 任务 2.2 资产替换 ✅（构建验证见下）

- 移除：luna_pinyin 族 7 个符号链接、stroke 族 2 个、essay.txt、tongwenfeng.trime.yaml、default.yaml 符号链接（换用户版真实文件）。
- 移入：清单全量（schema/custom 7、词典 18、lua 41 个 .lua 剔除 sendKeyCode.dll/.so、opencc/emoji.json、全局 8 + default.yaml 基座）。保留 prelude 三件符号链接与 trime.yaml 默认主题。
- `flyhe.trime.yaml` 的 TrimeApp/TrimeCmp 按键包名已替换为 com.jiaoay.jrime；assets 内 `com.osfans` 零残留。
- `.gitignore`：`assets/shared/opencc` 调整为 `opencc/*` + `!opencc/emoji.json`（构建生成的 opencc 数据仍忽略，内置 emoji.json 进 git）。

## 任务 2.3 checksums 重生成

- 由构建期 `generateDataChecksums`（DataChecksumsPlugin）自动产出，随任务 2.2 的 `assembleDebug` 构建验证一并核验（构建通过后抽查 checksums.json 覆盖新清单）。

## 任务 2.4 孤儿清理行为确认 ✅

- 答案归档：`design.md` Open Question 1。`DataDiff`（`core/data/DataDiff.kt:58-63`）对「旧记录有、新清单无」的路径产出 `DeleteFile`/`DeleteDir` 并被 `DataManager.sync()`（`core/data/DataManager.kt:75-86`）执行删除——升级安装场景的 luna_pinyin/stroke/essay/tongwenfeng 残留会被自动清理，无需补逻辑。

## 任务 3.1 ensureDefaultCustomConfig 收紧 ✅（代码审查证据）

`core/data/UserDataManager.kt` 改动后的行为矩阵：

| 场景 | 行为 | 证据 |
|---|---|---|
| user 目录有 default.custom.yaml（用户自建或 SAF 同步） | 不写、不删，user 版生效（librime user 优先） | `!custom.exists()` 短路；决策 5「用户主动行为不算缺陷」 |
| 仅 shared 有（正常全新安装/升级） | 不写代码生成版，预置四方案补丁生效 | `!preset.exists()` 短路 |
| 双缺失（极端兜底：预置资产损坏/被清） | 写入 flypy 四方案代码生成版 | `custom.createNewFile()` 分支 |
| user 有旧代码生成版（升级存量，逐字节等于旧 luna_pinyin 模板） | 自动删除，预置版接管；用户编辑过的文件绝不触碰 | `LEGACY_SCHEMA_LIST_CUSTOM_PATCH` trim 比对 |

任务 3.2（升级场景）由同一改动的第 4 行为覆盖，手段=自动清理（design Open Question 2 答案）。

## 内置主题测试适配（任务 2.2 连带）✅

内置主题集合从 trime+tongwenfeng 变为 trime+flyhe 后，主题测试栈随之适配：

- `ThemeTestSupport`：`loadResource` 回调从恒 null 改为在内置主题集（assets/shared）内解析跨文件引用——flyhe 的 `__include: trime:/android_keys`（文档性段落，解码为空）由此可解析。
- `ThemeGoldenTest`：第一个 Given 按 flyhe 实际值重写（name 飞鹤、23 preset keyboards、18 color schemes、36 键默认键盘、35 项 fallback 色表全量 golden）。
- `ThemeDrawableFallbackTest`：场景重写为「全部 flyhe palette 一致缺 `hilited_key_text_color` → fallback 链解析出可用通用色」，default palette 锚定 `0x0A1E37` 精确值。
- `ThemeDiagnosticsTest`：flyhe 的 24 项已知发现记录在案（5 个锚点定义块、android_keys、15 个旧版 style 键、日期式 config_version、by_astralwelkin 缺 light_scheme 回链）——全部 WARNING 级，不阻塞加载。

**顺带修复 flyhe.trime.yaml 的 5 个真实按键 bug**（这些 send 值原样下 `KeyCode.parse` 解析失败，运行时按键全部无效）：

| 原值 | 修正 | 影响的按键 |
|---|---|---|
| `send: Find` | `FIND` | SEARCH（搜索键） |
| `send: function`（13 处） | `FUNCTION` | 粘贴、刷新主题、剪贴板、emoji/颜符/符号/希腊字键盘、菜单键盘等全部命令键 |
| `send: Mode_switch`（9 处） | `MODE_SWITCH` | 模式切换、全半角、候选注释开关 |
| `send: Help` | `HELP` | 帮助键 |
| `send: Menu` | `MENU` | 菜单键 |

验证：`:app:testDebugUnitTest`（313 用例）+ `spotlessCheck` 全绿。

## 待真机验收（不在本次实施范围）

- 3.3 方案列表（四方案默认启用/停用/排序/添加入口，无通用方案残留）
- 4.1 全新安装冒烟（部署、四方案出字、flyhe 主题、lua 抽查、ecdict 反查链路）
- 4.2 两源三源回归（按 `default-yaml-basis.md` 的 87 判据键抽查）
- 4.3 gram/predict.db 缺件行为
- 4.4 双口径性能基线实测（`doc/refactor/roadmap.md:344-351` 附录表）
- 4.5 升级安装场景

## 模拟器部署验收（2026-11-15，Android 16 x86_64 模拟器）✅ 部署部分通过

**全新安装（uninstall → install → 引导 APP_STORAGE → 触发输入会话）部署结果：**

```
finished updating schemas: 6 success, 0 failure.
3 tasks ran: 3 success, 0 failure.
```

ready 全名单：flyhe_fast、flypy_xhfast、flypy_radical、flypy_reverse、easy_en、ecdict；bin 产物 6×3=18 个全齐。

**4.4 基线（模拟器口径，真机复测为准）：**

| 指标 | 数值 |
|---|---|
| 首启部署（全新安装，DeployMessage Start → tasks ran） | 约 285s（00:14:46→00:19:11，含 60M assets 全量释放 + 6 方案全量编译，ecdict 98 万词条占大头 23:18:22→00:18:38） |
| APK 体积（debug） | arm64 46.1M / x86_64 46.1M（`com.jiaoay.jrime-23223b7f-*`） |
| 出字链路 | ✓ 键序→IME→候选→空格上屏全通（easy_en 默认方案下字母组合上屏符合预期；模拟器 Ctrl/alt 键序注入受限，中文方案切换后的候选目验待真机/模拟器手动） |
| 稳态二次启动 | 模拟器输入连接反复失活，未取得稳定测量——真机补测 |

**部署期修复的三个资产问题（均已归档 built-in-manifest.md）：**
1. 主词典须在 shared 根（easy_en/ecdict 从 en_dicts/ 移出）；
2. ecdict.schema.yaml 补入内置集（easy_en 的 dependencies + 反查依赖）；
3. en_ext_private.dict.yaml 空骨架（import 断链兜底）。

**附带发现（app 代码问题，期 2 处理）：** `DataManager.copyFile`（`core/data/DataManager.kt:96`）对 `FileNotFoundException` 无容错——checksums 与 assets 不一致时直接 FATAL crash（rime-main 线程）。本次由 checksums 增量陷阱（git mv 后旧表残留）触发；构建期资产一致性由全量重生成保证，但运行期该路径仍是裸奔，startup-fastpath 重构 sync 链路时应加容错。
