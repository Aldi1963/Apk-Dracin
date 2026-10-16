package com.clipku.drama.ui.detail
import com.clipku.drama.ui.theme.Str

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clipku.drama.ui.components.FastImage
import com.clipku.drama.ui.theme.BgCard
import com.clipku.drama.ui.theme.BgChip
import com.clipku.drama.ui.theme.BgDeep
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.StarYellow
import com.clipku.drama.ui.theme.TextMain
import com.clipku.drama.ui.theme.TextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    slug: String,
    fallbackTitle: String = "",
    fallbackPoster: String = "",
    onBack: () -> Unit,
    onPlay: (contentId: String, ep: Int, totalEp: Int, title: String, poster: String, provider: String) -> Unit,
    vm: DetailViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current
    val state by vm.uiState.collectAsState()
    LaunchedEffect(slug) { vm.load(slug) }

    Box(Modifier.fillMaxSize().background(BgDeep)) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Coral)
                }
            }
            state.error != null -> {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.error!!, color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.load(slug) },
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) { Text(Str.retry) }
                }
            }
            state.detail != null -> {
                val raw = state.detail!!
                // dramabite & sejenis: detail tak punya judul/poster — pakai dari daftar
                val d = raw.copy(
                    title = raw.title.takeIf { it.isNotBlank() && it != Str.untitled } ?: fallbackTitle.ifBlank { raw.title },
                    posterUrl = raw.posterUrl?.takeIf { it.isNotBlank() } ?: fallbackPoster.ifBlank { null }
                )
                val backdrop = d.bannerUrl ?: d.posterUrl
                val watchedEp by com.clipku.drama.data.history.WatchHistory.items.collectAsState()
                val watchedSet = remember(watchedEp, d.id) {
                    watchedEp.filter { it.contentId == d.id }.map { it.lastEp }.toSet()
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // ===== BACKDROP (collapsing visual) =====
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(6) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9.5f)
                        ) {
                            FastImage(
                                url = backdrop,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            Box(
                                Modifier.matchParentSize().background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Transparent,
                                            BgDeep
                                        )
                                    )
                                )
                            )
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                            // tombol favorit (hati) di kanan atas backdrop
                            val favList by com.clipku.drama.data.history.FavoriteStore.items.collectAsState()
                            val isFav = remember(favList, d.id) {
                                favList.any { it.contentId == d.id }
                            }
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    com.clipku.drama.data.history.FavoriteStore.toggle(
                                        d.id, d.title, d.posterUrl, d.providerName, d.totalEpisodes
                                    )
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorit",
                                    tint = if (isFav) Coral else Color.White
                                )
                            }
                            // tombol play besar di tengah backdrop
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Coral)
                                    .clickable { onPlay(d.id, 1, d.totalEpisodes, d.title, d.posterUrl ?: "", d.providerName) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                    }

                    // ===== INFO (full 6 kolom) =====
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(6) }) {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Text(d.title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoPill(Str.episodeCount(d.totalEpisodes), highlight = true)
                                if (!d.status.isNullOrBlank()) InfoPill(d.status!!)
                                // genre asli dari provider (bila ada)
                                d.genres.take(4).forEach { g -> InfoPill(g) }
                                if (d.providerName.isNotBlank()) InfoPill(d.providerName)
                                if (d.type.isNotBlank()) InfoPill(d.type)
                                if (d.rating > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(BgChip)
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Icon(Icons.Default.Star, null, tint = StarYellow, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            String.format("%.1f", d.rating).replace(".", ","),
                                            color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (d.viewCount > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(BgChip)
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            Str.viewsWatched(d.viewCount),
                                            color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            // tombol tonton full-width (lanjutkan progres bila ada)
                            val resumeEp = remember(watchedSet) {
                                (1..d.totalEpisodes).firstOrNull { n -> n !in watchedSet } ?: 1
                            }
                            Button(
                                onClick = { onPlay(d.id, resumeEp, d.totalEpisodes, d.title, d.posterUrl ?: "", d.providerName) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Coral)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (resumeEp > 1) Str.continueEpisode(resumeEp) else Str.watchEpisode1,
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                                )
                            }
                            if (!d.description.isNullOrBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Text(Str.synopsis, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(Modifier.height(6.dp))
                                var expanded by remember { mutableStateOf(false) }
                                Text(
                                    d.description!!,
                                    color = TextMuted,
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp,
                                    maxLines = if (expanded) Int.MAX_VALUE else 5,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
                                    style = androidx.compose.ui.text.TextStyle(
                                        hyphens = androidx.compose.ui.text.style.Hyphens.Auto
                                    )
                                )
                                Text(
                                    if (expanded) Str.close else Str.seeMore,
                                    color = Coral,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { expanded = !expanded }
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(Str.episode, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                // Badge jumlah episode ala PineDrama
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BgChip)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        "${d.totalEpisodes} EP",
                                        color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    // ===== EPISODE GRID (urut 1..N) =====
                    items(d.episodes.ifEmpty { (1..d.totalEpisodes).map { com.clipku.drama.data.model.Episode(episodeNumber = it) } }.sortedBy { it.episodeNumber }) { e ->
                        val ep = e.episodeNumber
                        val isWatched = watchedSet.contains(ep)
                        // centang eksplisit bila episode benar-benar selesai (>=90% / sampai habis)
                        val isDone = com.clipku.drama.data.history.WatchHistory.isEpisodeDone(d.id, ep)
                        // progress untuk episode yang sedang ditonton
                        val progress = remember(watchedEp, d.id, ep) {
                            watchedEp.find { it.contentId == d.id && it.lastEp == ep }
                                ?.let { if (it.durationMs > 0) it.positionMs.toFloat() / it.durationMs else 0f }
                                ?: 0f
                        }
                        // Episode berikutnya yang belum ditonton = highlight coral (ala PineDrama)
                        val nextEp = remember(watchedSet) {
                            (1..d.totalEpisodes).firstOrNull { n -> n !in watchedSet } ?: 1
                        }
                        val isNext = ep == nextEp && !isWatched
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isNext -> Coral
                                        isWatched -> BgChip.copy(alpha = 0.45f)
                                        else -> BgChip
                                    }
                                )
                                .border(
                                    width = if (isNext) 0.dp else 1.dp,
                                    color = if (isWatched) Color.Transparent else TextMuted.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPlay(d.id, ep, d.totalEpisodes, d.title, d.posterUrl ?: "", d.providerName)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$ep",
                                color = when {
                                    isNext -> Color.White
                                    isWatched -> TextMuted.copy(alpha = 0.55f)
                                    else -> TextMain
                                },
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            // progress bar tipis di bawah untuk episode yang ditonton sebagian
                            if (!isWatched && progress > 0.02f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(Color.White.copy(alpha = 0.25f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                                            .background(Coral)
                                    )
                                }
                            }
                            // centang untuk episode yang sudah selesai ditonton
                            if (isDone) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Default.Check,
                                    Str.watched,
                                    tint = Coral,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(5.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(text: String, highlight: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (highlight) Coral else BgChip)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            color = if (highlight) Color.White else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
        )
    }
}
