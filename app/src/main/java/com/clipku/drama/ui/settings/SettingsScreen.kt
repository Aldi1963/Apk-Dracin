package com.clipku.drama.ui.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import com.clipku.drama.data.health.ProviderHealth
import com.clipku.drama.data.prefs.AppSettings
import com.clipku.drama.data.prefs.CachePrefs
import com.clipku.drama.data.sonzaix.SonzaixProviders
import com.clipku.drama.ui.theme.BgCard
import com.clipku.drama.ui.theme.BgDeep
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.Divider
import com.clipku.drama.ui.theme.Str
import com.clipku.drama.ui.theme.TextMain
import com.clipku.drama.ui.theme.TextMuted
import com.clipku.drama.ui.theme.ThemeMode
import com.clipku.drama.ui.theme.ThemePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pengaturan — desain grouped modern ala iOS Settings / Telegram.
 * Semua perilaku dipertahankan sama persis seperti v61 (recreate untuk
 * bahasa & font, dsb). Daftar provider pindah ke ProviderSettingsScreen.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenProviders: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pkg = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (_: Exception) { null }
    val versionName = pkg?.versionName ?: "-"

    val manualHidden by AppSettings.hiddenProvidersManual.collectAsState()
    val autoHidden by ProviderHealth.hidden.collectAsState()
    val hiddenCount = (manualHidden + autoHidden).size
    val totalProviders = SonzaixProviders.all.size

    Column(Modifier.fillMaxSize().background(BgDeep)) {
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ===== Header aplikasi =====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(BgCard)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Coral),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("CLIPKU+", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text("${Str.version} $versionName", color = TextMuted, fontSize = 12.sp)
                        Text("Aldi Irawan", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
            // ===== Tampilan =====
            item {
                val mode by ThemePrefs.mode.collectAsState()
                val lang by AppSettings.appLanguage.collectAsState()
                val cols by AppSettings.gridColumns.collectAsState()
                val fs by AppSettings.uiFontScale.collectAsState()
                GroupCard(Str.appearance) {
                    SegmentedSettingRow(
                        icon = Icons.Default.Palette,
                        title = Str.theme,
                        options = ThemeMode.values().map { it to it.label },
                        selected = mode,
                        onSelect = { ThemePrefs.setMode(it) }
                    )
                    RowDivider()
                    SegmentedSettingRow(
                        icon = Icons.Default.Language,
                        title = Str.language,
                        options = listOf("id" to "Indonesia", "en" to "English"),
                        selected = lang,
                        onSelect = {
                            AppSettings.setAppLanguage(it)
                            (context as? Activity)?.recreate()
                        }
                    )
                    RowDivider()
                    SegmentedSettingRow(
                        icon = Icons.Default.GridView,
                        title = Str.gridDensity,
                        options = listOf(2 to "2", 3 to "3"),
                        selected = cols,
                        onSelect = { AppSettings.setGridColumns(it) }
                    )
                    RowDivider()
                    SegmentedSettingRow(
                        icon = Icons.Default.TextFields,
                        title = Str.uiFontSize,
                        options = listOf(0.9f to Str.small, 1.0f to Str.normal, 1.15f to Str.large),
                        selected = fs,
                        onSelect = {
                            AppSettings.setUiFontScale(it)
                            (context as? Activity)?.recreate()
                        }
                    )
                }
            }
            // ===== Pemutaran =====
            item {
                val autoNext by AppSettings.autoNext.collectAsState()
                val dtSecs by AppSettings.doubleTapSecs.collectAsState()
                GroupCard(Str.playback) {
                    SettingRow(
                        icon = Icons.Default.PlayCircle,
                        title = Str.autoNextEp,
                        subtitle = Str.autoNextEpDesc,
                        trailing = {
                            CoralSwitch(autoNext) { AppSettings.setAutoNext(it) }
                        }
                    )
                    RowDivider()
                    SegmentedSettingRow(
                        icon = Icons.Default.Timer,
                        title = Str.doubleTapDuration,
                        options = listOf(5, 10, 15, 30).map { it to Str.seekSec(it).trimStart('+') },
                        selected = dtSecs,
                        onSelect = { AppSettings.setDoubleTapSecs(it) }
                    )
                }
            }
            // ===== Jaringan =====
            item {
                val saver by AppSettings.dataSaver.collectAsState()
                GroupCard(Str.network) {
                    SettingRow(
                        icon = Icons.Default.DataSaverOn,
                        title = Str.dataSaver,
                        subtitle = Str.dataSaverDesc,
                        trailing = {
                            CoralSwitch(saver) { AppSettings.setDataSaver(it) }
                        }
                    )
                }
            }
            // ===== Provider (ringkas → layar tersendiri) =====
            item {
                GroupCard(Str.providers) {
                    SettingRow(
                        icon = Icons.Default.Dns,
                        title = Str.providers,
                        subtitle = Str.providersHidden(hiddenCount, totalProviders),
                        onClick = onOpenProviders
                    )
                }
            }
            // ===== Penyimpanan =====
            item {
                val autoDays by AppSettings.autoClearCacheDays.collectAsState()
                var clearing by remember { mutableStateOf(false) }
                GroupCard(Str.storage) {
                    SettingRow(
                        icon = Icons.Default.Storage,
                        title = Str.clearImageCache,
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Coral.copy(alpha = 0.12f))
                                    .clickable {
                                        if (!clearing) {
                                            clearing = true
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    context.imageLoader.diskCache?.clear()
                                                    context.imageLoader.memoryCache?.clear()
                                                    CachePrefs.markCleared(context)
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
                    )
                    RowDivider()
                    SegmentedSettingRow(
                        icon = Icons.Default.Storage,
                        title = Str.autoClearCache,
                        options = listOf(0 to Str.never, 7 to Str.everyDays(7), 30 to Str.everyDays(30)),
                        selected = autoDays,
                        onSelect = { AppSettings.setAutoClearCacheDays(it) }
                    )
                }
            }
            // ===== Lainnya =====
            item {
                GroupCard(Str.others) {
                    SettingRow(
                        icon = Icons.Default.BarChart,
                        title = Str.watchStats,
                        onClick = onOpenStats
                    )
                    RowDivider()
                    SettingRow(
                        icon = Icons.Default.Info,
                        title = Str.appInfo,
                        onClick = onOpenAppInfo
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------- Komponen grouped settings ----------

@Composable
private fun GroupCard(header: String, content: @Composable () -> Unit) {
    Column {
        Text(
            header,
            color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
        ) {
            content()
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Coral.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Coral, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = Divider,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextMuted, fontSize = 12.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted,
                modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun <T> SegmentedSettingRow(
    icon: ImageVector,
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon)
            Spacer(Modifier.width(12.dp))
            Text(title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier.weight(1f),
                    label = { Text(label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
    }
}

@Composable
private fun CoralSwitch(checked: Boolean, onChecked: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChecked,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Coral
        )
    )
}
