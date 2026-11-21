# Tasks

> 状态（2026-11-21）：完成。

## 1. 数据层

- [x] 1.1 `CustomPhraseCodec`（解析/渲染/header 保留/坏行丢弃）+ 单测
- [x] 1.2 `CustomPhraseStore`（用户目录读写、shared 播种、公开流、保存后重启会话）
- [x] 1.3 首次使用从 shared 目录播种 115 条随包置顶词

## 2. 表面接线

- [x] 2.1 `PhraseWindow` + controller `PanelId.Phrase` 映射 + executor 打开面板
- [x] 2.2 `PhrasePanel` Compose UI（列表/点选上屏/新增/删除）
- [x] 2.3 顶栏「语」入口（顶栏动作行改为可横向滚动，避免动作增多后挤出屏幕）

## 3. 验证

- [x] 3.1 全仓闸门（test/assembleDebug/spotless）
- [x] 3.2 模拟器：面板显示置顶词、点选「啊」上屏并回到键盘（截图归档）
- [ ] 3.3 真机：新增/删除条目后候选立即生效（最终用户验收）
