package com.bladderdiary.app.domain.model

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionFlow: Flow<UserSession?>
    val rememberedAccountFlow: Flow<AuthAccount?>
    val accountSwitchArmedFlow: Flow<Boolean>

    suspend fun signUp(email: String, password: String): Result<AuthResult>
    suspend fun signIn(email: String, password: String): Result<AuthResult>
    suspend fun signInWithSocial(provider: SocialProvider): Result<Unit>
    suspend fun handleOAuthCallback(callbackUrl: String): Result<AuthResult>
    suspend fun armAccountSwitch()
    suspend fun clearPendingAccountSwitch()
    suspend fun signOut()
    suspend fun getSession(): UserSession?
    suspend fun refreshSession(): Result<UserSession>
}
