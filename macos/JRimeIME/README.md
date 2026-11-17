# JRimeIME — macOS 输入法壳(IMK)

Swift IMK 壳 + JVM 子进程引擎(`desktop/engine` 的 `DesktopServer` 行协议,与
harness/桌面调研同一套架构:薄原生壳 + 行协议引擎)。候选窗用系统
`IMKCandidates`,预编辑走 inline marked text;引擎未处理的键(`handled:false`)
透传给系统插原字符——与 harness 建立的引擎级语义一致。

## 构建

```sh
./macos/JRimeIME/build.sh
# 产物:macos/build/JRimeIME.app(内含 jlink runtime、引擎 classes、
# librime_jni.dylib、rime 资产树;ad-hoc 签名)
```

前置:JDK(jlink)与 `app/src/main/jni/host/build-host/` 已就绪(与
`./harness/smoke.sh` 相同要求)。

## 安装与试用

```sh
cp -R macos/build/JRimeIME.app ~/Library/Input\ Methods/
# 系统设置 → 键盘 → 输入法 → 简体中文 → 添加「JRimeIME」
# 切到该输入法后任意应用输入:nihc → 候选「你好」→ 空格或数字选字上屏
```

卸载:先在系统设置里移除输入法,再 `rm -rf ~/Library/Input\ Methods/JRimeIME.app`。

## 边界(v1)

- 设置界面、主题、候选窗跟随光标的精细定位均未做(候选窗为系统样式)
- 引擎热启动跨输入源切换保留;JVM 子进程空闲 10 分钟自动退出(服务端看门狗)
- 桌面桥已知缺陷(见 harness/README.md):4 字节 UTF-8 候选在桥的字符串转换层
  会乱码——生僻字候选显示不正确,Android 侧不受影响
- 真机验收项:候选窗定位、多屏 DPI、安全输入应用(密码框)行为——构建本机已
  验证到「协议端到端 + Swift 编译 + 组包签名」,输入法激活需人工在系统设置操作
