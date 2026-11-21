package com.rizumu.music.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared geometry/tone so the mini player and dock read as one family. */
object ChromeSpec {
    val Width = 250.dp
    val Height = 60.dp
    val Gap = 8.dp
    val BottomMargin = 16.dp
    val Shape = RoundedCornerShape(30.dp)
    const val TintAlpha = 0.10f
}

@Composable
fun ChromeGlassSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val contentHazeState = LocalHazeState.current
    val chromeHazeState = LocalChromeHazeState.current ?: contentHazeState
    CompositionLocalProvider(LocalHazeState provides chromeHazeState) {
        GlassSurface(
            modifier = modifier,
            shape = ChromeSpec.Shape,
            fillAlpha = 0.88f,
            shadowElevation = 0.dp,
            tintAlpha = ChromeSpec.TintAlpha,
            transparent = true,
            onClick = onClick,
            content = content,
        )
    }
}
