package com.bladderdiary.app.data.remote

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bladderdiary.app.domain.model.AuthAccount
import com.bladderdiary.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "session_store")

class SessionStore(private val context: Context) {
    private val userIdKey = stringPreferencesKey("user_id")
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val emailKey = stringPreferencesKey("email")
    private val providerKey = stringPreferencesKey("provider")
    private val rememberedUserIdKey = stringPreferencesKey("remembered_user_id")
    private val rememberedEmailKey = stringPreferencesKey("remembered_email")
    private val rememberedProviderKey = stringPreferencesKey("remembered_provider")
    private val accountSwitchArmedKey = booleanPreferencesKey("account_switch_armed")
    private val pendingOAuthProviderKey = stringPreferencesKey("pending_oauth_provider")

    val sessionFlow: Flow<UserSession?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs.toSession() }

    val rememberedAccountFlow: Flow<AuthAccount?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs.toRememberedAccount() }

    val accountSwitchArmedFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs[accountSwitchArmedKey] ?: false }

    suspend fun save(session: UserSession) {
        context.dataStore.edit { prefs ->
            val rememberedUserId = prefs[rememberedUserIdKey]
            val preservedEmail = if (rememberedUserId == session.userId) {
                prefs[rememberedEmailKey]
            } else {
                null
            }
            val preservedProvider = if (rememberedUserId == session.userId) {
                prefs[rememberedProviderKey]
            } else {
                null
            }
            val email = session.email?.trim()?.takeIf { it.isNotEmpty() } ?: preservedEmail
            val provider = session.provider
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
                ?: preservedProvider

            prefs[userIdKey] = session.userId
            prefs[accessTokenKey] = session.accessToken
            prefs[refreshTokenKey] = session.refreshToken
            if (email == null) {
                prefs.remove(emailKey)
            } else {
                prefs[emailKey] = email
            }
            if (provider == null) {
                prefs.remove(providerKey)
            } else {
                prefs[providerKey] = provider
            }

            prefs[rememberedUserIdKey] = session.userId
            if (email == null) {
                prefs.remove(rememberedEmailKey)
            } else {
                prefs[rememberedEmailKey] = email
            }
            if (provider == null) {
                prefs.remove(rememberedProviderKey)
            } else {
                prefs[rememberedProviderKey] = provider
            }

            prefs.remove(accountSwitchArmedKey)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(userIdKey)
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
            prefs.remove(emailKey)
            prefs.remove(providerKey)
            prefs.remove(pendingOAuthProviderKey)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun getRememberedAccount(): AuthAccount? = rememberedAccountFlow.first()

    suspend fun isAccountSwitchArmed(): Boolean = accountSwitchArmedFlow.first()

    suspend fun armAccountSwitch() {
        context.dataStore.edit { prefs ->
            prefs[accountSwitchArmedKey] = true
        }
    }

    suspend fun clearPendingAccountSwitch() {
        context.dataStore.edit { prefs ->
            prefs.remove(accountSwitchArmedKey)
        }
    }

    suspend fun savePendingOAuthProvider(provider: String) {
        context.dataStore.edit { prefs ->
            prefs[pendingOAuthProviderKey] = provider
        }
    }

    suspend fun getPendingOAuthProvider(): String? =
        context.dataStore.data.first()[pendingOAuthProviderKey]

    suspend fun clearPendingOAuthProvider() {
        context.dataStore.edit { prefs ->
            prefs.remove(pendingOAuthProviderKey)
        }
    }

    private fun Preferences.toSession(): UserSession? {
        val userId = this[userIdKey] ?: return null
        val accessToken = this[accessTokenKey] ?: return null
        val refreshToken = this[refreshTokenKey] ?: return null
        return UserSession(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = this[emailKey],
            provider = this[providerKey]
        )
    }

    private fun Preferences.toRememberedAccount(): AuthAccount? {
        val userId = this[rememberedUserIdKey] ?: return null
        return AuthAccount(
            userId = userId,
            email = this[rememberedEmailKey],
            provider = this[rememberedProviderKey]
        )
    }
}
