package com.clipku.drama.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clipku.drama.ui.theme.BgDeep
import com.clipku.drama.ui.theme.Coral
import com.clipku.drama.ui.theme.Str
import com.clipku.drama.ui.theme.TextMain
import com.clipku.drama.ui.theme.TextMuted
import kotlinx.coroutines.delay

/**
 * Splash screen: tampil ~1,3 detik saat aplikasi dibuka, lalu navigasi ke Beranda.
 * Back press dari Beranda tidak kembali ke splash (popUpTo inclusive di pemanggil).
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1300)
        onDone()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(BgDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Coral),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "CLIPKU",
                color = TextMain,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Text(
                "+",
                color = Coral,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(
                color = Coral,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                Str.appTagline,
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
