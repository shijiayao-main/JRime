# Tasks

## 1. Vendor 化

- [x] 1.1 librime 及插件/依赖源码入库：librime（1.17.0）、librime-lua、librime-lua-deps、librime-octagram、librime-predict、snappy 及 librime 嵌套依赖从 submodule 指针改为源码快照提交；prelude（key_bindings/punctuation/symbols 编译输入）同样入库；删除 luna-pinyin/stroke/essay 指针与 `.gitmodules` 全部条目。验证：`git submodule status` 为空；`:app:assembleDebug` native 全量构建绿。
- [x] 1.2 构建去网络化核查：vendored 树的设备构建不发生网络拉取（FetchContent 清点并入库或冻结）。验证：断网状态全量构建绿（或 FetchContent 缓存清单归档）。

## 2. Spike：主机预编译 + 平台无关验证（先行，任务 3 依赖其结论）

- [x] 2.1 用 vendored librime 树构建宿主机部署工具（x86_64），对当前内置集跑完整部署，产出 `build/`（3 方案 × table/prism/reverse.bin + 展开后的 schema.yaml/default.yaml）。验证：产物清单与设备端部署产物逐一对齐（文件名/数量/大小量级）归档进 change 目录。
- [x] 2.2 平台无关验证：产物放入 `assets/shared/build/` → 模拟器全新安装 → **维护检查跳过编译、引擎直接 ready**（logcat 无 dict compiler 跑动、`tasks ran` 后即 Success）→ 出字/简拼/部件反查正常。验证：logcat 证据归档；arm64 真机复验列入验收前必做项。
- [x] 2.3 解决 Open Question 1（prebuilt 下 default.custom.yaml 的 patch 语义）：预置 custom 存在时 schema_list/热键是否生效，按观测结果决定 custom 链归宿主工具或保留运行时；结论与证据归档 design.md。

## 3. 构建集成与瘦身

- [x] 3.1 `precompileRimeData` 构建期任务（convention plugin）：编译输入源迁至仓库内固定目录（落地为 `app/data/rime/dicts/`，跟随既有 prelude 布局）、主机工具产物进 staging 预置位 `assets/rime/build/`（spike 修正：原文的 `assets/shared/build/` prebuilt 协议位不安装 table/reverse.bin，依据见 design.md「Spike record」）、任务在 `generateDataChecksums` 前执行、工具构建缓存化。YAML 词典源退出 APK 分发（`*.schema.yaml` 系运行时维护期重展开所需，保留随 APK；仅 `build/` 产物 + 运行时必需的用户侧文件随 APK 走）。验证：clean 构建一次产出完整资产；APK 体积实测记录（62.58→48.47 MB，净减 14.1 MB）。
- [x] 3.2 主界面 `deploy()` 菜单保留为修复路径（预置产物缺失/损坏时全量重编恢复）；启动链不再主动触发部署。验证：删除设备 `build/` 目录后 deploy 菜单可恢复出字（修复路径有效）。

## 4. 主题面裁剪（修订 5）

- [x] 4.1 引用闭包机械推导（方法同 built-in-manifest）：flyhe.trime.yaml 的 23 键盘 → default 及引用闭包、18 调色板 → default + dark_scheme 链、215 preset_keys → 闭包并集、15 旧 style 键清单。保留清单归档待主人过目。验证：清单 + 预计体积归档。（已归档 `theme-trim-list.md`：键盘 23→7、调色板 18→2、preset_keys 215→71、style 键 15 个裁剪单；4.2 待主人过目后开启。）
- [ ] 4.2 按清单应用裁剪并适配 golden 测试（数量断言第三次更新）。验证：`:app:testDebugUnitTest` + spotless + assembleDebug 全绿。

## 5. 验收

- [ ] 5.1 双口径实测归档：首启（安装到 ready，目标秒级，对照 105s 基线）与稳态（二次冷启动）；APK 体积；模拟器 + **arm64 真机**各一轮（真机为 archive 前置）。验证：数字写入 roadmap 附录表期 2 列。
- [ ] 5.2 回归：出字/简拼/单字表情上屏/部件反查/英文内嵌（模拟器全项 + 真机抽查）。验证：逐项记录。
