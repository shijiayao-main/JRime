# expert_team_state.md — JRime Java→Kotlin 全量转换 + 整体架构 review
- 任务根目录: /Users/jiao/Workspace/Program/Personal/JRime
- 调度模式: 模式二(orchestrator 派单工具不可用,主对话代执行)
- 波次预算: 8 | 已用: 3(W3 进行中,S3+S4 并行) | 熔断线: 波次>8 或同一子任务返工 2 次
- 原始需求: "补充个优化项, 看看能不能把java文件全都换成kotlin, 再进行下整体架构的review"

## 1. 任务分解总表
| # | 目标 | 专家 | 依赖 | 状态 |
|---|------|------|------|------|
| S1 | 转换可行性调查 | researcher | 无 | 完成(W1 验收通过) |
| S2 | 实施转换:13 Java→Kotlin+5 文件配套 | coder | S1 | 完成(终态确认:S3 转换面通过+S4 P0=0) |
| S3 | 独立验证取证(机器闸门实跑) | verifier | S2 | 完成(W3 验收:转换面通过,FAIL 条目归属并发会话) |
| S4 | 转换 diff 三维评审 | reviewer | S2 | 完成(W3 验收通过,P0=0;P1 转 S6 约束;P2×6 入台账) |
| S6 | 单 commit(**白名单式 31 条路径 add**,严禁 git add -A,不 push) | coder | S3+S4 | 完成(612e669,31/31,零污染;**已推送 origin/main**) |
| S5 | 整体架构 review → doc/architecture-review.md(输入含台账 §6 战略指令) | reviewer | 无(与 S6 并行) | 进行中(W4,续跑收尾) |

## 2. 各波次任务单原文
- W1/S1、W2/S2、W3/S3+S4:均已完成,收敛为总表行(核心结论见 §3)。
- W4/S6(coder):在飞,任务单要点:31 条路径白名单单 commit(13 .kt 新增+13 .java 删除+5 修改:smoke.sh、test_server.py、macos/JRimeIME/build.sh、desktop-ci.yml、harness/README.md),排除清单=并发会话全部改动+台账+doc/architecture-review.md;步骤含污染防御(status 复核→显式 add→cached 复核≠31 则 reset 重试→commit→show 复核→零卷入确认);不 push;消息 conventional:"refactor(desktop): convert engine and harness Java sources to Kotlin"(body 四行)。
- W4/S5(reviewer):在飞,任务单要点:全仓整体架构评审,落盘 doc/architecture-review.md(唯一可写文件);七维:分层与边界/KMP 化路径(core DTO+行协议编解码候选 commonMain)/重复逻辑(DesktopServer vs Replay 采样)/协议演进性/构建打包一致性(五处编译入口收敛)/已知风险(NewStringUTF 4 字节、stub 错位,与 Path B 关系)/命名一致性;用户战略指令(去 YAML 化)为方向输入,不把 librime 冻结当终态;P0/P1/P2 逐条 file:line+可执行建议;SUMMARY: P0=<x> P1=<y> P2=<z>;验收看证据链完整性,计数不触发返工。

## 3. 累计验收结论
- W2/T3:通过。证据指针:desktop/protocol-common/com/jiaoay/jrime/protocol/{JsonLineCodec,EngineSnapshotSampler,SchemaReadyProbe}.kt 三件套;import 面仅 Rime×2+零平台 import(orchestrator 独立 grep);golden 12/12 IDENTICAL+git status golden 空(coder);test_server/smoke/build.sh 6/6 实跑过;重复体计数消费方=0;commit 5124958 恰 9 文件(3A+6M)零卷入。已知登记:protocol-common→Rime 门面直调=Step 2 依赖倒置前置项(设计文档 §1.3)。
- W2/T4:通过。doc/multiplatform-design.md 342 行八节全在(orchestrator 独立 grep 标题核验);待裁决三件集中 §6.3;§1.4 roadmap 口径修订显式;未 commit 待 R-doc。
- W4/T5(主对话代执行,执行背景=orchestrator 第三次 provider 饱和停滞):通过。证据指针:test_server.py 新增握手断言(proto==1/engine=="1.0.0-desktop")实跑 PASS;EngineSnapshotSampler.kt 增 optional proto/engine 参数(null=不发射,Replay 调用点零变化);DesktopServer.kt ready 分支传 PROTOCOL_VERSION/VERSION+P2-2 注释一行收编("minus the step field")+PROTOCOL_VERSION 常量;EngineClient.swift ServerReply 增 proto/engine 字段+start() 握手断言 proto>1 拒绝(只增不改义,低版本容忍);desktop-ci.yml windows 哈希比较 expected 侧 ToLower(R1-P2-3 收编)。闸门:test_server PASS(含新断言)/golden diff IDENTICAL(**无需重录实证**:T5 只动 ready 响应、共享采样器默认参数下 Replay 输出零变化,任务单预期的"重录窗口"经实证免除)/smoke PASS/spotless PASS/macos build.sh 全流程 PASS(含 Swift 握手断言编译+codesign)。commit c571d1c(恰 5 文件,未 push)。
- W1 commit 闭环:T1=e7d8a29(恰 5 文件:Rime.kt M/RimeDaemon.kt M/RimeInlinePreeditFormatter.kt A/RimeOpenCCBridge.kt A/InlinePreeditFormatter.kt A)、T2=19d91fe(恰 5 文件:toolchain.properties A/smoke.sh M/test_server.py M/build.sh M/desktop-ci.yml M),均 show 复核零卷入未 push;执行方=主对话;台账增量随 cadcb3b 落库。T1/T2 verify 证据=主对话降级取证(合并树四闸全过);终树 verify 由 W3 合并 V 覆盖。
- W2/R1:通过。证据指针:git show e7d8a29/19d91fe 独立对照+javap/哈希实测;Rime( 构造点全仓唯一(RimeDaemon.kt:54);core 三越层引用零残余;五消费方属性读取失败模式全部响亮(set -e/KeyError/bash -e/Get-FileHash throw);properties 纯 LF;注释 URL 与四下载点逐字一致;SHA 复核闭环(properties:7-8→四下载点)。P0=0 P1=0 P2=4(①BuildConfig 残余期 4 收编 ②RimeDaemon 形态备案 ③windows 哈希大小写归一缺口 ④SHA 校验不含缓存命中路径,随 P2-6)。预存反向依赖 AppPrefs.kt:14→ime.core.InlinePreeditMode 备案期 4。
- W1/S1:通过。证据指针:jni-utils.h:176-239(桥依赖面=12 FindClass+12 签名,无 GetFieldID);kotlinc 实证三类样本;rime_jni.cc:115-123(证伪 status 缓存假设,真因=stub 字段错位);5 处 javac 调用点。
- W2/S2:自查通过,待独立取证。SUMMARY: files=13/13/5,四件套自查 PASS(test_server PASS/golden 双录 IDENTICAL 12:12/smoke PASS/单测 176 全绿),javap 实证 12 个桥依赖签名与 jni-utils.h:176-239 逐字节一致。范围裁定:macos/JRimeIME/build.sh 改动属允许集("不改 macos/"条款系起草缺陷,与 stdlib 解包自决冲突;该文件为 S1 清单内 javac 点 build.sh:22-24);S4 复核其 diff 最小性与 jlink 行未动。
- 基线漂移::app:testDebugUnitTest 175→176(并发会话新增 1),后续验收按 176 全绿。
- W3/S3:转换面 PASS(FAIL 条目归属并发会话,不构成 S2 返工;S2 终态=完成)。证据指针:test_server exit 0 五断言全过;golden 12/12 逐字节 IDENTICAL(README §H1 流程+cmp -s 独立复核+golden 零改动);smoke PASS(真实回调);单测快照 181/181 绿;spotless 转换面过;桥签名 11/11 ALL MATCH(jni-utils.h:176-239 机械抽取 vs desktop/build 与 harness/build 双产物 javap)。运行窗口 01:29:16-01:32:00 三采一致;判定 5/6 限定快照,2/3/4 对当前树仍有效。
- 计数与基线修正(采信 S3 实测):桥依赖面 11 条(此前 12 系 S1 口径偏差;Candidates 含 $Paged/$Bulk 两个构造器);单测基线 176→181(并发 72ac5a0 新增 GestureThresholdsTest 5 例)。
- W4/S6:通过。commit 612e669b5082385bbe8f30e3b51523c410f88579(短 612e669),恰 31 条路径(13 .kt 新增/13 .java 删除/5 修改,647+/603-),排除清单零卷入,未 push(ahead 1,父 commit=并发会话 72ac5a0)。偏差 1 条采信:GestureThresholdsTest.kt 已随并发 72ac5a0 入库,工作树新出现 KeyPopupTest.kt 未跟踪+GboardKeyboard.kt 修改——均不在白名单、零卷入。任务 1(转换+提交)完整闭环。
- 观察项更新:并发会话在途集合动态变化(GestureThresholdsTest.kt 已随 72ac5a0 提交;GboardKey.kt spotless 违规待其收口;新增 KeyPopupTest.kt/GboardKeyboard.kt 在途)——本任务全程零触碰,后续会话注意。
- 待主人操作项:commit 612e669 尚未 push,是否 push 由用户决定(ahead 1)。
- P1 转记(写入 S6 任务单硬约束):提交边界白名单——工作区混入并发会话改动(app/.../data/prefs/AppPrefs.kt、ime/compose/GboardKey.kt、ime/compose/KeyboardPanel.kt、doc/surface-plan.md 4 个 M + 未跟踪 app/src/test/.../GestureThresholdsTest.kt、.team-surface/、expert_team_state.md);S6 仅按 31 条路径白名单式 add(13 .kt 新增+13 .java 删除+5 修改),严禁 git add -A。
- P2 待办登记(6 条,来源 S4,不阻塞,随最终报告进"剩余风险/建议下一步"):①strip→trim Unicode 空白边差(DesktopServer.kt:102,116,122、Replay.kt:38,77,78);②kotlinc zip 下载无 SHA256 校验(smoke.sh:51、test_server.py:47、desktop-ci.yml 两段);③test_server.py:25 缓存判据不校验执行位、:55 rename 半成品残留 FileExistsError;④build.sh:15 smoke 失败被 >/dev/null 掩盖;⑤windows Expand-Archive 解 stdlib 不在本地闸门内,CI 为首次实跑点(剩余风险);⑥KOTLIN_VERSION=2.4.21 字面量在 5 处脚本重复。

## 4. BLOCKED 与返工记录
(无)

## 5. 需裁决项
(无在途。三项编译器/打包/形态裁决已于 W2 自决回填,见下。)

## 6. 战略指令与独立工作项
- 【战略指令,2026-11-18,用户原话】"我期望还是能舍弃掉rime的yaml这种低效且低性能的实现方式"——已确认登记,作为 W5/S5 架构评审输入(评审不把 librime 冻结当终态;Path B 落地则桌面桥作废、桥待修项随之关闭);不进入 S4(避免混淆本轮转换评审基准)。路线:短期=Android fastpath+config 读取常量化;中期=路线二 Kotlin 引擎(运行时只读 prism/table/reverse bin+schema 生成 Kotlin 常量,yaml-cpp/config_compiler/Lua 全链退出运行时)。
- 【独立工作项】修复桌面 stub 字段错位(StatusProto isAsciiMode 实收 is_disabled 等)——修复必伴随 golden 重录,与转换互斥;若 Path B 落地桌面桥作废,本项随之关闭。登记日 2026-11-18。

## 固定决策(Plan,已定)
1. 测试策略:仅跑既有回归四件套,golden 逐字节 IDENTICAL 即转换的等价契约。
2. 机器闸门:以 S1 Q7 清单为准,禁止静态自扫替代闸门实跑。
3. 硬约束三原则:零 C++ 桥改动 + DesktopServer stdout 字节行为不变 + golden 双录 diff IDENTICAL;冲突时登记需裁决项并转 BLOCKED(类别:需用户决策);日常小决策取最小合理默认并在任务单风险节注明。
4. 自决回填(W2):kotlinc 直调(2.4.21,脚本自动获取+缓存);kotlin-stdlib 解包进 engine 目录(-cp 单路径);Rime 用 object+@JvmStatic。

## 【终态收尾】2026-11-18 — 任务完成
### 1. 任务分解总表(终态)
S1 完成(10/10 题取证)| S2 完成(612e669)| S3 完成(转换面全 PASS,FAIL 条目归属并发会话)| S4 完成(P0=0,P1 转 S6 约束)| S6 完成(31/31 白名单零污染,已推送)| S5 完成(architecture-review.md 184 行,P0=0 P1=6 P2=9,经第二评审者逐行复核)。
### 2. 波次记录
W1=S1;W2=S2;W3=S3+S4 并行;W4=S6+S5 并行;累计 4/8 波,6 张单+0 返工单(另:主对话派 CI 修复轮 R1-R4)。
### 3. 验收要点(终态)
test_server exit 0 / golden 12:12 IDENTICAL(cmp 复核)/ smoke PASS / 单测快照 181 绿;桥签名 javap 11/11(口径修正:原记 12 系 S1 偏差);S4 P0=0;S6 白名单零卷入;S5 报告接管复核。
### 4. BLOCKED 与返工(终态)
返工 0 单;BLOCKED 0 次;S5 会话中断 1 次(第二评审者接管,非协议熔断)。
### 5. 需裁决项(终态)
3 项已自决回填(kotlinc 直调/stdlib 解包/object 形态);"P1×6/P2×9 整改立项"移交待主人操作项。
### 6. 剩余风险清单(终态)
见 doc/architecture-review.md P1×6/P2×9 + CI windows leg 首跑验证 + trim Unicode 边差 + kotlinc 校验与版本字面量 + 桥两缺陷按 Path B 裁决 + 并发会话在途项收口。
### 7. 待主人操作项(权威清单,登记 2026-11-18)
1) push 612e669 —— 已由主对话按用户既定节奏完成(72ac5a0..612e669..34a75b0 均已推送);2) doc/architecture-review.md 提交与否;3) P1×6/P2×9 整改立项裁决(Path B 时间表决定桥缺陷是否修复);4) 并发会话 GboardKey.kt spotless 收口(归其会话)。

---

# 期二(2026-11-19 开工):多平台适配方案设计 + 第一批执行波次
- 任务根目录: /Users/jiao/Workspace/Program/Personal/JRime(期一终态记录以上,本段为追加,不改写历史)
- 调度模式: 模式二(派单工具不可用,主对话代执行;SendMessage 缺失,回传=主对话中途来信或增量续派)
- 波次预算: 8 | 已用: 0(W1 派单中,3 单在飞) | 熔断线: 波次>8 或同一子任务返工 2 次
- 用户指令(2026-11-19 原话): "1可以"(=批准架构评审 P1×6 整改立项,桥两条缺陷按 Path B 时间表裁决)、"验收我后面自己做"(macOS IME 真机验收用户自理,不阻塞)、"专家团启动"(=多平台适配方案的输入法:整体设计+开始执行)
- 本期目标: 设计方案落 doc/multiplatform-design.md(KMP commonMain 抽取/Path B/平台壳/行协议演进/CI 矩阵/P1 与 Path B 排期关系/风险清单);第一批可落地工作项开工,每波可验证

## 1. 任务分解总表
| # | 目标 | 专家 | 依赖 | 状态 |
|---|------|------|------|------|
| T0 | Path B 数据面/schema 常量化输入/KMP 抽取面/平台壳约束/机器闸门清单调查(只读) | researcher | 无 | 完成(W2 验收通过,抽验 3/3 命中;48 facts 已随 T4 转交) |
| T1 | P1-5 core 层解依赖(Rime.kt 零 ime/data import,三步注入化) | coder | 无 | 完成(W1 验收 ①-④ 通过;commit e7d8a29,已闭环) |
| T2 | P1-6 工具链单一事实源(script/toolchain.properties + SHA256 校验,五处字面量归一) | coder | 无 | 完成(W1 验收 ①-⑤ 通过;commit 19d91fe,已闭环) |
| R1 | T1+T2 两 commit 三维评审(e7d8a29+19d91fe) | reviewer | 无(基于 commit) | 完成(W2 验收:P0=0 P1=0 P2=4,焦点 (a)(b)(c) 全过) |
| T3 | P1-3 采样器/JSON 编码器/就绪探测三段抽共享源集 desktop/protocol-common/ | coder | T2 commit(同脚本文件串行) | 完成(W2 验收:硬闸 golden 12/12 IDENTICAL+零改动,commit 5124958 恰 9 文件) |
| T4 | 设计方案文档 doc/multiplatform-design.md(输入=T0 结论+架构评审+战略指令) | coder | T0 | 完成(342 行落盘未提交,R-doc 后白名单提交) |
| R2 | T3(commit 5124958)三维评审 | reviewer | T3 | 进行中(W3) |
| R-doc | 设计文档 doc/multiplatform-design.md 六维评审 | reviewer | T4 | 进行中(W3) |
| V合并 | T1+T2+T3 三 commit 合并闸门实跑取证 | verifier | T1,T2,T3 | 进行中(W3) |
| T5 | P1-4 行协议版本握手(ready 响应加 proto/engine 字段+Swift 壳断言) | coder | T3,T4,V合并 | 完成(W4 主对话代执行:commit c571d1c;五闸全过+golden 无需重录实证) |
| V3+R3 | T5 闸门实跑+评审 | verifier+reviewer | T5 | 待派(W5) |
- 本期不执行(设计文档裁决后处置): P1-1 stub 错位 / P1-2 NewStringUTF(按 Path B 排期裁决)、Path B PoC 实施、平台壳动工、CI 矩阵扩展实施(待用户批准设计文档)、P2×9(未立项)

## 2. 波次任务单原文
- W1 三单全文:见下方【T0】【T1】【T2】原文块(此为在飞波次,原文必留)。

### 【T0】researcher —— Path B 数据面与设计底座调查
- 目标: 为 doc/multiplatform-design.md 提供事实底座,回答 Q1-Q7,结论级摘要+file:line/命令证据。
- 范围边界: 只读调查,不改任何文件;不联网(平台壳技术面用仓库证据+既有知识);vendored librime 读源码(app/src/main/jni/librime/src/rime/),不扫 boost 第三方库内部。
- 调查问题清单:
  Q1 librime 检索运行时数据产物与格式: prism(prism.cc/mapped_file.cc,魔数/版本/结构)、table 主词典(table.cc/table_db.cc/string_table.cc)、reverse db(reverse_lookup_dictionary.cc)、userdb(LevelDB,user_db.cc);Path B Kotlin 运行时最小读取集=哪些文件哪些结构,dictionary.cc 的 lookup 链路语义。
  Q2 "按键路径不碰 YAML"实证+session-open 快照解析真包袱核实: YAML/config 解析发生在生命周期哪个阶段、量级;部署机器 dict_compiler.cc 全量编译链的输入输出清单。
  Q3 内置集实际消费的 config 面: 影响按键行为的配置项(processors/translators/page_size/punct/simplification/switches)、lua/octagram/predict 在内置集中的真实引用面(roadmap 决策 2: gram 不内置、predict.db 不内置);schema 生成 Kotlin 常量的输入字段清单与内置集资产在仓库的实际位置(候选 app/src/main/assets/shared/,以调查为准)。
  Q4 构建期工具链形态对比: (a)复用 librime 构建期工具产出 bin+导出配置面 (b)独立解析器(Kotlin/Python 直读 YAML schema)生成常量——各自依赖面/风险/与"librime 降级为构建期工具链"的关系,给对比结论不做最终裁决。
  Q5 KMP commonMain 抽取面核实(复核架构评审 §五候选清单): desktop/engine 10 文件逐个判定 commonMain/平台面;app 侧 RimeProto.kt/SchemaItem.kt 与 roadmap 期 4 :core:model 白名单的汇合约束;protocol-common 源集在 kotlinc 脚本编译(非 gradle)下的编译闭包形态。
  Q6 平台壳接口面: Linux ibus engine(实现语言选项、与行协议 DesktopServer 的组合方式)、Windows TSF(IME DLL 进程/语言约束、JVM 行协议进程组合可行性)、iOS(Kotlin/Native framework+cinterop 形态:Xcode 集成、线程模型)、macOS 现状为基线。每平台给"壳必须实现的接口面+已知风险",不做选型裁决。
  Q7 机器闸门清单精确化(后续所有 Code 单验收标准依据): test_server.py / golden 双录(harness/README.md §H1 流程) / harness/smoke.sh / app 单测(./gradlew :app:testDebugUnitTest 当前基线数) / spotless / CI 四 leg(.github/workflows/desktop-ci.yml 触发方式与产物)——每条给确切命令、通过判据、时长量级。
- 验收标准: Q1-Q7 逐题作答,每题结论附证据;Q7 每条命令可直接复制执行;报告末尾 SUMMARY 行(自定计数,如 Q=7/7)。

### 【T1】coder —— P1-5 core 层解依赖
- 目标: 按 doc/architecture-review.md P1-5 整改建议三步落地,使 app core 引擎层不再依赖 ime/data 层,打通 roadmap 期 4 :core:rime 拆分前置条件。
- 完整上下文: 评审证据=app/src/main/java/com/jiaoay/jrime/core/Rime.kt:11-13 import data.opencc.OpenCCDictManager、data.prefs.AppPrefs、ime.core.InlinePreeditMode;使用点 :77-78(prefs 直读)、:323-335(InlinePreeditMode 映射进引擎响应格式化)、:363(OpenCCDictManager 构建词典);评审已核实全 core 目录向上依赖仅此一处。整改三步:①InlinePreeditMode 映射(:323-335)上提 ime 层——引擎只交原始 composition,"格式化 preedit 展示形态"归 ime;②AppPrefs 两个值(:77-78)改构造参数注入(调用方在 ime 层,天然可取);③OpenCC 词典构建(:363)抽成 RimeOpenCCBridge 接口由 ime/daemon 侧注入实现。做完 core 包 import 面收窄到 kotlinx/java 标准库。
- 范围边界: 可动=Rime.kt、其 ime/daemon 侧调用方接线文件、新增接口文件(落包按最小依赖方向)。逐条不做:①不改 app/src/main/java/com/jiaoay/jrime/data/prefs/AppPrefs.kt(并发会话在途);②不碰 app/src/main/java/com/jiaoay/jrime/ime/compose/ 下任何文件(GboardKey.kt/GboardKeyboard.kt/KeyboardPanel.kt 等并发会话在途面——若发现必须触碰即按 BLOCKED 上报,不绕道);③不做模块拆分(期 4 的事);④不改 app/src/main/jni/ 全目录(C++ 桥);⑤不改 desktop/、harness/、macos/、.github/。
- 必须遵守: 行为不变(Android 键盘行为无感);不破坏既有公共 API 语义(可为注入参数提供默认实现保持兼容);提交纪律=git status 复核后白名单式只 add 本任务文件,严禁 git add -A,单 commit 不 push,消息建议 "refactor(app): remove core layer upward deps to ime/data (P1-5)";报告 SUMMARY 行含 files=N 与四件套自查结果。
- 验收标准(verifier 可执行): ①grep -rn "import com\.jiaoay\.jrime\.data\.\|import com\.jiaoay\.jrime\.ime\." app/src/main/java/com/jiaoay/jrime/core/ 零命中;②./gradlew :app:testDebugUnitTest 全绿(基线以开工时实测快照为准,不少于 181,不接受静默丢测试);③./gradlew :app:compileDebugKotlin 通过;④spotlessCheck(若有)通过;⑤白名单 commit 恰含自报清单文件。

### 【T2】coder —— P1-6 工具链单一事实源
- 目标: KOTLIN_VERSION 与编译器下载校验收敛为单一事实源:新建 script/toolchain.properties(KOTLIN_VERSION + KOTLIN_COMPILER_SHA256),五处字面量改为读它,下载后统一 SHA256 校验。
- 完整上下文: 评审证据=KOTLIN_VERSION=2.4.21 字面量五处(harness/smoke.sh:48、desktop/test_server.py:23、macos/JRimeIME/build.sh:22、.github/workflows/desktop-ci.yml:166 与 :220);下载 URL 四段(smoke.sh:51-52、test_server.py:37-38、desktop-ci.yml:166、:220),全部无 SHA256 校验;stdlib 解包 hack 五份(smoke.sh:59、test_server.py:60-61、build.sh:26、desktop-ci.yml:169/:225)——本单不动。读取方式: shell source/读行、python 解析、CI bash leg 用 grep/sed、windows leg 用 PowerShell 等价;校验: bash shasum -a 256 -c 或等价、python hashlib、windows Get-FileHash。SHA256 值由 coder 实际下载 zip 计算回填;若各处下载 artifact 不同,按实际 artifact 数列多条 SHA 键。test_server.py 等处版本派生的缓存目录名改由属性值派生。
- 范围边界: 可动=新建 script/toolchain.properties;harness/smoke.sh、desktop/test_server.py、macos/JRimeIME/build.sh、.github/workflows/desktop-ci.yml。逐条不做:①stdlib 解包 hack 五份一律不动(中期随 gradle 化消解);②不动 harness/build.sh 的 find 编译参数(P1-3 的事,本单 build.sh 只改 :22 版本字面量);③不升级 Kotlin 版本(仍 2.4.21);④desktop-ci.yml 改动最小化(仅版本读取方式+校验步,预期 diff <40 行),不动其他 leg 逻辑;⑤不 push。
- 必须遵守: 五入口编译产物行为等价;并发会话防御同 T1(白名单 commit、git status 复核);commit 消息建议 "build: centralize kotlinc toolchain version and add SHA256 verification (P1-6)";报告 SUMMARY 行含 files=N 与本地闸门自查结果。
- 验收标准: ①grep 复核:版本字面量 2.4.21 不再硬编码于上述五文件(仅存于 toolchain.properties 及从属性派生的动态拼接);②harness/smoke.sh 本地全流程实跑通过(含新校验逻辑),desktop/test_server.py 若本机可跑则实跑;③toolchain.properties 含 KOTLIN_VERSION 与 SHA 键,校验命令实测通过;④desktop-ci.yml YAML 解析通过(有 actionlint 则用之);⑤白名单 commit 恰含自报清单文件。

## 3. 累计验收结论
- W1/T2:通过。证据指针:script/toolchain.properties:9-10(版本+SHA 单源);四文件 2.4.21 字面量零命中(orchestrator 独立 grep);smoke.sh:50,55 shasum -a 256 -c;test_server.py:26-40 _load_toolchain_properties;build.sh:22-23 sed 读取;desktop-ci.yml:168,170 sha256sum -c(linux)+:264-267 Get-FileHash(windows,CI 实跑验证在案);smoke/test_server 实跑 PASS(coder);SHA 双向校验+重下载交叉一致(coder)。diff 20 行<40;commit 19d91fe 落库(执行方=主对话,白名单 5 文件)。
- W1/T1:通过。证据指针:core 目录 ime/data import 零命中(orchestrator 独立 grep+主对话实跑双证);Rime.kt:33,35 注入+322,349 调用点(AppPrefs 零残留);三新文件落位(core/RimeInlinePreeditFormatter.kt、core/RimeOpenCCBridge.kt、ime/core/InlinePreeditFormatter.kt);单测 193/193 绿;spotlessCheck PASS;合并树 test_server+smoke PASS(主对话实跑)。评审焦点(供 R1 专项):接口签名纯度+期 4 纯移动成立性。commit e7d8a29 落库(执行方=主对话,白名单 5 文件)。

## 4. BLOCKED 与返工记录
(无)

## 5. 需裁决项
1. P1-1 stub 错位 / P1-2 NewStringUTF 修与不修 | 候选 A 现修(各需 golden 重录窗口,P1-1 是 macOS ascii 显示正确性的产品化前置;P1-2 需真机复核"Android 不受影响"判定,证伪则转 Android 项存续) B 不修(Path B 排期先于桌面线对外可用,随桥作废零成本关闭) | 阻塞: 本期无子任务(T4 设计文档出排期建议后提请用户裁决) | 默认: 不修,等设计文档排期建议。
2. 设计文档批准 | doc/multiplatform-design.md 已落盘(342 行),裁决集中=§6.3 三件(opencc 数据面/词汇表二选一/P1-1 P1-2 排期)+§1.4(Step 2 jvm() target 对 roadmap 决策 6 的修订属性)+§2.7(D2-D4);R-doc 通过后升级为正式提请 | 阻塞: 期三实施波次 | 默认: 见文档各默认路径。

## 6. 战略指令衔接(本期)
- 期一 §6 继续有效: 舍弃 YAML 运行时;按键路径不碰 YAML;真包袱=部署机器+session-open 快照解析;短期=Android fastpath+config 常量化;中期=Path B(运行时只读 prism/table/reverse bin + schema 生成 Kotlin 常量,yaml-cpp/config_compiler/Lua 退出运行时,librime 降级构建期工具链)。
- 投入裁决基线(doc/architecture-review.md 战略衔接章): 放心投入=P1-3/P1-4/P2-4 协议层+KMP Step 0-1+P1-5;按时间表裁决=P1-1/P1-2;与 Path B 无关应修=P1-6/P2-5/P2-6/P2-8/P2-9/P2-3/P2-7。本期执行前四项中的已批 P1:P1-5/P1-6/P1-3/P1-4。

## 7. 固定决策(Plan,本期)
1. 测试策略: T1/T2/T3 为行为保持型改动,仅跑既有回归四件套(test_server.py/golden 双录/smoke/app 单测,按改动面取交集,Android 面加 spotless);T5 为协议变更,新测试纳入 coder 范围(test_server.py 握手断言+Swift 壳 proto 断言),并含 golden 重录窗口(T5 为 ready 响应附加字段,旧 golden 会字节失配,重录后 12/12 双录 IDENTICAL 为准);Path B PoC 测试策略随其任务单另定。
2. 机器闸门: 以 T0 Q7 盘点清单为准,Code 单验收标准引用之;禁止静态自扫替代闸门实跑(期一教训);golden 双录为行为等价契约。
3. 提交纪律: 每张 Code 单完成即白名单式单 commit(只 add 本任务文件,git status 复核,严禁 git add -A),不 push,push 节奏由用户掌握。
4. 小决策自决(最小合理默认,风险节注明): T3 共享源集位置=desktop/protocol-common/(评审首选项);P1-4 字段形态=ready 响应附加 "proto":1(整数,只增不改义)与 "engine":"<VERSION>";T2 属性文件=script/toolchain.properties。

## 8. 待主人操作项
- 期一 4 项均已闭环(push 完成/doc/architecture-review.md 已提交 438d5e2/P1×6 立项已批"1可以"/GboardKey.kt spotless 收口归并发会话)。
- 【2026-11-19 更新】T2+T1 两笔白名单 commit 已由主对话执行完毕(e7d8a29=T1 五路径、19d91fe=T2 五路径,各自 show 复核恰 5、排除清单零卷入、未 push——push 节奏由用户掌握)。
- 基线漂移:app 单测基线 181→193(并发会话 +12),W1 后续取证按 193 全绿口径。
- windows leg PowerShell 校验段 CI 实跑验证在案(本地 pwsh 不可用),随下轮 CI 验证。
