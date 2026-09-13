package com.music.pexpo.ui.v16

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Pexpo16FeatureArchitectureTest {
    @Test
    fun switchingFeatureClearsDetail() {
        val state = Pexpo16FeatureState()
            .openDetail("album-123")
            .open(Pexpo16Feature.LIBRARY)

        assertEquals(Pexpo16Feature.LIBRARY, state.feature)
        assertEquals(null, state.detailId)
    }

    @Test
    fun detailCanBeOpenedAndClosedWithoutChangingFeature() {
        val state = Pexpo16FeatureState()
            .open(Pexpo16Feature.SEARCH)
            .openDetail("playlist-456")

        assertEquals(Pexpo16Feature.SEARCH, state.feature)
        assertEquals("playlist-456", state.detailId)
        assertEquals(null, state.closeDetail().detailId)
    }

    @Test
    fun nowPlayingExpansionIsIndependentFromFeature() {
        val state = Pexpo16FeatureState().open(Pexpo16Feature.LIBRARY)
        assertFalse(state.isNowPlayingExpanded)
        assertTrue(state.toggleNowPlaying().isNowPlayingExpanded)
        assertEquals(Pexpo16Feature.LIBRARY, state.toggleNowPlaying().feature)
    }
}
