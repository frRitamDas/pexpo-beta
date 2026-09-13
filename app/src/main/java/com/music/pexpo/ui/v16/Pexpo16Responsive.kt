package com.music.pexpo.ui.v16

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

@Immutable
enum class Pexpo16WindowSize { COMPACT, MEDIUM, EXPANDED }

@Immutable
data class Pexpo16WindowMetrics(
    val widthDp: Int,
    val heightDp: Int,
    val size: Pexpo16WindowSize,
    val isLandscape: Boolean,
) {
    val isPhone: Boolean get() = size == Pexpo16WindowSize.COMPACT
    val isTablet: Boolean get() = size != Pexpo16WindowSize.COMPACT
    val columns: Int get() = when (size) {
        Pexpo16WindowSize.COMPACT -> 2
        Pexpo16WindowSize.MEDIUM -> 3
        Pexpo16WindowSize.EXPANDED -> 5
    }
}

/** Stable width buckets for phones, foldables and tablets. */
@Composable
fun rememberPexpo16WindowMetrics(): Pexpo16WindowMetrics {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        val width = configuration.screenWidthDp
        val size = when {
            width < 600 -> Pexpo16WindowSize.COMPACT
            width < 840 -> Pexpo16WindowSize.MEDIUM
            else -> Pexpo16WindowSize.EXPANDED
        }
        Pexpo16WindowMetrics(
            widthDp = width,
            heightDp = configuration.screenHeightDp,
            size = size,
            isLandscape = configuration.screenWidthDp > configuration.screenHeightDp,
        )
    }
}
