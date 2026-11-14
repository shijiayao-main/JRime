# Tasks

> 状态（2026-11-15）：任务组 1-3 的非真机项全部完成并已提交（见 implementation-notes.md）；模拟器验收已确认部署 6/6 全绿与出字链路。剩余待验：3.3 / 4.1（方案切换出字与 lua 功能抽查）/ 4.2 / 4.3 / 4.4（稳态口径）/ 4.5 —— 真机项，待设备验收后勾验并 archive。

## 1. Submodule 恢复与基座判据确认

- [x] 1.1 恢复 librime submodule checkout 并记录版本；对照 `app/src/main/jni/librime_jni/rime_jni.cc:18-26` 的模块链接开关，确认 librime 版本满足 lua 扩展（≥1.16）且 predict/octagram 模块与 predict.db/gram 引用版本匹配。验证：submodule 目录非空，版本与能力核对结论记录归档进本 change 目录。
- [x] 1.2 恢复 submodule 后 diff 用户版 default.yaml（`/home/shijy7/Workspace/Personal/rime/default.yaml`）vs prelude 版（`app/data/rime/prelude/default.yaml`）：确认用户版引擎必需键无缺失（缺键从 prelude 回补后内置）；并从「未被 default.custom.yaml patch 覆盖且两版取值不同」的键中圈定基座判据清单（排除 page_size/switcher 热键/fold_options——被 custom patch 覆盖、无判别力）。验证：diff 结论与判据清单归档进本 change 目录，供任务组 4 验收引用。

## 2. 内置集清单生成与资产替换

- [x] 2.1 按 schema `dependencies:`/`import`/词典引用 + lua 挂载引用（`lua_processor@`/`lua_translator@`/`lua_filter@` 等 `@*name` 形式）+ lua 脚本间 require 传递依赖的完整闭包，从 `/home/shijy7/Workspace/Personal/rime` 机械推导内置集文件清单（覆盖四方案依赖闭包，含 ecdict 声明依赖、melt_eng 词典挂载、全部被挂载 lua 脚本及其 require 依赖等易漏项）。验证：生成的清单覆盖四个内置方案的全部声明依赖与 lua 引用，逐方案核对无缺件、lua 功能所需脚本全部在清单内。
- [x] 2.2 按清单替换 `app/src/main/assets/shared/`：移入内置集真实文件（根级 schema/custom、cn_dicts/、en_dicts/ 含 ecdict、反查/部件词库、lua/ 41 个 .lua 脚本全量但剔除仅被未启用脚本 revert_commit_word.lua 依赖的桌面平台二进制 lua/lib/sendKeyCode.dll 与 sendKeyCode.so、opencc/、flyhe.trime.yaml、用户版 default.yaml 基座、四方案 default.custom.yaml）；移除 luna_pinyin 族 7 个与 stroke 族 2 个符号链接、essay.txt、tongwenfeng.trime.yaml；保留 prelude 三件（key_bindings/punctuation/symbols）。验证：assets 目录内容与生成清单一一对应，`./gradlew :app:assembleDebug` 构建通过（悬空符号链接改真实文件后的构建行为以实测为准）。
- [x] 2.3 DataChecksumsPlugin 对新资产集重新生成 checksums 清单。验证：构建产物中 checksums 覆盖新清单全部文件（构建日志或产物抽查）。
- [x] 2.4 确认 `DataManager.sync()` 对校验清单之外文件（物化目录孤儿）的处理行为（design Open Question 1）；若不清理，补孤儿文件清理逻辑，覆盖升级安装场景下已释放通用方案的残留。验证：以代码审查/单测确认清理行为，或真机升级安装后物化目录与方案列表无通用方案残留。

## 3. 两源三源与方案 UI 落地

- [x] 3.1 `ensureDefaultCustomConfig`（`core/data/UserDataManager.kt:135-140`）写入条件收紧为「user 与 shared 均缺失才写」，兜底 patch 内容（`:43-48`）从 luna_pinyin 双方案改为 flypy 四方案。验证：三分支（user 有/仅 shared 有/双缺失）行为有单测或代码审查证据覆盖，`./gradlew :app:testDebugUnitTest` 全绿（现有 26 个 kotest 单测不劣化）。
- [x] 3.2 升级场景处理：检测 user 目录旧代码生成版 default.custom.yaml 的特征并处理——自动清理或升级明确告知（手段按 design Open Question 2 裁量），不静默遮蔽预置补丁。验证：模拟旧版升级场景（user 目录放置旧代码生成版），升级后预置四方案补丁生效。
- [ ] 3.3 验证方案列表现有「可用 → 已启用」机制（`SchemaListFragment.kt:26-33` 的添加/停用/排序）与资产替换协同：四方案默认启用、可停用与排序，可添加列表无通用方案残留，非预置方案经 SAF 同步部署后可经现有添加入口启用（现有添加入口即自定义方案入口，不新开发功能）；如验证中发现与通用方案相关的残留文案或路径，再按实际代码清理。验证：真机逐项操作通过（四方案启用管理 + 自定义方案添加启用）。

## 4. 真机集成验收与基线归档

- [ ] 4.1 全新安装冒烟：首启部署完成；四方案逐一切换出字；默认键盘主题为 flyhe；lua 脚本功能抽查（calculator 等）生效；easy_en 反查释义抽查（输入英文单词出中文释义，验证 ecdict→reverse_lookup_filter→lua 注释链路）。验证：逐项记录进验收记录。
- [ ] 4.2 两源三源回归：首启后 shared 版 default.yaml 基座生效（按任务 1.2 圈定的判据清单逐键抽查 patch 未覆盖键）、预置 default.custom.yaml 四方案生效、user 目录无代码生成版；SAF 同步启用后 user 版 default.custom.yaml 优先。验证：判据清单逐键核对通过并记录。
- [ ] 4.3 gram/predict.db 缺件行为验证：真机确认主方案缺 gram/缺 predict.db 时部署完成、主方案可出字、对应增强功能禁用且不阻塞（不符预期则按 design 决策 2 回退处理）。验证：缺件行为记录归档；可选抽查 predict.db 经用户目录导入后自动启用。
- [ ] 4.4 双口径基线实测并归档，基线面 = 路线图附录表全部「期 1 待实测」行（`doc/refactor/roadmap.md:344-351`）：首启口径（安装后首次启动的 deploy Start→Success logcat 时间戳 + adb 手工计时冷启动到首字）与稳态口径（部署完成后二次冷启动）分别实测；主题列表加载文件解析次数以日志/profiler 证据记录现状值；APK 体积以构建产物实测；另记录构建/CI 时长与 release 产物体积（ecdict 降级判据的数据来源，见 design 决策 1 备选）。验证：附录表期 1 基线列 6 项全部填写完整且口径标注齐全，构建/体积实测记录随验收记录归档。
- [ ] 4.5 升级安装场景验证：从旧版本（预置 luna_pinyin 与旧代码生成版）升级——孤儿资产不残留（任务 2.4）、存量代码生成版被处理（任务 3.2）、基座与四方案补丁生效。验证：升级场景逐项通过并记录。

## Workflow follow-up

- 期 1 验收（任务组 1-4 全部勾验）通过后 archive 本 change（`openspec-archive-change` 流程），随后按路线图启动期 2 `startup-fastpath` 的 change 提案。
- 任务 4.4 的基线数字是期 2 承诺（首启部署耗时较期 1 基线下降 ≥50%）的对照前提，archive 前必须完成归档。
