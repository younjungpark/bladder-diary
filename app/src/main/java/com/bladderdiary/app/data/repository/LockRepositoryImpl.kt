package com.bladderdiary.app.data.repository

import com.bladderdiary.app.data.remote.PinStoreDataSource
import com.bladderdiary.app.data.security.PinCrypto
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.LockRepository
import com.bladderdiary.app.domain.model.LockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.security.MessageDigest

class LockRepositoryImpl(
    private val authRepository: AuthRepository,
    private val pinStore: PinStoreDataSource
) : LockRepository {
    private val unlockedUserId = MutableStateFlow<String?>(null)

    override fun observeLockState(): Flow<LockState> {
        return authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(LockState())
            } else {
                combine(
                    pinStore.observe(session.userId),
                    unlockedUserId
                ) { persisted, unlockedId ->
                    val now = System.currentTimeMillis()
                    val isPinSet = !persisted.pinHash.isNullOrEmpty() && !persisted.pinSalt.isNullOrEmpty()
                    val stillLockedUntil = persisted.lockedUntilEpochMs?.takeIf { it > now }
                    val failedAttempts = if (persisted.lockedUntilEpochMs != null && stillLockedUntil == null) {
                        0
                    } else {
                        persisted.failedAttempts
                    }

                    LockState(
                        isPinSet = isPinSet,
                        isUnlocked = isPinSet && unlockedId == session.userId,
                        failedAttempts = if (isPinSet) failedAttempts else 0,
                        lockedUntilEpochMs = if (isPinSet) stillLockedUntil else null
                    )
                }
            }
        }
    }

    override suspend fun setPin(pin: String): Result<Unit> {
        return runCatching {
            require(pin.matches(PIN_REGEX)) { "PIN은 4자리 숫자여야 합니다." }
            val session = authRepository.getSession() ?: throw IllegalStateException("로그인이 필요합니다.")
            val salt = PinCrypto.generateSaltBase64()
            val hash = PinCrypto.hashPinBase64(pin, salt)
            pinStore.savePin(session.userId, hash, salt)
            unlockedUserId.value = session.userId
        }
    }

    override suspend fun verifyPin(pin: String): Result<Boolean> {
        return runCatching {
            require(pin.matches(PIN_REGEX)) { "PIN은 4자리 숫자여야 합니다." }
            val session = authRepository.getSession() ?: throw IllegalStateException("로그인이 필요합니다.")
            val userId = session.userId
            val stored = pinStore.read(userId)
            val currentHash = stored.pinHash ?: throw IllegalStateException("PIN이 설정되지 않았습니다.")
            val currentSalt = stored.pinSalt ?: throw IllegalStateException("PIN이 설정되지 않았습니다.")
            val now = System.currentTimeMillis()

            if (stored.lockedUntilEpochMs != null && stored.lockedUntilEpochMs > now) {
                return@runCatching false
            }

            val normalizedAttempts = if (stored.lockedUntilEpochMs != null && stored.lockedUntilEpochMs <= now) {
                pinStore.clearFailedAttempts(userId)
                0
            } else {
                stored.failedAttempts
            }

            val inputHash = PinCrypto.hashPinBase64(pin, currentSalt)
            val isValid = MessageDigest.isEqual(inputHash.toByteArray(), currentHash.toByteArray())
            if (isValid) {
                pinStore.clearFailedAttempts(userId)
                unlockedUserId.value = userId
                true
            } else {
                val nextAttempts = normalizedAttempts + 1
                val nextLockedUntil = if (nextAttempts >= MAX_FAILED_ATTEMPTS) {
                    now + LOCK_DURATION_MS
                } else {
                    null
                }
                pinStore.updateFailedAttempts(userId, nextAttempts, nextLockedUntil)
                unlockedUserId.value = null
                false
            }
        }
    }

    override suspend fun resetForForgotPin(): Result<Unit> {
        return runCatching {
            val session = authRepository.getSession() ?: throw IllegalStateException("로그인이 필요합니다.")
            pinStore.clearUser(session.userId)
            unlockedUserId.value = null
        }
    }

    override suspend fun removePin(): Result<Unit> {
        return runCatching {
            val session = authRepository.getSession() ?: throw IllegalStateException("로그인이 필요합니다.")
            pinStore.clearUser(session.userId)
            unlockedUserId.value = null
        }
    }

    override fun clearRuntimeUnlock() {
        unlockedUserId.value = null
    }

    companion object {
        private val PIN_REGEX = Regex("^\\d{4}$")
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCK_DURATION_MS = 30_000L
    }
}
