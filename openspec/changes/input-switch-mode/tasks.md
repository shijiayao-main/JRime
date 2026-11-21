# Tasks

- [x] 1.1 `AsciiSwitchMode` 偏好（BOTH/BUTTON_ONLY/SHIFT_ONLY）+ 中英文案 + 输入组归类
- [x] 1.2 `ImeSurfaceConfig.showLanguageToggle/shiftTogglesLanguage` 与顶栏渲染
- [x] 1.3 `ComposeInputView` 监听偏好并注入；`ImeCommandExecutor` 在仅按钮模式拦截 Shift_L
- [x] 1.4 修复外观注入遗漏（候选字号/键面提示/候选注解开关）
- [x] 1.5 单测/编译/spotless 全绿
- [x] 1.6 模拟器验证：输入组出现「中英切换方式」，弹窗三选项；选「仅顶栏按钮」后偏好持久化（BUTTON_ONLY），
      重新安装后行值正确显示「仅顶栏按钮」，切回默认后恢复 BOTH（截图为证）
- [ ] 1.7 「仅顶栏按钮」下 Shift 不再切换的实际键盘行为（触屏点按验收）
