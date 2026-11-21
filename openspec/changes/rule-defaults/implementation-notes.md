# Implementation Notes — rule-defaults

## 2026-11-21

- 修改资产源（`app/src/main/assets/shared`）：
  - `flypy_xhfast.schema.yaml` / `flyhe_fast.schema.yaml`：`ascii_punct reset: 0`；
  - `flypy_xhfast.custom.yaml`：`charset reset: 0`，`menu/page_size: 7`。
  - `assets/rime/build` 是 `precompileRimeData` 从 shared 生成的（.gitignore 忽略），
    运行 assembleDebug 后已确认重新生成：ascii_punct reset 0、charset reset 0、page_size 7。
  - 未动 emoji（默认开）、注解（默认声调）、prediction（默认关）等符合预期的开关。
- 验证（emulator-5554，`pm clear` 后首装）：设置 → 分类 → 输入，UI dump 的 switch 状态：
  使用英文标点 `checked=false`、全角 `false`、表情候选 `true`、联想 `false`、增广字集 `false`。
  截图：`screenshots/2026-11-21-input-rules-default-{1,2}.png`。
- 升级路径：`DataManager.sync()` 每次引擎启动按 checksum diff 释放资产（既有机制），旧安装不清数据也会更新；
  用户自建 custom 覆盖仍优先。
