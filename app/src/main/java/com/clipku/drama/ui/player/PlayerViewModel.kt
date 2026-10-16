package com.clipku.drama.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clipku.drama.data.sonzaix.SonzaixRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val streamUrl: String? = null,
    val streamType: String? = null,
    // BUGFIX v61: default false — expand dari mini player TIDAK reload,
    // jadi loader tidak boleh nyangkut true dan video tidak boleh ke-pause.
    val isLoading: Boolean = false,
    val error: String? = null,
    val qualities: List<com.clipku.drama.data.model.VideoQuality> = emptyList(),
    val currentQuality: String? = null,
    val subtitleUri: String? = null,
    val subtitleEnabled: Boolean = true,
    val playerError: String? = null
)

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    private var contentId: String = ""
    private var episode: Int = 1
    private var loadJob: kotlinx.coroutines.Job? = null
    private var loadSeq = 0

    fun load(contentId: String, ep: Int) {
        this.contentId = contentId
        this.episode = ep
        loadJob?.cancel()
        // Jeda audio episode lama SEGERA saat load baru dimulai (ganti episode /
        // kualitas / retry). Ditaruh di sini, BUKAN di LaunchedEffect(isLoading),
        // agar expand dari mini player tidak ikut ke-pause.
        try { MiniPlayerController.playerOrNull()?.pause() } catch (_: Exception) { }
        val seq = ++loadSeq
        val saverForced = isDataSaverForced()
        loadJob = viewModelScope.launch {
            _uiState.value = PlayerUiState(isLoading = true)
            val res = StreamLoader.loadStream(
                getApplication(), contentId, ep,
                subtitleLang(), savedQuality(), saverForced
            )
            if (seq != loadSeq) return@launch // basi: episode sudah ganti
            if (res.ok) {
                _uiState.value = PlayerUiState(
                    streamUrl = res.url, streamType = res.type, isLoading = false,
                    qualities = res.qualities,
                    currentQuality = res.initialQuality?.label,
                    subtitleUri = res.subtitleUri
                )
                // bila kualitas tersimpan beda dari default, langsung pakai
                val initial = res.initialQuality
                if (initial != null && initial.label != res.qualities.firstOrNull()?.label) {
                    switchQuality(initial, {}, save = !saverForced)
                }
            } else {
                _uiState.value = PlayerUiState(
                    isLoading = false,
                    error = res.errorMessage ?: "URL stream tidak tersedia."
                )
            }
        }
    }

    fun setPlayerError(msg: String?) {
        _uiState.value = _uiState.value.copy(playerError = msg)
    }

    fun toggleSubtitle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(subtitleEnabled = enabled)
        // persist agar auto-next controller (saat minimized) ikut preferensi ini
        try { prefs().edit().putBoolean("subtitle_enabled", enabled).apply() } catch (_: Exception) { }
    }

    private fun prefs() = getApplication<Application>()
        .getSharedPreferences("clipku_player", android.content.Context.MODE_PRIVATE)

    /** Kualitas terakhir yang dipilih user (mis. "720p"). */
    fun savedQuality(): String? =
        prefs().getString("last_quality", null)

    /** Bahasa subtitle: "id" / "en" / "off". */
    fun subtitleLang(): String = prefs().getString("subtitle_lang", "id") ?: "id"
    fun setSubtitleLang(lang: String) {
        prefs().edit().putString("subtitle_lang", lang).apply()
    }

    /** Ukuran subtitle: 0=kecil, 1=normal, 2=besar. */
    fun subtitleSize(): Int = prefs().getInt("subtitle_size", 1)
    fun setSubtitleSize(size: Int) {
        prefs().edit().putInt("subtitle_size", size).apply()
    }

    private fun saveQuality(label: String) {
        prefs().edit().putString("last_quality", label).apply()
    }

    /** true bila hemat kuota aktif DAN jaringan seluler (bukan WiFi). */
    private fun isDataSaverForced(): Boolean {
        return try {
            com.clipku.drama.data.prefs.AppSettings.dataSaver.value &&
                com.clipku.drama.data.prefs.AppSettings.isCellular(getApplication())
        } catch (_: Exception) { false }
    }

    /** Ganti kualitas; kembalikan true bila URL langsung tersedia (tanpa fetch ulang). */
    fun switchQuality(
        q: com.clipku.drama.data.model.VideoQuality,
        onNeedReload: () -> Unit,
        save: Boolean = true
    ) {
        if (save) saveQuality(q.label)
        if (q.url.startsWith("__quality__:")) {
            // BiliTV: fetch ulang dengan param quality
            val quality = q.url.substringAfter("__quality__:")
            viewModelScope.launch {
                try {
                    val res = SonzaixRepository.getStream(contentId, episode, mapOf("quality" to quality))
                    val url = res.url
                    if (!url.isNullOrBlank()) {
                        _uiState.value = _uiState.value.copy(
                            streamUrl = url, streamType = res.type,
                            currentQuality = q.label
                        )
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    _uiState.value = _uiState.value.copy(playerError = "Gagal ganti kualitas.")
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                streamUrl = q.url,
                streamType = if (q.url.contains(".m3u8")) "hls" else "mp4",
                currentQuality = q.label
            )
            onNeedReload()
        }
    }
}
