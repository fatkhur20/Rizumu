package com.rizumu.music.data.charts

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RegionStore {
    private const val PREFS = "ftx_region"
    private const val KEY_CODE = "code"

    private val _region = MutableStateFlow<ChartRegion>(ChartRegion.INDONESIA)
    val region: StateFlow<ChartRegion> = _region.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs?.getString(KEY_CODE, null).orEmpty()
        _region.value = ChartRegion.byCode(saved)
    }

    fun select(region: ChartRegion) {
        if (region !in ChartRegion.SELECTABLE) return
        prefs?.edit()?.putString(KEY_CODE, region.code)?.apply()
        _region.value = region
    }
}
