package com.bladderdiary.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.cloudDataNoticeDataStore by preferencesDataStore(
    name = "cloud_data_notice_store"
)

class CloudDataNoticeStore(private val context: Context) {
    val isAcknowledgedFlow: Flow<Boolean?> = context.cloudDataNoticeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            prefs[acknowledgedVersionKey] == CURRENT_NOTICE_VERSION
        }

    suspend fun acknowledgeCurrentNotice() {
        context.cloudDataNoticeDataStore.edit { prefs ->
            prefs[acknowledgedVersionKey] = CURRENT_NOTICE_VERSION
        }
    }

    private companion object {
        private const val CURRENT_NOTICE_VERSION = 1
        private val acknowledgedVersionKey = intPreferencesKey("acknowledged_version")
    }
}
