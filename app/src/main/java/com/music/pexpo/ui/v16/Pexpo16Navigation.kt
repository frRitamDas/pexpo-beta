package com.music.pexpo.ui.v16

import androidx.compose.runtime.Immutable

/**
 * 1.6 navigation contract. Existing routes can be mapped into this model
 * without changing the playback/session layer.
 */
@Immutable
sealed interface Pexpo16Destination {
    val key: String

    data object Home : Pexpo16Destination { override val key = "home" }
    data object Search : Pexpo16Destination { override val key = "search" }
    data object Library : Pexpo16Destination { override val key = "library" }
    data object NowPlaying : Pexpo16Destination { override val key = "now_playing" }
    data object Settings : Pexpo16Destination { override val key = "settings" }
}

@Immutable
data class Pexpo16NavigationState(
    val current: Pexpo16Destination = Pexpo16Destination.Home,
    val backStack: List<Pexpo16Destination> = emptyList(),
) {
    fun navigate(destination: Pexpo16Destination): Pexpo16NavigationState {
        if (destination == current) return this
        return copy(current = destination, backStack = backStack + current)
    }

    fun back(): Pexpo16NavigationState = when {
        backStack.isEmpty() -> this
        else -> copy(current = backStack.last(), backStack = backStack.dropLast(1))
    }
}

fun Pexpo16NavigationState.canGoBack(): Boolean = backStack.isNotEmpty()
