package com.kyrix.ftxmusic.data.local.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kyrix.ftxmusic.FtxApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoId = inputData.getString(KEY_VIDEO_ID)?.takeIf { it.isNotBlank() }
            ?: return@withContext Result.failure()
        val app = applicationContext as? FtxApp ?: return@withContext Result.failure()
        val dao = DownloadDatabase.get(applicationContext).downloadDao()
        val row = dao.get(videoId) ?: return@withContext Result.failure()
        val now = System.currentTimeMillis()
        dao.markRunning(videoId, DownloadStatus.RUNNING, 0, now)

        var partial: java.io.File? = null
        try {
            val resolved = app.playback.youtube.resolveAudio(videoId)
            val target = DownloadFiles.target(applicationContext, videoId, resolved.suffix)
            partial = DownloadFiles.partial(applicationContext, videoId, resolved.suffix)
            partial.delete()
            target.delete()

            val request = Request.Builder()
                .url(resolved.url)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Server audio menolak unduhan (${response.code}).")
                }
                val body = response.body ?: throw IOException("Respons audio kosong.")
                val total = body.contentLength()
                var copied = 0L
                var lastProgress = -1
                var lastUpdate = 0L
                body.byteStream().use { input ->
                    partial.outputStream().buffered().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            if (isStopped) throw CancellationException("Unduhan dihentikan.")
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (total > 0L) {
                                val percent = ((copied * 100L) / total).toInt().coerceIn(0, 100)
                                val current = System.currentTimeMillis()
                                if (percent != lastProgress && current - lastUpdate >= PROGRESS_INTERVAL_MS) {
                                    lastProgress = percent
                                    lastUpdate = current
                                    dao.updateProgress(videoId, percent, current)
                                    setProgress(workDataOf(KEY_PROGRESS to percent))
                                }
                            }
                        }
                    }
                }
            }

            if (!partial.isFile || partial.length() <= 0L) {
                throw IOException("File audio kosong.")
            }
            if (dao.get(videoId) == null) {
                partial.delete()
                target.delete()
                return@withContext Result.failure()
            }
            if (!partial.renameTo(target)) {
                throw IOException("Gagal menyimpan file audio.")
            }
            dao.markComplete(
                videoId = videoId,
                status = DownloadStatus.COMPLETE,
                fileName = target.name,
                mimeType = resolved.mimeType,
                bytes = target.length(),
                updatedAt = System.currentTimeMillis(),
            )
            setProgress(workDataOf(KEY_PROGRESS to 100))
            Result.success()
        } catch (error: CancellationException) {
            partial?.delete()
            dao.markFailed(
                videoId,
                DownloadStatus.FAILED,
                "Unduhan dihentikan.",
                System.currentTimeMillis(),
            )
            throw error
        } catch (error: Exception) {
            partial?.delete()
            dao.markFailed(
                videoId,
                DownloadStatus.FAILED,
                error.message?.takeIf { it.isNotBlank() } ?: "Unduhan gagal.",
                System.currentTimeMillis(),
            )
            if (error is IOException) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_VIDEO_ID = "videoId"
        const val KEY_PROGRESS = "progress"
        private const val BUFFER_SIZE = 64 * 1024
        private const val PROGRESS_INTERVAL_MS = 350L
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0.0.0 Mobile Safari/537.36"

        private val client: OkHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
