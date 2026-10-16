package com.clipku.drama.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
fun ClipkuDramaTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    // Sinkronkan status sistem ke ThemeState (untuk mode SYSTEM)
    LaunchedEffect(systemDark) {
        ThemeState.systemDark = systemDark
    }
    // Baca mode agar composable ini recompose saat mode berubah
    val mode = ThemePrefs.mode.collectAsState().value
    LaunchedEffect(mode) {
        ThemeState.mode = mode
    }
    val darkTheme = ThemeState.isDark

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = Coral,
            onPrimary = Color.White,
            secondary = CoralDark,
            background = BgDeep,
            onBackground = TextMain,
            surface = BgCard,
            onSurface = TextMain,
            surfaceVariant = BgCard2,
            onSurfaceVariant = TextMuted,
            error = Coral
        )
    } else {
        lightColorScheme(
            primary = Coral,
            onPrimary = Color.White,
            secondary = CoralDark,
            background = BgDeep,
            onBackground = TextMain,
            surface = BgCard,
            onSurface = TextMain,
            surfaceVariant = BgCard2,
            onSurfaceVariant = TextMuted,
            error = Coral
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
