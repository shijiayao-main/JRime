# Implementation notes: startup-fastpath

## 2026-11-15 — Task 3.1: precompileRimeData 构建期任务

### 机制与布局

- 新增 convention plugin `build-logic/convention/src/main/kotlin/RimePrecompilePlugin.kt`
  （id `com.jiaoay.jrime.rime-precompile`，在 `app/build.gradle.kts` 应用），三个任务：
  - `buildRimeDeployer`：配置/构建 `app/src/main/jni/host/CMakeLists.txt`（复用 spike 踩平的主机
    rime_deployer 工具链）到 `app/src/main/jni/host/build-host/`（已 gitignore，作为缓存，
    刻意不挂到 `clean`）。输入 = vendored 源树（librime、librime-lua(-deps)、octagram、
    predict、snappy、jni/cmake、host/CMakeLists.txt），输出 = deployer 二进制；源不变则
    UP-TO-DATE，不重编。Boost 树（~716M，按 tarball 哈希钉住）排除在指纹外。
  - `precompileRimeData`：组装 scratch（`app/build/rime-precompile/`）：拷 `assets/shared/`
    （**解引用** prelude 符号链接，等价 `cp -rL`，拷完断言 scratch 内无任何符号链接）+
    拷 `app/data/rime/dicts/`；scratch 内所有源文件 mtime 钉到固定纪元（2000-01-01），使
    compiled 配置里的 `__build_info/timestamps` 跨构建字节稳定；跑
    `rime_deployer --build <scratch-user> <scratch-shared> <scratch-user>/build`；
    校验 13 件预期产物（default.yaml + 3 方案 × schema.yaml/table/prism/reverse.bin）后
    全量落到 `assets/rime/build/`（staging 预置位，经既有 checksums 释放进用户目录
    `<externalFilesDir>/rime/build/`）。
  - `cleanPrecompileRimeData`：删 `assets/rime/build/`，挂到 `clean`。
- 接线：`generateDataChecksums`.dependsOn(`precompileRimeData`)；`precompileRimeData`
  .dependsOn(`buildRimeDeployer` + `installOpenCCData`——后者是 gradle
  implicit-dependency 校验强制的：`assets/shared` 同时是 installOpenCCData 的输出目录）。
- 编译输入源目录选择 `app/data/rime/dicts/`（而非任务原文的 `app/data/rime-src/`）：跟随
  `app/data/rime/prelude/` 既有布局（design §1 已确立 app/data/rime/ 为编译输入的家），
  并原样保留 `cn_dicts/`、`en_dicts/` 相对路径，使 `import_tables` 引用在 scratch 里不用改。

### 留/迁清单（逐项对照引擎实际读取行为敲定）

**迁出 APK（git mv → `app/data/rime/dicts/`，15 个文件）**：
`flypy_xhfast/flyhe_fast/flypy_radical.dict.yaml`、`melt_eng.dict.yaml`、`easy_en.dict.yaml`、
`cn_dicts/`（5）、`en_dicts/`（5）。
依据：librime 运行时只经 staging→prebuilt resolver 读 `.table.bin`
（`service.cc:183` `CreateDeployedResourceResolver`），从不读词典 YAML；
`DictCompiler::Compile` 在源缺失时走 "reuse existing table" 路径
（`dict_compiler.cc:92-100`：`build_table_from_source=false`，校验和直接取自现成 table）。
三个主词典源及其 import 链是宿主部署的编译输入；`easy_en`/`melt_eng`/`en_dicts` 现阶段
不被任何部署任务消费（deploy 只编译各方案 `translator/dictionary`，`compile_unit.cc:158-164`；
`table_translator@melt_eng` 在 flyhe_fast.schema.yaml:60 是注释状态）——其
`*.table.bin` 改动前后都不存在，对应 translator 均为 inert，行为无变化（见"遗留观察"）。

**留在 APK（运行时必需/用户侧）**：
- `default.yaml`、`default.custom.yaml`：配置链基座 + patch。维护期 `ConfigFileUpdate` 每次重展开
  （compiled default.yaml 的 `__build_info/timestamps` 记录 `default`+`default.custom`）。
- `flypy_xhfast/flyhe_fast/flypy_radical.schema.yaml`：**必须随 APK**。`WorkspaceUpdate` 对
  schema_list 逐项 `fs::exists` 检查源文件，缺失即 "missing input schema" → deploy 失败
  （`deployment_tasks.cc:205-210`）；且维护期每次经 `ConfigFileUpdate` 从源重编译 schema 配置
  （`deployment_tasks.cc:218-221`）。
- `flypy_xhfast.custom.yaml`、`flyhe_fast.custom.yaml`：schema patch 链（compiled schema 的
  `__build_info/timestamps` 记录了二者；flypy_radical 无 custom，记录为 0 = 缺失可接受）。
- `symbols.yaml`（prelude 符号链接）、`symbols.custom.yaml`：各 schema `punctuator:
  import_preset: symbols`（flypy_xhfast.schema.yaml:310）+ `__include: symbols.custom:/symbols`
  （:336）；compiled schema 的 timestamps 记录 `symbols`+`symbols.custom`。
- `punctuation.yaml`、`key_bindings.yaml`（prelude 符号链接）：当前 default.yaml 无 prelude 式
  `__include`，不在活跃链上，但属 prelude 用户面（任务口径点名保留，各 ~90B）。
- `flyhe.trime.yaml`、`trime.yaml`：Trime 主题/应用配置。
- `lua/`、`opencc/`（构建期生成 + emoji.json）、`fonts/`：运行时数据。
- `custom_phrase.txt`、`calendar.yaml`、`custom_predict_rules.yaml`、`flypy_chord_rule.yaml`：
  用户自定义槽位/样例（仅在注释型配置示例中被引用，体量可忽略）。

关键证据：compiled 产物自身的 `__build_info/timestamps` 就是"维护期会重读哪些源"的权威清单
（`ConfigNeedsUpdate` 按 `config_source_file` resolver = 用户目录→shared 逐项比对 mtime，
`deployment_tasks.cc:441-477`）。

### 验证证据（2026-11-15 实测）

1. **干净状态一次构建出完整资产**：`rm -rf app/build app/src/main/jni/host/build-host
   app/src/main/assets/rime/build` + `rm app/src/main/assets/checksums.json`（资产集变动后
   强制全量重生成，规避 DataChecksumsPlugin 增量累积陷阱；boost 缓存未动）后，
   `./gradlew :app:assembleDebug`：主机工具全量构建 `[100%] Built target rime_deployer`，
   precompileRimeData 产出 13 件 staging 产物，`BUILD SUCCESSFUL in 1m 42s`。
   （首次跑被 gradle implicit-dependency 校验拦下 assets/shared 与 installOpenCCData 的重叠，
   补上 dependsOn 后通过。）
2. **工具缓存化**：紧接二次 `./gradlew :app:assembleDebug`：
   `buildRimeDeployer UP-TO-DATE` + `precompileRimeData UP-TO-DATE`，`BUILD SUCCESSFUL in 7s`。
   词典源探针：改动 `flypy_xhfast.dict.yaml` → `precompileRimeData` 重跑而
   `buildRimeDeployer UP-TO-DATE`（14s）；回滚后重跑，13 件产物 sha256 与探针前逐一相同
   （`sha256sum -c` 13/13 OK）——管线字节级确定。
3. **checksums.json 全量覆盖新集合**：regenerate 后 119 条：`dict.yaml` 0 条、`cn_dicts` 0 条、
   `rime/build/` 13 条文件 + 2 条目录、shared 用户侧文件齐全。
4. **APK 体积（debug，四 split）**：改动前 62.58 / 61.44 / 62.92 / 62.61 MB →
   改动后 48.47 / 47.32 / 48.81 / 48.49 MB（arm64-v8a 净减 **14.1 MB**）。APK 内容实测
   （unzip -l）：13 件 `assets/rime/build/*` 在包，`assets/shared/` 下 dict.yaml/cn_dicts/
   en_dicts 为 0 条。
5. **产物对齐 spike 记录**：`flypy_xhfast.table.bin` = 31,275,480 字节，与 spike 记录
   byte-size-for-byte 一致；compiled default.yaml 的 schema_list 解析为 flypy_xhfast
   （shared default.custom.yaml 的 patch 在宿主部署时生效）。
6. **不劣化**：`./gradlew :app:testDebugUnitTest` → 313 tests, 0 failures, 0 errors
   （golden/theme 测试只断言 shared/trime.yaml 与 flyhe 主题文件，均未迁移，无需适配）；
   `./gradlew spotlessCheck` → BUILD SUCCESSFUL。
7. **git 状态**：15 个 git mv 重命名（R，保留历史）+ 3 处构建接线改动；生成物
   `assets/rime/build/`（命中全局 `build/`，`.gitignore:11`）、checksums.json（:474）、
   shared/opencc 生成件（:475）均被既有规则忽略，**未新增任何 gitignore 行**；
   `git check-ignore` 验证 `app/data/rime/dicts/**` 不被忽略。

### 遗留观察 / 交给后续验收

- `easy_en`（英文内嵌模糊匹配）与 `custom_phrase`（pin_word.lua）槽位：其 `*.table.bin`
  在改动前后都不由任何部署任务产出，translator/lua Memory 加载失败即静默失效——
  现状如此，非本任务回归；5.2 回归若要启用，需为它们增加部署编译输入。
- 设备端行为预测（待 3.2/5.x 模拟器 + arm64 真机验收确认）：维护期配置链照常重展开
  （设备源 mtime ≠ 记录值），词典零编译（reuse existing table）；若 schema 配置重展开导致
  prism 的 schema_file_checksum 漂移，`BuildPrism` 会从已复用 table 加载 syllabary 重建
  prism（`dict_compiler.cc:307-318`），秒级、正确。
- 主机工具链在全新机器上首次配置时 Boost 由 CMake 下载（tarball 哈希钉住，jni/boost-*.tar.xz
  已 gitignore）；断网环境的预热方式沿 spike 既有结论，未在本任务改动。

## 2026-11-15 — Task 3.2: 启动链去主动部署化 + deploy 菜单降级为修复路径

### 两条链的分界（实现后的形态）

- **资产释放链**（保留，且是预置产物到达引擎的唯一通道）：`DataManager.sync()` 在每次引擎
  启动时跑 checksums diff，把 APK 内资产（含 `rime/build/` 13 件 staging 预置产物）释放进
  `<externalFilesDir>/`。本任务只加固它，不改布局。
- **部署链**（YAML→bin 全量编译维护）：启动**完全不触发**；`deploy()`（菜单/横幅/键盘
  DEPLOY 命令/adb 广播）= 修复路径，`updateConfig()`（键盘"更新配置"）= 增量维护，二者是
  仅有的显式维护入口。

### 启动链改动（结构性证据）

- `librime_jni/rime_jni.cc`：`startup()` 去掉 `fullCheck` 参数、**不再调用
  `rime->start_maintenance`**；新增 `startMaintenance(fullCheck)` 方法与 JNI 导出
  `Java_..._startRimeMaintenance`。`startupRime` extern 签名从 4 参改 3 参——启动调用在
  结构上**无法**请求部署。arm64 `librime_jni.so` 实测导出新旧两个符号。
- `core/Rime.kt`：`startRime()`（原 `startRime(fullCheck)`）= sync + ensureDefaultCustomConfig
  + `startupRime(3参)`；`nativeStartup()` → `startRime()`，READY 即刻发射（不再等部署）。
  grep 证据：`startRimeMaintenance` 全部调用点 = `deploy()`（true）与 `updateConfig()`
  （false）各一处；启动路径（`Rime.kt:65`）无任何维护调用。

### 修复路径（deploy 菜单）——为什么必须"先重释放再全量重编"

3.1 把词典 YAML 源迁出 APK 后，设备端全量重编**恢复不了 table.bin**：
`dict_compiler.cc:112-117` 在"源缺失且现成 table 也缺失"时直接返回 false。而释放记录
（checksums.json 部署记录）仍宣称产物"已部署"，sync 不会重释放——删掉 `build/` 后 deploy
菜单两头都够不着。因此 `deploy()` 增加重释放前置步骤：

1. `DataManager.revalidateStagingRelease()`：对记录中 `rime/build/` 前缀的条目逐件
   sha256 比对磁盘（流式 MessageDigest，格式与构建期 guava `Hashing.sha256().toString()`
   一致：64 位小写 hex），缺失/损坏的条目从部署记录中剔除并清掉记录的 manifest 哈希；
2. `startRime()` 内的 sync 把被剔除的产物从 APK 重新释放（`DataDiff` 将其重新判为
   CreateFile）；
3. `startRimeMaintenance(true)` 全量检查：配置链重展开 + 词典 "reuse existing table"
   （零编译，秒级）。

### 全新安装且预置产物缺失（不得软锁）

启动可完成（引擎无产物也照常 READY，不再有部署等待）；主界面复用 setup_banner 模式新增
`deploy_banner`（`?attr/colorPrimary` + `?android:attr/textColorPrimaryInverse`——AppCompat
主题下安全；刻意不用 Material 专属的 `?attr/colorOnPrimary`，AppCompat 下解析即
InflateException），`StagingArtifacts.isReady`（default.yaml + ≥1 组 schema/table/prism，
staging 与 prebuilt fallback 目录同查，与引擎 deployed resolver 的目录序一致）判定缺失即
显示，三语文案指引用户点横幅或工具栏"部署"。横幅与菜单共用 `runRepairDeploy()`，deploy
完成/失败后回主线程刷新横幅；onResume 与引擎 READY 后各刷新一次。修复 deploy 失败不再让
未捕获异常炸掉 activity 协程（runCatching + Timber.e，横幅保持可见可重试）。

### DataManager.copyFile 容错（清单内文件缺失不再崩 rime-main）

主人亲历的 FATAL 根因：`AssetManager.open(filename)` 对清单内缺失条目抛
FileNotFoundException，沿 `RimeDispatcher` 的 `internalScope.launch`（"rime-main"线程）
未捕获直达默认 handler 杀进程。现 `sync()` 语义：

- 按 diff 条目逐个 `runCatching`（拷贝与删除两侧都包），失败仅 `Timber.e` 记录并跳过，
  **不抛、不崩**；
- 被跳过文件在**日志**（逐条 + 汇总 `Data sync incomplete: N entry(ies) skipped`）与
  **部署结果**（`sync()` 返回 `DataSyncResult(skippedFiles)`，`Rime.startRime` 对非完整
  结果再打一条 `Asset release incomplete`）中可见；
- **不出现"部署半成品却报成功"**：失败的拷贝条目从部署记录剔除（下次 sync 重试）、失败
  的删除条目保留陈旧记录（下次仍 diff 为 Delete）、记录的 manifest 哈希清空（禁用
  sha256 快路径直到一次完全成功的 sync 恢复它）——`DataChecksums.afterRelease` 纯函数承载
  该语义，kotest 验证三种情形 + 下次 diff 的重试行为；
- checksums.json 本身读不出来（更上游的坏构建）也不再崩：记录一条 error、保留既有释放
  树、返回 `DataSyncResult(listOf("checksums.json"))`。

### 验证证据（2026-11-15 实测）

1. **启动链不触发部署**：结构证据如上（extern 签名 + 调用点 grep + so 符号表）。
2. **修复路径三段式本地场景**（主机 `rime_deployer --build` = `WorkspaceUpdate::Run`，
   与 `start_maintenance(true)` 调度的是同一任务，`rime_api_impl.h:84`/`rime_deployer.cc:126`）：
   - A 健康 staging 树 + 全量部署：SUCCESS，dict-compile `Pass 1` 行数 **0**，13 件产物无损；
   - B 删 `build/` + **不重释放** + 全量部署：**FAILED**（`dict_compiler.cc:115` "neither
     … .dict.yaml nor … .table.bin exists" → 3 个 compile unit 失败）——坐实"必须重释放"；
   - C 删 `build/` + 重释放（revalidate→sync 语义的等价模拟）+ 全量部署：SUCCESS，
     `Pass 1` **0**，13 件产物齐，`flypy_xhfast.table.bin` 与 APK 预置件 sha256 逐一相同
     （962b40e985ba…ef95f7）。
3. **单测**：`./gradlew :app:testDebugUnitTest` → **331 tests, 0 failures, 0 errors,
   0 skipped**（3.1 基线 313 + 新增 18：StagingArtifactsTest 7、DataChecksumsTest 4、
   StagingReleaseTest 7，后者含真实临时文件的流式哈希正确性/缺文件/损坏/重试链验证）。
   golden/theme 既有测试无需适配（未触及主题文件）。
4. **构建**：`./gradlew :app:assembleDebug spotlessCheck` 全绿（native 增量重编含 rime_jni.cc）。

### 行为变化备忘 / 交给冒烟与验收

- 启动不再发 deploy 进度/完成通知（维护不跑自然没有）；`DeployMessage.Start` 触发的
  `OpenCCDictManager.buildOpenCCDict()`（.txt→ocd2 预转换）也随之移到显式 deploy/
  updateConfig 时才跑——vendored OpenCC 自带 TextDict 直接加载 .txt（`deps/opencc/src/
  TextDict.cpp`），简繁/emoji 候选转换不依赖该转换，首次 deploy 后照旧得到 ocd2。
- `updateConfig()` 保留增量维护（detect_modifications），用户改 YAML 后经键盘"更新配置"
  生效的既有路径不变；键盘 DEPLOY 命令与 adb 广播照旧汇入 `deploy()`（现兼修复语义）。
- 引擎"从未跑过任何维护"直接加载 staging 产物的端到端确认（首启秒级 READY + 出字）留给
  冒烟阶段：加载路径与 spike 2.2 已验证的 deployed resolver 相同，差异仅在于设备端不再
  重展开配置链（产物即宿主编译态，patch 链已在宿主侧生效，见 3.1 记录）。
- `RimeDaemon.restartRime` 无调用方，其"重启即增量维护"的隐含行为随启动去部署化消失，
  属死代码语义变化，未清理（不顺手清 unrelated code）。


## 2026-11-15 — Task 5.1（模拟器口径）：Android 16 x86_64 全新安装实测

### 环境与口径

- 构建：`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL；x86_64 产物
  `app/build/outputs/apk/debug/com.jiaoay.jrime-4842b0fb-x86_64-debug.apk`（66,241,093 B，含预置产物）。
- 设备：emulator-5554（AVD `Medium_Phone_API_36.1`，Android 16 / API 36，x86_64，
  `sdk_gphone64_x86_64`）；测完保持运行未回收。
- IME 全名 `com.jiaoay.jrime.debug/com.jiaoay.jrime.ime.core.TrimeInputMethodService`；
  卸载旧包后 `adb install -t` 全新安装，`ime enable` / `ime set` 一次成功（未触发重试）。

### 全新安装启动耗时（logcat threadtime 毫秒级时间戳）

| 事件 | 时刻 |
| --- | --- |
| 安装完成（PackageManager: installation completed） | 10:02:26.447 |
| IME 进程拉起（ActivityManager: Start proc …for service） | 10:02:28.187 |
| `RimeDaemon start rime` → nativeStartup | 10:02:29.042 |
| `DataManager … Synced!`（首装资产释放完成） | 10:02:30.056 |
| `Starting rime with:`（native 初始化开始） | 10:02:30.065 |
| `ConcreteEngine::InitializeOptions`（引擎初始化完成） | 10:02:31.068 |
| 首个输入会话 StatusMessage（flypy_xhfast 飞鹤快拼） | 10:02:34.668 |

**安装完成 → 引擎就绪 ≈ 4.6s**（26.447→31.068）；进程内分解：首装资产释放 ≈1.9s
（28.187→30.056），引擎初始化 ≈1.0s（30.065→31.068）；到首个输入会话可响应 8.2s
（含触发间隙）。对照期 1 基线 105s、spike 16s：**4.6s**。

### 证据

1. **零词典编译**：全新安装启动全程 logcat 无 `Pass 1/2/3`、无 building table/prism、
   无 dict compiler 跑动；引擎直接 `loading table file / prism file` 用户目录
   `rime/build/*.bin`（flyhe_fast、flypy_xhfast 命中预置产物，`found double array
   image of size …`）。
2. **维护检查**：启动路径**零维护跑动**（logcat 无任何 maintenance/deployment 任务行；
   `startupRime` 仅 `Rime::Instance().startup()`，`startRimeMaintenance` 只由
   `deploy()` / `updateConfig()` 显式调用）——强于"秒级完成"口径：正常启动根本不跑。
3. **出字**：Google 搜索框聚焦后发键序 `n i h k`：`preedit=ni‸`、`commitTextPreview=你`、
   `BulkCandidates(candidatesCount=16)`；空格上屏 `CommitTextMessage(text=䭲)`，
   uiautomator dump 搜索框 `text="䭲"`。候选/自绘选单无 accessibility 文本（IME 窗口在
   dump 中零节点），出字证据以 logcat 引擎消息 + 落框文本为准。
4. **修复路径（3.2 验收项）**：`rm -rf files/rime/build/`（13 件）→ 打开 MainActivity：
   deploy banner 出现（"Precompiled input data is missing, so nothing can be typed
   yet — tap here or use Deploy in the toolbar to rebuild it"）→ 点工具栏 Deploy →
   `DeployMessage(Start)` 10:05:03.018 → `(Success)` 10:05:10.682，**≈7.7s**，
   `3 tasks ran: 3 success, 0 failure`；`build/` 13 件全部恢复、banner 消失；
   Test input 界面发 `n i` 复现 `preedit=ni‸` + 16 候选、空格上屏
   `CommitTextMessage(text=䭲)` —— **恢复出字 ✓**。
   - deploy 语义实测：staging table 重新释放后校验复用（`reuse existing table` ×3，
     无 YAML→table 编译 Pass——词典 YAML 已不随包，与 3.2 设计一致）；3 个 schema 的
     `prism.bin` 全部重建（`building prism… / saving prism` ×3）；schema.yaml /
     default.yaml 重新生成。
5. **如实记录的非阻塞观察**：`easy_en.table.bin` 不在预置清单（APK staging 与设备端
   `build/` 均为 13 件、完全一致；该词典报 `Error loading table for dictionary
   'easy_en'`，easy_en 无候选输出，主方案不受影响）；`predict.db` 与
   `zh-hans-t-huayu-v7-bgw.gram` 未随包（error opening 提示，不影响出字）；首启
   MainActivity 依次弹应用内通知权限对话框与系统通知权限对话框（GRANT → Allow）；
   force-stop 回退输入法怪癖未触发（全程未 force-stop）。

## 2026-11-15 — 修复：easy_en 内嵌英文段预置产物缺失（3.1 预置清单补全）

**根因**：librime 部署的词典编译单元只覆盖各 schema 的主词典
（`compile_unit.cc` `CreateDictionaryCompileUnit` 仅读 `translator/dictionary`，
`compile_unit.cc:141-149`）。easy_en 自 single-schema 起降级为 flypy_xhfast 的
内嵌段（`table_translator@easy_en` + `easy_en.lua` 的 `Memory(..., "easy_en")`），
不再是独立 schema → 无任何编译单元会产出 `easy_en.table.bin`。该缺口自
single-schema 实施起即潜伏（当时设备部署也只有 3 个编译单元），期 2 冒烟以
logcat `Error loading table for dictionary 'easy_en'` 抓到。

**修复（机制同 flyhe_fast 隐藏底座）**：
1. 新增 `shared/easy_en.schema.yaml` 最小隐藏 schema：speller 沿旧独立方案的
   字母表（恒等映射——旧 schema 的 `__include: algebra_flypy` 在其独立编译树内
   本就悬空、解析为空代数，行为一致），`translator/dictionary: easy_en`；不带
   engine/lua/ecdict 引用（那些在 single-schema 中已死，全量恢复会炸部署）。
2. `flypy_xhfast.schema.yaml` `dependencies:` 增 `easy_en`（隐藏经依赖部署，
   不进 schema_list、不可切换）。
3. `RimePrecompilePlugin.kt`：`SCHEMAS` 增 `easy_en`（预期产物 13→17 件）；
   `stageArtifacts` 补"多余文件"fail-fast（独立评审发现项：staging 里超出预期
   清单的文件一律构建失败，防未来 librime 升级静默夹带）。
4. `rm app/src/main/assets/checksums.json` 强制全量重生成（增量陷阱惯例处置）。

**验证（2026-11-15，模拟器 Medium_Phone_API_36.1 / Android 16 x86_64）**：
- `:app:precompileRimeData` staged 17 件（含 easy_en.table/prism/reverse.bin +
  easy_en.schema.yaml）；checksums 124 条 = 17 rime/build 文件 + 目录条目 + 其余，
  无旧路径残留。
- `:app:assembleDebug` + `spotlessCheck` + `:app:testDebugUnitTest`（331 用例）全绿。
- 全新安装：释放链 logcat `Diff: CreateFile(path=rime/build/easy_en.*.bin)` 四件 +
  `shared/easy_en.schema.yaml`；全程**零** `Error loading table`。
- **英文内嵌端到端**：搜索框聚焦，键序 `h e l l o` + `*`（KEYCODE_STAR=17，
  触发 recognizer `easy_en: "^[a-zA-Z]+[-_']*[*][a-zA-Z]*$"`）：
  `segmentation: [hello*|0,6{easy_en}]` → `composition: [{easy_en}hello*=>hello]`、
  `commitTextPreview=hello`、`BulkCandidates(candidatesCount=3)`；空格上屏
  `CommitTextMessage(text=hello)` ✓。
- 回归：中文出字 `nihk` → 16 候选、`commitTextPreview=䭲`（与基线一致）；
  简拼 `nh` → 16 候选、`commitTextPreview=囊`。
- 遗留目验项（自绘 UI 无 accessibility 文本，机器只能看引擎消息）：
  部件反查 `` `L `` 与单字表情上屏入口是**主题屏幕按键**（backtick 不在
  flypy_xhfast 的 speller alphabet `…&_-/` 内，物理键注入不可达）；
  简拼 fuzzy 前排的候选排序效果——留主人目验/真机抽查。

**模拟器注入经验**：`input text` 在 IME 未连接时直接提交不经组合链（表现为
文字落框但引擎零日志），需先点输入框唤起 IME 再逐键 `input keyevent`；
字母 KEYCODE 自 A=29 顺序排列（G=35、H=36、J=38、L=40、N=42——曾按错误记忆
把 35 当 H，打出 "gejjo" 排查了一轮）。

**5.1 稳态口径（模拟器，2026-11-15 补测）**：模拟器重启 + 多轮进程级冷启动
（kill 后系统即时重拉）实测，logcat 毫秒时间戳：
`RimeDaemon start rime` 10:24:51.958 → `DataManager Synced!` 10:24:52.832
（稳态 diff 零拷贝，0.87s）→ `Starting rime with:` 10:24:53.005（1.05s）→
按键触发首个输入会话惰性初始化 `starting engine.` →（config chain 读取
~0.6s）→ 可出候选。**进程冷启动→引擎就绪 ≈1.0s，打字可用 ≈2s 内**；
全程零 Pass 1/编译/维护跑动（对照：首装口径 4.6s 含 1.9s 资产释放、
期 1 基线 105s）。口径注意：首装口径终点是 InitializeOptions，稳态口径
终点是 Starting rime（引擎惰性启动由首个会话触发），真机验收时统一复测。
另：未绑定焦点时 IME 进程会被系统作 cached 进程回收（观测到自行死亡），
属正常生命周期，非泄漏。
