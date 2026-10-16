package com.clipku.drama.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Global theme state. Dibaca oleh getter warna di Color.kt;
 * karena berupa MutableState, Compose otomatis recompose saat berubah.
 */
object ThemeState {
    var mode by mutableStateOf(ThemeMode.DARK)
    var systemDark by mutableStateOf(false)

    val isDark: Boolean
        get() = when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemDark
        }
}
