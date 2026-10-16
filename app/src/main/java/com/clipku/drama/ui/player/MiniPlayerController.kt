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
import com.clipku.drama.data.prefs.AppSettings
import com.clipku.drama.ui.theme.Str
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private var appCtx: Context? = null

    /** Scope milik aplikasi: untuk auto-next & sleep timer saat full player tidak terlihat. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * true saat full player sedang terlihat. Di-set oleh PlayerScreen via
     * DisposableEffect. Dipakai agar auto-next tidak dobel dengan countdown
     * milik full player.
     */
    var fullPlayerVisible: Boolean = false

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    /** Menit sleep timer yang aktif (null = mati). */
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()
    private var sleepJob: Job? = null

    /** Hook error player untuk UI yang sedang aktif (di-set oleh PlayerScreen). */
    var onPlayerError: ((String) -> Unit)? = null

    /** Hook saat sleep timer fire (untuk hint UI di full player). */
    var onSleepTimerFired: (() -> Unit)? = null

    /** ExoPlayer app-scoped: dibuat sekali, dipakai ulang antar sesi & antar layar. */
    fun ensurePlayer(appContext: Context): ExoPlayer {
        playerRef?.let { return it }
        appCtx = appContext.applicationContext
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
                            // Auto-next saat MINIMIZED: full player tidak terlihat,
                            // jadi countdown milik layar tidak jalan — controller yang majukan.
                            if (!fullPlayerVisible) {
                                _session.value?.let { s ->
                                    if (s.ep < s.totalEp && AppSettings.autoNext.value) {
                                        advanceToNextEpisode(s)
                                    }
                                }
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

    /**
     * Maju ke episode berikutnya saat mini player aktif (full player tidak
     * terlihat). Dipanggil dari listener ENDED milik singleton.
     */
    private fun advanceToNextEpisode(s: Session) {
        val ctx = appCtx ?: return
        val p = playerRef ?: return
        scope.launch {
            try {
                val prefs = ctx.getSharedPreferences("clipku_player", Context.MODE_PRIVATE)
                val subLang = prefs.getString("subtitle_lang", "id") ?: "id"
                val subEnabled = prefs.getBoolean("subtitle_enabled", true)
                val savedQ = prefs.getString("last_quality", null)
                val saverForced = try {
                    AppSettings.dataSaver.value && AppSettings.isCellular(ctx)
                } catch (_: Exception) { false }
                val res = StreamLoader.loadStream(
                    ctx, s.contentId, s.ep + 1, subLang, savedQ, saverForced
                )
                if (!res.ok) {
                    onPlayerError?.invoke(res.errorMessage ?: "Gagal memuat episode berikutnya.")
                    return@launch
                }
                val item = StreamLoader.buildMediaItem(
                    res.url!!, res.type, res.subtitleUri, subEnabled, subLang
                )
                p.setMediaItem(item)
                p.prepare()
                // resume posisi episode baru bila pernah ditonton
                try {
                    val savedPos = WatchHistory.getPosition(s.contentId, s.ep + 1)
                    if (savedPos > 5000) p.seekTo(savedPos)
                } catch (_: Exception) { }
                updateEpisode(s.ep + 1, s.totalEp)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onPlayerError?.invoke("Gagal memuat episode berikutnya.")
            }
        }
    }

    /** Sleep timer milik aplikasi: tetap jalan walau full player di-minimize. */
    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        _sleepTimerMinutes.value = minutes
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            try { playerRef?.pause() } catch (_: Exception) { }
            _sleepTimerMinutes.value = null
            try { onSleepTimerFired?.invoke() } catch (_: Exception) { }
        }
    }

    fun cancelSleepTimer() {
        try { sleepJob?.cancel() } catch (_: Exception) { }
        sleepJob = null
        _sleepTimerMinutes.value = null
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
        cancelSleepTimer()
        try {
            playerRef?.stop()
            playerRef?.clearMediaItems()
        } catch (_: Exception) {}
        _isPlaying.value = false
        _isBuffering.value = false
        _session.value = null
    }
}
