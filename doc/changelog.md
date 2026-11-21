# 交付记录（2026-11-21 汇总）

> 按批次记录「做了什么、怎么验证、留了什么」。逐轮 git log 是最终事实源；本文件用于快速交接与验收对照。

## 手机（M0–M4）

| 批次 | 内容 | 验证 |
|---|---|---|
| M0 | 总规划、文档权威链、基线 | 文档评审 |
| M1 | 共享 CMP-clean 表面（design tokens/layout/interaction/surface/session 五层）、`ImeSurfaceHost` 平台缝、候选分页/长按菜单/键面韵母提示 | 模拟器截图 + 单测（SurfaceGuard 等） |
| M2 | 简繁/全半/标点/表情/联想等 10 条输入规则、外观（深浅/强调色）、Compose 设置中心六组+搜索+无孤儿断言、死设置清理、候选单页数量/字号/候选注解开关 | 模拟器 + 设置无孤儿测试 |
| M3 | 常用语面板（读写 Rime `custom_phrase.txt`）；用户词典浏览/编辑（调频/删除/加词，负 commits 语义，编辑后重启引擎）；整机备份/恢复；诊断复制 | 单测 + 模拟器（设置组与词典页截图） |
| M4 | 启动失败降级（异常兜底 + 重试对话框）、一键安装脚本、发布物料清单（签名 secrets/真机矩阵/7 天观察待主人） | 全仓闸门 + 脚本实跑 |

关键修复：
- 设置中心系统组漏加「导出/恢复备份、复制诊断」动作行（此前 UI 不可见）→ 已补 + 回归断言。
- 设置页大范围整数滑块崩溃（NegativeArraySizeException）→ 改文本编辑 + 滑块步数上限 + 回归测试。
- 用户词典编辑不重启引擎则不生效 → 编辑后自动重启。

## 桌面（D1–D3）

| 批次 | 内容 | 验证 |
|---|---|---|
| D1 | `:desktop:engine`/`:desktop:protocol` Gradle 化；`EngineDataAccess` 依赖倒置；macOS Shift 切中英（press/release 语义）、一键安装脚本 | `desktop/test_server.py` PASS、golden 12/12 |
| D2 | 五个共享模块 KMP 化（android+jvm desktop）；`:desktop:app`（引擎+行协议+CMP 候选窗一体，`rect` 定位/候选点击 `event:update`）；`:desktop:settings`（共享 SettingsScreen + properties 存储，候选窗读同一配置）；Linux IBus 薄壳（协议客户端 6 单测）；Windows TSF 契约 | CMP 窗口 spike（焦点不抢占）、`desktop:app` ready/key 冒烟、设置页渲染、Linux 单测 |
| D3 | 候选窗空闲隐藏 + 设置热重载；SDK/链接器固定；macOS `.app` 重建安装；验收手册 | 冒烟输出 hidden/shown/reloaded；codesign OK |

平台结论：
- macOS 公开 IMK API 不提供插入点坐标，自绘候选窗需辅助功能权限 → 默认用系统 `IMKCandidates`；CMP 候选窗用于 Linux X11/Windows。
- Linux IBus 壳需 Linux 会话实测；Windows TSF 需 Windows 工具链/CI 批次。

## 复现命令

```sh
# 全仓闸门
./gradlew test :core:model:jvmTest :core:engine:jvmTest :ui:ime:desktopTest \
  :app:assembleDebug :desktop:app:compileKotlin :desktop:settings:compileKotlin spotlessCheck

# 手机
./script/install-debug.sh
# macOS
./script/install-macos-ime.sh
# 桌面引擎协议
python3 desktop/test_server.py
# Linux 协议客户端
python3 -m unittest discover -s desktop/linux/tests -p 'test_*.py'
```
