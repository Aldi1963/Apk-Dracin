package com.clipku.drama.ui.explore
import com.clipku.drama.ui.theme.Str

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.clipku.drama.data.sonzaix.SonzaixRepository
import com.clipku.drama.data.model.FeatureItem
import com.clipku.drama.ui.components.FastImage
import com.clipku.drama.ui.components.ProviderAvatar
import com.clipku.drama.ui.components.GenreChipRow
import com.clipku.drama.ui.components.GenreList
import com.clipku.drama.ui.components.matchGenre
import com.clipku.drama.ui.components.SortMode
import com.clipku.drama.ui.theme.BgCard
import com.clipku.drama.ui.theme.BgChip
import com.clipku.drama.ui.theme.BgDeep
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.TextMain
import com.clipku.drama.ui.theme.TextMuted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onBack: () -> Unit,
    onDramaClick: (com.clipku.drama.data.model.DramaItem) -> Unit,
    vm: ExploreViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val gridCols by com.clipku.drama.data.prefs.AppSettings.gridColumns.collectAsState()
    var resolving by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var selectedGenre by remember { mutableStateOf(GenreList[0]) }
    LaunchedEffect(state.selectedSlug, state.selectedFeature) { selectedGenre = GenreList[0] }
    val filteredItems = remember(state.items, selectedGenre) {
        if (selectedGenre.keywords.isEmpty()) state.items
        else state.items.filter { matchGenre(it.title, selectedGenre) }
    }

    fun resolveAndOpen(item: FeatureItem) {
        if (resolving) return
        resolving = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // cari judul di katalog kita
                val res = SonzaixRepository.getContents(null, item.title.take(40), 1, 5)
                val q = item.title.lowercase()
                // verifikasi kecocokan judul agar tidak membuka drama yang salah
                val match = res.items.firstOrNull { r ->
                    val t = r.title.lowercase()
                    q.length >= 4 && (t.contains(q.take(12)) || q.contains(t.take(12)))
                }
                withContext(Dispatchers.Main) {
                    resolving = false
                    if (match != null) onDramaClick(match)
                    else notice = "“${item.title.take(30)}” belum ada di katalog."
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    resolving = false
                    notice = Str.searchCatalogFailed
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(BgDeep)) {
        // header
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = TextMain)
            }
            Text(Str.exploreProviders, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }

        // ---- provider picker (grid elegan) ----
        if (state.isLoadingProviders) {
            Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Coral, modifier = Modifier.size(28.dp))
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.providers) { p ->
                    val selected = p.providerSlug == state.selectedSlug
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Coral.copy(alpha = 0.16f) else BgCard)
                            .clickable { vm.selectProvider(p.providerSlug, p.providerName) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .width(72.dp)
                    ) {
                        ProviderAvatar(
                            name = p.providerName.ifBlank { p.providerSlug },
                            logoUrl = p.logoUrl,
                            size = 44
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            p.providerName.ifBlank { p.providerSlug },
                            color = if (selected) Coral else TextMain,
                            fontSize = 10.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- feature tabs ----
        when {
            state.isLoadingFeatures -> {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Coral, modifier = Modifier.size(28.dp))
                }
            }
            state.features.isNotEmpty() -> {
                ScrollableTabRow(
                    selectedTabIndex = state.selectedFeature,
                    containerColor = BgDeep,
                    contentColor = TextMain,
                    edgePadding = 16.dp
                ) {
                    state.features.forEachIndexed { i, f ->
                        Tab(
                            selected = i == state.selectedFeature,
                            onClick = { vm.selectFeature(i) },
                            text = {
                                Text(
                                    prettyFeatureName(f.name),
                                    fontSize = 13.sp,
                                    fontWeight = if (i == state.selectedFeature) FontWeight.Bold else FontWeight.Normal,
                                    color = if (i == state.selectedFeature) Coral else TextMuted
                                )
                            }
                        )
                    }
                }

                // ---- filter genre ----
                if (!state.isSearchMode && !state.isLabelOnly && state.items.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    GenreChipRow(
                        selected = selectedGenre,
                        onSelect = { selectedGenre = it }
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // ---- konten feature ----
                if (state.isSearchMode) {
                    // Mode pencarian: tampilkan search bar
                    var query by remember(state.selectedSlug, state.selectedFeature) { mutableStateOf("") }
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(BgCard)
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 14.sp),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onSearch = { vm.searchInProvider(query) }
                                ),
                                decorationBox = { inner ->
                                    if (query.isEmpty()) Text(Str.searchIn(state.selectedName), color = TextMuted, fontSize = 14.sp)
                                    inner()
                                }
                            )
                            if (query.isNotEmpty()) {
                                androidx.compose.material3.TextButton(onClick = { vm.searchInProvider(query) }) {
                                    Text(Str.search, color = Coral, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                        when {
                            state.isLoadingItems -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Coral)
                                }
                            }
                            state.error != null && state.items.isEmpty() && state.searchQuery.isNotEmpty() -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(state.error!!, color = TextMuted, fontSize = 14.sp)
                                }
                            }
                            state.items.isNotEmpty() -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(gridCols),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.items) { item ->
                                        FeatureCard(item = item, onClick = { resolveAndOpen(item) })
                                    }
                                }
                            }
                            else -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Search, null, tint = TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(12.dp))
                                        Text(Str.typeKeywordPressSearch, color = TextMuted, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                } else when {
                    state.isLoadingItems -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Coral)
                        }
                    }
                    state.isLabelOnly -> {
                        // label chips (tabs/kategori tanpa gambar)
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.items.chunked(2)) { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { item ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(BgCard)
                                                .padding(14.dp)
                                        ) {
                                            Text(item.title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridCols),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredItems) { item ->
                                FeatureCard(item = item, onClick = { resolveAndOpen(item) })
                            }
                        }
                    }
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
                        onClick = { vm.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) { Text(Str.retry) }
                }
            }
        }
    }

    // notice snackbar sederhana
    notice?.let { msg ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgChip)
                    .clickable { notice = null }
                    .padding(14.dp)
            ) {
                Text(msg, color = TextMain, fontSize = 13.sp)
            }
        }
    }
    if (resolving) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = Coral) }
    }
}

@Composable
private fun FeatureCard(item: FeatureItem, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 2.8f)
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
        ) {
            FastImage(
                url = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                        startY = 200f
                    )
                )
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title, color = TextMain, fontSize = 11.sp,
            fontWeight = FontWeight.Medium, maxLines = 2,
            overflow = TextOverflow.Ellipsis, lineHeight = 14.sp
        )
    }
}

private fun prettyFeatureName(raw: String): String {
    val map = mapOf(
        "banners" to "Banner", "tabs" to "Tab", "categories" to Str.category,
        "tags" to "Tag", "collections" to Str.collection, "rankings" to Str.ranking,
        "trending" to Str.trending, "languages" to Str.language, "home" to Str.home,
        "search" to Str.search, "detail" to Str.details, "episodes" to Str.episode,
        "recommend" to Str.recommended, "popular" to Str.popular, "latest" to Str.latest,
        "genres" to Str.genre, "filters" to Str.filter
    )
    val lower = raw.lowercase()
    map.forEach { (k, v) -> if (lower.contains(k)) return v }
    return raw.replaceFirstChar { it.uppercaseChar() }
}
