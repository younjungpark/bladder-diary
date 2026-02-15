package com.bladderdiary.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.bladderdiary.app.data.remote.SupabaseAuthClient
import com.bladderdiary.app.data.remote.SessionStore
import com.bladderdiary.app.data.remote.SupabaseApi
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.AuthResult
import com.bladderdiary.app.domain.model.SocialProvider
import com.bladderdiary.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepositoryImpl(
    private val appContext: Context,
    private val api: SupabaseApi,
    private val authClient: SupabaseAuthClient,
    private val sessionStore: SessionStore
) : AuthRepository {
    override val sessionFlow: Flow<UserSession?> = sessionStore.sessionFlow

    override suspend fun signUp(email: String, password: String): Result<AuthResult> {
        return runCatching {
            val response = api.signUp(email, password)
            AuthResult(userId = response.user?.id ?: "pending_email_confirmation")
        }
    }

    override suspend fun signIn(email: String, password: String): Result<AuthResult> {
        return runCatching {
            val response = api.signIn(email, password)
            val session = response.toSession()
            sessionStore.save(session)
            AuthResult(userId = session.userId)
        }
    }

    override suspend fun signInWithSocial(provider: SocialProvider): Result<Unit> {
        return runCatching {
            val authUri = Uri.parse(authClient.buildOAuthSignInUrl(provider))
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isChromeAvailable()) {
                customTabsIntent.intent.setPackage(CHROME_PACKAGE)
            }
            customTabsIntent.launchUrl(appContext, authUri)
        }
    }

    override suspend fun handleOAuthCallback(callbackUrl: String): Result<AuthResult> {
        return runCatching {
            val session = authClient.createSessionFromCallback(callbackUrl)
            sessionStore.save(session)
            AuthResult(userId = session.userId)
        }
    }

    override suspend fun signOut() {
        sessionStore.clear()
    }

    override suspend fun getSession(): UserSession? = sessionFlow.first()

    private fun isChromeAvailable(): Boolean {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
        intent.`package` = CHROME_PACKAGE
        return intent.resolveActivity(appContext.packageManager) != null
    }

    companion object {
        private const val CHROME_PACKAGE = "com.android.chrome"
    }
}

private fun com.bladderdiary.app.data.remote.dto.AuthResponseDto.toSession(): UserSession {
    val userId = user?.id ?: throw IllegalStateException("사용자 정보가 없습니다.")
    val accessToken = accessToken ?: throw IllegalStateException("액세스 토큰이 없습니다.")
    val refreshToken = refreshToken ?: ""
    return UserSession(userId, accessToken, refreshToken)
}
