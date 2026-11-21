# 交付记录（2026-11-21 汇总）

> 按批次记录「做了什么、怎么验证、留了什么」。逐轮 git log 是最终事实源；本文件用于快速交接与验收对照。

## 手机（M0–M4）

| 批次 | 内容 | 验证 |
|---|---|---|
| M0 | 总规划、文档权威链、基线 | 文档评审 |
| M1 | 共享 CMP-clean 表面（design tokens/layout/interaction/surface/session 五层）、`ImeSurfaceHost` 平台缝、候选分页/长按菜单/键面韵母提示 | 模拟器截图 + 单测（SurfaceGuard 等） |
| M2 | 简繁/全半/标点/表情/联想等 10 条输入规则、外观（深浅/强调色）、Compose 设置中心六组+搜索+无孤儿断言、死设置清理、候选单页数量/字号/候选注解开关 | 模拟器 + 设置无孤儿测试 |
| M3 | 常用语面板（读写 Rime `custom_phrase.txt`）；用户词典浏览/编辑（调频/删除/加词，负 commits 语义，编辑后重启引擎）；中英切换方式（Shift/按钮/两者）；整机备份/恢复；诊断复制 | 单测 + 模拟器（设置组与词典页截图） |
| M4 | 启动失败降级（异常兜底 + 重试对话框）、一键安装脚本、发布物料清单（签名 secrets/真机矩阵/7 天观察待主人） | 全仓闸门 + 脚本实跑 |

关键修复：
- 设置中心系统组漏加「导出/恢复备份、复制诊断」动作行（此前 UI 不可见）→ 已补 + 回归断言。
- 设置页大范围整数滑块崩溃（NegativeArraySizeException）→ 改文本编辑 + 滑块步数上限 + 回归测试。
- 用户词典编辑不重启引擎则不生效 → 编辑后自动重启。
- 候选字号/键面提示/候选注解开关未注入键盘（写死 18sp）→ 已从偏好注入。
- 两套深浅模式（应用 ui_mode 与键盘 palette_mode）合并为一套：外观里的「深浅模式」同时驱动应用主题与键盘，旧 `ui_mode` 移除。
- 设置中心选择/文本弹窗改值后行标签不刷新（LazyColumn 跳过重组）→ 用 revision key 强制刷新。

## 桌面（D1–D3）

| 批次 | 内容 | 验证 |
|---|---|---|
| D1 | `:desktop:engine`/`:desktop:protocol` Gradle 化；`EngineDataAccess` 依赖倒置；macOS Shift 切中英（press/release 语义）、一键安装脚本 | `desktop/test_server.py` PASS、golden 12/12 |
| D2 | 五个共享模块 KMP 化（android+jvm desktop）；`:desktop:app`（引擎+行协议+CMP 候选窗一体，`rect` 定位/候选点击 `event:update`）；`:desktop:settings`（共享 SettingsScreen + properties 存储，候选窗读同一配置）；Linux IBus 薄壳（协议客户端 6 单测）；Windows TSF 契约 | CMP 窗口 spike（焦点不抢占）、`desktop:app` ready/key 冒烟、设置页渲染、Linux 单测 |
| D3 | 候选窗空闲隐藏 + 设置热重载；SDK/链接器固定；macOS `.app` 重建安装；验收手册 | 冒烟输出 hidden/shown/reloaded；codesign OK |

平台结论：
- macOS 公开 IMK API 不提供插入点坐标，自绘候选窗需辅助功能权限 → 默认用系统 `IMKCandidates`；CMP 候选窗用于 Linux X11/Windows。
- Linux IBus 壳需 Linux 会话实测；Windows TSF 需 Windows 工具链/CI 批次。

## 基线与证据

- `doc/perf-baseline.md`：debug 77MB / release 57MB（每 ABI，未签名）；`am start -W` 冷启动 1232ms，
  librime 加载 +0.90s，首帧 +1.23s（模拟器 debug）。
- `doc/screenshots/README.md`：键盘/面板/设置/词典截图索引，验收对照用。

## 长按输入与语音（2026-11-21）

- 功能行长按补全：空格→语音输入（可开关，切系统语音 IME，零麦克风权限）、回车→换行、逗号→顿号；
  字母/数字行既有长按保持不变。可行性调研见 `doc/voice-input-notes.md`。
- 预设/绑定单测覆盖；真机验收需安装语音输入法。

## 设置分类展示（2026-11-21）

- 设置页从「一条超长列表」改为两层：分类首页（6 张卡片 + 说明 + 项数）→ 分类详情（返回/项数/局部搜索），
  首页搜索仍跨分类；Android 与桌面设置窗口共用同一 Compose 屏幕。模拟器截图见
  `openspec/changes/settings-categories/screenshots/`。

## 默认规则调优（2026-11-21）

- 默认标点改中文（`ascii_punct reset: 0`）、默认字集改通用（`charset`/`extended_charset reset: 0`）、
  flypy 候选单页统一为 7；shared 源与预编译 build 同步，资产 release 链保证旧安装升级生效。
- 清数据首装实测：使用英文标点=关、增广字集=关、表情候选=开、联想=关（截图见
  `openspec/changes/rule-defaults/screenshots/`）。详见 `doc/schema-rules-notes.md`。

## 模型化扩展（P1，2026-11-21）

- 新增 typed `ImeAction` 与 `ActionResolver`：随包布局全部槽位类型化，YAML 时代文本宏改为显式
  `TextEdit` 序列，Unsupported 为空（测试钉死）。
- 新增 `JrimeExtension`/`ExtensionRegistry`：扩展按 Kotlin model 注册（actions/keyboards/panels/
  inputRules），冲突显式报错。
- 架构与 P1–P5 路线见 `doc/model-first.md`；本批只加模型与测试，运行时 UI 暂不变。

## UI 图标系统（2026-11-21）

- 语义图标 `ImeIcon`（26 个 key，方案中立，outlined 风格）落地到顶栏工具、面板返回/收起、候选翻页与
  设置分类卡；emoji/文字混搭的旧图标（⚙/📋/☻/语）退场。
- 设计系统文档 `doc/ui-design-system.md`：品牌命名口径（飞鹤快拼=默认方案数据，AI 优化/改名不牵动
  图标与品牌）、token、图标表与组件规格。
- 测试：`ImeIconSetTest`（唯一/方案中立）、分类图标断言、SurfaceGuard；模拟器截图见
  `openspec/changes/ui-icon-system/screenshots/`。

## 模型化扩展 P2：Compose 执行器 typed 化（2026-11-21）

- 新增 `RimeActionDispatcher`：Compose 表面的按键/命令直接执行 typed `ImeAction`（键处理、上屏、
  双击空格、选项切换、文本编辑、面板/键盘切换、语音、维护命令），删除字符串命令解释器在 Compose 路径上的使用。
- `ImeCommandExecutor` 用 `ActionResolver` 解析槽位；`ClipboardChip` 改为直接提交文本；面板打开显式化。
- 模拟器验证：字母组合/候选上屏/双空格句号/剪贴板面板/长按空格切语音输入（截图见
  `openspec/changes/model-first-action-model/screenshots/`）。

## 功能精简 P4（第一批，2026-11-21）

- 移除自定义按键音效（YAML 音效包）特性：应用层最后一个 YAML 解析器删除；仅保留系统按键音/音量/振动；
  设置中心不再有音效选择入口。
- 候选长按菜单加入语义图标（置顶/删词/复制/查编码）。
- 变更记录：`openspec/changes/feature-simplify/`；下一步 P3 面板去 BoardWindow 化后再清主题/死偏好。

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
