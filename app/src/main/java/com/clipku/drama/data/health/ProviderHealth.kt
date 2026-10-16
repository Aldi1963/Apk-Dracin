package com.clipku.drama.data.health

import android.content.Context
import android.content.SharedPreferences
import com.clipku.drama.data.sonzaix.SonzaixRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Monitoring kesehatan provider otomatis.
 * - Probe endpoint browse tiap provider yang dipakai (maks 1x/jam, di background).
 * - Provider disembunyikan setelah 2x gagal beruntun, ditampilkan lagi saat probe sukses.
 * - Daftar sembunyi dipakai SonzaixRepository untuk memfilter provider di
 *   Eksplor, Beranda, dan pencarian.
 */
object ProviderHealth {
    private const val PREFS = "clipku_provider_health"
    private const val KEY_HIDDEN = "hidden_slugs"
    private const val KEY_FAILS = "fail_counts"
    private const val KEY_LAST_PROBE = "last_probe"
    private const val KEY_SEEDED = "seeded_dead"
    private const val FAIL_THRESHOLD = 2
    private const val PROBE_DEBOUNCE_MS = 60 * 60 * 1000L

    private var prefs: SharedPreferences? = null

    private val _hidden = MutableStateFlow<Set<String>>(emptySet())
    val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val p = prefs!!
        _hidden.value = p.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()
        // seed awal: provider yang terprobe mati upstream (13 Okt 2026)
        if (!p.getBoolean(KEY_SEEDED, false)) {
            val seeded = _hidden.value + setOf("reelshort", "netshort", "freereels", "dramabox")
            _hidden.value = seeded
            p.edit().putStringSet(KEY_HIDDEN, seeded).putBoolean(KEY_SEEDED, true).apply()
        }
    }

    fun isHidden(slug: String): Boolean = _hidden.value.contains(slug)

    private fun failCounts(): MutableMap<String, Int> {
        val raw = prefs?.getString(KEY_FAILS, "") ?: ""
        return raw.split(",").mapNotNull {
            val kv = it.split("=")
            if (kv.size == 2) kv[0] to (kv[1].toIntOrNull() ?: 0) else null
        }.toMap().toMutableMap()
    }

    private fun saveFailCounts(map: Map<String, Int>) {
        prefs?.edit()?.putString(KEY_FAILS, map.entries.joinToString(",") { "${it.key}=${it.value}" })?.apply()
    }

    /** Catat hasil probe satu provider. Aman dipanggil dari thread mana pun. */
    @Synchronized
    fun recordResult(slug: String, ok: Boolean) {
        val fails = failCounts()
        if (ok) {
            if (fails.remove(slug) != null) saveFailCounts(fails)
            if (_hidden.value.contains(slug)) {
                val next = _hidden.value - slug
                _hidden.value = next
                prefs?.edit()?.putStringSet(KEY_HIDDEN, next)?.apply()
            }
            return
        }
        val n = (fails[slug] ?: 0) + 1
        fails[slug] = n
        saveFailCounts(fails)
        if (n >= FAIL_THRESHOLD && !_hidden.value.contains(slug)) {
            val next = _hidden.value + slug
            _hidden.value = next
            prefs?.edit()?.putStringSet(KEY_HIDDEN, next)?.apply()
        }
    }

    /**
     * Probe semua provider aktif di background. Debounce: maks 1x/jam.
     * Tidak memblokir UI; dipanggil saat app start.
     */
    fun probeIfDue(scope: CoroutineScope) {
        val p = prefs ?: return
        val last = p.getLong(KEY_LAST_PROBE, 0)
        if (System.currentTimeMillis() - last < PROBE_DEBOUNCE_MS) return
        p.edit().putLong(KEY_LAST_PROBE, System.currentTimeMillis()).apply()
        scope.launch(Dispatchers.IO) {
            for (slug in SonzaixRepository.monitoredProviderSlugs()) {
                val ok = try {
                    withTimeoutOrNull(15000) { SonzaixRepository.probeProvider(slug) } == true
                } catch (_: Exception) { false }
                recordResult(slug, ok)
            }
        }
    }
}
