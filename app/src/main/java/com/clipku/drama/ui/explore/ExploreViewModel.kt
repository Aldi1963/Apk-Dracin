package com.clipku.drama.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipku.drama.data.model.FeatureItem
import com.clipku.drama.data.model.Provider
import com.clipku.drama.data.model.ProviderFeature
import com.clipku.drama.data.sonzaix.SonzaixRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class ExploreUiState(
    val providers: List<Provider> = emptyList(),
    val selectedSlug: String? = null,
    val selectedName: String = "",
    val features: List<ProviderFeature> = emptyList(),
    val selectedFeature: Int = 0,
    val items: List<FeatureItem> = emptyList(),
    val isLabelOnly: Boolean = false,
    val isLoadingProviders: Boolean = true,
    val isLoadingFeatures: Boolean = false,
    val isLoadingItems: Boolean = false,
    val error: String? = null,
    val isSearchMode: Boolean = false,
    val searchQuery: String = ""
)

class ExploreViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init { loadProviders() }

    private fun loadProviders() {
        viewModelScope.launch {
            try {
                val providers = SonzaixRepository.getProviders()
                _uiState.value = _uiState.value.copy(
                    providers = providers, isLoadingProviders = false
                )
                // auto-pilih provider pertama yg kemungkinan punya fitur
                providers.firstOrNull()?.let { selectProvider(it.providerSlug, it.providerName) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingProviders = false,
                    error = "Gagal memuat provider: ${e.message}"
                )
            }
        }
    }

    fun selectProvider(slug: String, name: String) {
        _uiState.value = _uiState.value.copy(
            selectedSlug = slug, selectedName = name,
            features = emptyList(), selectedFeature = 0,
            items = emptyList(), isLoadingFeatures = true, error = null
        )
        viewModelScope.launch {
            try {
                val res = SonzaixRepository.getProviderFeatures(slug)
                // filter: hanya tampilkan feature yg namanya bermakna untuk konten
                val feats = res.features
                _uiState.value = _uiState.value.copy(
                    features = feats, isLoadingFeatures = false
                )
                if (feats.isNotEmpty()) loadFeature(0)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingFeatures = false,
                    error = "Fitur provider belum tersedia."
                )
            }
        }
    }

    fun selectFeature(index: Int) {
        if (index == _uiState.value.selectedFeature) return
        val feature = _uiState.value.features.getOrNull(index)
        val isSearch = feature?.name?.equals("Pencarian", ignoreCase = true) == true
        _uiState.value = _uiState.value.copy(
            selectedFeature = index,
            items = emptyList(),
            isSearchMode = isSearch,
            searchQuery = "",
            error = null
        )
        if (!isSearch) loadFeature(index)
    }

    fun searchInProvider(query: String) {
        val slug = _uiState.value.selectedSlug ?: return
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(items = emptyList(), error = null)
            return
        }
        _uiState.value = _uiState.value.copy(isLoadingItems = true, error = null, searchQuery = query)
        viewModelScope.launch {
            try {
                val res = SonzaixRepository.getContents(slug, query, 1, 30)
                val items = res.items.map { d ->
                    FeatureItem(id = d.id, title = d.title, imageUrl = d.posterUrl)
                }
                _uiState.value = _uiState.value.copy(
                    items = items,
                    isLabelOnly = false,
                    isLoadingItems = false,
                    error = if (items.isEmpty()) "Tidak ditemukan untuk \"$query\"." else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingItems = false,
                    error = "Gagal mencari: ${e.message}"
                )
            }
        }
    }

    private fun loadFeature(index: Int) {
        val feature = _uiState.value.features.getOrNull(index) ?: return
        _uiState.value = _uiState.value.copy(isLoadingItems = true, error = null)
        viewModelScope.launch {
            try {
                val json = SonzaixRepository.getRawJson(feature.url)
                    ?: throw IllegalStateException("Respons kosong")
                val (items, labelOnly) = parseFeatureItems(json)
                _uiState.value = _uiState.value.copy(
                    items = items, isLabelOnly = labelOnly,
                    isLoadingItems = false,
                    error = if (items.isEmpty()) "Tidak ada data untuk fitur ini." else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingItems = false,
                    error = "Gagal memuat: ${e.message}"
                )
            }
        }
    }

    fun retry() {
        val slug = _uiState.value.selectedSlug ?: return
        selectProvider(slug, _uiState.value.selectedName)
    }

    companion object {
        private val TITLE_KEYS = listOf("title", "name", "label", "text")
        private val IMAGE_KEYS = listOf("cover", "poster", "posterUrl", "image", "imageUrl", "thumbnail", "thumb", "pic", "img", "banner", "coverUrl")

        fun parseFeatureItems(json: String): Pair<List<FeatureItem>, Boolean> {
            return try {
                val root = JSONObject(json)
                val arr: JSONArray = when {
                    root.has("data") && root.get("data") is JSONArray -> root.getJSONArray("data")
                    else -> {
                        // cari array pertama di dalam root
                        var found: JSONArray? = null
                        val keys = root.keys()
                        while (keys.hasNext()) {
                            val v = root.opt(keys.next())
                            if (v is JSONArray) { found = v; break }
                        }
                        found ?: return Pair(emptyList(), false)
                    }
                }
                val items = mutableListOf<FeatureItem>()
                var withImage = 0
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val title = TITLE_KEYS.firstNotNullOfOrNull { k -> o.optString(k).ifBlank { null } } ?: continue
                    val image = IMAGE_KEYS.firstNotNullOfOrNull { k -> o.optString(k).ifBlank { null } }
                    val id = o.optString("id").ifBlank { o.optString("slug") }
                    if (image != null) withImage++
                    items.add(FeatureItem(id = id, title = title, imageUrl = image))
                }
                Pair(items, withImage == 0)
            } catch (_: Exception) {
                Pair(emptyList(), false)
            }
        }
    }
}
