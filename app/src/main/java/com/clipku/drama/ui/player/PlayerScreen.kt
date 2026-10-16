package com.clipku.drama.ui.player
import com.clipku.drama.ui.theme.Str

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.clipku.drama.ui.theme.AccentRed
import com.clipku.drama.ui.components.ClipkuLoadingIndicator
import com.clipku.drama.ui.theme.AppIcons

@OptIn(UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    contentId: String,
    ep: Int,
    totalEp: Int = 1,
    title: String,
    posterUrl: String = "",
    providerName: String = "",
    onBack: () -> Unit,
    vm: PlayerViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val autoNext by com.clipku.drama.data.prefs.AppSettings.autoNext.collectAsState()
    val doubleTapSecs by com.clipku.drama.data.prefs.AppSettings.doubleTapSecs.collectAsState()
    val tapSeekMs = doubleTapSecs * 1000L
    val context = LocalContext.current
    val activity = context as? Activity
    var isFullscreen by remember { mutableStateOf(true) }
    // Default portrait sesuai permintaan user (tidak auto-detect dari video)
    var isVideoPortrait by remember { mutableStateOf(true) }
    var orientationLocked by remember { mutableStateOf(true) }
    var currentEp by remember(contentId) { mutableStateOf(ep) }
    // reset kunci orientasi hanya saat buka video baru (bukan ganti episode)
    LaunchedEffect(contentId) { orientationLocked = false }
    var showEpisodePicker by remember { mutableStateOf(false) }
    var showNextCountdown by remember { mutableStateOf(false) }
    var countdownSec by remember { mutableStateOf(5) }
    // Back dari full player = minimize ala YouTube (video tetap jalan di mini bar).
    var showSettings by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<androidx.media3.ui.PlayerView?>(null) }
    var subSizeIdx by remember { mutableStateOf(vm.subtitleSize()) }
    // terapkan ukuran subtitle ke SubtitleView ExoPlayer
    fun applySubtitleSize() {
        val sv = playerViewRef?.subtitleView ?: return
        val sp = when (subSizeIdx) { 0 -> 14f; 2 -> 24f; else -> 18f }
        sv.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp)
    }
    var playbackSpeed by remember { mutableStateOf(1f) }
    // Sleep timer milik MiniPlayerController (app-scoped): tetap jalan saat minimize.
    val sleepMinutes by MiniPlayerController.sleepTimerMinutes.collectAsState()
    var pendingSeekPos by remember { mutableStateOf(0L) }
    var gestureHint by remember { mutableStateOf<String?>(null) }
    // Kontrol custom (pengganti controller bawaan ExoPlayer)
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    // buffering dilacak singleton agar mini bar ikut tahu
    val isBuffering by MiniPlayerController.isBuffering.collectAsState()
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0f) }
    // Indikator brightness/volume ala streaming app
    var brightLevel by remember { mutableStateOf<Float?>(null) }
    var volumeLevel by remember { mutableStateOf<Float?>(null) }
    val scope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }

    // ===== Chromecast: discovery perangkat via MediaRouter + dialog Compose =====
    var isCasting by remember { mutableStateOf(false) }
    val castContext = remember {
        try {
            com.google.android.gms.cast.framework.CastContext.getSharedInstance(context.applicationContext)
        } catch (_: Exception) { null }
    }
    var showCastPicker by remember { mutableStateOf(false) }
    var castRoutes by remember {
        mutableStateOf<List<androidx.mediarouter.media.MediaRouter.RouteInfo>>(emptyList())
    }
    val mediaRouter = remember { androidx.mediarouter.media.MediaRouter.getInstance(context) }
    val castSelector = remember {
        androidx.mediarouter.media.MediaRouteSelector.Builder()
            .addControlCategory(com.google.android.gms.cast.CastMediaControlIntent.categoryForCast("CC1AD845"))
            .build()
    }
    DisposableEffect(mediaRouter) {
        val cb = object : androidx.mediarouter.media.MediaRouter.Callback() {
            fun refresh() {
                castRoutes = try {
                    mediaRouter.routes.filter { it.matchesSelector(castSelector) && it.isEnabled }
                } catch (_: Exception) { emptyList() }
            }
            override fun onRouteAdded(
                router: androidx.mediarouter.media.MediaRouter,
                route: androidx.mediarouter.media.MediaRouter.RouteInfo
            ) = refresh()
            override fun onRouteRemoved(
                router: androidx.mediarouter.media.MediaRouter,
                route: androidx.mediarouter.media.MediaRouter.RouteInfo
            ) = refresh()
            override fun onRouteChanged(
                router: androidx.mediarouter.media.MediaRouter,
                route: androidx.mediarouter.media.MediaRouter.RouteInfo
            ) = refresh()
        }
        try {
            mediaRouter.addCallback(
                castSelector, cb,
                androidx.mediarouter.media.MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
            )
        } catch (_: Exception) { }
        cb.refresh()
        onDispose { try { mediaRouter.removeCallback(cb) } catch (_: Exception) { } }
    }

    // Auto-hide kontrol setelah 3 detik
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            kotlinx.coroutines.delay(3000)
            controlsVisible = false
        }
    }

    // bila totalEp tidak diketahui (mis. dari favorit lama), ambil dari detail
    var realTotalEp by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(totalEp) }
    LaunchedEffect(contentId) {
        realTotalEp = totalEp
        if (totalEp <= 1) {
            try {
                val d = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.clipku.drama.data.sonzaix.SonzaixRepository.getDetail(contentId)
                }
                if (d.totalEpisodes > 1) realTotalEp = d.totalEpisodes
            } catch (_: Exception) {}
        }
    }
    val effTotalEp = realTotalEp.coerceAtLeast(1)

    // Mini player: buka sesi di controller app-scoped.
    // true = sesi baru (wajib load stream); false = expand dari mini player (JANGAN reload).
    LaunchedEffect(contentId, currentEp) {
        val isNew = MiniPlayerController.openSession(
            contentId, currentEp, effTotalEp, title, posterUrl, providerName
        )
        vm.setPlayerError(null)
        if (isNew) {
            vm.load(contentId, currentEp)
            com.clipku.drama.data.history.WatchHistory.saveWatch(
                contentId, title, posterUrl.ifEmpty { null }, providerName, effTotalEp, currentEp
            )
        }
    }

    // Sinkronkan episode/total ke sesi mini player (untuk label EP & expand)
    LaunchedEffect(currentEp, effTotalEp) {
        MiniPlayerController.updateEpisode(currentEp, effTotalEp)
    }

    // Fullscreen immersive
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            WindowInsetsControllerCompat(it, it.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            it.statusBarColor = Color.Black.toArgb()
            it.navigationBarColor = Color.Black.toArgb()
        }
        val prevOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose {
            activity?.let { act ->
                WindowCompat.setDecorFitsSystemWindows(act.window, true)
                WindowInsetsControllerCompat(act.window, act.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                if (prevOrientation != null) act.requestedOrientation = prevOrientation
            }
        }
    }

    // Orientasi mengikuti video: portrait -> portrait, landscape -> landscape
    LaunchedEffect(isFullscreen, isVideoPortrait) {
        activity?.requestedOrientation = if (!isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else if (isVideoPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // Mini player ala YouTube: ExoPlayer app-scoped (singleton MiniPlayerController).
    // Video tetap jalan saat user minimize dan browsing tab lain / search / detail.
    // Dibuat sekali di sini, dipakai ulang antar sesi & antar layar.
    val exoPlayer = remember { MiniPlayerController.ensurePlayer(context.applicationContext) }

    // Listener UI milik layar ini: countdown auto-next + hook error player.
    // (bookkeeping ENDED untuk riwayat/statistik ditangani singleton agar tidak dobel)
    DisposableEffect(exoPlayer, currentEp, effTotalEp) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (currentEp < effTotalEp) {
                        countdownSec = 5
                        showNextCountdown = true
                    } else {
                        gestureHint = Str.completedNoNext
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        MiniPlayerController.onPlayerError = { msg -> vm.setPlayerError(msg) }
        MiniPlayerController.onSleepTimerFired = {
            gestureHint = Str.sleepTimerPaused
            scope.launch {
                kotlinx.coroutines.delay(2000)
                gestureHint = null
            }
        }
        onDispose {
            exoPlayer.removeListener(listener)
            MiniPlayerController.onPlayerError = null
            MiniPlayerController.onSleepTimerFired = null
        }
    }

    // ===== Chromecast: fungsi & listener sesi (di sini karena butuh exoPlayer) =====
    fun castCurrentVideo(session: com.google.android.gms.cast.framework.CastSession) {
        val url = state.streamUrl ?: return
        try {
            val mime = if (state.streamType?.lowercase() in listOf("hls", "m3u8") || url.contains(".m3u8"))
                "application/x-mpegurl" else "video/mp4"
            val metadata = com.google.android.gms.cast.MediaMetadata(
                com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MOVIE
            )
            metadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE, title)
            val mediaInfo = com.google.android.gms.cast.MediaInfo.Builder(url)
                .setStreamType(com.google.android.gms.cast.MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(mime)
                .setMetadata(metadata)
                .build()
            session.remoteMediaClient?.load(
                com.google.android.gms.cast.MediaLoadRequestData.Builder()
                    .setMediaInfo(mediaInfo).build()
            )
            exoPlayer.pause()
            gestureHint = Str.castingToTv
            scope.launch { kotlinx.coroutines.delay(2000); gestureHint = null }
        } catch (_: Exception) { }
    }
    DisposableEffect(castContext) {
        val sm = castContext?.sessionManager
        if (sm == null) return@DisposableEffect onDispose { }
        val listener = object : com.google.android.gms.cast.framework.SessionManagerListener<com.google.android.gms.cast.framework.CastSession> {
            override fun onSessionStarted(session: com.google.android.gms.cast.framework.CastSession, sessionId: String) {
                isCasting = true
                castCurrentVideo(session)
            }
            override fun onSessionEnded(session: com.google.android.gms.cast.framework.CastSession, error: Int) { isCasting = false }
            override fun onSessionStarting(session: com.google.android.gms.cast.framework.CastSession) {}
            override fun onSessionStartFailed(session: com.google.android.gms.cast.framework.CastSession, error: Int) {}
            override fun onSessionEnding(session: com.google.android.gms.cast.framework.CastSession) {}
            override fun onSessionResuming(session: com.google.android.gms.cast.framework.CastSession, sessionId: String) {}
            override fun onSessionResumed(session: com.google.android.gms.cast.framework.CastSession, wasSuspended: Boolean) { isCasting = true }
            override fun onSessionResumeFailed(session: com.google.android.gms.cast.framework.CastSession, error: Int) {}
            override fun onSessionSuspended(session: com.google.android.gms.cast.framework.CastSession, reason: Int) { isCasting = false }
        }
        sm.addSessionManagerListener(listener, com.google.android.gms.cast.framework.CastSession::class.java)
        onDispose { sm.removeSessionManagerListener(listener, com.google.android.gms.cast.framework.CastSession::class.java) }
    }
    // bila URL berubah saat casting (ganti episode/kualitas), lempar ulang ke TV
    LaunchedEffect(isCasting, state.streamUrl) {
        if (isCasting) {
            castContext?.sessionManager?.currentCastSession?.let { castCurrentVideo(it) }
        }
    }

    // BUGFIX v61: blok pause-di-isLoading DIHAPUS — expand dari mini player
    // tidak me-reload (openSession return false), jadi isLoading tidak boleh
    // mem-pause video. Pause eksplisit sudah dipindah ke awal vm.load().

    // Tandai full player terlihat: controller tidak auto-next sendiri
    // (countdown milik layar yang jalan), agar tidak dobel-advance.
    DisposableEffect(Unit) {
        MiniPlayerController.fullPlayerVisible = true
        onDispose { MiniPlayerController.fullPlayerVisible = false }
    }

    // Countdown episode berikutnya (hanya bila auto-putar aktif)
    LaunchedEffect(showNextCountdown, autoNext) {
        if (showNextCountdown && autoNext) {
            while (countdownSec > 0 && showNextCountdown) {
                kotlinx.coroutines.delay(1000)
                countdownSec--
            }
            if (showNextCountdown) {
                showNextCountdown = false
                showEpisodePicker = false
                currentEp++
            }
        }
    }

    // Update posisi & status play tiap 500ms (untuk seek bar custom)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            try {
                isPlaying = exoPlayer.isPlaying
                if (!isSeeking) {
                    positionMs = exoPlayer.currentPosition
                    val d = exoPlayer.duration.coerceAtLeast(0)
                    if (d > 0) durationMs = d
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(playbackSpeed) { exoPlayer.setPlaybackSpeed(playbackSpeed) }

    // Sleep timer dipindah ke MiniPlayerController (app-scoped) agar tetap
    // jalan saat video di-minimize. UI di bawah hanya memanggil controller.

    // Back = minimize ala YouTube (video tetap jalan), BUKAN stop.
    BackHandler {
        when {
            showEpisodePicker -> showEpisodePicker = false
            showSettings -> showSettings = false
            showCastPicker -> showCastPicker = false
            else -> onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // simpan posisi terakhir saat keluar/minimize;
            // player TIDAK di-release (milik singleton MiniPlayerController),
            // cukup lepas dari PlayerView layar ini.
            try {
                com.clipku.drama.data.history.WatchHistory.saveWatch(
                    contentId, title, posterUrl.ifEmpty { null }, providerName,
                    effTotalEp, currentEp, exoPlayer.currentPosition, exoPlayer.duration
                )
            } catch (_: Exception) {}
            try { playerViewRef?.player = null } catch (_: Exception) {}
        }
    }

    // Simpan posisi tiap 10 detik + catat statistik nonton
    LaunchedEffect(state.streamUrl) {
        while (true) {
            kotlinx.coroutines.delay(10_000)
            if (exoPlayer.isPlaying) {
                com.clipku.drama.data.history.WatchHistory.saveWatch(
                    contentId, title, posterUrl.ifEmpty { null }, providerName,
                    effTotalEp, currentEp, exoPlayer.currentPosition, exoPlayer.duration
                )
                try { com.clipku.drama.data.history.WatchStats.recordWatch(10) } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(state.streamUrl, state.streamType, state.subtitleUri, state.subtitleEnabled) {
        state.streamUrl?.let { url ->
            val mediaItem = StreamLoader.buildMediaItem(
                url, state.streamType, state.subtitleUri, state.subtitleEnabled, vm.subtitleLang()
            )
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            // Prioritas: posisi saat ganti kualitas
            if (pendingSeekPos > 1000) {
                exoPlayer.seekTo(pendingSeekPos)
                pendingSeekPos = 0L
            } else {
                // Resume dari posisi terakhir
                val savedPos = com.clipku.drama.data.history.WatchHistory.getPosition(contentId, currentEp)
                if (savedPos > 5000) {
                    exoPlayer.seekTo(savedPos)
                    gestureHint = Str.continueFrom(formatMs(savedPos))
                    scope.launch {
                        kotlinx.coroutines.delay(2500)
                        gestureHint = null
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ClipkuLoadingIndicator(size = 84.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(Str.loadingStream, color = Color.White)
                    }
                }
            }
            state.error != null || state.playerError != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            state.playerError ?: state.error!!,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { vm.setPlayerError(null); vm.load(contentId, currentEp) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                        ) { Text(Str.retry) }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onBack) { Text(Str.back) }
                    }
                }
            }
            else -> {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            // spinner buffering bawaan ExoPlayer dimatikan:
                            // diganti overlay ClipkuLoadingIndicator (branded) di bawah
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            playerViewRef = this
                        }
                    },
                    update = { applySubtitleSize() },
                    onRelease = { view -> view.player = null },
                    modifier = Modifier.fillMaxSize()
                )
                // ===== OVERLAY BUFFERING branded (pengganti spinner bawaan ExoPlayer) =====
                // tampil saat jaringan jelek / video nge-buffer; tidak menelan gesture
                if (isBuffering && !state.isLoading) {
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        ClipkuLoadingIndicator(size = 64.dp)
                    }
                }
                // ===== GESTURE OVERLAYS =====
                // Kiri: brightness (swipe vertikal) + mundur 10s (double-tap) + toggle kontrol (tap)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .fillMaxWidth(0.25f)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { controlsVisible = !controlsVisible },
                                onDoubleTap = {
                                    exoPlayer.seekTo((exoPlayer.currentPosition - tapSeekMs).coerceAtLeast(0))
                                    gestureHint = Str.seekSec(-10)
                                    scope.launch { kotlinx.coroutines.delay(1200); gestureHint = null }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            var currentBright = 0.5f
                            detectVerticalDragGestures(
                                onDragStart = {
                                    val sys = activity?.window?.attributes?.screenBrightness ?: -1f
                                    currentBright = if (sys < 0) 0.5f else sys
                                },
                                onVerticalDrag = { _, dy ->
                                    // akumulasi delta tiap callback biar ngikutin jari
                                    currentBright = (currentBright - dy / 600f).coerceIn(0.05f, 1f)
                                    activity?.window?.attributes?.let { lp ->
                                        lp.screenBrightness = currentBright
                                        activity.window.attributes = lp
                                    }
                                    brightLevel = currentBright
                                    volumeLevel = null
                                    gestureHint = null
                                },
                                onDragEnd = { scope.launch { kotlinx.coroutines.delay(1200); brightLevel = null } }
                            )
                        }
                )
                // Kanan: volume (swipe vertikal) + maju 10s (double-tap) + toggle kontrol (tap)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(0.25f)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { controlsVisible = !controlsVisible },
                                onDoubleTap = {
                                    exoPlayer.seekTo((exoPlayer.currentPosition + tapSeekMs).coerceAtMost(exoPlayer.duration.coerceAtLeast(0)))
                                    gestureHint = Str.seekSec(10)
                                    scope.launch { kotlinx.coroutines.delay(1200); gestureHint = null }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            var accDy = 0f
                            var baseVol = 0
                            var lastApplied = -1
                            detectVerticalDragGestures(
                                onDragStart = {
                                    accDy = 0f
                                    baseVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                                    lastApplied = baseVol
                                },
                                onVerticalDrag = { _, dy ->
                                    // akumulasi jarak drag: tiap 50px = 1 step, ngikutin jari dengan mulus
                                    accDy += dy
                                    val target = kotlin.math.round(baseVol - accDy / 50f).toInt()
                                        .coerceIn(0, maxVol)
                                    if (target != lastApplied) {
                                        audioManager.setStreamVolume(
                                            android.media.AudioManager.STREAM_MUSIC,
                                            target, 0
                                        )
                                        lastApplied = target
                                        volumeLevel = target.toFloat() / maxVol
                                        brightLevel = null
                                        gestureHint = null
                                    }
                                },
                                onDragEnd = { scope.launch { kotlinx.coroutines.delay(1200); volumeLevel = null } }
                            )
                        }
                )
                // Tengah: swipe vertikal ganti episode + toggle kontrol (tap)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .fillMaxWidth(0.5f)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                        }
                        .pointerInput(currentEp, effTotalEp) {
                            var totalDy = 0f
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (totalDy < -120 && currentEp < effTotalEp) {
                                        currentEp++; showEpisodePicker = false
                                        gestureHint = "Episode ${currentEp}"
                                    } else if (totalDy > 120 && currentEp > 1) {
                                        currentEp--; showEpisodePicker = false
                                        gestureHint = "Episode ${currentEp}"
                                    }
                                    totalDy = 0f
                                    scope.launch { kotlinx.coroutines.delay(1200); gestureHint = null }
                                },
                                onVerticalDrag = { _, dragAmount -> totalDy += dragAmount }
                            )
                        }
                )
                // Indikator gesture (tengah layar)
                gestureHint?.let { hint ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(hint, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
                // ===== BOTTOM CONTROL BAR (auto-hide) =====
                if (controlsVisible && !state.isLoading && state.error == null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Seek bar + time labels
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                formatMs(if (isSeeking) (seekPosition * durationMs).toLong() else positionMs),
                                color = Color.White, fontSize = 12.sp,
                                modifier = Modifier.width(48.dp)
                            )
                            androidx.compose.material3.Slider(
                                value = if (isSeeking) seekPosition else if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                                onValueChange = {
                                    isSeeking = true
                                    seekPosition = it
                                },
                                onValueChangeFinished = {
                                    exoPlayer.seekTo((seekPosition * durationMs).toLong())
                                    isSeeking = false
                                    controlsVisible = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF4D67),
                                    activeTrackColor = Color(0xFFFF4D67),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Text(
                                formatMs(durationMs),
                                color = Color.White, fontSize = 12.sp,
                                modifier = Modifier.width(48.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                        // Tombol kontrol
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Minimize ala YouTube: kembali browsing, video tetap jalan di mini bar
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.KeyboardArrowDown, Str.minimize, tint = Color.White)
                            }
                            IconButton(onClick = {
                                if (currentEp > 1) { currentEp--; showEpisodePicker = false; controlsVisible = true }
                            }) {
                                Icon(AppIcons.SkipPrevious, Str.prevEpisode, tint = Color.White)
                            }
                            // Play/Pause besar di tengah
                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    controlsVisible = true
                                },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlaying) Str.pause else Str.play,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            IconButton(onClick = {
                                if (currentEp < effTotalEp) { currentEp++; showEpisodePicker = false; controlsVisible = true }
                            }) {
                                Icon(AppIcons.SkipNext, Str.nextEpisode, tint = Color.White)
                            }
                            androidx.compose.material3.TextButton(onClick = { showEpisodePicker = true }) {
                                Text("EP $currentEp/$effTotalEp", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp)
                            }
                            // Chromecast: lempar video aktif ke TV
                            if (castContext != null) {
                                IconButton(onClick = {
                                    val sm = castContext.sessionManager
                                    val session = sm.currentCastSession
                                    if (session != null && session.isConnected) {
                                        sm.endCurrentSession(true)
                                    } else {
                                        showCastPicker = true
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Cast, Str.castToTv,
                                        tint = if (isCasting) Color(0xFFFF4D67) else Color.White
                                    )
                                }
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, Str.settings, tint = Color.White)
                            }
                            IconButton(onClick = {
                                try {
                                    activity?.enterPictureInPictureMode(
                                        android.app.PictureInPictureParams.Builder().build()
                                    )
                                } catch (_: Exception) {}
                            }) {
                                Icon(AppIcons.Pip, Str.miniPlayer, tint = Color.White)
                            }
                        }
                    }
                }
                // Indikator brightness/volume ala streaming app (sisi layar)
                brightLevel?.let { lvl ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 24.dp)
                            .background(Color.Black.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.BrightnessMedium, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(120.dp)
                                    .background(Color.White.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .fillMaxHeight(lvl.coerceIn(0f, 1f))
                                        .background(Color(0xFFFF4D67), androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${(lvl * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
                volumeLevel?.let { lvl ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp)
                            .background(Color.Black.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(120.dp)
                                    .background(Color.White.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .fillMaxHeight(lvl.coerceIn(0f, 1f))
                                        .background(Color(0xFFFF4D67), androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${(lvl * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
                // Dialog pengaturan: kualitas + kecepatan + sleep timer
                // ===== Dialog pilih perangkat Chromecast =====
                if (showCastPicker) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showCastPicker = false },
                        title = { Text(Str.castToDevice, color = Color.White) },
                        text = {
                            if (castRoutes.isEmpty()) {
                                Text(
                                    Str.noCastDevices,
                                    color = Color.Gray, fontSize = 13.sp
                                )
                            } else {
                                androidx.compose.foundation.lazy.LazyColumn {
                                    items(castRoutes) { route ->
                                        androidx.compose.material3.TextButton(
                                            onClick = {
                                                try { route.select() } catch (_: Exception) { }
                                                showCastPicker = false
                                            }
                                        ) {
                                            Text(
                                                route.name ?: Str.castDevices,
                                                color = Color.White, fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showCastPicker = false }
                            ) { Text(Str.close, color = Color(0xFFFF4D67)) }
                        },
                        containerColor = Color(0xFF1E1E1E)
                    )
                }

                if (showSettings) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showSettings = false },
                        title = { Text(Str.settings) },
                        text = {
                            Column {
                                // Kualitas video
                                if (state.qualities.isNotEmpty()) {
                                    Text(Str.videoQuality, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        state.qualities.forEach { q ->
                                            androidx.compose.material3.FilterChip(
                                                selected = state.currentQuality == q.label,
                                                onClick = {
                                                    if (state.currentQuality != q.label) {
                                                        pendingSeekPos = exoPlayer.currentPosition
                                                        vm.switchQuality(q, onNeedReload = { })
                                                        showSettings = false
                                                    }
                                                },
                                                label = { Text(q.label) }
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                                // Subtitle: bahasa
                                if (!state.subtitleUri.isNullOrBlank()) {
                                    Text(Str.subtitles, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    var subLang by remember { mutableStateOf(vm.subtitleLang()) }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf("id" to "Indonesia", "en" to "English", "off" to Str.off).forEach { (code, label) ->
                                            androidx.compose.material3.FilterChip(
                                                selected = subLang == code,
                                                onClick = {
                                                    subLang = code
                                                    vm.setSubtitleLang(code)
                                                    pendingSeekPos = exoPlayer.currentPosition
                                                    vm.toggleSubtitle(code != "off")
                                                    if (code != "off") vm.load(contentId, currentEp)
                                                },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    // Subtitle: ukuran teks
                                    Text(Str.subtitleSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(Str.small, "Normal", Str.large).forEachIndexed { idx, label ->
                                            androidx.compose.material3.FilterChip(
                                                selected = subSizeIdx == idx,
                                                onClick = {
                                                    subSizeIdx = idx
                                                    vm.setSubtitleSize(idx)
                                                    applySubtitleSize()
                                                },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                                Text(Str.playbackSpeed, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    listOf(0.5f, 0.75f, 1f).forEach { s ->
                                        androidx.compose.material3.FilterChip(
                                            selected = playbackSpeed == s,
                                            onClick = { playbackSpeed = s },
                                            label = { Text(if (s == 1f) "Normal" else "${s}x") }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    listOf(1.25f, 1.5f, 2f).forEach { s ->
                                        androidx.compose.material3.FilterChip(
                                            selected = playbackSpeed == s,
                                            onClick = { playbackSpeed = s },
                                            label = { Text("${s}x") }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(Str.sleepTimer, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    listOf<Pair<Int?, String>>(null to Str.off, 15 to Str.minLabel(15), 30 to Str.minLabel(30)).forEach { (m, label) ->
                                        androidx.compose.material3.FilterChip(
                                            selected = sleepMinutes == m,
                                            onClick = { if (m == null) MiniPlayerController.cancelSleepTimer() else MiniPlayerController.startSleepTimer(m) },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    listOf(60 to "60 mnt", 90 to "90 mnt").forEach { (m, label) ->
                                        androidx.compose.material3.FilterChip(
                                            selected = sleepMinutes == m,
                                            onClick = { if (m == null) MiniPlayerController.cancelSleepTimer() else MiniPlayerController.startSleepTimer(m) },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                                val sm = sleepMinutes
                                if (sm != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(Str.timerActive(sm), color = Color(0xFFFF4D67), fontSize = 12.sp)
                                }
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { showSettings = false }) {
                                Text(Str.close)
                            }
                        }
                    )
                }
                // Bottom sheet daftar episode (setengah layar, muncul dari bawah)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showEpisodePicker,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) +
                            androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) +
                            androidx.compose.animation.fadeOut()
                ) {
                    Box(Modifier.fillMaxSize()) {
                        // scrim gelap
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                                .clickable(
                                    indication = null,
                                    interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) { showEpisodePicker = false }
                        )
                        // sheet setengah layar dari bawah, ujung atas melengkung
                        Column(
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(0.62f)
                                .clip(
                                    androidx.compose.foundation.shape.RoundedCornerShape(
                                        topStart = 24.dp, topEnd = 24.dp
                                    )
                                )
                                .background(androidx.compose.ui.graphics.Color(0xFF1A1A2E))
                        ) {
                            // drag handle
                            Box(
                                modifier = Modifier
                                    .align(androidx.compose.ui.Alignment.CenterHorizontally)
                                    .padding(vertical = 10.dp)
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f))
                            )
                            Text(
                                Str.selectEpisode,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                            Text(
                                Str.epOf(currentEp, effTotalEp),
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(effTotalEp) { i ->
                                    val num = i + 1
                                    val selected = num == currentEp
                                    val done = com.clipku.drama.data.history.WatchHistory.isEpisodeDone(contentId, num)
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1.4f)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                            .background(
                                                if (selected) androidx.compose.ui.graphics.Color(0xFFFF4D67)
                                                else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) androidx.compose.ui.graphics.Color.Transparent
                                                else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                                                androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                            )
                                            .clickable { currentEp = num; showEpisodePicker = false },
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        Text(
                                            "$num",
                                            color = if (selected) androidx.compose.ui.graphics.Color.White
                                            else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold
                                            else androidx.compose.ui.text.font.FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        // centang untuk episode yang sudah selesai ditonton
                                        if (done && !selected) {
                                            Icon(
                                                Icons.Default.Check, Str.watched,
                                                tint = androidx.compose.ui.graphics.Color(0xFFFF4D67),
                                                modifier = Modifier
                                                    .align(androidx.compose.ui.Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== Countdown episode berikutnya (ala Netflix) =====
    if (showNextCountdown) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.BottomEnd
        ) {
            androidx.compose.material3.Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f)
                ),
                modifier = Modifier.padding(20.dp).width(240.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        Str.nextEpisode,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    if (autoNext) {
                        Text(
                            Str.nextEpIn(currentEp, countdownSec),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = { showNextCountdown = false },
                            modifier = Modifier.weight(1f)
                        ) { Text(Str.cancel, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)) }
                        androidx.compose.material3.Button(
                            onClick = {
                                showNextCountdown = false
                                showEpisodePicker = false
                                currentEp++
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFFFF4D67)
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text(Str.play, color = androidx.compose.ui.graphics.Color.White) }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return String.format("%02d:%02d", s / 60, s % 60)
}
