package com.bladderdiary.app.domain.model

import kotlinx.coroutines.flow.Flow

data class E2eeState(
    val isCheckingRemoteState: Boolean = false,
    val isEnabled: Boolean = false,
    val isUnlocked: Boolean = false,
    val lastErrorMessage: String? = null
)

data class VoidingEventSyncPayload(
    val recordCiphertext: String?,
    val recordEncryption: String,
    val memoCiphertext: String?,
    val memoEncryption: String
)

data class DecryptedVoidingEventPayload(
    val voidedAtEpochMs: Long,
    val memo: String?,
    val volumeMl: Int?,
    val urgency: Int?,
    val hasIncontinence: Boolean,
    val isNocturia: Boolean
)

interface E2eeRepository {
    fun observeState(): Flow<E2eeState>
    suspend fun refreshRemoteState(): Result<Unit>
    suspend fun setupPassphrase(passphrase: String): Result<Unit>
    suspend fun changePassphrase(passphrase: String): Result<Unit>
    suspend fun unlock(passphrase: String): Result<Unit>
    fun isReadyForCloudRecordSync(): Boolean
    suspend fun prepareVoidingEventSyncPayload(
        userId: String,
        eventId: String,
        localDate: String,
        voidedAtEpochMs: Long,
        memo: String?,
        volumeMl: Int?,
        urgency: Int?,
        hasIncontinence: Boolean,
        isNocturia: Boolean
    ): Result<VoidingEventSyncPayload>
    suspend fun decryptVoidingEventPayload(
        userId: String,
        eventId: String,
        localDate: String,
        recordCiphertext: String?,
        recordEncryption: String,
        voidedAtEpochMs: Long,
        memoCiphertext: String?,
        memoEncryption: String,
        volumeMl: Int?,
        urgency: Int?,
        hasIncontinence: Boolean,
        isNocturia: Boolean
    ): Result<DecryptedVoidingEventPayload?>
    fun clearRuntimeUnlock()
}
