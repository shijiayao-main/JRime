# Implementation Notes — release-hardening（第一批）

## 2026-11-21

- 启动失败：`nativeStartup` 包 `runCatching`；失败记录 `Rime.startupError` 并
  `RimeLifecycleRegistry.resetAfterFailedStartup()` 回 STOPPED（STARTING 无合法 ON_STOP，
  这是唯一安全逃生口）；`RimeDaemon.startupErrorOrNull()` 供 UI 读取。
- 主界面：就绪等待改 `withTimeoutOrNull(8s)`；失败弹「输入引擎启动失败」对话框，
  重试走 `RimeDaemon.restartRime()` 并重新等待。
- 备份：`UserDataBackup`（zip：custom_phrase.txt / default.custom.yaml / userdict/*.txt；
  导出走 `UserDictManager.exportUserDict`，恢复走 `importUserDict` 并重启会话）；
  设置中心系统组新增导出/恢复动作（SAF + 恢复二次确认 + toast 词典数）。
- 诊断：系统组「复制诊断信息」写剪贴板（版本/构建/Android/机型/引擎错误/默认输入法）。
- 发布：`script/install-debug.sh`（按 ABI 选 APK → adb install → ime enable/set）。
- 全仓闸门：test + assembleDebug + spotless 绿（203+ 单测）；脚本 udev 实跑安装成功。
- 待主人：签名 secrets、真机矩阵、备份-重装-恢复回归、7 天日常观察。
