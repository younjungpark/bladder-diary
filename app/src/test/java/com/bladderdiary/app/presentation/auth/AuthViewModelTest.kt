package com.bladderdiary.app.presentation.auth

import com.bladderdiary.app.MainDispatcherRule
import com.bladderdiary.app.domain.model.AuthAccount
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.AuthResult
import com.bladderdiary.app.domain.model.CloudSyncPreference
import com.bladderdiary.app.domain.model.SocialProvider
import com.bladderdiary.app.domain.model.SyncReport
import com.bladderdiary.app.domain.model.UserSession
import com.bladderdiary.app.domain.model.VoidingEvent
import com.bladderdiary.app.domain.model.VoidingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `앱 시작 시 로그인 세션이 있으면 원격 복원을 시도한다`() = runTest {
        val authRepository = FakeAuthRepository(
            initialSession = null
        )
        val voidingRepository = FakeVoidingRepository(
            initialCloudSyncPreference = CloudSyncPreference(
                isEnabled = true,
                hasUserChoice = true
            )
        )

        AuthViewModel(
            authRepository = authRepository,
            voidingRepository = voidingRepository
        )
        authRepository.emitSession(
            UserSession(
                userId = "user-1",
                accessToken = "access",
                refreshToken = "refresh"
            )
        )
        advanceUntilIdle()

        assertEquals(1, voidingRepository.fetchAndSyncAllCallCount)
    }

    @Test
    fun `클라우드 동기화가 꺼져 있으면 로그인 세션이 있어도 원격 복원을 건너뛴다`() = runTest {
        val authRepository = FakeAuthRepository(
            initialSession = null
        )
        val voidingRepository = FakeVoidingRepository(
            initialCloudSyncPreference = CloudSyncPreference(
                isEnabled = false,
                hasUserChoice = true
            )
        )

        AuthViewModel(
            authRepository = authRepository,
            voidingRepository = voidingRepository
        )
        authRepository.emitSession(
            UserSession(
                userId = "user-1",
                accessToken = "access",
                refreshToken = "refresh"
            )
        )
        advanceUntilIdle()

        assertEquals(0, voidingRepository.fetchAndSyncAllCallCount)
    }

    @Test
    fun `클라우드 동기화를 다시 켜면 원격 복원을 다시 시도한다`() = runTest {
        val authRepository = FakeAuthRepository(initialSession = null)
        val voidingRepository = FakeVoidingRepository(
            initialCloudSyncPreference = CloudSyncPreference(
                isEnabled = true,
                hasUserChoice = true
            )
        )

        AuthViewModel(
            authRepository = authRepository,
            voidingRepository = voidingRepository
        )
        authRepository.emitSession(
            UserSession(
                userId = "user-1",
                accessToken = "access",
                refreshToken = "refresh"
            )
        )
        advanceUntilIdle()
        voidingRepository.emitCloudSyncPreference(isEnabled = false)
        advanceUntilIdle()
        voidingRepository.emitCloudSyncPreference(isEnabled = true)
        advanceUntilIdle()

        assertEquals(2, voidingRepository.fetchAndSyncAllCallCount)
    }

    @Test
    fun `기존 기록 계정이 카카오면 계정 전환 허용 전에는 구글 로그인을 막는다`() = runTest {
        val authRepository = FakeAuthRepository(
            initialSession = null,
            initialRememberedAccount = AuthAccount(
                userId = "user-1",
                email = "trusted@example.com",
                provider = "kakao"
            )
        )
        val viewModel = AuthViewModel(
            authRepository = authRepository,
            voidingRepository = FakeVoidingRepository()
        )

        advanceUntilIdle()
        viewModel.signInWithSocial(SocialProvider.GOOGLE)
        advanceUntilIdle()

        assertEquals(0, authRepository.signInWithSocialCallCount)
        assertTrue(viewModel.uiState.value.oauthErrorMessage?.contains("기존 기록 계정") == true)
    }

    @Test
    fun `계정 전환을 명시적으로 허용하면 다른 소셜 로그인도 진행된다`() = runTest {
        val authRepository = FakeAuthRepository(
            initialSession = null,
            initialRememberedAccount = AuthAccount(
                userId = "user-1",
                email = "trusted@example.com",
                provider = "kakao"
            )
        )
        val viewModel = AuthViewModel(
            authRepository = authRepository,
            voidingRepository = FakeVoidingRepository()
        )

        advanceUntilIdle()
        viewModel.armAccountSwitch()
        advanceUntilIdle()
        viewModel.signInWithSocial(SocialProvider.GOOGLE)
        advanceUntilIdle()

        assertEquals(1, authRepository.signInWithSocialCallCount)
        assertTrue(viewModel.uiState.value.isOAuthLoading)
    }

    @Test
    fun `회원탈퇴 성공 시 저장소를 호출하고 로그아웃 상태가 된다`() = runTest {
        val authRepository = FakeAuthRepository(
            initialSession = UserSession(
                userId = "user-1",
                accessToken = "access",
                refreshToken = "refresh"
            )
        )
        val viewModel = AuthViewModel(
            authRepository = authRepository,
            voidingRepository = FakeVoidingRepository()
        )

        advanceUntilIdle()
        viewModel.deleteAccountData()
        advanceUntilIdle()

        assertEquals(1, authRepository.deleteAccountDataCallCount)
        assertTrue(!viewModel.uiState.value.isLoggedIn)
        assertTrue(!viewModel.uiState.value.isDeletingAccount)
    }

    @Test
    fun `회원탈퇴 실패 시 오류 메시지를 노출하고 로그인 상태를 유지한다`() = runTest {
        val authRepository = FakeAuthRepository(
            initialSession = UserSession(
                userId = "user-1",
                accessToken = "access",
                refreshToken = "refresh"
            )
        )
        authRepository.deleteAccountDataResult = Result.failure(
            IllegalStateException("클라우드 기록 삭제 실패")
        )
        val viewModel = AuthViewModel(
            authRepository = authRepository,
            voidingRepository = FakeVoidingRepository()
        )

        advanceUntilIdle()
        viewModel.deleteAccountData()
        advanceUntilIdle()

        assertEquals(1, authRepository.deleteAccountDataCallCount)
        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertEquals(
            "클라우드 기록 삭제 실패",
            viewModel.uiState.value.accountDeletionErrorMessage
        )
    }
}

private class FakeAuthRepository(
    initialSession: UserSession?,
    initialRememberedAccount: AuthAccount? = null,
    initialAccountSwitchArmed: Boolean = false
) : AuthRepository {
    private val sessionState = MutableStateFlow(initialSession)
    private val rememberedAccountState = MutableStateFlow(initialRememberedAccount)
    private val accountSwitchArmedState = MutableStateFlow(initialAccountSwitchArmed)
    var signInWithSocialCallCount: Int = 0
    var deleteAccountDataCallCount: Int = 0
    var deleteAccountDataResult: Result<Unit> = Result.success(Unit)

    override val sessionFlow: Flow<UserSession?> = sessionState
    override val rememberedAccountFlow: Flow<AuthAccount?> = rememberedAccountState
    override val accountSwitchArmedFlow: Flow<Boolean> = accountSwitchArmedState

    override suspend fun signUp(email: String, password: String): Result<AuthResult> =
        Result.success(AuthResult("user-1"))

    override suspend fun signIn(email: String, password: String): Result<AuthResult> {
        sessionState.value = UserSession("user-1", "access", "refresh")
        return Result.success(AuthResult("user-1"))
    }

    override suspend fun signInWithSocial(provider: SocialProvider): Result<Unit> {
        signInWithSocialCallCount += 1
        return Result.success(Unit)
    }

    override suspend fun handleOAuthCallback(callbackUrl: String): Result<AuthResult> {
        sessionState.value = UserSession("user-1", "access", "refresh")
        return Result.success(AuthResult("user-1"))
    }

    override suspend fun armAccountSwitch() {
        accountSwitchArmedState.value = true
    }

    override suspend fun clearPendingAccountSwitch() {
        accountSwitchArmedState.value = false
    }

    override suspend fun signOut() {
        sessionState.value = null
    }

    override suspend fun deleteAccountData(): Result<Unit> {
        deleteAccountDataCallCount += 1
        if (deleteAccountDataResult.isSuccess) {
            sessionState.value = null
        }
        return deleteAccountDataResult
    }

    override suspend fun getSession(): UserSession? = sessionState.value

    override suspend fun refreshSession(): Result<UserSession> =
        Result.success(sessionState.value ?: UserSession("user-1", "access", "refresh"))

    fun emitSession(session: UserSession?) {
        sessionState.value = session
    }
}

private class FakeVoidingRepository(
    initialCloudSyncPreference: CloudSyncPreference = CloudSyncPreference(
        isEnabled = false,
        hasUserChoice = true
    )
) : VoidingRepository {
    var fetchAndSyncAllCallCount: Int = 0
    private val cloudSyncPreference = MutableStateFlow(initialCloudSyncPreference)

    override suspend fun addNow(
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> = Result.success(Unit)

    override suspend fun addAt(
        date: LocalDate,
        hour: Int,
        minute: Int,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateEvent(
        localId: String,
        hour: Int,
        minute: Int,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> = Result.success(Unit)

    override suspend fun getByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<VoidingEvent>> = Result.success(emptyList())

    override fun observeByDate(date: LocalDate): Flow<List<VoidingEvent>> =
        MutableStateFlow(emptyList())

    override fun observeDailyCount(date: LocalDate): Flow<Int> = MutableStateFlow(0)

    override fun observeMonthlyCounts(yearMonth: String): Flow<Map<LocalDate, Int>> =
        MutableStateFlow(emptyMap())

    override fun observePendingSyncCount(): Flow<Int> = MutableStateFlow(0)

    override fun observePendingSyncError(): Flow<String?> = MutableStateFlow(null)

    override fun observeSyncInProgress(): Flow<Boolean> = MutableStateFlow(false)

    override fun observeCloudSyncPreference(): Flow<CloudSyncPreference> = cloudSyncPreference

    override suspend fun delete(localId: String): Result<Unit> = Result.success(Unit)

    override suspend fun fetchAndSyncAll(): Result<Unit> {
        fetchAndSyncAllCallCount += 1
        return Result.success(Unit)
    }

    override suspend fun syncPending(): Result<SyncReport> = Result.success(SyncReport(0, 0))

    override suspend fun requeueAllForUpload(): Result<Unit> = Result.success(Unit)

    override suspend fun setCloudSyncEnabled(isEnabled: Boolean): Result<Unit> {
        cloudSyncPreference.value = CloudSyncPreference(
            isEnabled = isEnabled,
            hasUserChoice = true
        )
        return Result.success(Unit)
    }

    fun emitCloudSyncPreference(isEnabled: Boolean) {
        cloudSyncPreference.value = CloudSyncPreference(
            isEnabled = isEnabled,
            hasUserChoice = true
        )
    }
}
