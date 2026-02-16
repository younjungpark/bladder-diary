package com.bladderdiary.app.data.repository

import com.bladderdiary.app.data.remote.PinStoreDataSource
import com.bladderdiary.app.data.remote.PinStoredState
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.AuthResult
import com.bladderdiary.app.domain.model.SocialProvider
import com.bladderdiary.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LockRepositoryImplTest {
    @Test
    fun `PIN 설정 후 검증 성공 시 잠금 해제 상태가 된다`() = runTest {
        val auth = FakeAuthRepository()
        val store = FakePinStore()
        val repo = LockRepositoryImpl(auth, store)

        repo.setPin("1234")
        val result = repo.verifyPin("1234")

        assertTrue(result.getOrThrow())
        assertTrue(repo.observeLockState().first().isUnlocked)
    }

    @Test
    fun `오입력 5회면 잠금된다`() = runTest {
        val auth = FakeAuthRepository()
        val store = FakePinStore()
        val repo = LockRepositoryImpl(auth, store)
        repo.setPin("1234")

        repeat(5) {
            repo.verifyPin("0000")
        }

        val state = repo.observeLockState().first()
        assertTrue(state.lockedUntilEpochMs != null)
        assertFalse(state.isUnlocked)
    }

    @Test
    fun `잠금 만료 후 올바른 PIN이면 다시 통과한다`() = runTest {
        val auth = FakeAuthRepository()
        val store = FakePinStore()
        val repo = LockRepositoryImpl(auth, store)
        repo.setPin("1234")
        val userId = auth.getSession()!!.userId

        store.updateFailedAttempts(
            userId = userId,
            failedAttempts = 5,
            lockedUntilEpochMs = System.currentTimeMillis() - 1_000
        )

        val result = repo.verifyPin("1234")
        assertTrue(result.getOrThrow())
        assertTrue(store.read(userId).failedAttempts == 0)
    }

    @Test
    fun `PIN 분실 초기화 시 저장 PIN이 삭제된다`() = runTest {
        val auth = FakeAuthRepository()
        val store = FakePinStore()
        val repo = LockRepositoryImpl(auth, store)
        repo.setPin("1234")

        repo.resetForForgotPin().getOrThrow()

        val userId = auth.getSession()!!.userId
        val stored = store.read(userId)
        assertNull(stored.pinHash)
        assertNull(stored.pinSalt)
    }
}

private class FakeAuthRepository : AuthRepository {
    private val sessionState = MutableStateFlow<UserSession?>(UserSession("user-1", "access", "refresh"))
    override val sessionFlow: Flow<UserSession?> = sessionState

    override suspend fun signUp(email: String, password: String): Result<AuthResult> {
        return Result.success(AuthResult(userId = "user-1"))
    }

    override suspend fun signIn(email: String, password: String): Result<AuthResult> {
        sessionState.value = UserSession("user-1", "access", "refresh")
        return Result.success(AuthResult(userId = "user-1"))
    }

    override suspend fun signInWithSocial(provider: SocialProvider): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun handleOAuthCallback(callbackUrl: String): Result<AuthResult> {
        sessionState.value = UserSession("user-1", "access", "refresh")
        return Result.success(AuthResult(userId = "user-1"))
    }

    override suspend fun signOut() {
        sessionState.value = null
    }

    override suspend fun getSession(): UserSession? = sessionState.value
}

private class FakePinStore : PinStoreDataSource {
    private val map = mutableMapOf<String, PinStoredState>()

    override fun observe(userId: String): Flow<PinStoredState> = flow {
        emit(read(userId))
    }

    override suspend fun read(userId: String): PinStoredState {
        return map[userId] ?: PinStoredState()
    }

    override suspend fun savePin(userId: String, pinHash: String, pinSalt: String) {
        map[userId] = read(userId).copy(
            pinHash = pinHash,
            pinSalt = pinSalt,
            failedAttempts = 0,
            lockedUntilEpochMs = null
        )
    }

    override suspend fun updateFailedAttempts(userId: String, failedAttempts: Int, lockedUntilEpochMs: Long?) {
        map[userId] = read(userId).copy(
            failedAttempts = failedAttempts,
            lockedUntilEpochMs = lockedUntilEpochMs
        )
    }

    override suspend fun clearFailedAttempts(userId: String) {
        map[userId] = read(userId).copy(
            failedAttempts = 0,
            lockedUntilEpochMs = null
        )
    }

    override suspend fun clearUser(userId: String) {
        map.remove(userId)
    }
}
