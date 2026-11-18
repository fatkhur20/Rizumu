package com.kyrix.ftxmusic.data.local.playlist

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistOrderTest {

    @Test
    fun movesItemDown() {
        assertEquals(
            listOf("a", "c", "b", "d"),
            PlaylistOrder.move(listOf("a", "b", "c", "d"), 1, 2),
        )
    }

    @Test
    fun movesItemUp() {
        assertEquals(
            listOf("a", "d", "b", "c"),
            PlaylistOrder.move(listOf("a", "b", "c", "d"), 3, 1),
        )
    }

    @Test
    fun keepsOrderWhenIndicesMatch() {
        val items = listOf("a", "b", "c")
        assertEquals(items, PlaylistOrder.move(items, 1, 1))
    }

    @Test
    fun ignoresInvalidIndices() {
        val items = listOf("a", "b", "c")
        assertEquals(items, PlaylistOrder.move(items, -1, 1))
        assertEquals(items, PlaylistOrder.move(items, 1, 9))
        assertEquals(emptyList(), PlaylistOrder.move(emptyList<String>(), 0, 0))
    }

    @Test
    fun keepsAllItemsAfterMove() {
        val result = PlaylistOrder.move(listOf("a", "b", "c", "d", "e"), 4, 0)
        assertEquals(listOf("e", "a", "b", "c", "d"), result)
        assertEquals(5, result.size)
    }
}
