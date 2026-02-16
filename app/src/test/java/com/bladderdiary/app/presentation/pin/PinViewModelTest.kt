package com.bladderdiary.app.presentation.pin

import com.bladderdiary.app.MainDispatcherRule
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.AuthResult
import com.bladderdiary.app.domain.model.LockRepository
import com.bladderdiary.app.domain.model.LockState
import com.bladderdiary.app.domain.model.SocialProvider
import com.bladderdiary.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PinViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Setup 모드에서 확인 PIN 불일치면 에러`() = runTest {
        val lockRepo = FakeLockRepository(
            initial = LockState(isPinSet = false, isUnlocked = false)
        )
        val viewModel = PinViewModel(FakeAuthRepository(), lockRepo, startTicker = false)

        viewModel.onPinChange("1234")
        viewModel.onConfirmPinChange("5678")
        viewModel.submit()

        assertEquals("PIN이 일치하지 않습니다.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Unlock 모드에서 오입력 시 에러`() = runTest {
        val lockRepo = FakeLockRepository(
            initial = LockState(isPinSet = true, isUnlocked = false, failedAttempts = 0)
        )
        lockRepo.verifyResult = Result.success(false)
        val viewModel = PinViewModel(FakeAuthRepository(), lockRepo, startTicker = false)

        viewModel.onPinChange("0000")
        viewModel.submit()

        assertEquals("PIN이 올바르지 않습니다.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `잠금 상태면 제출 버튼 비활성`() = runTest {
        val lockRepo = FakeLockRepository(
            initial = LockState(
                isPinSet = true,
                isUnlocked = false,
                failedAttempts = 5,
                lockedUntilEpochMs = System.currentTimeMillis() + 20_000
            )
        )
        val viewModel = PinViewModel(FakeAuthRepository(), lockRepo, startTicker = false)

        viewModel.onPinChange("1234")
        assertTrue(viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.submitEnabled)
    }
}

private class FakeLockRepository(
    initial: LockState
) : LockRepository {
    private val lockState = MutableStateFlow(initial)
    var verifyResult: Result<Boolean> = Result.success(true)
    var setPinResult: Result<Unit> = Result.success(Unit)

    override fun observeLockState(): Flow<LockState> = lockState

    override suspend fun setPin(pin: String): Result<Unit> = setPinResult

    override suspend fun verifyPin(pin: String): Result<Boolean> = verifyResult

    override suspend fun resetForForgotPin(): Result<Unit> = Result.success(Unit)

    override fun clearRuntimeUnlock() = Unit
}

private class FakeAuthRepository : AuthRepository {
    private val sessionState = MutableStateFlow<UserSession?>(UserSession("user-1", "a", "r"))
    override val sessionFlow: Flow<UserSession?> = sessionState

    override suspend fun signUp(email: String, password: String): Result<AuthResult> = Result.success(AuthResult("user-1"))
    override suspend fun signIn(email: String, password: String): Result<AuthResult> = Result.success(AuthResult("user-1"))
    override suspend fun signInWithSocial(provider: SocialProvider): Result<Unit> = Result.success(Unit)
    override suspend fun handleOAuthCallback(callbackUrl: String): Result<AuthResult> = Result.success(AuthResult("user-1"))
    override suspend fun signOut() {
        sessionState.value = null
    }
    override suspend fun getSession(): UserSession? = sessionState.value
}
