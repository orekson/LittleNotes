package tw.local.memonote.cloud

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import tw.local.memonote.data.BackupRepository
import java.io.File
import java.io.IOException

/** A persisted, network-constrained job; note writes only enqueue it. */
class CloudBackupJob : JobService() {
    private var running: Thread? = null

    override fun onStartJob(params: JobParameters): Boolean {
        if (!CloudBackupState.connected(this) ||
            (!CloudBackupState.enabled(this) && !params.extras.getBoolean("manual"))) return false
        running = Thread {
            var password: CharArray? = null
            var encrypted: File? = null
            var retry = false
            try {
                password = CloudBackupState.password(this) ?: throw AuthorizationNeeded()
                val token = CloudAuth.token(this)
                encrypted = File.createTempFile("cloud-backup-", ".lnbackup", cacheDir)
                encrypted.outputStream().use { BackupRepository.write(this, it, password) }
                CloudDriveClient(token).upload(packageName, encrypted)
                CloudBackupState.markSuccess(this)
            } catch (e: AuthorizationNeeded) {
                CloudBackupState.markProblem(this, "請重新連接 Google 帳號並確認備份密碼")
            } catch (e: DriveHttpException) {
                retry = e.status == 401 || e.status == 408 || e.status == 429 || e.status >= 500
                CloudBackupState.markProblem(this,
                    if (e.status == 403) "Google Drive 權限不足，請重新登入"
                    else "Google Drive 備份失敗（" + e.status + "），請稍後重試")
            } catch (e: Exception) {
                retry = e is IOException || e is java.util.concurrent.ExecutionException
                CloudBackupState.markProblem(this, "備份未完成，請確認網路與 Google 帳號狀態")
            } finally {
                password?.fill('\u0000')
                encrypted?.delete()
                jobFinished(params, retry)
            }
        }.also { it.start() }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.interrupt()
        running = null
        return true
    }

    companion object {
        private const val JOB_ID = 51340

        fun schedule(context: Context, manual: Boolean = false) {
            if (!CloudBackupState.connected(context) ||
                (!CloudBackupState.enabled(context) && !manual)) return
            val scheduler = context.getSystemService(JobScheduler::class.java)
            val extras = PersistableBundle().apply { putBoolean("manual", manual) }
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, CloudBackupJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setMinimumLatency(if (manual) 0L else 15_000L)
                .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setExtras(extras)
                .build()
            if (scheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) {
                CloudBackupState.markProblem(context, "系統無法排入自動備份")
            }
        }

        fun cancel(context: Context) {
            context.getSystemService(JobScheduler::class.java).cancel(JOB_ID)
        }
    }
}