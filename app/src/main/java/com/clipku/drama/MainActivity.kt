package com.clipku.drama
import com.clipku.drama.ui.theme.Str

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import coil.imageLoader
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import com.clipku.drama.ui.theme.AppIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clipku.drama.ui.detail.DetailScreen
import com.clipku.drama.ui.explore.ExploreScreen
import com.clipku.drama.ui.home.HomeMode
import com.clipku.drama.ui.home.HomeScreen
import com.clipku.drama.ui.navigation.Routes
import com.clipku.drama.data.history.WatchHistory
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import com.clipku.drama.ui.history.HistoryScreen
import com.clipku.drama.ui.player.PlayerScreen
import com.clipku.drama.ui.player.MiniPlayerController
import com.clipku.drama.ui.player.MiniPlayerBar
import com.clipku.drama.ui.theme.BgCard
import com.clipku.drama.ui.theme.ClipkuDramaTheme
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.TextMuted

data class BottomTab(val route: String, val label: String, val icon: @Composable (tint: Color) -> Unit)

val bottomTabs = listOf(
    BottomTab(Routes.HOME, Str.home) { tint -> Icon(Icons.Default.Home, null, tint = tint) },
    BottomTab(Routes.POPULAR, Str.popular) { tint -> Icon(AppIcons.TrendingUp, null, tint = tint) },
    BottomTab(Routes.EXPLORE, Str.explore) { tint -> Icon(AppIcons.Explore, null, tint = tint) },
    BottomTab(Routes.HISTORY, "Riwayat") { tint -> Icon(AppIcons.History, null, tint = tint) }
)

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // Terapkan skala font UI + locale dari pengaturan sebelum activity dibuat.
        // AppSettings.init idempoten — aman dipanggil di sini lalu di onCreate.
        com.clipku.drama.data.prefs.AppSettings.init(newBase)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        val lang = com.clipku.drama.data.prefs.AppSettings.appLanguage.value
        config.setLocale(java.util.Locale(if (lang == "en") "en" else "in"))
        config.fontScale = com.clipku.drama.data.prefs.AppSettings.uiFontScale.value
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WatchHistory.init(this)
        com.clipku.drama.data.history.SearchHistory.init(this)
        com.clipku.drama.data.history.FavoriteStore.init(this)
        com.clipku.drama.ui.theme.ThemePrefs.init(this)
        com.clipku.drama.data.prefs.AppSettings.init(this)
        com.clipku.drama.data.history.WatchStats.init(this)
        com.clipku.drama.data.health.ProviderHealth.init(this)
        // hapus cache gambar otomatis sesuai jadwal (0 = mati)
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val days = com.clipku.drama.data.prefs.AppSettings.autoClearCacheDays.value
                if (days > 0) {
                    val ctx = this@MainActivity
                    val last = com.clipku.drama.data.prefs.CachePrefs.lastCleared(ctx)
                    if (System.currentTimeMillis() - last > days * 24L * 3600L * 1000L) {
                        ctx.imageLoader.diskCache?.clear()
                        ctx.imageLoader.memoryCache?.clear()
                        com.clipku.drama.data.prefs.CachePrefs.markCleared(ctx)
                    }
                }
            } catch (_: Exception) { }
        }
        // probe kesehatan provider di background (debounce 1x/jam di dalam)
        lifecycleScope.launch {
            try { com.clipku.drama.data.health.ProviderHealth.probeIfDue(this) }
            catch (_: Exception) { }
        }
        // izin notifikasi (Android 13+): untuk info episode baru judul favorit
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permLauncher = registerForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { }
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent {
            ClipkuDramaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScaffold()
                }
            }
        }
    }
}

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val tabRoutes = setOf(Routes.HOME, Routes.POPULAR, Routes.EXPLORE, Routes.HISTORY)
    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        dest.route in tabRoutes
    } == true
    // Mini player ala YouTube: tampil di semua layar kecuali full player & splash
    val miniSession by MiniPlayerController.session.collectAsState()
    val miniPlaying by MiniPlayerController.isPlaying.collectAsState()
    val isPlayerRoute = currentDestination?.route?.startsWith("player/") == true
    val showMiniBar = miniSession != null && !isPlayerRoute

    Scaffold(
        bottomBar = {
            Column {
                if (showMiniBar) {
                    val s = miniSession!!
                    MiniPlayerBar(
                        session = s,
                        isPlaying = miniPlaying,
                        onTogglePlay = { MiniPlayerController.togglePlayPause() },
                        onClose = { MiniPlayerController.close() },
                        onExpand = {
                            navController.navigate(
                                Routes.player(s.contentId, s.ep, s.totalEp, s.title, s.posterUrl, s.providerName)
                            ) { launchSingleTop = true }
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .shadow(12.dp, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(BgCard)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) Coral else Color.Transparent)
                                    .clickable {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    tab.icon(if (selected) Color.White else TextMuted)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        tab.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.SPLASH) {
                com.clipku.drama.ui.splash.SplashScreen(
                    onDone = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.POPULAR) {
                HomeScreen(
                    mode = com.clipku.drama.ui.home.HomeMode.POPULER,
                    onDramaClick = { item -> navController.navigate(Routes.detail(item.slug, item.title, item.posterUrl.orEmpty())) },
                    onExploreClick = { navController.navigate(Routes.EXPLORE) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    mode = HomeMode.BERANDA,
                    onDramaClick = { item -> navController.navigate(Routes.detail(item.slug, item.title, item.posterUrl.orEmpty())) },
                    onExploreClick = { navController.navigate(Routes.EXPLORE) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    onBack = { navController.popBackStack() },
                    onDramaClick = { item -> navController.navigate(Routes.detail(item.slug, item.title, item.posterUrl.orEmpty())) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onResume = { cid, e, te, t -> navController.navigate(Routes.player(cid, e, te, t)) },
                    onBrowse = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.SETTINGS) {
                com.clipku.drama.ui.settings.SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onOpenAppInfo = { navController.navigate(Routes.APP_INFO) },
                    onOpenProviders = { navController.navigate(Routes.PROVIDERS) }
                )
            }
            composable(Routes.PROVIDERS) {
                com.clipku.drama.ui.settings.ProviderSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.APP_INFO) {
                com.clipku.drama.ui.settings.AppInfoScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.STATS) {
                com.clipku.drama.ui.settings.StatsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Routes.DETAIL,
                arguments = listOf(
                    navArgument("slug") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("poster") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
                DetailScreen(
                    slug = slug,
                    fallbackTitle = backStackEntry.arguments?.getString("title").orEmpty(),
                    fallbackPoster = backStackEntry.arguments?.getString("poster").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onPlay = { cid, e, te, t, p, prov -> navController.navigate(Routes.player(cid, e, te, t, p, prov)) }
                )
            }
            composable(
                Routes.PLAYER,
                arguments = listOf(
                    navArgument("contentId") { type = NavType.StringType },
                    navArgument("ep") { type = NavType.StringType },
                    navArgument("totalEp") { type = NavType.IntType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("poster") { type = NavType.StringType },
                    navArgument("provider") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val contentId = backStackEntry.arguments?.getString("contentId") ?: return@composable
                val ep = backStackEntry.arguments?.getString("ep")?.toIntOrNull() ?: 1
                val totalEp = backStackEntry.arguments?.getInt("totalEp") ?: 1
                val title = backStackEntry.arguments?.getString("title") ?: ""
                val poster = backStackEntry.arguments?.getString("poster")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
                val provider = backStackEntry.arguments?.getString("provider")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
                PlayerScreen(
                    contentId = contentId,
                    ep = ep,
                    totalEp = totalEp,
                    posterUrl = poster,
                    providerName = provider,
                    title = java.net.URLDecoder.decode(title, "UTF-8"),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
