# Tasks

> 状态（2026-11-21）：D1a 完成；D1b（协议拆分/模型汇合/部署热更新）待下一批。

## 1. Gradle 模块

- [x] 1.1 新增 `:desktop:engine` JVM 模块（source=engine/java + protocol-common），`engine.jar` 可构建
- [x] 1.2 settings 接线；脚本/Swift 打包目录不变（零回归）

## 2. macOS 壳

- [x] 2.1 `flagsChanged` 转发 + Shift/CapsLock 修饰键映射（中英切换/commit_code）
- [x] 2.2 `.app` 重建（Swift 变更编入）并 `codesign OK`
- [x] 2.3 `script/install-macos-ime.sh` 安装到 `~/Library/Input Methods/`；本机已安装

## 3. 验证

- [x] 3.1 `desktop/test_server.py` → DESKTOP SERVER TEST PASS
- [x] 3.2 全仓闸门（Android 侧 test/assembleDebug/spotless）
- [ ] 3.3 真机：系统设置添加输入源、打字/候选/Shift 切中英验收（主人）

## 4. D1b（协议拆分已完成，模型汇合待办）

- [x] 4.1 协议依赖倒置（EngineDataAccess）+ `:desktop:protocol` 物理拆分（DTO 迁入协议模块）
- [ ] 4.2 桌面 DTO 与 `:core:model` 汇合（P1-1/P2-1 收口，golden 重录窗口）
      —— 结论：桌面 DTO 的构造签名由 C++ 桥 `FindClass` 绑定（`Candidates.Paged(finished,lastPageSize,reverse,pageSize,candidates)`），与 app `RimeProto` 语义不同；汇合需桥侧适配器 + golden 重录，D2 处理
- [ ] 4.3 桌面部署/配置热更新（deploy 请求或启动维护）
- [ ] 4.4 零拷贝启动/每宿主 JVM 评估
