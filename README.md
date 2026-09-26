# 小小筆記

Android 個人筆記 App 與可調整大小的桌面小工具。介面可在右上角切換繁體中文、英文、日文、韓文與西班牙文；筆記預設只存在手機本機，可選擇登入 Google 帳號啟用加密 Drive 自動備份。

## 安裝與使用

1. 將交付的 `LittleNotes-1.1.apk` 傳到 Android 手機，開啟安裝。已安裝舊版時直接覆蓋更新。若手機要求，為你用來開啟 APK 的檔案管理器允許「安裝未知應用程式」。
2. 開啟「小小筆記」，按「寫一篇新筆記」。
3. 輸入標題與內容。選取文字後套用色彩、彩虹、柔光；沒有選取時會套用整篇。使用「清除樣式」復原一般文字。
4. 內文下方可使用貼圖、時間提醒、圖片與勾選方框；在這四格上向右滑，第二組第一格是「桌面預覽」，其餘三格預留後續功能。貼圖選單依 flavor 顯示內建素材，也能匯入自己的貼圖。
5. 按「＋ 圖片」在游標處插入手機照片，使用滑桿或雙指縮放，點「套用」。點筆記中的圖片可再次調整；範圍為 24–480 dp，維持比例並自動適應筆記寬度。圖片與貼圖可如文字一般刪除。
6. 按「☐ 勾選方框」新增清單項目，接著輸入文字。筆記內和桌面小工具上皆可點方框，完成會呈現綠色勾勾；再點一下可取消。
7. 選擇奶油／櫻花／海風／星夜背景，或從手機選取照片。「背景淡化」0% 是原圖，100% 淡至底色，不影響文字與貼圖。
8. 按「儲存」，再按主頁「加到桌面小工具」；也可以長按桌面空白處 → 小工具 → 小小筆記。
9. 選擇顯示的筆記，放到空白桌面頁，長按拉大。內容太長可在小工具內上下捲動；上方提供編輯和更換筆記。
10. 底部最右側「個人」頁可連接 Google Drive，設定至少 5 碼備份密碼並開啟自動備份；也可查看上次成功時間、立即備份或從雲端還原。
11. 點筆記首頁或個人頁右上角的語言圖示，可立即切換介面語言、上方標題與日期頁文字；筆記內容不會被翻譯或改動。
12. 底部「日期」頁可查看今天及前後一年，為每個桌面小工具指定某一天要顯示的既有筆記；到該日會自動切換。手動換筆記會覆蓋已到期的安排，未來安排仍保留。
13. 在筆記內文任意位置插入「時間提醒」，選日期與 24 小時制時間；標記顯示 HH:mm。儲存後，到時會以高優先通知呈現標記所在的整行文字。可點標記修改或刪除。Android 13 以上須允許通知；Android 12 以上若未允許精準鬧鐘，提醒可能延遲。

## 後續更新

版本改動見 [CHANGELOG.md](CHANGELOG.md)，Git／GitHub 持續更新、建置、保留原安裝簽章的方式見 [docs/UPDATING.md](docs/UPDATING.md)。GitHub Actions 會檢查單元測試、Lint 與編譯；其 APK 是測試用簽章，手機升級請使用本機原簽章建立的交付檔。

## 功能與界線

- 每個小工具可獨立選不同的筆記及日期安排。同篇筆記的文字、貼圖、背景修改會更新到相關小工具。日期安排與 Widget ID 綁定，換手機後需重新設定；提醒標記與筆記內容一同備份。
- 支援 Android 8.0 / API 26 以上；本次已驗證的系統及未驗證範圍見 `docs/verification.md`。
- 小工具的最大尺寸受桌面程式格線限制，無法覆蓋狀態列、系統導覽區或桌面保留區。
- 彩虹和柔光為靜態效果，不持續閃爍；內建貼圖是 128px 靜態縮圖，放大後可能看見像素。可匯入較高解析度的個人貼圖。
- 背景與樣式按筆記保存。背景照片採置中裁切，暫無手動裁切／移動焦點功能。
- 單篇內文最多 100,000 個 UTF-16 字元，標題 160 字元。長文分段生成小工具圖片，保留原文換行。
- 存檔後或重新調整尺寸可能回到文章頂端。背景更新不是持續輪詢。
- 原始相簿圖片不會修改；App 會保存縮小後的副本。移除小工具不會刪除筆記。
- 加密筆記鎖定期間不會在背景顯示提醒內容；請先解除加密再設定時間提醒。Android 通知顯示方式仍受系統通知權限、勿擾模式與通知頻道設定控制。
- 可手動匯出與還原加密 .lnbackup，也可選擇將最近三份加密備份存到自己 Google Drive 可見資料夾。雲端是單向備份，還原需手動選擇並輸入原密碼；解除安裝會移除本機筆記與 Keystore 中保存的密碼。
- Google 登入需要先在 Google Cloud Console 為各 Flavor 與簽章設定 Android OAuth；操作步驟見 [docs/CLOUD_BACKUP_SETUP.md](docs/CLOUD_BACKUP_SETUP.md)。
- Personal flavor 的內建貼圖來源與非官方標示見 `app/src/personal/docs/ASSET_SOURCES.md`；Play flavor 的公開授權見 `docs/ASSET_SOURCES_PLAY.md`。

## 建置

工具：JDK 17、Android SDK Platform 36 / Build Tools 36.0.0、Gradle 8.13、AGP 8.11.1、Kotlin 2.1.20。

一般開發環境：使用 Android Studio 開啟此資料夾，設定 SDK 後執行：

```powershell
.\gradlew.bat testPersonalDebugUnitTest testPlayDebugUnitTest
.\gradlew.bat lintPersonalDebug lintPlayDebug
.\gradlew.bat assemblePersonalDebug assemblePlayDebug
```

本工作區已安裝專案內工具，可執行：

```powershell
.\build-local.ps1
```

APK 輸出：`app/build/outputs/apk/personal/debug/app-personal-debug.apk` 或 `app/build/outputs/apk/play/debug/app-play-debug.apk`。

## 測試

```powershell
.\gradlew.bat testPersonalDebugUnitTest testPlayDebugUnitTest
.\gradlew.bat assemblePersonalDebugAndroidTest assemblePlayDebugAndroidTest
```

裝置測試使用本地 SQLite、真實 StaticLayout、圖片編解碼及 Activity 重建，並非用假資料庫替代。
測試程式只存在於 androidTest APK，正式交付的 debug App 不含測試資料或測試入口。

`DemoSetup` 是裝置測試 runner；正常執行跑測試，明確傳入 `-e seedDemo true` 才會在測試装置建立截圖用範例筆記。

## 專案結構

- `app/src/main/java/tw/local/memonote/data`：筆記、SQLite、圖片匯入與草稿暫存。
- `model`：選取文字樣式的範圍轉換。
- `rich`：文字樣式、貼圖、背景及長文分段繪製。
- `widget`：Android 桌面小工具更新與捲動內容。
- `app/src/test`、`app/src/androidTest`：單元與裝置測試。
- `app/src/personal`：只放自用版貼圖與私人授權說明。
- `app/src/play`：只放 Google Play 可公開貼圖與授權資料。
- `scripts/Export-Play.ps1`：從 Private source 產生不含 Personal flavor 的安全 Play 匯出。
- `docs/superpowers`：需求、使用者增補及實作計畫。
