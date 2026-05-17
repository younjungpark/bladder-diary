package com.bladderdiary.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.bladderdiary.app.data.backup.BackupDekStore
import com.bladderdiary.app.data.backup.BackupPreferenceStore
import com.bladderdiary.app.data.local.AppDatabase
import com.bladderdiary.app.data.local.CloudSyncPreferenceStore
import com.bladderdiary.app.data.remote.PinStoreDataSource
import com.bladderdiary.app.data.remote.SessionStore
import com.bladderdiary.app.data.remote.SupabaseApi
import com.bladderdiary.app.data.remote.SupabaseAuthClient
import com.bladderdiary.app.data.remote.dto.AccountDeletionRequestDto
import com.bladderdiary.app.data.remote.resolveProvider
import com.bladderdiary.app.data.security.E2eeLocalKeyStoreDataSource
import com.bladderdiary.app.domain.model.AuthAccount
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.AuthResult
import com.bladderdiary.app.domain.model.SocialProvider
import com.bladderdiary.app.domain.model.UserSession
import com.bladderdiary.app.domain.model.toAuthAccount
import com.bladderdiary.app.worker.BackupWorkScheduler
import com.bladderdiary.app.worker.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val appContext: Context,
    private val api: SupabaseApi,
    private val authClient: SupabaseAuthClient,
    private val sessionStore: SessionStore,
    private val db: AppDatabase,
    private val pinStore: PinStoreDataSource,
    private val localKeyStore: E2eeLocalKeyStoreDataSource,
    private val syncScheduler: SyncScheduler,
    private val cloudSyncPreferenceStore: CloudSyncPreferenceStore,
    private val backupScheduler: BackupWorkScheduler,
    private val backupDekStore: BackupDekStore,
    private val backupPreferenceStore: BackupPreferenceStore
) : AuthRepository {
    override val sessionFlow: Flow<UserSession?> = sessionStore.sessionFlow
    override val rememberedAccountFlow: Flow<AuthAccount?> = sessionStore.rememberedAccountFlow
    override val accountSwitchArmedFlow: Flow<Boolean> = sessionStore.accountSwitchArmedFlow

    override suspend fun signUp(email: String, password: String): Result<AuthResult> = runCatching {
        val response = api.signUp(email, password)
        AuthResult(userId = response.user?.id ?: "pending_email_confirmation")
    }

    override suspend fun signIn(email: String, password: String): Result<AuthResult> = runCatching {
        val response = api.signIn(email, password)
        val session = response.toSession(
            fallbackEmail = email.trim(),
            fallbackProvider = "email"
        )
        val persistedSession = persistApprovedSession(session)
        AuthResult(userId = persistedSession.userId)
    }

    override suspend fun signInWithSocial(provider: SocialProvider): Result<Unit> = runCatching {
        sessionStore.savePendingOAuthProvider(provider.providerKey)
        val authUri = Uri.parse(authClient.buildOAuthSignInUrl(provider))
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        if (isChromeAvailable()) {
            customTabsIntent.intent.setPackage(CHROME_PACKAGE)
        }
        customTabsIntent.launchUrl(appContext, authUri)
    }.onFailure {
        sessionStore.clearPendingOAuthProvider()
    }

    override suspend fun handleOAuthCallback(callbackUrl: String): Result<AuthResult> =
        runCatching {
            val pendingProvider = sessionStore.getPendingOAuthProvider()
            val session = authClient.createSessionFromCallback(
                callbackUrl = callbackUrl,
                fallbackProvider = pendingProvider
            )
            val persistedSession = persistApprovedSession(session)
            AuthResult(userId = persistedSession.userId)
        }.also {
            sessionStore.clearPendingOAuthProvider()
        }

    override suspend fun armAccountSwitch() {
        sessionStore.armAccountSwitch()
    }

    override suspend fun clearPendingAccountSwitch() {
        sessionStore.clearPendingAccountSwitch()
    }

    override suspend fun signOut() {
        sessionStore.clearPendingAccountSwitch()
        sessionStore.clear()
    }

    override suspend fun deleteAccountData(): Result<Unit> {
        val session = getSession() ?: return Result.failure(
            IllegalStateException("로그인이 필요합니다.")
        )

        return runCatching {
            withFreshAccessToken(session) { token ->
                api.createAccountDeletionRequest(
                    accessToken = token,
                    request = session.toAccountDeletionRequest()
                )
                api.deleteAccountData(token, session.userId)
            }
            syncScheduler.cancel()
            backupScheduler.cancel()
            withContext(Dispatchers.IO) {
                db.clearAllTables()
                pinStore.clearUser(session.userId)
                localKeyStore.clearDek(session.userId)
                cloudSyncPreferenceStore.clearUser(session.userId)
                backupDekStore.clear(session.userId)
                backupPreferenceStore.clearUser(session.userId)
                sessionStore.clearAll()
            }
        }
    }

    override suspend fun getSession(): UserSession? = sessionFlow.first()

    override suspend fun refreshSession(): Result<UserSession> {
        val current = getSession() ?: return Result.failure(IllegalStateException("로그인이 필요합니다."))
        if (current.refreshToken.isBlank()) {
            return Result.failure(IllegalStateException("리프레시 토큰이 없습니다. 다시 로그인해주세요."))
        }
        return runCatching {
            val response = api.refreshSession(current.refreshToken)
            val refreshed = response.toSession(
                fallbackUserId = current.userId,
                fallbackRefreshToken = current.refreshToken,
                fallbackEmail = current.email,
                fallbackProvider = current.provider
            )
            persistApprovedSession(refreshed)
        }
    }

    private suspend fun persistApprovedSession(session: UserSession): UserSession {
        val rememberedAccount = sessionStore.getRememberedAccount()
        val enrichedSession = session.enrichFromRememberedAccount(rememberedAccount)
        AccountSwitchGuard.ensureAllowed(
            rememberedAccount = rememberedAccount,
            candidateSession = enrichedSession,
            isAccountSwitchArmed = sessionStore.isAccountSwitchArmed()
        )
        sessionStore.save(enrichedSession)
        return enrichedSession
    }

    private suspend fun <T> withFreshAccessToken(
        session: UserSession,
        block: suspend (String) -> T
    ): T = try {
        block(session.accessToken)
    } catch (error: Throwable) {
        if (!error.isJwtExpiredForAuth()) {
            throw error
        }
        val refreshed = refreshSession().getOrThrow()
        block(refreshed.accessToken)
    }

    private fun isChromeAvailable(): Boolean {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com")
        )
        intent.`package` = CHROME_PACKAGE
        return intent.resolveActivity(appContext.packageManager) != null
    }

    companion object {
        private const val CHROME_PACKAGE = "com.android.chrome"
    }
}

private fun Throwable.isJwtExpiredForAuth(): Boolean {
    val text = message?.lowercase() ?: return false
    return text.contains("jwt expired") || text.contains("pgrst303")
}

private fun UserSession.toAccountDeletionRequest(): AccountDeletionRequestDto {
    val account = toAuthAccount()
    return AccountDeletionRequestDto(
        userId = userId,
        email = email?.trim()?.takeIf { it.isNotEmpty() },
        provider = provider?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
        accountSummary = account.summary
    )
}

private fun com.bladderdiary.app.data.remote.dto.AuthResponseDto.toSession(
    fallbackUserId: String? = null,
    fallbackRefreshToken: String? = null,
    fallbackEmail: String? = null,
    fallbackProvider: String? = null
): UserSession {
    val userId = user?.id ?: fallbackUserId ?: throw IllegalStateException("사용자 정보가 없습니다.")
    val accessToken = accessToken ?: throw IllegalStateException("액세스 토큰이 없습니다.")
    val refreshToken = refreshToken ?: fallbackRefreshToken ?: ""
    return UserSession(
        userId = userId,
        accessToken = accessToken,
        refreshToken = refreshToken,
        email = user?.email ?: fallbackEmail,
        provider = resolveProvider(
            preferredProvider = user?.appMetadata?.provider,
            fallbackProvider = fallbackProvider
        )
    )
}

private fun UserSession.enrichFromRememberedAccount(rememberedAccount: AuthAccount?): UserSession {
    val normalizedEmail = email?.trim()?.takeIf { it.isNotEmpty() }
    val normalizedProvider = provider?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    if (rememberedAccount == null || rememberedAccount.userId != userId) {
        return copy(
            email = normalizedEmail,
            provider = normalizedProvider
        )
    }
    return copy(
        email = normalizedEmail ?: rememberedAccount.email,
        provider = normalizedProvider ?: rememberedAccount.provider
    )
}
