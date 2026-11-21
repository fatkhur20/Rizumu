package com.rizumu.music.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizumu.music.data.charts.ChartEntry
import com.rizumu.music.data.charts.ChartKind
import com.rizumu.music.data.charts.ChartRegion
import com.rizumu.music.data.charts.ChartRepository
import com.rizumu.music.data.charts.ChartResult
import com.rizumu.music.data.local.UserLibrary
import com.rizumu.music.data.yt.YoutubeClient
import com.rizumu.music.data.yt.YoutubeTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory

data class HomeSection(
    val loading: Boolean = false,
    val items: List<YoutubeTrack> = emptyList(),
    val error: String? = null,
)

data class ChartUiState(
    val loading: Boolean = false,
    val entries: List<ChartEntry> = emptyList(),
    val error: String? = null,
    val cached: Boolean = false,
    val fetchedAt: Long? = null,
    val source: String? = null,
    val chartTitle: String? = null,
)

data class HomeUiState(
    val region: ChartRegion = ChartRegion.INDONESIA,
    val trending: ChartUiState = ChartUiState(loading = true),
    val global: ChartUiState = ChartUiState(loading = true),
    val recommended: HomeSection = HomeSection(),
)

class HomeViewModel(
    private val youtube: YoutubeClient,
    private val charts: ChartRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var recommendedSeed: String? = null
    private var lastAutoRefreshAt = 0L

    init {
        lastAutoRefreshAt = System.currentTimeMillis()
        loadTrending(forceRefresh = true)
        loadGlobal(forceRefresh = true)
        loadRecommended()
    }

    fun refreshOnOpen() {
        val now = System.currentTimeMillis()
        if (now - lastAutoRefreshAt < AUTO_REFRESH_THROTTLE_MS) return
        lastAutoRefreshAt = now
        loadTrending(forceRefresh = true)
        loadGlobal(forceRefresh = true)
    }

    fun selectRegion(region: ChartRegion) {
        if (region.code == _state.value.region.code) return
        _state.update { it.copy(region = region, trending = ChartUiState(loading = true)) }
        loadTrending()
    }

    fun refreshTrending() = loadTrending(forceRefresh = true)

    fun refreshGlobal() = loadGlobal(forceRefresh = true)

    fun retryTrending() = loadTrending(forceRefresh = true)

    fun retryGlobal() = loadGlobal(forceRefresh = true)

    fun retryRecommended() = loadRecommended(force = true)

    fun ensureRecommended() {
        val seed = seedKey() ?: return
        if (seed == recommendedSeed) return
        loadRecommended()
    }

    private fun loadTrending(forceRefresh: Boolean = false) {
        val region = _state.value.region
        charts.cached(region, ChartKind.SONGS)?.let { snapshot ->
            _state.update {
                it.copy(
                    trending = ChartUiState(
                        loading = true,
                        entries = snapshot.entries,
                        cached = true,
                        fetchedAt = snapshot.fetchedAt,
                        source = snapshot.source,
                        chartTitle = snapshot.chartTitle,
                    ),
                )
            }
        } ?: _state.update { it.copy(trending = it.trending.copy(loading = true, error = null)) }

        viewModelScope.launch {
            try {
                val result = charts.getTrendingSongs(region, limit = CHART_LIMIT, forceRefresh = forceRefresh)
                if (_state.value.region.code != region.code) return@launch
                _state.update { it.copy(trending = result.toUiState()) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (_state.value.region.code != region.code) return@launch
                _state.update {
                    it.copy(
                        trending = it.trending.copy(
                            loading = false,
                            error = error.message ?: DEFAULT_CHART_ERROR,
                        ),
                    )
                }
            }
        }
    }

    private fun loadGlobal(forceRefresh: Boolean = false) {
        charts.cached(ChartRegion.GLOBAL, ChartKind.MUSIC_VIDEOS)?.let { snapshot ->
            _state.update {
                it.copy(
                    global = ChartUiState(
                        loading = true,
                        entries = snapshot.entries,
                        cached = true,
                        fetchedAt = snapshot.fetchedAt,
                        source = snapshot.source,
                        chartTitle = snapshot.chartTitle,
                    ),
                )
            }
        } ?: _state.update { it.copy(global = it.global.copy(loading = true, error = null)) }

        viewModelScope.launch {
            try {
                val result = charts.getGlobalTopMusicVideos(limit = CHART_LIMIT, forceRefresh = forceRefresh)
                _state.update { it.copy(global = result.toUiState()) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        global = it.global.copy(
                            loading = false,
                            error = error.message ?: DEFAULT_CHART_ERROR,
                        ),
                    )
                }
            }
        }
    }

    private fun ChartResult.toUiState(): ChartUiState = ChartUiState(
        loading = false,
        entries = snapshot.entries,
        error = refreshError,
        cached = fromCache,
        fetchedAt = snapshot.fetchedAt,
        source = snapshot.source,
        chartTitle = snapshot.chartTitle,
    )

    private fun loadRecommended(force: Boolean = false) {
        val seed = UserLibrary.state.value.recent.firstOrNull() ?: return
        val seedText = seedKey() ?: return
        if (!force && seedText == recommendedSeed) return
        recommendedSeed = seedText
        load(
            query = "${seed.artist.ifBlank { seed.title }} mix",
            update = { section -> _state.update { it.copy(recommended = section) } },
            current = _state.value.recommended,
        )
    }

    private fun seedKey(): String? {
        val seed = UserLibrary.state.value.recent.firstOrNull() ?: return null
        return seed.artist.ifBlank { seed.title }.takeIf { it.isNotBlank() }
    }

    private fun load(
        query: String,
        current: HomeSection,
        update: (HomeSection) -> Unit,
    ) {
        update(current.copy(loading = true, error = null))
        viewModelScope.launch {
            try {
                val page = youtube.search(query, YoutubeSearchQueryHandlerFactory.MUSIC_SONGS)
                update(HomeSection(loading = false, items = page.items.take(12)))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                update(HomeSection(loading = false, error = messageFor(error)))
            }
        }
    }

    private fun messageFor(error: Throwable): String {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is SignInConfirmNotBotException) {
                return "Layanan memblokir permintaan dari jaringan ini."
            }
            cause = cause.cause
        }
        return error.message ?: "Tidak bisa memuat. Periksa koneksi lalu coba lagi."
    }

    private companion object {
        const val CHART_LIMIT = 20
        const val AUTO_REFRESH_THROTTLE_MS = 5_000L
        const val DEFAULT_CHART_ERROR = "Tidak bisa memuat tangga lagu."
    }
}
