package com.bladderdiary.app.data.repository

import com.bladderdiary.app.data.remote.SessionStore
import com.bladderdiary.app.data.remote.SupabaseApi
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.AuthResult
import com.bladderdiary.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepositoryImpl(
    private val api: SupabaseApi,
    private val sessionStore: SessionStore
) : AuthRepository {
    override val sessionFlow: Flow<UserSession?> = sessionStore.sessionFlow

    override suspend fun signUp(email: String, password: String): Result<AuthResult> {
        return runCatching {
            api.signUp(email, password)
            val signInResponse = api.signIn(email, password)
            val session = signInResponse.toSession()
            sessionStore.save(session)
            AuthResult(userId = session.userId)
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

    override suspend fun signOut() {
        sessionStore.clear()
    }

    override suspend fun getSession(): UserSession? = sessionFlow.first()
}

private fun com.bladderdiary.app.data.remote.dto.AuthResponseDto.toSession(): UserSession {
    val userId = user?.id ?: throw IllegalStateException("사용자 정보가 없습니다.")
    val accessToken = accessToken ?: throw IllegalStateException("액세스 토큰이 없습니다.")
    val refreshToken = refreshToken ?: ""
    return UserSession(userId, accessToken, refreshToken)
}
