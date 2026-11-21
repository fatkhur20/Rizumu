package com.rizumu.music.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Keeps cover-derived backdrop colors dark enough that white text always wins.
 *
 * The app is permanently dark, so backdrop colors are clamped by HSL lightness
 * instead of by value alone. Yellow/cyan/magenta are the worst case because two
 * channels stay bright; MAX_LIGHTNESS = 0.24 plus a saturation cap keeps the
 * worst-case contrast above 4.5:1 against white.
 */
object BackdropColor {
    const val MAX_LIGHTNESS = 0.24f
    const val MAX_SATURATION = 0.78f

    const val BLOB_MIN_LIGHTNESS = 0.25f
    const val BLOB_MAX_LIGHTNESS = 0.45f
    const val BLOB_MIN_SATURATION = 0.40f

    fun clamp(argb: Int): Color = Color(clampArgb(argb))

    fun blob(argb: Int): Color = Color(blobArgb(argb))

    fun blobArgb(argb: Int): Int {
        val hsl = rgbToHslComponents(argb)
        val saturation = hsl[1].coerceIn(BLOB_MIN_SATURATION, MAX_SATURATION)
        val lightness = hsl[2].coerceIn(BLOB_MIN_LIGHTNESS, BLOB_MAX_LIGHTNESS)
        return hslToArgb((argb ushr 24) and 0xFF, hsl[0], saturation, lightness)
    }

    fun clampArgb(argb: Int): Int {
        val hsl = rgbToHslComponents(argb)
        val saturation = (hsl[1] * 0.82f).coerceAtMost(MAX_SATURATION)
        val lightness = hsl[2].coerceIn(0f, MAX_LIGHTNESS)
        return hslToArgb((argb ushr 24) and 0xFF, hsl[0], saturation, lightness)
    }

    fun relativeLuminance(argb: Int): Float {
        val r = linearize(((argb shr 16) and 0xFF) / 255f)
        val g = linearize(((argb shr 8) and 0xFF) / 255f)
        val b = linearize((argb and 0xFF) / 255f)
        return r * 0.2126f + g * 0.7152f + b * 0.0722f
    }

    fun contrastRatio(foregroundArgb: Int, backgroundArgb: Int): Float {
        val first = relativeLuminance(foregroundArgb)
        val second = relativeLuminance(backgroundArgb)
        val lighter = max(first, second)
        val darker = min(first, second)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun linearize(channel: Float): Float =
        if (channel <= 0.03928f) {
            channel / 12.92f
        } else {
            ((channel + 0.055f) / 1.055f).pow(2.4f)
        }

    private fun Float.toChannel(): Int = (this * 255f).roundToInt().coerceIn(0, 255)

    private fun rgbToHslComponents(argb: Int): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val maximum = max(r, max(g, b))
        val minimum = min(r, min(g, b))
        val lightness = (maximum + minimum) / 2f
        if (maximum == minimum) return floatArrayOf(0f, 0f, lightness)

        val delta = maximum - minimum
        val saturation = if (lightness > 0.5f) {
            delta / (2f - maximum - minimum)
        } else {
            delta / (maximum + minimum)
        }
        val hue = when (maximum) {
            r -> (g - b) / delta + if (g < b) 6f else 0f
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        } / 6f
        return floatArrayOf(hue, saturation, lightness)
    }

    private fun hslToArgb(alpha: Int, hue: Float, saturation: Float, lightness: Float): Int {
        val rgb = hslToRgb(hue, saturation, lightness)
        return (alpha shl 24) or
            (rgb[0].toChannel() shl 16) or
            (rgb[1].toChannel() shl 8) or
            rgb[2].toChannel()
    }

    private fun hslToRgb(hue: Float, saturation: Float, lightness: Float): FloatArray {
        if (saturation == 0f) return floatArrayOf(lightness, lightness, lightness)
        val q = if (lightness < 0.5f) {
            lightness * (1f + saturation)
        } else {
            lightness + saturation - lightness * saturation
        }
        val p = 2f * lightness - q
        return floatArrayOf(
            hueToChannel(p, q, hue + 1f / 3f),
            hueToChannel(p, q, hue),
            hueToChannel(p, q, hue - 1f / 3f),
        )
    }

    private fun hueToChannel(p: Float, q: Float, hue: Float): Float {
        val wrapped = when {
            hue < 0f -> hue + 1f
            hue > 1f -> hue - 1f
            else -> hue
        }
        return when {
            wrapped < 1f / 6f -> p + (q - p) * 6f * wrapped
            wrapped < 1f / 2f -> q
            wrapped < 2f / 3f -> p + (q - p) * (2f / 3f - wrapped) * 6f
            else -> p
        }
    }
}
