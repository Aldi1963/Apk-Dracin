package com.clipku.drama.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.clipku.drama.R

object AppIcons {
    val Explore: Painter @Composable get() = painterResource(R.drawable.ic_explore)
    val TrendingUp: Painter @Composable get() = painterResource(R.drawable.ic_trending_up)
    val History: Painter @Composable get() = painterResource(R.drawable.ic_history)
    val SkipNext: Painter @Composable get() = painterResource(R.drawable.ic_skip_next)
    val SkipPrevious: Painter @Composable get() = painterResource(R.drawable.ic_skip_previous)
    val Pip: Painter @Composable get() = painterResource(R.drawable.ic_pip)
}
