# Trime 专精化改造 · 分期路线图

> **已被 [`doc/master-plan.md`](../master-plan.md) 取代**（2026-11-21）。本文保留为历史证据，不再作为执行依据。


> 版本：2026-11-14 r6（规划期产物，本期不改任何工程代码）。
> 依据：`.zcode/workflow-inputs/trime-refactor-survey.md`（调查事实包 **v2**，下称「事实包」）+ 对其引用源码的抽查核实 + 第 2、3 轮评审与独立终审核实。
> r6 相对 r5 的变化（独立终审 1 条，逐条处理）：补充决策 11 的基座判据举例修正——r5 所举 page_size 9、switcher 热键与折叠三项**恰被 default.custom.yaml patch 覆盖**（`default.custom.yaml:17,18,23-25` patch 为 7、[F4, Alt+grave]、false），基座取值不影响最终生效值、无判别力；改用真正未被 patch 覆盖的定制项（menu/alternative_select_labels、switcher/caption、ascii_composer、navigator/editor/punctuator 等）作举例与期 1 基座回归用例，并注明 page_size=7、fold_options=false 是 custom patch 值不能作基座判据（补充决策 11、期 1 验收与风险）。
> r5 相对 r4 的变化（第 3 轮评审 1 条）：commonMain 白名单再审——`RimeMessage.kt`/`RimeKeyEvent.kt` 类体引用平台类型移出白名单，复核新发现 `RimeSchema.kt:20-27` 引用 RimeConfig 一并移出；闸门升级为「三查」：import 核对 + 签名/类体引用核对（绑定 androidMain 保留类型具名清单）+ **commonMain 独立编译终闸**（决策 5/6/10、期 3/4、全局风险 7）。
> r4 相对 r3 的变化（第 2 轮评审 5 条）：① KMP 范围矛盾修正——RimeDispatcher/剪贴板库/SymbolHistory 带平台依赖移出白名单（决策 5/6/7）；② ecdict 改判内置（easy_en 声明依赖+反查释义词典），内置集约 62M（决策 2）；③ gram/predict 启用语义修正，验证口径改为缺件时的部署运行行为（决策 2）；④ default.yaml 基座两源决策——用户 374 行定制版替换 prelude 版进 assets（补充决策 11）；⑤ 符号链接计数修正：14 个符号链接 + 2 个真实文件（r3 误写 16）。
> 实施方式：每期一个 openspec change（`openspec/changes/<期名>/`），按 `openspec/changes/specialize-flypy/` 起步逐期推进。

## 1. 目标与现状判断

用户四条原始需求：**专精化**（内置小鹤系配置，替换文件读写重的通用配置体系，配置目录 `/home/shijy7/Workspace/Personal/rime`）、**现代化**（Compose 等）、**模块化**（dynamic feature / AAB）、**KMP 预备**（通用核心逻辑与通用 UI 控件可跨平台提取，参考 Toolbelt 模式）。

事实包揭示的关键事实（本轮已抽查核实，代码引用均在仓库内）：

- 代码不老旧：100% Kotlin、协程已全面落地（本轮 grep 复核：47 个文件 import kotlinx.coroutines、117 处 `suspend fun`）、工具链最新（AGP 9.4.1 / Kotlin 2.3.21 / KSP 2.3.12 / Room 2.8.5，`gradle/libs.versions.toml:2-6`）。真正缺口是**零 Compose、单模块**（`settings.gradle.kts:24-25` 仅 `:app` + `:codegen`）、**CI 只产 APK**（`.github/workflows/release-ci.yml:86-88`）。
- 「字库读写耗时」的根因不在按键查询路径（`core/Rime.kt:323-335` 一次 JNI 取全部响应），而在**启动串行链**：`DataManager.sync()`（`core/data/DataManager.kt:66-93`）每次启动必跑 → librime `start_maintenance` 全量部署（`app/src/main/jni/librime_jni/rime_jni.cc:39-59`）→ 才能处理按键（`core/Rime.kt:280-299`）；以及 `deploy()` 的全量重部署 + 最长 5 分钟阻塞（`core/Rime.kt:99-133`，timeout 在 `:125`）与主题链路的反复全量解析（`data/theme/ThemeFilesManager.kt:15-47`、`data/theme/ThemeLoader.kt:227-230`）。
- 硬约束：librime 无内存挂载 API，JNI 只经环境变量传 shared/user 目录路径（`rime_jni.cc:42-48`）→ **词库本体不可能搬进 Room**；opencc 词典必须落盘（`jni/librime_jni/opencc.cc:19` `SimpleConverter` 直读文件）；userdb 本体在 librime 内部 LevelDB；IME service 必须留在 base 模块；键盘本体保留自绘 View；KMP 预备 ≠ 全盘 KMP（librime JNI 桥、IME service、键盘自绘 View 是平台面）。
- 本轮新核实的积极证据：`rime_jni.cc:18-26` 已显式链接 librime 的 **lua / octagram / predict** 模块——predict.db（词频预测）与 gram（octagram 语言模型）的 native 侧编译开关已开；但配置侧的启用语义见决策 2（方案引用关系与仓库现状有出入，验证口径以主方案缺件行为为准）。

主轴：**先专精（换配置集、砍通用包袱、治启动链）→ 再建库（Room 做配置元数据 source of truth，数据层按 KMP 纪律组织）→ 后模块化（KMP 壳在此落地）→ 再 Compose 化**。现代化与模块化对「配置体验差、读写耗时」两个核心痛点是间接贡献，排序靠后；dynamic feature 一项经评估裁剪为「资产包导入更新」（决策 8）。

## 2. 总体分期视图

| 期 | change 名 | 主题 | 一句话目标 | KMP 预备动作 |
|---|---|---|---|---|
| 1 | `specialize-flypy` | 专精化内置 | 预置资产整体替换为小鹤系内置集（约 62M，含 ecdict 硬依赖），用户版 default.yaml 作基座，理顺两源三源优先级，剔除通用方案，建立性能基线 | — |
| 2 | `startup-fastpath` | 启动链优化 | 首启一次部署（预置产物，含 per-boot-mode 语义），二次启动免拷贝、免编译 | — |
| 3 | `config-room` | Room 配置库 | 配置元数据（资产清单/主题/部署与同步记录）迁入 Room，退役 JSON manifest 文件 | **KMP 纪律先行**：新写数据层 DAO 全 suspend、Entity 零 android/java import，守护测试绑定可搬运清单 |
| 4 | `modularize` | 模块化与 AAB | 按四层拓扑拆模块，KMP 壳落地（范围=纪律担保的新代码+已核实纯模型），native 独立，CI 产 AAB | **KMP 壳落地**：`:core:model`/`:core:rime`/`:core:data` 以 `kotlin("multiplatform")` + 单 android() target 声明；commonMain 化是受控改造（仅白名单成员），Android 面拆分保持纯移动 |
| 5 | `settings-compose` | 设置 Compose 化（核心页面） | 核心设置页面迁移 Compose，建立组件基线 | 组件纪律：纯 Composable + 状态提升（不承诺 CMP） |
| 6 | `compose-finish` | Compose 收尾 | 剩余页面迁移，候选栏模式收敛，旧 View 依赖退役 | — |

依赖关系：1 → 2 → 3 严格串行（配置集先定型，优化和建模才有稳定对象）；4 依赖 3（数据层稳定后再物理拆分）；5 依赖 4（Compose 迁移发生在拆好的 `:ui` 模块内）；6 依赖 5。期 5/6 与期 4 的部分工作理论可并行，规划上保持串行以控制每期验收面。

KMP 预备贯穿节奏（决策 6 详述）：**期 1-2 不做任何 KMP 动作**（纯 `:app` 内改造，主线优先）；**期 3 起数据层按 KMP 纪律写**（结构未动、纪律先行）；**期 4 结构落地**（KMP 壳模块 + commonMain/androidMain 分层，commonMain 范围按白名单三查闸门收缩，终闸为 commonMain 独立编译）；**路线图期内不承诺任何第二平台 target**。

## 3. 关键决策点（对应事实包第 6 节，逐条显式回答）

### 决策 1：分期顺序 —— 专精化先行，Room 随后；KMP 预备穿插期 3-4，不占独立期

**决策**：专精化（期 1-2）先行，Room 配置库（期 3）紧随，两者不同期做；KMP 预备不设独立期，作为期 3 的「纪律约束」与期 4 的「结构动作」穿插落地。

**理由**（判据：依赖关系 + 每期可独立验收）：
- Room 的元数据模型（资产清单、主题清单、部署记录）必须按**最终配置集**设计。先建库后专精，等于给 luna_pinyin 等即将删除的资产建模，做完就推翻。
- 专精化本身即用户痛点的一半：预置资产从通用全家桶缩到约 62M 专属配置集，`DataManager.sync()` 的拷贝面、`start_maintenance` 的编译面同步缩小，启动链收益立即可测。
- KMP 预备设独立期无法独立验收（「预备好了」不是可验证状态，见决策 6 的完成标准），且会推迟痛点的解决；穿插进期 3-4 则每一步都被当期验收覆盖。
- 期 1 可独立验收（真机出字 + 双口径基线实测），期 2 依赖期 1 的基线数字，期 3 依赖期 1 定型的配置集，链路清晰。

**备选**：专精化与 Room 同期——被否，两期风险叠加且无法归因验收指标；KMP 设独立期——被否，「预备」的完成标准是结构与纪律就位（决策 6），任何独立期都只能产出无法验收的空壳。

### 决策 2：配置内置方式 —— 约 62M 内置集全量进 assets（含 ecdict 硬依赖）；gram 不内置（主方案引用的是仓库缺失的 huayu-v7）；predict.db 不内置走可选导入；清单按 schema 引用闭包机械生成

**决策**：内置集（约 62M，本轮 du 复核：`cn_dicts/` 26M（其中 flypy_base 25M）+ `en_dicts/` 32M（**含 ecdict.dict.yaml 27M**）+ `lua/` 532K + `opencc/` 516K + 根级 schema/custom 与 `flyhe.trime.yaml` 300,932B 约 1M）**全量进 `app/src/main/assets/shared/`**，复用现有 checksums 机制首启释放（`core/data/DataManager.kt:66-93` 已是 diff 拷贝，`build-logic/.../DataChecksumsPlugin.kt` 构建期生成清单）。三个大件的去留判定：

- **ecdict：内置**（r3「不内置」被评审核实推翻）。核实依据：`easy_en.schema.yaml:14-15` 声明 `dependencies: - ecdict`（librime 方案声明依赖，缺件时方案部署不可用）；`:46` 挂 `reverse_lookup_filter@easy_en-ecdict`（英文反查中文释义）；`:70-72` `easy_en-ecdict: dictionary: ecdict`；`lua/easy_en.lua:2` 与 `lua/flypy_switcher.lua:35,127-129` 围绕 `easy_en-ecdict/overwrite_comment` 开关工作（把 ECDICT 词典的 text 作 comment 输出，即英译汉注释）。ecdict 是四方案之一 easy_en 的**声明依赖 + 核心功能件**，不是可裁剪的增强件——剔除它，「期 1 验收：四方案可切换出字」自相矛盾。62M 对自用 sideload 无压力；ecdict 词典的部署耗时增量如实计入期 1 基线（期 2 预置产物方案会消化它）。
- **gram：不内置**（启用语义修正）。核实依据：`flypy_xhfast.schema.yaml:127` 注释掉的备选才是 essay-bgw，`:129` 实际启用 `language: zh-hans-t-huayu-v7-bgw`，而用户仓库仅存在 `zh-hans-t-essay-bgw.gram`（10.4M），**huayu-v7 gram 不在仓库中**——内置 essay-bgw 对主方案不会生效。决策：维持仓库现状（方案引用的 gram 与仓库一致地缺失），**期 1 验证项 = 主方案在 gram 缺失时的部署/运行行为**（预期 octagram 检测不到语言模型即禁用语法功能、不阻塞部署，需真机确认）；用户日后取得 huayu-v7 gram 后经资产包导入（决策 8）或 SAF 同步放入即可生效。
- **predict.db：不内置**（20.8M 二进制，不可压）。核实依据：`flypy_xhfast.custom.yaml:57-58` 向引擎注入 `predictor` 处理器与 `predict_translator`，`:317` 硬引用 `db: predict.db`。**期 1 验证项 = 主方案缺 predict.db 时的部署/运行行为**（预期 predictor 组件失效、输入主链路不受影响，需真机确认）；验证通过后，predict.db 经一次性 SAF 导入放入用户目录即被 `:317` 硬引用自动启用。

**理由**：
- 自用 sideload 场景无 Play 商店体积上限压力，APK 增大是最廉价可靠的分发方式（词典为文本 YAML，打包压缩后实际增量以构建实测为准，规划不预设数字）。
- 「首启从打包资源释放」与「全量进 assets」在本仓库现有机制下是同一件事：assets 即打包资源，`DataManager.sync()` 即释放通道，零新机制。
- **内置集清单不手抄**：按「schema `dependencies:`/`import`/词典引用闭包」从配置目录机械推导（本轮已发现手抄易漏项：`melt_eng.schema.yaml`、`flyhe_fast.schema.yaml:97-98` 的 melt_eng 词典挂载、easy_en 对 ecdict 的声明依赖），以生成的清单为准准备资产并纳入 DataChecksums。

**备选**：ecdict 不内置、easy_en 降级（主输入可用、反查释义无结果）——被否：方案声明依赖缺失的部署行为未经验证，且反查释义正是 easy_en 的存在理由；仅当 APK 体积/部署耗时实测不可接受时，作为降级路径启用（启用前提 = 真机验证 librime 对缺失声明依赖不崩溃）。外部导入为主（首启引导拷入）——被否：多一步手工操作，违背「开箱即用」。

### 决策 3：Room 的边界 —— 元数据的存储与查询角色入库；物理文件仍是引擎的唯一口粮，SAF 树枚举不消灭

**决策**：
- **进 Room**：预置资产清单与校验（替代 `checksums.json` 的存储）、同步清单与状态（替代 `.sync-manifest.json` 的存储）、主题元数据（ID/名称/版本，替代 `ThemeFilesManager` 的逐文件解析）、音效元数据、部署记录（时间/耗时/触发源）、导入导出记录。
- **留文件**：物化目录（`<externalFilesDir>/shared` 与 `<externalFilesDir>/rime`，`core/data/DataManager.kt:43-47`）——启动前由 Room 清单驱动物化，喂给引擎；opencc 词典（native 直读文件，`jni/librime_jni/opencc.cc:19`）；userdb 本体（librime 内部 LevelDB，仅经 levers API 管导入导出，`data/userdict/UserDictManager.kt`）。
- **明确不承诺**：消灭 SAF 同步的树枚举。现有 `UserDataManager.sync()` 已是增量设计——全未变即跳过的 fast path（`core/data/UserDataManager.kt:187-207`），哈希仅对变更候选文件计算并复用摘要（`:313-322`）。每次同步的固有成本是 `StorageAccess.walk`（SAF 远端，`:179-180`）+ `walkLocal`（本地，`:182`）的树枚举与 manifest 读写——**远端树的变化只能靠枚举发现，Room 只持有本地视角的历史状态，无法替代枚举**；Room 能替换的是 manifest 的存储与比对载体（JSON 文件 → 表查询）。
- **首次迁移**：参照 `data/clip/LegacyClipMigrator.kt:39-84` 的模式——首启读取 `checksums.json` / `.sync-manifest.json` 与文件树 → 单事务写入 Room（`withTransaction`，同 `LegacyClipMigrator.kt:53`）→ 旧文件 retire（改名保留，同 `:126-133`）；读取失败则保留文件下次重试（同 `:48` 注释语义）。

**理由**：硬约束 1-3 的直接推论。librime 只认文件系统（`rime_jni.cc:42-48`），词库本体入库被排除；`exitRime + startRime` 循环（`core/Rime.kt:120-121`）天然支持「改库 → 重新物化 → 重启引擎」。Room 的真实收益是三件事：主题/音效等元数据的「一次解析、随处查询」（消掉 `ThemeFilesManager.kt:15-47` 为取名字对每个 yaml 全量解析）、manifest 持久化的健壮性与可查询性（含部署历史）、给资产包导入更新（决策 8）提供账本。

**备选**：无实质备选（schema/词典本体入库被硬约束 1 直接排除；「Room 查询替代 SAF 枚举」技术上不成立，不做）。

### 决策 4：Compose 化范围 —— 设置/主界面渐进迁移，键盘本体不重写

**决策**：键盘与候选栏保留自绘 View 体系（`ime/keyboard/KeyboardView.kt`、`KeyView.kt`、`Keyboard.kt`），不重写。Compose 主战场为设置界面与主界面。页面清单以实际代码为准（本轮核实）：`ui/main/settings/` 下 10 个页面型 Fragment（GeneralSettings、AdvancedSettings、CandidatesSettings、ClipboardSettings、KeyboardSettings、ProfileSettings、Progress，及 `schema/SchemaListFragment`、`theme/ThemeSettingsFragment`、`userdict/UserDictionaryFragment`）+ 5 个 Preference 派生 Dialog（ColorPickerDialog、DialogSeekBarPreference、EditTextIntPreference、SoundEffectPickerDialog、ThemePickerDialog），`ui/main/` 下 4 个页面（MainFragment、AboutFragment、LicenseFragment、DeveloperFragment）。分两期迁移：期 5 迁交互最复杂的核心 4 页（General、Keyboard、SchemaList、ThemeSettings）+ Compose 组件基线；期 6 迁剩余 6 个设置页 + 4 个主界面页 + 剩余 Dialog。

**理由**：IME 渲染低延迟属性 + Canvas 自绘已成熟（硬约束 5）；设置页 Compose 化收益（声明式、预览、与协程/Flow 自然衔接）明确。工作量控制不以「页数」为刚性单位（页面复杂度差异大），以「期 5 的 4 个复杂页 ≈ 期 6 的 10 个简单页」的工作量当量为预算基准；任何一期超预算则顺延页面，不硬塞。

**备选**：键盘候选栏用 Compose `LazyList` 重写评估——列为期 6 后的可选实验，不进主线（见第 6 节「路线图之后」）。

### 决策 5：模块化切分（含 KMP 预备）—— 四层拓扑按 Trime 实际改造；KMP 壳判定与 commonMain 白名单逐文件核对；先立护栏再拆；native 独立的理由是构建缓存与 JNI 边界

**决策**：目标模块边界（四层拓扑的 Trime 版）：

| 模块 | 内容 | 构建声明 | KMP 分层（本轮逐文件核对） |
|---|---|---|---|
| `:core:model` | 引擎响应协议与纯数据类（从 `:core:rime` 拆出） | `kotlin("multiplatform")` 壳 | **commonMain**：`RimeProto.kt`（145 行，全部类型纯 String/Int/Boolean/Array，本轮全文核实）、`SchemaItem.kt`（10 行纯 data class）——初始仅此两个文件，是三查闸门下如实核算的结果；**androidMain**：`RimeMessage.kt`（`KeyMessage.Data` 字段为平台类型 KeyValue/KeyModifiers，`:112-113`；`nativeCreate` 构造平台类型，`:181-182`）、`RimeKeyEvent.kt`（类体构造 KeyValue/KeyModifiers，`:13,15`；三个 `external fun` JNI，`:24-26`）、`RimeSchema.kt`（类体调用 `RimeConfig.openConfig/openSchema` 读 JNI 配置，`:20-27`）、`KeyValue.kt`/`KeyModifier.kt`（`android.view.*` + splitties）、`RimeApi.kt`（`processKey(value: KeyValue, ...)` 签名引用平台类型，`:33-36`）、`RimeLifecycle.kt` |
| `:core:rime` | JNI 门面、dispatcher、daemon、session | `kotlin("multiplatform")` 壳 | **androidMain**：`Rime.kt`（external fun + BuildConfig）、`RimeDispatcher.kt`（`java.util.concurrent` + timber，`:16-19`）、`RimeConfig.kt`（timber）、`daemon/`、`core/data/` 现有实现；commonMain 仅承接 `:core:model` 迁入的协议部分 |
| `:core:data` | Room、DataManager、主题数据、prefs、SAF | `kotlin("multiplatform")` 壳 | **commonMain**：期 3 新写的 `ConfigDatabase` Entity/DAO/`@Database`（纪律担保）；**androidMain**：`DataManager`/`UserDataManager`/SAF/prefs/主题 kaml 解析、剪贴板库与 `SymbolHistory` **现状形态**（含平台依赖，见决策 6 排除清单） |
| `:core:native` | librime_jni C++ 与 4 ABI 的 .so 产出 | Android library（平台面） | 不做 KMP |
| `:ui` | 设置/主界面（期 5 起在此模块 Compose 化） | Android library（compose） | 本路线图内不做 KMP（组件纪律见决策 6） |
| `:app`（base） | IME service + 键盘 + 壳工程 | Android application | 平台面（硬约束 4/7） |
| `:codegen` | 既有 KSP 符号处理器（`codegen/src/main/java/com/jiaoay/jrime/codegen/GenKeyMapping.kt`） | 保持 | 不动 |

KMP 壳判定规则（可机械执行）：**模块**层面——含「未来可跨平台资产」的模块用 `kotlin("multiplatform")` 声明 + 暂时只有 android() target；纯平台面模块（JNI C++、IME service、键盘 View、宿主）用常规 Android 插件。**文件**层面——进 commonMain 的闸门是**三查**（r4 的「唯一闸门=逐文件 import 核对」被第 3 轮评审证伪：同包类型引用不出现在 import，`RimeMessage.kt`/`RimeSchema.kt` 均零 import 却引用平台类型）：① import 核对（零 `android.*`/`java.*`/JVM 库 import，必要非充分）；② **签名/类体引用核对**——对候选文件全文本扫描「androidMain 保留类型具名清单」（`Rime`/`RimeApi`/`RimeConfig`/`RimeDispatcher`/`RimeMessage`/`RimeKeyEvent`/`RimeSchema`/`KeyValue`/`KeyModifiers`/`BuildConfig`…，清单随拆分维护），并核对同包引用闭包（被引用者必须同在 commonMain）——本轮实证其必要性：第一轮按类名直觉误判（r3）、第二轮按 import 核对仍漏（r4 复查用的 grep 模式同样漏掉了 `RimeSchema.kt:20-27` 对 `RimeConfig` 的引用——静态文本扫描的匹配模式本身不可靠，核对必须绑定具名清单并记录核对结论）；③ **commonMain 独立编译终闸**——KMP 源集天然隔离，commonMain 编译看不到 androidMain 声明，任何泄漏引用直接编译失败，这是不可绕过的最终验证。三查不过的一律留 androidMain，**不为凑 commonMain 做改造**（改造项见决策 6 排除清单与第 6 节）；commonMain 无平台依赖由守护测试（①+②，期 3）与独立编译（③，期 4）双层强制（Toolbelt 的 SDK 隔离测试模式，事实包 4.2 模式 6）。

拆分顺序：**先补契约测试护栏**（对 RimeSession 消息协议、Theme golden、DataDiff/物化行为固化基线，现有 26 个 kotest 单测上补关键路径），再从依赖最少的 `:core:model` 拆起，随后 `:core:rime` → `:core:data` → `:ui`，`:app` 壳最后收口。

**理由**：拆分是纯物理移动，没有护栏时行为回归无法察觉；`build-logic/convention/` 已有 7 个 convention plugin 类 + 3 个辅助文件（本轮核实，修正 r2 的「8 个插件」表述：AndroidApp/AndroidBase/DataChecksums/NativeApp/NativeBase/NativeCacheHash/OpenCCData），多模块基建只差拆本身；新模块的 convention（KMP library、KMP feature）照 Toolbelt 三件套骨架适配（`EasyDevToolbox/build-logic/src/main/kotlin/{KmpComposeConventionPlugin,KmpFeatureConventionPlugin,JvmCliConventionPlugin}.kt`，本轮核实存在）。**`:core:native` 独立的理由**：a) 构建缓存隔离——librime C++ 编译链极重（4 ABI），独立成 Gradle 模块后 Kotlin 侧改动不触发 native 重链，CI 缓存粒度更细；b) JNI 边界显性化——native 产物与 JNI 门面同模块，外部模块只见 Kotlin API。**特别澄清**：AAB 对 base 模块的 native 库本来就自动按 ABI 生成 configuration splits，这是 AAB 格式的固有能力，与 librime 是否独立成 Gradle 模块**无关**——「按 ABI 拆体积」不作为拆 `:core:native` 的依据。

**与「只拆不改」的关系**（评审核实的矛盾在此消解）：期 4 拆分动作分两类——**Android 面（模块边界重划、androidMain 内移动）是纯移动**，行为回归靠护栏测试兜底；**commonMain 化是受控改造**，且范围被三查闸门收缩到期 3 纪律担保的新代码 + 核对通过的纯模型（`RimeProto.kt`/`SchemaItem.kt`），这两类成员进 commonMain 不需要行为改造、只有包名与依赖装配调整，「只拆不改」承诺的适用面由此成立。白名单在 r3→r4→r5 三轮评审中持续收缩（五文件 → 三文件 → 两文件），收缩本身不是失败——白名单宁可小而真，不可大而假；commonMain 初始体量小恰是「预备」而非「迁移」的正确形态，未来新代码按纪律直接落 commonMain，改造完成的排除成员按第 6 节流程迁入。

**备选**：先拆模块后补测试——被否，回归风险不可控；「只拆不壳」（全部 Android library，未来再转 KMP）——被否：转 KMP 壳要重排 source set 与依赖装配，`kotlin("multiplatform")` + 单 android() target 从第一天就声明的成本几乎为零（Toolbelt 已验证，事实包 4.2 模式 1）；把 RimeDispatcher 等排除项改造进 commonMain——被否，扩大期 4 验收面且当期无消费方（见决策 6）。

### 决策 6：KMP 预备的范围与节奏 —— 范围=「纪律担保的新代码 + 已核实纯模型」白名单；排除清单显式化；节奏「纪律（期 3）→ 结构（期 4）→ 无承诺（期内不加第二 target）」

**决策**：
- **commonMain 白名单**（进 commonMain 的唯一入口，三查闸门制，见决策 5）：
  a) 期 3 新写的 `ConfigDatabase` 数据层（Entity/DAO/`@Database`，KMP 纪律从第一行代码就生效）；
  b) 三查核对通过的纯模型：`RimeProto.kt`（145 行引擎响应协议：CommitProto/CandidateProto/CompositionProto/ContextProto/StatusProto/Candidates/RimeResponse，全纯类型，本轮全文核实）、`SchemaItem.kt`（10 行纯 data class）——**初始仅此两个文件**；
  c) 此后**新写**的通用逻辑按纪律直接进 commonMain。
- **排除清单**（现状含平台依赖，本轮核实，改造完成前一律留 androidMain，**不为 KMP 壳提前改造**）：
  - `RimeMessage.kt:112-113,181-182`：`KeyMessage.Data` 字段类型与 `nativeCreate` 构造体引用平台类型 `KeyValue`/`KeyModifiers`——同包引用不出现在 import，文件本身零 import 也不合格（r4 误列入白名单，r5 依第 3 轮评审移出）；其纯消息类型（SchemaMessage/CommitTextMessage/CompositionMessage/StatusMessage 等引用的均为 commonMain 合格类型），整体提取需先拆出 KeyMessage，列第 6 节改造项；
  - `RimeKeyEvent.kt:13,15,24-26`：类体构造 `KeyValue`/`KeyModifiers` + 三个 `external fun` JNI 声明——消息外壳与 JNI 桥一体，r4 误列入白名单，r5 移出；
  - `RimeSchema.kt:20-27`：类体调用 `RimeConfig.openConfig/openSchema`（RimeConfig 依赖 timber 与 JNI external fun）——r5 复核新发现（第二轮扫描的 grep 模式同样漏掉它，静态文本扫描不可靠的又一实证），移出；
  - `RimeDispatcher.kt:16-19`：`java.util.concurrent.Executors/LinkedBlockingQueue/AtomicBoolean` + `timber.log.Timber`——线程池实现是 JVM API，进 commonMain 需纯协程重写（r3 误将其计入「纯协程」白名单，r4 修正）；
  - 剪贴板库：`data/clip/ClipboardEntity.kt:8-11` `android.content.ClipData/ClipDescription`、`android.os.Build/PersistableBundle`（Entity 即平台 API，`fromClipData`/`markSensitive` 桥接是 Android 剪贴板专有）、`ClipboardDao.kt:50` `PagingSource`（当前依赖 Android 侧 paging-runtime）——KMP 化需先拆 ClipData 桥接与 paging 依赖（r3 误列为通用数据，r4 修正）；
  - `data/SymbolHistory.kt:7,16-17`：`TrimeApplication` + `java.io.File`——文件读写是平台面（r3 误列为通用数据，r4 修正）；
  - `KeyValue.kt`/`KeyModifier.kt`（`android.view.*` + splitties）、`RimeApi.kt`（`:33-36` 签名引用平台类型）、`RimeLifecycle.kt`、主题 kaml 解析链（`ThemeLoader` 依赖 `java.io.File` 树遍历）、SAF 同步（StorageAccess）、opencc/userdb 管理——本来就是平台面，不属提取对象。
- **通用 UI 控件**：用户需求原话包含「通用 UI 控件」。本路线图内的兑现方式是**组件纪律而非框架切换**：期 5 的 Compose 组件基线要求「纯 Composable + 状态提升、不捕获 Activity/Context」；`:ui` 模块本身留在 Android library，转 Compose Multiplatform 列为路线图之后的独立评估（第 6 节）——CMP 会引入 androidx↔org.jetbrains.compose 依赖镜像与 IME 场景之外的复杂度，当前无验收路径，不进承诺。
- **节奏**：期 1-2 零 KMP 动作；期 3 纪律先行（新写数据层按 KMP 组织规则写 + 守护测试）；期 4 结构落地（KMP 壳模块 + 白名单成员 commonMain 化）；**期内完成标准 = 结构与纪律就位并机械可验证**（模块声明为 multiplatform、commonMain 无平台 import 由守护测试强制、白名单核对记录归档），**不承诺任何第二平台 target（不加 jvm()/ios()）、不承诺跨平台跑通**——「预备」的可验收终点就是结构，跨平台运行属于路线图之后的 PoC。

**理由**：硬约束 7 的直接落实；「预备」的价值在后续扩展时**不需重排架构**（Toolbelt 模式 1 验证：后续加平台 = 改 convention plugin 一行 + 逐步搬文件），提前兑现「跨平台跑通」在只有 Android 一个真实用户面的当下是无法验收的投入。范围用白名单制且闸门经三轮收敛：r3「按类名直觉」误纳 RimeDispatcher/剪贴板/SymbolHistory（第 2 轮评审）；r4「逐文件 import 核对」仍漏 `RimeMessage`/`RimeKeyEvent`/`RimeSchema` 的类体/签名引用（第 3 轮评审：同包引用不出现在 import）；r5 闸门定为三查，并把 **commonMain 独立编译**设为不可绕过的终闸——静态核对无论怎么设计都可能漏，编译器不会。被排除的成员留在 androidMain 完全不妨碍「壳先行」——未来改造完成后按三查流程搬入 commonMain 不需要重排架构，这正是 KMP 壳的价值。

**备选**：期 3 即建 `:core:data` KMP 壳模块（commonMain 从第一天物理存在）——被否：「配置元数据迁 Room」是体验主线，不该背负模块基建与 KSP×KMP 组合的构建摩擦风险，纪律约束 + 期 4 一次搬运的成本更低；把排除项的改造纳入期 4——被否，改造（协程重写/桥接拆分）是独立工程量，当期无消费方，列路线图之后；全盘 KMP 化（含 IME service）——被硬约束 7 直接排除。

### 决策 7：存储选型与 KMP 的关系 —— Room（不选 SQLDelight）；数据层从期 3 起按「KMP 纪律」组织，期 4 白名单成员 commonMain 化

**决策**：Room。数据层组织：期 3 在 `:app` 内建库时即遵守 KMP 组织纪律——**Entity/DAO/`@Database` 与 builder 装配分文件、DAO 全 suspend、Entity/DAO 零 `android.*`/`java.*` import、不用 Android 独占 API（createFromAsset/setQueryExecutor 等），并以守护测试机械强制**；期 4 拆 `:core:data` 为 KMP 壳时把这套文件搬进 commonMain，仅新增 `@ConstructedBy`/`RoomDatabaseConstructor` expect/actual 样板（这是全路线图唯一预期内的 expect/actual）与 `BundledSQLiteDriver` 平台装配。

**理由**：
- Room vs SQLDelight：**Room 2.8.5 已在依赖内**（`gradle/libs.versions.toml:6,27-30`）且自 2.7.0 起 stable 支持 KMP（事实包 4.3 已核实：Android/iOS/JVM Desktop/native Mac/Linux target，配套要求 Kotlin 2.0+ KSP2 代码生成——本仓库 Kotlin 2.3.21 + KSP 2.3.12 均满足）。
- **剪贴板先例的角色限定**（r4 修正）：`data/clip/ClipDatabase.kt` + `LegacyClipMigrator.kt` 是 **Room 数据库组织与一次性迁移模式的先例**（期 3 直接套用）；但其 Entity 含平台 API（`ClipboardEntity.kt:8-11`），**不是 KMP 组织方式的先例**——期 3 的 `ConfigDatabase` 不能照抄它的文件组织（`fromClipData` 桥接与 `ClipboardDao` 的 `PagingSource` 都不能出现在新数据层），守护测试因此必须独立设立。
- 「第一天就按 commonMain 组织」的等价问题：单模块期没有 commonMain source set，「按 commonMain 组织」的实际形态就是上述纪律约束；纪律的成本是一条测试，而「先随手写、后改签名」的返工成本是所有 DAO 的 suspend 化改造（Room KMP 要求非 Android 平台 DAO 全 suspend）。故纪律先行值得。

**备选**：SQLDelight（Toolbelt 用 2.4.1）——被否：模式上同构（.sq 真源在 commonMain + 平台驱动），但引入新工具链、新查询语言、无双先例，收益仅是「与 Toolbelt 同栈」——同构可复制的是**模式**，不必同栈；期 3 随意写、期 4 再统一改造——被否，DAO 签名返工 + 二次审查成本高于一条守护测试。

### 决策 8：AAB / dynamic feature —— 产 AAB，裁掉真 dynamic feature，以「资产包导入更新」替代

**决策**：期 4 CI 增加 `bundleRelease` 产物并以 bundletool 验证侧载（AAB + 模块化，安装期合并）；**不做**真 dynamic feature（按需安装/动态更新模块）。用户的「模块化动态更新」诉求改造为：**配置/词库/主题资产包（zip）应用内导入更新**——依托期 3 的 Room「改库 → 物化 → 重启引擎」循环，导入新资产包 → 更新 Room 清单 → 物化 → `exitRime/startRime`，全程无需重装 APK。gram 与 predict.db 也经此通道补件（决策 2）。

**理由**（诚实评估）：
- 自用 sideload 不走 Play 分发，真 dynamic feature 的核心收益（Play 按需下发、免重装更新）全部依赖 Play 通道或 bundletool 自建流程，实际操作成本高于收益；
- IME service 必须 base（硬约束 4），能进 dynamic feature 的只剩资源性内容——而资源性内容的「动态更新」用资产包导入即可达成，且能更新**词典数据**这种真高频内容（dynamic feature 反而做不到不重装更新内置 assets）；
- 宁可裁掉诉求也不留一个无法验收的空壳期：资产包导入有明确验收路径（导入 → 物化 → 引擎重启 → 新词生效）。

**备选**：真 dynamic feature（`on-demand` 模块承载词库主题）——被否，理由如上；若后续真需要免电脑更新，可评估自建 https 分发 + `SplitInstallManager`，属路线图之后的独立评估项，不进承诺。

### 决策 9：历史包袱剔除范围 —— 资产删、死代码删、候选栏按实际生效模式收敛；SAF 同步与 direct boot 保留但瘦身

**决策**：
- **删（资产）**：`app/src/main/assets/shared/` 中 luna_pinyin 全家与 stroke（本轮 `find -type l` 核实 14 个符号链接的构成：luna 族 7 个（luna_pinyin.dict/schema/fluency/simp/tw、luna_quanpin、pinyin）+ stroke 族 2 个（stroke.dict/schema）+ essay 1 个 + prelude 族 4 个）、essay.txt、tongwenfeng.trime.yaml（197,771B 真实文件，被 `flyhe.trime.yaml` 取代）、原通用 `trime.yaml`（78,253B）中为多方案服务的配置段——删的是 luna 族 7 个与 stroke 族 2 个链接及 essay.txt，prelude 族（default/key_bindings/punctuation/symbols）中除 default.yaml 按补充决策 11 换为用户版外均保留。
- **删/改（代码路径）**：`core/data/UserDataManager.kt:43-48` 写死 luna_pinyin 双方案的 `SCHEMA_LIST_CUSTOM_PATCH`——`ensureDefaultCustomConfig` 的写入条件收紧为「user 与 shared 均缺失才写」且 patch 内容改为 flypy 四方案（详见补充决策 11）；多方案通用化的 UI 路径（方案市场、通用方案描述）随期 1/5 收敛。
- **收敛（候选栏多模式）**：候选栏现有三套实现——compact（水平候选，`ime/candidates/compact/`）、unrolled（Flexbox 长句候选，`ime/candidates/unrolled/`）、popup（弹窗分页，`ime/candidates/popup/`，本轮核实三目录齐全）。这是主题/偏好驱动的功能面而非纯冗余，**不盲目删**：期 6 前置调研确认 flyhe 主题实际生效的候选模式，未使用的模式代码与其独占依赖列入剔除清单——flexbox（`app/build.gradle.kts:149`）与 bravh（`app/build.gradle.kts:150`）为 unrolled/RecyclerView 路径独占，随 unrolled 模式去留一并裁决；paging-runtime 仅为评估项不承诺清退（剪贴板库经 room-paging 共用，`app/build.gradle.kts:140,146`）。
- **保留但简化**：SAF DocumentsProvider（`provider/RimeDataProvider.kt`）与 WorkManager 周期同步（`worker/BackgroundSyncWork.kt`）——保留理由：用户配置目录本身是 git 仓库，SAF 同步是从该仓库拉取配置更新的现实通道（也是 gram/predict.db 补件通道，见决策 2）；简化方向：期 3 把 `.sync-manifest.json` 的存储迁 Room（枚举与增量比对机制保留，见决策 3）。
- **保留**：direct boot 双目录逻辑（`TrimeApplication.kt:89-104`、`core/data/DataManager.kt:29-34`）——directBootAware 是 IME 锁屏可输入的硬需求；专精化后预置资产固定，direct boot 拷贝面自然缩小，维护成本随期 1 下降。
- **不进内置集**：`build/`（329M 部署产物）、`sync/`（55M）、`.git/`（356M）、`*.userdb/` 快照（最大 220K，事实包 3 已盘点，无需搬运）。

**理由**：剔除以「是否仍有真实用户路径」为判据。同步与 direct boot 都有不可替代的真实路径（git 仓库同步源、锁屏输入）；资产、多方案通用代码与未使用的候选模式则在专精化后失去路径或用户。

### 决策 10：每期验收标准 —— 三层门槛 + 双口径实测指标 + 结构守护（期 3 起）

**决策**（每期必须全部满足才算完成）：
1. **构建层**：期 4 前 `./gradlew :app:assembleDebug :app:testDebugUnitTest` 全绿；期 4 拆出 `:core:*` 模块后，门槛命令改为**全仓库聚合** `./gradlew test :app:assembleDebug`（`:app:testDebugUnitTest` 只覆盖 `:app` 模块，新模块单测必须由根聚合任务覆盖），期 4 起另加 `:app:bundleRelease`。现有 26 个 kotest 单测不因改造劣化。
2. **结构层（期 3 起）**：期 3 验收含数据层守护测试（DAO 全 suspend、新数据层文件零 `android.*`/`java.*` import **且**对 androidMain 保留类型具名清单的全文本引用扫描为零命中，断言绑定「期 3 定义的可搬运文件清单」）；期 4 验收含 KMP 壳结构检查（`:core:model`/`:core:rime`/`:core:data` 声明为 `kotlin("multiplatform")`、**commonMain 源集独立编译通过**（三查终闸，KMP 源集隔离使泄漏引用在此显形）、白名单三查核对记录归档、无第二平台 target）+ 模块依赖白名单检查（模块间只经声明 API 依赖）。
3. **行为层**：真机安装冒烟——能启用、能出字、四方案可切换、lua 脚本功能生效（如 calculator）、easy_en 反查释义抽查（输入英文单词出中文释义，验证 ecdict 链路）。
4. **指标层**（期别专属，先定义测量口径再定数值）：
   - **两个口径**：**首启口径** = 安装后首次启动（含完整部署链），**稳态口径** = 部署完成后的二次冷启动。两个口径的数字不允许混比。
   - 期 1 建立基线：两个口径分别实测——首启部署耗时（logcat 中 deploy Start→Success 通知时间戳，`rime_jni.cc:248-265` → `core/Rime.kt:594-603` 路径）、冷启动到首次出字（adb 手工计时，无 instrumented test 基建，不虚构自动化指标），写入本文档附录；
   - 期 2：首启口径——首启部署耗时较期 1 首启基线下降 ≥50%（预置产物路径达成时；spike 失败走降级方案则如实记录并调整承诺）；稳态口径——二次冷启动 logcat 无词典编译证据 + 稳态冷启动到首字较期 1 稳态基线**不劣化**；
   - 期 3：主题列表加载零文件解析（profiler/日志证据）+ 改配置→物化→deploy 全链路较期 2 不劣化 + Room 迁移幂等（重复启动不重复迁移、失败可重试）；
   - 期 4：AAB 经 bundletool 生成的 APK 可侧载安装且冒烟通过；APK/AAB 体积对比记录（AAB 体积记录仅验证产物形态，ABI configuration splits 为 AAB 固有能力，不归因模块拆分）；
   - 期 5/6：设置页面功能对等清单逐项勾验（每页面一张对照表）。

**理由**：无 instrumented test 基建（事实包 1.1），真机实测以手工冒烟 + logcat 为准；先锁口径再定数值，避免「首启基线比稳态实测」这类口径错配导致指标形同虚设或不可达；相对指标（较基线下降比例）避免规划期拍脑袋定绝对值；结构层门槛把 KMP 预备（决策 6）的「可机械验证」承诺落成可执行检查。

### 补充决策 11：配置基座两源 + custom 三源 —— default.yaml 用户版作 assets 基座；default.custom.yaml 三源 user > shared > 代码兜底

**现状机制**（本轮核实，评审核实的两源冲突在此显式处理）：
- `assets/shared/default.yaml` 是指向 prelude（`app/data/rime/prelude/default.yaml`，submodule 未 checkout）的符号链接之一；用户仓库另有 **374 行、12,699B 的完整定制版 `default.yaml`**（config_version 2025.01.11；定制面覆盖 menu/switcher/ascii_composer/navigator/editor/punctuator/recognizer/key_binder 各段；文件头注释自述机制：「要比共享目录的同名文件的 config_version 大才可以生效」）。
- librime 对非 `.custom` 同名文件按 **user > shared 目录优先整文件覆盖**（与 `.custom.yaml` 的 patch 合并机制不同）。→ APP_STORAGE 模式（Trime 现状默认，用户目录初始无 default.yaml）下生效的是 assets 物化的 prelude 版，**用户定制版因同名进不了 assets，其未被 default.custom.yaml patch 覆盖的定制项静默丢失**——「开箱即用」在 default.yaml 层面未兑现。
- **patch 覆盖关系辨析与基座判据**（r6 依终审修正）：menu/page_size、switcher/hotkeys、switcher/fold_options **恰被 custom patch 覆盖**（`/home/shijy7/Workspace/Personal/rime/default.custom.yaml:17,18,23-25` 分别 patch 为 7、[F4, Alt+grave]、false，本轮实读两个文件逐项对照核实）——无论基座是 prelude 版还是用户版，这三个键的最终生效值相同，**不能用作「基座是否生效」的判据**；真正未被 patch 覆盖的用户定制项是 `menu/alternative_select_labels`（default.yaml:18）、`switcher/caption`（:22）、`switcher/abbreviate_options`/`option_list_separator`（:31-32）、`ascii_composer`（:47-54）、`navigator/bindings`（:64-69）、`editor/bindings`（:86-96）、`punctuator`（:105-172）、`recognizer/patterns`（:175-187）、`key_binder/bindings`（:189-230）——**基座判据只能从这里选**，且最终判据清单以期 1 恢复 submodule 后 diff 用户版 vs prelude 版、从「patch 未覆盖且两版取值不同」的键中圈定。
- default.custom.yaml 三源问题（r2 已识别）：启动链每次经 `UserDataManager.ensureDefaultCustomConfig()`（`core/Rime.kt:282` 调用），user 目录缺失时写入写死 luna_pinyin 双方案的 patch 版（`core/data/UserDataManager.kt:135-140`，patch 内容 `:43-48`），而 librime 查找 user 优先于 shared → 代码生成版会**永久遮蔽** assets 预置的用户版（四方案 + menu/page_size + switcher patch，`/home/shijy7/Workspace/Personal/rime/default.custom.yaml`）。

**决策**：
- **default.yaml（基座）两源**：assets 内置版**直接采用用户定制版**（替换 prelude 版位置），物化到 shared 作为基座生效版本——APP_STORAGE 模式下开箱即用户定制；prelude 其余三件（key_bindings/punctuation/symbols）保留（仍被引用）。EXTERNAL_SYNC 模式下，用户仓库的 default.yaml 经 SAF 同步进 user 目录，按目录优先级覆盖 shared 版——用户后续演进 default.yaml 走同步通道生效，闭环成立（与用户文件头注释的 config_version 语义一致）。
- **default.custom.yaml 三源**：user（用户经 SAF 同步/手工修改）> shared（assets 预置物化）> 代码兜底。`ensureDefaultCustomConfig` 写入条件从「user 缺失即写」收紧为「**user 与 shared 均缺失才写**」，正常路径不再往 user 目录写代码生成版；patch 内容同步改为 flypy 四方案（仅作兜底）。用户启用 SAF 同步带入自己仓库的 default.custom.yaml 时，user 版优先生效——这是用户主动修改，符合 librime 目录语义，不视为缺陷。
- 期 1 实施项：恢复 submodule 后 **diff 用户版 default.yaml vs prelude 版**，确认引擎必需键无缺失（prelude 当前未 checkout，本轮无法本地 diff，如实列为验证项；用户版缺键则从 prelude 回补后再内置）。

**备选**：用户定制转写为 default.custom.yaml patch（全部经 patch 表达）——被否：改变用户配置管理方式、用户仓库继续演进时两边漂移；维持 prelude 版内置——被否：APP_STORAGE 模式下 boomker 定制不生效，「开箱即用」在 default.yaml 层面落空；assets 不放 default.custom.yaml、patch 硬编码进代码——被否：配置硬编码违背「配置即数据」方向。

## 4. 各期详述

### 期 1 · `specialize-flypy` 专精化内置

**目标**：Trime 从「通用输入法」变为「小鹤系专精输入法」：预置资产整体替换为 `/home/shijy7/Workspace/Personal/rime` 的内置集（含 ecdict），用户版 default.yaml 作为基座，通用方案与相关代码路径剔除，配置基座两源与 custom 三源理顺，并建立全路线图的双口径性能基线。

**范围**：
1. `app/src/main/assets/shared/` 资产替换：移入 `default.custom.yaml`（四方案）、**用户版 `default.yaml`（374 行定制版，作基座替换 prelude 版，见补充决策 11）**、根级 schema/custom、`cn_dicts/`、`en_dicts/`（**全量 32M，含 ecdict.dict.yaml 27M——easy_en 硬依赖**）、反查/部件词库、`lua/`（30 脚本）、`opencc/`、`flyhe.trime.yaml`（300,932B，作为默认键盘主题）；移除 luna_pinyin 全家、stroke、essay.txt、tongwenfeng.trime.yaml。prelude 其余三件（key_bindings/punctuation/symbols）保留——引擎运行必需。
2. **内置集清单机械生成**：按 schema `dependencies:`/`import`/词典引用闭包从配置目录推导（本轮已发现手抄易漏项：`melt_eng.schema.yaml`、`flyhe_fast.schema.yaml:97-98` 的 melt_eng 词典挂载、`easy_en.schema.yaml:14-15` 的 ecdict 声明依赖），以生成的清单为准准备资产。
3. `build-logic` DataChecksumsPlugin 对新资产集重新生成清单（构建配置属实施期工作，本期只规划）。
4. **两源三源落地**（补充决策 11）：用户版 default.yaml 进 assets 作基座；`ensureDefaultCustomConfig`（`core/data/UserDataManager.kt:135-140`）写入条件收紧 + patch 内容改 flypy 四方案；恢复 submodule 后 diff 用户版 vs prelude 版确认基座完整性。
5. **gram / predict.db 验证与通道**（决策 2）：不内置 gram 与 predict.db；期 1 真机验证「主方案缺 gram（方案引用 huayu-v7，仓库缺失）/缺 predict.db（`flypy_xhfast.custom.yaml:317` 硬引用）时的部署与运行行为」（预期分别禁用语法模型/predictor，不阻塞部署与主输入链路）；验证通过后经 SAF 导入或资产包通道补件。predict.db 放入用户目录后由 `:317` 硬引用自动启用。
6. 方案切换 UI 收敛为内置四方案 + 自定义方案入口。
7. 实施验证项：恢复 submodule 并确认 librime 版本对 lua 扩展（≥1.16）与 predict/octagram 模块的实际版本匹配（native 链接开关已核实为开，`rime_jni.cc:18-26`）；ecdict 若体积实测不可接受，按决策 2 备选降级（前提=真机验证缺失声明依赖不崩溃）。

**依赖**：无（起点期）。

**验收**：决策 10 三层门槛 + 双口径基线建立（首启与稳态的部署耗时、冷启动到首字，记录到附录）+ 四方案全部可切换出字 + **easy_en 反查释义抽查**（输入英文单词出中文释义，验证 ecdict→reverse_lookup_filter→lua 注释链路）+ **gram/predict 缺件场景部署运行行为符合预期并记录** + lua 脚本抽查（calculator 等）生效 + **两源三源回归**（首启后：shared 版 default.yaml 基座与 default.custom.yaml 均生效、user 目录无代码生成版、**未被 custom patch 覆盖的基座定制项生效**——抽查 alternative_select_labels/ascii_composer/navigator 等 patch 未覆盖键，判据清单以期 1 diff 用户版 vs prelude 版后从「patch 未覆盖且两版取值不同」的键中圈定；page_size=7、fold_options=false 是 custom patch 值，不随基座变化，不作基座判据；SAF 同步启用后 user 版优先）。

**风险**：
- **首启部署耗时可能显著上升**：内置语料 62M（flypy_base 25M + ecdict 27M）远大于原 luna_pinyin 语料，首次编译时间变长——如实记录为基线，它是期 2 的直接优化对象；ecdict 若令部署/体积不可接受，按决策 2 备选降级为可选导入；
- assets 中原为悬空符号链接（submodule 未 checkout，本轮核实 `assets/shared/` 下 **14 个符号链接** + 2 个真实文件；r3 误写 16），新资产改为真实文件后构建行为需实测；
- 62M 资产对构建/CI 时长与 release 产物体积的影响需实测（词典文本压缩率以构建为准）；
- **default.yaml 基座替换的完整性**：用户版与 prelude 版的 diff 在 submodule 恢复前无法进行，若用户版缺引擎必需键需回补——基座回归用例（**从未被 custom patch 覆盖的定制项中抽查**：ascii_composer、alternative_select_labels、navigator 等，判据清单以 diff 为准；page_size/switcher 热键与折叠三项被 custom patch 覆盖、无基座判别力，不作判据）纳入验收；
- 三源收紧后「从旧版 Trime 升级」场景：user 目录已有代码生成版 default.custom.yaml 的存量用户会被 user 版继续遮蔽——升级路径需显式处理（检测代码生成版特征并清理，或文档说明），不静默吞掉。

### 期 2 · `startup-fastpath` 启动链优化

**目标**：消掉「每次启动」的重复开销：首启一次完整部署（预置产物），二次启动免拷贝、免编译；direct boot 语义不被破坏。

**范围**：
1. `DataManager.sync()` 版本指纹短路：启动时比对 assets 整体指纹与上次记录，一致则完全跳过 diff/copy（现为每次启动必走，`core/data/DataManager.kt:66-93`）。**指纹记录必须继承 per-boot-mode 存储语义**——`DataManager.kt:29-34` 的既有约束：checksums 记录按 boot mode 分开存，否则 normal 模式运行会覆盖 direct-boot 记录、下次 direct boot 误信跳过部署；指纹机制同样双份记录，不重蹈这个已修复的 bug。
2. 预置部署产物评估与实施：将精选的 librime 编译产物（用户仓库 `build/` 329M 部署产物中剔除临时文件与 userdb 快照后的部分）随 assets 分发，物化到位后 `start_maintenance` 检测一致即跳过编译。现有接缝：`DataManager.kt:46-47` 已定义 `prebuiltDataDir = shared/build` 与 `stagingDir = user/build`，`resolveDeployedResourcePath`（`:56-64`）已按 staging → prebuilt 顺序回退——预置产物放 shared 侧有现成读取通道。**必须保留回退**：预置产物校验不符时回退完整编译（不能出现「预置产物损坏导致永远无法部署」）。
3. 部署状态语义理顺：`ON_READY` 与 deploy 完成通知（`core/Rime.kt:594-603`，`rime_jni.cc:248-265` 的 deploy 消息）的状态机文档化，部署期间 UI 明确「部署中」而非假就绪。
4. 主题加载缓存：`ThemeLoader` 解析结果按（文件 mtime+size）缓存，避免同进程重复 `readText` + kaml 全量解析（`data/theme/ThemeLoader.kt:227-230`）；`ThemeFilesManager.listThemes` 的重复调用收敛（彻底方案在期 3 进 Room）。
5. 次要热点顺带清理：`data/soundeffect/SoundEffectManager.kt:38-59` 的 `userEffects` 为 `get() = listSounds()`，每次访问都 listFiles + 逐文件全量解析——改缓存；`SymbolHistory` 全量重写（`data/SymbolHistory.kt:20-31`）。若期 3 建模更合适则移期 3，实施时按工作量裁量。

**依赖**：期 1（双口径基线 + 定型资产集）。

**验收**：决策 10 门槛 + **按口径分列**——首启口径：首启部署耗时较期 1 首启基线下降 ≥50%（预置产物路径达成时；spike 失败走降级方案则如实记录并调整承诺）；稳态口径：二次冷启动零词典编译（logcat 证据）+ 稳态冷启动到首字较期 1 稳态基线不劣化。+ 预置产物损坏场景可自动回退完整部署。+ **direct boot 回归**：锁屏（direct boot 模式）首启不误跳过部署，能正常出字。

**风险**：
- **预置编译产物的兼容性**：librime 编译产物是否可跨设备/跨版本复用（含 checksum 校验语义）需实验验证；失败则退化为「首启后台部署 + 前台可用性提示」，指标相应调整（此为该期最大不确定性，实施期先做 spike 再定方案）；
- **预置产物的存放位置与 SAF 同步的交互**：`UserDataManager.walkLocal`（`UserDataManager.kt:432-438`）对 user 目录全树枚举、无排除逻辑，物化到 `user/build` 的产物会被 SAF 同步推回用户 git 仓库（用户仓库现有 329M `build/` 即此现象）——spike 需对比「物化到 shared/build（只读侧、现成回退通道）」与「user/build + 同步排除」两条路径，选型时把「不污染同步源」作为硬判据；
- 版本指纹短路与用户手动改 shared 目录的交互（用户改文件但指纹未变 → 物化校验兜底）；
- **指纹记录若丢失 per-boot-mode 语义，会重新引入已修复的 direct boot 部署 bug**（`DataManager.kt:29-34` 注释即为该 bug 的修复记录）——范围 1 已显式规避，验收含 direct boot 回归。

### 期 3 · `config-room` Room 配置元数据库（KMP 纪律先行）

**目标**：配置元数据的存储与查询角色从「JSON 文件」迁入 Room（清单、校验、历史）；物化机制改为「Room 清单驱动」；数据层从本期起按 KMP 组织纪律写（决策 7），守护测试断言绑定「可搬运文件清单」。SAF 同步的树枚举保留（见决策 3，不承诺消灭）。

**范围**：
1. 新建 `ConfigDatabase`（组织参照 `data/clip/ClipDatabase.kt` 的 Room 结构，**不照抄其 KMP 违例部分**，见决策 7）：`AssetEntity`（预置资产路径/版本/SHA-256）、`ThemeEntity`（主题 ID/名称/版本/来源）、`SoundEffectEntity`（承接期 2 范围 5 的音效元数据，若未提前做）、`DeployRecordEntity`、`SyncRecordEntity`、`ImportExportRecordEntity`。
2. **KMP 纪律（本期为约束，非结构）**：Entity/DAO/`@Database` 与 DB builder 装配分文件；DAO 全 suspend；新数据层文件零 `android.*`/`java.*` import；不用 Android 独占 Room API；新增守护测试机械强制（kotest 断言，参照 Toolbelt 的 SDK 隔离测试模式）。**守护断言 = 三查的①+②**（import 核对 + 对 androidMain 保留类型具名清单的全文本引用扫描——同包引用不出现在 import，仅查 import 必然漏检，见决策 5/6 的三轮收敛教训）**且绑定「期 3 定义的可搬运文件清单」**——清单即期 4 将搬入 commonMain 的文件集合，清单变更需显式评审；断言的已知局限（静态扫描模式不可穷举）如实记录，终闸是期 4 的 commonMain 独立编译。
3. `LegacyConfigMigrator`（参照 `data/clip/LegacyClipMigrator.kt:39-84`）：首启读取 `checksums.json` + `.sync-manifest.json` + 文件树 → 单事务入库 → 旧文件 retire；失败保留下次重试。
4. `ThemeFilesManager.listThemes`（`data/theme/ThemeFilesManager.kt:15-47`）改查 Room（新主题文件入库时解析一次名字），消掉「为取名字全量解析每个 yaml」。
5. `.sync-manifest.json` 的**存储**迁 Room（`SyncRecordEntity`）：`UserDataManager.sync()` 的 fast path 比对（`UserDataManager.kt:187-207`）与仅对变更候选做哈希、摘要复用的机制（`:313-322`）保持不变，比对载体从 JSON 反序列化改为 Room 查询；`StorageAccess.walk + walkLocal` 树枚举（`:179-182`）保留——远端变化只能枚举发现，这是同步机制的固有成本。
6. `checksums.json` 与 `.sync-manifest.json` 文件机制退役。

**依赖**：期 1（配置集定型）、期 2（优化后的物化路径，避免在旧机制上建模）。

**验收**：决策 10 三层门槛 + 数据层守护测试通过（结构层新增项，断言覆盖可搬运清单）+ 主题列表加载零文件解析 + 改配置→物化→deploy 全链路较期 2 不劣化 + SAF 同步功能回归（枚举与增量同步行为不劣化）+ 迁移幂等（重复启动不重复迁移；损坏旧文件不阻塞、保留重试）+ 卸载重装/覆盖升级两个场景真机验证。

**风险**：
- **Room 与文件系统的一致性**：Room 记录「已物化」但文件写入中断 → 下次启动引擎读到半截文件。缓解：物化完成后再提交 Room 事务（先写文件后记账），启动时抽验清单指纹；
- Room 迁移代码本身成为新的历史包袱（严格限定「一次性迁移」语义，不留双写常驻路径）；
- per-boot-mode 记录语义（`DataManager.kt:29-34`）在 manifest 入库时同样要继承——`SyncRecordEntity`/`AssetEntity` 的键设计包含 boot mode，避免期 2 修过的坑在期 3 复发；
- 守护测试若与实际搬运清单脱节（清单漂移、断言过松）会在期 4 暴露为搬运返工——清单变更走显式评审，期 4 验收核对清单与实际搬运一致。

### 期 4 · `modularize` 模块化拆分、KMP 壳落地与 AAB

**目标**：模块边界物理化；KMP 壳按决策 5/6 落地（commonMain 范围 = 白名单成员：期 3 纪律担保的数据层 + 已核实纯模型，其余留 androidMain）；CI 产 AAB 并验证侧载；裁剪 dynamic feature 诉求为资产包导入更新。

**范围**：
1. 测试护栏补强（拆分前置）：RimeSession 消息协议、Theme golden、DataDiff/物化行为的契约单测。
2. convention plugin 扩展：在现有 7 个 plugin 基础上新增 KMP library / KMP feature 约定（照 Toolbelt 三件套骨架适配：`KmpComposeConventionPlugin`/`KmpFeatureConventionPlugin`/`JvmCliConventionPlugin`，取其「公共依赖装配 + feature 间零互依赖」结构，KSP/Room/Kotest 配置按 Trime 现状改写）；feature 间零互依赖、宿主单点拼接的契约注册模式作为 `:ui` 与 `:app` 的接线约定引入。
3. 按决策 5 的边界与顺序拆模块：`:core:model`（`RimeProto.kt`/`SchemaItem.kt` 进 commonMain，`RimeMessage.kt`/`RimeKeyEvent.kt`/`RimeSchema.kt`/`KeyValue.kt`/`KeyModifier.kt`/`RimeApi.kt`/`RimeLifecycle.kt` 留 androidMain）→ `:core:rime`（JNI 门面/dispatcher/daemon 留 androidMain）→ `:core:data`（期 3 的 ConfigDatabase 数据层 commonMain 化 + `@ConstructedBy` expect/actual 样板 + `BundledSQLiteDriver` androidMain 装配；DataManager/SAF/prefs/主题/剪贴板/SymbolHistory 留 androidMain）→ `:core:native`（librime_jni + 4 ABI）→ `:ui`；`:app` 收口为 IME service + 键盘 + 壳。kodein DI 跨模块注入关系同步梳理。
4. KMP 壳结构守护：commonMain 守护测试（import 核对 + androidMain 保留类型具名清单引用扫描）随拆分迁移到各模块；**commonMain 源集独立编译通过**纳入结构验收（三查终闸）；白名单三查核对记录（逐文件 import/引用扫描/编译验证结论）归档进期 4 的 change 工件；全路线图期内不加第二平台 target。
5. CI：release/nightly 增加 `bundleRelease` 产物；bundletool build-apks 侧载验证纳入发布前检查（`.github/workflows/release-ci.yml:86-88` 现仅收集 APK）。
6. 资产包导入更新：zip 导入 → 校验 → 更新 Room 清单 → 物化 → `exitRime/startRime`（承接决策 8，替代 dynamic feature；gram/predict.db 补件亦走此通道）。

**依赖**：期 3（数据层稳定后再物理拆分，避免边拆边改）。

**验收**：决策 10 门槛（构建层命令切换为全仓库聚合 `./gradlew test`；结构层含 KMP 壳检查与白名单核对记录）+ 模块依赖白名单检查通过 + AAB 侧载安装冒烟通过 + APK/AAB 体积对比记录（AAB 的 ABI splits 为格式固有能力，仅验证产物形态，不归因模块拆分）+ 资产包导入→新词生效全链路真机验证。

**风险**：
- 拆分过程中的循环依赖暴露（`core` 与 `ime`、`data` 与 `ui` 的既有耦合点在拆分时才完全显形），预留一次「只拆不改」的纯移动提交策略（适用面见决策 5：Android 面移动），行为回归靠护栏测试兜底；
- **commonMain 化的核对面仍可能收缩**：白名单经三轮评审已收缩至 `RimeProto.kt`/`SchemaItem.kt` + 期 3 新数据层（决策 6），但三查②的引用扫描与 androidMain 保留类型具名清单的维护本身可能不完备（`RimeSchema.kt:20-27` 即在第二轮复查中仍漏检、第三轮才补上）——终闸（commonMain 独立编译）会把漏检暴露为编译失败，届时该文件退回 androidMain 即可，**不扩大改造**；若期 3 数据层守护测试有漏网，搬运时暴露为返工；
- convention plugin 在 KMP library 模块下的适配（现有 7 个插件主要面向 application，实施期按需扩展）；
- **KMP 壳 × Room KSP 组合的构建摩擦**（KSP2 对 multiplatform source set 的代码生成）——期 3 守护纪律已把搬运面缩到最小，若仍有摩擦，降级方案为 `:core:data` 先以 Android library 落位、KMP 壳化顺延为独立小步（结构验收项相应顺延，不阻塞其余拆分）；
- 全仓库聚合 `./gradlew test` 的执行时长随模块数上升——以构建缓存（尤其 `:core:native` 的 native 缓存隔离）对冲；
- AAB 侧载流程（bundletool + 签名）的自用顺畅度——若验证成本过高，AAB 产物保留为 CI 附带产物，日常分发达仍可继续用 APK，此为可接受的降级。

### 期 5 · `settings-compose` 设置界面 Compose 化（核心页面）

**目标**：交互最复杂的核心设置页面迁移 Compose，建立 Compose 基线与组件库（组件按「纯 Composable + 状态提升」纪律写，为远期跨平台留缝，见决策 6）。

**范围**：
1. 迁移核心 4 页：GeneralSettingsFragment、KeyboardSettingsFragment、SchemaListFragment（`ui/main/settings/schema/`）、ThemeSettingsFragment（`ui/main/settings/theme/`）；配套高频 Dialog（ThemePickerDialog、SoundEffectPickerDialog）按工作量并入。
2. 建立 Compose 主题/组件基线（与现有 YAML 主题体系的关系仅做 UI 层映射，不动键盘渲染）。
3. `AppPrefs`（`data/prefs/AppPrefs.kt`）以 StateFlow/Flow 暴露，衔接 Compose 状态收集（现有 SharedPreferences 存储不变，只换暴露层）。

**依赖**：期 4（在 `:ui` 模块内进行）。

**验收**：决策 10 门槛 + 迁移页面功能对等清单逐项勾验 + 未迁移页面与已迁移页面共存无导航断裂。

**风险**：代码式 PreferenceFragmentCompat 的偏好持久化语义（key 绑定、依赖联动、`SharedPreferences` change 监听，见 `data/prefs/PreferenceDelegateFragment.kt:27-28` 对依赖关系的注释）在 Compose 重建下的等价性——用对等清单逐项验收，不追求一次性全迁。

### 期 6 · `compose-finish` Compose 收尾、候选栏收敛与旧依赖清理

**目标**：设置/主界面全量 Compose，未使用的候选栏模式收敛，旧 View 依赖退役，现代化收官。

**范围**：
1. 剩余页面迁移（清单以实际代码为准）：设置 6 页——AdvancedSettingsFragment、CandidatesSettingsFragment、ClipboardSettingsFragment、ProfileSettingsFragment、ProgressFragment、UserDictionaryFragment（`ui/main/settings/` 及其 `userdict/` 子目录）；主界面 4 页——MainFragment、AboutFragment、LicenseFragment、DeveloperFragment（`ui/main/`）；剩余 Dialog（ColorPickerDialog、DialogSeekBarPreference、EditTextIntPreference）。期 6 页面多为列表管理型/展示型，单页工作量低于期 5 核心页，两期工作量预算大体相当（决策 4）。
2. **候选栏模式收敛**（决策 9）：前置调研确认 flyhe 主题实际生效的候选模式（compact/unrolled/popup 三套，`ime/candidates/`），未使用模式的代码与其独占依赖剔除——flexbox（`app/build.gradle.kts:149`）、bravh（`app/build.gradle.kts:150`）随 unrolled/RecyclerView 路径去留一并裁决；paging 仅评估不承诺（room-paging 与剪贴板库共用，`:140,146`）。
3. 旧依赖退役：`ui/main/settings/` 与 `ui/main/` 的 Preference 系页面全部 Compose 化后，`ui/common/PaddingPreferenceFragment.kt`（全仓库唯一直接继承 `PreferenceFragmentCompat` 处，`:20`）与 `data/prefs/PreferenceDelegateFragment.kt` 随最后一个使用者退役；随后按实际残余使用清退 splitties views-dsl 系列、preference-ktx、appcompat（`gradle/libs.versions.toml:25,42-48`）。

**依赖**：期 5。

**验收**：决策 10 门槛 + `ui/main/settings/` 与 `ui/main/` 的 Preference 系页面全部 Compose 化（PaddingPreferenceFragment 与 PreferenceDelegateFragment 退役）+ 候选栏收敛后真机候选显示回归（实际使用模式行为不变）+ flexbox/bravh 去留裁决有真机依据并记录 + 清退依赖从版本目录移除 + 全设置页真机冒烟。

**风险**：aboutlibraries、对话框等零散 View 组件的 Compose 替代方案成熟度；旧依赖清退触发 R8/打包配置的连带调整；候选栏收敛若误删实际使用的模式，直接影响输入体验——收敛前必须有真机实际使用模式的调研证据，宁缓勿删。

## 5. 全局风险

1. **librime submodule 状态**：当前未 checkout、assets 符号链接悬空（本轮 `find -type l` 实测 **14 个符号链接** + 2 个真实文件；r3 误写 16），期 1 实施第一步必须先恢复 submodule 并确认 librime 版本能力（lua ≥1.16、predict/octagram 模块与 predict.db/gram 的版本匹配、用户版 default.yaml 与 prelude 版 diff）——这是全路线图最大的外部依赖。native 侧模块链接开关已核实为开（`rime_jni.cc:18-26`），降低了该项的部分不确定性。
2. **预置部署产物方案（期 2 核心收益）技术上未经验证**：librime 编译产物跨安装复用的可行性决定期 2 首启口径指标能否达成，实施期以 spike 先行，失败则按期 2 风险栏的降级方案调整指标；spike 还需裁决物化位置与 SAF 同步的交互（`user/build` 会被全树同步推回 git 仓库）。
3. **期 1 基线的「变差」是设计内现象**：62M 内置语料（含 ecdict 27M 硬依赖）的首启部署耗时会显著大于现状通用语料——这正是期 2 的优化对象，期 1 只如实记录；若实测证明 ecdict 令部署/体积不可接受，按决策 2 备选降级。
4. **单人大精力战线**：6 期全靠 openspec 逐期推进，任何一期卡住不应阻塞已交付期别的价值——每期结束都是可独立安装使用的版本，这是分期设计的底线。
5. **指标真实性**：无 instrumented test 基建，性能验收依赖手工实测纪律（双口径基线必须随期 1 提交存档，后续每期同口径对照），避免「口径错配」与「感觉变快」式的验收。
6. **配置两源三源的回归面**：default.yaml 基座两源 + default.custom.yaml 三源（补充决策 11）贯穿期 1-3（期 3 Room 接管清单后仍要保 per-boot-mode 语义），任一期破坏优先级都会造成「配置静默失效」这类难察觉缺陷，验收必须含两源三源回归用例。
7. **KMP 预备的纪律漂移与核对失效**（r5 修订）：白名单三轮收敛暴露了核对方法的演进——r3 类名直觉误纳 RimeDispatcher/剪贴板/SymbolHistory（第 2 轮评审）；r4 import 核对仍漏 `RimeMessage.kt:112-113,181-182`/`RimeKeyEvent.kt:13,15` 的类体引用与 `RimeSchema.kt:20-27` 的同包 JNI 调用（第 3 轮评审：同包引用不出现在 import，静态扫描模式设计本身不可靠）；r5 定为三查闸门 + commonMain 独立编译终闸——**静态核对无论怎么设计都可能漏，编译器不会**。执行纪律：白名单核对记录随 change 工件归档、守护断言绑定具名清单与搬运清单、清单变更显式评审；期内绝不为「跨平台跑通」放宽纪律（无验收路径的投入一律推迟到路线图之后）。

## 6. 路线图之后（显式不承诺，独立评估项）

以下事项均有真实价值但**不在本路线图承诺范围**，避免留无法验收的空壳期：

1. **第二平台 target PoC**（KMP 壳的第一次真实消费）：jvm desktop target 成本最低（Room KMP 支持 JVM Desktop，`:core:data`/`:core:model` 直接复用），以此验证「壳先行」投资——届时只需改 convention plugin + 平台 builder 装配，不需重排架构。
2. **排除成员的改造提取**（决策 6 排除清单的后续，每项完成后按三查流程搬入 commonMain）：`RimeMessage` 拆分（`KeyMessage` 与 `nativeCreate` 的 Key 分支留平台侧，纯消息类型随拆分入 commonMain）、`RimeKeyEvent` 协议化（JNI `external fun` 与 `KeyValue`/`KeyModifiers` 构造拆出平台源集集）、`RimeSchema` 读写分离（`RimeConfig` JNI 读取留平台侧，`Switch` 等纯模型可入 commonMain）、`RimeDispatcher` 纯协程化重写（去 `java.util.concurrent` + timber）、剪贴板库 ClipData 桥接拆分（`ClipboardEntity.kt:8-11` 平台部分移出 Entity）与 paging 依赖的 KMP 化、`SymbolHistory` 入 Room 建模（文件 IO 换 DAO）。
3. **`:ui` 转 Compose Multiplatform**：期 5 组件纪律就位后的独立评估，需权衡 androidx↔org.jetbrains.compose 依赖镜像成本。
4. **候选栏 Compose 化评估**（决策 4 备选）。
5. **真 dynamic feature 重评**（自建分发通道 + `SplitInstallManager`，见决策 8 备选）。
6. **主题解析链跨平台化**（kaml 支持 KMP，但文件树访问是平台面；需先做 okio 化改造评估）。

## 附录 · 性能基线（期 1 实施时填写；所有指标必须标注口径）

| 指标 | 口径 | 期 1 基线 | 期 2 | 期 3 | 备注 |
|---|---|---|---|---|---|
| 部署耗时（deploy Start→Success） | 首启（安装后首次启动） | 待实测 | 目标：较基线降 ≥50% | 不劣化 | logcat 时间戳 |
| 部署耗时 | 稳态（二次冷启动） | 待实测（应为 0/仅增量） | 目标：零词典编译（logcat 证据） | 不劣化 | logcat 证据 |
| 冷启动到首次出字 | 首启 | 待实测 | 随部署改善 | 不劣化 | adb 手工计时 |
| 冷启动到首次出字 | 稳态 | 待实测 | 不劣化（指纹短路收益有限，不承诺百分比） | 不劣化 | adb 手工计时 |
| 主题列表加载文件解析次数 | 任意（缓存生效后） | 待实测 | 缓解（进程内缓存） | 目标：0（Room） | 日志证据 |
| APK 体积 | — | 待实测 | — | — | 期 4 附 AAB 体积对比（ABI splits 为 AAB 固有，不归因模块拆分） |

---

## 修订记录（2026-11-15）：vendor 化与预编译部署（用户方向输入，期 1 验收期间）

期 1 模拟器验收期间用户对架构方向给出系列输入，本节固化为对期 2/3 的修订；正文其余部分保持原评审产物不动，冲突处以本节为准。

### 修订 1：librime vendor 化（期 2 前置动作）

- librime（1.17.0，现 submodule `app/src/main/jni/librime`）及其插件（librime-lua / librime-octagram / librime-predict）与依赖（snappy 等）**源码移入项目**（gitlink 移除、源码快照进 git），脱离上游演进。
- 动机：「设备端构建瘦身」与「配置数据源改造」需要动 librime 源码，submodule 指针形态无法支持。
- 用户本地 `/home/shijy7/Workspace/Github/librime` 为官方 clone（1.16.1+2，无定制），落后于项目 submodule 的 1.17.0——**vendor 基线取 1.17.0**。
- 维护策略：不追上游；GPL 版权与来源声明保留（LICENSE/SPDX 不动）。

### 修订 2：期 2 `startup-fastpath` 升级为「构建期预编译 + 设备端零编译零部署」

原承诺「首启部署耗时较期 1 基线（285s）下降 ≥50%」升级为「**设备端部署趋近于零**」：

- 构建期（宿主机）：用 vendor 后的 librime 源码编主机版编译工具，把全部词典（60M YAML）与配置预编译为 bin/展开产物，随 assets 分发；YAML 源不再随 APK 分发。
- 设备端：librime 构建剔除 deployer / dict-compiler 模块（yaml-cpp 因运行时仍读展开后的 `build/*.schema.yaml` 暂保留，待修订 3 配置结构化后再剔除）；`deploy()` 与维护检查链整个移除，启动链 = assets diff 拷贝 + 引擎加载。
- spike（期 2 第一个任务）：bin 平台无关性验证（x86_64 宿主编 → x86_64 模拟器加载 → arm64 真机加载）与损坏回退路径。
- 重新部署 / SAF 同步 / 自定义方案部署等通用化机制随部署链一并移除（升级场景由 assets diff 覆盖；换机由 userdb 导入导出覆盖）。

### 修订 3：期 3 `config-room` 扩展为「配置结构化 + 引擎数据源改造」

vendor 化解除「librime 无内存挂载 API」的原硬约束：期 3 在 Room 元数据迁移基础上，增加 **librime 配置数据源接口改造**（`build/*.schema.yaml` 展开产物 → 结构化数据 / 内存数据源），完成后 yaml-cpp 从设备端剔除。

### 修订 4：新增独立小 change `drop-setup-wizard`（期 2 之前）

- 移除 Setup 向导（3 步），启动直进主界面；「未启用 / 未设默认」降级为主界面非阻塞提示（启用 IME 为系统强制步骤，无法 app 内完成）。
- 存储模式硬编码 APP_STORAGE；移除 EXTERNAL_SYNC 路径：ProfileSettingsFragment 存储选择 UI、UserDataManager SAF 同步分支、BackgroundSyncWork（WorkManager 周期同步）、StorageAccess 依赖。

### 修订 5：内置资产展示配置裁剪（期 2 任务）

按「实际键盘引用闭包」机械推导保留集（方法同内置集清单）：flyhe.trime.yaml 23 个预设键盘 → 引用闭包；18 个调色板 → 用户常用 1-2 个；215 个 preset_keys → 键盘引用闭包；15 个旧版 style 键清除。词典侧 ecdict 保留（预编译后零运行时成本，easy_en 反查释义依赖）。

### 修订 6：大词典架构结论（记录，不改排期）

实测（Android 16 x86_64 模拟器）：18 个 bin 共约 150MB 经 mmap 映射，进程驻留仅约 8MB——librime Table/Prism 为 mmap 惰性分页，内存与词典规模解耦；几亿词条量级的瓶颈是 APK 体积与编译时长，分别由「资产包按需导入」（决策 8）与「构建期预编译」（修订 2）覆盖。「全内存数据源」仅指 schema/default 配置（KB 级），不含词典。
