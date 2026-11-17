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
