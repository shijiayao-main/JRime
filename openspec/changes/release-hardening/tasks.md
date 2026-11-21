# Tasks

> 状态（2026-11-21）：第一批（启动降级、备份/恢复、诊断复制、安装脚本）完成；真机与签名项待主人最终验收。

## 1. 启动失败降级

- [x] 1.1 `nativeStartup` 异常兜底 + `startupError` + 生命周期安全回退（可重试，不崩 dispatcher）
- [x] 1.2 `RimeDaemon.startupErrorOrNull()` 暴露
- [x] 1.3 主界面等待就绪加超时 + 启动失败重试对话框（文案/重试/稍后）

## 2. 整机备份 / 恢复

- [x] 2.1 `UserDataBackup` 导出 zip（custom_phrase + default.custom + 用户词典文本）
- [x] 2.2 恢复 zip（走 import 通道 + 重启引擎）
- [x] 2.3 设置中心系统组「导出备份 / 恢复备份」动作 + SAF 选择 + 恢复二次确认 + 结果提示

## 3. 诊断与发布

- [x] 3.1 「复制诊断信息」动作（版本/构建/设备/引擎错误/输入法）
- [x] 3.2 `script/install-debug.sh` 一键安装（构建/安装/启用 IME）
- [x] 3.3 全仓闸门（test/assembleDebug/spotless）

## 4. 待主人 / 真机

- [ ] 4.1 release 签名 secrets 配置（仓库 Settings → Secrets）
- [ ] 4.2 真机矩阵冒烟（API 26/33/36 + 折叠/大屏）
- [ ] 4.3 备份 → 卸载重装 → 恢复 真机回归
- [ ] 4.4 连续 7 天日常使用观察（崩溃/ANR/手感）
