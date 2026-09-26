package tw.local.memonote.ui

import android.app.Activity
import android.widget.LinearLayout

object BottomNavigation {
    fun add(
        activity: Activity,
        root: LinearLayout,
        profileSelected: Boolean,
        notes: () -> Unit,
        profile: () -> Unit
    ) {
        val bar = Ui.row(activity).apply {
            setPadding(Ui.dp(activity, 16), Ui.dp(activity, 8),
                Ui.dp(activity, 16), Ui.dp(activity, 8))
            setBackgroundColor(0xffffffff.toInt())
            elevation = Ui.dp(activity, 6).toFloat()
        }
        bar.addView(Ui.button(activity, "✦  筆記", !profileSelected, notes).apply {
            contentDescription = "筆記頁"
        }, LinearLayout.LayoutParams(0, Ui.dp(activity, 48), 1f))
        bar.addView(Ui.button(activity, "☺  個人", profileSelected, profile).apply {
            contentDescription = "個人與設定頁"
        }, LinearLayout.LayoutParams(0, Ui.dp(activity, 48), 1f))
        root.addView(bar)
    }
}