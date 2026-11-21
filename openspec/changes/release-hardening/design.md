# Design: release-hardening

## 1. 启动失败

- `RimeDispatcher.RimeController.nativeStartup()` 包 `runCatching`：
  - 成功：清空 `startupError`，发 ON_READY；
  - 失败：记录 `startupError: Throwable?`、`Timber.e`、调用 `RimeLifecycleRegistry.resetAfterFailedStartup()` 强制回 STOPPED（这是 STARTING → STOPPED 的唯一安全逃生口，避免非法状态迁移），不崩溃 dispatcher 线程。
- `RimeDaemon.startupErrorOrNull()` 暴露给 UI。
- MainActivity：首次就绪等待改为 `withTimeoutOrNull(8s)`；超时/失败弹「输入引擎启动失败」对话框，正按钮 `RimeDaemon.restartRime()` 后重新等待，失败再弹；不再让 `runOnReady` 永久悬挂导致横幅/等待卡死。

## 2. 用户数据备份（zip）

格式（可手动检查/修复）：

```
custom_phrase.txt
default.custom.yaml
userdict/<dictName>.txt     # 每个用户词典的文本导出
```

- 导出：`UserDataBackup.exportTo(OutputStream)`，词典枚举用 `UserDictManager.getUserDictList()`，逐词导出走 `exportUserDict(dest=zip,…)`。
- 恢复：`importFrom(InputStream)` 读 zip：`userdict/*.txt` → `UserDictManager.importUserDict`；`custom_phrase.txt`/`default.custom.yaml` → 写用户目录；完成后 `RimeDaemon.restartRime(fullCheck=false)`。
- UI：设置中心系统组三个动作行（SAF `CreateDocument("application/zip")` / `OpenDocument`），恢复前二次确认；结果 toast 带词典数。

## 3. 诊断

「复制诊断信息」写系统剪贴板：版本名 + 构建哈希、Android/设备、引擎错误、当前默认输入法、偏好组数。不做日志上传（无网络权限）。

## 4. 安装脚本

`script/install-debug.sh`：按设备 ABI 选择 split APK，`adb install -r` 后 `ime enable/set`；输出下一步提示。

## 5. 剩余（真机/主人）

- 真机矩阵冒烟（API 26/33/36、折叠/大屏）。
- release 签名 secrets（仓库 Settings → Secrets）。
- 备份→卸载→重装→恢复 真机回归。
- 崩溃/ANR 观察与性能预算复核（就绪 <2s、首键 <300ms）。

## 6. 测试

- 编译/单测/spotless 全仓闸门；设置无孤儿测试继续覆盖新动作（动作 key 以 `__action_` 前缀排除）。
- 备份/恢复：JVM 层无法跑 JNI 词典导入，逻辑以真机回归为准（tasks 显式登记）。
