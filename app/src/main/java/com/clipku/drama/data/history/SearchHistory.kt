package com.clipku.drama.data.history

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

object SearchHistory {
    private const val PREFS = "clipku_search"
    private const val KEY = "queries"
    private const val MAX = 10

    private val _queries = MutableStateFlow<List<String>>(emptyList())
    val queries: StateFlow<List<String>> = _queries.asStateFlow()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _queries.value = load()
    }

    fun add(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        val list = (_queries.value.toMutableList().apply {
            remove(q); add(0, q)
            if (size > MAX) subList(MAX, size).clear()
        })
        _queries.value = list
        persist(list)
    }

    fun remove(query: String) {
        _queries.value = _queries.value - query
        persist(_queries.value)
    }

    /** Untuk backup. */
    fun exportItems(): List<String> = _queries.value

    /** Untuk restore. */
    fun replaceAll(items: List<String>) {
        _queries.value = items
        persist(items)
    }

    fun clear() {
        _queries.value = emptyList()
        prefs?.edit()?.remove(KEY)?.apply()
    }

    private fun load(): List<String> {
        val p = prefs ?: return emptyList()
        // format baru: JSONArray berurutan
        try {
            p.getString(KEY, null)?.let { raw ->
                val arr = JSONArray(raw)
                return List(arr.length()) { arr.getString(it) }
            }
        } catch (_: Exception) {}
        // migrasi format lama: StringSet (sekali, lalu ditimpa format baru)
        return try {
            p.getStringSet(KEY, null)?.toList() ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun persist(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs?.edit()?.putString(KEY, arr.toString())?.apply()
    }
}
