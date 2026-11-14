# default.yaml 基座结论（任务 1.2 产出）

> 对比：用户版 default.yaml（`/home/shijy7/Workspace/Personal/rime/default.yaml`，12699B）
> vs prelude 版（`app/data/rime/prelude/default.yaml`，submodule 3803f09）。
> 基座决策（tasks 2.2 / design）：**用户版作基座**。

## 引擎必需键核对（结论：用户版为超集，无缺键，无需回补）

- prelude 顶层键 8 个：config_version、schema_list、switcher、menu、punctuator、key_binder、recognizer、ascii_composer
- 用户版顶层键 11 个：以上 8 个 + navigator、selector、editor（flypy 定制键位绑定）
- prelude 的全部顶层键在用户版中均有对应定义 → 用户版可直接作基座

## 两版取值差异总览

- 两版均有但取值不同：8 键
- 仅用户版有：85 键
- 仅 prelude 版有：5 键：['ascii_composer/switch_key/Eisu_toggle', 'key_binder/bindings/__patch', 'punctuator/full_shape/__include', 'punctuator/half_shape/__include', 'recognizer/patterns/uppercase']

## 基座判据清单（87 键）

> 判据 = 两版取值不同 **且** 未被 default.custom.yaml patch 覆盖（含父路径）的键。
> 真机验收（tasks 4.2）按此清单逐键抽查 shared 基座生效；被 patch 覆盖的键（page_size/switcher 热键/fold_options 等）
> 无判别力（独立终审结论），已排除。

- `ascii_composer/switch_key/Caps_Lock`：prelude = `clear` → 用户版 = `commit_code`
- `ascii_composer/switch_key/Shift_L`：prelude = `inline_ascii` → 用户版 = `commit_code`
- `ascii_composer/switch_key/Shift_R`：prelude = `commit_text` → 用户版 = `noop`
- `editor/bindings/BackSpace`：prelude = `（prelude 无此键）` → 用户版 = `revert`
- `editor/bindings/Control+BackSpace`：prelude = `（prelude 无此键）` → 用户版 = `back_syllable`
- `editor/bindings/Control+Shift+Return`：prelude = `（prelude 无此键）` → 用户版 = `commit_comment`
- `editor/bindings/Delete`：prelude = `（prelude 无此键）` → 用户版 = `delete`
- `editor/bindings/Escape`：prelude = `（prelude 无此键）` → 用户版 = `cancel`
- `editor/bindings/Return`：prelude = `（prelude 无此键）` → 用户版 = `commit_raw_input`
- `editor/bindings/Shift+Delete`：prelude = `（prelude 无此键）` → 用户版 = `delete_candidate`
- `editor/bindings/Shift+Return`：prelude = `（prelude 无此键）` → 用户版 = `commit_script_text`
- `editor/bindings/space`：prelude = `（prelude 无此键）` → 用户版 = `confirm`
- `key_binder/bindings`：prelude = `（prelude 无此键）` → 用户版 = `[{'accept': 'KP_0', 'send': 0, 'when': 'composing'}, {'accept': 'KP_1', 'send': 1, 'when': 'composing'}, {'accept': 'KP_2', 'send': 2, 'when': 'composing'}, {'accept': 'KP_3', 'send': 3, 'when': 'composing'}, {'accept': 'KP_4', 'send': 4, 'when': 'composing'}, {'accept': 'KP_5', 'send': 5, 'when': 'composing'}, {'accept': 'KP_6', 'send': 6, 'when': 'composing'}, {'accept': 'KP_7', 'send': 7, 'when': 'composing'}, {'accept': 'KP_8', 'send': 8, 'when': 'composing'}, {'accept': 'KP_9', 'send': 9, 'when': 'composing'}, {'accept': 'KP_Add', 'send': 'plus', 'when': 'composing'}, {'accept': 'KP_Divide', 'send': 'slash', 'when': 'composing'}, {'accept': 'KP_Subtract', 'send': 'minus', 'when': 'composing'}, {'accept': 'KP_Decimal', 'send': 'period', 'when': 'composing'}, {'accept': 'KP_Multiply', 'send': 'asterisk', 'when': 'composing'}]`
- `navigator/bindings/Left`：prelude = `（prelude 无此键）` → 用户版 = `left_by_char_no_loop`
- `navigator/bindings/Right`：prelude = `（prelude 无此键）` → 用户版 = `right_by_char_no_loop`
- `navigator/bindings/Shift+Left`：prelude = `（prelude 无此键）` → 用户版 = `left_by_syllable`
- `navigator/bindings/Shift+Right`：prelude = `（prelude 无此键）` → 用户版 = `right_by_syllable`
- `punctuator/full_shape/ /commit`：prelude = `（prelude 无此键）` → 用户版 = `　`
- `punctuator/full_shape/!/commit`：prelude = `（prelude 无此键）` → 用户版 = `！`
- `punctuator/full_shape/"/pair`：prelude = `（prelude 无此键）` → 用户版 = `['“', '”']`
- `punctuator/full_shape/#`：prelude = `（prelude 无此键）` → 用户版 = `['＃', '⌘']`
- `punctuator/full_shape/$`：prelude = `（prelude 无此键）` → 用户版 = `['￥', '$', '€', '£', '¥', '¢', '¤']`
- `punctuator/full_shape/%`：prelude = `（prelude 无此键）` → 用户版 = `['％', '°', '℃']`
- `punctuator/full_shape/&`：prelude = `（prelude 无此键）` → 用户版 = `＆`
- `punctuator/full_shape/'/pair`：prelude = `（prelude 无此键）` → 用户版 = `['‘', '’']`
- `punctuator/full_shape/(`：prelude = `（prelude 无此键）` → 用户版 = `（`
- `punctuator/full_shape/)`：prelude = `（prelude 无此键）` → 用户版 = `）`
- `punctuator/full_shape/*`：prelude = `（prelude 无此键）` → 用户版 = `['＊', '·', '・', '×', '※', '❂']`
- `punctuator/full_shape/+`：prelude = `（prelude 无此键）` → 用户版 = `＋`
- `punctuator/full_shape/,/commit`：prelude = `（prelude 无此键）` → 用户版 = `，`
- `punctuator/full_shape/-`：prelude = `（prelude 无此键）` → 用户版 = `－`
- `punctuator/full_shape/./commit`：prelude = `（prelude 无此键）` → 用户版 = `。`
- `punctuator/full_shape//`：prelude = `（prelude 无此键）` → 用户版 = `['、', '､', '/', '／', '÷']`
- `punctuator/full_shape/:/commit`：prelude = `（prelude 无此键）` → 用户版 = `：`
- `punctuator/full_shape/;/commit`：prelude = `（prelude 无此键）` → 用户版 = `；`
- `punctuator/full_shape/<`：prelude = `（prelude 无此键）` → 用户版 = `['《', '〈', '«', '‹']`
- `punctuator/full_shape/=`：prelude = `（prelude 无此键）` → 用户版 = `＝`
- `punctuator/full_shape/>`：prelude = `（prelude 无此键）` → 用户版 = `['》', '〉', '»', '›']`
- `punctuator/full_shape/?/commit`：prelude = `（prelude 无此键）` → 用户版 = `？`
- `punctuator/full_shape/@`：prelude = `（prelude 无此键）` → 用户版 = `['＠', '☯']`
- `punctuator/full_shape/[`：prelude = `（prelude 无此键）` → 用户版 = `['「', '【', '〔', '［']`
- `punctuator/full_shape/\`：prelude = `（prelude 无此键）` → 用户版 = `['、', '＼']`
- `punctuator/full_shape/]`：prelude = `（prelude 无此键）` → 用户版 = `['」', '】', '〕', '］']`
- `punctuator/full_shape/^/commit`：prelude = `（prelude 无此键）` → 用户版 = `……`
- `punctuator/full_shape/_`：prelude = `（prelude 无此键）` → 用户版 = `——`
- `punctuator/full_shape/``：prelude = `（prelude 无此键）` → 用户版 = `｀`
- `punctuator/full_shape/{`：prelude = `（prelude 无此键）` → 用户版 = `['『', '〖', '｛']`
- `punctuator/full_shape/|`：prelude = `（prelude 无此键）` → 用户版 = `['·', '｜', '§', '¦']`
- `punctuator/full_shape/}`：prelude = `（prelude 无此键）` → 用户版 = `['』', '〗', '｝']`
- `punctuator/full_shape/~`：prelude = `（prelude 无此键）` → 用户版 = `～`
- `punctuator/half_shape/!`：prelude = `（prelude 无此键）` → 用户版 = `！`
- `punctuator/half_shape/"/pair`：prelude = `（prelude 无此键）` → 用户版 = `['“', '”']`
- `punctuator/half_shape/#`：prelude = `（prelude 无此键）` → 用户版 = `#`
- `punctuator/half_shape/$`：prelude = `（prelude 无此键）` → 用户版 = `¥`
- `punctuator/half_shape/%`：prelude = `（prelude 无此键）` → 用户版 = `%`
- `punctuator/half_shape/&`：prelude = `（prelude 无此键）` → 用户版 = `&`
- `punctuator/half_shape/'/pair`：prelude = `（prelude 无此键）` → 用户版 = `['‘', '’']`
- `punctuator/half_shape/(`：prelude = `（prelude 无此键）` → 用户版 = `（`
- `punctuator/half_shape/)`：prelude = `（prelude 无此键）` → 用户版 = `）`
- `punctuator/half_shape/*`：prelude = `（prelude 无此键）` → 用户版 = `*`
- `punctuator/half_shape/+`：prelude = `（prelude 无此键）` → 用户版 = `+`
- `punctuator/half_shape/,`：prelude = `（prelude 无此键）` → 用户版 = `，`
- `punctuator/half_shape/-`：prelude = `（prelude 无此键）` → 用户版 = `-`
- `punctuator/half_shape/.`：prelude = `（prelude 无此键）` → 用户版 = `。`
- `punctuator/half_shape//`：prelude = `（prelude 无此键）` → 用户版 = `['／', '÷']`
- `punctuator/half_shape/:`：prelude = `（prelude 无此键）` → 用户版 = `:`
- `punctuator/half_shape/;`：prelude = `（prelude 无此键）` → 用户版 = `；`
- `punctuator/half_shape/<`：prelude = `（prelude 无此键）` → 用户版 = `《`
- `punctuator/half_shape/=`：prelude = `（prelude 无此键）` → 用户版 = `=`
- `punctuator/half_shape/>`：prelude = `（prelude 无此键）` → 用户版 = `》`
- `punctuator/half_shape/?`：prelude = `（prelude 无此键）` → 用户版 = `？`
- `punctuator/half_shape/@`：prelude = `（prelude 无此键）` → 用户版 = `@`
- `punctuator/half_shape/[`：prelude = `（prelude 无此键）` → 用户版 = `「`
- `punctuator/half_shape/\`：prelude = `（prelude 无此键）` → 用户版 = `､`
- `punctuator/half_shape/]`：prelude = `（prelude 无此键）` → 用户版 = `」`
- `punctuator/half_shape/^`：prelude = `（prelude 无此键）` → 用户版 = `……`
- `punctuator/half_shape/_`：prelude = `（prelude 无此键）` → 用户版 = `——`
- `punctuator/half_shape/``：prelude = `（prelude 无此键）` → 用户版 = ```
- `punctuator/half_shape/{`：prelude = `（prelude 无此键）` → 用户版 = `【`
- `punctuator/half_shape/|`：prelude = `（prelude 无此键）` → 用户版 = `['·', '｜', '§', '¦']`
- `punctuator/half_shape/}`：prelude = `（prelude 无此键）` → 用户版 = `】`
- `punctuator/half_shape/~`：prelude = `（prelude 无此键）` → 用户版 = `~`
- `recognizer/patterns/punct`：prelude = `（prelude 无此键）` → 用户版 = `^v/([0-9]0?|[A-Za-z]+)$`
- `selector/bindings/Down`：prelude = `（prelude 无此键）` → 用户版 = `next_candidate`
- `selector/bindings/Next`：prelude = `（prelude 无此键）` → 用户版 = `next_page`
- `selector/bindings/Prior`：prelude = `（prelude 无此键）` → 用户版 = `previous_page`
- `selector/bindings/Up`：prelude = `（prelude 无此键）` → 用户版 = `previous_candidate`

## 被默认 patch 覆盖而排除出判据的差异键（6 键，验收时最终生效值以 custom patch 为准）

- `menu/page_size`
- `switcher/caption`
- `switcher/hotkeys`
- `switcher/option_list_separator`
- `switcher/save_options`
- `menu/alternative_select_labels`
