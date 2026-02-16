package com.bladderdiary.app.domain.model

import kotlinx.coroutines.flow.Flow

data class LockState(
    val isPinSet: Boolean = false,
    val isUnlocked: Boolean = false,
    val failedAttempts: Int = 0,
    val lockedUntilEpochMs: Long? = null
)

interface LockRepository {
    fun observeLockState(): Flow<LockState>
    suspend fun setPin(pin: String): Result<Unit>
    suspend fun verifyPin(pin: String): Result<Boolean>
    suspend fun resetForForgotPin(): Result<Unit>
    fun clearRuntimeUnlock()
}
