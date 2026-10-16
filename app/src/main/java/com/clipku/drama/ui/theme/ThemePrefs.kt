package com.clipku.drama.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    DARK, LIGHT, SYSTEM;
    val label: String
        get() = when (this) {
            DARK -> Str.themeDark
            LIGHT -> Str.themeLight
            SYSTEM -> Str.themeSystem
        }
}

object ThemePrefs {
    private const val PREFS = "clipku_theme"
    private const val KEY = "mode"

    private val _mode = MutableStateFlow(ThemeMode.DARK)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs?.getString(KEY, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        _mode.value = try { ThemeMode.valueOf(saved) } catch (_: Exception) { ThemeMode.DARK }
    }

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        prefs?.edit()?.putString(KEY, mode.name)?.apply()
    }
}
