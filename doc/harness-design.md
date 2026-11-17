# 候选输出 harness 设计(librime oracle → Kotlin 内核的安全绳)

> 2026-11-17 · 为路线二(自研 Kotlin 运行时,librime 降级为离线工具链)提供回归保障。
> 兼职:执行 `doc/refactor/roadmap.md` 期1 验证项——主方案缺 gram / 缺 predict.db 时的部署运行行为,在 host JVM 上直接观测,不必真机。

## 1. 前提(已实证)

- librime_jni 在桌面 JVM 加载冒烟 PASS:`clang++ -dynamiclib -force_load app/src/main/jni/host/build-host/librime-build/lib/librime.a`(+opencc/leveldb/marisa/yaml-cpp/snappy/spdlog 静态库)→ `librime_jni.dylib`;唯一桥改动 = `jni-utils.h:82,149` 的 AttachCurrentThread 签名差(Android NDK `JNIEnv**` vs 桌面 JDK `void**`,2 行 reinterpret_cast);load → startupRime → 部署线程 C++→Java 回调 → exitRime 全链路通。
- 数据目录经环境变量注入(`rime_jni.cc:44-46`,getenv `RIME_USER_DATA_DIR` / `RIME_SHARED_DATA_DIR`)→ **每用例独立目录 = 每用例独立进程**,天然隔离 userdb 与部署缓存。
- 结论:harness 全程跑在 host JVM,不依赖设备;Linux/Windows 产物随桌面工作补齐后同一套直接可用。

## 2. 架构

新增 `:harness` 模块(`kotlin("jvm")`,进 `settings.gradle.kts`;零 Android 依赖):

```text
harness/
  replay/    ReplayDriver:external fun 直接声明(绕开 core/Rime.kt 的 BuildConfig 依赖)
  corpora/   用例 *.jsonl
  golden/    oracle 基线 *.jsonl(绑定 librime 版本 + assets 哈希 + 桥补丁哈希)
  traces/    实跑轨迹 *.jsonl
  diff/      比对器 + 报告(按场景一致率矩阵)
```

两个 driver 后端实现同一 replay 协议:

1. `LibrimeOracle` — host JVM 加载 librime_jni.dylib(macOS 已实证)。
2. `KotlinEngine` — 路线二 PoC 的第二实现,同一协议喂同一 golden。

## 3. 用例格式(corpora/*.jsonl,一行一用例)

```json
{"id":"xhfast-fullcode-2char","tags":["fullcode","word"],"setup":{"schema":"flypy_xhfast","options":{},"userDict":"empty"},"steps":[{"key":"n"},{"key":"i"},{"key":"h"},{"key":"a"},{"key":"o"}]}
```

- `key`:单键名(`"n"`)或序列对象 `{"seq":"Control+a"}`;每步后采样一条 trace。
- `userDict`:`empty`(临时空目录)| `snapshot:<id>`(预置用户词快照,调频用例用)。

## 4. 轨迹格式(traces / golden,每键一行)

```json
{"step":3,"rawInput":"nih","preedit":"nih","candidates":[{"text":"你好","type":"phrase","comment":"~ni hao"}],"commit":null,"status":{"asciiMode":false}}
```

## 5. diff 语义

- **L0**(必须逐键一致):每步的 commit 文本 + 首候选文本。
- **L1**(排序差异 = warn):前 N 候选文本多重集。
- **L2**(提示差异 = warn):注释/类型(编码提示、辅助码提示)。
- 报告:按 tags 分组的一致率矩阵;任一 L0 失败 = 整用例 fail。

## 6. oracle 录制纪律

- golden 绑定三元组:librime 版本(1.17.0)+ assets 哈希 + 桥补丁哈希;任一变化 → 脚本一键重录,人工 review diff 后入库,不自动覆盖。
- **照实录制缺件现状**(gram / predict.db 不在包内):golden 即产品真实行为基线;roadmap 期1 的「缺件部署运行行为」验证由 H1 直接产出(预期:octagram 检测不到语言模型即禁用语法、不阻塞部署——在 host 上即可确认)。

## 7. 语料场景(≥60 用例,全部对齐 flypy_xhfast 实际特性)

| tags | 场景 | 关键出处 |
|---|---|---|
| fullcode | 双拼全码单字 / 二字词 / 三字词 | speller algebra_flypy |
| jianpin | 简拼(fuzz 首字母) | flyhe_fast 底座 fuzz 规则 |
| fuzzy | 模糊音(jv↔ju 等) | `flypy_xhfast.custom.yaml:26-30` |
| auxcode | 辅码 `AB/CD`、`AB/C` | algebra_common derive(xhfast:366-371) |
| userdict | 用户词调频(commit 3 次后重查) | enable_user_dict |
| punct | 配对标点 / 连续标点 | pair_punct lua + punctuator |
| trigger | date / time / calculator / 农历 | date_time_seg + lua_translator |
| radical | 部件反查 `L | table_translator@flypy_radical |
| english | easy_en 切换与模糊匹配 | table_translator@easy_en |
| makeword | ` 精准造词、~= 自由造词 | make_sentence / free_make_word |
| emoji | emoji 开关 | simplifier@emoji |
| ascii | 英文段 / URL | recognizer patterns |
| grammar | 长句候选(缺件降级行为) | grammar 段 + max_sentences: 2 |
| predict | 联想(predict.db 缺件行为) | predictor 注入(xhfast.custom.yaml:57-58) |

## 8. 阶段与验收

| 阶段 | 内容 | 验收 |
|---|---|---|
| H0 | 冒烟产品化:构建与冒烟收进 `harness/smoke.sh`(gradle 模块化推迟到 H1 再议);`jni-utils.h` 2 行补丁上游化(`#ifdef` 分支) | `harness/smoke.sh` 一键出 dylib + Smoke PASS |
| H1 | ReplayDriver + 用例 schema + 隔离目录管理 | 10 用例重录两次 golden 逐字节一致 |
| H2 | 60+ 用例 golden 入库 | 本地重跑 diff = 0 |
| H3 | diff 报告 + 一致率矩阵 | 人为注入排序差异能被 L1 捕获、缺候选能被 L0 捕获 |
| H4 | KotlinEngine 后端接入 | 主路径场景 L0 ≥ 95%(起步目标) |

## 9. 风险与边界

- 唯一的桥改动是 `jni-utils.h` 补丁上游化;不再动 librime 本体。
- host 与真机的缺件行为预期一致(同一份 assets、同一部署逻辑);H1 后在真机对齐一次。
- `getRimeResponse` 的分页语义在 driver 里固定 `pagingMode=false`(全量),避免分页状态污染 diff。
- golden 是有版本的人工资产:librime 升级 = 重录 + review。
