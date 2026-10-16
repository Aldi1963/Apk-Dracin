package com.clipku.drama.ui.components
import com.clipku.drama.ui.theme.Str

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.NorthWest
import com.clipku.drama.ui.theme.BgDeep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.clipku.drama.data.sonzaix.SonzaixApi
import com.clipku.drama.data.model.DramaItem
import com.clipku.drama.ui.theme.BgCard
import com.clipku.drama.ui.theme.BgChip
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.StarYellow
import com.clipku.drama.ui.theme.TextMain
import com.clipku.drama.ui.theme.TextMuted
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Gambar teroptimasi: crossfade halus, cache memori+disk agresif,
 * dan decode sesuai ukuran tampil agar cepat dimuat & hemat memori.
 */
@Composable
fun FastImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val resolved = SonzaixApi.resolveUrl(url)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(resolved)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

// ===== Header compact 56dp dengan search inline =====

// ===== Header dengan search bar permanen (langsung cari semua provider) =====
@Composable
fun InlineSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onSearchSubmit: () -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Logo kompak
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Coral),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        // Search bar permanen — ketik langsung cari semua provider
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { onFocusChange(it.isFocused) }
                .clip(RoundedCornerShape(18.dp))
                .background(BgCard)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 14.sp),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSearchSubmit() }
            ),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isEmpty()) {
                        Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Str.searchAllDramas, color = TextMuted, fontSize = 13.sp)
                    }
                    Box(Modifier.weight(1f)) { inner() }
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close, null, tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = androidx.compose.runtime.remember {
                                        androidx.compose.foundation.interaction.MutableInteractionSource()
                                    }
                                ) { onQueryChange("") }
                        )
                    }
                    // tombol mic: voice search
                    VoiceSearchButton(onSpoken = onQueryChange)
                }
            }
        )
        // Ikon gear -> layar Pengaturan
        IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Settings, contentDescription = Str.settings, tint = TextMuted)
        }
    }
}


// ===== "Maksud Anda" — koreksi typo hasil search =====
@Composable
fun DidYouMeanRow(correction: String, onPick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(correction) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(Str.didYouMean + ": ", color = TextMuted, fontSize = 13.sp)
        Text(
            correction, color = Coral, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text("?", color = TextMuted, fontSize = 13.sp)
    }
}

// ===== Kata kunci trending (tampil saat kolom search kosong) =====
val TrendingKeywords = listOf(
    "kultivasi", "CEO", "balas dendam", "menikah kontrak",
    "istri", "reinkarnasi", "cinta pertama", "dewa perang",
    "raja iblis", "pernikahan", "anak hilang", "orang kaya"
)


// ===== Tombol voice search (dipakai ulang di header & layar search) =====
@Composable
fun VoiceSearchButton(
    onSpoken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()
            if (!spoken.isNullOrEmpty()) onSpoken(spoken)
        }
    }
    IconButton(
        onClick = {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, Str.searchHintShort)
                }
                voiceLauncher.launch(intent)
            } catch (_: Exception) { }
        },
        modifier = modifier.size(28.dp)
    ) {
        Icon(Icons.Default.Mic, null, tint = TextMuted, modifier = Modifier.size(16.dp))
    }
}

// ===== Baris hasil search ala drama app: poster + info + sinopsis =====
@Composable
fun SearchResultRow(
    item: com.clipku.drama.data.model.DramaItem,
    keyword: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 108.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BgCard)
        ) {
            FastImage(
                url = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            RatingBadge(item.rating, modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
        }
        Column(Modifier.weight(1f)) {
            if (keyword.isNotBlank()) {
                HighlightText(
                    content = item.title, keyword = keyword,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2
                )
            } else {
                Text(
                    item.title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.providerName.isNotBlank()) {
                    Text(item.providerName, color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                if (item.episodeCount > 0) {
                    Text(Str.episodeCount(item.episodeCount), color = TextMuted, fontSize = 11.sp)
                }
                if (item.viewCount > 0) {
                    Text(Str.viewsWatched(item.viewCount), color = TextMuted, fontSize = 11.sp)
                }
            }
            if (item.genres.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.genres.take(3).forEach { g ->
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(BgChip)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(g, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }
            val snippet = remember(item.synopsis, keyword) {
                synopsisSnippet(item.synopsis, keyword)
            }
            if (snippet.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                HighlightText(
                    content = snippet, keyword = keyword,
                    fontSize = 11.sp, fontWeight = FontWeight.Normal, maxLines = 2
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight, null, tint = TextMuted,
            modifier = Modifier.size(20.dp).align(Alignment.CenterVertically)
        )
    }
}

// ===== Daftar trending berperingkat ala drama app =====
@Composable
fun TrendingSearchList(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Whatshot, null, tint = Coral, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(Str.popularSearches, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        TrendingKeywords.take(10).forEachIndexed { i, kw ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(kw) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${i + 1}",
                    color = if (i < 3) Coral else TextMuted,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    kw, color = TextMain, fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.TrendingUp, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ===== Riwayat sebagai chips ala drama app =====
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryChips(
    history: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Str.recentlySearched, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClear) {
                Text(Str.delete, color = Coral, fontSize = 12.sp)
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            history.forEach { q ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgChip)
                        .clickable { onPick(q) }
                        .padding(start = 12.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        q, color = TextMain, fontSize = 12.sp, maxLines = 1,
                        modifier = Modifier.widthIn(max = 160.dp),
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { onRemove(q) }, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

// ===== Layar pencarian full-screen ala aplikasi drama =====
@Composable
fun DramaSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean,
    results: List<com.clipku.drama.data.model.DramaItem>,
    resultCountText: String,
    suggestions: List<String>,
    onSuggestionPick: (String) -> Unit,
    history: List<String>,
    onHistoryPick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
    sortMode: SortMode,
    onSortChange: (SortMode) -> Unit,
    providers: List<String>,
    providerFilter: String?,
    onProviderFilter: (String?) -> Unit,
    genres: List<String>,
    genreFilter: String?,
    onGenreFilter: (String?) -> Unit,
    didYouMean: String?,
    onDramaClick: (com.clipku.drama.data.model.DramaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        try { focusRequester.requestFocus() } catch (_: Exception) { }
    }
    Column(modifier = modifier.fillMaxSize().background(BgDeep)) {
        // top bar: kembali + kolom cari + mic
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextMain,
                    modifier = Modifier.size(22.dp)
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BgCard)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                textStyle = TextStyle(color = TextMain, fontSize = 14.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isEmpty()) {
                            Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Str.searchHintLong, color = TextMuted, fontSize = 13.sp)
                        }
                        Box(Modifier.weight(1f)) { inner() }
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close, null, tint = TextMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember {
                                            androidx.compose.foundation.interaction.MutableInteractionSource()
                                        }
                                    ) { onQueryChange("") }
                            )
                        }
                        VoiceSearchButton(onSpoken = onQueryChange)
                    }
                }
            )
        }
        // konten
        when {
            isLoading && results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Coral)
                }
            }
            query.isBlank() -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    item { TrendingSearchList(onPick = onSuggestionPick) }
                    item {
                        HistoryChips(
                            history = history,
                            onPick = onHistoryPick,
                            onRemove = onHistoryRemove,
                            onClear = onHistoryClear
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
            results.isNotEmpty() -> {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(resultCountText, color = TextMuted, fontSize = 12.sp)
                        SortDropdown(selected = sortMode, onSelect = onSortChange)
                    }
                    if (didYouMean != null) {
                        DidYouMeanRow(correction = didYouMean, onPick = onSuggestionPick)
                    }
                    if (providers.size > 1) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            item {
                                FilterChipSmall(Str.all, providerFilter == null) { onProviderFilter(null) }
                            }
                            items(providers) { p ->
                                FilterChipSmall(p, providerFilter == p) { onProviderFilter(p) }
                            }
                        }
                    }
                    if (genres.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(genres) { g ->
                                FilterChipSmall(g, genreFilter == g) {
                                    onGenreFilter(if (genreFilter == g) null else g)
                                }
                            }
                        }
                    }
                    LazyColumn(Modifier.fillMaxSize().weight(1f)) {
                        items(results, key = { it.id }) { item ->
                            SearchResultRow(
                                item = item,
                                keyword = query,
                                onClick = { onDramaClick(item) }
                            )
                        }
                    }
                }
            }
            suggestions.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(suggestions) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionPick(s) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            HighlightText(
                                content = s, keyword = query, fontSize = 14.sp, maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.NorthWest, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.SearchOff, null, tint = TextMuted, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(Str.notFound, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        Str.noResultsFor(query) + ". " + Str.tryOtherKeywords,
                        color = TextMuted, fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (didYouMean != null) {
                        Spacer(Modifier.height(8.dp))
                        DidYouMeanRow(correction = didYouMean, onPick = onSuggestionPick)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onQueryChange("") },
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) { Text(Str.retry) }
                }
            }
        }
    }
}

// ===== Chip filter kecil =====
@Composable
fun FilterChipSmall(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Coral else BgChip)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextMain,
            fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

// ===== Dialog pilih tema =====

// ===== Teks dengan highlight kata kunci =====
@Composable
fun HighlightText(
    content: String,
    keyword: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    maxLines: Int = 2
) {
    val query = keyword.lowercase().trim()
    val annotated = androidx.compose.runtime.remember(content, query) {
        val b = AnnotatedString.Builder()
        if (query.isEmpty()) {
            b.append(content)
        } else {
            val low = content.lowercase()
            var pos = 0
            while (true) {
                val at = low.indexOf(query, pos)
                if (at < 0) {
                    b.append(content.substring(pos))
                    break
                }
                b.append(content.substring(pos, at))
                b.pushStyle(
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(0xFFFF4D67),
                        fontWeight = FontWeight.Bold
                    )
                )
                b.append(content.substring(at, at + query.length))
                b.pop()
                pos = at + query.length
            }
        }
        b.toAnnotatedString()
    }
    Text(
        annotated, modifier = modifier, color = TextMain,
        fontSize = fontSize, fontWeight = fontWeight,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        lineHeight = 14.sp
    )
}

// ===== Badge rating kecil =====
@Composable
fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    if (rating <= 0) return
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Star, null, tint = StarYellow, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            String.format("%.1f", rating).replace(".", ","),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ===== Kartu poster portrait (grid) =====
@Composable
fun DramaPosterCard(
    item: DramaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightKeyword: String = ""
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
        ) {
            FastImage(
                url = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            // gradient bawah
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                            startY = 300f
                        )
                    )
            )
            RatingBadge(item.rating, modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        Spacer(Modifier.height(6.dp))
        if (highlightKeyword.isNotBlank()) {
            HighlightText(
                content = item.title,
                keyword = highlightKeyword,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
        } else {
            Text(
                item.title,
                color = TextMain,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
        }
        if (item.episodeCount > 0) {
            Text(
                Str.episodeCount(item.episodeCount),
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
        // jumlah ditonton (hanya provider yang menyediakan data: drakorid, melolo)
        if (item.viewCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Visibility, null, tint = TextMuted,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    Str.viewsWatched(item.viewCount),
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
        // cuplikan sinopsis yang cocok dengan kata kunci (hanya di hasil search)
        if (highlightKeyword.isNotBlank()) {
            val snippet = remember(item.synopsis, highlightKeyword) {
                synopsisSnippet(item.synopsis, highlightKeyword)
            }
            if (snippet.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                HighlightText(
                    content = snippet,
                    keyword = highlightKeyword,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Potongan sinopsis di sekitar kata kunci yang cocok, untuk kartu hasil search.
 * "" bila tidak ada kata kunci yang cocok di sinopsis.
 */
fun synopsisSnippet(synopsis: String, query: String): String {
    val q = query.lowercase().trim()
    if (q.isEmpty() || synopsis.isBlank()) return ""
    val words = q.split(Regex("\\s+")).filter { it.length >= 3 }
    val low = synopsis.lowercase()
    var at = low.indexOf(q)
    if (at < 0) {
        for (w in words) {
            at = low.indexOf(w)
            if (at >= 0) break
        }
    }
    if (at < 0) return ""
    val start = maxOf(0, at - 40)
    val end = minOf(synopsis.length, at + 90)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < synopsis.length) "…" else ""
    return prefix + synopsis.substring(start, end).trim() + suffix
}

// ===== Format jumlah views ringkas (5,4 jt / 5.4M, 823 rb / 823K) =====
// ===== Header section =====
@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                color = Coral,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

// ===== Avatar inisial provider =====
@Composable
fun ProviderAvatar(name: String, logoUrl: String? = null, size: Int = 40, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(BgCard),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrBlank()) {
            FastImage(
                url = logoUrl,
                contentDescription = name,
                modifier = Modifier.matchParentSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(initial, color = Coral, fontWeight = FontWeight.Bold, fontSize = (size * 0.42).sp)
        }
    }
}

// ===== Filter genre berbasis keyword judul =====
data class GenreDef(val name: String, val keywords: List<String>)

val GenreList = listOf(
    GenreDef(Str.all, emptyList()),
    GenreDef("Romantis", listOf("cinta", "love", "sayang", "nikah", "kawin", "tunangan", "pacar", "istri", "suami", "married", "wife", "husband", "bride", "wedding", "romantis", "romance", "jodoh", "lamaran")),
    GenreDef("Dendam", listOf("dendam", "balas", "revenge", "hancur")),
    GenreDef("Keluarga", listOf("keluarga", "ibu", "ayah", "anak", "mama", "papa", "family", "mother", "father", "nenek", "kakek")),
    GenreDef("Komedi", listOf("lucu", "komedi", "comedy", "funny", "kocak", "ngakak")),
    GenreDef("Misteri", listOf("misteri", "pembunuh", "mystery", "killer", "thriller", "rahasia", "secret", "hilang", "missing")),
    GenreDef("Fantasi", listOf("naga", "siluman", "dewa", "fantasy", "dragon", "vampire", "werewolf", "immortal", "reinkarnasi", "kultivasi")),
    GenreDef("Bos Kaya", listOf("ceo", "bos", "miliarder", "direktur", "billionaire", "boss", "kaya", "rich", "konglomerat"))
)

fun matchGenre(title: String, genre: GenreDef): Boolean {
    if (genre.keywords.isEmpty()) return true
    // word boundary: "bos" tidak cocok "bosan", "kaya" tidak cocok "kekayaan"
    val words = title.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
    return genre.keywords.any { kw ->
        val k = kw.lowercase()
        k in words || words.any { w -> w.startsWith(k) && w.length <= k.length + 3 }
    }
}

@Composable
fun GenreChipRow(
    selected: GenreDef,
    onSelect: (GenreDef) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(GenreList) { g ->
            val sel = g.name == selected.name
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) Coral else BgChip)
                    .clickable { onSelect(g) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    g.name,
                    color = if (sel) Color.White else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

// ===== Pilihan urutan hasil =====
enum class SortMode {
    RELEVAN, RATING, DITONTON, EPISODE_TERBANYAK, JUDUL_AZ;
    val label: String
        get() = when (this) {
            RELEVAN -> Str.relevant
            RATING -> Str.topRated
            DITONTON -> Str.mostWatched
            EPISODE_TERBANYAK -> Str.mostEpisodes
            JUDUL_AZ -> Str.titleAZ
        }
}

@Composable
fun SortDropdown(
    selected: SortMode,
    onSelect: (SortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(BgChip)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Sort, null, tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(selected.label, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(BgCard)
        ) {
            SortMode.values().forEach { mode ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            mode.label,
                            color = if (mode == selected) Coral else TextMain,
                            fontSize = 13.sp,
                            fontWeight = if (mode == selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = { onSelect(mode); expanded = false }
                )
            }
        }
    }
}


// ===== Branded loading indicator: badge play coral + cincin pulsing + arc berputar =====
// Dipakai di player (loading awal & buffering), pengganti CircularProgressIndicator polos.
@Composable
fun ClipkuLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp
) {
    val infinite = rememberInfiniteTransition(label = "clipku_loading")
    // cincin pulsing: melebar sambil memudar
    val pulse by infinite.animateFloat(
        initialValue = 0.70f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.65f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    // arc berputar halus
    val rotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Coral.copy(alpha = pulseAlpha),
                radius = (size.toPx() / 2f) * pulse,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
        Canvas(
            Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }
        ) {
            val pad = 5.dp.toPx()
            drawArc(
                color = Coral,
                startAngle = -90f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.toPx() - pad * 2, size.toPx() - pad * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        // badge brand: lingkaran coral + ikon play putih
        Box(
            Modifier.size(size * 0.52f).clip(CircleShape).background(Coral),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow, null, tint = Color.White,
                modifier = Modifier.size(size * 0.30f)
            )
        }
    }
}
