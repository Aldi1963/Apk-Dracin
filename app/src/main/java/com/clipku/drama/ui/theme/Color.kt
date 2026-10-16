package com.clipku.drama.ui.theme

import androidx.compose.ui.graphics.Color

// Palet warna CLIPKU+ — dark & light.
// Getter membaca ThemeState.isDark (MutableState) sehingga Compose
// otomatis recompose saat tema berubah. Bukan @Composable getter,
// jadi aman dipakai di konteks non-composable juga.

val BgDeep: Color get() = if (ThemeState.isDark) Color(0xFF0B0E1A) else Color(0xFFF4F6FB)
val BgCard: Color get() = if (ThemeState.isDark) Color(0xFF141A2E) else Color(0xFFFFFFFF)
val BgCard2: Color get() = if (ThemeState.isDark) Color(0xFF1B2340) else Color(0xFFEDF0F7)
val BgChip: Color get() = if (ThemeState.isDark) Color(0xFF1E2745) else Color(0xFFE4E9F4)
val Coral: Color get() = Color(0xFFFF4D67)
val CoralDark: Color get() = Color(0xFFE63E57)
val TextMain: Color get() = if (ThemeState.isDark) Color(0xFFF5F6FA) else Color(0xFF141A2E)
val TextMuted: Color get() = if (ThemeState.isDark) Color(0xFF8B93B0) else Color(0xFF6B7390)
val StarYellow: Color get() = if (ThemeState.isDark) Color(0xFFFFC531) else Color(0xFFE8A100)
val Divider: Color get() = if (ThemeState.isDark) Color(0xFF232C4E) else Color(0xFFDFE4F0)

// legacy aliases
val Navy = Color(0xFF243370)
val NavyLight = Color(0xFF3A4A8C)
val Background: Color get() = BgDeep
val Surface: Color get() = BgCard
val SurfaceVariant: Color get() = BgCard2
val OnBackground: Color get() = TextMain
val OnSurfaceMuted: Color get() = TextMuted
val AccentRed: Color get() = Coral
