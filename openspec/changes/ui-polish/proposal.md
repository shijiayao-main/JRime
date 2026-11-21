# Proposal: ui-polish

## Why

图标系统已覆盖顶栏/设置/候选/面板，但键盘功能键仍是字形（⌫/↵/⇧），启动图标延续 `快` 字旧标记
（方案名联想），示例扩展键盘高度偏矮。收尾这三处，让「图标语义化 + 品牌方案中立」在键面与启动器也成立。

## What Changes

- `ImeIcon` 增加 `Backspace`/`Enter`/`Shift`（outlined Material Symbols），`KeySurface` 对
  `BackSpace`/`Return`/`Shift_L` 键面渲染矢量图标（schema 驱动的回车文字、中文「辅助」保留文字面）。
- 启动图标重绘：白色 `J` monogram + 强调色 key dot，背景 `#12263A`；去掉 `快` 字（方案名联想），
  保持 monochrome adaptive icon。
- 示例扩展键盘补足符号行，使扩展键盘填充与内置键盘一致的 band。
- 文档：图标表更新、变更记录、变更目录。

## 验证

- 模拟器：键盘功能键显示矢量图标；示例键盘 4 行；构建/单测/spotless 闸门。
- 启动图标矢量通过资源编译；真机桌面效果留最终验收。
