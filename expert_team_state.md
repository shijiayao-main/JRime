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
