package com.rizumu.music.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizumu.music.data.local.UserLibrary
import com.rizumu.music.data.yt.ChannelHit
import com.rizumu.music.data.yt.PageToken
import com.rizumu.music.data.yt.SearchFilter
import com.rizumu.music.data.yt.YoutubeClient
import com.rizumu.music.data.yt.YoutubeTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException

const val FILTER_ALL = SearchFilter.ALL

data class SearchUiState(
    val queryText: String = "",
    val activeGenre: String? = null,
    val filter: String = SearchFilter.ALL,
    val results: List<YoutubeTrack> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false,
    val activeQuery: String = "",
    val matchedChannel: ChannelHit? = null,
)

class SearchViewModel(
    private val youtube: YoutubeClient,
    initialQuery: String = "",
    initialGenre: String = "",
) : ViewModel() {

    private val _state = MutableStateFlow(
        SearchUiState(
            queryText = initialQuery,
            activeQuery = if (initialGenre.isNotBlank()) {
                initialQuery.ifBlank { initialGenre }
            } else {
                initialQuery
            },
        ),
    )
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var debounceJob: Job? = null
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var nextToken: PageToken? = null
    private var activeQuery: String = initialQuery
    private var activeFilter: String = SearchFilter.ALL

    init {
        if (initialGenre.isNotBlank()) {
            openCategory(initialGenre, initialQuery.ifBlank { initialGenre })
        } else if (initialQuery.isNotBlank()) {
            executeSearch(initialQuery)
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(queryText = query, activeGenre = null) }
        debounceJob?.cancel()
        if (query.isBlank()) {
            searchJob?.cancel()
            loadMoreJob?.cancel()
            nextToken = null
            activeQuery = ""
            _state.update {
                it.copy(
                    activeGenre = null,
                    activeQuery = "",
                    results = emptyList(),
                    isLoading = false,
                    hasMore = false,
                    error = null,
                    hasSearched = false,
                    matchedChannel = null,
                )
            }
            return
        }
        debounceJob = viewModelScope.launch {
            delay(300)
            executeSearch(query)
        }
    }

    fun submitQuery(query: String) {
        if (query.isBlank()) return
        debounceJob?.cancel()
        _state.update { it.copy(queryText = query, activeGenre = null) }
        executeSearch(query)
    }

    /**
     * Single entry point for category/suggestion/genre taps. The header keeps the
     * human-readable label while the search request may use a different term.
     */
    fun openCategory(label: String, query: String = label) {
        val trimmedLabel = label.trim()
        val trimmedQuery = query.trim().ifBlank { trimmedLabel }
        if (trimmedLabel.isBlank() || trimmedQuery.isBlank()) return
        debounceJob?.cancel()
        _state.update { it.copy(queryText = "", activeGenre = trimmedLabel) }
        executeSearch(trimmedQuery)
    }

    /** Back/X from the genre results header: return to the discovery screen. */
    fun clearGenre() {
        debounceJob?.cancel()
        searchJob?.cancel()
        loadMoreJob?.cancel()
        nextToken = null
        activeQuery = ""
        _state.update {
            it.copy(
                queryText = "",
                activeGenre = null,
                activeQuery = "",
                results = emptyList(),
                isLoading = false,
                isLoadingMore = false,
                hasMore = false,
                error = null,
                hasSearched = false,
                matchedChannel = null,
            )
        }
    }

    fun onFilterChange(filter: String) {
        if (filter == activeFilter) return
        activeFilter = filter
        _state.update { it.copy(filter = filter) }
        if (activeQuery.isNotBlank()) executeSearch(activeQuery)
    }

    fun retry() {
        if (activeQuery.isNotBlank()) executeSearch(activeQuery)
    }

    fun maybeLoadMore(lastVisibleIndex: Int) {
        val current = _state.value
        if (!current.hasMore || current.isLoading || current.isLoadingMore) return
        if (lastVisibleIndex < current.results.size - 3) return
        loadMore()
    }

    private fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        activeQuery = trimmed
        searchJob?.cancel()
        loadMoreJob?.cancel()
        nextToken = null
        searchJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    activeQuery = trimmed,
                    isLoading = true,
                    error = null,
                    results = emptyList(),
                    hasMore = false,
                    hasSearched = true,
                    matchedChannel = null,
                )
            }
            try {
                val page = youtube.search(trimmed, activeFilter)
                nextToken = page.next
                UserLibrary.recordSearch(trimmed)
                _state.update {
                    it.copy(
                        isLoading = false,
                        results = page.items,
                        hasMore = page.next != null,
                        matchedChannel = page.matchedChannel,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = messageFor(error),
                    )
                }
            }
        }
    }

    private fun loadMore() {
        val token = nextToken ?: return
        if (loadMoreJob?.isActive == true) return
        loadMoreJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                val page = youtube.search(activeQuery, activeFilter, token)
                nextToken = page.next
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        results = it.results + page.items,
                        hasMore = page.next != null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        error = messageFor(error),
                    )
                }
            }
        }
    }

    private fun messageFor(error: Throwable): String {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is SignInConfirmNotBotException) {
                return "Layanan memblokir pencarian dari jaringan ini. Coba jaringan lain."
            }
            cause = cause.cause
        }
        return error.message ?: "Tidak bisa memuat hasil. Coba lagi."
    }
}
