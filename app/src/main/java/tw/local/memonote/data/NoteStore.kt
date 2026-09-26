package tw.local.memonote.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import tw.local.memonote.cloud.CloudBackupJob

class NoteStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "notes.db", null, 2) {
    private val appContext = context.applicationContext
    private fun changed() { runCatching { CloudBackupJob.schedule(appContext) } }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, " +
                "formatting TEXT NOT NULL, background TEXT NOT NULL, fade INTEGER NOT NULL, " +
                "updated INTEGER NOT NULL, category TEXT NOT NULL DEFAULT '', sealed TEXT NOT NULL DEFAULT '')"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE notes ADD COLUMN sealed TEXT NOT NULL DEFAULT ''")
        }
    }

    fun all(): List<Note> = read(null, null)
    fun find(id: Long): Note? = read("id=?", arrayOf(id.toString())).firstOrNull()

    private fun read(where: String?, args: Array<String>?): List<Note> =
        readableDatabase.query("notes", null, where, args, null, null, "updated DESC, id DESC").use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Note(
                            id = c.getLong(c.getColumnIndexOrThrow("id")),
                            title = c.getString(c.getColumnIndexOrThrow("title")),
                            body = c.getString(c.getColumnIndexOrThrow("body")),
                            formatting = c.getString(c.getColumnIndexOrThrow("formatting")),
                            background = c.getString(c.getColumnIndexOrThrow("background")),
                            fade = c.getInt(c.getColumnIndexOrThrow("fade")),
                            updated = c.getLong(c.getColumnIndexOrThrow("updated")),
                            category = c.getString(c.getColumnIndexOrThrow("category")),
                            sealed = c.getString(c.getColumnIndexOrThrow("sealed"))
                        )
                    )
                }
            }
        }

    private fun values(note: Note, updated: Long): ContentValues = ContentValues().apply {
        val locked = note.isLocked
        put("title", if (locked) "" else note.title)
        put("body", if (locked) "" else note.body)
        put("formatting", if (locked) "{}" else note.formatting)
        put("background", if (locked) "paper" else note.background)
        put("fade", if (locked) 35 else note.fade.coerceIn(0, 100))
        put("updated", updated)
        put("category", if (locked) "" else note.category)
        put("sealed", note.sealed)
    }

    fun save(note: Note): Long {
        if (note.id != 0L && find(note.id)?.isLocked == true && !note.isLocked) {
            error("加密筆記必須以密文儲存")
        }
        val v = values(note, System.currentTimeMillis())
        val savedId = if (note.id == 0L) {
            writableDatabase.insertOrThrow("notes", null, v)
        } else {
            check(writableDatabase.update("notes", v, "id=?", arrayOf(note.id.toString())) == 1) {
                "筆記已不存在"
            }
            note.id
        }
        changed()
        return savedId
    }

    fun saveLocked(noteId: Long, sealed: String) {
        require(sealed.isNotBlank())
        val current = find(noteId) ?: error("筆記已不存在")
        check(writableDatabase.update(
            "notes",
            values(current.copy(sealed = sealed), System.currentTimeMillis()),
            "id=?",
            arrayOf(noteId.toString())
        ) == 1)
        changed()
    }

    fun removeLock(note: Note) {
        require(note.id > 0 && !note.isLocked)
        check(find(note.id)?.isLocked == true) { "加密筆記已不存在" }
        check(writableDatabase.update(
            "notes", values(note, System.currentTimeMillis()),
            "id=?", arrayOf(note.id.toString())
        ) == 1)
        changed()
    }

    fun insertImported(notes: List<Note>): Int {
        val db = writableDatabase
        db.beginTransaction()
        try {
            notes.forEach { db.insertOrThrow("notes", null, values(it, it.updated)) }
            db.setTransactionSuccessful()
            changed()
            return notes.size
        } finally {
            db.endTransaction()
        }
    }

    fun delete(id: Long) {
        writableDatabase.delete("notes", "id=?", arrayOf(id.toString()))
        changed()
    }

    fun toggleCheck(noteId: Long, checkId: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val note = find(noteId) ?: return false
            if (note.isLocked) return false
            val changed = CheckState.toggle(note, checkId) ?: return false
            save(changed)
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun saveFromEditor(original: Note, draft: Note): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val latest = find(draft.id)
            check(latest?.isLocked != true) { "請先解鎖筆記" }
            val id = save(if (latest != null) CheckState.merge(original, draft, latest) else draft)
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }
}
