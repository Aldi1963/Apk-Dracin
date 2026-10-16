package com.clipku.drama.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clipku.drama.ui.theme.BgCard
import com.clipku.drama.ui.theme.BgDeep
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.Str
import com.clipku.drama.ui.theme.TextMain
import com.clipku.drama.ui.theme.TextMuted

/**
 * Layar "Info Aplikasi": nama, versi, package, developer, privasi, copyright.
 */
@Composable
fun AppInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pkg = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (_: Exception) { null }
    val versionName = pkg?.versionName ?: "-"
    @Suppress("DEPRECATION")
    val versionCode = pkg?.versionCode ?: 0

    Column(Modifier.fillMaxSize().background(BgDeep)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = Str.back, tint = TextMain)
            }
            Text(Str.appInfo, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(Coral),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("CLIPKU+", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "${Str.version} $versionName ($versionCode)",
                color = TextMuted, fontSize = 12.sp
            )
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InfoRow(Str.appName, "CLIPKU+")
                InfoRow(Str.version, "$versionName ($versionCode)")
                InfoRow(Str.packageLabel, context.packageName)
                InfoRow(Str.developer, "Aldi Irawan")
                InfoRowLink(Str.github, "Aldi1963") {
                    try {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/Aldi1963")
                            )
                        )
                    } catch (_: Exception) { }
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .padding(16.dp)
            ) {
                Text(Str.privacy, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(Str.privacyNote, color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(Str.copyright, color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Text(
            value, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Baris info yang bisa di-tap (mis. buka link di browser). */
@Composable
private fun InfoRowLink(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Text(
            value, color = Coral, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
