package com.rizumu.music.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.rizumu.music.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class UpdateRepository(
    private val context: Context,
    private val checker: UpdateChecker = UpdateChecker(),
    private val store: UpdateStore = UpdateStore(context),
    private val currentVersion: String = BuildConfig.VERSION_NAME,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient()

    init {
        scope.launch { pruneCache() }
    }

    suspend fun check(manual: Boolean): UpdateCheckResult {
        val timestamp = now()
        val lastCheck = runCatching { store.lastCheckAt() }.getOrDefault(0L)
        if (!manual && timestamp - lastCheck in 0L until CHECK_INTERVAL_MS) {
            return UpdateCheckResult.Throttled
        }

        return try {
            val release = checker.fetchLatest(
                owner = BuildConfig.GITHUB_OWNER,
                repo = BuildConfig.GITHUB_REPO,
                userAgent = "Rizumu/$currentVersion",
            )
            runCatching { store.setLastCheckAt(timestamp) }
            when {
                release == null -> UpdateCheckResult.UpToDate
                !GithubReleaseParser.isNewer(release.version, currentVersion) ->
                    UpdateCheckResult.UpToDate
                !manual && runCatching { store.skippedVersion() }.getOrNull() == release.version ->
                    UpdateCheckResult.Skipped(release.version)
                else -> UpdateCheckResult.Available(release)
            }
        } catch (error: Exception) {
            runCatching { store.setLastCheckAt(timestamp) }
            UpdateCheckResult.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Tidak dapat memeriksa pembaruan. Periksa koneksi internet.",
            )
        }
    }

    suspend fun skip(version: String) {
        store.skipVersion(version)
    }

    suspend fun download(
        update: AvailableUpdate,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val directory = cacheDirectory().apply { mkdirs() }
        val target = File(directory, "Rizumu-${safeName(update.version)}.apk")
        val temp = File(directory, "${target.name}.part")
        directory.listFiles()?.forEach { file ->
            if (file != temp && file != target) file.delete()
        }
        temp.delete()
        target.delete()
        onProgress(0f)

        val request = Request.Builder()
            .url(update.apkUrl)
            .header("User-Agent", "Rizumu/$currentVersion")
            .build()

        try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unduhan gagal (${response.code})")
                }
                val body = response.body ?: throw IOException("File APK kosong")
                val total = body.contentLength().takeIf { it > 0L } ?: update.apkSize ?: -1L
                var readTotal = 0L
                var lastPercent = -1

                body.byteStream().use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            readTotal += read
                            if (total > 0L) {
                                val percent = ((readTotal * 100L) / total).toInt()
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    val progress = (readTotal.toDouble() / total).toFloat()
                                        .coerceIn(0f, 1f)
                                    withContext(Dispatchers.Main.immediate) { onProgress(progress) }
                                }
                            }
                        }
                    }
                }
            }

            val expected = update.sha256
            if (expected != null && !sha256(temp).equals(expected, ignoreCase = true)) {
                temp.delete()
                throw IOException("Verifikasi SHA-256 gagal. Unduhan dihapus.")
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            withContext(Dispatchers.Main.immediate) { onProgress(1f) }
            target
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
    }

    fun install(file: File): InstallResult {
        if (!context.packageManager.canRequestPackageInstalls()) {
            return InstallResult.PermissionRequired
        }
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            scheduleCleanup(file)
            InstallResult.Started
        } catch (error: Exception) {
            InstallResult.Failed(
                error.message?.takeIf { it.isNotBlank() } ?: "Tidak dapat membuka pemasang APK.",
            )
        }
    }

    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_SECURITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
    }

    fun pruneCache() {
        runCatching {
            cacheDirectory().listFiles()?.forEach { it.delete() }
        }
    }

    private fun scheduleCleanup(file: File) {
        scope.launch {
            delay(CACHE_CLEANUP_DELAY_MS)
            if (file.exists()) file.delete()
        }
    }

    private fun cacheDirectory(): File = File(context.cacheDir, "updates")

    private fun safeName(value: String): String =
        value.replace(Regex("[^0-9A-Za-z._-]"), "_").ifBlank { "latest" }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
        const val CACHE_CLEANUP_DELAY_MS = 10L * 60L * 1000L
    }
}
