# UI 设计规范（v1，Gboard × FlorisBoard 架构，任务 1.3 重构依据）

> 方法修正（主人 2026-11-15 指示）：旧 YAML 数据降级为**行为清单**，视觉层从本规范
> 设计。参照 = FlorisBoard 源码（`/home/shijy7/Workspace/Github/florisboard`）的
> 成熟架构 + Gboard 视觉语言。

## 1. 架构（抄 FlorisBoard keyboard3）

- **数据与 chrome 分离**：字符排（3 行字母）来自数据；shift/退格/符号/空格/回车
  是**应用自有的 chrome**（代码定义、图标化），不从主题数据渲染。
  （floris：`characters/qwerty.json` 仅 26 字母，`FnKeyArrangement` 代码配置。）
- **固定行高**：`keyboardRowBaseHeight = 65dp`，键盘高度 = 行数 × 65dp，
  上下标（候选/组合条）独立高度（floris: smartbar 40dp）。
- **归一化布局**：键以 (x, y, w, h) ∈ [0,1] 定位，绝对摆放于一个 Box
  （floris `ImeKeyboardBox` 的 `Modifier.layout` 方案）；键内缩 margin 出缝。
- **按压反馈**：ripple + `PressInteraction`；repeatable 键 = 初始 timeout 后
  按 repeatDelay 连发（floris `ImeKeyButton`）。
- **不做主题系统**：floris 的 Snygg 样式表不抄，色值/尺寸全部硬编码常量。

## 2. 键面规格（Gboard 亮色）

| 元素 | 值 |
| --- | --- |
| 键盘底 | #F1F3F4（1.5 校准） |
| 字母键 | #FFFFFF，8dp 圆角，22sp 居中 |
| 功能键（shift/退格/符号/，/。） | #E8EAED，8dp |
| 回车键 | #1A73E8 胶囊，白图标 |
| 空格键 | #FFFFFF，12sp 淡字（方案名「中文」） |
| 按压态 | #DADCE0 |
| 键缝 | 水平/垂直各 ~4dp（margin 半分） |
| hint（角标） | 右上角 9sp #5F6368，来源=数据的 labelSymbol，空则 hint（主人 2026-11-15 指示：要按键上的小信息，全部显示） |

## 3. 排布（Gboard 拼音键盘结构）

```
行0  q w e r t y u i o p            ← 数据（10 键，各 1 单位满宽）
行1  a s d f g h j k l              ← 数据（9 键 = 9 单位，左右各缩 0.5 单位居中）
行2  [⇧1.5] z x c v b n m [⌫stretch→1.5]  ← chrome 几何 + 数据(7 字母)
行3  [符号15] [，·辅助15] [空格·中文40] [。15] [↵15]
```

- 几何 = FlorisBoard 单位网格（TouchModel.kt：行满 10 单位；短行
  `currentX=(1-sum)/2` 居中；shift 1.5 + 7×1 + 退格 stretch=1.5）
- 行 2/3 的每个 chrome 槽**优先取数据里的同 token 键**（保留长按/下滑行为：
  长按，→数字键盘、空格下滑→26/36 键等），数据缺失才回退纯 chrome 键；
  键面（label/字号/配色）由代码硬编码，数据只供行为
- **辅助码并入逗号**（主人 2026-11-15 指示）：逗号键 composing 槽="/"——组合中
  点逗号=辅助码分隔符，平时=逗号；独立 / 辅助键删除，腾出的宽度给其余键
- Q 行长按=数字 1-0 完整排（u i o p=7 8 9 0，原字符入口挪到上滑）
- 非 qwerty 键盘（number/editor/basic_symbol/math_symbol/t9_symbol）走
  KeyboardPanel 全数据渲染，判定=≥3 个单字母数字行且 ≥70% 键为单字符
- 图标：⇧ U+21E7 / ⌫ U+232B（阶段一用字形，后续可换矢量图标）

## 4. 度量

- 行高 65dp ×4 = 260dp 键盘带 + smartbar 44dp（在带上方，常驻不闪跳）
- smartbar：左组合串（16sp 深色）│ 竖分隔线 │ 候选横滚（18sp，高亮蓝色加粗，
  词尾 11sp 灰色辅助码注释），候选间 1dp 发丝分隔线
- 底边：键带背景画穿手势区（不透明覆盖，否则后面窗口透出），内容
  `navigationBarsPadding()+6dp` 上抬——键与系统 ime 按钮留呼吸、永不重叠
- smartbar 只在**候选非空**时渲染（单声母有真候选照常出条；组合中无候选的
  中间态由输入框 inline preedit 承担反馈，不出空白条）
- 符号/数字页复用同结构（数据行 + chrome 行）

## 5. 亮/暗

暗色版 chrome 常量表（task 1.4 换源时接 GboardColors.Dark），阶段一先亮色。

## 6. 操作栏（第六轮决策，commit a78355fa，2026-11-15）

QWERTY 行上方 44dp **常驻操作栏**（替代原 smartbar 的条件渲染）：空闲=菜单态、
打字=候选态，**band 高度恒定**（候选出现/消失不再撑高 band，带顶不移动）。

```
空闲态: [⚙设置] [，。/. ,符号] [📋剪贴] [☻/⌨emoji·返回] <spacer> [中/EN] [▼收起]
候选态: [候选词 横滚（18sp 蓝高亮+11sp 灰辅助码）] [中/EN]
```

- 菜单入口全部走现成派发链：⚙=Settings preset（KEYCODE_SETTINGS→主界面）、
  符号=toggle ascii_punct（states ，。/. ,）、📋=clipboard_window、
  ☻=liquid_keyboard_emoji（liquid 页时该槽变 ⌨=Keyboard_default 返回键，
  直至 2.1 tab 条落地）、▼=Hide preset（send BACK→requestHideSelf）
- **中英切换**双态：toggle ascii_mode + states["中","EN"]，EN 态蓝底白字
  （#1A73E8/#FFF）随 onRimeOptionUpdated 即时翻转；**候选态也保留**（打一半
  切英文是高频）；字母直通走 trime KeyMessage 物理键路径（方案无
  ascii_committer，process_key false→sendDownUpKeyEvent，原版行为）
- **字母键零标记**（主人验收：低频操作不扰乱视线）：a/z/x/c/v/b 的
  labelSymbol 编辑图标（渲染层是主标签优先级、会顶替字母）与 j/k/l/n/m 的
  ;{}'" hint 全部清除；长按/滑动槽保留为盲操作；Q 行数字、逗号 / 辅助码、
  句号 ? 角标保留
- 呼吸 6dp→12dp（键区与系统手势区两按钮的间距拉大）
- liquid 面板：LiquidWindow.currentKeys 暴露为 Compose State，Compose 网格
  镜像真实激活页；面板高度钉 4×65dp（网格内滚动），杜绝 fillMaxSize 撑爆 band
- 📋 剪贴板暂为空白面板（legacy 窗口无 Compose 渲染，2.1 排队）
