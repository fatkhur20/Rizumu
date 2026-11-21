package com.rizumu.music.data.local.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistNamesTest {

    @Test
    fun `trims and collapses whitespace`() {
        assertEquals("Malam Santai", PlaylistNames.normalize("  Malam   Santai  "))
    }

    @Test
    fun `blank names are rejected`() {
        assertNull(PlaylistNames.normalize(""))
        assertNull(PlaylistNames.normalize("   "))
    }

    @Test
    fun `long names are capped`() {
        val normalized = PlaylistNames.normalize("a".repeat(120))
        assertEquals(PlaylistNames.MAX_LENGTH, normalized?.length)
    }
}
