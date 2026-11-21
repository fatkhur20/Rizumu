package com.kyrix.ftxmusic.playback

import java.io.IOException

object PlaybackErrorPolicy {
    private const val MAX_AUTO_RETRIES = 1

    fun shouldRetry(attempt: Int): Boolean = attempt < MAX_AUTO_RETRIES

    fun friendlyError(error: Throwable): String {
        var cause: Throwable? = error
        while (cause != null) {
            val name = cause.javaClass.simpleName
            val message = cause.message.orEmpty()
            when {
                name.contains("ReCaptcha", ignoreCase = true) ||
                    name.contains("SignInConfirmNotBot", ignoreCase = true) ->
                    return "Layanan memblokir permintaan dari jaringan ini. Coba jaringan lain."
                message.contains("No audio stream", ignoreCase = true) ->
                    return "Lagu ini tidak tersedia untuk diputar saat ini."
                cause is IOException ->
                    return "Koneksi terganggu. Coba lagi sebentar."
            }
            cause = cause.cause
        }
        return "Gagal memutar lagu. Coba lagi sebentar."
    }
}
