# JRime 多平台适配方案设计

> 2026-11-20 · 期二 T4(coder)产出 · **状态:草案,待用户批准后作为期三执行的基线**。
> 本文自包含:读者不需要依赖本期对话上下文。代码事实引用均带 file:line(2026-11-20 工作树实测;T3 在飞文件 desktop/protocol-common/、DesktopServer.kt、test_server.py、Replay.kt 的行号按当日工作树,T3 合入后如有漂移以合入态为准);来自 T0 调查报告(48 facts)与既有文档的结论在引用处注明来源。
> 输入资产:①`doc/architecture-review.md`(规则底座,下称「评审」——本文引用其结论执行点而非抄写);②`doc/refactor/roadmap.md` 期 3/4 KMP 纪律(下称「roadmap」)与 `doc/harness-design.md`(下称「harness 设计」);③researcher T0 调查报告(Path B 数据面/YAML 实证/config 消费面/工具链形态/KMP 抽取面/平台壳约束/机器闸门,Q1-Q7);④战略指令(期一台账 §6 原话「我期望还是能舍弃掉rime的yaml这种低效且低性能的实现方式」;期二用户指令「1可以」= 批准评审 P1×6 整改立项、桥两缺陷按 Path B 时间表裁决);⑤本期已落地事实:T1(e7d8a29,P1-5 core 层解依赖)与 T2(19d91fe,工具链单源)已入库,T3(P1-3 三段抽共享源集 `desktop/protocol-common/`)在飞——本文按其落点收编为 KMP Step 1 的事实。
> 口径承诺:与 roadmap/评审口径一致;存在口径差异处显式标注差异与理由(§1.4)。阻力点如实列出,不做超出本文范围的实施承诺。
> 范围边界:本文只新建本文档;不改任何代码/脚本/CI/golden;不实施 Path B PoC、平台壳、CI 矩阵扩展(仅设计);对其他既有文档的勘误以注记形式写进本文(不改它们)。

## 一、多平台目标架构

### 1.1 总览

目标形态是「一个引擎内核、三类消费形态、一份协议形状」:

- **桌面线(macOS/Linux/Windows)**:平台壳进程(Swift IMK / ibus engine / TSF DLL)↔ 行协议(stdin/stdout JSON 行)↔ 引擎进程(DesktopServer JVM,短期 librime JNI;中期 Path B KotlinEngine)↔ 引擎内核。壳不碰引擎内部,协议是唯一交界面。
- **Android 线**:进程内直连(Compose IME → core/Rime.kt JNI 门面 → C++ 桥 → librime);中期 Rime.kt JNI 门面让位 Kotlin 门面(roadmap 期 3/4 纪律直接受益)。
- **iOS 线**:进程内直调(Keyboard Extension → Kotlin/Native framework 内的 Path B KotlinEngine),行协议不跨进程,但协议形状(采样器)仍复用以保 golden 可比性(§3.4)。
- **协议形状单一事实源**:`desktop/protocol-common/`(T3 落点)——行协议响应行与 golden 轨迹的形状、JSON 编码、就绪探测一处定义,全平台 golden 对齐靠它。

四层里「壳」是平台面(Swift/ibus/TSF/UIInputViewController 各自实现),「协议形状」与「引擎数据访问接口」是跨平台面(KMP 化对象),「引擎内核」短期是平台面(JNI),中期以 Path B KotlinEngine 收敛为 commonMain 主体 + 各平台数据文件访问。

### 1.2 KMP 四步路线(评审 §五 Step 0-3)

| Step | 内容 | 现状 |
|---|---|---|
| Step 0 | 双事实源先对齐再收敛:按 P1-1 把 desktop stub 字段名与 app 词汇表统一(一次 golden 重录窗口);不做任何模块动作(评审 :145) | **待裁决**——与 P1-1 修/不修捆绑(§6.1/§6.3);若裁决不修,词汇表统一顺延到 Step 2 汇合时执行 |
| Step 1 | 抽 `protocol-common` 共享 Kotlin 源集(P1-3 三件套 + P1-4 版本字段 + P2-4 负载扩展落点),五编译入口脚本改 find 参数即可编译;纪律 = 零 `android.*`/`java.*` 平台 import + commonMain 式引用闭包核对(评审 :146) | **在飞**——T3 已落 `desktop/protocol-common/`(三件套,§1.3);P1-4 归 T5(待派);8 DTO 搬入为 Step 1 续段(§1.3) |
| Step 2 | `:desktop-engine` + `:harness` 建 gradle JVM 模块(P2-5 一并消解),`protocol-common` 升 `kotlin("multiplatform")` commonMain(单 android()/jvm() target),与 roadmap 期 4 `:core:model` 汇合为同一模块;五编译入口收敛,commonMain 独立编译成为终闸(评审 :147) | 待启动;时序约束 = harness 设计 H0 明言 gradle 模块化推迟到 H1 再议(harness-design.md:80),H1 收口后启动(评审 :152) |
| Step 3 | Path B 接入:KotlinEngine 作为行协议第二后端(harness 设计 §2 driver 2,H4 验收 L0≥95%,harness-design.md:84);协议模块已引擎无关,壳零改动换引擎;librime JNI 面整体降级为离线工具链(评审 :148) | 待批准本文后立项;前置项见 §2.6 |

### 1.3 protocol-common 现状与 Step 2 升格路径(T0 Q5 + T3 落点)

- **现状(在飞 T3)**:`desktop/protocol-common/`,包 `com.jiaoay.jrime.protocol`,三件套 `EngineSnapshotSampler`(46 行)/`JsonLineCodec`(32 行)/`SchemaReadyProbe`(26 行),共 104 行。编译形态 = 普通 kotlinc JVM 源——plain kotlinc 无 KMP 插件,**expect/actual 不可用**(T0 Q5),现阶段只能是「普通 JVM 源 + 纯 Kotlin 纪律」。纪律 = roadmap 三查中的①(import 核对:零 `android.*`/`java.*` import)与②(签名/类体引用核对),三查③(commonMain 独立编译终闸,roadmap.md:101)在 Step 2 gradle 化前不存在,靠①②纪律维持——这是如实登记的闸门缺口(§7 R5),不是缺陷,是脚本编译期的形态极限。
- **依赖闭包实况(Step 2 前置改造项,实测)**:三件套自身零平台 import,但依赖闭包含两处平台语义:①引擎数据面直调 desktop Rime stub(`EngineSnapshotSampler.kt:3`/`SchemaReadyProbe.kt:3` import `com.jiaoay.jrime.core.Rime`——该 stub 含 `import java.util.ArrayList` 与 16 个 `external fun`(R-doc 实测修正),是平台面,Rime.kt:3/:24-69);②`SchemaReadyProbe` 用 `Thread.sleep`(`java.lang` 语义)。升 commonMain 前需两步依赖倒置:(a) 引擎数据面抽成 commonMain 接口(如 `EngineDataAccess`:getRimeContext/getRimeStatus/getRimeCommit/getRimeCandidates/getRimeRawInput),desktop Rime stub 降为 jvmMain 实现、Path B KotlinEngine 同接口实现;(b) 线程/时序语义换 kotlinx 协程 `delay` 或时钟注入。此倒置不阻塞现纪律形态下使用,列入 Step 2 范围。
- **8 个纯 DTO 可先入(T0 Q5,逐一读过)**:desktop/engine/java/com/jiaoay/jrime/core/ 下 10 文件中纯类型 8 个——CandidateProto.kt(7 行)/CommitProto.kt(5)/CompositionProto.kt(10)/ContextProto.kt(7)/StatusProto.kt(13)/Candidates.kt(17)/RimeResponse.kt(8)/SchemaItem.kt(6),零 import 零 external,与评审 §五候选清单(architecture-review.md:140)逐字吻合。**搬入时点与 P1-1 裁决解耦**:搬入 = 纯移动(类名与构造签名不变——桥按类名 FindClass + 构造器签名绑定,jni-utils.h:176-239 锁签名不锁字段名,字段名不在 JNI 绑定面),golden 不变;字段语义修正(P1-1)= 行为变化,golden 重录。平台面 2 个(Rime.kt:import java.util.ArrayList + 17 external fun;RimeKeyEvent.kt:external fun parse + JNI 伴生)留 desktop/engine。
- **五编译入口接线实况(T3 已完成)**:`harness/smoke.sh:61`、`desktop/test_server.py:80`、`macos/JRimeIME/build.sh:25`、`.github/workflows/desktop-ci.yml:172`(linux leg)与 :269-270(windows leg)均已把 `desktop/protocol-common` 加入 kotlinc 编译源(评审 P1-3 预言的「find 参数加一段」落地)。
- **Step 2 升格**:gradle 化后 protocol-common 升 `kotlin("multiplatform")` commonMain(单 android()/jvm() target);三查③(独立编译)成为终闸;stdlib 解包 hack 五份与 toolchain.properties(T2)随 P2-5 自然退役。

### 1.4 与 roadmap 期 4 的汇合约束与口径差异标注

- **汇合约束(评审 :146-147,roadmap.md:93/:116)**:期 4 `:core:model` commonMain 白名单初始仅 `RimeProto.kt` + `SchemaItem.kt` 两个文件(r5 定案,三轮评审收敛的结论);desktop 8 DTO 与 app 侧 `RimeProto.kt` 同概念不同体(P1-1 字段错位、P2-2 命名分歧,评审 :85-93)。汇合前提 = 先按 app 词汇表统一 desktop stub 字段名(Step 0),且同一模块同一包名收敛,**忌长出第二个模型模块**(评审 :147)。`RimeMessage/RimeKeyEvent/RimeSchema/KeyValue/KeyModifier/RimeApi/RimeLifecycle` 留 androidMain(roadmap.md:274)。
- **【口径差异标注】**roadmap 决策 6(roadmap.md:127)承诺「路线图期内不承诺任何第二平台 target(不加 jvm()/ios())」;本方案 Step 2 的 commonMain 是单 android()/jvm() target。差异成立的说明:jvm() target 恰是 roadmap §6.1「路线图之后」显式列出的第 1 项(「第二平台 target PoC:jvm desktop target 成本最低」);本文档 Step 2 的执行时序按评审阻力点约束放在「H1 收口后」(评审 :152),本就落在 roadmap 六期之后的窗口,不构成方向冲突。处理方式:Android 主线 `:core:*` 在 roadmap 期内继续按决策 6 纪律执行(单 android() target);桌面线 protocol-common 以脚本源集形态(现纪律)并行;Step 2 物理合并且加 jvm() target 的时点,构成对 roadmap 决策 6 节奏条款的显式修订,随本文批准一并确认。
- **排期耦合阻力(如实列)**:Step 2 与期 4 的先后未定——先走桌面线 Step 2 则须做成期 4 可直接收编的形态(同包名 `com.jiaoay.jrime.protocol`/同词汇表),避免期 4 迁移时二次改名(评审 :153)。

### 1.5 决策 D1:protocol-common 落点与升格路径

| 要素 | 内容 |
|---|---|
| 决策 | 共享协议源集落 `desktop/protocol-common/`(Step 1,脚本编译 + 纯 Kotlin 纪律);Step 2 gradle 化后升 commonMain(单 android()/jvm() target)并与 `:core:model` 汇合同一模块 |
| 依据 | T0 Q5(8 DTO 纯类型核实 + 脚本编译闭包形态);评审 P1-3 整改建议与 §五 Step 1(:61/:146);期二台账固定决策 4(T3 落点自决);T3 已按此落点在飞(五入口已接线) |
| 备选 | `harness/common/`(评审另一首选项)——被否:源集语义归属桌面协议层而非 harness 侧;立即 gradle 化跳过 Step 1——被否:harness 设计 H0 明言 gradle 模块化推迟(harness-design.md:80),提前做需先收回该决策 |
| 风险 | ①三查③终闸缺位至 Step 2,靠纪律维持(§7 R5);②依赖闭包含 Rime stub 与 Thread.sleep,Step 2 需依赖倒置(§1.3);③汇合前 desktop 8 DTO 与 app 词汇表漂移存量(P1-1/P2-2)待 Step 0 或 Step 2 收敛 |

## 二、Path B 设计(中期 Kotlin 引擎)

### 2.1 战略叙事校正:YAML 真包袱的定量事实(T0 Q2)

战略指令要求「舍弃 YAML 运行时」。T0 实测校正叙事如下——**方向不变,收益面锚定到事实**:

- **按键路径零 config 读取已经是现状,不是 Path B 的收益**:`engine.cc:167-196` `ConcreteEngine::ProcessKey` 函数体无任何 config 调用;对 gear/*.cc 的 ProcessKeyEvent/Query/Proceed 函数体扫描(找 Config Get*/LoadFromFile/Config::Require)零命中;config 读取全部发生在组件构造函数/LoadConfig:`ascii_composer.cc:57→349-365`(含读 default preset)、`key_binder.cc:238`(ctor)、`editor.cc:202/217`(ctor)、`navigator.cc:67-68`(ctor);switcher 的 config 读取在 `:268-290`(构造)与 `:136`(schema 切换流程),均非逐字热路径。
- **YAML 真包袱两处**:
  1. **session-open 一次性解析**:启动不部署(`rime_jni.cc:40-58` 注释明言「(maintenance) is NOT started here; the startup path must not compile anything」,实测核对一致);session 惰性创建——首个 key/get 触发 `SessionHolder()`→`api->create_session()`(`rime_jni.cc:222-240`,session.h:9-13)→`Service::CreateSession`(service.cc:85-103)→Engine 构造→`Schema(id)`→`Config::Require("schema")`→`SchemaComponent::Create`(schema.cc:17-22,40-42)→yaml-cpp 直读 `build/<id>.schema.yaml`(`config_component.cc:195-201`→config_data.cc:75);解析结果按 config_id 弱引用缓存,**每进程每份 config 只解析一次**(config_component.cc:182-193)。量级 = build/ 全部 yaml 合计 148126B≈145KB(default.yaml 3670B + flypy_xhfast.schema.yaml 73404B/1126 行 + 其余三方案 71052B,wc -c 实测;R-doc 修正:原稿「约 77KB」实为 default+主方案之和,148126B 误记为「其余三方案合计」)——**非每键、非每会话**。
  2. **部署机**:Android 首启部署链 = `RimeStartMaintenance(full_check)`(rime_api_impl.h:65-88)→`Deployer::StartWork` 异步(deployer.cc:106-124)→workspace_update 全量链(deployment_tasks.cc:170-302):每 schema 一次 ConfigFileUpdate(__build_info/timestamps 过期才重建,:439-498)→CreateDictionaryCompileUnit→并行编译(≤4 线程、内存预算=物理/cgroup÷8,compile_unit.cc:38-44,337-345)。dict 编译单元(dict_compiler.cc:80-208):输入 = `<dict>.dict.yaml`(头部→DictSettings,tables 列表 :39-62)+ 编译后 schema yaml(取 speller/algebra 算 prism,checksums);复用判据 = dict_file_checksum/schema_file_checksum 比对(:104-138);输出 = `.table.bin`(table.cc:331-391)+ `.reverse.bin`(:281-296)+ `.prism.bin`(:298-376,Projection 应用 speller/algebra→Script→Darts)。
  - 构建期工具链已落地:`./gradlew :app:precompileRimeData`(RimePrecompilePlugin.kt)= 宿主编译 rime_deployer(:121-163)+ scratch 目录跑 `--build`、mtime 钉 2000-01-01 产物字节稳定(:213,268-274)+ 白名单 17 文件(=default.yaml + 4 方案×4 件)拷入 assets/rime/build(:200-214,300-333);词典源不随 APK(:38-44),词典源 32M 在 app/data/rime/(T0 Q2)。
- **校正结论**:Path B 运行时去 YAML 的真实收益 = ①消掉 session-open 的 yaml-cpp 解析与 77KB 快照;②yaml-cpp/config_compiler/Lua 依赖整体退出运行时(体积、内存与供应链面);③部署机从设备端退役(Path B 运行时无部署概念,预编译产物即数据)。**不是按键延迟**——按键路径已经不碰 YAML。短期 Android fastpath + config 常量化(战略指令)消化的是 Kotlin 侧消费面(§2.3 输出分层),与 Path B 同向且为其输入。

### 2.2 运行时数据面(T0 Q1)

基座:四种数据文件全部 MappedFile mmap 只读加载(`mapped_file.cc:24-44` 实现、`:76-84` OpenReadOnly)。

| 文件 | 魔数/版本闸 | 结构 | 查询原语 |
|---|---|---|---|
| `<name>.prism.bin` | `"Rime::Prism/5.0"`(prism.cc:32-33),format_ < 5.0 拒载(:110-115) | Metadata(prism.h:36-50)= format[32]+dict_file_checksum+schema_file_checksum+num_syllables+num_spellings+double_array_size+double_array+spelling_map(v1.0)+alphabet[256]+max_key_length(v5.0);主体 = Darts 双数组 trie(vendored darts.h)+ SpellingMap(Array\<List\<SpellingDescriptor\>\>,prism.h:25-34,descriptor={syllable_id:int32,type:int32(bit30=is_correction),credibility:float,tips:String}) | exactMatchSearch(:263-275)/commonPrefixSearch(:279-286)/ExpandSearch(按 alphabet BFS,:288-329)/QuerySpelling(:331) |
| `<name>.table.bin` | `"Rime::Table/4.0"`(table.cc:18-19),低于 4.0 拒载(:292-298) | Metadata(table.h:79-92,v2 增 string_table 偏移/尺寸);Syllabary(Array\<StringType\>,str_id:int32 联合,table.h:34)→Index=HeadIndex(按 syllable_id 直接下标)→PhraseIndex 联合 TrunkIndex(排序节点,lower_bound,table.cc:143-150)/TailIndex(LongEntry 带 extra_code);索引码长上限 kIndexCodeMaxLength=3(vocabulary.h:27);Entry={text:str_id,weight:float}(table.h:42-45);全部字符串进 marisa-trie StringTable(string_table.cc:14-71,marisa vendored) | 四级查询 `TableQuery::Walk`(table.cc:152-183);`Table::Query` 对 SyllableGraph 做 BFS 多路径匹配,credibility 累计(table.cc:572-631) |
| `<name>.reverse.bin` | `"Rime::Reverse/3.1"`(reverse_lookup_dictionary.cc:24-25),兼容窗 [3.0,4.0)(:57-63) | Metadata(reverse_lookup_dictionary.h:26-36)= format+dict_file_checksum+dict_settings(String,内嵌 YAML,供 UniTE)+index: List\<StringId\>+key_trie/value_trie 两张 marisa 镜像 | Lookup = key_trie.Lookup(text)→index[key_id]→value_trie.GetString(:73-84);stem 键后缀 `"\x1fstem"`(:30) |
| `.userdb`(目录) | LevelDB 组件 `"userdb"`(dict_module.cc:31),目录扩展名 .userdb(level_db.cc:369-376) | leveldb::DB::Open + 文件锁 LOCK(:64-71,237-239),共享 2MB LRU block cache(:21-25);value 文本 `"c=<commits> d=<dee> t=<tick>"`(user_db.cc:22-26),key=`"code \t phrase"`(user_db.cc:69);另有 TextDb 变体 .userdb.txt/stabledb/tabledb(.txt,user_db.cc:58-63、table_db.cc:57-87) | leveldb 原生 get/iterate |

**装载与查询链**(Path B 必须等价复刻的语义):

- `Dictionary::Load` = 主 table.Load + prism.Load + 可选 packs(dictionary.cc:457-481);资源解析 `CreateDeployedResourceResolver`,类型 {prism,.prism.bin}/{table,.table.bin}(dictionary.cc:489-491),解析顺序 = user_data_dir/build(staging)优先、fallback prebuilt_data_dir(service.cc:178-186);`Exists()` = prism 文件 + 主 table 文件都存在(dictionary.cc:442-445)。
- 主链 `Dictionary::Lookup(SyllableGraph)`→每张表 `Table::Query`→结果按 end_pos 分组进 DictEntryCollector(dictionary.cc:308-339;lookup_table 含 extra_code 递归匹配 :114-145);chunk 合并用堆序(dictionary.cc:80-106):exact-match 优先 → remaining_code 短者优先 → weight(credibility+entry.weight)大者优先;`entry.weight = e.weight − log(1e8) + chunk.credibility`(dictionary.cc:212-213)。
- 副链 `LookupWords` = prism exact/ExpandSearch→SpellingAccessor→各表 QueryWords(dictionary.cc:377-427)。

**Path B Kotlin 最小读取集(结论)**:每方案 `<name>.prism.bin` + `<name>.table.bin`(+packs 表)+ `<name>.reverse.bin`;userdb(LevelDB 目录)按需。**不需要**:default.yaml/schema.yaml(常量层替代,§2.3)、词典 YAML 源(已不随 APK)、lua 脚本(§2.4 等价实现)、`.gram`(内置集本就缺,行为照实对齐)、predict.db(同缺件)。

**Kotlin 实现注记(userdb 是数据面最重一块,设计默认待实施前评估)**:LevelDB 文件格式(Manifest/compaction/snappy 压缩)在 JVM/Kotlin 无第一方实现;候选 = 成熟 Java 移植(如 iq80 leveldb 一类)/Kotlin 原生(只读)实现/延后。**默认 = PoC 阶段不支持 userdb**(harness 语料 userDict 即 `empty` 隔离口径,harness-design §3;一用例一进程一 fresh user 目录天然成立),调频场景(userdict tags)单列为后期里程碑;中期再裁决移植引入。此为设计默认,实施前需独立评估,不进本文批准范围。

### 2.3 schema 常量生成:混合形态 (c) 为基线(T0 Q4)

| 要素 | 内容 |
|---|---|
| 决策 | 混合形态 (c):`rime_deployer` 编译产物 yaml(语义已合并展开)为唯一输入,其上做**无指令平 YAML 的薄抽取器**生成 Kotlin 常量;常量生成步不依赖 librime(rime_deployer 只出编译产物,由 `:app:precompileRimeData` 现有 gradle 任务承担) |
| 依据 | ①语义保真零漂移——编译产物里 `__include/__patch/__append/__merge` 四指令(config_compiler.h:43-46)、`"+"` 下标追加(config_compiler.cc:214-218)、`.custom.yaml` 自动补丁(auto_patch_config_plugin.cc:23-30)、import_preset 预设合并、6 个 config 插件链(core_module.cc:23-32)已被 rime_deployer 展开,抽取器只见平 YAML;②(b) 独立解析器直读 YAML 源必须复刻 config_compiler.cc 本体 602 行 + config_types/config_data 语义,每个偏差都是真实输入行为 bug(T0 Q4,置信度高);③管线已通——precompileRimeData 产物字节稳定(mtime 钉扎、白名单 17 文件);④与评审「常量层即 Path B 的输入」(architecture-review.md:170)完全同构,(c) 是 (a) 的自然延伸而非新管线 |
| 备选 | (a) 全 librime 链:常量生成绑「能跑 rime_deployer 的机器」,仍需自写抽取器——被 (c) 覆盖;(b) 独立解析器:摆脱 librime 但语义复刻成本高、漂移风险实证充分——否 |
| 风险 | ①抽取器只覆盖约定字段清单(下),字段清单演进需同步;②用户目录 custom patch 的动态定制不在常量内(Path B 用户定制面另行设计,本文不承诺);③Windows leg 已验证 rime_deployer 可独立编译(desktop-ci.yml:223 只编 rime_deployer target) |

**输出分层(短期 vs Path B 目标集)**:

- **短期(Android fastpath,librime 仍在运行时)**:常量只覆盖 **Kotlin 侧消费面**——`RimeSchema.kt:20-38`(switches[] 的 name/options/reset/states 四字段 + speller/alphabet)+ `SchemaItem(id,name)`(SchemaItem.kt:7-10,来自 getRimeSchemaList,rime_jni.cc:349-353);全 app `RimeConfig.openConfig/openSchema` 消费方 grep 仅 RimeSchema.kt 一处(T0 Q3)。C++ 侧消费面(key_binder 71 bindings+具名键 schema :280-364、punctuator full/half_shape+import_preset :531+内联符号表 :534-990、recognizer patterns :997-1018、speller algebra/alphabet/delimiter :1039-1051、editor/navigator/selector bindings(default.yaml:69-79/schema:413-418,1033-1038)、preedit_convert_rules :429-461)**短期仍由 librime 运行时读 build/*.schema.yaml**,不在短期常量化范围。
- **Path B 目标集**:C++ 侧消费面全部转 Kotlin 等价实现,常量集 = 编译产物对 Path B 消费面剪枝后的全量(仅 Q3 清单字段,73KB 源料可压到常量集)。`Schema::FetchUsefulConfigItems`(schema.cc:24-38)另读 schema/name、menu/page_size(=9,(:399-412))、alternative_select_keys、page_down_cycle——抽取器字段清单须含。
- 生成器落点建议:随 `:app:precompileRimeData` 之后的新 gradle 任务(读 assets/rime/build 的编译产物,产出 Kotlin 常量文件);实施属期三工作项,本文不定接口。

### 2.4 lua 挂点 38 处:Kotlin 等价实现清单与分期(T0 Q3)

事实底座:flypy_xhfast 编译产物含 **12 lua_processor + 15 lua_translator + 11 lua_filter + 1 lua_segmentor = 39 个 lua 挂点**(R-doc 实测修正,原稿 38 系计数偏差;engine/processors 21 项 = predictor 列首 + 12 lua + ascii_composer/recognizer/key_binder/speller/selector/navigator/punctuator/express_editor 等,:133-154;segmentors 21,:155-176;translators 25 = predict_translator 列首 + punct + 15 lua + reverse_lookup + script + 6 table,:177-202;filters 18,:114-132);脚本实体在 `assets/shared/lua/`(29 个 .lua,528K);librime-lua 静态编入并 require(rime_jni.cc:18-25;`app/src/main/jni/cmake/Rime.cmake:6` RIME_PLUGINS=lua/octagram/predict,实测核对)。

**脚本实体盘点**(ls 实测,29 个):calculator / cand_selector / charset / chinese_lunar / chinese_number / cn_en_custom / cold_word_drop(+cold_word_records 状态文件)/ commit_history / date_time / easy_en / easy_navigator / flypy_fixed / flypy_fuzzy / flypy_help_menu / flypy_switcher / keybinder / laTex / pair_punct / pin_word(+pin_word_record)/ reduce_emoji / select_char / shortcut / shortcut_config / symbol_menu / word_append_space / word_autocaps + lib/。

**清单形态与缺口如实标注**:38 挂点按脚本实体多对多归属(一个脚本可挂多类组件);脚本→挂点逐个语义映射未做(T0 已知缺口:「librime-lua 脚本语义逐个分析」未展开)。故本节给出**分类框架与分期策略**,逐个映射列为 Path B 实施期任务(每映射一个、随 golden 对照验收)。

**分期策略(按行为风险分批,与 harness 语料 tags 对齐)**:

1. **第一批:触发器型 translator(纯函数、无按键拦截)**——calculator/date_time/chinese_lunar/chinese_number/laTex/shortcut 等:输入触发词、输出候选,等价实现风险最低;对应 harness tags:`trigger`。
2. **第二批:候选流 filter(候选流变换)**——charset/reduce_emoji/cn_en_custom(触发器型 translator)/easy_en 模糊匹配与造词(easy_en.lua:2;**勘误:原稿「easy_en 注释链」基于 roadmap 期 1 调查态——single-schema 移除后英文候选已不带中文注释,easy_en 归 translator 类**;flypy_switcher.lua:35,127-129 围绕 overwrite_comment 工作,roadmap 决策 2)等;对应 tags:`english`/`emoji`。
3. **第三批:按键拦截 processor + segmentor(状态机交互,最难点)**——pair_punct/cand_selector/select_char/easy_navigator/commit_history/flypy_switcher/keybinder/word_append_space/word_autocaps/flypy_fixed/flypy_fuzzy 等;对应 tags:`punct`/`makeword`/`radical`。
4. **长尾裁剪原则**:等价实现前逐个核对「内置集实际生效引用」——存在脚本被引用但行为面空转的可能;未生效挂点不实现,以 golden 对照兜底(golden 是引擎级 oracle,未生效行为自动对齐,harness/README.md「oracle 是引擎级的」节)。

**纪律约束**:lua 等价实现进度与 H4 验收(L0≥95%)联动——harness 设计 §7 语料中 punct/trigger/radical/english/makeword/emoji 等 tags 依赖 lua 行为,验收按 tags 分组一致率矩阵(harness 设计 §5)如实报告,**不因 lua 未完成而放宽 L0 口径**;未覆盖 tags 的用例如实标注为「Path B 未达成场景」而非口径调整。

**octagram/predict 补充**:两者已编入已 require(grammar_module.cc:17),但 assets 无任何 .gram(octagram.cc:21,28 按 grammar/language 找;schema :267-270 引用 zh-hans-t-huayu-v7-bgw)、无 predict.db(predictor/db :424-428,predict_db 打开仅报错 predict_db.cc:15-21,主链路不受影响)→ 运行时无语言模型/联想,与 roadmap 决策 2 一致;Path B 同样缺件降级,golden 已照实录制缺件现状(harness 设计 §6),**无额外等价实现工作**。

### 2.5 opencc 数据面归属(设计决策点 → §6.3 待用户裁决)

背景:simplifier 四组 opencc_config(zh_simp/zh_trad/zh_hk/zh_tw,schema :1104-1127);数据在 `assets/shared/opencc/`(实测 37 文件合计 1,324,201B≈1.26MB,R-doc 修正:原稿 516K 系 roadmap 期 1 对用户配置目录的调查值);opencc 词典 native 直读文件(opencc.cc:19 SimpleConverter,roadmap 决策 3:opencc 词典必须落盘)。Path B 运行时去 librime 后,简繁转换链需 Kotlin 等价(转换算法 + 数据消费)。候选与判据见 §6.3(默认:**独立数据文件**)。缺口如实标注:opencc 数据格式细节 T0 未深查(仅确认文件存在与消费入口);无论候选 A/B,opencc 转换算法本身都需要 Kotlin 等价实现。

### 2.6 PoC 前置项:syllabifier.cc 生产侧精读(T0 已知缺口)

T0 只查到消费侧:`Table::Query` 消费 SyllableGraph(table.cc:572-631);**graph 生产侧(syllabifier.cc/algebra 的运行时角色)未展开精读**。SyllableGraph 构造是 KotlinEngine 的第一个运行时组件(输入字母串 + alphabet + max_key_length(prism metadata),输出音节图),其切分语义(匹配策略/credibility 初值/音节边界)直接决定 prism/table 查询正确性。**要求:Path B PoC 的第一个任务 = syllabifier.cc 全文精读 + 语义笔记落盘(沿用 T0 报告格式),随后才写 Kotlin 骨架**。此为 PoC 立项的显式前置条件,不满足不启动编码。

### 2.7 决策记录 D2-D4

| # | 决策 | 依据 | 备选 | 风险 |
|---|---|---|---|---|
| D2 | schema 常量生成 = 混合形态 (c) 基线(rime_deployer 编译产物 + 无指令薄抽取) | T0 Q4;评审 :170;precompileRimeData 管线已通 | (a) 全 librime 链 / (b) 独立解析器(均见 §2.3 表) | 抽取器字段清单演进同步;用户 custom 动态定制不在常量内 |
| D3 | lua 分期 = 触发器 translator → 候选 filter → 状态机 processor,长尾按实际生效裁剪;与 H4 tags 一致率联动 | T0 Q3(38 挂点分布);harness 设计 §5/§7 | 一次性全量实现——被否:工作量未知(语义未逐个分析)且 H4 起步目标无法渐进验收 | 逐个语义映射缺口(§7 R6);空转挂点误实现浪费 |
| D4 | userdb = PoC 阶段不支持(empty 隔离口径),中期再裁决 Java 移植引入 | T0 Q1(LevelDB 格式重);harness 设计 §3(empty 即标准口径) | iq80 类 Java 移植即时引入 / Kotlin 只读实现 | 调频场景(userdict tags)长期缺口;实施前需独立评估,非本文批准范围 |

## 三、平台壳路线(T0 Q6)

**总则**:壳只说行协议,引擎实现零感知——现 macOS 壳即此形态,`DesktopServer.kt:18-21` 头注释明言「the shared core behind every platform shell(the macOS IMK client today; a Linux ibus adapter and a Windows TSF server later all speak this same line protocol over their transport)」。每平台壳必须实现的接口面五件:①输入法宿主键事件 → key repr 映射;②引擎进程管理(spawn + ready 探测 + watchdog;watchdog 语义见 DesktopServer.kt:75-90,壳死则引擎 600s 后自杀);③行协议七请求 `ready / k <repr> / sel <i> / commit / pg next / pg prev / opt <n> <bool> / exit`(DesktopServer.kt:23);④ServerReply 字段级解码(EngineClient.swift:10-26 现形);⑤候选 UI 与上屏。

### 3.1 macOS(基线,已实现)

现状:Swift IMK 壳四文件(`macos/JRimeIME/swift/main.swift`、`InputController.swift`、`EngineClient.swift`、`KeyCodeMapper.swift`)+ Info.plist;打包 `build.sh` 六步(jlink java.base 运行时 + engine classes + librime_jni.dylib + rime 资产进 .app,ad-hoc codesign,build.sh [2/6]-[6/6]);EngineClient.swift:37-82 spawn JVM DesktopServer、ready 探测 :76;打包源已含 protocol-common(build.sh:25)。
已知缺陷(评审在案,处置见 §6):P1-1 ascii 字段错位(StatusProto 实收 is_disabled,golden 双录因此仍 IDENTICAL——两侧一致地错)、P1-2 扩展 B 乱码(harness/README.md:52-55)、P2-3 错误行被静默吞(architecture-review.md:95-99)。

### 3.2 Linux ibus

- **形态**:ibus engine 可以是任何会说 ibus D-Bus 协议的进程(常规 Python gi/C/Vala,T0 标注既有知识,置信度中);组合方式 = ibus engine 进程以 macOS 壳同款模式 spawn DesktopServer JVM 子进程、管道行协议转发。仓库零 ibus 代码;CI 已定义消费形态——linux-engine leg 产 jrime-engine-linux-x64.zip(librime_jni.so + engine classes + rime 资产,desktop-ci.yml:163-177),注释明言「The ibus shell consumes exactly this package」(:121-122,实测核对)。
- **壳必须实现**:IBusComponent/IBusEngine 注册(component XML + D-Bus)、keysym→key repr 映射(注意 ibus keysym 空间与 macOS 键码不同,需自有映射表;桥侧 key.cc 的 `RimeKeyEvent.parse` 语义可参照,macOS 侧 KeyCodeMapper.swift 是同类先例)、commit-text/preedit 上报、引擎进程生命周期。
- **已知风险(如实列)**:Wayland 下 D-Bus session 差异;用户机器 JVM 存在性(jlink 运行时是否随包未决——随包则包体增 ~40MB 级,需实测);每引擎实例一进程的内存开销;X11/Wayland 双栈候选窗口实现差异。

### 3.3 Windows TSF

- **形态**(T0 标注既有知识,置信度中):TSF text service 是 COM in-proc DLL,载入目标应用/ctfmon 进程,UI 线程 = 宿主线程,DLL 需与宿主位数一致;JVM 不能 in-proc——可行组合 = **原生 TSF DLL 薄壳,spawn/连接独立 JVM DesktopServer 进程**(与 macOS 同构)。仓库零 TSF 代码;CI 注释「The future TSF DLL + server pair consumes exactly this package」(desktop-ci.yml:186-187,实测核对),windows-engine leg 产 librime_jni.dll + engine classes(:236-276)。
- **壳必须实现**:ITfTextInputProcessor 全套 COM 接口、注册表 + TF_InputProcessorProfiles 注册、按键→repr、候选 UI(TSF 自绘候选窗口或 UI 候选窗口二选一,实施期裁决)、进程代理管理。
- **已知风险(如实列)**:多宿主进程 → 多引擎实例(每应用一 JVM 内存不可行,需 broker 设计——单 broker 多会话或共享引擎进程,Windows 特有设计点,实施期单独设计);AppContainer/UWP 应用限制子进程 spawn;P2-9(Windows leg 无本地闸门,CI 为首次实跑点,§5.2/§7);ClangCL 工具链 ABI 已有一次实战修复先例(85402e6:Release 静态库链接,Debug runtime ABI mismatch)。

### 3.4 iOS(Kotlin/Native)

- **形态**(T0 标注既有知识,置信度中):键盘 = Keyboard Extension(UIInputViewController),沙盒、禁网、扩展内存上限约 60-70MB(随 iOS 版本浮动);**无 JVM → 唯一可行引擎 = Path B KotlinEngine 经 Kotlin/Native 编译为 framework**(Xcode embed + import)。行协议不跨进程(iOS 为进程内直调);cinterop 仅在保留 librime C API 时需要(Path B 恰好移除);线程模型 = KN 新内存模型(1.7.20 起默认)支持跨线程共享,UI 调用需回主线程;共享数据走 App Group。
- **约束传导(本设计最关键的一条)**:protocol-common 若含 `java.io`/`Thread` 等 JVM 语义或平台类型则无法进 KN 编译——**反证 §1.3 纯 Kotlin 纪律的必要性**;现状 `SchemaReadyProbe` 的 `Thread.sleep` 与 Sampler 对 Rime stub 的直调即 Step 2 依赖倒置(§1.3)的具体动因。
- **已知风险(如实列)**:扩展内存上限对 table.bin mmap 的适配——mmap 惰性分页有利(roadmap 修订 6 实测 Android 侧 150MB bin 常驻仅约 8MB),iOS 同机制预期可行但**未验证**;禁网沙盒无网络类风险;KN framework 体积(引擎 + 数据访问层)需实测;App Group 配置与键盘扩展审核面。
- **时序**:iOS 排在 Path B 数据面定型之后——其唯一可行引擎就是 Path B,先行无意义(§3.5)。

### 3.5 平台壳分期建议

桌面三壳(macOS 已有 → Linux ibus → Windows TSF)优先,三者共享同一引擎包消费形态(CI linux/windows leg 已对齐产出);iOS 最后(Path B 定型后)。每壳动工前按本节接口面清单出独立任务单;Windows 壳动工前 P2-9 冒烟步应已落地(§5.2)。

## 四、行协议演进

### 4.1 版本握手(P1-4,已拍板,基线落笔)

| 要素 | 内容 |
|---|---|
| 决策 | ready 响应附加两个字段:`"proto":1`(协议版本,**整数,只增不改义**)与 `"engine":"<VERSION>"`(引擎实现版本,初值 = DesktopServer `VERSION = "1.0.0-desktop"`,DesktopServer.kt:29);壳侧在 ready 交换处断言 `proto <= 支持值`,超版即报错退出而非带病运行 |
| 依据 | 评审 P1-4(architecture-review.md:63-67);期二台账固定决策 4(字段形态拍板);CI 已把引擎核心作独立制品分发(desktop-ci.yml:179-184 linux/:278-283 windows),壳与引擎核心各自演进时错配将静默降级——Swift 解码侧全是 `?? 默认值`(EngineClient.swift:13-18),错配表现为「看起来能跑但行为不对」 |
| 备选 | 语义化版本字符串跨协议传输——不可比较,弃;握手放首键交换——ready 本就是首交换点,无需另设 |
| 风险 | golden 全量字节失配 → T5 任务单已含 golden 重录窗口(期二台账固定决策 1);旧壳对新引擎由断言拦截;新壳对旧引擎(响应缺 proto)的兼容策略实施时定死并在 harness-design.md §4 补协议词汇表小节(评审 P2-4 建议同款) |

**执行载体**:T5(待派,依赖 T3+T4);落点 = protocol-common `EngineSnapshotSampler` 单点改造(DesktopServer 与 Replay 两侧同时生效,单一事实源的价值即在此);Swift 壳断言点 = EngineClient.swift:76 附近 ready 交换处。

### 4.2 负载扩展方向(P2-4,演进标注)

- **方向**:响应附加 `"hl":<highlightedIndex>`、`"cursor":<preeditCursorPos>`、cands 三元组化(带 label)——preeditCursor 与候选高亮在桥上现成(objconv.h:84/:106)却被丢弃;Swift 侧 preedit 光标只能置尾(InputController.swift:117),IMKCandidates 高亮同步因此空实现(:87-89)(评审 :101-105)。
- **落点**:protocol-common 单一实现处(P1-3 抽出后一处改两侧生效);字段级解码天然向后兼容。
- **约束**:负载扩展必须与 golden 重录同窗(轨迹字节变化);若与 §6.3 词汇表裁决同窗触发语料变更,合并执行避免两次重录。P2-4 未立项(期二台账 §1),本文仅落演进方向,不进本期承诺。

### 4.3 错误行结构化(P2-3,未立项,演进标注)

现状:错误行为非规则行 `{"error":...}`(DesktopServer.kt:65-71 引擎未挂载、:134 未知请求、:136-138 异常兜底),缺规则响应其余字段;Swift 解码无 error 访问器,error 行解码为 handled=false/raw=""/preedit="",错误被无声翻译成「引擎认为没有组合」(评审 :95-99)。未立项(期二台账 §1:P2 系列未立项);演进方向标注:若做,错误行同时带 proto 字段(与 4.1 解码兼容性一并解决)、ServerReply 增加 error 访问器、壳对非 nil error 显式 NSLog、test_server.py 对含 error 响应提前抛断言。不进本期承诺。

### 4.4 请求词汇表二选一(→ §6.3 待用户裁决)

现状两套并存:server 侧 `k <repr> / sel <i> / pg next / pg prev / opt <n> <bool>`(DesktopServer.kt:23 及 :100-130 七请求分派)vs harness 侧 `k:<repr> / sel:<i> / pg:next / pg:prev / opt:<n>=<bool>`(Replay.kt:18-19 及 :81-94 分派)——同语义两语法,无文档声明等价关系(评审 :103)。候选、客观判据与默认路径见 §6.3(默认:server 空格式为准)。

## 五、CI 矩阵扩展

### 5.1 现状(`.github/workflows/desktop-ci.yml`,workflow_dispatch 手动触发 :6-7,无 push/PR 触发)

| leg | runner | 关键步 | 产物 |
|---|---|---|---|
| android-apk | ubuntu-26.04 | make debug | jrime-android-apk |
| macos-app | macos-26 | 宿主 librime 构建 → `:app:precompileRimeData` → smoke.sh → macos build.sh | jrime-ime-macos |
| linux-engine | ubuntu-26.04 | 宿主构建 → precompile → build-bridge.sh → kotlinc 打包 | jrime-engine-linux-x64.zip |
| windows-engine | windows-2025 | ClangCL 只编 rime_deployer target(:223)→ precompile → build-bridge.ps1 → 打包 | jrime-engine-windows-x64.zip |

T2 已落地:CI 读 `script/toolchain.properties` 并做 SHA256 校验(linux :167-170 sha256sum -c;windows :255-267 Get-FileHash)。T3 已接线:两个 engine leg 打包源含 `desktop/protocol-common`(:172/:269-270)。引擎包内容 = 桥二进制 + engine classes + rime 资产(assets/shared 源 + assets/rime/build 编译产物)。

### 5.2 新增面

1. **P2-9 windows 冒烟步**(评审 :131-135):windows-engine leg 末尾加一步 PowerShell 冒烟——用打包产物(librime_jni.dll + engine classes + staged assets)启动 DesktopServer,断言 ready 响应含 schema 字段即可(不必全流程);中期与 P2-5 gradle 化合并后由 `:harness` JVM 测试矩阵直接覆盖 Windows leg。
2. **Path B PoC 的 golden diff 步:不新增 leg**——H4 验收跑在现有 macOS/Linux leg(harness 双 driver 同协议,harness 设计 §2;linux leg 已可产引擎包,只差 harness 装配),属既有 leg 的测试步扩展。
3. **iOS KN 产物 leg(可行性与边界如实列)**:前提 = KMP ios target 存在(Step 2 之后 + Path B 数据面定型);形态 = macos runner 跑 Kotlin/Native framework link 任务(gradle `link*Framework`)+ zip artifact 上传。**边界**:①GitHub macos runner 有 Xcode 可产 framework,但键盘扩展行为无法自动化验证(无真机/签名链路),leg 只证「KN 编译产物可产出」;②framework 体积与 60-70MB 内存上限实测只能在真机;③起步判据 = protocol-common 纯 Kotlin 纪律已升级为独立编译闸门(否则 leg 第一天就红——`java.*`/JVM 语义直接编译失败)。
4. **Step 2 gradle 化后的收敛(P2-5)**:五入口脚本收敛为 gradle 调用(脚本保留薄包装以兼容既有 CI 步骤名),toolchain.properties(T2)与 stdlib 解包 hack 五份自然退役;CI kotlinc 步换 gradle 调用。
5. **观察项(不进本期承诺)**:desktop-ci.yml 仅 workflow_dispatch 手动触发、无 push/PR 闸门——本文不改触发策略(改 CI 触发属行为变更,需独立立项),列为 CI 共享面风险(§7 R7)。

### 5.3 决策 D5

| 要素 | 内容 |
|---|---|
| 决策 | CI 扩展按「步扩展优先、leg 新增有前提」执行:P2-9 冒烟步(Path B PoC 前落地)、Path B golden 步进现有 leg、iOS KN leg 三前提满足后才建(§5.2.3) |
| 依据 | 评审 P2-9 建议;harness 设计 §2 双 driver 同协议;T0 Q6 iOS 约束传导 |
| 备选 | iOS leg 与 Path B PoC 并行启动——被否:KN 编译闸门未就绪,leg 只会持续红 |
| 风险 | windows 冒烟步本身无本地预演环境(Windows 本地闸门缺位是 P2-9 的因),首次落地仍靠 CI 迭代 |

## 六、P1 整改与 Path B 排期关系

### 6.1 P1-1/P1-2 修与不修的裁决建议(候选 + 判据 + 默认 → §6.3)

背景:两缺陷均为桌面桥在案项——P1-1 stub 字段错位(objconv.h:115-122 传参序 vs StatusProto.kt:3-13 声明序,stub.isAsciiMode 实收 is_disabled 等 6 处错位)、P1-2 NewStringUTF 捣毁 4 字节 UTF-8(jni-utils.h:59-64,CJK 扩展 B 乱码);**修则必伴随 golden 重录**(台账固定决策:重录与转换窗口互斥);Path B 落地则桌面桥作废、两缺陷零成本关闭(评审 :163-164,期二用户指令「桥两缺陷按 Path B 时间表裁决」)。

| 候选 | 内容 | 代价 |
|---|---|---|
| **A 现修** | P1-1:stub 字段按 objconv 传参序重排并更名(与 app 侧 RimeProto.kt:45-55 词汇表一致,顺带完成 P2-2)+ 新增 ascii 翻转断言用例(评审 :49);P1-2:JString 改按字节构造(一处改 jni-utils.h:59-64 覆盖 objconv 全调用点,直调点 rime_jni.cc:350,370/opencc.cc:20/config.cc:77 各换一次辅助函数)+ README「Android 不受影响」判定真机复核(评审 :55) | 两次 golden 重录窗口(可与 T5/P1-4 的重录合窗,实际一次窗口内串行完成);macOS 产品化(对外分发)前置缺陷清零 |
| **B 不修** | 随 Path B 桥作废零成本关闭,零重录窗口 | macOS 壳带缺陷:ascii 模式显示错(P1-1)+ 扩展 B 字符乱码(P1-2);自用可忍,对外分发不可忍 |

**判据(核心一条,量化可执行)**:

- **Path B PoC 完成日 早于 桌面线对外可用日 → 选 B(不修)**;
- **桌面线对外可用日 早于 Path B PoC 完成日 → 选 A(P1-1 必修——ascii 显示正确性是产品化前置;P1-2 按扩展 B 用户面裁量:用户词库/opencc 数据机械扫描无扩展 B 字符则可延,有则必修)**。

辅助定义:①「桌面线对外可用」= macOS .app 交付本人以外用户(含任何形式分发);②「Path B PoC 完成」= harness H4 首次达成主路径场景 L0≥95%(harness 设计 §8,可机器判定);③两缺陷若修,与 T5(P1-4)golden 重录合窗执行以省窗口(期二固定决策 1 已含 T5 重录窗)。

**默认路径:不修(候选 B)**——与期二台账 §5 需裁决项 1 的默认一致;**复审触发器 = 用户启动 macOS 产品化(对外分发)时,P1-1 提前转必修**。

### 6.2 投入面矩阵(放心投入面 / 按 Path B 裁决面 / 与 Path B 无关应修面)

| 面 | 项目 | 状态(2026-11-20) | Path B 后命运 |
|---|---|---|---|
| 放心投入(Path B 后全额存活) | P1-3 采样器/编码器/就绪探测抽取 | **T3 在飞**(desktop/protocol-common/ 三件套,五入口已接线) | 存续——单一事实源随 KotlinEngine 继续服务 |
| 放心投入 | P1-4 版本握手 | T5 待派(依赖 T3+T4) | 存续——壳/引擎分离分发的前提设施 |
| 放心投入 | P2-4 负载扩展(hl/cursor/cands 三元组) | 未立项(演进标注 §4.2) | 存续 |
| 放心投入 | KMP Step 0-1(词汇表对齐 + protocol-common) | Step 0 待裁决(随 P1-1);Step 1 在飞 | 存续——Step 2/3 的直接前置 |
| 放心投入 | P1-5 core 层解依赖 | **已完成**(T1,e7d8a29:Rime.kt 零 ime/data import,三步注入化) | 存续——期 4 与 Path B 双向受益 |
| 按 Path B 时间表裁决 | P1-1 stub 错位 | 未修(§6.1) | 随桌面桥作废关闭;若先修须 golden 重录窗口 |
| 按 Path B 时间表裁决 | P1-2 NewStringUTF | 未修(§6.1) | 桌面侧随桥作废关闭;README「Android 不受影响」判定无测试锚点,若真机复核证伪则转 Android 项存续 |
| 与 Path B 无关应修 | P1-6 工具链单源 | **已完成**(T2,19d91fe:script/toolchain.properties + SHA256 校验五处归一) | 已闭环 |
| 与 Path B 无关应修(未立项) | P2-5 构建二元收敛 / P2-6 kotlinc 缓存判据 / P2-7 README 过时诊断 / P2-8 build.sh 吞输出 / P2-9 windows 冒烟 | 未立项(P2 系列,期二台账 §1) | 不随 Path B 消失(P2-5 与 Step 2 合并执行,P2-9 进 CI 扩展) |
| 与 Path B 无关应修(未立项) | P2-3 错误行结构化 | 未立项(§4.3) | 协议可见性,建议随 P1-4 后续窗执行 |

### 6.3 待用户裁决事项汇总(三件,集中给候选 + 客观判据 + 默认路径)

> 本节集中承载对用户有实质影响的三项裁决;正文 §2.5/§4.4/§6.1 为对应技术上下文。裁决不阻塞本文其余部分作为基线执行(默认路径即基线)。

| # | 事项 | 候选 | 客观判据 | 默认路径 |
|---|---|---|---|---|
| 1 | **opencc 数据面归属**(§2.5) | A 独立数据文件:Path B simplifier 等价组件按 opencc 数据格式直读 `assets/shared/opencc/`;B 常量化:编译期把四组转换词典预转成 Kotlin 常量/自定义 bin | ①opencc 词典是静态数据,四组配置(zh_simp/zh_trad/zh_hk/zh_tw)由 schema 引用、用户可经 custom 开关启用组——开关逻辑归常量,词典本体不随开关变;②无论 A/B,opencc 转换算法都要 Kotlin 等价实现——A 只加「读数据文件」,B 额外要生成管线 + 自定义格式;③体积实测 1.26MB(37 文件,R-doc 修正:原稿 516K 系旧调查值),mmap/流式读皆可,常量化收益 = 省解析器,成本 = 再造格式 + 生成管线 | **A 独立数据文件**(数据归数据、常量归配置,B 生成管线收益低);缺口如实标注:opencc 数据格式 T0 未深查,裁决 A 后 PoC 前补一次 opencc 数据面精读(与 §2.6 syllabifier 精读并列前置) |
| 2 | **行协议请求词汇表二选一**(§4.4) | A server 词汇 `k <repr>`(空格分隔)为唯一标准,harness corpora(`k:<repr>`)由 Replay 解析层适配;B harness 词汇 `k:<repr>` 为标准,DesktopServer 改收冒号格式(动协议线 + macOS 壳发送侧) | ①线上消费者数量:server 词汇现消费者 = macOS 壳(EngineClient.swift)+ test_server.py,**未来 ibus/TSF 壳全部接入 server 词汇**;corpora 是内部资产(12 用例,重生成成本最低);②语义无歧义(repr 形态如 `Control+a`,两种分隔均无歧义,判据不区分);③评审 P2-4 建议方向「另一侧改别名并在 harness-design.md §4 建协议词汇表小节」 | **A(server 空格式为准,corpora 归 Replay 适配层)**;随下一次协议变更(P1-4/P2-4 的 golden 重录窗)一并执行,避免单独重录 |
| 3 | **P1-1/P1-2 排期**(§6.1) | A 现修(golden 重录窗口 + macOS 产品化前置清零);B 不修(随 Path B 先行零成本关闭) | **Path B PoC 完成日 早于 桌面线对外可用日 → B;反之 → A**(P1-1 必修,P1-2 按扩展 B 用户面裁量);两个日期均可机器/流程判定(PoC 完成 = H4 首次 L0≥95%;对外可用 = .app 交付他人) | **B 不修**;复审触发器 = 用户启动 macOS 产品化时 P1-1 提前转必修 |

## 七、风险清单

| # | 风险 | 影响 | 缓解/处置 | 出处 |
|---|---|---|---|---|
| R1 | **Swift 无 KMP 消费通道**:macOS 壳解码器(EngineClient.swift:10-25)是手写镜像,不引 Kotlin/Native 无法共享 | 协议演进时 Swift 侧需人工同步(第四份拷贝,评审 :60) | 靠协议版本字段(P1-4)与 golden 断言对齐;若日后要消除镜像,评估 KN 产出 framework 头(重依赖,不进本路线承诺) | 评审 :151 |
| R2 | **桌面 gradle 化与 harness 设计 H0 时序冲突**:H0 明言「gradle 模块化推迟到 H1 再议」(harness-design.md:80),提前做需先收回该决策 | Step 2 无法立即启动 | 时序约束 = H1 收口(语料/golden 基建稳定)后启动 Step 2(评审 :152);本文已在 §1.2 落笔 | 评审 :152 |
| R3 | **期 4 排期耦合**:`:core:model` 在期 4 落地;桌面 Step 2 若先走,须做成期 4 可直接收编形态(同包名/同词汇表),避免二次改名 | 两线汇合时返工 | 口径差异已显式标注(§1.4);汇合前提三件(Step 0 词汇表对齐/同一模块同一包名/忌第二模型模块)写入 Step 2 验收 | 评审 :153;roadmap.md:93/:116 |
| R4 | **平台壳风险(逐平台)**:Linux = Wayland D-Bus 差异/JVM 存在性/每实例一进程内存;Windows = 多宿主 broker 设计/AppContainer 限制子进程/ClangCL ABI;iOS = 60-70MB jetsam 与 mmap 适配未验证/KN framework 体积未测/禁网沙盒 | 各壳实施期返工或体验缺陷 | 每壳动工前独立任务单;Windows 壳前置 P2-9 冒烟步;iOS 前置 Path B 定型与真机内存实测 | §3.2/§3.3/§3.4(T0 Q6) |
| R5 | **闸门缺口**:①windows-engine leg 本地无闸门(DLL 构建/打包 CI 首跑点);②protocol-common 无 commonMain 独立编译终闸(三查③缺位至 Step 2,靠①②纪律维持) | Windows 产物零验证发布;平台类型泄漏晚发现 | ①P2-9 冒烟步(§5.2.1);②Step 2 依赖倒置后独立编译即终闸;期内核对记录随 change 工件归档(roadmap 决策 6 执行纪律) | T0 Q7;T0 Q5;roadmap.md:101/:127 |
| R6 | **已知调查缺口**:syllabifier.cc 生产侧未精读(§2.6)/opencc 数据格式未深查(§2.5)/lua 38 挂点语义未逐个分析(§2.4) | Path B PoC 编码起点不稳;lua 工作量不可估 | 三项分别列为 PoC 前置精读与实施期逐个映射任务;不满足不启动对应编码 | T0「未查/建议」节;§2.4/§2.5/§2.6 |
| R7 | **并发会话与 CI 共享面**:CI 仅 workflow_dispatch 手动触发、无 push/PR 闸门,CI 是共享资源(多会话争用 runner 与缓存);五编译入口经 T2/T3 改动后的一致性依赖 toolchain.properties 单源;并发会话在途面(app UI 层)与本文档设计无耦合但同仓并行 | 合入顺序冲突;CI 产能争用 | 脚本文件改动按台账串行纪律(T3 与 T2 同脚本文件已串行);CI 触发策略变更需独立立项(本文不改);并发面登记备查 | T0 Q7;期二台账 §1 |
| R8 | **userdb(LevelDB)Kotlin 实现成本**:文件格式含 Manifest/compaction/snappy,无第一方 JVM 实现 | 调频场景(userdict tags)可能长期缺口 | PoC 阶段 empty 隔离口径(§2.7 D4);中期独立评估 Java 移植引入 | T0 Q1 |
| R9 | **两桥缺陷若最终不修,存在 golden 固化乱码风险**:P1-2 乱码已被 golden 固化,日后若修桥而不重录,golden 会反向「钉死」乱码 | oracle 契约语义失效 | 裁决触发器(§6.1)显式绑定:任一日改判修桥,重录窗口同步立项;golden 重录与修复永不分离(台账固定决策) | 评审 :54 |

## 八、附录

### 8.1 机器闸门六条(命令可直接复制执行;时长量级为经验估计未实测计时,T0 置信度中)

| # | 闸门 | 命令 | 前置 | 通过判据 |
|---|---|---|---|---|
| 1 | 桌面协议端到端 | `python3 desktop/test_server.py`(macOS host) | `harness/build/librime_jni.dylib` 存在(先跑 smoke;缺则 test_server.py:111 直接报错退出) | 末行 `DESKTOP SERVER TEST PASS` 且 exit 0(:148);断言链 = ready.schema==flypy_xhfast → "nihc" 四键全 handled → raw=="nihc" → cands[0][0]=="你好" → sel 0 commit=="你好" → 新组合 "k n" raw=="n" → exit(:130-146) |
| 2 | golden 双录 | `python3 harness/run_cases.py record --golden harness/golden` → `python3 harness/run_cases.py record --golden /tmp/golden2` → `python3 harness/run_cases.py diff --golden harness/golden --other /tmp/golden2` → `python3 harness/run_cases.py verify` | 无(dylib 由同流程构建) | diff 末行 `diff: IDENTICAL` exit 0(run_cases.py:100-105);verify = `verify: ok=12 fail=0` exit 0(:131-132);12 用例(baseline.jsonl);隔离 = 一用例一进程一 fresh user 目录(:43-72) |
| 3 | 桌面 smoke | `./harness/smoke.sh` | build-host 已填充 + JDK + clang(README.md:16-18) | 末行 `SMOKE TEST PASS`(Smoke.kt:45)且 exit 0(set -e);内容 = 6 个桥 .cc 编链成 dylib + kotlinc 全量编译 harness/java + desktop/protocol-common + desktop/engine/java + JVM 跑 load→startup→deploy 回调→schema 列表→exit |
| 4 | app 单测 | `./gradlew :app:testDebugUnitTest` | Android SDK | BUILD SUCCESSFUL、0 failures;基线 193(2026-11-19 台账口径,W1 后并发会话基线 181→193;后续验收按开工时实测快照,不少于 193 且不静默丢测试) |
| 5 | spotless | `./gradlew spotlessCheck` | 无(ktlint 1.8.0,target **/*.kt + **/*.kts,build.gradle.kts:16-22) | BUILD SUCCESSFUL;自动修复 `./gradlew spotlessApply` |
| 6 | CI 四 leg | `.github/workflows/desktop-ci.yml` 手动触发(workflow_dispatch :6-7) | GitHub runner | 四 leg 全绿 + artifact 上传成功(retention 90 天);T2 校验步(linux sha256sum -c / windows Get-FileHash)内含 |

### 8.2 术语表

| 术语 | 含义 |
|---|---|
| Path B | 路线二:自研 Kotlin 运行时引擎,运行时只读 prism/table/reverse bin + schema 生成 Kotlin 常量,yaml-cpp/config_compiler/Lua 全链退出运行时;librime 降级为离线编译工具链(rime_deployer) |
| 壳(shell) | 平台输入法宿主侧进程:macOS Swift IMK / Linux ibus engine / Windows TSF DLL / iOS Keyboard Extension;只说行协议(桌面)或进程内直调(iOS) |
| 行协议 | 壳 ↔ DesktopServer 的 stdin/stdout JSON 行协议;请求七种(ready/k/sel/commit/pg/opt/exit),响应一行一 JSON |
| 协议形状 | 响应行与 golden 轨迹的共享 JSON 结构;单一事实源 = protocol-common 的 EngineSnapshotSampler |
| protocol-common | `desktop/protocol-common/`,P1-3 抽出的共享 Kotlin 源集(三件套),KMP Step 1 落点,Step 2 升 commonMain |
| 三查闸门 | roadmap 期 3/4 commonMain 白名单闸门:①import 核对 ②签名/类体引用核对(绑定 androidMain 保留类型具名清单)③commonMain 独立编译终闸(roadmap.md:101) |
| commonMain / androidMain / jvmMain | KMP 源集分层;commonMain 编译看不到平台源集声明,泄漏引用直接编译失败 |
| 汇合约束 | desktop protocol-common 与 roadmap 期 4 `:core:model` 汇合同一模块、同一包名、同词汇表,忌第二模型模块(评审 :147) |
| oracle / golden | harness 的 librime 引擎级基线;golden 双录逐字节 IDENTICAL 是行为等价契约 |
| L0/L1/L2 | harness diff 语义分级:L0(逐键一致,必须)/L1(排序差异,warn)/L2(提示差异,warn)(harness 设计 §5) |
| H0-H4 | harness 阶段:H0 冒烟产品化 → H1 回放 → H2 60 用例 → H3 diff 报告 → H4 KotlinEngine 接入(L0≥95%)(harness 设计 §8) |
| fastpath | 短期 Android 启动链优化(期 2 预置产物、零设备端编译) |
| 常量层 | 编译产物 schema 经薄抽取器生成的 Kotlin 常量;即 Path B 的输入(评审 :170) |
| KN / framework | Kotlin/Native 与其 Xcode 产物(iOS 唯一可行引擎载体) |
| TSF / broker | Windows Text Services Framework(COM in-proc DLL)/ 多宿主下共享引擎进程的代理设计 |
| 五编译入口 | harness/smoke.sh、desktop/test_server.py、macos/JRimeIME/build.sh、desktop-ci.yml linux leg、windows leg 五处 kotlinc 调用(经 T2 单源、T3 接线后仍并存,Step 2 收敛) |
| 机器闸门六条 | §8.1 表:test_server / golden 双录 / smoke / app 单测 / spotless / CI 四 leg |

### 8.3 Path B 数据格式速查

| 产物 | 要点 |
|---|---|
| prism.bin | 魔数 `Rime::Prism/5.0`,<5.0 拒载;Darts 双数组 trie + SpellingMap(descriptor: syllable_id/type(bit30=is_correction)/credibility/tips);metadata 含 alphabet[256] 与 max_key_length;查询 = exact/prefix/ExpandSearch(BFS)/QuerySpelling |
| table.bin | 魔数 `Rime::Table/4.0`,<4.0 拒载;Syllabary(字符串表)→HeadIndex(音节直下标)→TrunkIndex(排序,lower_bound)/TailIndex(extra_code)四级查询;索引码长 ≤3;Entry={str_id,weight};字符串全在 marisa StringTable;Query 对 SyllableGraph BFS,credibility 累计 |
| reverse.bin | 魔数 `Rime::Reverse/3.1`,兼容窗 [3.0,4.0);marisa key/value 双 trie + StringId index;Lookup = key→id→value;stem 键后缀 `\x1fstem`;dict_settings 内嵌 YAML(供 UniTE) |
| userdb(LevelDB 目录) | 组件名 `userdb`,目录 `.userdb`;key=`"code \t phrase"`,value=`"c=<commits> d=<dee> t=<tick>"`;文件锁 + 2MB LRU;TextDb 变体为 .txt Tsv |
| 编译产物 yaml | build/<id>.schema.yaml(主方案 73KB/1126 行)+ default.yaml(3.6KB);按键路径不读,session-open 每进程弱引用缓存解析一次;Path B 由常量层替代 |
| mmap 基座 | 四类 bin 全部 MappedFile 只读(mapped_file.cc:24-44/:76-84);内存驻留与词典规模解耦(roadmap 修订 6:150MB bin 常驻约 8MB) |

---

### 章节自查清单(验收对照)

- [x] 八节不缺项:一(多平台目标架构)/二(Path B 设计)/三(平台壳路线)/四(行协议演进)/五(CI 矩阵扩展)/六(P1 与 Path B 排期关系)/七(风险清单)/八(附录)。
- [x] 决策四要素:D1(§1.5)/D2-D4(§2.7)/D5(§5.3)/握手决策(§4.1)各含 决策/依据/备选/风险;opencc、词汇表、P1-1/P1-2 三项显式进 §6.3 待用户裁决(候选+客观判据+默认路径)。
- [x] P1-1/P1-2 排期建议含候选(A 现修/B 不修)+ 判据(Path B PoC 完成日 vs 桌面线对外可用日)+ 默认(不修 + 复审触发器)。
- [x] 关键事实引用带 file:line:lua 38 挂点(§2.4,T0 Q3)、三 bin 格式(§2.2/§8.3,T0 Q1)、闸门六条(§8.1,T0 Q7)、YAML 定量(§2.1,T0 Q2)、平台壳接口面(§3,T0 Q6)。
- [x] 口径差异显式标注:§1.4(Step 2 jvm() target 与 roadmap 决策 6 节奏条款);阻力点如实列:§1.4/§7 R1-R3(Swift 无 KMP 消费通道/桌面 gradle 化与 H0 时序/期 4 排期耦合)。
- [x] 已收敛决策按基线落笔注明依据:混合形态 (c)(§2.3)/握手字段形态(§4.1)/protocol-common 落点(§1.3/§1.5);T1/T2 已入库、T3 在飞收编(§1.3)。
- [x] 勘误注记(不改其他文档):harness/README.md:56-57「疑似桥内状态缓存,待查」已过时——真因 = desktop stub StatusProto 字段错位(评审 P2-7 已在案,本文按 §6.1 处置,不动 README)。
