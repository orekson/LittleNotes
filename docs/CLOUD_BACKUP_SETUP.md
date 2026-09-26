# Google Drive 自動備份設定

小小筆記的「個人」頁可用 Google 帳號連接 Drive。第一次連接時設定至少 5 個字元的備份密碼；App 使用既有 .lnbackup 格式加密筆記、分類與附件，再放到使用者 My Drive 可看見的「小小筆記備份」資料夾。Personal 與 Play 依各自的 applicationId 使用不同檔名前綴，避免相互還原不相容素材。每個版本保留最近三份快照。

啟用後，新增、修改、刪除筆記和桌面勾選變更都會排入網路工作。Android 可能延後背景工作；「上次成功備份」時間才代表檔案已上傳。需要立即備份時，可在個人頁按「立即備份」並稍後重新整理狀態。雲端還原會新增筆記，不覆蓋現有筆記；重複還原可能產生重複項目。解除安裝或換機後，本機 Keystore 密碼不會保留，必須重新輸入原備份密碼。

## Google Cloud Console 必要設定

程式碼不包含 OAuth 憑證或服務帳號金鑰。要讓手機的 Google 登入真正運作，應在你自己的 Google Cloud 專案完成：

1. 啟用 Google Drive API，設定 OAuth 同意畫面、應用程式名稱與支援信箱；測試階段將要測試的 Google 帳號加入測試使用者。
2. 將非敏感的 https://www.googleapis.com/auth/drive.file scope 加入同意畫面。App 只建立與管理自己建立的 Drive 備份資料夾與檔案。
3. 為 tw.local.memonote（Personal）及 tw.local.memonote.play（Play）各建立 Android OAuth 用戶端。每個用戶端需填入對應的套件名稱與簽章憑證 SHA-1。Debug、自己簽署的正式版，以及 Google Play App Signing 憑證若不同，應分別登錄對應的 SHA-1。
4. 用已登錄簽章的 APK 在裝有 Google Play 服務的 Android 裝置登入，確認 Drive 中出現「小小筆記備份」資料夾、上次成功時間更新，並實際下載與還原一份備份。

查詢常見 debug 簽章的 SHA-1：

    keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

Google 官方說明：[Android 授權](https://developers.google.com/identity/authorization/android)、[Drive OAuth 範圍](https://developers.google.com/workspace/drive/api/guides/api-specific-auth)、[Drive 檔案上傳](https://developers.google.com/workspace/drive/api/guides/manage-uploads)。

目前本專案沒有已登錄的 Google Cloud OAuth 用戶端資訊；僅編譯 APK 不代表登入已在真實 Google 帳號完成驗證。正式發佈前還需更新 Google Play 的 Data safety 申報及公開隱私權政策，使其與可選雲端備份一致。