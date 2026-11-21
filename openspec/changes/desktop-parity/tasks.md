# Tasks

> 状态（2026-11-21）：安装/验收文档与 macOS 重装完成；平台实测与 Windows 批次待后续。

## 1. macOS

- [x] 1.1 `.app` 重建（含 Shift Release 语义）并安装到 `~/Library/Input Methods/`，codesign 通过
- [x] 1.2 `build.sh` 固定 `DEVELOPER_DIR`/`SDKROOT`，避免 CLT SDK 与 Xcode 链接器错配
- [ ] 1.3 真机：系统设置添加输入源 → 打字/候选/Shift 切中英（主人最终验收）

## 2. Linux

- [x] 2.1 `desktop/linux/install.sh` 一键安装 IBus 组件；协议客户端 6 单测通过
- [ ] 2.2 Linux 会话实测（安装引擎包、ibus-daemon 重启、添加输入源）

## 3. Windows

- [ ] 3.1 TSF DLL + broker（Windows CI 批次；契约见 `desktop/windows/README.md`）

## 4. 验收与状态

- [x] 4.1 `doc/verification-guide.md`（手机 + 三平台逐项清单、已知限制、回滚）
- [x] 4.2 `doc/master-plan.md` 状态回写
- [ ] 4.3 归档已完成的 OpenSpec change（待全部真机验收后）
