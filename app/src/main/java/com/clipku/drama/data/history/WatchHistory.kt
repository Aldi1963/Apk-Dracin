package com.clipku.drama.data.history

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val contentId: String,
    val title: String,
    val posterUrl: String?,
    val providerName: String,
    val totalEp: Int,
    val lastEp: Int,
    val watchedAt: Long,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)

object WatchHistory {
    private const val PREFS = "clipku_history"
    private const val KEY = "items"
    private const val MAX = 50

    private val _items = MutableStateFlow<List<HistoryItem>>(emptyList())
    val items: StateFlow<List<HistoryItem>> = _items.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _items.value = load()
    }

    fun saveWatch(
        contentId: String,
        title: String,
        posterUrl: String?,
        providerName: String,
        totalEp: Int,
        ep: Int,
        positionMs: Long = 0,
        durationMs: Long = 0
    ) {
        val current = _items.value.toMutableList()
        // pertahankan posisi lama bila tidak ada update posisi
        val old = current.find { it.contentId == contentId && it.lastEp == ep }
        val pos = if (positionMs > 0) positionMs else old?.positionMs ?: 0
        val dur = if (durationMs > 0) durationMs else old?.durationMs ?: 0
        // satu entri per (contentId, episode) — jangan collapse agar status tonton akurat
        current.removeAll { it.contentId == contentId && it.lastEp == ep }
        current.add(0, HistoryItem(contentId, title, posterUrl, providerName, totalEp, ep, System.currentTimeMillis(), pos, dur))
        if (current.size > MAX) current.subList(MAX, current.size).clear()
        _items.value = current
        persist(current)
    }

    fun isWatched(contentId: String, ep: Int): Boolean =
        _items.value.any { it.contentId == contentId && it.lastEp >= ep }

    /**
     * Episode dianggap SELESAI bila pernah ditonton sampai habis
     * (posisi >= 90% durasi). Dipakai untuk tanda centang di daftar episode.
     */
    fun isEpisodeDone(contentId: String, ep: Int): Boolean {
        val it = _items.value.find { it.contentId == contentId && it.lastEp == ep } ?: return false
        return it.durationMs > 0 && it.positionMs >= (it.durationMs * 0.9).toLong()
    }

    fun watchedEpisodes(contentId: String): Set<Int> =
        _items.value.filter { it.contentId == contentId }.map { it.lastEp }.toSet()

    fun remove(contentId: String) {
        _items.value = _items.value.filter { it.contentId != contentId }
        persist(_items.value)
    }

    fun getPosition(contentId: String, ep: Int): Long =
        _items.value.find { it.contentId == contentId && it.lastEp == ep }?.positionMs ?: 0

    fun clear() {
        _items.value = emptyList()
        prefs?.edit()?.remove(KEY)?.apply()
    }

    /** Untuk backup: ambil semua item mentah. */
    fun exportItems(): List<HistoryItem> = _items.value

    /** Untuk restore: ganti seluruh isi dari backup. */
    fun replaceAll(items: List<HistoryItem>) {
        _items.value = items
        persist(items)
    }

    /** Parse satu item dari JSONObject backup (null bila corrupt). */
    fun parseItem(o: JSONObject): HistoryItem? = try {
        HistoryItem(
            o.getString("contentId"),
            o.getString("title"),
            o.optString("posterUrl").ifEmpty { null },
            o.optString("providerName"),
            o.optInt("totalEp", 1),
            o.optInt("lastEp", 1),
            o.optLong("watchedAt", 0),
            o.optLong("positionMs", 0),
            o.optLong("durationMs", 0)
        )
    } catch (_: Exception) { null }

    fun toJsonObject(it: HistoryItem): JSONObject = JSONObject()
        .put("contentId", it.contentId)
        .put("title", it.title)
        .put("posterUrl", it.posterUrl ?: "")
        .put("providerName", it.providerName)
        .put("totalEp", it.totalEp)
        .put("lastEp", it.lastEp)
        .put("watchedAt", it.watchedAt)
        .put("positionMs", it.positionMs)
        .put("durationMs", it.durationMs)

    private fun load(): List<HistoryItem> {
        val raw = prefs?.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            // item corrupt di-skip satu-satu, jangan hapus seluruh riwayat
            List(arr.length()) { i ->
                try {
                    val o = arr.getJSONObject(i)
                    HistoryItem(
                        o.getString("contentId"),
                        o.getString("title"),
                        o.optString("posterUrl").ifEmpty { null },
                        o.optString("providerName"),
                        o.optInt("totalEp", 1),
                        o.optInt("lastEp", 1),
                        o.optLong("watchedAt", 0),
                        o.optLong("positionMs", 0),
                        o.optLong("durationMs", 0)
                    )
                } catch (_: Exception) { null }
            }.filterNotNull()
        } catch (_: Exception) { emptyList() }
    }

    private fun persist(list: List<HistoryItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject()
                .put("contentId", it.contentId)
                .put("title", it.title)
                .put("posterUrl", it.posterUrl ?: "")
                .put("providerName", it.providerName)
                .put("totalEp", it.totalEp)
                .put("lastEp", it.lastEp)
                .put("watchedAt", it.watchedAt)
                .put("positionMs", it.positionMs)
                .put("durationMs", it.durationMs))
        }
        prefs?.edit()?.putString(KEY, arr.toString())?.apply()
    }
}
