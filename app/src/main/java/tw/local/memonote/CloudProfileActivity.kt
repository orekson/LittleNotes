package tw.local.memonote

import android.accounts.Account
import android.app.AlertDialog
import android.content.Intent
import tw.local.memonote.ui.LocalizedActivity
import tw.local.memonote.ui.LanguageSwitcher
import tw.local.memonote.ui.AppLanguage
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import tw.local.memonote.cloud.CloudAuth
import tw.local.memonote.cloud.CloudBackupJob
import tw.local.memonote.cloud.CloudBackupState
import tw.local.memonote.cloud.CloudDriveClient
import tw.local.memonote.cloud.RemoteBackup
import tw.local.memonote.data.BackupRepository
import tw.local.memonote.ui.BottomNavigation
import tw.local.memonote.ui.PasswordDialogs
import tw.local.memonote.ui.Ui
import tw.local.memonote.widget.NoteWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date

class CloudProfileActivity : LocalizedActivity() {
    private companion object {
        const val GOOGLE_AUTH = 503
        const val CONNECT = 1
        const val RESTORE = 2
    }
    private var pendingAction = 0

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        pendingAction = state?.getInt("pendingAction") ?: 0
    }

    override fun onResume() {
        super.onResume()
        showProfile()
    }

    override fun onSaveInstanceState(out: Bundle) {
        out.putInt("pendingAction", pendingAction)
        super.onSaveInstanceState(out)
    }

    private fun showProfile() {
        val root = Ui.root(this)
        val scroll = ScrollView(this)
        val content = Ui.column(this)
        Ui.pad(content, 24)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val titleRow = Ui.row(this)
        titleRow.addView(Ui.label(this, "個人與設定", 30f, bold = true),
            LinearLayout.LayoutParams(0, -2, 1f))
        LanguageSwitcher.add(this, titleRow)
        content.addView(titleRow)
        content.addView(Ui.space(this, 10))
        content.addView(Ui.label(this, "Google Drive 自動備份", 21f, bold = true))
        content.addView(Ui.space(this, 8))

        val linked = CloudBackupState.connected(this)
        val account = CloudBackupState.account(this)
        content.addView(Ui.label(this,
            if (linked) AppLanguage.text(this, "已連結 Google Drive") + if (account.isBlank()) "" else " · " + account
            else "尚未連結 Google 帳號",
            15f, Ui.muted))
        content.addView(Ui.space(this, 10))
        content.addView(Ui.button(this,
            if (linked) "重新連接 Google 帳號" else "使用 Google 帳號登入", true) {
            authorize(CONNECT)
        })
        content.addView(Ui.label(this,
            "備份會以密碼加密，存到你在 Google Drive 可看見的「小小筆記備份」資料夾。",
            13f, Ui.muted))
        if (linked) {
            content.addView(Ui.space(this, 14))
            content.addView(Ui.button(this,
                if (CloudBackupState.enabled(this)) "自動備份：開啟（點一下關閉）"
                else "自動備份：關閉（點一下開啟）") {
                val turnOn = !CloudBackupState.enabled(this)
                try {
                    CloudBackupState.setEnabled(this, turnOn)
                    if (turnOn) CloudBackupJob.schedule(this, manual = true)
                    else CloudBackupJob.cancel(this)
                    showProfile()
                } catch (e: Exception) {
                    Ui.toast(this, "無法更新自動備份設定")
                }
            })
            content.addView(Ui.button(this, "立即備份到 Google Drive") {
                CloudBackupJob.schedule(this, manual = true)
                Ui.toast(this, "已排入雲端備份，請稍後查看狀態")
            })
            content.addView(Ui.button(this, "從 Google Drive 還原") {
                AlertDialog.Builder(this).setTitle(AppLanguage.text(this, "從雲端還原筆記？"))
                    .setMessage(AppLanguage.text(this, "選取的備份會新增到這台裝置；現有筆記不會被覆蓋。需要當初設定的備份密碼。"))
                    .setNegativeButton(AppLanguage.text(this, "取消"), null)
                    .setPositiveButton(AppLanguage.text(this, "繼續")) { _, _ -> authorize(RESTORE) }
                    .show()
            })
            val last = CloudBackupState.lastSuccess(this)
            val text = if (last == 0L) "尚未完成雲端備份"
            else AppLanguage.format(this, "上次成功備份：%1\$s", SimpleDateFormat("yyyy/MM/dd HH:mm", AppLanguage.locale(this))
                .format(Date(last)))
            content.addView(Ui.space(this, 12))
            content.addView(Ui.label(this, text, 14f, Ui.ink))
            val problem = CloudBackupState.problem(this)
            if (problem.isNotBlank()) {
                val http = Regex("Google Drive 備份失敗（([0-9]+)），請稍後重試").matchEntire(problem)
                val displayProblem = if (http != null) AppLanguage.format(this,
                    "Google Drive 備份失敗（%1\$d），請稍後重試", http.groupValues[1].toInt())
                else AppLanguage.text(this, problem)
                content.addView(Ui.label(this, displayProblem, 13f, 0xffb64453.toInt()))
            }
            content.addView(Ui.button(this, "重新整理備份狀態") { showProfile() })
            content.addView(Ui.button(this, "中斷 Google 連結") { confirmDisconnect() })
        }
        content.addView(Ui.space(this, 20))
        content.addView(Ui.label(this, "備份密碼請自行保存。換手機、重裝或忘記密碼時，只有輸入原密碼才能還原雲端檔案。",
            13f, Ui.muted))
        content.addView(Ui.space(this, 20))
        content.addView(Ui.button(this, "隱私權政策") {
            val policy = assets.open("i18n/privacy_" + AppLanguage.code(this) + ".txt").bufferedReader().use { it.readText() }
            AlertDialog.Builder(this)
                .setTitle(AppLanguage.text(this, "小小筆記隱私權政策"))
                .setMessage(policy)
                .setPositiveButton(AppLanguage.text(this, "關閉"), null)
                .show()
        })
        content.addView(Ui.space(this, 12))
        BottomNavigation.add(this, root, true, { finish() }, {})
    }

    private fun authorize(action: Int) {
        pendingAction = action
        val request = CloudAuth.request(
            if (action == RESTORE) CloudBackupState.account(this) else ""
        )
        try {
            Identity.getAuthorizationClient(this).authorize(request)
                .addOnSuccessListener(this) { result ->
                    if (result.hasResolution()) {
                        try {
                            val pending = result.pendingIntent ?: error("沒有 Google 授權視窗")
                            startIntentSenderForResult(
                                pending.intentSender, GOOGLE_AUTH, null, 0, 0, 0
                            )
                        } catch (e: Exception) {
                            Ui.toast(this, "無法開啟 Google 登入畫面")
                        }
                    } else handleAuthorization(result)
                }
                .addOnFailureListener(this) {
                    Ui.toast(this, "無法連接 Google，請檢查網路與 Google Play 服務")
                }
        } catch (e: Exception) {
            Ui.toast(this, "這台裝置無法使用 Google 登入")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != GOOGLE_AUTH) return
        if (resultCode != RESULT_OK || data == null) {
            Ui.toast(this, "已取消 Google 授權")
            return
        }
        try {
            handleAuthorization(
                Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data)
            )
        } catch (e: Exception) {
            Ui.toast(this, "Google 授權未完成，請重試")
        }
    }

    private fun handleAuthorization(result: AuthorizationResult) {
        if (!result.grantedScopes.contains(CloudBackupState.DRIVE_SCOPE)) {
            Ui.toast(this, "請同意小小筆記存取自己建立的 Drive 備份檔")
            return
        }
        val token = result.accessToken?.takeIf { it.isNotBlank() }
        if (token == null) {
            Ui.toast(this, "Google 未提供 Drive 授權，請重試")
            return
        }
        val action = pendingAction
        pendingAction = 0
        if (action == RESTORE) {
            chooseCloudRestore(token)
            return
        }
        val account = CloudAuth.accountName(result)
        val previous = CloudBackupState.account(this)
        val existing = if (CloudBackupState.connected(this) &&
            (account.isBlank() || account == previous)) {
            runCatching { CloudBackupState.password(this) }.getOrNull()
        } else null
        if (existing != null) {
            try { enableCloud(account.ifBlank { previous }, existing) }
            finally { existing.fill('\u0000') }
        } else {
            PasswordDialogs.ask(
                this, "設定雲端備份密碼",
                "至少 5 個字元。每份雲端備份都會加密；換手機還原時必須輸入這個密碼，忘記就無法還原。",
                true
            ) { password ->
                try { enableCloud(account, password) }
                finally { password.fill('\u0000') }
            }
        }
    }

    private fun enableCloud(account: String, password: CharArray) {
        try {
            CloudBackupState.connect(this, account, password)
            CloudBackupJob.schedule(this, manual = true)
            showProfile()
            Ui.toast(this, "已連接 Google Drive，首次備份已排入")
        } catch (e: Exception) {
            Ui.toast(this, "無法啟用雲端備份，請重試")
        }
    }

    private fun chooseCloudRestore(token: String) {
        work("正在讀取雲端備份", { CloudDriveClient(token).backups(packageName) }) { result ->
            result.onSuccess { backups ->
                if (backups.isEmpty()) {
                    Ui.toast(this, "這個 Google 帳號沒有小小筆記雲端備份")
                } else showBackupChoices(token, backups)
            }.onFailure { Ui.toast(this, "無法讀取雲端備份，請檢查 Google Drive 連線") }
        }
    }

    private fun showBackupChoices(token: String, backups: List<RemoteBackup>) {
        val items = backups.map { backup ->
            backup.createdTime.replace('T', ' ').take(16).ifBlank { backup.name }
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle(AppLanguage.text(this, "選擇要還原的備份"))
            .setItems(items) { _, index ->
                val remote = backups[index]
                PasswordDialogs.ask(
                    this, "輸入雲端備份密碼",
                    "密碼錯誤或檔案損壞時，現有筆記不會改動。",
                    false
                ) { password ->
                    work("正在還原雲端備份", {
                        try {
                            CloudDriveClient(token).readBackup(remote.id) {
                                BackupRepository.restore(this, it, password)
                            }
                        } finally { password.fill('\u0000') }
                    }) { restored ->
                        restored.onSuccess {
                            NoteWidgetProvider.updateAll(this)
                            Ui.toast(this, AppLanguage.format(this, "已還原 %1\$d 篇筆記", it))
                        }.onFailure {
                            Ui.toast(this, "雲端還原失敗：密碼錯誤、檔案損壞或網路中斷")
                        }
                    }
                }
            }.setNegativeButton(AppLanguage.text(this, "取消"), null).show()
    }

    private fun confirmDisconnect() {
        AlertDialog.Builder(this).setTitle(AppLanguage.text(this, "中斷 Google 連結？"))
            .setMessage(AppLanguage.text(this, "停止自動備份並清除這台裝置保存的備份密碼。Drive 裡已建立的加密備份檔會保留。"))
            .setNegativeButton(AppLanguage.text(this, "取消"), null)
            .setPositiveButton(AppLanguage.text(this, "中斷連結")) { _, _ ->
                val account = CloudBackupState.account(this)
                try {
                    CloudBackupState.disconnect(this)
                    showProfile()
                    val builder = RevokeAccessRequest.builder()
                        .setScopes(listOf(Scope(CloudBackupState.DRIVE_SCOPE)))
                    if (account.isNotBlank()) {
                        builder.setAccount(Account(account, "com.google"))
                    }
                    Identity.getAuthorizationClient(this).revokeAccess(builder.build())
                        .addOnFailureListener(this) {
                            Ui.toast(this, "本機已停止備份；Google 授權移除失敗，可至 Google 帳號設定檢查")
                        }
                } catch (e: Exception) {
                    Ui.toast(this, "無法中斷連結，請重試")
                }
            }.show()
    }

    private fun <T> work(label: String, action: () -> T, finish: (Result<T>) -> Unit) {
        val progress = AlertDialog.Builder(this).setTitle(AppLanguage.text(this, label))
            .setView(ProgressBar(this)).setCancelable(false).create()
        progress.show()
        Thread {
            val result = runCatching(action)
            runOnUiThread {
                progress.dismiss()
                if (!isDestroyed) finish(result)
            }
        }.start()
    }
}