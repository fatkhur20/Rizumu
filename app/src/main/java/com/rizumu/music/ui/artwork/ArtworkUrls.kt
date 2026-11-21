package com.rizumu.music.ui.artwork

object ArtworkUrls {

    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
    private val YOUTUBE_VARIANTS = listOf(
        "maxresdefault",
        "sddefault",
        "hqdefault",
        "mqdefault",
    )

    fun chain(url: String?, videoId: String?, profile: ArtworkProfile): List<String> {
        val urls = LinkedHashSet<String>()
        val validId = videoId?.takeIf { VIDEO_ID.matches(it) }
        val source = url?.takeIf { it.isNotBlank() }
        val sourceIsGoogle = source != null && isGoogleCdn(source)

        // Square album covers (music.youtube.com) must win over 16:9 video frames.
        if (source != null && sourceIsGoogle) {
            urls += resize(source, profile.targetPx)
        }
        if (validId != null) {
            YOUTUBE_VARIANTS.forEach { variant ->
                urls += "https://i.ytimg.com/vi/$validId/$variant.jpg"
            }
        }
        if (source != null && !sourceIsGoogle) {
            // A video thumbnail source is already covered by the variant chain above.
            val coveredByVariants = validId != null &&
                (source.contains("i.ytimg.com") || source.contains("img.youtube.com"))
            if (!coveredByVariants) {
                urls += resize(source, profile.targetPx)
            }
        }
        return urls.toList()
    }

    fun resize(url: String, targetPx: Int): String {
        if (targetPx <= 0) return url
        if (!isGoogleCdn(url)) return url
        var result = url
        result = Regex("=w\\d+-h\\d+").replace(result) { "=w$targetPx-h$targetPx" }
        result = Regex("=s\\d+(?=[-&]|$)").replace(result) { "=s$targetPx" }
        result = Regex("/w\\d+-h\\d+/").replace(result) { "/w$targetPx-h$targetPx/" }
        return result
    }

    private fun isGoogleCdn(url: String): Boolean =
        url.contains("googleusercontent.com") || url.contains("ggpht.com")
}
