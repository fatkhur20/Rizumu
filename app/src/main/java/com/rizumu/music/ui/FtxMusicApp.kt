package com.rizumu.music.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rizumu.music.BuildConfig
import com.rizumu.music.FtxApp
import com.rizumu.music.data.backup.BackupManager
import com.rizumu.music.data.charts.ChartRepository
import com.rizumu.music.data.update.AvailableUpdate
import com.rizumu.music.data.update.InstallResult
import com.rizumu.music.data.update.UpdateCheckResult
import com.rizumu.music.playback.NoOpEqualizerController
import com.rizumu.music.playback.PlaybackManager
import com.rizumu.music.ui.components.ChromeSpec
import com.rizumu.music.ui.components.DockPager
import com.rizumu.music.ui.components.LiquidBackdrop
import com.rizumu.music.ui.components.LocalBottomContentPadding
import com.rizumu.music.ui.components.LocalChromeHazeState
import com.rizumu.music.ui.components.LocalHazeState
import com.rizumu.music.ui.components.LocalSheetVisibility
import com.rizumu.music.ui.components.SheetVisibilityController
import com.rizumu.music.ui.components.UpdateDialog
import com.rizumu.music.ui.components.UpdateDialogState
import com.rizumu.music.ui.screens.HomeScreen
import com.rizumu.music.ui.screens.LibraryScreen
import com.rizumu.music.ui.screens.NowPlayingScreen
import com.rizumu.music.ui.screens.PlaylistScreen
import com.rizumu.music.ui.screens.SearchScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class SearchRequest(
    val query: String = "",
    val genre: String = "",
    val key: Int = 0,
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FtxMusicApp(playback: PlaybackManager, charts: ChartRepository) {
    val state by playback.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val pagerState = rememberPagerState(pageCount = { 3 })
    val selectedRoute = when {
        currentRoute?.startsWith("playlist") == true -> "library"
        pagerState.currentPage == 1 -> "search"
        pagerState.currentPage == 2 -> "library"
        else -> "home"
    }
    val reduceMotion = rememberReducedMotion()
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var playerPageRequest by remember { mutableIntStateOf(0) }
    var navPageRequest by remember { mutableIntStateOf(0) }
    var pagerTouching by remember { mutableStateOf(false) }
    var searchRequest by remember { mutableStateOf(SearchRequest()) }
    val openPlayer: () -> Unit = {
        playerPageRequest++
        showPlayer = true
    }
    val context = LocalContext.current
    val app = context.applicationContext as FtxApp
    val scope = rememberCoroutineScope()
    val openSearchTab: (String, String) -> Unit = { query, genre ->
        searchRequest = searchRequest.copy(
            query = query,
            genre = genre,
            key = searchRequest.key + 1,
        )
        if (currentRoute?.startsWith("playlist") == true) {
            navController.popBackStack("main", false)
        }
        scope.launch { pagerState.animateScrollToPage(1) }
    }
    val equalizer = remember { NoOpEqualizerController() }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateDialog by remember { mutableStateOf<UpdateDialogState?>(null) }
    val contentHazeState = remember { HazeState() }
    val chromeHazeState = remember { HazeState() }
    val sheetVisibility = remember { SheetVisibilityController() }
    val density = LocalDensity.current
    val navBottom = with(density) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }

    val checkUpdates: (Boolean) -> Unit = { manual ->
        if (!checkingUpdate) {
            checkingUpdate = true
            scope.launch {
                when (val result = app.updates.check(manual)) {
                    is UpdateCheckResult.Available -> {
                        updateDialog = UpdateDialogState.Available(result.update)
                    }
                    is UpdateCheckResult.Failed -> if (manual) {
                        updateDialog = UpdateDialogState.Message(
                            title = "Gagal memeriksa pembaruan",
                            message = result.message,
                        )
                    }
                    UpdateCheckResult.UpToDate -> if (manual) {
                        updateDialog = UpdateDialogState.Message(
                            title = "Rizumu sudah terbaru",
                            message = "Versi ${BuildConfig.VERSION_NAME} adalah versi terbaru.",
                        )
                    }
                    is UpdateCheckResult.Skipped -> if (manual) {
                        updateDialog = UpdateDialogState.Message(
                            title = "Versi ${result.version} dilewati",
                            message = "Pembaruan ini ditandai lewati. Cek manual lagi untuk menampilkannya.",
                        )
                    }
                    UpdateCheckResult.Throttled -> Unit
                }
                checkingUpdate = false
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val message = runCatching {
                    val json = BackupManager.export()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)
                            ?.bufferedWriter()
                            ?.use { it.write(json) }
                            ?: error("Tidak dapat membuka file tujuan.")
                    }
                    "Backup diekspor."
                }.getOrElse { error ->
                    "Ekspor gagal: ${error.message?.takeIf { it.isNotBlank() } ?: "coba lagi"}"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val message = runCatching {
                    val raw = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?: error("Tidak dapat membaca file backup.")
                    }
                    val summary = BackupManager.restore(raw)
                    "Backup digabung: ${summary.favorites} suka, ${summary.tracks} lagu playlist."
                }.getOrElse { error ->
                    "Impor gagal: ${error.message?.takeIf { it.isNotBlank() } ?: "file tidak valid"}"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    val exportBackup = {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        exportLauncher.launch("rizumu-backup-$stamp.json")
    }
    val importBackup = {
        importLauncher.launch(
            arrayOf("application/json", "text/plain", "application/octet-stream"),
        )
    }

    val installUpdate: (AvailableUpdate) -> Unit = { update ->
        scope.launch {
            updateDialog = UpdateDialogState.Downloading(update, 0f)
            try {
                val file = app.updates.download(update) { progress ->
                    updateDialog = UpdateDialogState.Downloading(update, progress)
                }
                when (val result = app.updates.install(file)) {
                    InstallResult.Started -> updateDialog = null
                    InstallResult.PermissionRequired -> {
                        updateDialog = UpdateDialogState.InstallPending(
                            update = update,
                            file = file,
                            message = "Izinkan Rizumu memasang aplikasi dari sumber ini, lalu tekan Pasang.",
                        )
                        app.updates.openInstallPermissionSettings()
                    }
                    is InstallResult.Failed -> {
                        updateDialog = UpdateDialogState.Message(
                            title = "Pemasangan gagal",
                            message = result.message,
                        )
                    }
                }
            } catch (error: Exception) {
                updateDialog = UpdateDialogState.Message(
                    title = "Unduhan gagal",
                    message = error.message?.takeIf { it.isNotBlank() }
                        ?: "APK tidak dapat diunduh. Coba lagi.",
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val result = app.updates.check(manual = false)
        if (result is UpdateCheckResult.Available) {
            updateDialog = UpdateDialogState.Available(result.update)
        }
    }

    LaunchedEffect(pagerState) {
        var swipeActive = false
        snapshotFlow {
            val moving = pagerState.isScrollInProgress ||
                pagerState.currentPageOffsetFraction != 0f
            moving to pagerTouching
        }
            .distinctUntilChanged()
            .collect { (scrolling, touching) ->
                if (scrolling && touching && !swipeActive) {
                    swipeActive = true
                    navPageRequest++
                } else if (!scrolling && swipeActive) {
                    swipeActive = false
                    playerPageRequest++
                }
            }
    }

    PredictiveBackHandler(enabled = showPlayer) { progress ->
        try {
            progress.collect { event -> backProgress = event.progress }
            showPlayer = false
        } finally {
            backProgress = 0f
        }
    }

    BackHandler(
        enabled = !showPlayer &&
            currentRoute == "main" &&
            pagerState.currentPage != 0 &&
            !sheetVisibility.open &&
            updateDialog == null,
    ) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    CompositionLocalProvider(
        LocalHazeState provides contentHazeState,
        LocalChromeHazeState provides chromeHazeState,
        LocalSheetVisibility provides sheetVisibility,
        LocalBottomContentPadding provides ChromeSpec.Height + ChromeSpec.BottomMargin + navBottom + 12.dp,
    ) {
    SharedTransitionLayout(Modifier.fillMaxSize()) {
    val sharedTransitionScope = this
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .haze(chromeHazeState),
        ) {
        LiquidBackdrop(artworkUrl = state.artworkUrl)
        NavHost(
                navController = navController,
                startDestination = "main",
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                enterTransition = {
                    if (reduceMotion) {
                        EnterTransition.None
                    } else {
                        fadeIn(tween(FtxMotion.page)) +
                            slideInHorizontally(tween(FtxMotion.page)) { it / 22 }
                    }
                },
                exitTransition = {
                    if (reduceMotion) ExitTransition.None else fadeOut(tween(140))
                },
                popEnterTransition = {
                    if (reduceMotion) {
                        EnterTransition.None
                    } else {
                        fadeIn(tween(FtxMotion.page)) +
                            slideInHorizontally(tween(FtxMotion.page)) { -it / 22 }
                    }
                },
                popExitTransition = {
                    if (reduceMotion) {
                        ExitTransition.None
                    } else {
                        fadeOut(tween(140)) +
                            slideOutHorizontally(tween(140)) { it / 22 }
                    }
                },
            ) {
                composable("main") {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !sheetVisibility.open,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Initial,
                                    )
                                    pagerTouching = true
                                    try {
                                        var pressed = true
                                        while (pressed) {
                                            pressed = awaitPointerEvent(PointerEventPass.Initial)
                                                .changes
                                                .any { it.pressed }
                                        }
                                    } finally {
                                        pagerTouching = false
                                    }
                                }
                            },
                    ) { page ->
                        when (page) {
                            0 -> HomeScreen(
                                playback = playback,
                                charts = charts,
                                equalizer = equalizer,
                                checkingUpdate = checkingUpdate,
                                onCheckUpdate = { checkUpdates(true) },
                                onOpenSearch = { query -> openSearchTab(query, "") },
                                onOpenCategory = { label, query ->
                                    openSearchTab(query, label)
                                },
                                onOpenPlayer = openPlayer,
                                onExportBackup = exportBackup,
                                onImportBackup = importBackup,
                            )
                            1 -> SearchScreen(
                                playback = playback,
                                initialQuery = searchRequest.query,
                                initialGenre = searchRequest.genre,
                                requestKey = searchRequest.key,
                            )
                            else -> LibraryScreen(
                                playback = playback,
                                onOpenSearch = { query -> openSearchTab(query, "") },
                                onOpenPlaylist = { id ->
                                    navController.navigate("playlist/$id") {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                    }
                }
                composable(
                    route = "playlist/{id}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                    ),
                ) { entry ->
                    PlaylistScreen(
                        playlistId = entry.arguments?.getLong("id") ?: 0L,
                        playback = playback,
                        onBack = { navController.popBackStack() },
                        onOpenArtist = { artist -> openSearchTab(artist, "") },
                    )
                }
        }
        }

        AnimatedContent(
            targetState = showPlayer,
            transitionSpec = {
                if (reduceMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(tween(320, delayMillis = 90)) togetherWith fadeOut(tween(180))
                }
            },
            label = "playerShell",
            modifier = Modifier.fillMaxSize(),
        ) { playerOpen ->
            val visibilityScope = this
            Box(Modifier.fillMaxSize()) {
                if (playerOpen) {
                    NowPlayingScreen(
                        state = state,
                        playback = playback,
                        equalizer = equalizer,
                        onClose = { showPlayer = false },
                        onOpenArtist = { artist ->
                            showPlayer = false
                            openSearchTab(artist, "")
                        },
                        onTogglePlayPause = playback::togglePlayPause,
                        onNext = playback::next,
                        onPrevious = playback::previous,
                        onSeek = playback::seekTo,
                        onToggleShuffle = playback::toggleShuffle,
                        onCycleRepeat = playback::cycleRepeat,
                        onPlayAt = playback::playAt,
                        backProgress = { backProgress },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = visibilityScope,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AnimatedVisibility(
                        visible = !sheetVisibility.open,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f),
                        enter = if (reduceMotion) {
                            EnterTransition.None
                        } else {
                            slideInVertically(tween(180)) { it } + fadeIn(tween(120))
                        },
                        exit = if (reduceMotion) {
                            ExitTransition.None
                        } else {
                            slideOutVertically(tween(160)) { it } + fadeOut(tween(120))
                        },
                    ) {
                    DockPager(
                        state = state,
                        selectedRoute = selectedRoute,
                        onNavigate = { route ->
                            val page = when (route) {
                                "home" -> 0
                                "search" -> 1
                                else -> 2
                            }
                            if (currentRoute?.startsWith("playlist") == true) {
                                navController.popBackStack("main", false)
                            }
                            if (pagerState.currentPage != page) {
                                scope.launch { pagerState.animateScrollToPage(page) }
                            }
                        },
                        onOpenPlayer = openPlayer,
                        onTogglePlayPause = playback::togglePlayPause,
                        onNext = playback::next,
                        onPrevious = playback::previous,
                        playerPageRequest = playerPageRequest,
                        navPageRequest = navPageRequest,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = visibilityScope,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                bottom = ChromeSpec.BottomMargin,
                            ),
                    )
                    }
                }
            }
        }
    }
    }
    }

    updateDialog?.let { dialog ->
        UpdateDialog(
            state = dialog,
            onPrimary = {
                when (val current = updateDialog) {
                    is UpdateDialogState.Available -> installUpdate(current.update)
                    is UpdateDialogState.InstallPending -> {
                        when (val result = app.updates.install(current.file)) {
                            InstallResult.Started -> updateDialog = null
                            InstallResult.PermissionRequired -> {
                                app.updates.openInstallPermissionSettings()
                                updateDialog = current.copy(
                                    message = "Izinkan Rizumu memasang aplikasi dari sumber ini, lalu tekan Pasang.",
                                )
                            }
                            is InstallResult.Failed -> {
                                updateDialog = UpdateDialogState.Message(
                                    title = "Pemasangan gagal",
                                    message = result.message,
                                )
                            }
                        }
                    }
                    is UpdateDialogState.Message -> updateDialog = null
                    else -> Unit
                }
            },
            onLater = {
                if (dialog !is UpdateDialogState.Downloading) updateDialog = null
            },
            onSkip = {
                (dialog as? UpdateDialogState.Available)?.let { available ->
                    scope.launch {
                        app.updates.skip(available.update.version)
                        updateDialog = null
                    }
                }
            },
        )
    }
}
