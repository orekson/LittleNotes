# 小小筆記專案開發規則

## 主要專案

- `C:\LittleNotes` 是唯一主要開發專案。
- `C:\LittleNotes-GooglePlay` 只是 Public 版本的安全匯出結果，不要直接在裡面開發功能。

## 共用與版本專屬功能

- `app/src/main` 是 Personal 與 Play 共用功能的唯一來源。
- 如果使用者沒有特別指定，所有新功能預設放在 `app/src/main`，兩個版本都必須具備。
- 使用者說「只給 personal」時，該功能只能出現在 Personal 版。
- 使用者說「只給 play」時，該功能只能出現在 Play 版。
- 不要建立第二份獨立 Android 專案來實作共同功能。

## Product Flavors

- `personal` 的 applicationId 是 `tw.local.memonote`。
- `personal` 可以包含 Hololive 私人素材；這些素材只能存在 Private repository。
- `play` 的 applicationId 是 `tw.local.memonote.play`。
- `play` 只能包含可公開、可合法上架 Google Play 的內容與素材。

## Public 匯出安全規則

- 絕對不能把 `src/personal` 匯出到 Public 版本。
- 絕對不能把 Hololive、Walfie 或相關私人素材、檔名、來源文件與文字匯出到 Public 版本。
- Public 匯出必須使用既有的安全匯出與掃描流程，並排除 Private repository 的 Git 歷史、build 產物與簽章檔。若安全掃描、build、test、lint已在本次工作中成功通過，後續步驟不要重複執行，除非相關原始碼或匯出內容有再次變更

## Git 提交訊息

- Git commit 訊息使用繁體中文，簡短描述新增或修改的功能。
