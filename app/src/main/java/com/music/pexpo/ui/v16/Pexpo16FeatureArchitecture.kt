package com.music.pexpo.ui.v16

import androidx.compose.runtime.Immutable

/**
 * Stable top-level feature contracts for the 1.6 migration.
 *
 * UI implementations can evolve independently while MainActivity keeps the
 * existing playback/session wiring. This prevents each redesign pass from
 * inventing another routing vocabulary.
 */
@Immutable
enum class Pexpo16Feature {
    HOME,
    SEARCH,
    LIBRARY,
    NOW_PLAYING,
    SETTINGS,
}

@Immutable
data class Pexpo16FeatureState(
    val feature: Pexpo16Feature = Pexpo16Feature.HOME,
    val detailId: String? = null,
    val isNowPlayingExpanded: Boolean = false,
)

fun Pexpo16FeatureState.open(feature: Pexpo16Feature): Pexpo16FeatureState =
    if (feature == this.feature) this else copy(feature = feature, detailId = null)

fun Pexpo16FeatureState.openDetail(browseId: String): Pexpo16FeatureState =
    copy(detailId = browseId)

fun Pexpo16FeatureState.closeDetail(): Pexpo16FeatureState =
    copy(detailId = null)

fun Pexpo16FeatureState.toggleNowPlaying(): Pexpo16FeatureState =
    copy(isNowPlayingExpanded = !isNowPlayingExpanded)
