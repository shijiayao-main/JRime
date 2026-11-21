# Implementation Notes — branding-attribution

## 2026-11-21

- Const 删除 QQ/Telegram 常量；AboutFragment 重写：项目简介 + 「版本与构建」+ 「开源与致谢」
  （开源说明/源代码/许可证/隐私），不再展示上游社区入口。
- 字体：strings 三语删除 qq_group_* 与 telegram；新增 about_intro_*、licenses_*、about_version/open_section、
  ime_subtype_label；slogan 改为 JRime 自己的 tagline；`method.xml` 与 `xml-v30/method.xml` 子类型标签改用
  `ime_subtype_label`。
- LicenseFragment：顶部简介 + 「上游项目」致谢（Rime 官网）+ 原生组件分类 + 应用依赖分类；
  条目仍点击查看许可证（AboutLibraries 数据源不变）。
- 新增 `script/gen-third-party-notices.py`，从 AboutLibraries JSON + `app/licenses/libraries` 生成
  `THIRD_PARTY_NOTICES.md`（8 native + 156 libraries）。
- 文档：`doc/style-guide.md`（命名/文案/视觉/默认规则/开源口径）与 `doc/schema-rules-notes.md`
  （默认方案规则盘点与下一批建议）；README.md / README_sc.md / README_tc.md 重写。
- 模拟器验证（emulator-5554）：关于页显示简介/版本构建/开源与致谢，无 QQ 群；开源说明页显示
  GPL 简介、上游项目、Rime 原生组件（boost/OpenCC/fmt/spdlog/leveldb…）与应用库条目。
- 未做：schema 默认值调整（ascii_punct/候选单页/charset）——需要预编译与旧安装部署/回归，列入下一规则批次。
