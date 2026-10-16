package com.clipku.drama.data.history

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup & restore riwayat tonton, favorit, dan riwayat pencarian ke file JSON.
 * Format: { "version": 1, "exportedAt": ..., "history": [...], "favorites": [...], "searches": [...] }
 */
object BackupManager {

    fun defaultFileName(): String {
        val ts = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        return "clipku-backup-$ts.json"
    }

    fun exportJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "clipku-plus")
        root.put("exportedAt", System.currentTimeMillis())

        val hArr = JSONArray()
        WatchHistory.exportItems().forEach { hArr.put(WatchHistory.toJsonObject(it)) }
        root.put("history", hArr)

        val fArr = JSONArray()
        FavoriteStore.exportItems().forEach { fArr.put(FavoriteStore.toJsonObject(it)) }
        root.put("favorites", fArr)

        val sArr = JSONArray()
        SearchHistory.exportItems().forEach { sArr.put(it) }
        root.put("searches", sArr)

        return root.toString(2)
    }

    data class RestoreResult(
        val history: Int,
        val favorites: Int,
        val searches: Int,
        val skipped: Int
    )

    /**
     * Restore dari JSON backup. Mode REPLACE: seluruh isi diganti data backup.
     * Item corrupt di-skip satu-satu, tidak menggagalkan seluruh restore.
     */
    fun importJson(json: String): RestoreResult {
        val root = JSONObject(json)
        var skipped = 0

        val history = try {
            val arr = root.optJSONArray("history") ?: JSONArray()
            List(arr.length()) { i ->
                try {
                    WatchHistory.parseItem(arr.getJSONObject(i))
                } catch (_: Exception) {
                    skipped++
                    null
                }
            }.filterNotNull()
        } catch (_: Exception) { emptyList() }

        val favorites = try {
            val arr = root.optJSONArray("favorites") ?: JSONArray()
            List(arr.length()) { i ->
                try {
                    FavoriteStore.parseItem(arr.getJSONObject(i))
                } catch (_: Exception) {
                    skipped++
                    null
                }
            }.filterNotNull()
        } catch (_: Exception) { emptyList() }

        val searches = try {
            val arr = root.optJSONArray("searches") ?: JSONArray()
            List(arr.length()) { i ->
                try { arr.getString(i) } catch (_: Exception) { skipped++; null }
            }.filterNotNull()
        } catch (_: Exception) { emptyList() }

        WatchHistory.replaceAll(history)
        FavoriteStore.replaceAll(favorites)
        SearchHistory.replaceAll(searches)

        return RestoreResult(history.size, favorites.size, searches.size, skipped)
    }
}
