# 驗證紀錄

## 1.1 驗證（2026-09-21）

- 建置 `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` 成功；5 項 JVM 測試通過，Lint 0 errors、25 warnings。
- 裝置測試最終結果：`OK (15 tests)`，0 failures；完整輸出在交付資料夾 `output/device-tests-v1.1.txt`。
- Android 11／API 30 AOSP Launcher3 以原簽章覆蓋更新，保留舊筆記與既有桌面綁定。
- 新版與舊版憑證 SHA-256 相同：`74455b9f6dc2e615f2ee38080e4ca5f5d3d39b9108712fe31338e8c70effa51a`。
- 桌面實際點擊：第一個方框變綠色勾勾，其餘保持未完成；再次點擊可取消。圖像與原生點擊區域位置吻合。
- 圖片實際操作：在筆記點既有圖片，大小面板顯示 220 dp；滑桿調為 157 dp 並套用、儲存成功。
- 新增格式相容、圖片尺寸往返與 bitmap 大小限制、方框持久化、編輯期間桌面勾選合併、31 張素材解碼、滿版圖片換行點擊、橫直寬度選擇、密集方框不漏點擊區域測試。
- 獨立審查的圖片換行／橫向幾何問題已修正。實際 Launcher 另發現巢狀 RemoteViews 點擊事件無效，改為集合項目本身的固定點擊區域，重新實測通過。
- 橫直寬度選擇有自動化測試；本次 Launcher 鎖定直向，未宣稱完成其他廠牌橫向桌面實測。尚未覆蓋所有 Android／Launcher 組合。
- GitHub Actions 工作流程已建立於原始碼；GitHub 尚待登入與上傳，尚無雲端執行結果。

## 1.0 驗證（2026-09-20，歷史紀錄）

## 建置與靜態檢查

- `build-local.ps1 -Tasks testDebugUnitTest,assembleDebug,assembleDebugAndroidTest,lintDebug`：BUILD SUCCESSFUL。
- JVM 單元測試：5 項通過、0 失敗。驗證重疊選取、保留原色、柔光和彩虹組合、局部清除、相鄰範圍合併。
- Android lint：0 errors、17 warnings。主要是繁體中文單語文字未抽取成資源、向下相容屬性、可更新版本提醒與系統 EXIF API 建議；未把警告宣稱為零。
- APK v2 簽章驗證通過；套件 `tw.local.memonote`、版本 1.0、minSdk 26、targetSdk 35。
- 環境曾出現 Kotlin daemon 沙箱寫入限制，已改用 in-process compiler。Android 工具的 analytics 目錄警告不影響成功建置。

## Android 模擬器

裝置：Android 11 / API 30，x86_64，Pixel 2 尺寸，AOSP Launcher3；Windows WHPX 加速。

裝置測試共 8 項：

1. SQLite 重開後保留文字、顏色／彩虹／柔光、背景淡化與貼圖來源。
2. 長段中文及 emoji 分段，所有文字完整、每張圖片低於測試設定的記憶體上限。
3. 背景淡化 0% 與 100% 的實際像素結果。
4. 兩個小工具綁定互相獨立，刪除一篇不誤顯示另一篇。
5. 匯入 JPEG 後套用 EXIF 90 度方向。
6. 超寬貼圖縮放至可容納的範圍。
7. 4,000 個格式範圍的筆記，saved state 小於 64KB，Activity 重建後內容與樣式完整。
8. 2,500 行筆記不建立整篇 EditText 的巨大 software bitmap layer。

原始輸出見 `output/device-tests.txt`。

## 真正桌面操作

- 從 App 呼叫系統加入小工具，Launcher3 成功放置。
- 小工具第一次尚未選文時，顯示選文入口；完成選擇後顯示指定筆記。
- 顯示兩張小人貼圖、彩虹漸層、柔光與櫻花淡化背景；保存 `output/widget-preview.png`。
- 長按並拖動尺寸控制點，由 4 格寬拉到 5 格寬；文字重新換行，沒有橫向裁切。
- 在小工具內連續向上滑動，成功讀到「已經讀到最後一行了 ✦」；保存 `output/widget-scroll-end.png`。
- 透過「換筆記」切至另一篇，標題及內文皆替換正確。
- 從小工具「編輯」修改標題並儲存，回桌面可見更新後標題，無需手動重新加入。
- 該輪操作後查閱 AndroidRuntime / MemoNote error log，未見 App 崩潰或小工具更新錯誤。

## 審查與修正

獨立程式審查指出四項問題：巨大軟體圖層、Bundle 草稿容量、寬貼圖裁切、EXIF 方向。已逐一加入可重現測試，先觀察失敗，再修正並通過。

草稿改用 AtomicFile，本機只保存少量還原識別資訊於 Bundle；移除無界 software layer；貼圖寬高同時受限；匯入照片先套用 EXIF 方向再保存。

## 實際限制

- 尚未在使用者的實體手機、Android 8 或 Android 12–16、各品牌桌面程式逐一驗證。
- 真正桌面已測試一個小工具的換文和編輯；多個同時顯示的小工具之独立資料綁定已由裝置測試驗證，未逐一拍攝多桌面操作。
- 相簿系統選擇器的所有外部雲端供應商未逐一驗證；圖片解碼與方向處理由實際檔案測試驗證。
- 此次交付為可安裝的 debug APK，尚未公開上架；貼圖來源及素材散布限制記錄於 ASSET_SOURCES.md。
