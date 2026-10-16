package com.clipku.drama.ui.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.clipku.drama.data.model.VideoQuality
import com.clipku.drama.data.sonzaix.SonzaixRepository
import java.io.File
import kotlinx.coroutines.CancellationException

/**
 * Logika load stream yang dipakai BERSAMA oleh PlayerViewModel (full player)
 * dan MiniPlayerController (auto-next saat minimized). Perilaku identik:
 * qualities, kualitas tersimpan, data saver, subtitle.
 */
data class StreamLoadResult(
    val url: String? = null,
    val type: String? = null,
    val qualities: List<VideoQuality> = emptyList(),
    val initialQuality: VideoQuality? = null,
    val subtitleUri: String? = null,
    val errorMessage: String? = null
) {
    val ok: Boolean get() = !url.isNullOrBlank()
}

object StreamLoader {

    suspend fun loadStream(
        appContext: Context,
        contentId: String,
        ep: Int,
        subtitleLang: String,
        savedQuality: String?,
        dataSaverForced: Boolean
    ): StreamLoadResult {
        try {
            val res = SonzaixRepository.getStream(contentId, ep)
            val url = res.url
            if (url.isNullOrBlank()) {
                return StreamLoadResult(
                    errorMessage = res.message ?: "URL stream tidak tersedia."
                )
            }
            // ambil subtitle paralel (khusus provider yang dukung, cth BiliTV)
            var subUri: String? = null
            try {
                val sub = if (subtitleLang == "off") null
                    else SonzaixRepository.getSubtitle(contentId, ep, subtitleLang)
                subUri = when {
                    !sub?.url.isNullOrBlank() -> sub?.url
                    !sub?.vttContent.isNullOrBlank() -> {
                        val f = File(appContext.cacheDir, "sub")
                        f.mkdirs()
                        val sf = File(f, "sub_${System.currentTimeMillis()}.vtt")
                        sf.writeText(sub?.vttContent ?: "")
                        Uri.fromFile(sf).toString()
                    }
                    else -> null
                }
            } catch (_: Exception) { }
            // hemat kuota: paksa kualitas terendah saat data seluler (tanpa menimpa yang diingat)
            val initial = if (dataSaverForced) {
                lowestQuality(res.qualities) ?: res.qualities.firstOrNull()
            } else {
                res.qualities.find { it.label == savedQuality } ?: res.qualities.firstOrNull()
            }
            return StreamLoadResult(
                url = url, type = res.type,
                qualities = res.qualities,
                initialQuality = initial,
                subtitleUri = subUri
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val msg = e.message ?: ""
            val friendly = when {
                msg.contains("502") -> "Video tidak tersedia saat ini. Server sedang bermasalah atau video sudah dihapus."
                msg.contains("404") -> "Video tidak ditemukan."
                msg.contains("timeout", ignoreCase = true) -> "Koneksi timeout. Periksa internet kamu."
                else -> "Gagal memuat video. Coba lagi."
            }
            return StreamLoadResult(errorMessage = friendly)
        }
    }

    /** Kualitas terendah dari daftar (berdasarkan angka di label, mis. "480p"). */
    fun lowestQuality(qualities: List<VideoQuality>): VideoQuality? {
        if (qualities.isEmpty()) return null
        return qualities.minByOrNull { q ->
            Regex("\\d+").find(q.label)?.value?.toIntOrNull() ?: Int.MAX_VALUE
        } ?: qualities.lastOrNull()
    }

    /** Bangun MediaItem ExoPlayer dari hasil load (dipakai full player & controller). */
    @OptIn(UnstableApi::class)
    fun buildMediaItem(
        url: String,
        type: String?,
        subtitleUri: String?,
        subtitleEnabled: Boolean,
        subtitleLang: String
    ): MediaItem {
        val mimeType = when (type?.lowercase()) {
            "hls", "m3u8" -> MimeTypes.APPLICATION_M3U8
            "dash", "mpd" -> MimeTypes.APPLICATION_MPD
            else -> null
        }
        val builder = MediaItem.Builder().setUri(url)
        if (mimeType != null) builder.setMimeType(mimeType)
        if (subtitleEnabled && !subtitleUri.isNullOrBlank()) {
            try {
                val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUri))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(subtitleLang.ifBlank { "id" })
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                builder.setSubtitleConfigurations(listOf(subConfig))
            } catch (_: Exception) { }
        }
        return builder.build()
    }
}
