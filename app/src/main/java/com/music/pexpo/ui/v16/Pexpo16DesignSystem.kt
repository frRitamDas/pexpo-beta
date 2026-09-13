package com.music.pexpo.ui.v16

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Pexpo 1.6 design primitives, isolated so the redesign can roll out safely. */
@Immutable
data class Pexpo16Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val page: Dp = 20.dp,
)

@Immutable
data class Pexpo16Motion(
    val fast: FiniteAnimationSpec<Float> = tween(160, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
    val normal: FiniteAnimationSpec<Float> = tween(260, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
    val emphasis: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow,
    ),
)

@Immutable
data class Pexpo16DesignSystem(
    val spacing: Pexpo16Spacing = Pexpo16Spacing(),
    val motion: Pexpo16Motion = Pexpo16Motion(),
    val cornerRadius: Dp = 20.dp,
    val compactCornerRadius: Dp = 14.dp,
    val contentMaxWidth: Dp = 960.dp,
)

val LocalPexpo16DesignSystem = staticCompositionLocalOf { Pexpo16DesignSystem() }

object Pexpo16Defaults {
    val designSystem: Pexpo16DesignSystem = Pexpo16DesignSystem()
}
