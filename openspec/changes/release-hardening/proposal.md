# Proposal: release-hardening

## Why

M1–M3 把手机端的输入体验做成了日常可用形态，但「能长期稳定用」还差三件事：

1. **启动失败会静默/崩溃**：`nativeStartup` 里 `startRime()` 没有异常兜底，失败会让 dispatcher 协程崩掉、状态停在 STARTING，`runOnReady` 永不返回（设置页/主界面的就绪等待悬挂），用户看到的是「键盘没反应」。
2. **数据没有整机备份**：只有逐词典的导出/导入与用户词典快照，换机/重装前无法一次备份 用户词典 + 常用语 + 自定义 patch。
3. **安装/反馈成本高**：没有一键安装脚本；出问题时没有可一键复制的诊断信息（版本/设备/引擎错误/当前输入法）。

## What Changes

- **启动失败降级**：`nativeStartup` 捕获异常 → 记录 `startupError` → 生命周期安全回退到 STOPPED（可重试）；主界面在等待就绪超时后弹「引擎启动失败」对话框，可一键重试；不再悬挂 `runOnReady` 等待。
- **整机备份/恢复**：设置中心「系统与数据」新增导出/恢复动作。备份为可检查的 zip：`custom_phrase.txt` + `default.custom.yaml` + 每个用户词典的文本导出（`userdict/<name>.txt`）；恢复走引擎自身的 import 通道并重启会话。
- **诊断信息**：新增「复制诊断信息」动作（版本/构建哈希/Android/机型/引擎错误/当前输入法）。
- **一键安装**：`script/install-debug.sh`（构建 → adb 安装 → 启用并选中 IME）。
- **发布物料**：M4 剩余项在 tasks 中显式登记（真机矩阵、release 签名 secrets、备份恢复真机验证）。

**不做**：签名密钥配置（仅主人可做）；Play 商店发布；iOS。

## Capabilities

### New Capabilities

- `release-hardening`：启动失败可恢复、用户数据可整机备份恢复、诊断可复制、安装可一键。

### Modified Capabilities

（无。）

## Impact

- 引擎门面：`Rime`/`RimeLifecycle`/`RimeDaemon` 的失败路径；无 JNI/协议改动。
- app：`UserDataBackup`、设置中心动作、主界面失败对话框、安装脚本、文案。
- 风险：恢复是「合并」语义（import 通道），不删除本地已有词条；真机需验证重启后词典生效。
