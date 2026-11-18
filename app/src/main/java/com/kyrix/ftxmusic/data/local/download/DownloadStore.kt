package com.kyrix.ftxmusic.data.local.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kyrix.ftxmusic.data.yt.YoutubeTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

object DownloadStore {

    private lateinit var appContext: Context
    private lateinit var dao: DownloadDao
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _downloads = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val downloads: StateFlow<List<DownloadEntity>> = _downloads.asStateFlow()

    fun init(context: Context) {
        if (::dao.isInitialized) return
        appContext = context.applicationContext
        dao = DownloadDatabase.get(context).downloadDao()
        scope.launch {
            dao.observeAll().collect { _downloads.value = it }
        }
    }

    fun enqueue(context: Context, track: YoutubeTrack) {
        if (track.videoId.isBlank()) return
        if (_downloads.value.any { it.videoId == track.videoId && it.status != DownloadStatus.FAILED }) {
            return
        }
        val now = System.currentTimeMillis()
        scope.launch {
            dao.upsert(
                DownloadEntity(
                    videoId = track.videoId,
                    title = track.title,
                    artist = track.artist,
                    durationSec = track.durationSec,
                    thumbnailUrl = track.thumbnailUrl,
                    status = DownloadStatus.QUEUED,
                    progress = 0,
                    fileName = null,
                    mimeType = null,
                    bytes = 0L,
                    error = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setInputData(workDataOf(DownloadWorker.KEY_VIDEO_ID to track.videoId))
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                workName(track.videoId),
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }

    fun remove(context: Context, videoId: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(workName(videoId))
        scope.launch {
            dao.delete(videoId)
            DownloadFiles.delete(context, videoId)
        }
    }

    fun fileFor(videoId: String): File? {
        if (!::appContext.isInitialized) return null
        val row = _downloads.value.firstOrNull {
            it.videoId == videoId && it.status == DownloadStatus.COMPLETE
        } ?: return null
        val file = row.fileName?.let { File(DownloadFiles.directory(appContext), it) } ?: return null
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    fun totalBytes(): Long = _downloads.value
        .filter { it.status == DownloadStatus.COMPLETE }
        .sumOf { it.bytes }

    private fun workName(videoId: String): String = "download-$videoId"
}
