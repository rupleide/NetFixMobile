package com.rupleide.netfix.core.help

import android.content.Context
import android.content.SharedPreferences
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.dpibypass.getPreferences
import java.io.File

object HelpRepairStore {
    private const val PREFS_NAME = "netfix_help_repair"
    private const val SLOT_BEFORE = "before"
    private const val KEY_TAKEN_AT = "before_taken_at"
    private const val KEY_SYMPTOM = "before_symptom"
    private const val KEY_ABSENT_FILES = "before_absent_files"
    private const val KEY_JOURNAL = "journal"
    private const val BACKUP_DIR = "help_backup"
    private const val KEEP_MS = 7L * 24 * 60 * 60 * 1000
    private const val MAX_JOURNAL_LINES = 60

    private fun store(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun takenAt(context: Context): Long? {
        val store = store(context)
        val takenAt = store.getLong(KEY_TAKEN_AT, 0L)
        if (takenAt <= 0L || HelpSnapshot.load(store, SLOT_BEFORE) == null) return null
        if (System.currentTimeMillis() - takenAt > KEEP_MS) {
            clear(context)
            return null
        }
        return takenAt
    }

    fun hasBefore(context: Context): Boolean = takenAt(context) != null

    fun beginIfNeeded(context: Context, symptom: HelpSymptom) {
        if (hasBefore(context)) return
        val app = context.applicationContext
        val store = store(app)
        HelpSnapshot.save(store, SLOT_BEFORE, HelpSnapshot.capture(app.getPreferences()))
        File(app.filesDir, BACKUP_DIR).deleteRecursively()
        store.edit()
            .putLong(KEY_TAKEN_AT, System.currentTimeMillis())
            .putString(KEY_SYMPTOM, symptom.title)
            .remove(KEY_ABSENT_FILES)
            .putString(KEY_JOURNAL, "")
            .apply()
    }

    fun restoreBefore(context: Context): Set<String> {
        val app = context.applicationContext
        val store = store(app)
        val snapshot = HelpSnapshot.load(store, SLOT_BEFORE) ?: return emptySet()
        val prefs = app.getPreferences()
        val changed = HelpSnapshot.changedKeys(prefs, snapshot)
        HelpSnapshot.restore(prefs, snapshot)
        StrategyTestManager.reloadSavedState(app)
        clear(app)
        return changed
    }

    fun clear(context: Context) {
        val app = context.applicationContext
        val store = store(app)
        HelpSnapshot.clear(store, SLOT_BEFORE)
        store.edit()
            .remove(KEY_TAKEN_AT)
            .remove(KEY_SYMPTOM)
            .remove(KEY_ABSENT_FILES)
            .remove(KEY_JOURNAL)
            .apply()
        File(app.filesDir, BACKUP_DIR).deleteRecursively()
    }

    fun appendJournal(context: Context, line: String) {
        val store = store(context)
        val lines = journal(context) + line
        store.edit().putString(KEY_JOURNAL, lines.takeLast(MAX_JOURNAL_LINES).joinToString("\n")).apply()
    }

    fun journal(context: Context): List<String> =
        store(context).getString(KEY_JOURNAL, "").orEmpty().lines().filter { it.isNotBlank() }
}
