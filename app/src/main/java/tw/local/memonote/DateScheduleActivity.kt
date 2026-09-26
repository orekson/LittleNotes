package tw.local.memonote

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.ScrollView
import tw.local.memonote.data.Note
import tw.local.memonote.data.NoteStore
import tw.local.memonote.ui.AppLanguage
import tw.local.memonote.ui.BottomNavigation
import tw.local.memonote.ui.LocalizedActivity
import tw.local.memonote.ui.Ui
import tw.local.memonote.ui.localizedDisplayTitle
import tw.local.memonote.widget.DateWidgetSchedule
import tw.local.memonote.widget.NoteWidgetProvider
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DateScheduleActivity : LocalizedActivity() {
    private var selectedDate = LocalDate.now()
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onResume() {
        super.onResume()
        showPage()
    }

    private fun activeWidgets(): IntArray = AppWidgetManager.getInstance(this)
        .getAppWidgetIds(ComponentName(this, NoteWidgetProvider::class.java))

    private fun label(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd", AppLanguage.locale(this)))

    private fun showPage() {
        val ids = activeWidgets()
        if (widgetId !in ids) widgetId = ids.firstOrNull() ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val notes = runCatching { NoteStore(this).use { it.all() } }.getOrDefault(emptyList())
        val root = Ui.root(this)
        val scroll = ScrollView(this)
        val content = Ui.column(this)
        Ui.pad(content, 24)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(Ui.label(this, "日期", 30f, bold = true))
        content.addView(Ui.space(this, 8))
        content.addView(Ui.label(this,
            AppLanguage.format(this, "今天：%1\$s", label(LocalDate.now())),
            15f, Ui.muted))
        content.addView(Ui.label(this, "可選擇今天前後一年內的日期，指定已寫好的筆記。", 13f, Ui.muted))
        content.addView(Ui.space(this, 16))
        if (ids.isEmpty()) {
            content.addView(Ui.label(this, "先加入一個桌面小工具，再設定日期切換。", 16f))
            content.addView(Ui.button(this, "加入桌面小工具", true) {
                val manager = AppWidgetManager.getInstance(this)
                if (manager.isRequestPinAppWidgetSupported)
                    manager.requestPinAppWidget(ComponentName(this, NoteWidgetProvider::class.java),
                        null, null)
                else Ui.toast(this, "請長按桌面空白處，從小工具清單加入小小筆記。")
            })
            val selected = Ui.label(this,
                AppLanguage.format(this, "所選日期：%1\$s", label(selectedDate)), 16f)
            content.addView(CalendarView(this).apply {
                minDate = LocalDate.now().minusYears(1).atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                maxDate = LocalDate.now().plusYears(1).atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                date = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                setOnDateChangeListener { _, year, month, day ->
                    selectedDate = LocalDate.of(year, month + 1, day)
                    selected.text = AppLanguage.format(this@DateScheduleActivity,
                        "所選日期：%1\$s", label(selectedDate))
                    Ui.toast(this@DateScheduleActivity, "先加入一個桌面小工具，再設定日期切換。")
                }
            })
            content.addView(selected)
        } else {
            val index = ids.indexOf(widgetId)
            val current = notes.firstOrNull { it.id == NoteWidgetProvider.noteId(this, widgetId) }
            content.addView(Ui.button(this,
                AppLanguage.format(this, "桌面小工具 %1\$d：%2\$s", index + 1,
                    current?.localizedDisplayTitle(this) ?: AppLanguage.text(this, "尚未選擇筆記"))) {
                val labels = ids.mapIndexed { i, id ->
                    val title = notes.firstOrNull { it.id == NoteWidgetProvider.noteId(this, id) }
                        ?.localizedDisplayTitle(this) ?: AppLanguage.text(this, "尚未選擇筆記")
                    AppLanguage.format(this, "桌面小工具 %1\$d：%2\$s", i + 1, title)
                }.toTypedArray()
                AlertDialog.Builder(this).setTitle(AppLanguage.text(this, "選擇桌面小工具"))
                    .setItems(labels) { _, which -> widgetId = ids[which]; showPage() }
                    .show()
            })
            content.addView(Ui.space(this, 12))
            val calendar = CalendarView(this).apply {
                minDate = LocalDate.now().minusYears(1).atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                maxDate = LocalDate.now().plusYears(1).atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                date = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                setOnDateChangeListener { _, year, month, day ->
                    selectedDate = LocalDate.of(year, month + 1, day)
                    chooseNote(notes)
                }
            }
            content.addView(calendar)
            content.addView(Ui.space(this, 12))
            val assigned = DateWidgetSchedule.assignments(this, widgetId)[selectedDate]
            val note = notes.firstOrNull { it.id == assigned }
            content.addView(Ui.label(this,
                AppLanguage.format(this, "所選日期：%1\$s", label(selectedDate)), 16f, bold = true))
            content.addView(Ui.rawLabel(this,
                if (note == null) AppLanguage.text(this, "這一天尚未指定筆記")
                else AppLanguage.format(this, "已指定：%1\$s", note.localizedDisplayTitle(this)),
                14f, Ui.muted))
            content.addView(Ui.button(this, "選擇這一天的筆記", true) { chooseNote(notes) })
            if (assigned != null) content.addView(Ui.button(this, "移除這一天的安排") {
                DateWidgetSchedule.remove(this, widgetId, selectedDate)
                showPage()
            })
        }
        BottomNavigation.add(this, root, BottomNavigation.Tab.DATE, {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }, {}, {
            startActivity(Intent(this, CloudProfileActivity::class.java))
        })
    }

    private fun chooseNote(notes: List<Note>) {
        if (notes.isEmpty()) {
            Ui.toast(this, "還沒有筆記，先寫一篇吧。")
            return
        }
        val labels = notes.map { it.localizedDisplayTitle(this) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(AppLanguage.format(this, "選擇 %1\$s 要顯示的筆記", label(selectedDate)))
            .setItems(labels) { _, index ->
                runCatching {
                    DateWidgetSchedule.assign(this, widgetId, selectedDate, notes[index].id)
                }.onSuccess {
                    Ui.toast(this, "日期安排已儲存")
                    showPage()
                }.onFailure { Ui.toast(this, "日期安排儲存失敗，請重試") }
            }
            .setNegativeButton(AppLanguage.text(this, "取消"), null)
            .show()
    }
}