package com.clipku.drama.ui.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import com.clipku.drama.data.health.ProviderHealth
import com.clipku.drama.data.prefs.AppSettings
import com.clipku.drama.data.sonzaix.SonzaixProviders
import com.clipku.drama.ui.theme.BgCard
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.Str
import com.clipku.drama.ui.theme.TextMain
import com.clipku.drama.ui.theme.TextMuted
import com.clipku.drama.ui.theme.ThemeMode
import com.clipku.drama.ui.theme.ThemePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(com.clipku.drama.ui.theme.BgDeep)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = Str.back, tint = TextMain)
            }
            Text(Str.settings, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ===== Tampilan =====
            item {
                SettingsSection(Str.theme) {
                    val mode by ThemePrefs.mode.collectAsState()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.values().forEach { m ->
                            val sel = m == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (sel) Coral else BgCard)
                                    .clickable { ThemePrefs.setMode(m) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    m.label,
                                    color = if (sel) Color.White else TextMain,
                                    fontSize = 13.sp,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Bahasa
                    val lang by AppSettings.appLanguage.collectAsState()
                    SettingsRow(Str.language, null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("id" to "Indonesia", "en" to "English").forEach { (code, label) ->
                                val sel = lang == code
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (sel) Coral else BgCard)
                                        .clickable {
                                            AppSettings.setAppLanguage(code)
                                            (context as? Activity)?.recreate()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        label,
                                        color = if (sel) Color.White else TextMain,
                                        fontSize = 13.sp,
                                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    // Kerapatan grid
                    val cols by AppSettings.gridColumns.collectAsState()
                    SettingsRow(Str.gridDensity, null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2, 3).forEach { c ->
                                val sel = cols == c
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (sel) Coral else BgCard)
                                        .clickable { AppSettings.setGridColumns(c) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "$c",
                                        color = if (sel) Color.White else TextMain,
                                        fontSize = 13.sp,
                                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    // Ukuran font UI
                    val fs by AppSettings.uiFontScale.collectAsState()
                    SettingsRow(Str.uiFontSize, null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.9f to Str.small, 1.0f to Str.normal, 1.15f to Str.large).forEach { (v, label) ->
                                val sel = fs == v
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (sel) Coral else BgCard)
                                        .clickable {
                                            AppSettings.setUiFontScale(v)
                                            (context as? Activity)?.recreate()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        label,
                                        color = if (sel) Color.White else TextMain,
                                        fontSize = 13.sp,
                                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // ===== Putar =====
            item {
                val autoNext by AppSettings.autoNext.collectAsState()
                val dtSecs by AppSettings.doubleTapSecs.collectAsState()
                SettingsSection(Str.play) {
                    SettingsToggleRow(
                        title = Str.autoNextEp,
                        desc = Str.autoNextEpDesc,
                        checked = autoNext,
                        onChecked = { AppSettings.setAutoNext(it) }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(Str.doubleTapDuration, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 30).forEach { s ->
                            val sel = dtSecs == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (sel) Coral else BgCard)
                                    .clickable { AppSettings.setDoubleTapSecs(s) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    Str.seekSec(s).trimStart('+'),
                                    color = if (sel) Color.White else TextMain,
                                    fontSize = 13.sp,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
            // ===== Jaringan =====
            item {
                val saver by AppSettings.dataSaver.collectAsState()
                SettingsSection(Str.dataSaver) {
                    SettingsToggleRow(
                        title = Str.dataSaver,
                        desc = Str.dataSaverDesc,
                        checked = saver,
                        onChecked = { AppSettings.setDataSaver(it) }
                    )
                }
            }
            // ===== Provider =====
            item {
                val manualHidden by AppSettings.hiddenProvidersManual.collectAsState()
                val autoHidden by ProviderHealth.hidden.collectAsState()
                SettingsSection(Str.providers) {
                    Text(
                        Str.hideDeadProviderNote,
                        color = TextMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SonzaixProviders.all.forEach { cfg ->
                        val autoOff = autoHidden.contains(cfg.slug)
                        val manualOff = manualHidden.contains(cfg.slug)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !autoOff) {
                                    AppSettings.setProviderHiddenManual(cfg.slug, !manualOff)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    cfg.name,
                                    color = if (autoOff || manualOff) TextMuted else TextMain,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (autoOff) {
                                    Text(
                                        if (AppSettings.appLanguage.value == "en") "Auto-hidden (unhealthy)" else "Disembunyikan otomatis (tidak sehat)",
                                        color = Coral, fontSize = 11.sp
                                    )
                                }
                            }
                            Switch(
                                checked = !manualOff && !autoOff,
                                enabled = !autoOff,
                                onCheckedChange = { checked ->
                                    AppSettings.setProviderHiddenManual(cfg.slug, !checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Coral
                                )
                            )
                        }
                    }
                }
            }
            // ===== Penyimpanan =====
            item {
                val autoDays by AppSettings.autoClearCacheDays.collectAsState()
                var clearing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                SettingsSection(Str.clearImageCache) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Str.clearImageCache, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(BgCard)
                                .clickable {
                                    if (!clearing) {
                                        clearing = true
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                context.imageLoader.diskCache?.clear()
                                                context.imageLoader.memoryCache?.clear()
                                                com.clipku.drama.data.prefs.CachePrefs.markCleared(context)
                                            } catch (_: Exception) {}
                                            withContext(Dispatchers.Main) { clearing = false }
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                if (clearing) "…" else Str.delete,
                                color = Coral, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(Str.autoClearCache, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to Str.never, 7 to Str.everyDays(7), 30 to Str.everyDays(30)).forEach { (d, label) ->
                            val sel = autoDays == d
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (sel) Coral else BgCard)
                                    .clickable { AppSettings.setAutoClearCacheDays(d) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    label,
                                    color = if (sel) Color.White else TextMain,
                                    fontSize = 13.sp,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
            // ===== Statistik =====
            item {
                SettingsSection(Str.watchStats) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BgCard)
                            .clickable(onClick = onOpenStats)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BarChart, null, tint = Coral, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(Str.watchStats, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            // ===== Info Aplikasi =====
            item {
                SettingsSection(Str.about) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BgCard)
                            .clickable(onClick = onOpenAppInfo)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = Coral, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(Str.appInfo, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .padding(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(title: String, desc: String?, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (desc != null) Text(desc, color = TextMuted, fontSize = 11.sp)
        }
        trailing()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    desc: String?,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    SettingsRow(title, desc) {
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Coral
            )
        )
    }
}
