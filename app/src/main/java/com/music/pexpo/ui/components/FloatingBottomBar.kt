package com.music.pexpo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.pexpo.data.settings.AppSettings
import com.music.pexpo.ui.haptics.Haptic
import com.music.pexpo.ui.haptics.rememberHaptics
import com.music.pexpo.ui.v16.pexpo16PressScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.abs
import kotlin.math.roundToInt

data class BottomTab(
    val label: String,
    val icon: ImageVector,
)

internal val PILL_INSET = 8.dp
internal val TAB_VERTICAL_PADDING = 10.dp
internal val TAB_ICON_LABEL_GAP = 3.dp

private val GlassSpring = spring<Float>(dampingRatio = 0.78f, stiffness = 360f)
private const val STRETCH = 0.14f
private const val SQUASH = 0.45f

/** Pexpo 1.6 navigation capsule with spring motion, swipe navigation and tactile press feedback. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FloatingBottomBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    val pillShape = remember { RoundedCornerShape(percent = 50) }
    val container = MaterialTheme.colorScheme.surface
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useGlass = LocalLiquidGlassEnabled.current && isGlassSupported()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val animationSpec: AnimationSpec<Float> = if (reduceAnimation) snap() else GlassSpring
    val haptics = rememberHaptics()
    val density = LocalDensity.current
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var lastHapticTab by remember { mutableIntStateOf(selectedIndex) }

    val gapPx = with(density) { 6.dp.toPx() }
    val count = tabs.size
    val tabWidthPx = if (rowSize.width > 0) (rowSize.width - gapPx * (count - 1)) / count else 0f
    val tabStepPx = if (rowSize.width > 0) (rowSize.width + gapPx) / count else 0f
    val targetOffset = selectedIndex * tabStepPx + dragOffset
    val animatedOffset by animateFloatAsState(targetOffset, animationSpec, label = "pexpo16NavIndicator")
    val lag = if (tabStepPx > 0f) (abs(targetOffset - animatedOffset) / tabStepPx).coerceIn(0f, 1f) else 0f

    LaunchedEffect(selectedIndex) {
        dragOffset = 0f
        lastHapticTab = selectedIndex
    }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = PAGE_GUTTER)
            .padding(bottom = 4.dp)
            .fillMaxWidth()
            .clip(pillShape)
            .then(
                if (reduceDynamicBlur) Modifier.background(container)
                else if (useGlass) Modifier.liquidGlass(shape = pillShape)
                else Modifier.optimizedHazeEffect(state = hazeState, style = HazeMaterials.regular(container)),
            )
            .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, pillShape)
            .padding(horizontal = PILL_INSET, vertical = PILL_INSET),
    ) {
        if (tabWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .width(with(density) { tabWidthPx.toDp() })
                    .height(with(density) { rowSize.height.toDp() })
                    .graphicsLayer {
                        translationX = animatedOffset
                        scaleX = 1f + lag * STRETCH
                        scaleY = 1f - lag * STRETCH * SQUASH
                    }
                    .clip(pillShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowSize = it }
                .pointerInput(count, tabStepPx, currentSelectedIndex) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { dragOffset = 0f },
                        onDragEnd = {
                            if (tabStepPx > 0f) {
                                val ratio = totalDrag / tabStepPx
                                val shift = when {
                                    ratio > 0.35f -> maxOf(1, ratio.roundToInt())
                                    ratio < -0.35f -> minOf(-1, ratio.roundToInt())
                                    else -> 0
                                }
                                val next = (currentSelectedIndex + shift).coerceIn(0, tabs.lastIndex)
                                if (next != currentSelectedIndex) {
                                    haptics.play(Haptic.Select)
                                    onTabSelected(next)
                                }
                            }
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, delta ->
                            totalDrag += delta
                            val edgeResistance =
                                (totalDrag > 0f && currentSelectedIndex == tabs.lastIndex) ||
                                    (totalDrag < 0f && currentSelectedIndex == 0)
                            dragOffset = if (edgeResistance) totalDrag * 0.22f else totalDrag
                            if (tabStepPx > 0f) {
                                val preview = (currentSelectedIndex + dragOffset / tabStepPx)
                                    .coerceIn(0f, tabs.lastIndex.toFloat()).roundToInt()
                                if (preview != lastHapticTab) {
                                    haptics.play(Haptic.Tick)
                                    lastHapticTab = preview
                                }
                            }
                        },
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val glassTint = glassContentColor()
            val selectedTint = if (useGlass) glassTint else null
            val unselectedTint = if (useGlass) glassTint.copy(alpha = 0.62f) else null
            tabs.forEachIndexed { index, tab ->
                BottomBarItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    animationSpec = animationSpec,
                    selectedTint = selectedTint,
                    unselectedTint = unselectedTint,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: BottomTab,
    selected: Boolean,
    animationSpec: AnimationSpec<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTint: Color? = null,
    unselectedTint: Color? = null,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1f,
        animationSpec = animationSpec,
        label = "pexpo16TabScale",
    )
    val haptics = rememberHaptics()
    val tint by animateColorAsState(
        targetValue = if (selected) selectedTint ?: MaterialTheme.colorScheme.primary
        else unselectedTint ?: MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "pexpo16TabTint",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .pexpo16PressScale {
                if (!selected) haptics.play(Haptic.Select)
                onClick()
            }
            .padding(vertical = TAB_VERTICAL_PADDING),
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(25.dp).graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
        Spacer(Modifier.height(TAB_ICON_LABEL_GAP))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
