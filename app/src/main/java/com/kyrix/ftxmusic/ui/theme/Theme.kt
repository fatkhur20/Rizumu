package com.kyrix.ftxmusic.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val FtxColorScheme = darkColorScheme(
    primary = FtxAccent,
    onPrimary = FtxBlack,
    primaryContainer = FtxSurfaceElevated,
    onPrimaryContainer = FtxText,
    secondary = FtxTextMuted,
    onSecondary = FtxBlack,
    secondaryContainer = FtxSurfaceElevated,
    onSecondaryContainer = FtxText,
    background = FtxBlack,
    onBackground = FtxText,
    surface = FtxSurface,
    onSurface = FtxText,
    surfaceVariant = FtxSurfaceVariant,
    onSurfaceVariant = FtxTextMuted,
    surfaceTint = FtxAccent,
    outline = FtxOutline,
)

@Composable
fun FtxMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FtxColorScheme,
        typography = FtxTypography,
    ) {
        CompositionLocalProvider(LocalContentColor provides FtxText, content = content)
    }
}
