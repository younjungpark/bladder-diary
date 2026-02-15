package com.bladderdiary.app.data.remote

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bladderdiary.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "session_store")

class SessionStore(
    private val context: Context
) {
    private val userIdKey = stringPreferencesKey("user_id")
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    val sessionFlow: Flow<UserSession?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs.toSession() }

    suspend fun save(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[userIdKey] = session.userId
            prefs[accessTokenKey] = session.accessToken
            prefs[refreshTokenKey] = session.refreshToken
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(userIdKey)
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
        }
    }

    private fun Preferences.toSession(): UserSession? {
        val userId = this[userIdKey] ?: return null
        val accessToken = this[accessTokenKey] ?: return null
        val refreshToken = this[refreshTokenKey] ?: return null
        return UserSession(userId, accessToken, refreshToken)
    }
}
