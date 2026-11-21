package com.rizumu.music.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height of the floating bottom chrome (mini player + navigation pill).
 * Scrollable screens add this to their LazyColumn bottom content padding so
 * content can continue behind the glass instead of being clipped above it.
 */
val LocalBottomContentPadding = compositionLocalOf<Dp> { 0.dp }
