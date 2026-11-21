<!--
SPDX-FileCopyrightText: 2015 - 2026 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# JRime

**JRime 是一個專注日常輸入的 Android Rime 輸入法**：共享 Compose 鍵盤與介面、精簡的分類設定、
使用者詞典工具，以及桌面端配套。它衍生自 [Trime](https://github.com/osfans/trime)，透過 JNI
執行 [RIME](https://rime.im/) 引擎（`librime`）。

預設方案是 **小鶴雙拼 / 飛鶴快拼**（`flypy_xhfast`）——這只是預設值，不是綁定：JRime 是通用
Rime 前端，其他方案可以像任何 Rime 應用一樣匯入並切換。

[English](README.md) | [简体中文](README_sc.md)

## 功能

- **一套 Compose 介面**：鍵盤、頂欄、候選條、面板與設定共用同一份 Compose 程式碼，並已 KMP 化
  到桌面；統一的 design token / 配色層。
- **日常可用的候選**：候選分頁、長按選單（置頂/刪詞/複製/查碼）、小鶴韻母鍵面提示、
  候選單頁數量/字號/註解可調。
- **輸入規則放進應用**：簡繁、全形、英文標點、表情候選、聯想、單字優先、全拼預編輯、碼區提示、
  增廣字集。
- **分類設定 + 搜尋**：輸入/鍵盤/候選/外觀/剪貼簿/系統與資料六類，不再是一條超長清單。
- **詞典工具**：瀏覽/搜尋/加詞/調頻/刪除使用者詞典；常用語面板（`custom_phrase.txt`）；
  整機使用者資料備份/還原；複製診斷資訊。
- **長按輸入**：字母列長按數字/大寫/符號；空格長按語音輸入法、Enter 長按換行、逗號長按頓號。
- **桌面配套（進行中）**：共享 JVM 引擎/協議、Compose 候選窗應用與設定應用、macOS IMK 殼、
  Linux IBus 引擎殼、Windows TSF 腳手架。
- **穩定性**：啟動失敗降級、設定崩潰修復、設定無孤兒測試、表面守護測試、引擎 golden 回歸。

## 環境需求

- Android 8.0（API 26）以上。
- 建置：JDK 20、Android SDK 37、Gradle wrapper（`./gradlew`）。引擎原始碼已隨倉庫；桌面建置另需
  宿主 librime（見 `harness/README.md`）。
- 可選語音輸入：安裝任意系統語音輸入法（如 Google 語音輸入）後可使用長按空格。

## 建置與安裝

```sh
# 全倉閘門（單測 + assembleDebug + spotless）
./gradlew test :core:model:jvmTest :core:engine:jvmTest :ui:ime:desktopTest \
  :app:assembleDebug spotlessCheck

# 建置 + 安裝到已連線裝置並啟用輸入法
./script/install-debug.sh
```

release 預設未簽名；設定 `SIGNING_KEY`、`KEY_STORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`
（見 `app/build.gradle.kts`）後簽名。

## 自己驗收

- [doc/verification-guide.md](doc/verification-guide.md)：手機/macOS/Linux 逐項驗收、已知限制、
  回滾與回饋清單。
- [doc/changelog.md](doc/changelog.md)：每批交付與驗證方式。
- [doc/perf-baseline.md](doc/perf-baseline.md)：APK 體積與冷啟動基線。

## 文件

| 文件 | 用途 |
|---|---|
| [doc/master-plan.md](doc/master-plan.md) | 總規劃（產品原則、架構、里程碑、風險） |
| [doc/verification-guide.md](doc/verification-guide.md) | 主人驗收清單 |
| [doc/changelog.md](doc/changelog.md) | 交付記錄與已修問題 |
| [doc/voice-input-notes.md](doc/voice-input-notes.md) | 長按空格語音的路線比較與結論 |
| [doc/perf-baseline.md](doc/perf-baseline.md) | 體積/啟動基線 |
| [doc/screenshots/README.md](doc/screenshots/README.md) | 介面證據索引 |
| [openspec/changes/](openspec/changes/) | 各期 change 提案、任務與實作說明 |

桌面殼見 [desktop/](desktop/README.md)，macOS 殼見 [macos/JRimeIME/](macos/JRimeIME/README.md)。

## 授權與致謝

JRime 以 **GPL-3.0-or-later** 授權——見 [LICENSE](LICENSE)。

- 完整第三方授權清單：[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
  （由 `script/gen-third-party-notices.py` 產生），應用內「關於 → 開源說明」同樣可查。
- 上游致謝：[Rime 社群](https://rime.im/) 的 `librime`、[OpenCC](https://github.com/BYVoid/OpenCC)，
  以及 JRime 衍生自的 Android 前端 [Trime](https://github.com/osfans/trime)；Rime 詞典/方案
  版權歸各自作者。
- JRime 自身新增的 Compose 介面、設定、詞典工具、桌面配套與修復均為本專案內容；與上游應用無隸屬關係。

## 隱私

JRime 在本機執行 Rime 引擎，不收集、不上傳輸入內容。可選語音輸入由你切換到的系統語音輸入法負責
（JRime 本身不申請麥克風權限）。見 [PRIVACY.md](PRIVACY.md)。
