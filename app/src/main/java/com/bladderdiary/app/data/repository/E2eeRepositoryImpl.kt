package com.bladderdiary.app.data.repository

import com.bladderdiary.app.data.remote.SupabaseApi
import com.bladderdiary.app.data.remote.dto.UserE2eeKeyRemoteDto
import com.bladderdiary.app.data.security.E2eeLocalKeyStoreDataSource
import com.bladderdiary.app.data.security.MemoCrypto
import com.bladderdiary.app.data.security.MemoEncryptionScheme
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.E2eeRepository
import com.bladderdiary.app.domain.model.E2eeState
import com.bladderdiary.app.domain.model.MemoSyncPayload
import java.security.GeneralSecurityException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class E2eeRepositoryImpl(
    private val authRepository: AuthRepository,
    private val api: SupabaseApi,
    private val localKeyStore: E2eeLocalKeyStoreDataSource
) : E2eeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val state = MutableStateFlow(E2eeState())

    private var cachedRemoteKey: UserE2eeKeyRemoteDto? = null
    private var unlockedUserId: String? = null
    private var runtimeDek: ByteArray? = null

    init {
        authRepository.sessionFlow
            .onEach { session ->
                if (session == null) {
                    cachedRemoteKey = null
                    unlockedUserId = null
                    runtimeDek = null
                    state.value = E2eeState()
                } else {
                    state.update { it.copy(isCheckingRemoteState = true, lastErrorMessage = null) }
                    refreshRemoteState()
                }
            }
            .launchIn(scope)
    }

    override fun observeState(): Flow<E2eeState> = state.asStateFlow()

    override suspend fun refreshRemoteState(): Result<Unit> {
        val session = authRepository.getSession() ?: run {
            cachedRemoteKey = null
            unlockedUserId = null
            runtimeDek = null
            state.value = E2eeState()
            return Result.success(Unit)
        }

        return runCatching {
            val remoteKey = withFreshAccessToken { token ->
                api.getUserE2eeKey(token, session.userId)
            }
            cachedRemoteKey = remoteKey
            if (remoteKey == null) {
                localKeyStore.clearDek(session.userId)
                unlockedUserId = null
                runtimeDek = null
            } else if (unlockedUserId != session.userId || runtimeDek == null) {
                restoreLocalDek(session.userId)
            }
            state.value = E2eeState(
                isCheckingRemoteState = false,
                isEnabled = remoteKey != null,
                isUnlocked = remoteKey != null && unlockedUserId == session.userId && runtimeDek != null,
                lastErrorMessage = null
            )
        }.onFailure { error ->
            state.value = E2eeState(
                isCheckingRemoteState = false,
                isEnabled = cachedRemoteKey != null,
                isUnlocked = false,
                lastErrorMessage = error.message ?: "E2EE 상태를 확인하지 못했습니다."
            )
        }
    }

    override suspend fun setupPassphrase(passphrase: String): Result<Unit> {
        return runCatching {
            require(passphrase.length >= MIN_PASSPHRASE_LENGTH) {
                "비밀문구는 최소 ${MIN_PASSPHRASE_LENGTH}자 이상이어야 합니다."
            }
            val session = authRepository.getSession() ?: throw IllegalStateException("로그인이 필요합니다.")
            val derived = MemoCrypto.deriveKek(passphrase.toCharArray())
            val dek = MemoCrypto.generateDek()
            val remoteKey = UserE2eeKeyRemoteDto(
                userId = session.userId,
                kdf = KDF_NAME,
                kdfSalt = derived.saltBase64,
                kdfParams = jsonParams(
                    iterations = derived.iterations,
                    keyLengthBits = derived.keyLengthBits
                ),
                wrappedDek = MemoCrypto.wrapDek(dek, derived.keyBytes),
                keyVersion = 1
            )
            withFreshAccessToken { token ->
                api.upsertUserE2eeKey(token, remoteKey)
            }
            localKeyStore.saveDek(session.userId, dek)
            cachedRemoteKey = remoteKey
            unlockedUserId = session.userId
            runtimeDek = dek
            state.value = E2eeState(
                isCheckingRemoteState = false,
                isEnabled = true,
                isUnlocked = true,
                lastErrorMessage = null
            )
        }
    }

    override suspend fun changePassphrase(passphrase: String): Result<Unit> {
        return runCatching {
            require(passphrase.length >= MIN_PASSPHRASE_LENGTH) {
                "비밀문구는 최소 ${MIN_PASSPHRASE_LENGTH}자 이상이어야 합니다."
            }
            val session = authRepository.getSession() ?: throw IllegalStateException("로그인이 필요합니다.")
            val remoteKey = cachedRemoteKey ?: withFreshAccessToken { token ->
                api.getUserE2eeKey(token, session.userId)
            } ?: throw IllegalStateException("설정된 E2EE 키를 찾지 못했습니다.")
            val dek = ensureRuntimeDek(session.userId)
                ?: throw IllegalStateException("현재 기기에서는 비밀문구를 재설정할 수 없습니다. 기존 비밀문구로 먼저 잠금 해제해주세요.")

            val derived = MemoCrypto.deriveKek(passphrase.toCharArray())
            val updatedRemoteKey = remoteKey.copy(
                kdf = KDF_NAME,
                kdfSalt = derived.saltBase64,
                kdfParams = jsonParams(
                    iterations = derived.iterations,
                    keyLengthBits = derived.keyLengthBits
                ),
                wrappedDek = MemoCrypto.wrapDek(dek, derived.keyBytes)
            )
            withFreshAccessToken { token ->
                api.upsertUserE2eeKey(token, updatedRemoteKey)
            }
            localKeyStore.saveDek(session.userId, dek)
            cachedRemoteKey = updatedRemoteKey
            unlockedUserId = session.userId
            runtimeDek = dek
            state.value = E2eeState(
                isCheckingRemoteState = false,
                isEnabled = true,
                isUnlocked = true,
                lastErrorMessage = null
            )
        }
    }

    override suspend fun unlock(passphrase: String): Result<Unit> {
        return runCatching {
            require(passphrase.isNotBlank()) { "비밀문구를 입력해주세요." }
            val session = authRepository.getSession() ?: throw IllegalStateException("로그인이 필요합니다.")
            val remoteKey = cachedRemoteKey ?: withFreshAccessToken { token ->
                api.getUserE2eeKey(token, session.userId)
            } ?: throw IllegalStateException("설정된 E2EE 키를 찾지 못했습니다.")

            val iterations = remoteKey.kdfParams["iterations"]?.jsonPrimitive?.intOrNull
                ?: MemoCrypto.DEFAULT_PBKDF2_ITERATIONS
            val keyLengthBits = remoteKey.kdfParams["keyLengthBits"]?.jsonPrimitive?.intOrNull
                ?: MemoCrypto.DEFAULT_PBKDF2_KEY_LENGTH_BITS
            val derived = MemoCrypto.deriveKek(
                passphrase = passphrase.toCharArray(),
                saltBase64 = remoteKey.kdfSalt,
                iterations = iterations,
                keyLengthBits = keyLengthBits
            )
            val dek = try {
                MemoCrypto.unwrapDek(remoteKey.wrappedDek, derived.keyBytes)
            } catch (error: GeneralSecurityException) {
                throw IllegalArgumentException("비밀문구가 올바르지 않습니다.", error)
            }

            localKeyStore.saveDek(session.userId, dek)
            cachedRemoteKey = remoteKey
            unlockedUserId = session.userId
            runtimeDek = dek
            state.value = E2eeState(
                isCheckingRemoteState = false,
                isEnabled = true,
                isUnlocked = true,
                lastErrorMessage = null
            )
        }.onFailure { error ->
            val enabled = cachedRemoteKey != null
            state.value = E2eeState(
                isCheckingRemoteState = false,
                isEnabled = enabled,
                isUnlocked = false,
                lastErrorMessage = error.message ?: "비밀문구 확인에 실패했습니다."
            )
        }
    }

    override suspend fun prepareMemoSyncPayload(
        userId: String,
        eventId: String,
        localDate: String,
        memo: String?
    ): Result<MemoSyncPayload> {
        return runCatching {
            val isEnabled = state.value.isEnabled
            if (!isEnabled) {
                MemoSyncPayload(
                    memoCiphertext = memo,
                    memoEncryption = MemoEncryptionScheme.NONE
                )
            } else {
                val dek = ensureRuntimeDek(userId)
                if (memo.isNullOrBlank()) {
                    MemoSyncPayload(
                        memoCiphertext = null,
                        memoEncryption = MemoEncryptionScheme.E2EE_V1
                    )
                } else {
                    requireNotNull(dek) { "E2EE 비밀문구 잠금 해제 후 메모를 저장해주세요." }
                    MemoSyncPayload(
                        memoCiphertext = MemoCrypto.encryptMemo(
                            memo = memo,
                            dekBytes = dek,
                            userId = userId,
                            eventId = eventId,
                            localDate = localDate
                        ),
                        memoEncryption = MemoEncryptionScheme.E2EE_V1
                    )
                }
            }
        }
    }

    override suspend fun decryptMemo(
        userId: String,
        eventId: String,
        localDate: String,
        memoCiphertext: String?,
        memoEncryption: String
    ): Result<String?> {
        return runCatching {
            when (memoEncryption) {
                MemoEncryptionScheme.NONE -> memoCiphertext
                MemoEncryptionScheme.E2EE_V1 -> {
                    if (memoCiphertext.isNullOrBlank()) {
                        null
                    } else {
                        val dek = ensureRuntimeDek(userId) ?: return@runCatching null
                        MemoCrypto.decryptMemo(
                            payload = memoCiphertext,
                            dekBytes = dek,
                            userId = userId,
                            eventId = eventId,
                            localDate = localDate
                        )
                    }
                }
                else -> null
            }
        }
    }

    override fun clearRuntimeUnlock() {
        unlockedUserId = null
        runtimeDek = null
        state.update {
            it.copy(
                isUnlocked = false,
                lastErrorMessage = null
            )
        }
    }

    private suspend fun ensureRuntimeDek(userId: String): ByteArray? {
        val currentDek = runtimeDek
        if (unlockedUserId == userId && currentDek != null) {
            return currentDek
        }
        return restoreLocalDek(userId)
    }

    private suspend fun restoreLocalDek(userId: String): ByteArray? {
        val restoredDek = localKeyStore.loadDek(userId)
        if (restoredDek == null) {
            unlockedUserId = null
            runtimeDek = null
            state.update {
                it.copy(
                    isUnlocked = false,
                    lastErrorMessage = null
                )
            }
            return null
        }

        unlockedUserId = userId
        runtimeDek = restoredDek
        state.update {
            it.copy(
                isUnlocked = true,
                lastErrorMessage = null
            )
        }
        return restoredDek
    }

    private suspend fun <T> withFreshAccessToken(block: suspend (String) -> T): T {
        val current = authRepository.getSession() ?: throw IllegalStateException("로그인이 필요합니다.")
        return try {
            block(current.accessToken)
        } catch (error: Throwable) {
            if (!error.isJwtExpiredForE2ee()) {
                throw error
            }
            val refreshed = authRepository.refreshSession().getOrThrow()
            block(refreshed.accessToken)
        }
    }

    private fun jsonParams(iterations: Int, keyLengthBits: Int): JsonObject {
        return JsonObject(
            mapOf(
                "iterations" to JsonPrimitive(iterations),
                "keyLengthBits" to JsonPrimitive(keyLengthBits)
            )
        )
    }

    companion object {
        private const val KDF_NAME = "PBKDF2_SHA256"
        const val MIN_PASSPHRASE_LENGTH = 8
    }
}

private fun Throwable.isJwtExpiredForE2ee(): Boolean {
    val text = message?.lowercase() ?: return false
    return text.contains("jwt expired") || text.contains("pgrst303")
}
