package com.kyrix.ftxmusic.data.local.playlist

object PlaylistOrder {
    fun <T> move(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
            return items
        }
        return items.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }
}
