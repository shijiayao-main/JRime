# JNI 与资产裁剪清单

> 2026-11-17 · 基于 vendored librime 1.17.0 与 main(dca8717)工作树实测。
> 定位:与 `doc/refactor/roadmap.md`(r6)互补——路线图管资产/启动链/Room/模块化,本清单管 **JNI 面与死资产**;每批可独立落地为一个 openspec change(建议名 `trim-jni-surface`),不与在途功能混线。
> 原则:只删「零调用死代码与死资产」,不动 librime 核心算法(fork C++ 删 config 的路线已评估否决:48 个文件包含 config 头,config 是装配机制而非独立层);每个目标都附可复现的核查命令。

## 1. 证据基线

- 主方案是 **flypy_xhfast**(`default.custom.yaml:13`、`rime/build/default.yaml:136`、`core/data/UserDataManager.kt:35` 三处一致)。flyhe_fast / flypy_radical / easy_en 是它的 dependencies(`flypy_xhfast.schema.yaml:15-18`,功能底座/部件反查/英文)——**四个 build 目录与 schema 闭包一个都不能删**。
- grammar 配置启用(`flypy_xhfast.schema.yaml:126-135`,`zh-hans-t-huayu-v7-bgw`)但 .gram 数据不在包内(roadmap 决策 2:不内置、保持 native 链接、经资产包补件)→ **octagram / predict 插件不裁**(`rime_jni.cc:18-26` 链接保留)。
- JNI 面全仓 47 个 external fun 声明(core/Rime.kt 裁剪前 29、其他文件 18);librime_jni C++ 共 1,380 行(Trime 血统)。
- 资产 110MB:rime/build 82M + fonts 25M + shared 2.6M(opencc 1.4M、lua 368K、字体 708K)。

## 2. T1 零引用死代码(直接删)

| # | 目标 | C++ 位置 | Kotlin/资产位置 | 证据 |
|---|---|---|---|---|
| 1 | `deployRimeSchemaFile` | `librime_jni/rime_jni.cc:293` | `core/Rime.kt:500` | 全仓 0 调用(on-device 逐文件部署已被离线预编译取代) |
| 2 | `deployRimeConfigFile` | `librime_jni/rime_jni.cc:300` | `core/Rime.kt:503` | 全仓 0 调用 |
| 3 | `syncRimeUserData` | `librime_jni/rime_jni.cc:309` | `core/Rime.kt:509` | 全仓 0 调用(用户目录同步走 git/WorkManager 通道,roadmap §161) |
| 4 | `revert_commit_word.lua` | — | `assets/shared/lua/` | 唯一引用在 `flypy_xhfast.schema.yaml:99`,注释行 |
| 5 | `custom_predict_rules.yaml` | — | `assets/shared/` | 唯一引用在 `flypy_xhfast.custom.yaml:327`,注释行(predict.db 未内置,规则库同惰性) |

## 3. T2 接口级死方法(接口声明 + Kotlin 实现 + JNI 三层一起删)

| # | RimeApi 接口 | Kotlin 实现 | JNI C++ | 证据 |
|---|---|---|---|---|
| 6 | `availableSchemata`(`RimeApi.kt:55`) | `Rime.kt:207` → `getAvailableRimeSchemaList`(`:576`) | `levers.cc:98` | 除声明/实现外 0 引用 |
| 7 | `enabledSchemata`(`RimeApi.kt:57`) | `Rime.kt:209` → `getSelectedRimeSchemaList`(`:579`) | `levers.cc:105` | 同上 |
| 8 | `setEnabledSchemata`(`RimeApi.kt:59`) | `Rime.kt:211` → `selectRimeSchemas`(`:582`) | `levers.cc:112` | 同上 |

**保留的同族方法(勿动,均有真实调用方):**

- `selectedSchemata` / `selectedSchemaId` / `selectSchema` — `EnabledSchemaPickerDialog.kt:22/25/39`;对话框由 `CommonKeyboardActionListener.kt:27` 挂接,键盘可达。
- `simulateKeySequence` — `LiquidWindow.kt:111`(符号面板模拟按键)。
- `startRimeMaintenance` — `Rime.kt:132/154`,部署链核心。

## 4. T3 决策项(不默认执行)

- **D1 opencc**:zh_simp / zh_trad / zh_hk / zh_tw / emoji 五个 simplifier 全部在用(`flypy_xhfast.schema.yaml:107-112`);只有产品决定砍字型/emoji 切换才可减(1.4MB 数据 + `librime_jni/opencc.cc`)。
- **D2 fonts 25M**:生僻字字体是产品决策,本清单不动。
- **D3 flypy_chord_rule.yaml**:xhfast:377 仅注释引用(并击未启用),删除无风险,可与 T1 同批。

## 5. 明确不裁(防误删)

1. octagram / predict 的 native 链接与模块注册——资产包补件生效的前提。
2. 四个 schema 的 `build/*.bin` 产物与 `shared/*.yaml` 引用闭包。
3. librime 核心各层(路线三已否决)。
4. gear 中被 xhfast 引用的全部组件(23 翻译器 / 18 切分器 / 17 过滤器,`flypy_xhfast.schema.yaml:37-123`)。

## 6. 验证清单(每批删除后全跑)

1. `./gradlew :app:testDebugUnitTest`(基线:175 用例全绿,HEAD 实测)。
2. host 冒烟:裁 JNI 后重新编 `librime_jni.dylib` 并加载(startup → 部署回调 → exit;方法见 `doc/harness-design.md` H0)。
3. 真机:冷启部署 → 输入 10 键 → 候选/上屏/符号面板。
4. APK 与 .so 体积前后对比。

## 7. 工作树纪律

2026-11-17 更新:数字行/候选条功能已提交并推送(88076d7,另有 1f0c163/dca8717 两个构建修复);解压出的 `app/src/main/jni/boost-*/` 目录已补进 .gitignore,工作树干净。裁剪动手时:单独成 openspec change + 独立 commit,不与其他改动混线。
