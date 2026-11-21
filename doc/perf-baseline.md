# 基线：体积与启动（M4）

> 2026-11-21。环境为开发机模拟器（**emulator-5554**，arm64，Android 16 / API 37，2076×2152，
> debug 构建），真实设备数字请以验收时的实测为准。目的：给后续优化一个可比较的起点。

## 1. APK 体积

| 构建 | arm64-v8a | armeabi-v7a | x86 | x86_64 |
|---|---|---|---|---|
| debug | 77 MB | 76 MB | 77 MB | 77 MB |
| release（R8 + 资源压缩，未签名） | 57 MB | 56 MB | 57 MB | 57 MB |

说明：
- 体积主要在离线随包的 librime/opencc、rime 数据（词典 + 预编译 build）与 Compose 运行时。
- 已按 ABI 拆包；按设备架构安装单包。
- release APK 当前**未签名**（`*-unsigned.apk`）；签名需在仓库 secrets 配置 `SIGNING_KEY` 等
  （见 `doc/verification-guide.md` / `doc/changelog.md`），由主人最终配置。

复现：

```sh
./gradlew :app:assembleDebug      # app/build/outputs/apk/debug/*.apk
./gradlew :app:assembleRelease    # app/build/outputs/apk/release/*-unsigned.apk
```

## 2. 启动

`am start -W` 冷启动（force-stop 后）：

```
LaunchState: COLD
TotalTime: 1232 ms
WaitTime:  1235 ms
```

logcat 关键里程碑（同一轮）：

| 事件 | 相对启动 |
|---|---|
| Activity 进程启动（START） | 0 ms |
| `librime_jni.so` 加载完成 | +0.90 s |
| Rime userDataDir 就绪（`[rime-main] Rime`） | +0.96 s |
| 首帧显示（`Displayed ... +1s232ms`） | +1.23 s |

结论：debug 构建下应用冷启动 ≈1.2–1.3 s，引擎 native 库加载 ≈0.9 s；满足 master-plan
的目标区间（就绪 <2 s）。真实设备与 release 构建应更快，留待最终验收记录。

复现：

```sh
adb shell am force-stop com.jiaoay.jrime.debug
adb logcat -c
adb shell am start -W -n com.jiaoay.jrime.debug/com.jiaoay.jrime.ui.main.MainActivity
adb logcat -d | grep -E "librime_jni|\[rime-main\] Rime|Displayed"
```

## 3. 首键/日常手感

首键延迟与连续输入帧率需要在真机上用 `dumpsys gfxinfo` / 手动体感验收（本环境无屏幕录制权限）。
键盘表面为 Compose 渲染，候选翻页为客户端分页（M2），引擎走预编译数据（M0），
预期不触发 Trime 时代的 YAML 加载卡顿；请在验收时重点感受首键、长按连发、候选翻页。
