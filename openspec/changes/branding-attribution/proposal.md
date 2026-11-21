# Proposal: branding-attribution

## Why

用户反馈：整体展示去同文/Trime 化；JRime 是主体，飞鹤快拼只是默认方案；开源内容要有完善说明页
（用了哪些库/许可证/上游致谢）。

## What Changes

- **去同文/Trime 化（展示面）**：删除应用内 Trime 社区 QQ/Telegram 入口与常量；About 页重写为
  「JRime 简介 + 版本/构建 + 开源与致谢」；IME 子类型标签从 slogan 改为「JRime 输入法」；
  品牌文案改为 JRime 自己的 slogan；README 三语重写为 JRime 项目说明（含功能/构建/验收/致谢）。
- **风格定调**：新增 `doc/style-guide.md`（命名、文案语气、视觉、默认规则、开源口径）。
- **开源说明页**：License 页新增简介与「上游项目」致谢，并按「Rime 引擎与原生组件 / 应用与工具库」
  分组（AboutLibraries 数据源既有）；新增仓库级 `THIRD_PARTY_NOTICES.md` + 生成脚本
  `script/gen-third-party-notices.py`。
- **规则审查**：新增 `doc/schema-rules-notes.md` 盘点默认方案开关，指出标点默认、候选单页数量、
  charset 默认等不一致并列为下一规则批次（本轮不动预编译 schema，避免部署链风险）。
- About/License 在模拟器验证：简介、版本/构建、开源与致谢、上游项目、Rime 原生组件与库分组均正常。

## Impact

- 展示文案与 About/License 页面；Const 清理；IME subtype label。
- README ×3、THIRD_PARTY_NOTICES、style-guide、schema-rules-notes。
- 不改引擎/协议/数据；不动 schema 预编译产物。

## 验证

- `:app:compileDebugKotlin`、app 单测、KMP 测试、spotless 全绿。
- 模拟器：关于页（无 QQ 群）、开源说明页（简介 + 上游项目 + 原生组件 + 依赖分组）。
