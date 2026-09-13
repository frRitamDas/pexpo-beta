package com.music.pexpo.ui.v16

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Lightweight press feedback for 1.6 controls; no ripple replacement or heavy effect. */
@Composable
fun Modifier.pexpo16PressScale(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = LocalPexpo16DesignSystem.current.motion.fast,
        label = "pexpo16PressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            onClick = onClick,
        )
}
