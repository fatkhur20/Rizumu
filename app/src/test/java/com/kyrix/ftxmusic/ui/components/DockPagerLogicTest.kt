package com.kyrix.ftxmusic.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DockPagerLogicTest {

    @Test
    fun `no media starts on navigation`() {
        assertEquals(DockPage.Nav, DockPagerLogic.initial(hasMedia = false))
    }

    @Test
    fun `song starts on player`() {
        assertEquals(DockPage.Player, DockPagerLogic.mediaChanged(hasMedia = true))
    }

    @Test
    fun `queue ends returns to navigation`() {
        assertEquals(DockPage.Nav, DockPagerLogic.mediaChanged(hasMedia = false))
    }

    @Test
    fun `playing tab selection returns after two seconds`() {
        val due = DockPagerLogic.autoReturnAt(
            hasMedia = true,
            autoReturnEnabled = true,
            isPlaying = true,
            now = 1_000L,
        )

        assertEquals(3_000L, due)
        assertFalse(
            DockPagerLogic.shouldAutoReturn(DockPage.Nav, true, due, 2_999L),
        )
        assertTrue(
            DockPagerLogic.shouldAutoReturn(DockPage.Nav, true, due, 3_000L),
        )
    }

    @Test
    fun `paused tab selection returns after fifteen seconds`() {
        val due = DockPagerLogic.autoReturnAt(
            hasMedia = true,
            autoReturnEnabled = true,
            isPlaying = false,
            now = 1_000L,
        )

        assertEquals(16_000L, due)
        assertFalse(
            DockPagerLogic.shouldAutoReturn(DockPage.Nav, true, due, 15_999L),
        )
        assertTrue(
            DockPagerLogic.shouldAutoReturn(DockPage.Nav, true, due, 16_000L),
        )
    }

    @Test
    fun `manual nav switch schedules auto return`() {
        assertTrue(
            DockPagerLogic.shouldScheduleAutoReturn(
                target = DockPage.Nav,
                hasMedia = true,
                autoReturnEnabled = true,
            ),
        )
        assertFalse(
            DockPagerLogic.shouldScheduleAutoReturn(
                target = DockPage.Player,
                hasMedia = true,
                autoReturnEnabled = true,
            ),
        )
        assertFalse(
            DockPagerLogic.shouldScheduleAutoReturn(
                target = DockPage.Nav,
                hasMedia = true,
                autoReturnEnabled = false,
            ),
        )
    }

    @Test
    fun `disabled auto return never schedules`() {
        assertNull(
            DockPagerLogic.autoReturnAt(
                hasMedia = true,
                autoReturnEnabled = false,
                isPlaying = true,
                now = 0L,
            ),
        )
    }

    @Test
    fun `empty queue does not auto return`() {
        assertFalse(
            DockPagerLogic.shouldAutoReturn(DockPage.Nav, false, 2_000L, 9_999L),
        )
    }

    @Test
    fun `no media settles on visible navigation page`() {
        val frame = DockPagerLogic.settled(hasMedia = false)

        assertEquals(DockPage.Nav, frame.page)
        assertEquals(0f, frame.offsetFraction, 0f)
        assertEquals(1f, frame.navAlpha, 0f)
        assertEquals(0f, frame.playerAlpha, 0f)
    }

    @Test
    fun `active media settles on fully visible player page`() {
        val frame = DockPagerLogic.settled(hasMedia = true)

        assertEquals(DockPage.Player, frame.page)
        assertEquals(-1f, frame.offsetFraction, 0f)
        assertEquals(0f, frame.navAlpha, 0f)
        assertEquals(1f, frame.playerAlpha, 0f)
    }

    @Test
    fun `queue ending settles back on navigation page`() {
        val frame = DockPagerLogic.settled(hasMedia = false)

        assertEquals(DockPage.Nav, frame.page)
        assertEquals(0f, frame.offsetFraction, 0f)
        assertEquals(1f, frame.navAlpha, 0f)
        assertEquals(0f, frame.playerAlpha, 0f)
    }
}
