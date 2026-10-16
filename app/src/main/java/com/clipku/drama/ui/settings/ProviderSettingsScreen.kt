package com.clipku.drama.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clipku.drama.data.health.ProviderHealth
import com.clipku.drama.data.prefs.AppSettings
import com.clipku.drama.data.sonzaix.SonzaixProviders
import com.clipku.drama.ui.theme.BgCard
import com.clipku.drama.ui.theme.BgDeep
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.Str
import com.clipku.drama.ui.theme.TextMain
import com.clipku.drama.ui.theme.TextMuted

/**
 * Layar pengaturan Provider tersendiri: search + daftar 40 provider
 * (nama + switch; nonaktif bila disembunyikan otomatis).
 * Logika dipindah verbatim dari SettingsScreen (v61).
 */
@Composable
fun ProviderSettingsScreen(onBack: () -> Unit) {
    val manualHidden by AppSettings.hiddenProvidersManual.collectAsState()
    val autoHidden by ProviderHealth.hidden.collectAsState()
    var query by remember { mutableStateOf("") }
    val list = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) SonzaixProviders.all
        else SonzaixProviders.all.filter {
            it.name.lowercase().contains(q) || it.slug.lowercase().contains(q)
        }
    }

    Column(Modifier.fillMaxSize().background(BgDeep)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = Str.back, tint = TextMain)
            }
            Text(Str.providers, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        // Search
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text(Str.searchProviders, color = TextMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(20.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BgCard,
                unfocusedContainerColor = BgCard,
                focusedBorderColor = Coral,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain,
                cursorColor = Coral
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            Str.hideDeadProviderNote,
            color = TextMuted, fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        if (list.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(Str.noProvidersFound, color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.slug }) { cfg ->
                    val autoOff = autoHidden.contains(cfg.slug)
                    val manualOff = manualHidden.contains(cfg.slug)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgCard)
                            .clickable(enabled = !autoOff) {
                                AppSettings.setProviderHiddenManual(cfg.slug, !manualOff)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                cfg.name,
                                color = if (autoOff || manualOff) TextMuted else TextMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (autoOff) {
                                Text(Str.autoHiddenNote, color = Coral, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = !manualOff && !autoOff,
                            enabled = !autoOff,
                            onCheckedChange = { checked ->
                                AppSettings.setProviderHiddenManual(cfg.slug, !checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Coral
                            )
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
