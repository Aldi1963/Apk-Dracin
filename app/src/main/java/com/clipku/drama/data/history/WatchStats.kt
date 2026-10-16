package com.clipku.drama.data.history

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Statistik nonton: total detik ditonton + jumlah episode, per hari.
 * Disimpan sebagai "detik,episode" per kunci tanggal yyyy-MM-dd.
 */
data class WatchStatsData(
    val seconds: Long = 0,
    val episodes: Int = 0
)

object WatchStats {
    private const val PREFS = "clipku_watch_stats"
    private const val KEY_PREFIX = "d_"

    private var prefs: SharedPreferences? = null
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _bump = MutableStateFlow(0)
    /** Berubah setiap ada pencatatan baru — untuk refresh UI. */
    val bump: StateFlow<Int> = _bump.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun todayKey(): String = KEY_PREFIX + dateFmt.format(Date())

    private fun readDay(key: String): WatchStatsData {
        val raw = prefs?.getString(key, null) ?: return WatchStatsData()
        val parts = raw.split(",")
        return WatchStatsData(
            parts.getOrNull(0)?.toLongOrNull() ?: 0,
            parts.getOrNull(1)?.toIntOrNull() ?: 0
        )
    }

    /** Catat detik nonton (dipanggil berkala dari player). */
    fun recordWatch(seconds: Long) {
        if (seconds <= 0) return
        val key = todayKey()
        val cur = readDay(key)
        prefs?.edit()?.putString(key, "${cur.seconds + seconds},${cur.episodes}")?.apply()
        _bump.value = _bump.value + 1
    }

    /** Catat satu episode selesai ditonton. */
    fun recordEpisodeDone() {
        val key = todayKey()
        val cur = readDay(key)
        prefs?.edit()?.putString(key, "${cur.seconds},${cur.episodes + 1}")?.apply()
        _bump.value = _bump.value + 1
    }

    /** Agregat N hari terakhir (termasuk hari ini). */
    fun lastDays(days: Int): WatchStatsData {
        var s = 0L
        var e = 0
        val cal = java.util.Calendar.getInstance()
        repeat(days) {
            val key = KEY_PREFIX + dateFmt.format(cal.time)
            val d = readDay(key)
            s += d.seconds
            e += d.episodes
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return WatchStatsData(s, e)
    }

    fun formatDuration(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        return if (h > 0) "${h} jam ${m} mnt" else "${m} mnt"
    }
}
