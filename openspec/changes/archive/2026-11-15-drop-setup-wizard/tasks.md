# Tasks

> 状态（2026-11-15）：全部完成。冒烟记录：全新安装直进主界面（无向导）；未启用时横幅可见、启用并设默认后隐藏（模拟器上 force-stop 默认 IME 进程会使系统回退到 Gboard，属设备调试现象，横幅逻辑经 onResume 判定验证正确）；以 JRime 为默认输入法实测按键经 onStartInputView → CommitTextMessage 上屏；设置页 maintenance 类别保留正常。横幅初版因 ?attr/colorOnPrimary（Material 属性）在 AppCompat 主题下解析失败 crash，改用 ?android:attr/textColorPrimaryInverse 修复——三处场景已复验。


## 1. 引导向导移除

- [x] 1.1 删除 `ui/setup/`（SetupActivity 与全部 setup fragment），移除 `MainActivity`/launcher alias 到向导的跳转逻辑与 `AndroidManifest.xml` 注册。验证：`./gradlew :app:assembleDebug` 通过；模拟器全新安装直接进主界面。
- [x] 1.2 主界面启用状态横幅：`MainActivity` resume 时检查 `InputMethodManager` enabled/selected 列表，未启用/未设默认时显示非阻塞提示行（现有 View 体系），点击跳 `ACTION_INPUT_METHOD_SETTINGS`。验证：模拟器上 `pm disable` 后横幅出现、启用后消失，主界面其余功能不受阻。

## 2. 存储模式硬编码与 SAF 路径移除

- [x] 2.1 删除 `core/data/DataStorageMode.kt` 与 `AppPrefs.profile.dataStorageMode`/`externalRimeTreeUri`；`DataManager`/`UserDataManager` 调用点收敛为 APP_STORAGE 语义（`isStorageChoiceDone`/`isStorageAvailable` 等判断随之化简或删除）。验证：编译通过，无 `DataStorageMode` 残留引用（`rg` 清零）。
- [x] 2.2 `UserDataManager` 删除 SAF 镜像：`externalRimeTreeUri`/`externalRimeDir`/`.sync-manifest.json`/`hashStream`/`localHash`/`remoteHash`/`sync()` 的 EXTERNAL_SYNC 分支与 `persistTreeUri`/`releasePersistedPermission` 全链；`sync()` 整体为空后连同 `Rime.kt` 中的调用点（`syncUserData`/`deploy` 前后置）一并清理（design Open Question 1 的 userdb levers 流保留决策在此落地）。验证：编译 + `rg "StorageAccess|externalRimeTreeUri|sync-manifest"` 清零。
- [x] 2.3 `ProfileSettingsFragment` 移除存储模式选择与外部目录行（:165-276），保留 userdb 导入导出区；`BackgroundSyncWork` 与 `TrimeApplication.startWorkManager` 删除；`RimeIntentReceiver` 删除 `ACTION_SYNC_USER_DATA`；`libs.versions.toml` 删除 `storageaccess` 与 `androidx.work` 依赖。验证：`./gradlew :app:assembleDebug` + `:app:testDebugUnitTest` + spotless 全绿。

## 3. 回归验收

- [x] 3.1 模拟器冒烟：uninstall → install → 直进主界面（无向导）→ ime enable/set → Chrome 地址栏出字正常（easy_en 上屏）；启用横幅行为符合 spec 场景；设置页 userdb 导入导出入口可达无 crash。验证：逐项记录进本 change 目录验收记录。
