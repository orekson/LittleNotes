package tw.local.memonote.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import tw.local.memonote.data.NoteStore
import tw.local.memonote.ui.AppLanguage

object ReminderScheduler {
    const val ACTION_FIRE = "tw.local.memonote.reminder.FIRE"
    const val CHANNEL = "note_reminders"
    private const val PREFS = "reminder_alarms"
    private const val KEY = "records"
    private data class Record(val noteId: Long, val id: String, val time: Long) {
        fun persist(): String = noteId.toString() + "|" + id + "|" + time
    }

    private fun parse(raw: String): Record? {
        val parts = raw.split('|')
        if (parts.size != 3) return null
        return Record(parts[0].toLongOrNull() ?: return null, parts[1],
            parts[2].toLongOrNull() ?: return null)
    }

    private fun alarm(context: Context) = context.getSystemService(AlarmManager::class.java)
    fun notificationId(noteId: Long, id: String): Int = (noteId.toString() + ":" + id).hashCode()

    private fun pending(context: Context, record: Record): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE)
            .setData(Uri.parse("memonote://reminder/" + record.noteId + "/" + record.id))
            .putExtra("noteId", record.noteId).putExtra("reminderId", record.id)
        return PendingIntent.getBroadcast(context, notificationId(record.noteId, record.id),
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun exactAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || alarm(context).canScheduleExactAlarms()

    fun notificationAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    fun channel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,
            AppLanguage.text(context, "筆記時間提醒"),
            NotificationManager.IMPORTANCE_HIGH).apply {
            description = AppLanguage.text(context, "在指定時間顯示筆記中的整行文字")
            enableVibration(true)
        })
    }

    private fun schedule(context: Context, record: Record) {
        val manager = alarm(context)
        val intent = pending(context, record)
        try {
            if (exactAllowed(context))
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, record.time, intent)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, record.time, intent)
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, record.time, intent)
        }
    }

    @Synchronized
    fun syncAll(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet(KEY, emptySet()).orEmpty().mapNotNull(::parse).toSet()
        val exact = exactAllowed(app)
        val permissionChanged = prefs.contains("exact") && prefs.getBoolean("exact", exact) != exact
        if (permissionChanged) previous.forEach { alarm(app).cancel(pending(app, it)) }
        val old = if (permissionChanged) emptySet() else previous
        val now = System.currentTimeMillis()
        val desired = NoteStore(app).use { store ->
            store.all().flatMap { note ->
                ReminderCodec.all(note).map { Record(note.id, it.id, it.timeMillis) }
            }
        }.filter { it.time > now || (it in old && it.time > now - 86_400_000L) }.toSet()
        old.filter { it !in desired }.forEach { alarm(app).cancel(pending(app, it)) }
        desired.filter { it.time > now && it !in old }.forEach { schedule(app, it) }
        check(prefs.edit().putStringSet(KEY, desired.map { it.persist() }.toSet())
            .putBoolean("exact", exact).commit())
        channel(app)
    }

    @Synchronized
    fun markDelivered(context: Context, noteId: Long, id: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val kept = prefs.getStringSet(KEY, emptySet()).orEmpty().mapNotNull(::parse)
            .filterNot { it.noteId == noteId && it.id == id }
            .map { it.persist() }.toSet()
        prefs.edit().putStringSet(KEY, kept).apply()
    }

    @Synchronized
    fun forceReschedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getStringSet(KEY, emptySet()).orEmpty().mapNotNull(::parse).forEach {
            alarm(context).cancel(pending(context, it))
        }
        check(prefs.edit().remove(KEY).commit())
        syncAll(context)
    }
    fun safeSync(context: Context) {
        runCatching { syncAll(context) }.onFailure {
            Log.e("LittleNotes", "Reminder scheduling failed", it)
        }
    }
}