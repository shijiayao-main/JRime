# Spec Delta

## Purpose

设置的信息架构与偏好生命周期：死设置清零、外观与候选体验可调、设置变更实时反映到共享表面。

## ADDED Requirements

### Requirement: 外观设置组

设置 SHALL 提供独立的「外观」页，包含深浅模式（跟随系统/亮/暗）、强调色（蓝/绿/紫）、候选单页数量（5–9）、候选字号（15–22sp）与键面提示开关；这些项 SHALL 不再散落在虚拟键盘页。

#### Scenario: 外观页可调

- **WHEN** 用户打开「外观」页
- **THEN** 五项设置均可见、可修改，且默认值分别为跟随系统/蓝/7/18sp/开

#### Scenario: 设置实时生效

- **WHEN** 用户修改强调色或深浅模式后回到输入框
- **THEN** 键盘的回车/候选高亮/强调元素立即使用新颜色，无需重启输入法

### Requirement: 无死设置

设置 SHALL NOT 暴露没有消费者的偏好；本变更 SHALL 删除已知的 7 个零消费者偏好与 4 个死常量。

#### Scenario: 虚拟键盘页无失效项

- **WHEN** 检查虚拟键盘页的偏好列表
- **THEN** 不存在 `vibrate_on_key_release`/`vibrate_on_key_repeat`/`swipe_velocity`/`double_tap_timeout`/`slide_step_size`/`hook_shift_arrow`/`clipboard_mask_sensitive` 条目

### Requirement: 偏好跨模式持久化

外观设置 SHALL 与其他键盘/剪贴板偏好一样在普通模式与 direct-boot 模式之间同步存储。

#### Scenario: 锁屏输入保留外观

- **WHEN** 设备重启进入 direct boot 并使用锁屏输入
- **THEN** 键盘仍使用用户设置的强调色与深浅模式
