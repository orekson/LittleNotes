package tw.local.memonote.ui

import android.app.Activity
import android.app.AlertDialog
import android.widget.*
import tw.local.memonote.data.ImageFiles

object StickerPicker {
    fun show(activity: Activity,onPick: (String)->Unit,onImport: ()->Unit) {
        val column=Ui.column(activity); Ui.pad(column,16)
        column.addView(Ui.label(activity,"小小的陪伴，放進你的日常",14f,Ui.muted))
        val grid=GridLayout(activity).apply { columnCount=4 }
        val dialog=AlertDialog.Builder(activity).setTitle("hololive 小人貼圖").setView(column).setNegativeButton("取消",null).setNeutralButton("匯入自己的貼圖") { _,_-> onImport() }.create()
        val files=(activity.assets.list("stickers") ?: emptyArray()).filter { it.endsWith(".png") }.sortedBy { it.removePrefix("holo_").removeSuffix(".png").toIntOrNull() ?: 0 }
        for((i,file) in files.withIndex()) {
            val ref="asset:stickers/$file"
            val image=ImageButton(activity).apply {
                setImageBitmap(ImageFiles.load(activity,ref)); scaleType=ImageView.ScaleType.FIT_CENTER
                background=Ui.rounded(0xfff2ebfa.toInt(),16f); contentDescription="小人貼圖 ${i+1}"
                setPadding(4,4,4,4)
                setOnClickListener { onPick(ref); dialog.dismiss() }
            }
            grid.addView(image,GridLayout.LayoutParams().apply { width=Ui.dp(activity,64); height=Ui.dp(activity,80); setMargins(3,6,3,6) })
        }
        column.addView(ScrollView(activity).apply { addView(grid) },LinearLayout.LayoutParams(-1,Ui.dp(activity,320))); column.addView(Ui.space(activity,8)); column.addView(Ui.label(activity,"${files.size} 張靜態貼圖 · 可隨文字刪除",12f,Ui.muted))
        dialog.show()
    }
}
