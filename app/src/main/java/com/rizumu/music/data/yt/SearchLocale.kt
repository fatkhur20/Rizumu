package com.rizumu.music.data.yt

import com.rizumu.music.data.charts.ChartRegion

data class SearchLocale(
    val hl: String,
    val gl: String,
) {
    companion object {
        fun global(): SearchLocale = SearchLocale("en", "US")

        fun fromRegion(region: ChartRegion): SearchLocale = when (region.code) {
            "ID" -> SearchLocale("id", "ID")
            "MY" -> SearchLocale("ms", "MY")
            "SG" -> SearchLocale("en", "SG")
            "US" -> SearchLocale("en", "US")
            "GB" -> SearchLocale("en", "GB")
            "JP" -> SearchLocale("ja", "JP")
            "KR" -> SearchLocale("ko", "KR")
            "IN" -> SearchLocale("hi", "IN")
            "AU" -> SearchLocale("en", "AU")
            "DE" -> SearchLocale("de", "DE")
            "FR" -> SearchLocale("fr", "FR")
            "BR" -> SearchLocale("pt", "BR")
            "MX" -> SearchLocale("es", "MX")
            "TH" -> SearchLocale("th", "TH")
            "VN" -> SearchLocale("vi", "VN")
            "PH" -> SearchLocale("en", "PH")
            else -> global()
        }
    }
}
