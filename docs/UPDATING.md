# 持續更新小小筆記

## 日常修改

1. 在本機專案資料夾拉取最新原始碼：`git pull --ff-only`。
2. 修改程式，每次交付增加 `app/build.gradle.kts` 的 `versionCode`，並更新 `versionName` 與 `CHANGELOG.md`。
3. 執行 `./gradlew testPersonalDebugUnitTest testPlayDebugUnitTest lintPersonalDebug lintPlayDebug assemblePersonalDebug assemblePlayDebug`；需要 AndroidTest APK 時執行 `assemblePersonalDebugAndroidTest assemblePlayDebugAndroidTest`。
4. `git add .`、`git commit -m "描述本次修改"`、`git push`。每次 main 更新或 PR 都會啟動 GitHub Actions。
5. 用原有簽章建立 APK，安裝到手機時選更新。不要先解除安裝。

Windows 現有工作區可以直接執行 `./build-local.ps1`，它使用 `.tools` 內已準備好的 SDK、JDK、Gradle 與簽章位置。新電腦請安裝 Android Studio／JDK 17，使用 Gradle Wrapper。`.tools` 不會上傳 GitHub。

## 保留筆記的升級條件

- 永遠保留 applicationId `tw.local.memonote`。產品顯示名稱可以改。
- 新 APK 必須使用和已安裝版本相同的簽章金鑰；版本號須遞增。
- 此工作區的 1.0／1.1 使用 `.tools/android-user/debug.keystore`。請自行備份此金鑰到安全位置。金鑰不會提交 Git，也不會放入原始碼 ZIP。
- 新電腦要延續安裝，需安全搬移同一金鑰，將 `ANDROID_USER_HOME` 指向其所在資料夾，再使用相同建置程序。不要把金鑰傳到公開儲存庫。
- GitHub Actions 的 `LittleNotes-ci-test-only` 使用臨時 debug 簽章，僅供全新測試裝置驗證，不能覆蓋手機已安裝版。正式交付使用本機原簽章建置的 APK。CI 製品不當作可直接升級的版本發布。
- 本機資料不在 GitHub；清除 App 資料或解除安裝會刪除筆記。修改 SQLite 結構時需提供保留資料的 migration。

## Product Flavors 與 Play 匯出

- `personal` 使用 `tw.local.memonote`，自用素材只在 `app/src/personal`。
- `play` 使用 `tw.local.memonote.play`，公開素材只在 `app/src/play`。
- 共用功能只修改 `app/src/main`；不要再建立第二份 Android 專案。
- 先在本機預覽安全匯出：`./scripts/Export-Play.ps1 -AllowUncommitted -SkipBuild`。
- 正式匯出會檢查路徑、檔案內容、素材授權與 Play flavor build：`./scripts/Export-Play.ps1`。
- 驗證既有匯出目錄：`./scripts/Verify-PlayExport.ps1 -Path output/LittleNotes-Play`。
- `Export-Play.ps1` 預設不會觸碰任何 remote；只有明確指定 `-Apply -PublicRepoPath ...` 才會更新本機 Public clone，`-Push` 另需明確指定。

## 分享與素材

此私人專案含第三方貼圖，來源列於 `ASSET_SOURCES.md`。公開散布前應確認素材授權或移除相關內建圖檔。GitHub 私人儲存庫的可見性可在取得適當素材使用權後再調整。
