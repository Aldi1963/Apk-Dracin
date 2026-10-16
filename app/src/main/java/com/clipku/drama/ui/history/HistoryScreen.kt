package com.clipku.drama.ui.history
import com.clipku.drama.ui.theme.Str

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.clipku.drama.ui.components.FastImage
import com.clipku.drama.data.history.WatchHistory
import com.clipku.drama.ui.theme.*

@Composable
fun HistoryScreen(
    onResume: (contentId: String, ep: Int, totalEp: Int, title: String) -> Unit,
    onBrowse: () -> Unit
) {
    val rawItems by WatchHistory.items.collectAsState()
    // satu baris per judul (ambil tontonan terbaru per contentId)
    val items = remember(rawItems) {
        rawItems.groupBy { it.contentId }.values.mapNotNull { g ->
            g.maxByOrNull { it.watchedAt }
        }.sortedByDescending { it.watchedAt }
    }
    val favItems by com.clipku.drama.data.history.FavoriteStore.items.collectAsState()
    var showFav by remember { mutableStateOf(false) }
    var cleaning by remember { mutableStateOf(false) }
    var cleanedCount by remember { mutableStateOf(0) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var backupMsg by remember { mutableStateOf<String?>(null) }

    // Backup: simpan ke file JSON
    val saveLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(com.clipku.drama.data.history.BackupManager.exportJson().toByteArray())
                }
                backupMsg = Str.backupSaved
            } catch (_: Exception) {
                backupMsg = Str.backupSaveFailed
            }
        }
    }
    // Restore: baca file JSON backup
    val openLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: throw IllegalArgumentException("empty")
                val r = com.clipku.drama.data.history.BackupManager.importJson(text)
                backupMsg = Str.restoredCounts(r.history, r.favorites, r.searches) +
                    (if (r.skipped > 0) Str.skippedCount(r.skipped) else "")
            } catch (_: Exception) {
                backupMsg = Str.invalidBackupFile
            }
        }
    }

    // Auto-bersih riwayat mati: maksimal 1x per hari, di background
    LaunchedEffect(Unit) {
        val prefs = ctx.getSharedPreferences("clipku_maintenance", android.content.Context.MODE_PRIVATE)
        val last = prefs.getLong("last_clean", 0)
        val dayMs = 24 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - last < dayMs) return@LaunchedEffect
        prefs.edit().putLong("last_clean", System.currentTimeMillis()).apply()
        val list = WatchHistory.items.value
        if (list.isEmpty()) return@LaunchedEffect
        cleaning = true
        var removed = 0
        try {
            for (h in list) {
                val alive = try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        kotlinx.coroutines.withTimeoutOrNull(8000) {
                            com.clipku.drama.data.sonzaix.SonzaixRepository.getDetail(h.contentId)
                        } != null
                    }
                } catch (_: Exception) { false }
                if (!alive) {
                    WatchHistory.remove(h.contentId)
                    removed++
                }
            }
        } catch (_: Exception) {}
        cleanedCount = removed
        cleaning = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf("Riwayat", "Favorit").forEach { tab ->
                    val selected = (tab == "Favorit") == showFav
                    Text(
                        if (tab == "Favorit") Str.favorites else Str.history,
                        color = if (selected) TextMain else TextMuted,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showFav = tab == "Favorit" }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    if (tab == "Riwayat") Spacer(Modifier.width(16.dp))
                }
            }
            Row {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var clearing by remember { mutableStateOf(false) }
                IconButton(onClick = {
                    if (!clearing) {
                        clearing = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                context.imageLoader.diskCache?.clear()
                                context.imageLoader.memoryCache?.clear()
                            } catch (_: Exception) {}
                            withContext(Dispatchers.Main) { clearing = false }
                        }
                    }
                }) {
                    if (clearing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TextMuted)
                    else Icon(Icons.Default.Refresh, Str.clearImageCache, tint = TextMuted)
                }
                if (items.isNotEmpty()) {
                    IconButton(onClick = { WatchHistory.clear() }) {
                        Icon(Icons.Default.Delete, Str.clearHistory, tint = TextMuted)
                    }
                }
                // Backup & restore
                IconButton(onClick = {
                    saveLauncher.launch(com.clipku.drama.data.history.BackupManager.defaultFileName())
                }) {
                    Icon(Icons.Default.Save, Str.backupHistoryFav, tint = TextMuted)
                }
                IconButton(onClick = {
                    openLauncher.launch(arrayOf("application/json"))
                }) {
                    Icon(Icons.Default.Upload, Str.restoreFromBackup, tint = TextMuted)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (cleaning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Coral)
                Spacer(Modifier.width(8.dp))
                Text(Str.checkingHistory, color = TextMuted, fontSize = 12.sp)
            }
        } else if (cleanedCount > 0) {
            Text(
                Str.cleanedDeadHistory(cleanedCount),
                color = TextMuted, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (backupMsg != null) {
            LaunchedEffect(backupMsg) {
                kotlinx.coroutines.delay(4000)
                backupMsg = null
            }
            Text(
                backupMsg!!,
                color = Coral, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (showFav) {
            // ===== TAB FAVORIT =====
            if (favItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Str.noFavoritesYet, color = TextMuted, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            Str.tapHeartHint,
                            color = TextMuted.copy(alpha = 0.7f), fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onBrowse,
                            colors = ButtonDefaults.buttonColors(containerColor = Coral),
                            shape = RoundedCornerShape(20.dp)
                        ) { Text(Str.browseDramas) }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favItems, key = { it.contentId }) { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BgCard)
                                .clickable { onResume(f.contentId, 1, f.totalEp.coerceAtLeast(1), f.title) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.clipku.drama.ui.components.FastImage(
                                url = f.posterUrl,
                                contentDescription = f.title,
                                modifier = Modifier
                                    .size(width = 56.dp, height = 76.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    f.title, color = TextMain,
                                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(f.providerName, color = TextMuted, fontSize = 12.sp)
                            }
                            IconButton(onClick = {
                                com.clipku.drama.data.history.FavoriteStore.remove(f.contentId)
                            }) {
                                Icon(Icons.Filled.Favorite, Str.removeFavorite, tint = Coral)
                            }
                        }
                    }
                }
            }
        } else if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Str.noHistoryYet, color = TextMuted, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onBrowse,
                        colors = ButtonDefaults.buttonColors(containerColor = Coral),
                        shape = RoundedCornerShape(20.dp)
                    ) { Text(Str.browseDramas) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.contentId }) { h ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { v ->
                            if (v == SwipeToDismissBoxValue.EndToStart) {
                                WatchHistory.remove(h.contentId)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, Str.delete, tint = Color.White)
                            }
                        }
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgCard)
                            .clickable { onResume(h.contentId, h.lastEp, h.totalEp, h.title) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FastImage(
                            url = h.posterUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 64.dp, height = 88.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                h.title, color = TextMain, fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                Str.lastEpOf(h.providerName, h.lastEp, h.totalEp),
                                color = TextMuted, fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            // progress bar
                            val progress = if (h.totalEp > 0) h.lastEp.toFloat() / h.totalEp else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Coral,
                                trackColor = BgChip
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { onResume(h.contentId, h.lastEp, h.totalEp, h.title) },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Coral, RoundedCornerShape(22.dp))
                        ) {
                            Icon(Icons.Default.PlayArrow, Str.resume, tint = Color.White)
                        }
                    }
                    }
                }
            }
        }
    }
}
