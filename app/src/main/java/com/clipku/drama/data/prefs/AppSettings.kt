package com.clipku.drama.data.prefs

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pengaturan aplikasi (SharedPreferences).
 * - Hemat kuota, auto-putar episode berikutnya, durasi double-tap,
 *   provider disembunyikan manual, bahasa, kerapatan grid,
 *   auto-hapus cache gambar, skala font UI.
 */
object AppSettings {
    private const val PREFS = "clipku_settings"
    private const val KEY_DATA_SAVER = "data_saver"
    private const val KEY_AUTO_NEXT = "auto_next"
    private const val KEY_DOUBLE_TAP_SECS = "double_tap_secs"
    private const val KEY_HIDDEN_PROVIDERS = "hidden_providers_manual"
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_GRID_COLUMNS = "grid_columns"
    private const val KEY_AUTO_CLEAR_CACHE = "auto_clear_cache_days"
    private const val KEY_UI_FONT_SCALE = "ui_font_scale"

    private var prefs: SharedPreferences? = null

    private val _dataSaver = MutableStateFlow(false)
    val dataSaver: StateFlow<Boolean> = _dataSaver.asStateFlow()

    private val _autoNext = MutableStateFlow(true)
    val autoNext: StateFlow<Boolean> = _autoNext.asStateFlow()

    private val _doubleTapSecs = MutableStateFlow(10)
    val doubleTapSecs: StateFlow<Int> = _doubleTapSecs.asStateFlow()

    private val _hiddenProvidersManual = MutableStateFlow<Set<String>>(emptySet())
    val hiddenProvidersManual: StateFlow<Set<String>> = _hiddenProvidersManual.asStateFlow()

    private val _appLanguage = MutableStateFlow("id")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _gridColumns = MutableStateFlow(3)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    /** 0 = mati, 7 / 30 hari. */
    private val _autoClearCacheDays = MutableStateFlow(0)
    val autoClearCacheDays: StateFlow<Int> = _autoClearCacheDays.asStateFlow()

    /** 0.9 = kecil, 1.0 = normal, 1.15 = besar. */
    private val _uiFontScale = MutableStateFlow(1.0f)
    val uiFontScale: StateFlow<Float> = _uiFontScale.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val p = prefs!!
        _dataSaver.value = p.getBoolean(KEY_DATA_SAVER, false)
        _autoNext.value = p.getBoolean(KEY_AUTO_NEXT, true)
        _doubleTapSecs.value = p.getInt(KEY_DOUBLE_TAP_SECS, 10)
        _hiddenProvidersManual.value = p.getStringSet(KEY_HIDDEN_PROVIDERS, emptySet())?.toSet() ?: emptySet()
        _appLanguage.value = p.getString(KEY_LANGUAGE, "id") ?: "id"
        _gridColumns.value = p.getInt(KEY_GRID_COLUMNS, 3).coerceIn(2, 3)
        _autoClearCacheDays.value = p.getInt(KEY_AUTO_CLEAR_CACHE, 0)
        _uiFontScale.value = p.getFloat(KEY_UI_FONT_SCALE, 1.0f)
    }

    fun setDataSaver(enabled: Boolean) {
        _dataSaver.value = enabled
        prefs?.edit()?.putBoolean(KEY_DATA_SAVER, enabled)?.apply()
    }

    fun setAutoNext(enabled: Boolean) {
        _autoNext.value = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_NEXT, enabled)?.apply()
    }

    fun setDoubleTapSecs(secs: Int) {
        val v = secs.coerceIn(5, 30)
        _doubleTapSecs.value = v
        prefs?.edit()?.putInt(KEY_DOUBLE_TAP_SECS, v)?.apply()
    }

    fun setProviderHiddenManual(slug: String, hidden: Boolean) {
        val next = _hiddenProvidersManual.value.toMutableSet()
        if (hidden) next.add(slug) else next.remove(slug)
        _hiddenProvidersManual.value = next
        prefs?.edit()?.putStringSet(KEY_HIDDEN_PROVIDERS, next)?.apply()
    }

    fun isProviderHiddenManual(slug: String): Boolean =
        _hiddenProvidersManual.value.contains(slug)

    fun setAppLanguage(lang: String) {
        val v = if (lang == "en") "en" else "id"
        _appLanguage.value = v
        prefs?.edit()?.putString(KEY_LANGUAGE, v)?.apply()
    }

    fun setGridColumns(cols: Int) {
        val v = cols.coerceIn(2, 3)
        _gridColumns.value = v
        prefs?.edit()?.putInt(KEY_GRID_COLUMNS, v)?.apply()
    }

    fun setAutoClearCacheDays(days: Int) {
        val v = if (days == 7 || days == 30) days else 0
        _autoClearCacheDays.value = v
        prefs?.edit()?.putInt(KEY_AUTO_CLEAR_CACHE, v)?.apply()
    }

    fun setUiFontScale(scale: Float) {
        val v = when {
            scale <= 0.95f -> 0.9f
            scale >= 1.1f -> 1.15f
            else -> 1.0f
        }
        _uiFontScale.value = v
        prefs?.edit()?.putFloat(KEY_UI_FONT_SCALE, v)?.apply()
    }

    /** true bila jaringan aktif adalah seluler (bukan WiFi). */
    fun isCellular(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) { false }
    }
}
