package com.clipku.drama.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.clipku.drama.data.history.WatchHistory
import com.clipku.drama.data.history.WatchStats
import com.clipku.drama.ui.theme.Str
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mini player ala YouTube: ExoPlayer hidup di level aplikasi (singleton),
 * BUKAN di lifecycle PlayerScreen. Video tetap jalan saat user minimize
 * dan browsing tab lain / search / halaman detail.
 *
 * - Back dari full player = minimize (popBackStack), player tetap jalan.
 * - Tap mini bar = expand kembali ke full player (tanpa reload).
 * - Tombol X di mini bar = stop total + tutup sesi.
 */
@OptIn(UnstableApi::class)
object MiniPlayerController {

    data class Session(
        val contentId: String,
        val ep: Int,
        val totalEp: Int,
        val title: String,
        val posterUrl: String,
        val providerName: String
    ) {
        val key: String get() = "$contentId|$ep"
    }

    private var playerRef: ExoPlayer? = null

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    /** Hook error player untuk UI yang sedang aktif (di-set oleh PlayerScreen). */
    var onPlayerError: ((String) -> Unit)? = null

    /** ExoPlayer app-scoped: dibuat sekali, dipakai ulang antar sesi & antar layar. */
    fun ensurePlayer(appContext: Context): ExoPlayer {
        playerRef?.let { return it }
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Infinix GT 10 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(60000)
            .setAllowCrossProtocolRedirects(true)
        // LoadControl dengan buffer lebih besar untuk jaringan lambat
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50000,  // minBufferMs
                100000, // maxBufferMs
                2500,   // bufferForPlaybackMs
                5000    // bufferForPlaybackAfterRebufferMs
            )
            .build()
        val p = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .apply {
                playWhenReady = true
                // Listener minimal milik singleton: state flow + bookkeeping riwayat.
                // Logika UI (countdown auto-next dsb) dipasang terpisah oleh PlayerScreen.
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _isBuffering.value = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_ENDED) {
                            // tandai episode selesai (posisi = durasi penuh) untuk centang riwayat
                            _session.value?.let { s ->
                                try {
                                    val d = duration.coerceAtLeast(0)
                                    WatchHistory.saveWatch(
                                        s.contentId, s.title, s.posterUrl.ifEmpty { null },
                                        s.providerName, s.totalEp, s.ep, d, d
                                    )
                                    WatchStats.recordEpisodeDone()
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        android.util.Log.e(
                            "ClipkuPlayer",
                            "Playback error: ${error.errorCode} - ${error.message}", error
                        )
                        onPlayerError?.invoke(Str.videoFailedRetry)
                    }
                })
            }
        playerRef = p
        return p
    }

    fun playerOrNull(): ExoPlayer? = playerRef

    /**
     * Buka sesi tonton. Mengembalikan true bila ini sesi BARU (pemanggil wajib
     * load stream); false bila melanjutkan sesi yang sama (expand dari mini
     * player — JANGAN reload, video lanjut jalan).
     */
    fun openSession(
        contentId: String,
        ep: Int,
        totalEp: Int,
        title: String,
        posterUrl: String,
        providerName: String
    ): Boolean {
        val cur = _session.value
        if (cur != null && cur.key == "$contentId|$ep") {
            _session.value = cur.copy(
                totalEp = totalEp, title = title,
                posterUrl = posterUrl, providerName = providerName
            )
            return false
        }
        // ganti video: simpan posisi sesi lama dulu
        savePosition()
        _session.value = Session(contentId, ep, totalEp, title, posterUrl, providerName)
        return true
    }

    /** Perbarui episode/total (mis. ganti episode dari picker) tanpa reload. */
    fun updateEpisode(ep: Int, totalEp: Int) {
        _session.value?.let { s ->
            if (s.ep != ep || s.totalEp != totalEp) {
                _session.value = s.copy(ep = ep, totalEp = totalEp)
            }
        }
    }

    fun togglePlayPause() {
        val p = playerRef ?: return
        try { if (p.isPlaying) p.pause() else p.play() } catch (_: Exception) {}
    }

    fun savePosition() {
        val s = _session.value ?: return
        val p = playerRef ?: return
        try {
            WatchHistory.saveWatch(
                s.contentId, s.title, s.posterUrl.ifEmpty { null }, s.providerName,
                s.totalEp, s.ep, p.currentPosition, p.duration
            )
        } catch (_: Exception) {}
    }

    /** Stop total + tutup mini player. */
    fun close() {
        savePosition()
        try {
            playerRef?.stop()
            playerRef?.clearMediaItems()
        } catch (_: Exception) {}
        _isPlaying.value = false
        _isBuffering.value = false
        _session.value = null
    }
}
