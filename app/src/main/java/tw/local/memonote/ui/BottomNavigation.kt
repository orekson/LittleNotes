package tw.local.memonote.ui

import android.app.Activity
import android.widget.LinearLayout

object BottomNavigation {
    enum class Tab { NOTES, DATE, PROFILE }

    fun add(activity: Activity, root: LinearLayout, selected: Tab,
            notes: () -> Unit, date: () -> Unit, profile: () -> Unit) {
        val bar = Ui.row(activity).apply {
            setPadding(Ui.dp(activity, 12), Ui.dp(activity, 8),
                Ui.dp(activity, 12), Ui.dp(activity, 8))
            setBackgroundColor(0xffffffff.toInt())
            elevation = Ui.dp(activity, 6).toFloat()
        }
        listOf(
            Triple("✦  筆記", Tab.NOTES, notes),
            Triple("▦  日期", Tab.DATE, date),
            Triple("☺  個人", Tab.PROFILE, profile)
        ).forEach { (label, tab, action) ->
            bar.addView(Ui.button(activity, label, selected == tab, action).apply {
                contentDescription = AppLanguage.text(activity, when (tab) {
                    Tab.NOTES -> "筆記頁"
                    Tab.DATE -> "日期頁"
                    Tab.PROFILE -> "個人與設定頁"
                })
            }, LinearLayout.LayoutParams(0, Ui.dp(activity, 48), 1f))
        }
        root.addView(bar)
    }
}