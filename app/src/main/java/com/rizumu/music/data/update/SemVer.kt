package com.rizumu.music.data.update

/** Minimal semantic version used for GitHub release tags and BuildConfig.versionName. */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val PATTERN = Regex("""^[vV]?(\d+)\.(\d+)\.(\d+)(?:[-+].*)?$""")

        fun parse(raw: String): SemVer? {
            val match = PATTERN.matchEntire(raw.trim()) ?: return null
            return SemVer(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: return null,
            )
        }
    }
}
