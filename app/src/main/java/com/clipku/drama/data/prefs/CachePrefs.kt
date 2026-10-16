package com.clipku.drama.data.prefs

import android.content.Context

/**
 * Lacak kapan terakhir cache gambar dibersihkan (untuk auto-clear terjadwal).
 */
object CachePrefs {
    private const val PREFS = "clipku_cache"
    private const val KEY_LAST_CLEARED = "last_cleared"

    fun lastCleared(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_CLEARED, 0L)

    fun markCleared(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_CLEARED, System.currentTimeMillis()).apply()
    }
}
