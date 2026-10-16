package com.clipku.drama.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipku.drama.data.model.DramaItem
import com.clipku.drama.data.model.Provider
import com.clipku.drama.data.sonzaix.SonzaixRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val providers: List<Provider> = emptyList(),
    val selectedProvider: String? = null,
    val items: List<DramaItem> = emptyList(),
    val heroItems: List<DramaItem> = emptyList(),
    val trendingItems: List<DramaItem> = emptyList(),
    val latestItems: List<DramaItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val nextCursor: String? = null,
    val error: String? = null
) {
    val effectiveHasMore: Boolean get() = nextCursor != null
}

class HomeViewModel : ViewModel() {
    private var mode: HomeMode = HomeMode.BERANDA

    fun applyMode(m: HomeMode) {
        if (mode == m) return
        mode = m
        refresh()
    }
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var page: Int = 1
    private var loadingMore = false

    init {
        loadProviders()
        refresh()
        loadHero()
        loadTrending()
        loadLatest()
    }

    /** Lengkapi rating/views dari web backend tanpa menghalangi tampilan awal. */
    private fun enrichAsync(
        items: List<DramaItem>,
        apply: (List<DramaItem>) -> Unit
    ) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            try {
                val enriched = SonzaixRepository.enrichWithWebMeta(items)
                apply(enriched)
            } catch (_: Exception) { }
        }
    }

    private fun loadProviders() {
        viewModelScope.launch {
            try {
                val providers = SonzaixRepository.getProviders()
                _uiState.value = _uiState.value.copy(providers = providers)
            } catch (_: Exception) { }
        }
    }

    private fun loadHero() {
        viewModelScope.launch {
            try {
                val res = SonzaixRepository.getContents(null, null, 1, 6)
                val hero = res.items.take(6)
                _uiState.value = _uiState.value.copy(heroItems = hero)
                enrichAsync(hero) { enriched ->
                    _uiState.value = _uiState.value.copy(heroItems = enriched)
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadTrending() {
        viewModelScope.launch {
            try {
                val res = SonzaixRepository.getContents(null, null, 1, 12)
                _uiState.value = _uiState.value.copy(trendingItems = res.items)
                enrichAsync(res.items) { enriched ->
                    _uiState.value = _uiState.value.copy(trendingItems = enriched)
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadLatest() {
        viewModelScope.launch {
            try {
                val res = SonzaixRepository.getLatest(1, 18)
                _uiState.value = _uiState.value.copy(latestItems = res.items)
                enrichAsync(res.items) { enriched ->
                    _uiState.value = _uiState.value.copy(latestItems = enriched)
                }
            } catch (_: Exception) { }
        }
    }

    fun selectProvider(slug: String?) {
        if (_uiState.value.selectedProvider == slug) return
        _uiState.value = _uiState.value.copy(selectedProvider = slug)
        refresh()
    }

    fun refresh() {
        page = 1
        _uiState.value = _uiState.value.copy(
            items = emptyList(), nextCursor = "init", error = null,
            isRefreshing = true, isLoading = true
        )
        loadPage()
        loadLatest()
    }

    fun loadMore() {
        if (loadingMore || !_uiState.value.effectiveHasMore || _uiState.value.isLoading) return
        loadingMore = true
        _uiState.value = _uiState.value.copy(isLoadingMore = true)
        loadPage()
    }

    private fun loadPage() {
        viewModelScope.launch {
            try {
                val res = if (mode == HomeMode.POPULER && page == 1 && _uiState.value.selectedProvider.isNullOrBlank()) {
                    // ranking asli dari endpoint rank provider
                    SonzaixRepository.getRanking(30)
                } else {
                    SonzaixRepository.getContents(
                        providerSlug = _uiState.value.selectedProvider,
                        query = null,
                        page = page,
                        limit = 18
                    )
                }
                val requestedPage = page
                page = res.nextCursor?.toIntOrNull() ?: (page + 1)
                val current = if (_uiState.value.isRefreshing) emptyList() else _uiState.value.items
                // anti-dobel: buang item yang id-nya sudah ada (overlap antar halaman)
                val combined = (current + res.items).distinctBy { it.id }
                // jaring pengaman: bila tidak ada item baru (semua dobel / API muter di tempat),
                // paksa berhenti agar spinner tidak muter selamanya
                val nextCursor = if (combined.size == current.size && current.isNotEmpty()) null else res.nextCursor
                // bila halaman 1 kosong melompong (kemungkinan gateway gagal sesaat),
                // tampilkan pesan error + tombol Coba Lagi, bukan layar kosong
                val emptyError = if (requestedPage == 1 && combined.isEmpty())
                    "Tidak ada drama dimuat. Kemungkinan koneksi ke server gagal sesaat." else null
                _uiState.value = _uiState.value.copy(
                    items = combined,
                    nextCursor = nextCursor,
                    isLoading = false,
                    isLoadingMore = false,
                    isRefreshing = false,
                    error = emptyError
                )
                enrichAsync(combined) { enriched ->
                    // hanya update bila daftar belum berubah (hindari timpa refresh baru)
                    val cur = _uiState.value.items
                    if (cur.map { it.id } == combined.map { it.id }) {
                        _uiState.value = _uiState.value.copy(items = enriched)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    isRefreshing = false,
                    error = if (_uiState.value.items.isEmpty()) "Gagal memuat: ${e.message}" else null
                )
            } finally {
                loadingMore = false
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

// legacy enum (kompatibilitas)
enum class HomeMode { BERANDA, TERBARU, POPULER }

fun cleanProviderName(slug: String): String =
    slug.split("-").joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
