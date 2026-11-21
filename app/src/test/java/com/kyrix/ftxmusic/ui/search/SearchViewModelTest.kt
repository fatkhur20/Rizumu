package com.kyrix.ftxmusic.ui.search

import com.kyrix.ftxmusic.data.yt.PageToken
import com.kyrix.ftxmusic.data.yt.SearchFilter
import com.kyrix.ftxmusic.data.yt.SearchPage
import com.kyrix.ftxmusic.data.yt.YoutubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private class RecordingYoutubeClient : YoutubeClient() {
        val queries = mutableListOf<String>()
        val filters = mutableListOf<String>()

        override suspend fun search(query: String, filter: String, token: PageToken?): SearchPage {
            queries += query
            filters += filter
            throw IllegalStateException("offline test")
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        initialQuery: String = "",
        initialGenre: String = "",
    ): Pair<SearchViewModel, RecordingYoutubeClient> {
        Dispatchers.setMain(dispatcher)
        val client = RecordingYoutubeClient()
        return SearchViewModel(client, initialQuery, initialGenre) to client
    }

    @Test
    fun `Pop quick action keeps input empty and uses the search term`() {
        val (viewModel, client) = viewModel()

        viewModel.openCategory("Pop", "lagu pop")

        assertEquals("", viewModel.state.value.queryText)
        assertEquals("Pop", viewModel.state.value.activeGenre)
        assertEquals(listOf("lagu pop"), client.queries)
        assertEquals(listOf(FILTER_ALL), client.filters)
        assertEquals(FILTER_ALL, viewModel.state.value.filter)
    }

    @Test
    fun `new release quick action keeps input empty`() {
        val (viewModel, client) = viewModel()

        viewModel.openCategory("Rilis Baru", "rilis baru")

        assertEquals("", viewModel.state.value.queryText)
        assertEquals("Rilis Baru", viewModel.state.value.activeGenre)
        assertEquals(listOf("rilis baru"), client.queries)
    }

    @Test
    fun `acoustic quick action keeps input empty`() {
        val (viewModel, client) = viewModel()

        viewModel.openCategory("Akustik", "lagu akustik")

        assertEquals("", viewModel.state.value.queryText)
        assertEquals("Akustik", viewModel.state.value.activeGenre)
        assertEquals(listOf("lagu akustik"), client.queries)
    }

    @Test
    fun `focus quick action keeps input empty`() {
        val (viewModel, client) = viewModel()

        viewModel.openCategory("Fokus", "lagu fokus")

        assertEquals("", viewModel.state.value.queryText)
        assertEquals("Fokus", viewModel.state.value.activeGenre)
        assertEquals(listOf("lagu fokus"), client.queries)
    }

    @Test
    fun `suggestion chip keeps input empty`() {
        val (viewModel, client) = viewModel()

        viewModel.openCategory("Dangdut")

        assertEquals("", viewModel.state.value.queryText)
        assertEquals("Dangdut", viewModel.state.value.activeGenre)
        assertEquals(listOf("Dangdut"), client.queries)
    }

    @Test
    fun `search idea keeps input empty`() {
        val (viewModel, client) = viewModel()

        viewModel.openCategory("Lagu galau")

        assertEquals("", viewModel.state.value.queryText)
        assertEquals("Lagu galau", viewModel.state.value.activeGenre)
        assertEquals(listOf("Lagu galau"), client.queries)
    }

    @Test
    fun `genre card keeps input empty`() {
        val (viewModel, client) = viewModel()

        viewModel.openCategory("K-Pop")

        assertEquals("", viewModel.state.value.queryText)
        assertEquals("K-Pop", viewModel.state.value.activeGenre)
        assertEquals(listOf("K-Pop"), client.queries)
    }

    @Test
    fun `initial category route uses q for search and genre for header`() {
        val (viewModel, client) = viewModel(initialQuery = "lagu pop", initialGenre = "Pop")

        assertEquals("", viewModel.state.value.queryText)
        assertEquals("Pop", viewModel.state.value.activeGenre)
        assertEquals(listOf("lagu pop"), client.queries)
    }

    @Test
    fun `typing resets activeGenre`() {
        val (viewModel, _) = viewModel()
        viewModel.openCategory("Dangdut")

        viewModel.onQueryChange("rock")

        assertEquals("rock", viewModel.state.value.queryText)
        assertNull(viewModel.state.value.activeGenre)
    }

    @Test
    fun `clearGenre returns to discovery and clears results`() {
        val (viewModel, _) = viewModel()
        viewModel.openCategory("Dangdut")

        viewModel.clearGenre()

        assertEquals("", viewModel.state.value.queryText)
        assertNull(viewModel.state.value.activeGenre)
        assertTrue(viewModel.state.value.results.isEmpty())
        assertFalse(viewModel.state.value.hasSearched)
    }
    @Test
    fun `filter change re-searches with selected filter`() {
        val (viewModel, client) = viewModel(initialQuery = "lagu pop")

        viewModel.onFilterChange(SearchFilter.WEEK)

        assertEquals(SearchFilter.WEEK, viewModel.state.value.filter)
        assertEquals(SearchFilter.WEEK, client.filters.last())
    }

}
