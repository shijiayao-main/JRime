# harness — 桌面 JVM 冒烟(H0)

以桌面 dylib 形态编译仓库自身的 `librime_jni` 桥源码,并在普通 JVM 里跑通
`load → startup → 部署回调 → schema 列表 → exit`。它是两条工作线共用的验证工具:

- **JNI 面裁剪**(openspec: `trim-jni-surface`):JNI 按名绑定,删一侧不报编译错,
  只有完整会话能暴露断链——每批裁剪后必须跑本冒烟。
- **内核替换**(路线二,`doc/harness-design.md`):H4 的 KotlinEngine 将作为第二
  driver 后端接入同一协议;本脚本是该线的 H0 起点。

`java/com/jiaoay/jrime/core/*.java` 是与 `jni-utils.h` GlobalRefSingleton 解析签名
逐条对齐的桌面 JVM 桩,只服务于 JNI_OnLoad 的类解析与回调回环,不是真实 Kotlin 类。

## 前置

- macOS host,已跑过 host 预编译:`app/src/main/jni/host/build-host/` 存在
  (librime.a + opencc/leveldb/marisa/yaml-cpp/snappy/spdlog 静态库)。
- JDK(`jni.h` 头文件)与 clang++。

## 运行

```sh
./harness/smoke.sh
```

产物在 `harness/build/`(根 `.gitignore` 的 `build/` 已覆盖)。预期末行
`SMOKE TEST PASS`。

## 边界

- 桌面与 NDK 的 `AttachCurrentThread` 签名差已由 `jni-utils.h` 的
  `#ifdef __ANDROID__` 分支吸收,仓库源码两端直接可编,无需打补丁副本。
- `build-host/` 产物被 gitignore,本机有效;CI 化与语料回放(H1+)之后推进。

## H1 用例回放

```sh
python3 harness/run_cases.py record --golden harness/golden     # 录制 golden(版本化资产)
python3 harness/run_cases.py record --golden /tmp/golden2       # 独立复录
python3 harness/run_cases.py diff --golden harness/golden --other /tmp/golden2
python3 harness/run_cases.py verify                             # 重放对照 golden
```

- 语料:`harness/corpora/baseline.jsonl`(12 条确定性场景;`date/time` 等随时间
  变化的场景不入 golden)。op 词汇表见 `Replay.java` 头注释;驱动经过
  `RimeKeyEvent.parse`(key.cc)解析键名,不手写 keysym 表。
- **oracle 是引擎级的**:引擎返回未处理的键(`handled:false`——ascii 模式下的字
  符、空输入时的逗号)由应用层自行上屏;trace 只记录引擎行为,H4 的 KotlinEngine
  对齐 L0 时需要同样的应用层语义。
- 隔离:一用例一进程一 fresh user 目录;shared staging 每次运行从 assets 复制一次
  (sources + 预编译 build 产物),引擎对其只读。
- 已知发现(确定性不受影响,待修):
  - 桌面 JVM 的 `NewStringUTF` 捣毁 4 字节 UTF-8 → CJK 扩展 B 区候选/注释在
    golden 中呈乱码;桌面桥的字符串转换层需要改为按字节构造 String,Android 侧
    不受影响。修复后 golden 需重录。
  - `setRimeOption("ascii_mode", true)` 的选项通知已到达(`type=2`),但随后的
    `getRimeStatus().isAsciiMode` 仍读 false——疑似桥内状态缓存,待查。
