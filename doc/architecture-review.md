# JRime 整体架构评审报告

> 2026-11-18 · S5(专家团 reviewer)。评审对象 = 任务 1(Java→Kotlin 全量转换,commit 612e669)终态之上的全仓架构;后续 CI 修正 commit(9953abd/69cbacf)不改本报告引用的代码证据。文中路径均相对仓库根。
> 方向输入 = 用户战略指令(台账 §6):"我期望还是能舍弃掉rime的yaml这种低效且低性能的实现方式"——本报告不以 librime 冻结为终态,建议的投入优先级按 Path B 兼容性排序;Path B(路线二 Kotlin 引擎)对各组件的影响见文末战略指令衔接章。
> 方法:七维逐维取证(分层边界 / KMP 化路径 / 重复逻辑 / 协议演进性 / 构建打包一致性 / 已知风险 / 命名一致性),全部结论附 file:line;全仓搜索均 path 限定(vendored librime/boost 不入扫描);并发会话在途改动(app/ 手势阈值相关)不在评审对象内,证据引用已避开。
> 接管复核:2026-11-18 第二评审者接管收尾,报告主体为前任 reviewer 落盘版本;其全部 P1/P2 引用已逐行独立复核(涉及 StatusProto.kt / objconv.h / jni-utils.h / rime_jni.cc / opencc.cc / config.cc / DesktopServer.kt / Replay.kt / Rime.kt / RimeProto.kt / EngineClient.swift / InputController.swift / smoke.sh / test_server.py / build.sh / desktop-ci.yml / doc/refactor/roadmap.md / expert_team_state.md)。复核修正两处事实:desktop stub 文件计数 13→10(9 个 DTO 类型 + Rime.kt stub 门面;"13"系任务 1 转换文件总数,已按口径归位),roadmap 引用路径补全为 doc/refactor/roadmap.md;另复核确认 P1-5 范围完整(core 层向上依赖仅 Rime.kt:11-13 一处,全 core 目录 grep 无第二处)。SUMMARY 计数不变。

## 一、概述与总评

### 1.1 架构图景

全仓呈"一个引擎内核、两种消费形态":

- **桌面线(协议层清晰)**:Swift IMK 壳(macos/JRimeIME/swift/)→ 行协议(stdin/stdout JSON 行)→ DesktopServer JVM 进程(desktop/engine/)→ JNI 桥(app/src/main/jni/librime_jni/,与 Android 共用)→ librime。壳不碰引擎内部,引擎不碰壳,协议是唯一交界面——三层职责清晰,协议层被有意设计为引擎无关(harness-design.md §2 已预留 KotlinEngine 第二后端),这正是 Path B 的关键接缝。
- **Android 线(进程内直连)**:Compose IME(ime/)→ core/Rime.kt JNI 门面 → 同一份 C++ 桥 → librime。无协议层(进程内不需要),但 core 层存在向上依赖(见 P1-5)。
- **harness 线**:Replay/Smoke(harness/java/)驱动同一桌面桥,golden 双录构成转换与未来内核替换的等价契约(12 用例,S3 实测逐字节 IDENTICAL)。
- **构建面**:`:app`/`:codegen` 走 gradle(settings.gradle.kts:24-25),desktop/harness 走 kotlinc 直调脚本——五处编译入口并存(见 P1-6)。

任务 1(13 Java→Kotlin)不改变上述结构,转换面四件套(test_server PASS / golden 12:12 IDENTICAL / smoke PASS / 单测全绿,verifier S3 报告)确认行为等价;本报告结论均建立在转换终态之上。

### 1.2 七维逐维结论

| # | 维度 | 结论 | 对应 issue |
|---|------|------|-----------|
| 1 | 分层与边界 | 桌面三层清晰、无越层调用;Android 侧 core 引擎层反向依赖 ime/data 层,是分层唯一实质破口,也是 roadmap 期 4 拆分的前置阻塞点 | P1-5 |
| 2 | KMP 化路径 | 可进 commonMain 的候选成立但现状是"三份协议类型并行"(desktop stub 10 文件 / app RimeProto.kt / Swift+Python 各自手写解码);现实路径是先收敛双事实源再抽共享协议模块,与 roadmap 期 4 `:core:model` 汇合而非另起炉灶 | P2-1、KMP 路线章 |
| 3 | 重复逻辑 | DesktopServer 与 Replay 的采样器/JSON 编码器/就绪探测三段逐字节级重复,golden 等价契约仅靠注释约定维持 | P1-3 |
| 4 | 协议演进性 | 协议无版本握手;错误行无结构且被 Swift 壳静默吞掉;两套请求词汇表并存;候选负载缺 highlight/cursor 字段 | P1-4、P2-3、P2-4 |
| 5 | 构建与打包一致性 | 五处 kotlinc 入口五处版本字面量、下载零校验、stdlib 解包 hack 五份;gradle 与脚本二元并存 | P1-6、P2-5、P2-6、P2-8、P2-9 |
| 6 | 已知风险 | 两条既有桥缺陷均在桌面线实证定位:stub 字段错位(真因,S1 已证伪"状态缓存"假设)与 NewStringUTF 4 字节 UTF-8;两者均为台账登记的独立工作项,与 Path B 的关闭关系见战略衔接章 | P1-1、P1-2、P2-7 |
| 7 | 命名一致性 | 同一 JNI 类在三处(desktop stub / app / 桥 objconv)有三套字段词汇表,drift 已实际产出 P1-1;建议错位修复与命名对齐一次完成 | P1-1、P2-2 |

### 1.3 总评

桌面线"壳/协议/引擎"分层与 harness 的 oracle 契约是本仓当前最有价值的两笔架构资产:前者让 Path B 有现成接缝,后者让 librime 内核替换全程有安全绳。主要债务集中在三处:(a) 协议类型的双/三事实源漂移(已实际造成 P1-1);(b) 五处手工编译入口的脆弱一致性;(c) Android core 层的向上依赖阻塞期 4 拆分。全部 P1 均有低成本收敛路径,且大多与 Path B 方向同向——修协议层与抽共享模块的投入在 Path B 落地后全额存活,修桌面桥缺陷的投入则应按 Path B 时间表裁决(战略衔接章给出裁决建议)。

**P0 = 0:未发现阻塞级问题。** 当前终态(转换已提交)可交付;P1-1/P1-2 是已知且已登记的产品级缺陷,不阻塞本轮交付,但应在桌面线对外可用前裁决修复或由 Path B 关闭。

## 二、P0(阻塞级)

(无。)

## 三、P1(应修)

### [P1-1] 桌面 stub StatusProto 字段错位:getRimeStatus 不反映真实状态,"ascii" 采样字段实收 is_disabled

- **证据**:`desktop/engine/java/com/jiaoay/jrime/core/StatusProto.kt:3-13`(声明顺序 schemaId, schemaName, **isAsciiMode**, isComposing, isAsciiPunch, isSimplified, isFullShape, isAsciiShape, isAsciiComma)vs 桥的按位构造:`app/src/main/jni/librime_jni/objconv.h:115-122` 传参顺序为 schemaId, schemaName, **isDisabled**, isComposing, **isAsciiMode**, isFullShape, isSimplified, isTraditional, isAsciiPunct;构造器签名 `app/src/main/jni/librime_jni/jni-utils.h:205-209`(`(Ljava/lang/String;Ljava/lang/String;ZZZZZZZ)V`)仅锁类型不锁语义。逐位对齐结果:stub.isAsciiMode←is_disabled、stub.isAsciiPunch←is_ascii_mode、stub.isSimplified←is_full_shape、stub.isFullShape←is_simplified、stub.isAsciiShape←is_traditional、stub.isAsciiComma←is_ascii_punct。
- **影响**:消费点两处全中招——`desktop/engine/java/com/jiaoay/jrime/desktop/DesktopServer.kt:170`(`"ascii":st.isAsciiMode`)与 `harness/java/Replay.kt:134` 同式:行协议与 golden 轨迹里的 `ascii` 字段记录的实际是 is_disabled。golden 双录因此仍然 IDENTICAL(两侧一致地错,等价契约成立、语义不成立);macOS 壳据此无法正确实现 ASCII 模式显示。此为台账 §6 登记的独立工作项,非任务 1 回归(Java 时代即错位)。
- **整改建议(可执行)**:① 将 desktop stub StatusProto 字段按 objconv.h:115-122 传参顺序重排并更名(`isDisabled, isComposing, isAsciiMode, isFullShape, isSimplified, isTraditional, isAsciiPunct`——与 app 侧 `app/src/main/java/com/jiaoay/jrime/core/RimeProto.kt:45-55` 词汇表一致,顺带完成 P2-2 对齐);② 修后 golden 必须重录(台账固定决策:重录与转换互斥,需单独窗口);③ 补一条确定性语料用例(`opt:ascii_mode=true` 前后各采一步,断言 trace `"ascii":true→false` 翻转)——现有 12 用例恰好没有语义断言能抓住此类错位;④ 把 S3 的机械闸门(javap 对 jni-utils.h:176-239)扩展为"签名 + 字段语义序"双层,防止回归。

### [P1-2] 桌面桥 NewStringUTF 捣毁 4 字节 UTF-8:CJK 扩展 B 区候选/注释乱码

- **证据**:字符串入 JNI 全走 `NewStringUTF`:`app/src/main/jni/librime_jni/jni-utils.h:59-64`(JString 包装器,覆盖 objconv.h:55-90 的 commit/candidate/composition/context 全部文本字段与 :12-29 schema 列表),另有直调点 `app/src/main/jni/librime_jni/rime_jni.cc:350,370`、`app/src/main/jni/librime_jni/opencc.cc:20`、`app/src/main/jni/librime_jni/config.cc:77`。NewStringUTF 语义是 modified UTF-8,4 字节 UTF-8 序列(CJK 扩展 B,U+20000+)在该路径产生乱码。桌面侧已被 harness 观测并登记:`harness/README.md:52-55`(golden 中呈乱码,桌面桥需改为按字节构造 String;README 判定 Android 侧不受影响)。
- **影响**:macOS 壳对扩展 B 区字符的候选、注释、上屏文本全链路乱码;golden 中乱码被固化(若日后桥修复而不重录,golden 会反向"钉死"乱码)。台账未单列此项但 README 已如实登记,与 P1-1 同属"修则必伴随 golden 重录"。
- **整改建议(可执行)**:桌面桥的 JString 改为按字节构造——jbyteArray + `java/lang/String` 的 `([B)V`/`([BLjava/lang/String;)V` 构造器(或 byte[]→String(UTF_8) 语义等价路径),一处改 jni-utils.h:59-64 即覆盖全部 objconv 调用点;直调点 rime_jni.cc:350,370、opencc.cc:20、config.cc:77 各换一次辅助函数。修后按 README:55 重录 golden。**与 Path B 的裁决关系**:若 Path B 时间表已定且桌面桥随即作废,本项可零成本关闭(战略衔接章);若桌面线先于 Path B 对外可用,则本项是 macOS 产品化的前置缺陷。另建议顺手把 README 的"Android 侧不受影响"判定用真机扩展 B 用例复核一次(该判定目前无测试锚点)。

### [P1-3] DesktopServer 与 Replay 的采样器/JSON 编码器/就绪探测三段重复,golden 等价契约仅靠注释约定

- **证据**:采样器:`desktop/engine/java/com/jiaoay/jrime/desktop/DesktopServer.kt:151-182` vs `harness/java/Replay.kt:114-146`——除 `step` 字段外逐行相同(同样的 getRimeContext/getRimeStatus/getRimeCommit 排空语义 + 同样的手拼 JSON + 同样 `cands` 二元组格式);编码器:`DesktopServer.kt:184-205` vs `Replay.kt:148-169` 的 `json()` 逐字节相同;就绪探测:`DesktopServer.kt:57-67` vs `Replay.kt:52-71`(同为 150×200ms 探询循环)。等价契约的证据只有注释:`DesktopServer.kt:150`("identical shape to the harness trace minus step")与 :23("the same shape as the harness trace")。
- **影响**:行协议响应形状与 golden 轨迹形状的"同形"是 harness 价值的核心假设,现在由两份拷贝靠纪律维持;任一侧演进(如 P2-4 的负载扩展)都会静默破坏 golden ↔ server 的可比性,而 diff 闸门不会报错——它会继续逐字节比对其各自漂移后的产物。另有第三、四份相邻拷贝:Swift 解码(EngineClient.swift:10-25)与 Python 测试断言(desktop/test_server.py:104-119)也各自硬编码字段名。
- **整改建议(可执行)**:抽共享 Kotlin 源集(建议 `desktop/protocol-common/` 或 `harness/common/`),内容三件:①`EngineSnapshotSampler`(输入 op/handled/step?,输出规范 JSON 行);②`JsonLineCodec`(现 json() 函数);③`awaitSchemaReady(schema, probe=150, interval=200)`。迁移零摩擦:smoke.sh:57-58 本就把 harness/java 与 desktop/engine/java 两个目录一起喂 kotlinc,新增第三个目录只是 find 参数加一段;test_server.py:56 的 rglob 与 build.sh:24 的 find 同理。改完后 DesktopServer/Replay 各剩主循环,重复消失,协议形状获得单一事实源。

### [P1-4] 行协议无版本握手与引擎标识,引擎核心包与壳未来分离分发时无法对齐

- **证据**:`desktop/engine/java/com/jiaoay/jrime/desktop/DesktopServer.kt:26` 定义 `VERSION = "1.0.0-desktop"`,但该常量只作为 distribution 版本传入 `startupRime`(:53),从不跨协议传输;`ready` 请求的响应(:113 → sample())只含 op/handled 与引擎状态,无协议版本、无引擎版本字段。
- **影响**:当前壳与引擎同包分发(macOS build.sh 把两者打进同一 .app),错配不可能发生;但 CI 已把引擎核心作为独立制品分发(desktop-ci.yml:175-180 linux、:231 附近 windows,注释 :122/:183 明言未来 ibus 壳/TSF 壳消费),壳与引擎核心一旦各自演进,唯一的表现形式是运行期字段缺失/语义漂移,而 Swift 解码侧全是 `?? 默认值`(EngineClient.swift:13-18)——错配将静默降级为"看起来能跑但行为不对"。
- **整改建议(可执行)**:在 ready 响应中追加两个附加字段:`"proto":1`(协议版本整数,只增不改义)与 `"engine":"<VERSION>"`;Swift 壳在 start() 的 ready 交换处断言 `proto <= 支持值`(EngineClient.swift:76 附近),超版即报错退出而非带病运行。附加字段对现有消费方(EngineClient.swift 的字段级解码、test_server.py 的键级断言)向后兼容。

### [P1-5] app core 引擎层向上依赖 ime/data 层,阻塞 roadmap 期 4 拆分

- **证据**:`app/src/main/java/com/jiaoay/jrime/core/Rime.kt:11-13` 依次 import `data.opencc.OpenCCDictManager`、`data.prefs.AppPrefs`、`ime.core.InlinePreeditMode`;使用点::77-78(prefs 直读)、:323-335(InlinePreeditMode 映射进引擎响应格式化)、:363(OpenCCDictManager 构建词典)。即"JNI 门面 + daemon"层反向依赖 IME 与数据层。
- **影响**:roadmap 期 4 计划把 JNI 门面拆进 `:core:rime`(doc/refactor/roadmap.md:94),而 `:core:rime` 依赖 `:ime`/`:data` 类型时模块拓扑无法成立(依赖倒置,拆分时必返工);同时也是分层语义破坏:引擎层行为被 IME 偏好与 UI 枚举改写。
- **整改建议(可执行)**:三步,均可独立落地:① InlinePreeditMode 映射(:323-335)上提至 ime 层——把"格式化 preedit 展示形态"从 Rime 响应组装中拆出,引擎只交出原始 composition;② AppPrefs 两个值(:77-78)改构造参数注入(调用方在 ime 层,天然可取);③ OpenCC 词典构建(:363)抽成 `RimeOpenCCBridge` 接口由 ime/daemon 侧注入实现。做完后 core 包 import 面收窄到 kotlinx/java 标准库,期 4 拆 `:core:rime` 变成纯移动。

### [P1-6] kotlinc 工具链五处字面量、下载零校验、无单一事实源

- **证据**:`KOTLIN_VERSION=2.4.21` 字面量五处——`harness/smoke.sh:48`、`desktop/test_server.py:23`、`macos/JRimeIME/build.sh:22`、`.github/workflows/desktop-ci.yml:166` 与 :220;五处各自独立下载(zip 下载 URL 分别在 smoke.sh:51-52、test_server.py:37-38、desktop-ci.yml:166、desktop-ci.yml:220),全部无 SHA256 校验;stdlib 解包到 classes 目录的 hack 五份(smoke.sh:59、test_server.py:60-61、build.sh:26、desktop-ci.yml:169、desktop-ci.yml:225)。
- **影响**:版本升级需要人肉同步 5 处 3 种语法(shell/python/yaml),漏一处即本地与 CI 产物 stdlib 错版(运行期 NoSuchMethodError 类故障,难定位);零校验的 release 下载是供应链缺口(超网下载 + unzip 直用)。本项在 S4 转换评审中按当时范围记 P2(台账 §3 ②⑥),架构评审口径(五个入口是桌面线的常驻构建面而非一次性 diff)升为 P1。
- **整改建议(可执行)**:① 建 `script/toolchain.properties`(或 .env):`KOTLIN_VERSION=2.4.21` + `KOTLIN_COMPILER_SHA256=<zip 摘要>`,五处全部改为读它(shell `source`、python 解析、CI 用 `grep`/`sed` 取值);② 下载后统一 `shasum -a 256 -c` 校验;③ 中期随 P2-5 的 gradle 化自然消解(gradle 管工具链与运行时 classpath,解包 hack 与下载脚本整体退役)。

## 四、P2(建议)

### [P2-1] 协议类型双事实源并行漂移:desktop stub 10 文件 vs app RimeProto.kt

- **证据**:desktop 侧 `desktop/engine/java/com/jiaoay/jrime/core/*.kt`(10 个文件,即 9 个 DTO 类型 + Rime.kt JNI stub 门面,如 StatusProto.kt:3-13、CandidateProto.kt:3-7)与 app 侧 `app/src/main/java/com/jiaoay/jrime/core/RimeProto.kt:8-145` 及 SchemaItem/RimeKeyEvent 同包同名不同体:StatusProto 字段集不同(见 P1-1)、CandidateProto 第三字段 stub 叫 seqId / app 叫 label、Candidates stub 是嵌套 class / app 是 sealed interface(RimeProto.kt:61-134)。
- **影响**:同一组线概念两处定义,漂移已有实际产出(P1-1);harness/README.md:11-13 已声明 stub"不是 app 侧的实现",但未约束其与 app 词汇表的一致性。
- **整改建议**:与 KMP 路线 Step 1 合并执行(见 KMP 路线章):两族收敛为一个共享协议模块;收敛前先按 P1-1 建议把 stub 字段名对齐 app 词汇表,把漂移成本压到一次 golden 重录。

### [P2-2] 桌面 stub 字段命名与桥语义、app 命名三套词汇表

- **证据**:`desktop/engine/java/com/jiaoay/jrime/core/StatusProto.kt:9` `isAsciiPunch` vs 桥 objconv.h:121 `isAsciiPunct` vs app RimeProto.kt:54 `isAsciiPunct`("Punch" 为 Trime 血统拼写);`desktop/engine/java/com/jiaoay/jrime/core/CandidateProto.kt:6` `seqId` 实收桥 `label`(objconv.h:65);`desktop/engine/java/com/jiaoay/jrime/core/RimeKeyEvent.kt:4-5` `keycode/modifier` vs app RimeKeyEvent.kt:9-10 `value/modifiers`;`desktop/engine/java/com/jiaoay/jrime/core/Candidates.kt:4-10` `finished/lastPageSize: Boolean/reverse`(lastPageSize 是 Boolean 却名如 Int;reverse 实收桥 is_horizontal_layout,objconv.h:103-106)vs app `hasPrevPage/hasNextPage/isHorizontalLayout`(RimeProto.kt:69-77);`desktop/engine/java/com/jiaoay/jrime/core/CompositionProto.kt:3-11` `preeditStart/selStartText` vs 桥与 app 的 `length/commitTextPreview`(objconv.h:82-89)。
- **影响**:同一位 JNI 槽位三处三个名字,读桥代码必须人肉对位表;P1-1 类错位正是这种 drift 的必然产物。
- **整改建议**:随 P1-1 修复一次完成:desktop stub 字段名统一采用 app 侧/桥语义词汇表(三处同一个说法),并在 harness/README.md 的 stub 说明段加一句"字段名与 app RimeProto.kt 对齐"的维护约定。

### [P2-3] 协议错误行无结构,Swift 壳静默吞错

- **证据**:错误响应为非规则行 `{"error":...}`(DesktopServer.kt:74-75 引擎未挂载、:141 未知请求、:144 异常兜底),缺规则响应的其余字段;Swift 解码 `macos/JRimeIME/swift/EngineClient.swift:10-25` 的 ServerReply 无 error 访问器,error 行解码为 handled=false/raw=""/preedit="";下游 `swift/InputController.swift:106-128` apply() 据此清空 preedit——错误被无声翻译成"引擎认为没有组合"。
- **影响**:引擎侧异常(如 `sel` 非数字、opt 拼错)在 macOS 壳上表现为"打字没反应",无任何日志线索;排障只能去 stderr 翻 librime 日志。
- **整改建议**:ServerReply 增加 `var error: String? { obj["error"] as? String }`;InputController.apply 对非 nil error 显式 NSLog 并保留既有降级行为;测试侧 test_server.py:79-80 的交换函数对含 error 的响应提前抛断言。若做 P1-4,错误行同时带上 proto 字段,解码兼容性一并解决。

### [P2-4] 请求词汇表两套并存 + 响应负载缺口(highlight/cursor/label)

- **证据**:词汇表:server 侧 `k <repr> / sel <i> / pg next / pg prev / opt <n> <bool>`(DesktopServer.kt:20-21 及 :115-139)vs harness 侧 `k:<repr> / sel:<i> / pg:next / pg:prev / opt:<n>=<bool>`(Replay.kt:16-17 及 :88-112)——同语义两语法,无文档声明等价关系。负载缺口:候选只发 [text, comment] 二元组(DesktopServer.kt:177-178;EngineClient.swift:19-24 按位置解),label/高亮索引/preedit 光标位均不上协议——CompositionProto.preeditCursor 与 Candidates 高亮在桥上现成(objconv.h:84、:106)却被丢弃;Swift 侧 preedit 光标只能置尾(InputController.swift:117),IMKCandidates 高亮同步因此只能空实现(InputController.swift:87-89)。
- **影响**:未来 ibus/TSF 壳接入时需在两套词汇表里做选择题,或有第三套;壳侧功能(高亮跟随、光标精确定位)被协议负载卡死,扩展时又要动两处实现(P1-3 的重复面)。
- **整改建议**:① 词汇表二选一,另一侧改别名并在 harness-design.md §4 建协议词汇表小节(半页即可),避免第三消费者再造词汇;② 响应附加字段按需增量扩展:`"hl":<highlightedIndex>`、`"cursor":<preeditCursorPos>`、cands 三元组化(带 label),Swift 侧字段级解码天然向后兼容;扩展落点放在 P1-3 抽出的共享采样器里,一处改两侧生效。

### [P2-5] 构建体系二元:gradle(:app/:codegen)与 kotlinc 脚本五入口并存,stdlib 解包 hack ×5

- **证据**:gradle 模块仅 :app 与 :codegen(settings.gradle.kts:24-25);desktop/harness 的 Kotlin 编译全走脚本五入口(P1-6 证据清单);stdlib 解包进 classes 目录的 -cp 单路径方案五份(smoke.sh:59、test_server.py:60-61、build.sh:26、desktop-ci.yml:169/:225)。harness-design.md H0 明确"gradle 模块化推迟到 H1 再议"(:80),属已登记的技术债而非失察。
- **影响**:同一批 engine 源文件每次全量被编译(五入口互不共享产物,全无增量);stdlib 解包方案把 kotlin-stdlib 全量类平铺进 engine 目录(体积与隔离性双输);新文件落盘后没有任何编译期闸门(只依赖下次脚本运行)。
- **整改建议**:按 harness-design.md H1+ 兑现:`:desktop-engine` 与 `:harness` 建 gradle JVM 模块(commonMain 优先,接 KMP 路线 Step 2),脚本五入口收敛为 gradle 调用(脚本保留为薄包装以兼容既有 CI 步骤名);stdlib 由 gradle 运行时依赖替代解包,jlink 打包(build.sh:29-30)不变。

### [P2-6] test_server.py 的 kotlinc 缓存判据与半成品残留;平台 chmod 不对称

- **证据**:`desktop/test_server.py:29` 缓存命中判据只查 `bin/kotlinc` 存在不查执行位;`:43` extractall 后 rename 若中途失败留下半成品目录(下次运行 `bin/kotlinc` 不存在会重新下载,但 kotlinc-2.4.21 残壳会遮蔽目标名);`:45-47` 对 bin 目录 chmod 0o755 修复 zipfile 不保执行位——仅 Python 路径有此修复;smoke.sh:55 的 mv 保位不需要,CI Linux unzip(desktop-ci.yml:167)凭 unzip 自身保位语义通过——三条获取路径三种执行位语义,全靠巧合各自成立。(台账 §3 ③,此处收编并补 CI 对照。)
- **影响**:缓存判据漏执行位时会以不可读的 "permission denied" 失败;半成品残留使 ensure_kotlinc 的"先查缓存"语义偶尔自相矛盾;三条路径语义不一致是隐性维护负担。
- **整改建议**:ensure_kotlinc 改为:判据 = `bin/kotlinc` 存在且 `os.access(..., X_OK)`;extract 到临时目录 + `os.replace` 原子落位,失败即清临时目录;chmod 修复保留。

### [P2-7] harness/README.md 对 status 问题的诊断记载已过时(疑似缓存 → 已证伪为 stub 错位)

- **证据**:`harness/README.md:56-57` 载"getRimeStatus().isAsciiMode 仍读 false——疑似桥内状态缓存,待查";S1 已机械证伪缓存假设、定位真因为 stub 字段错位(台账 §3:"rime_jni.cc:115-123 证伪 status 缓存假设,真因=stub 字段错位",对应本报告 P1-1)。
- **影响**:后来者按 README 线索去查桥内缓存会空跑一趟;文档与台账结论背离。
- **整改建议**:README:56-57 改为指向真因(一行:"真因=desktop stub StatusProto 字段错位,见 doc/architecture-review.md P1-1;修复伴随 golden 重录"),与 P1-1/P1-2 修复同一 commit 顺带完成。

### [P2-8] macos build.sh 吞掉 smoke 失败输出,dylib 失败只剩一行回显

- **证据**:`macos/JRimeIME/build.sh:15` `"$REPO/harness/smoke.sh" >/dev/null 2>&1 || { echo "dylib build failed"; exit 1; }`——失败被正确拦截(exit 1),但编译/冒烟的全部输出被丢弃,失败原因只剩一句话。(台账 §3 ④ 收编。)
- **影响**:CI(macos-app leg,desktop-ci.yml:104-107)上失败时排查只能本地复跑;失败信息丢失拉长定位路径。
- **整改建议**:失败分支改为 tee 落盘再转发:`... >build-log 2>&1 || { tail -50 build-log; exit 1; }`(或仅重定向 stdout、放行 stderr,smoke.sh 的协议输出本就不走 stdout 之外的关键通道)。

### [P2-9] Windows 引擎核心无本地闸门,CI 为首次实跑点

- **证据**:windows-engine leg 的 DLL 构建(build-bridge.ps1)与 stdlib Expand-Archive 打包(desktop-ci.yml:222-225)在本地均无对应测试入口——test_server.py 明写 macOS host(desktop/test_server.py:2),Windows 无 smoke/协议测试等价物;台账 §3 ⑤ 已记"CI 为首次实跑点"。
- **影响**:Windows 引擎包(未来 TSF 壳的消费物)在有人接入之前持续零验证发布;CI 红了才知道坏。
- **整改建议**:最小闸门:windows leg 末尾加一步"引擎能启动并应答 ready"的 PowerShell 冒烟(复用 DesktopServer,断言 ready 响应含 schema 字段即可,不必全流程);中期与 P2-5 gradle 化合并后,`:harness` 的 JVM 测试矩阵直接覆盖 Windows leg。

## 五、KMP 化路线建议

**候选面盘点(按三查闸门口径,roadmap 决策 5)**:
- 可进 commonMain:desktop core 10 个文件中纯类型的 8 个 DTO(CommitProto/CandidateProto/CompositionProto/ContextProto/StatusProto/Candidates/RimeResponse/SchemaItem,零平台 import,与 app 侧 RimeProto.kt:8-145 的纯类型面重合;RimeKeyEvent.kt 因 `external fun parse` 与 Rime.kt stub 门面留平台面);行协议模型与编解码(现状无独立类型,P1-3/P1-4 抽出后即为纯 Kotlin);就绪探测/采样逻辑(纯 kotlinx 或纯同步代码)。
- 平台面(不进):`Rime.kt` stub 的 `external fun` JNI 声明(desktop Rime.kt:24-69)与回调注册(:17-21);DesktopServer 的 stdin/stdout/FileDescriptor 装配(DesktopServer.kt:50-70)与 watchdog;Android IME service + Compose 键盘;Swift IMK 壳;C++ 桥六文件。

**现实迁移顺序(四步,每步独立可验收)**:

1. **Step 0(现在,零基建)**:双事实源先对齐再收敛——按 P1-1 把 desktop stub 字段名与 app 词汇表统一(一次 golden 重录窗口);不做任何模块动作。
2. **Step 1(共享源集,仍走脚本)**:抽 `protocol-common` 共享 Kotlin 源集(P1-3 的三件套 + P1-4 的版本字段 + P2-4 的负载扩展落点),五入口脚本改 find 参数即可编译它;以"零 android.*/java.* 平台 import + commonMain 式引用闭包核对"为纪律(直接沿用 roadmap 三查①②,终闸留给 Step 2 的编译隔离)。
3. **Step 2(gradle 化,KMP 壳)**:`:desktop-engine` + `:harness` 建 JVM 模块,`protocol-common` 升为 `kotlin("multiplatform")` 源集(单 android()/jvm() target),与 roadmap 期 4 的 `:core:model`(RimeProto.kt/SchemaItem.kt 入 commonMain,doc/refactor/roadmap.md:93/:116)**汇合为同一个模块**——切忌长出第二个模型模块;五入口收敛(P1-6/P2-5 一并消解),commonMain 独立编译成为终闸。
4. **Step 3(Path B 接入)**:KotlinEngine 作为行协议第二后端(harness-design.md §2 driver 2,H4 验收 L0≥95%);协议模块已引擎无关,壳零改动换引擎;librime JNI 面整体降级为离线工具链。

**阻力点(如实列出)**:
- **Swift 侧无 KMP 消费通道**:不引 Kotlin/Native 的前提下,Swift 解码器(EngineClient.swift:10-25)保持手写镜像,靠协议版本字段(P1-4)与 golden 断言对齐;若日后要消除镜像,评估 Kotlin/Native 产出框架头(重依赖,不进本路线承诺)。
- **桌面 gradle 化与 harness-design H0 决策的时序**:H0 明言推迟,提前做需先收回该决策;建议在 H1 收口时(语料/golden 基建稳定后)启动 Step 2。
- **roadmap 期 4 的排期耦合**:`:core:model` 在期 4 落地,桌面线 Step 2 若先走,应做成期 4 可直接收编的形态(同包名、同词汇表),避免期 4 迁移时二次改名。

## 六、战略指令衔接章:Path B 对各组件影响矩阵

战略口径(台账 §6):短期 = Android fastpath + config 读取常量化;中期 = Path B Kotlin 引擎,运行时只读 prism/table/reverse bin + schema 生成 Kotlin 常量,yaml-cpp/config_compiler/Lua 全链退出运行时;librime 降级为离线编译工具链(roadmap 修订 2/3 已登记同向动作,doc/refactor/roadmap.md:355-390)。台账已知推论:Path B 落地则桌面桥作废、两条桥待修项随之关闭;均不属本轮修复项。

| 组件 | Path B 后命运 | 依据/条件 |
|---|---|---|
| C++ 桥 librime_jni(六 .cc) | **Android 短期保留**(fastpath 期内仍是 librime 运行时);**中期随 Path B 全线退役** | 台账 §6;Android 侧转 KotlinEngine 后 JNI 桥无消费方 |
| 桌面桥专用链(host CMake、build-bridge.sh/.ps1、jni-utils.h:82-89/:155-160 桌面分支) | **作废** | 桌面线换 KotlinEngine 后无 JNI 可桥 |
| 桥待修项①stub 字段错位(P1-1) | **随桌面桥作废关闭**;若 Path B 前先修则须 golden 重录窗口 | 台账 §6 独立工作项;裁决见下文 |
| 桥待修项②NewStringUTF(P1-2) | **桌面侧随桥作废关闭**;README 判定 Android 不受影响,若真机复核证伪则该项转为 Android 项存续(P2-8 建议复核即为此) | harness/README.md:52-55 |
| DesktopServer 主循环 + 行协议 | **存续**,仅换后端(JNI → KotlinEngine);P1-3/P1-4/P2-4 的协议投入全额存活 | harness-design.md §2 预留 driver 2 |
| harness/Replay/golden | **存续并升级**为 Path B 的验收闸门(H4,L0≥95%);golden 当前仍是 librime oracle,桥缺陷修复才触发重录 | harness-design.md §8 |
| macOS Swift 壳 | **不动**(只说协议);受益于引擎更换后的启动时延改善 | EngineClient.swift 对引擎实现零感知 |
| Android Compose IME + core 门面 | 短期随 fastpath 强化;中期 Rime.kt JNI 门面让位 Kotlin 门面,期 3/4 的 KMP 纪律直接受益 | roadmap 期 3/4 |
| YAML 运行时链(yaml-cpp/config_compiler/Lua) | **运行时归零**;librime 保留为构建期预编译工具(与修订 2/3 同链) | 战略指令原文;doc/refactor/roadmap.md:366-377 |
| 资产形态(桌面包随发的 shared YAML + build bin,desktop-ci.yml:171-172) | 短期起 YAML 源退出分发,只发预编译产物;schema 常量由构建期生成器产出(常量层即 Path B 的输入) | 修订 2"YAML 源不再随 APK 分发" |
| CI 四 leg | android-apk:短期存续;macos/linux/windows:桥构建步骤退役、precompileRimeData 与 kotlinc 打包存续、增引擎冒烟步(P2-9) | desktop-ci.yml 现状结构 |
| 五处 kotlinc 入口(P1-6) | 与 Path B 无直接耦合,由 Step 2 gradle 化消解;先收敛可降低 Path B 期间的构建摩擦 | 本报告 P1-6/P2-5 |

**投入裁决建议**(按"Path B 兼容性"排序,供用户立项参考):
1. **放心投入**(Path B 后全额存活):协议层三件——P1-3 采样器抽取、P1-4 版本握手、P2-4 负载扩展;KMP 路线 Step 0-1;P1-5 的 core 层解依赖(期 4 与 Path B 双向受益)。
2. **按时间表裁决**(Path B 落地即关闭):P1-1 stub 错位与 P1-2 NewStringUTF——若 Path B 排期在桌面线对外可用之前,两者都建议不修(省两次 golden 重录窗口);反之 macOS 产品化前必须修 P1-1(ascii 显示错)+ 裁决 P1-2(扩展 B 用户面)。两修复互斥规则(台账固定决策:伴随 golden 重录、与转换窗口互斥)继续有效。
3. **与 Path B 无关但应修**:P1-6/P2-5/P2-6/P2-8/P2-9(构建面)、P2-3(错误可见性)、P2-7(文档纠偏)。

## 结论重述

七维评审完成:桌面三层与 harness oracle 契约是健康的架构核心;协议类型漂移、五入口构建面、Android core 向上依赖是三项结构性债务,全部有低成本收敛路径且与 Path B 方向同向;两条既有桥缺陷已按战略口径给出裁决建议。P0=0,当前终态可交付。

SUMMARY: P0=0 P1=6 P2=9
