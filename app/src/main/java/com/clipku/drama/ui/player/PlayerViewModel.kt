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
    val isLoading: Boolean = true,
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
        val seq = ++loadSeq
        loadJob = viewModelScope.launch {
            _uiState.value = PlayerUiState(isLoading = true)
            try {
                val res = SonzaixRepository.getStream(contentId, ep)
                if (seq != loadSeq) return@launch // basi: episode sudah ganti
                // sonzaix mengembalikan URL absolut; pakai langsung
                val url = res.url
                if (!url.isNullOrBlank()) {
                    // ambil subtitle paralel (khusus provider yang dukung, cth BiliTV)
                    var subUri: String? = null
                    try {
                        val subLang = subtitleLang()
                        val sub = if (subLang == "off") null
                            else SonzaixRepository.getSubtitle(contentId, ep, subLang)
                        subUri = when {
                            !sub?.url.isNullOrBlank() -> sub?.url
                            !sub?.vttContent.isNullOrBlank() -> {
                                val f = java.io.File(getApplication<Application>().cacheDir, "sub")
                                f.mkdirs()
                                val sf = java.io.File(f, "sub_${System.currentTimeMillis()}.vtt")
                                sf.writeText(sub?.vttContent ?: "")
                                android.net.Uri.fromFile(sf).toString()
                            }
                            else -> null
                        }
                    } catch (_: Exception) { }
                    val saved = savedQuality()
                    // hemat kuota: paksa kualitas terendah saat data seluler (tanpa menimpa yang diingat)
                    val saverForced = isDataSaverForced()
                    val initial = if (saverForced) {
                        lowestQuality(res.qualities) ?: res.qualities.firstOrNull()
                    } else {
                        res.qualities.find { it.label == saved } ?: res.qualities.firstOrNull()
                    }
                    _uiState.value = PlayerUiState(
                        streamUrl = url, streamType = res.type, isLoading = false,
                        qualities = res.qualities,
                        currentQuality = initial?.label,
                        subtitleUri = subUri
                    )
                    // bila kualitas tersimpan beda dari default, langsung pakai
                    if (initial != null && initial.label != res.qualities.firstOrNull()?.label) {
                        switchQuality(initial, {}, save = !saverForced)
                    }
                } else {
                    _uiState.value = PlayerUiState(
                        isLoading = false,
                        error = res.message ?: "URL stream tidak tersedia."
                    )
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val friendly = when {
                    msg.contains("502") -> "Video tidak tersedia saat ini. Server sedang bermasalah atau video sudah dihapus."
                    msg.contains("404") -> "Video tidak ditemukan."
                    msg.contains("timeout", ignoreCase = true) -> "Koneksi timeout. Periksa internet kamu."
                    else -> "Gagal memuat video. Coba lagi."
                }
                _uiState.value = PlayerUiState(
                    isLoading = false,
                    error = friendly
                )
            }
        }
    }

    fun setPlayerError(msg: String?) {
        _uiState.value = _uiState.value.copy(playerError = msg)
    }

    fun toggleSubtitle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(subtitleEnabled = enabled)
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

    /** Kualitas terendah dari daftar (berdasarkan angka di label, mis. "480p"). */
    private fun lowestQuality(
        qualities: List<com.clipku.drama.data.model.VideoQuality>
    ): com.clipku.drama.data.model.VideoQuality? {
        if (qualities.isEmpty()) return null
        return qualities.minByOrNull { q ->
            Regex("\\d+").find(q.label)?.value?.toIntOrNull() ?: Int.MAX_VALUE
        } ?: qualities.lastOrNull()
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
