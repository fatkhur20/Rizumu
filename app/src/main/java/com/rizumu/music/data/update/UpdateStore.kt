package com.rizumu.music.data.update

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.updateDataStore by preferencesDataStore(name = "rizumu_updates")

class UpdateStore(private val context: Context) {

    suspend fun lastCheckAt(): Long =
        context.updateDataStore.data.first()[KEY_LAST_CHECK_AT] ?: 0L

    suspend fun setLastCheckAt(timestamp: Long) {
        context.updateDataStore.edit { prefs ->
            prefs[KEY_LAST_CHECK_AT] = timestamp
        }
    }

    suspend fun skippedVersion(): String? =
        context.updateDataStore.data.first()[KEY_SKIPPED_VERSION]

    suspend fun skipVersion(version: String) {
        context.updateDataStore.edit { prefs ->
            prefs[KEY_SKIPPED_VERSION] = version
        }
    }

    private companion object {
        val KEY_LAST_CHECK_AT = longPreferencesKey("last_check_at")
        val KEY_SKIPPED_VERSION = stringPreferencesKey("skipped_version")
    }
}
