# doc/ 文档索引

> 2026-11-21 起，文档按「权威链」管理。找方向看 L1，找单期细节看 L2，别再从历史文档里翻执行计划。

## L1 权威

| 文档 | 用途 |
|---|---|
| [master-plan.md](master-plan.md) | **唯一权威**：产品定位、目标架构、键盘表面设计体系、里程碑路线（M0–M4 / D0–D3 / Path B）、治理与待裁决项 |

## L2 执行（单期变更）

见 `openspec/changes/`。当前在途：`surface-foundation`（对应 M1）。规划中的后续：`surface-p0`、`settings-v2`、`input-rules`、`userdict-v2`、`release-hardening`、`desktop-core`、`desktop-cmp`、`desktop-parity`。

## L3 专项事实文档

| 文档 | 用途 |
|---|---|
| [harness-design.md](harness-design.md) | librime oracle / golden 轨迹 / harness 验收设计（Path B 的验收底座） |
| [pathb-syllabifier-spec.md](pathb-syllabifier-spec.md) | Path B 音节切分器语义规格（41 facts） |
| [architecture-review.md](architecture-review.md) | 2026-11-18 全仓架构评审（历史证据，P1/P2 清单仍可引用） |
| [changelog.md](changelog.md) | 交付记录：各批次做了什么/怎么验证/遗留项，验收对照用 |
| [verification-guide.md](verification-guide.md) | **最终验收手册**：手机/三平台逐项操作与期望、已知限制、回滚与反馈清单 |
| [Keyboard.md](Keyboard.md) | Liquid Keyboard 历史参数笔记（trime 遗产） |
| [trime-schema.json](trime-schema.json) | 方案配置 schema（引擎输入参考） |

## L4 长期规格

`openspec/specs/`：已归档能力的契约（当前 `input-schemas`、`profile-storage`）。

## 历史（不再作为执行依据）

| 文档 | 被取代说明 |
|---|---|
| [refactor/roadmap.md](refactor/roadmap.md) | 六期主线已被 `master-plan.md` 的 M0–M4 取代；其中引擎/启动链事实与决策记录仍有效，可当证据引用 |
| [multiplatform-design.md](multiplatform-design.md) | 多平台设计已被 `master-plan.md` §3/§5 与 D0–D3 取代；其中 Path B 数据面、协议、平台壳约束仍是 D1/D2 的技术输入 |
| [surface-plan.md](surface-plan.md) | 表面层任务队列已执行完毕；后续需求进 `surface-foundation`/`surface-p0` 等 change |
| [trim-plan.md](trim-plan.md) | JNI 裁剪清单已完成（`trim-jni-surface`） |

## 文档纪律

- 执行计划只写在 `master-plan.md` 与 change 工件里；历史文档只加注记、不再续写。
- 每个 change 完成后把实际状态回写 `master-plan.md` 路线图，并归档 change。
