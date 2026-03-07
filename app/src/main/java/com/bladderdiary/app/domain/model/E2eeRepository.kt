package com.bladderdiary.app.domain.model

import kotlinx.coroutines.flow.Flow

data class E2eeState(
    val isCheckingRemoteState: Boolean = false,
    val isEnabled: Boolean = false,
    val isUnlocked: Boolean = false,
    val lastErrorMessage: String? = null
)

data class MemoSyncPayload(
    val memoCiphertext: String?,
    val memoEncryption: String
)

interface E2eeRepository {
    fun observeState(): Flow<E2eeState>
    suspend fun refreshRemoteState(): Result<Unit>
    suspend fun setupPassphrase(passphrase: String): Result<Unit>
    suspend fun changePassphrase(passphrase: String): Result<Unit>
    suspend fun unlock(passphrase: String): Result<Unit>
    suspend fun prepareMemoSyncPayload(
        userId: String,
        eventId: String,
        localDate: String,
        memo: String?
    ): Result<MemoSyncPayload>
    suspend fun decryptMemo(
        userId: String,
        eventId: String,
        localDate: String,
        memoCiphertext: String?,
        memoEncryption: String
    ): Result<String?>
    fun clearRuntimeUnlock()
}
