package com.clipku.drama.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipku.drama.data.model.DramaDetail
import com.clipku.drama.data.sonzaix.SonzaixRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val detail: DramaDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class DetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun load(slug: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            try {
                val detail = SonzaixRepository.getDetail(slug)
                _uiState.value = DetailUiState(detail = detail, isLoading = false)
                // lengkapi rating/views tanpa menghalangi tampilan
                try {
                    val enriched = SonzaixRepository.enrichDetailWithWebMeta(detail)
                    if (enriched.rating > 0 || enriched.viewCount > 0) {
                        _uiState.value = DetailUiState(detail = enriched, isLoading = false)
                    }
                } catch (_: Exception) { }
            } catch (e: Exception) {
                _uiState.value = DetailUiState(isLoading = false, error = "Gagal memuat detail: ${e.message}")
            }
        }
    }
}
