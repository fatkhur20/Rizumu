package com.rizumu.music.data.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GithubReleaseParserTest {

    private val hash = "a".repeat(64)

    @Test
    fun parsesReleaseAndSelectsApkByExtension() {
        val json = """
            {
              "tag_name": "v0.10.0",
              "body": "Perbaikan pemutar.\n\n**SHA-256:** $hash",
              "assets": [
                {
                  "name": "Rizumu-0.10.0.apk.sha256",
                  "browser_download_url": "https://example.com/hash",
                  "size": 12
                },
                {
                  "name": "Rizumu-0.10.0.APK",
                  "browser_download_url": "https://example.com/Rizumu-0.10.0.apk",
                  "size": 12345678
                }
              ]
            }
        """.trimIndent()

        val update = GithubReleaseParser.parse(json)

        assertEquals("0.10.0", update?.version)
        assertEquals("v0.10.0", update?.tagName)
        assertEquals("https://example.com/Rizumu-0.10.0.apk", update?.apkUrl)
        assertEquals(12345678L, update?.apkSize)
        assertEquals(hash, update?.sha256)
        assertTrue(update?.notes?.contains("Perbaikan pemutar") == true)
    }

    @Test
    fun ignoresReleaseWithoutApkAsset() {
        val json = """
            {
              "tag_name": "v1.0.0",
              "body": "",
              "assets": [
                {
                  "name": "notes.txt",
                  "browser_download_url": "https://example.com/notes.txt",
                  "size": 5
                }
              ]
            }
        """.trimIndent()

        assertNull(GithubReleaseParser.parse(json))
    }

    @Test
    fun extractsHashFromCommonReleaseNoteFormats() {
        assertEquals(hash, GithubReleaseParser.sha256FromNotes("SHA256: $hash"))
        assertEquals(hash, GithubReleaseParser.sha256FromNotes("sha-256 = ${hash.uppercase()}"))
        assertEquals(hash, GithubReleaseParser.sha256FromNotes("Hash `$hash`"))
        assertNull(GithubReleaseParser.sha256FromNotes("Tidak ada hash"))
    }

    @Test
    fun comparesSemanticVersions() {
        assertTrue(GithubReleaseParser.isNewer("v0.10.0", "0.9.0"))
        assertFalse(GithubReleaseParser.isNewer("0.9.0", "v0.9.0"))
        assertFalse(GithubReleaseParser.isNewer("v0.9.0-beta", "0.9.0"))
        assertTrue(GithubReleaseParser.isNewer("1.0.0", "0.99.99"))
        assertFalse(GithubReleaseParser.isNewer("invalid", "0.9.0"))
    }

    @Test
    fun parsesTagWithSuffixAndRejectsInvalidVersion() {
        assertEquals(SemVer(2, 4, 1), SemVer.parse("v2.4.1-rc.1"))
        assertEquals(SemVer(2, 4, 1), SemVer.parse("2.4.1+42"))
        assertNull(SemVer.parse("v2.4"))
        assertNull(SemVer.parse("rilis-baru"))
    }
}
