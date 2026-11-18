package com.kyrix.ftxmusic.data.update

import org.json.JSONObject

object GithubReleaseParser {

    private val SHA256_PATTERN = Regex(
        """(?i)\b(?:sha-?256|hash)\b[^a-f0-9]{0,24}([a-f0-9]{64})""",
    )

    fun parse(json: String): AvailableUpdate? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val tagName = root.optString("tag_name").trim().takeIf { it.isNotBlank() } ?: return null
        val version = SemVer.parse(tagName) ?: return null
        val assets = root.optJSONArray("assets") ?: return null

        var apkUrl: String? = null
        var apkSize: Long? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            val url = asset.optString("browser_download_url").trim()
            if (url.isBlank()) continue
            apkUrl = url
            apkSize = asset.optLong("size", -1L).takeIf { it > 0L }
            break
        }
        val downloadUrl = apkUrl ?: return null
        val notes = root.optString("body").orEmpty()

        return AvailableUpdate(
            version = version.toString(),
            tagName = tagName,
            notes = notes,
            apkUrl = downloadUrl,
            apkSize = apkSize,
            sha256 = sha256FromNotes(notes),
        )
    }

    fun sha256FromNotes(notes: String?): String? =
        notes?.let { SHA256_PATTERN.find(it)?.groupValues?.getOrNull(1)?.lowercase() }

    fun isNewer(latest: String, current: String): Boolean {
        val remote = SemVer.parse(latest) ?: return false
        val local = SemVer.parse(current) ?: return false
        return remote > local
    }
}
