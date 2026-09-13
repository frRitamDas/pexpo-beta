package com.music.pexpo.ui.v16

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Pexpo16NavigationTest {
    @Test
    fun navigateAndBackPreserveHistory() {
        val home = Pexpo16NavigationState()
        val search = home.navigate(Pexpo16Destination.Search)
        val player = search.navigate(Pexpo16Destination.NowPlaying)

        assertEquals(Pexpo16Destination.NowPlaying, player.current)
        assertEquals(listOf(Pexpo16Destination.Home, Pexpo16Destination.Search), player.backStack)
        assertTrue(player.canGoBack())
        assertEquals(Pexpo16Destination.Search, player.back().current)
    }

    @Test
    fun navigatingToCurrentDestinationIsIdempotent() {
        val state = Pexpo16NavigationState().navigate(Pexpo16Destination.Library)
        assertEquals(state, state.navigate(Pexpo16Destination.Library))
    }
}
