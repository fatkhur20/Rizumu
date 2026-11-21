package com.rizumu.music.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class UpdateChecker(
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun fetchLatest(
        owner: String,
        repo: String,
        userAgent: String,
    ): AvailableUpdate? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", userAgent)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub menolak permintaan (${response.code})")
            }
            val body = response.body?.string() ?: throw IOException("Respons GitHub kosong")
            GithubReleaseParser.parse(body)
        }
    }
}
