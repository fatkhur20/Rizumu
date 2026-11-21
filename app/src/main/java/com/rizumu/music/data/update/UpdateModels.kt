package com.rizumu.music.data.update

data class AvailableUpdate(
    val version: String,
    val tagName: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long?,
    val sha256: String?,
)

sealed interface UpdateCheckResult {
    data class Available(val update: AvailableUpdate) : UpdateCheckResult
    data class Skipped(val version: String) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Throttled : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

sealed interface InstallResult {
    data object Started : InstallResult
    data object PermissionRequired : InstallResult
    data class Failed(val message: String) : InstallResult
}
