package com.clipku.drama.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clipku.drama.data.history.WatchStats
import com.clipku.drama.ui.theme.BgCard
import com.clipku.drama.ui.theme.BgDeep
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.Str
import com.clipku.drama.ui.theme.TextMain
import com.clipku.drama.ui.theme.TextMuted

@Composable
fun StatsScreen(onBack: () -> Unit) {
    // refresh saat ada pencatatan baru
    val bump by WatchStats.bump.collectAsState()
    @Suppress("UNUSED_EXPRESSION") bump
    val week = WatchStats.lastDays(7)
    val month = WatchStats.lastDays(30)
    Column(Modifier.fillMaxSize().background(BgDeep)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = Str.back, tint = TextMain)
            }
            Text(Str.watchStats, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = Str.thisWeek,
                duration = WatchStats.formatDuration(week.seconds),
                episodes = week.episodes
            )
            StatCard(
                title = Str.thisMonth,
                duration = WatchStats.formatDuration(month.seconds),
                episodes = month.episodes
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (com.clipku.drama.data.prefs.AppSettings.appLanguage.value == "en")
                    "Statistics are stored locally on this device."
                else "Statistik tersimpan lokal di perangkat ini.",
                color = TextMuted, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatCard(title: String, duration: String, episodes: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(16.dp)
    ) {
        Text(title, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(duration, color = Coral, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "${Str.episodesWatched}: $episodes",
            color = TextMain, fontSize = 13.sp
        )
    }
}
