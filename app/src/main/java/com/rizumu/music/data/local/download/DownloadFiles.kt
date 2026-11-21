package com.rizumu.music.data.local.download

import android.content.Context
import java.io.File

object DownloadFiles {

    fun directory(context: Context): File =
        File(context.applicationContext.filesDir, "downloads").apply { mkdirs() }

    fun target(context: Context, videoId: String, suffix: String): File =
        File(directory(context), "${safeName(videoId)}.${safeSuffix(suffix)}")

    fun partial(context: Context, videoId: String, suffix: String): File =
        File(directory(context), "${safeName(videoId)}.${safeSuffix(suffix)}.part")

    fun find(context: Context, videoId: String): File? {
        val prefix = "${safeName(videoId)}."
        return directory(context)
            .listFiles()
            ?.firstOrNull { file ->
                file.isFile && file.name.startsWith(prefix) && !file.name.endsWith(".part") &&
                    file.length() > 0L
            }
    }

    fun delete(context: Context, videoId: String) {
        val prefix = "${safeName(videoId)}."
        directory(context).listFiles()
            ?.filter { it.name.startsWith(prefix) }
            ?.forEach { it.delete() }
    }

    fun safeSuffix(raw: String): String =
        raw.lowercase()
            .filter { it.isLetterOrDigit() }
            .take(8)
            .ifBlank { "m4a" }

    private fun safeName(raw: String): String =
        raw.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            .ifBlank { "track" }
}
