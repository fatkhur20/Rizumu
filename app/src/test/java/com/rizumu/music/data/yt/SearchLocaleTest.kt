package com.rizumu.music.data.yt

import com.rizumu.music.data.charts.ChartRegion
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchLocaleTest {

    @Test
    fun `region maps to its language`() {
        assertEquals(SearchLocale("id", "ID"), SearchLocale.fromRegion(ChartRegion.INDONESIA))
        assertEquals(SearchLocale("ko", "KR"), SearchLocale.fromRegion(ChartRegion("KR", "Korea")))
        assertEquals(SearchLocale("ja", "JP"), SearchLocale.fromRegion(ChartRegion("JP", "Jepang")))
        assertEquals(SearchLocale("en", "US"), SearchLocale.fromRegion(ChartRegion("US", "Amerika")))
    }

    @Test
    fun `unknown region falls back to global`() {
        assertEquals(SearchLocale.global(), SearchLocale.fromRegion(ChartRegion("ZZ", "Global")))
        assertEquals(SearchLocale.global(), SearchLocale.fromRegion(ChartRegion("", "?")))
    }

    @Test
    fun `music search uses injected locale`() {
        val search = MusicSearch(localeProvider = { SearchLocale("ko", "KR") })
        val client = search.contextBody(SearchLocale("ko", "KR"))
            .getJSONObject("client")

        assertEquals("ko", client.getString("hl"))
        assertEquals("KR", client.getString("gl"))
    }
}
