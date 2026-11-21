# Proposal: desktop-parity（D3）

## Why

D1/D2 已把桌面线做成「可构建 + macOS 可安装 + Linux IBus 薄壳 + Windows 契约 + 共享 CMP 候选窗/设置窗」。D3 收尾要回答：三平台**怎么装、怎么升级、怎么排查**，以及哪些项仍需对应平台实测。

## What Changes

- **macOS**：`script/install-macos-ime.sh` 已可一键构建安装；本轮重建并重新安装最新 `.app`（含 Shift Release 语义）；补 SDK/驱动对齐保护；补「启用输入源 / 切换 / 排查」文档。
- **Linux**：`desktop/linux/install.sh` 一键装 IBus 组件；文档覆盖引擎包、环境变量、ibus-daemon 重启与已知限制。
- **Windows**：契约与待办已落 `desktop/windows/README.md`；D3 不伪造未编译代码，明确交给 Windows CI/真机批次。
- **验收手册**：新增 `doc/verification-guide.md` —— 手机/三平台逐项验收清单（用户最终验收按此执行），含已知限制与回滚方式。
- **总规划状态**：`doc/master-plan.md` 回写 M0–M4 / D1–D2 的实际交付与剩余项。

**不做**：把未在对应平台编译/运行过的壳标记为完成；Path B。

## Capabilities

### New Capabilities

- `desktop-parity`：桌面三平台的安装/升级/验收契约。

### Modified Capabilities

（无。）

## Impact

- 脚本：`macos/JRimeIME/build.sh` SDK 对齐；安装脚本更新说明。
- 文档：验收手册、状态回写。
- 风险：Windows 壳仍未编译；Linux shell 未在 Linux 会话实测；macOS 自绘候选窗需辅助功能权限（默认系统候选窗）。
