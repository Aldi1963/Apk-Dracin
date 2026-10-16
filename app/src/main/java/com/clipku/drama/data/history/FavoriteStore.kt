package com.clipku.drama.data.history

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class FavoriteItem(
    val contentId: String,
    val title: String,
    val posterUrl: String?,
    val providerName: String,
    val totalEp: Int = 1,
    val addedAt: Long
)

object FavoriteStore {
    private const val PREFS = "clipku_favorites"
    private const val KEY = "items"

    private val _items = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val items: StateFlow<List<FavoriteItem>> = _items.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _items.value = load()
    }

    fun isFavorite(contentId: String): Boolean =
        _items.value.any { it.contentId == contentId }

    fun toggle(contentId: String, title: String, posterUrl: String?, providerName: String, totalEp: Int = 1): Boolean {
        val current = _items.value.toMutableList()
        val existing = current.find { it.contentId == contentId }
        val nowFav: Boolean
        if (existing != null) {
            current.remove(existing)
            nowFav = false
        } else {
            current.add(0, FavoriteItem(contentId, title, posterUrl, providerName, totalEp, System.currentTimeMillis()))
            nowFav = true
        }
        _items.value = current
        persist(current)
        return nowFav
    }

    fun remove(contentId: String) {
        _items.value = _items.value.filter { it.contentId != contentId }
        persist(_items.value)
    }

    /** Update jumlah episode tersimpan (dipakai worker notifikasi episode baru). */
    fun updateTotalEp(contentId: String, totalEp: Int) {
        _items.value = _items.value.map {
            if (it.contentId == contentId) it.copy(totalEp = totalEp) else it
        }
        persist(_items.value)
    }

    /** Untuk backup: ambil semua item mentah. */
    fun exportItems(): List<FavoriteItem> = _items.value

    /** Untuk restore: ganti seluruh isi dari backup. */
    fun replaceAll(items: List<FavoriteItem>) {
        _items.value = items
        persist(items)
    }

    /** Parse satu item dari JSONObject backup (null bila corrupt). */
    fun parseItem(o: JSONObject): FavoriteItem? = try {
        FavoriteItem(
            o.getString("contentId"),
            o.getString("title"),
            o.optString("posterUrl").ifEmpty { null },
            o.optString("providerName"),
            o.optInt("totalEp", 1),
            o.optLong("addedAt", 0)
        )
    } catch (_: Exception) { null }

    fun toJsonObject(it: FavoriteItem): JSONObject = JSONObject()
        .put("contentId", it.contentId)
        .put("title", it.title)
        .put("posterUrl", it.posterUrl ?: "")
        .put("providerName", it.providerName)
        .put("totalEp", it.totalEp)
        .put("addedAt", it.addedAt)

    private fun load(): List<FavoriteItem> {
        val raw = prefs?.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                try {
                    val o = arr.getJSONObject(i)
                    FavoriteItem(
                        o.getString("contentId"),
                        o.getString("title"),
                        o.optString("posterUrl").ifEmpty { null },
                        o.optString("providerName"),
                        o.optInt("totalEp", 1),
                        o.optLong("addedAt", 0)
                    )
                } catch (_: Exception) { null }
            }.filterNotNull()
        } catch (_: Exception) { emptyList() }
    }

    private fun persist(list: List<FavoriteItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject()
                .put("contentId", it.contentId)
                .put("title", it.title)
                .put("posterUrl", it.posterUrl ?: "")
                .put("providerName", it.providerName)
                .put("totalEp", it.totalEp)
                .put("addedAt", it.addedAt))
        }
        prefs?.edit()?.putString(KEY, arr.toString())?.apply()
    }
}
