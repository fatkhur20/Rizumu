package com.rizumu.music.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackdropColorTest {

    private val white = 0xFFFFFFFF.toInt()

    @Test
    fun `extreme cover colors keep white text above 4_5 contrast`() {
        val covers = mapOf(
            "white" to 0xFFFFFFFF.toInt(),
            "yellow" to 0xFFFFFF00.toInt(),
            "red" to 0xFFFF0000.toInt(),
            "blue" to 0xFF0000FF.toInt(),
            "green" to 0xFF00FF00.toInt(),
            "cyan" to 0xFF00FFFF.toInt(),
            "magenta" to 0xFFFF00FF.toInt(),
        )

        covers.forEach { (name, cover) ->
            val backdrop = BackdropColor.clampArgb(cover)
            val ratio = BackdropColor.contrastRatio(white, backdrop)
            assertTrue(
                "$name backdrop contrast was $ratio, expected >= 4.5",
                ratio >= 4.5f,
            )
        }
    }

    @Test
    fun `mesh blobs keep white text above 4_5 contrast over the base`() {
        val base = 0xFF0A0A0F.toInt()
        val covers = listOf(
            0xFFFFFFFF.toInt(),
            0xFFFFFF00.toInt(),
            0xFFFF0000.toInt(),
            0xFF0000FF.toInt(),
            0xFF00FF00.toInt(),
            0xFF00FFFF.toInt(),
            0xFFFF00FF.toInt(),
        )

        covers.forEach { cover ->
            val blob = BackdropColor.blobArgb(cover)
            val composited = composite(blob, base, 0.46f)
            val ratio = BackdropColor.contrastRatio(white, composited)
            assertTrue(
                "blob ${cover.toUInt().toString(16)} contrast was $ratio, expected >= 4.5",
                ratio >= 4.5f,
            )
        }
    }

    @Test
    fun `clamped backdrop stays in the dark luminance range`() {
        val backdrop = BackdropColor.clampArgb(0xFFFFFF00.toInt())

        assertTrue(BackdropColor.relativeLuminance(backdrop) <= 0.18f)
    }

    @Test
    fun `clamp preserves alpha and returns an opaque color for palette input`() {
        val clamped = BackdropColor.clampArgb(0x80FF0000.toInt())

        assertEquals(0x80, (clamped ushr 24) and 0xFF)
        assertEquals(0xFF, (BackdropColor.clampArgb(0xFFFF0000.toInt()) ushr 24) and 0xFF)
    }

    @Test
    fun `already dark colors keep white contrast`() {
        val dark = 0xFF101018.toInt()

        assertTrue(BackdropColor.contrastRatio(white, BackdropColor.clampArgb(dark)) >= 4.5f)
    }

    private fun composite(source: Int, destination: Int, alpha: Float): Int {
        fun channel(shift: Int): Int {
            val src = (source shr shift) and 0xFF
            val dst = (destination shr shift) and 0xFF
            return (src * alpha + dst * (1f - alpha)).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
