package com.clipku.drama.ui.home
import com.clipku.drama.ui.theme.Str

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import com.clipku.drama.data.model.DramaItem
import com.clipku.drama.ui.components.FastImage
import com.clipku.drama.ui.components.InlineSearchHeader
import com.clipku.drama.ui.components.DramaPosterCard
import com.clipku.drama.ui.components.RatingBadge
import com.clipku.drama.ui.components.SectionHeader
import com.clipku.drama.ui.components.FilterChipSmall
import com.clipku.drama.ui.components.HighlightText
import com.clipku.drama.ui.theme.BgCard
import com.clipku.drama.ui.theme.BgChip
import com.clipku.drama.ui.theme.BgDeep
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.TextMain
import com.clipku.drama.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mode: HomeMode = HomeMode.BERANDA,
    onDramaClick: (com.clipku.drama.data.model.DramaItem) -> Unit,
    onExploreClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    vm: HomeViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()

    // ===== ANTI-DOBEL: item yang sudah tampil di hero/trending jangan tampil lagi di bawah =====
    val heroIds = remember(state.heroItems) { state.heroItems.map { it.id }.toSet() }
    val trendingIds = remember(state.trendingItems) { state.trendingItems.map { it.id }.toSet() }
    val latestIds = remember(state.latestItems) { state.latestItems.map { it.id }.toSet() }
    // trending: buang yang sudah ada di hero
    val trendingVisible = remember(state.trendingItems, heroIds) {
        state.trendingItems.filter { it.id !in heroIds }
    }
    // grid Jelajahi/Hasil Filter: buang yang sudah ada di hero maupun trending
    val gridItems = remember(state.items, heroIds, trendingIds, latestIds) {
        state.items.filter { it.id !in heroIds && it.id !in trendingIds && it.id !in latestIds }
    }
    LaunchedEffect(mode) { vm.applyMode(mode) }

    // ===== Inline search state =====
    var searchQuery by remember { mutableStateOf("") }
    // layar search dibuka/tutup eksplisit (jangan pakai fokus header: fokus pindah ke
    // kolom di layar search sehingga header kehilangan fokus dan layar tertutup sendiri)
    var searchOpen by remember { mutableStateOf(false) }
    val isSearching = searchOpen || searchQuery.isNotEmpty()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var searchResults by remember { mutableStateOf<List<DramaItem>>(emptyList()) }
    var sortMode by remember { mutableStateOf(com.clipku.drama.ui.components.SortMode.RELEVAN) }
    var searchLoading by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var providerFilter by remember { mutableStateOf<String?>(null) } // null = semua
    var genreFilter by remember { mutableStateOf<String?>(null) } // null = semua genre
    var didYouMean by remember { mutableStateOf<String?>(null) } // koreksi typo
    val searchScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var suggestJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val searchHistory by com.clipku.drama.data.history.SearchHistory.queries.collectAsState()

    fun doSuggest(q: String) {
        suggestJob?.cancel()
        if (q.trim().length < 2) {
            suggestions = emptyList()
            return
        }
        // panaskan index lokal di background agar saran lokal tersedia
        searchScope.launch {
            try { com.clipku.drama.data.sonzaix.SonzaixRepository.warmIndex() } catch (_: Exception) { }
        }
        suggestJob = searchScope.launch {
            kotlinx.coroutines.delay(350)
            try {
                suggestions = com.clipku.drama.data.sonzaix.SonzaixRepository.getSuggestions(q)
            } catch (_: Exception) { }
        }
    }

    /** Lengkapi rating/views hasil pencarian tanpa menghalangi tampilan. */
    fun enrichSearchAsync(items: List<com.clipku.drama.data.model.DramaItem>) {
        if (items.isEmpty()) return
        val snapshot = items
        searchScope.launch {
            try {
                val enriched =
                    com.clipku.drama.data.sonzaix.SonzaixRepository.enrichWithWebMeta(snapshot)
                // hanya update bila hasil belum berubah
                if (searchResults.map { it.id } == snapshot.map { it.id }) {
                    searchResults = enriched
                }
            } catch (_: Exception) { }
        }
    }

    /** Eksekusi pencarian (langsung). saveHistory=false untuk saran/history tap biar tidak dobel. */
    fun execSearch(q: String, saveHistory: Boolean = true) {
        searchJob?.cancel()
        suggestJob?.cancel()
        suggestions = emptyList()
        if (q.isBlank()) {
            searchResults = emptyList()
            searchLoading = false
            return
        }
        if (saveHistory) com.clipku.drama.data.history.SearchHistory.add(q)
        searchLoading = true
        searchJob = searchScope.launch {
            try {
                val res = com.clipku.drama.data.sonzaix.SonzaixRepository.getContents(null, q, 1, 30)
                searchResults = res.items
                providerFilter = null
                genreFilter = null
                // "Maksud Anda": tawarkan koreksi bila hasil sedikit
                didYouMean = if (res.items.size < 5) {
                    try { com.clipku.drama.data.sonzaix.SonzaixRepository.suggestCorrection(q) }
                    catch (_: Exception) { null }
                } else null
                enrichSearchAsync(res.items)
            } catch (_: Exception) {
                searchResults = emptyList()
            }
            searchLoading = false
        }
    }

    fun doSearch(q: String) = execSearch(q, saveHistory = true)

    /** Dipanggil tiap ketikan: suggest + pencarian debounce. */
    fun onSearchType(q: String) {
        searchQuery = q
        searchJob?.cancel()
        // ketik ulang = hasil lama tidak relevan; kosongkan agar saran terlihat
        if (q.isNotBlank()) searchResults = emptyList()
        didYouMean = null
        genreFilter = null
        if (q.isBlank()) {
            searchResults = emptyList()
            searchLoading = false
            suggestions = emptyList()
            suggestJob?.cancel()
            return
        }
        doSuggest(q)
        searchJob = searchScope.launch {
            kotlinx.coroutines.delay(400)
            // jangan simpan history untuk tiap ketikan; disimpan saat user submit/tap saran
            try {
                searchLoading = true
                val res = com.clipku.drama.data.sonzaix.SonzaixRepository.getContents(null, q, 1, 30)
                searchResults = res.items
                providerFilter = null
                didYouMean = if (res.items.size < 5) {
                    try { com.clipku.drama.data.sonzaix.SonzaixRepository.suggestCorrection(q) }
                    catch (_: Exception) { null }
                } else null
                enrichSearchAsync(res.items)
            } catch (_: Exception) {
                searchResults = emptyList()
            }
            searchLoading = false
        }
    }

    /** Tutup layar pencarian dan reset state. */
    fun closeSearch() {
        focusManager.clearFocus()
        searchOpen = false
        searchQuery = ""
        searchResults = emptyList()
        suggestions = emptyList()
        providerFilter = null
        genreFilter = null
        didYouMean = null
    }

    // infinite scroll trigger: pakai total item LazyColumn, bukan jumlah drama
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { idx ->
                val total = listState.layoutInfo.totalItemsCount
                if (idx != null && total > 0 && idx >= total - 3) vm.loadMore()
            }
    }

    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        state = pullState,
        onRefresh = { vm.refresh() },
        // spinner lingkaran di tengah-atas dihapus (permintaan user):
        // skeleton cards sudah jadi indikator loading
        indicator = { },
        modifier = Modifier.fillMaxSize().background(BgDeep)
    ) {
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        InlineSearchHeader(
                query = searchQuery,
                onQueryChange = { onSearchType(it) },
                onFocusChange = { if (it) searchOpen = true },
                onSearchSubmit = { doSearch(searchQuery) },
                onCloseSearch = { closeSearch() },
                onSettingsClick = onSettingsClick
            )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
        // ===== HERO CAROUSEL =====
        if (state.heroItems.isNotEmpty()) {
            item {
                HeroCarousel(
                    items = state.heroItems,
                    onPlay = { onDramaClick(it) },
                    onClick = { onDramaClick(it) }
                )
            }
        }

        // ===== PROVIDER CHIPS (elegan) =====
        if (state.providers.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Str.providers, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            Str.exploreFeatures,
                            color = Coral, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = onExploreClick)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            ProviderChip(
                                label = Str.all,
                                selected = state.selectedProvider == null,
                                onClick = { vm.selectProvider(null) }
                            )
                        }
                        items(state.providers) { p ->
                            ProviderChip(
                                label = p.providerName.ifBlank { cleanProviderName(p.providerSlug) },
                                selected = state.selectedProvider == p.providerSlug,
                                onClick = { vm.selectProvider(p.providerSlug) }
                            )
                        }
                    }
                }
            }
        }

        // ===== TERBARU horizontal (feed khusus drama terbaru semua provider) =====
        if (state.latestItems.isNotEmpty() && state.selectedProvider == null) {
            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    SectionHeader(title = Str.latest)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.latestItems.filter { it.id !in heroIds }) { item ->
                            TrendingCard(item = item, onClick = { onDramaClick(item) })
                        }
                    }
                }
            }
        }

        // ===== TRENDING horizontal =====
        if (state.trendingItems.isNotEmpty() && state.selectedProvider == null) {
            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    SectionHeader(title = Str.trendingNow)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trendingVisible) { item ->
                            TrendingCard(item = item, onClick = { onDramaClick(item) })
                        }
                    }
                }
            }
        }

        // ===== JELAJAHI grid =====
        item {
            SectionHeader(
                title = if (state.selectedProvider == null) Str.browse else Str.filterResults,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (state.isLoading && state.items.isEmpty()) {
            // Skeleton loading
            items(6) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BgChip.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }

        val rows = gridItems.chunked(3)
        items(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    DramaPosterCard(
                        item = item,
                        onClick = { onDramaClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        if (state.isLoadingMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Coral, modifier = Modifier.size(28.dp))
                }
            }
        }

        state.error?.let { err ->
            item {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(err, color = TextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.refresh() },
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) { Text(Str.retry) }
                }
            }
        }
    }
    }

    // ===== Layar pencarian full-screen ala aplikasi drama =====
    if (isSearching) {
        androidx.activity.compose.BackHandler { closeSearch() }
        val searchProviders = androidx.compose.runtime.remember(searchResults) {
            searchResults.map { it.providerName }.distinct()
        }
        val searchTopGenres = androidx.compose.runtime.remember(searchResults) {
            searchResults.flatMap { it.genres }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .take(8).map { it.key }
        }
        val genreFilteredResults = androidx.compose.runtime.remember(searchResults, providerFilter, genreFilter) {
            val f = if (providerFilter == null) searchResults
                else searchResults.filter { it.providerName == providerFilter }
            if (genreFilter == null) f else f.filter { genreFilter in it.genres }
        }
        val sortedResults = androidx.compose.runtime.remember(genreFilteredResults, sortMode) {
            when (sortMode) {
                com.clipku.drama.ui.components.SortMode.RATING ->
                    genreFilteredResults.sortedByDescending { it.rating }
                com.clipku.drama.ui.components.SortMode.DITONTON ->
                    genreFilteredResults.sortedByDescending { it.viewCount }
                com.clipku.drama.ui.components.SortMode.EPISODE_TERBANYAK ->
                    genreFilteredResults.sortedByDescending { it.episodeCount }
                com.clipku.drama.ui.components.SortMode.JUDUL_AZ ->
                    genreFilteredResults.sortedBy { it.title.lowercase() }
                else -> genreFilteredResults
            }
        }
        // pilih saran/riwayat/trending: isi kolom lalu cari
        fun pickSearch(s: String) {
            searchQuery = s
            doSearch(s)
        }
        com.clipku.drama.ui.components.DramaSearchScreen(
            query = searchQuery,
            onQueryChange = { onSearchType(it) },
            onBack = { closeSearch() },
            onSubmit = { doSearch(searchQuery) },
            isLoading = searchLoading,
            results = sortedResults,
            resultCountText = "${genreFilteredResults.size} ${Str.resultsFor} \"$searchQuery\"",
            suggestions = suggestions,
            onSuggestionPick = { pickSearch(it) },
            history = searchHistory,
            onHistoryPick = { pickSearch(it) },
            onHistoryRemove = { com.clipku.drama.data.history.SearchHistory.remove(it) },
            onHistoryClear = { com.clipku.drama.data.history.SearchHistory.clear() },
            sortMode = sortMode,
            onSortChange = { sortMode = it },
            providers = searchProviders,
            providerFilter = providerFilter,
            onProviderFilter = { providerFilter = it },
            genres = searchTopGenres,
            genreFilter = genreFilter,
            onGenreFilter = { genreFilter = it },
            didYouMean = didYouMean,
            onDramaClick = { closeSearch(); onDramaClick(it) }
        )
    }

    // Dialog pilih tema

    }
    }
}

@Composable
private fun ProviderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Coral else BgChip)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarousel(
    items: List<DramaItem>,
    onPlay: (DramaItem) -> Unit,
    onClick: (DramaItem) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    // auto-scroll
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val item = items[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 8.2f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .clickable { onClick(item) }
            ) {
                FastImage(
                    url = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                            startY = 200f
                        )
                    )
                )
                // tombol play melayang
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 20.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Coral)
                        .clickable { onPlay(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp).fillMaxWidth(0.72f)
                ) {
                    Text(
                        item.title, color = Color.White, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingBadge(item.rating)
                        if (item.providerName.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Text(item.providerName, color = Color.White.copy(0.75f), fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        // dots
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(items.size) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (i == pagerState.currentPage) 18.dp else 6.dp, 6.dp)
                        .clip(CircleShape)
                        .background(if (i == pagerState.currentPage) Coral else BgChip)
                )
            }
        }
    }
}

@Composable
private fun TrendingCard(item: DramaItem, onClick: () -> Unit) {
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 2.6f)
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
        ) {
            FastImage(
                url = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            RatingBadge(item.rating, modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title, color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp
        )
    }
}
