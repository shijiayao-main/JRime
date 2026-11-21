# 默认方案规则审查（飞鹤快拼 / flypy_xhfast）

> 2026-11-21。用户要求「飞鹤快拼只是移植前的拼音方案，具体规则看看优化下」。本文盘点随包的
> 方案开关与默认值，给出结论与后续动作。**默认方案不等于产品身份**，见 `doc/style-guide.md`。

## 1. 当前开关与默认值（`flypy_xhfast.schema.yaml` + `.custom.yaml`）

| 开关 | 状态 | 默认 | 评价 |
|---|---|---|---|
| `ascii_mode` | 中 / En | 中 | 正确 |
| `ascii_punct` | 。/ . | **中文标点（reset: 0，2026-11-21 已改）** | ✅ 符合中文输入习惯；旧英文标点用户可在输入规则切换 |
| `full_shape` | 半 / 全 | 半 | 正确 |
| 字型（zh_simp/zh_trad/zh_hk/zh_tw） | — | 简体（App 启动时显式设 zh_simp） | 正确 |
| `emoji` | 🈚️ / 😄 | 开 | 符合主流 |
| `prediction` | 休想 / 联想 | 关 | 省资源，合理；可在设置开 |
| `char_mode` | 词组优先 / 单字优先 | 词组优先 | 合理 |
| `charset` | 通用 / 增广 | **通用（reset: 0，2026-11-21 已改）** | ✅ 候选更短更聚焦；需要生僻字时在设置开「增广字集」 |
| `charset_hint` | 码区隐藏 / 显示 | 隐藏 | 合理 |
| `preedit_format` | 双拼 / 全拼显示 | 双拼 | 合理 |
| 注解类型（mask/tone/comment_off） | 辅码/声调/空白 | **声调** | 合理（可切辅码） |
| 候选单页 `menu/page_size` | flypy 补丁 7（2026-11-21 已统一）/ 全局 7 / App 默认 7；flyhe 保留 5 | 7 | ✅ flypy 引擎与界面一致；flyhe36 方案保留自身 5 |
| 候选键 `alternative_select_labels` | 1–9,0 | 数字行 | 与键盘候选条一致 |

## 2. 长按/手势规则（App 层，已定）

- 字母行：长按数字（Q 行）/大写（A、Z 行）/符号；滑动标点/光标/音节。
- 功能行新增：空格长按→语音输入法、回车长按→换行、逗号长按→顿号；句号长按→问号（既有）。
- 面板/菜单位于顶栏，不再占用键面手势（保持 2026-11-19 的「键面只做输入」决定）。

## 3. 落地结果（2026-11-21，rule-defaults）

- ✅ 标点默认中文（`ascii_punct reset: 0`，shared + build）。
- ✅ 字符集默认通用（flypy `charset` / flyhe `extended_charset` reset 0）。
- ✅ flypy 候选单页统一 7（custom + build）；flyhe 保留自身 5。
- 验证：清数据首装后输入规则 UI 实测「使用英文标点关、增广字集关、表情候选开、联想关」；
  资产 release 链每次引擎启动做 checksum diff，旧安装升级后同样生效（用户自建 custom 优先）。
- 其余开关保持现状（emoji 开、注解声调、联想关），后续按使用反馈再调。
