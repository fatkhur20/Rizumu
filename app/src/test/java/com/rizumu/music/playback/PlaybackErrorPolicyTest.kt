package com.rizumu.music.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PlaybackErrorPolicyTest {

    private class ReCaptchaException : RuntimeException()

    @Test
    fun `auto retry is bounded`() {
        assertTrue(PlaybackErrorPolicy.shouldRetry(0))
        assertFalse(PlaybackErrorPolicy.shouldRetry(1))
    }

    @Test
    fun `blocked request has a clear message`() {
        assertEquals(
            "Layanan memblokir permintaan dari jaringan ini. Coba jaringan lain.",
            PlaybackErrorPolicy.friendlyError(IllegalStateException("outer", ReCaptchaException())),
        )
    }

    @Test
    fun `missing stream has a clear message`() {
        assertEquals(
            "Lagu ini tidak tersedia untuk diputar saat ini.",
            PlaybackErrorPolicy.friendlyError(IllegalStateException("No audio stream available for abc")),
        )
    }

    @Test
    fun `network error has a clear message`() {
        assertEquals(
            "Koneksi terganggu. Coba lagi sebentar.",
            PlaybackErrorPolicy.friendlyError(IOException("timeout")),
        )
    }

    @Test
    fun `unknown error uses generic message`() {
        assertEquals(
            "Gagal memutar lagu. Coba lagi sebentar.",
            PlaybackErrorPolicy.friendlyError(IllegalStateException("unknown")),
        )
    }
}
