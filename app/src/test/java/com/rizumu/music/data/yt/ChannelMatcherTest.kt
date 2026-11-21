package com.rizumu.music.data.yt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelMatcherTest {

    @Test
    fun `exact name wins over larger channel`() {
        val exact = ChannelHit("Romi", "u1", 1000L, false)
        val larger = ChannelHit("Romi Jahat", "u2", 5000L, false)

        assertEquals(exact, ChannelMatcher.select("romi", listOf(larger, exact)))
    }

    @Test
    fun `contains matches are ordered by subscribers`() {
        val small = ChannelHit("Romi Small", "u1", 100L, false)
        val big = ChannelHit("Romi & The Jahats", "u2", 474_000L, true)

        assertEquals(big, ChannelMatcher.select("romi", listOf(small, big)))
    }

    @Test
    fun `unknown subscriber counts skip enrichment`() {
        val unknown = ChannelHit("Romi", "u1", -1L, true)

        assertNull(ChannelMatcher.select("romi", listOf(unknown)))
    }

    @Test
    fun `unrelated channels are ignored`() {
        val other = ChannelHit("Rumi", "u1", 1000L, false)

        assertNull(ChannelMatcher.select("romi", listOf(other)))
    }
}
