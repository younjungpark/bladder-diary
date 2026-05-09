package com.bladderdiary.app.presentation.e2ee

import com.bladderdiary.app.MainDispatcherRule
import com.bladderdiary.app.data.security.MemoEncryptionScheme
import com.bladderdiary.app.data.security.RecordEncryptionScheme
import com.bladderdiary.app.domain.model.CloudSyncPreference
import com.bladderdiary.app.domain.model.DecryptedVoidingEventPayload
import com.bladderdiary.app.domain.model.E2eeRepository
import com.bladderdiary.app.domain.model.E2eeState
import com.bladderdiary.app.domain.model.SyncReport
import com.bladderdiary.app.domain.model.VoidingEventSyncPayload
import com.bladderdiary.app.domain.model.VoidingRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class E2eePassphraseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `설정 모드에서 비밀문구 불일치면 에러`() = runTest {
        val repo = FakeE2eeRepository(initial = E2eeState(isEnabled = false))
        val viewModel = E2eePassphraseViewModel(
            e2eeRepository = repo,
            voidingRepository = FakeVoidingRepository()
        )

        viewModel.onPassphraseChange("password1")
        viewModel.onConfirmPassphraseChange("password2")
        viewModel.submit()

        advanceUntilIdle()

        assertEquals("클라우드 기록 암호화 비밀문구가 일치하지 않습니다.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `잠금 해제 성공 시 기록 동기화를 요청한다`() = runTest {
        val repo = FakeE2eeRepository(initial = E2eeState(isEnabled = true, isUnlocked = false))
        val voidingRepository = FakeVoidingRepository()
        val viewModel = E2eePassphraseViewModel(
            e2eeRepository = repo,
            voidingRepository = voidingRepository
        )

        viewModel.onPassphraseChange("password1")
        viewModel.submit()

        advanceUntilIdle()

        assertTrue(voidingRepository.fetchCalled)
        assertTrue(repo.unlockCalled)
    }

    @Test
    fun `관리 모드에서 잠금 해제된 E2EE는 변경 모드가 된다`() = runTest {
        val repo = FakeE2eeRepository(initial = E2eeState(isEnabled = true, isUnlocked = true))
        val viewModel = E2eePassphraseViewModel(
            e2eeRepository = repo,
            voidingRepository = FakeVoidingRepository()
        )

        viewModel.setEntryMode(E2eeEntryMode.MANAGE)
        advanceUntilIdle()

        assertEquals(E2eeMode.CHANGE, viewModel.uiState.value.mode)
        assertFalse(viewModel.uiState.value.submitEnabled)
    }

    @Test
    fun `변경 모드 제출 시 비밀문구 변경을 요청한다`() = runTest {
        val repo = FakeE2eeRepository(initial = E2eeState(isEnabled = true, isUnlocked = true))
        val viewModel = E2eePassphraseViewModel(
            e2eeRepository = repo,
            voidingRepository = FakeVoidingRepository()
        )

        viewModel.setEntryMode(E2eeEntryMode.MANAGE)
        viewModel.onPassphraseChange("new-password")
        viewModel.onConfirmPassphraseChange("new-password")
        viewModel.submit()

        advanceUntilIdle()

        assertTrue(repo.changeCalled)
        assertEquals(null, viewModel.uiState.value.infoMessage)
    }

    @Test
    fun `변경 성공 시 이벤트가 발행된다`() = runTest {
        val repo = FakeE2eeRepository(initial = E2eeState(isEnabled = true, isUnlocked = true))
        val viewModel = E2eePassphraseViewModel(
            e2eeRepository = repo,
            voidingRepository = FakeVoidingRepository()
        )
        val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }

        viewModel.setEntryMode(E2eeEntryMode.MANAGE)
        viewModel.onPassphraseChange("new-password")
        viewModel.onConfirmPassphraseChange("new-password")
        viewModel.submit()
        advanceUntilIdle()

        val event = eventDeferred.await()
        assertEquals(
            E2eePassphraseEvent.PassphraseChanged("클라우드 기록 암호화 비밀문구가 변경되었습니다."),
            event
        )
    }
}

private class FakeE2eeRepository(initial: E2eeState) : E2eeRepository {
    private val state = MutableStateFlow(initial)
    var unlockCalled = false
    var changeCalled = false

    override fun observeState(): Flow<E2eeState> = state

    override suspend fun refreshRemoteState(): Result<Unit> = Result.success(Unit)

    override suspend fun setupPassphrase(passphrase: String): Result<Unit> {
        state.value = E2eeState(isEnabled = true, isUnlocked = true)
        return Result.success(Unit)
    }

    override suspend fun changePassphrase(passphrase: String): Result<Unit> {
        changeCalled = true
        state.value = E2eeState(isEnabled = true, isUnlocked = true)
        return Result.success(Unit)
    }

    override suspend fun unlock(passphrase: String): Result<Unit> {
        unlockCalled = true
        state.value = E2eeState(isEnabled = true, isUnlocked = true)
        return Result.success(Unit)
    }

    override fun isReadyForCloudRecordSync(): Boolean =
        state.value.isEnabled && state.value.isUnlocked

    override suspend fun prepareVoidingEventSyncPayload(
        userId: String,
        eventId: String,
        localDate: String,
        voidedAtEpochMs: Long,
        memo: String?,
        volumeMl: Int?,
        urgency: Int?,
        hasIncontinence: Boolean,
        isNocturia: Boolean
    ): Result<VoidingEventSyncPayload> = Result.success(
        VoidingEventSyncPayload(
            recordCiphertext = null,
            recordEncryption = RecordEncryptionScheme.NONE,
            memoCiphertext = memo,
            memoEncryption = MemoEncryptionScheme.NONE
        )
    )

    override suspend fun decryptVoidingEventPayload(
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
    ): Result<DecryptedVoidingEventPayload?> = Result.success(
        DecryptedVoidingEventPayload(
            voidedAtEpochMs = voidedAtEpochMs,
            memo = memoCiphertext,
            volumeMl = volumeMl,
            urgency = urgency,
            hasIncontinence = hasIncontinence,
            isNocturia = isNocturia
        )
    )

    override fun clearRuntimeUnlock() = Unit
}

private class FakeVoidingRepository : VoidingRepository {
    var fetchCalled = false
    private val cloudSyncPreference = MutableStateFlow(
        CloudSyncPreference(
            isEnabled = true,
            hasUserChoice = true
        )
    )

    override suspend fun addNow(
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> = Result.success(Unit)

    override suspend fun addAt(
        date: kotlinx.datetime.LocalDate,
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
        startDate: kotlinx.datetime.LocalDate,
        endDate: kotlinx.datetime.LocalDate
    ): Result<List<com.bladderdiary.app.domain.model.VoidingEvent>> = Result.success(emptyList())

    override fun observeByDate(
        date: kotlinx.datetime.LocalDate
    ): Flow<List<com.bladderdiary.app.domain.model.VoidingEvent>> = MutableStateFlow(emptyList())

    override fun observeDailyCount(date: kotlinx.datetime.LocalDate): Flow<Int> =
        MutableStateFlow(0)

    override fun observeMonthlyCounts(
        yearMonth: String
    ): Flow<Map<kotlinx.datetime.LocalDate, Int>> = MutableStateFlow(emptyMap())

    override fun observePendingSyncCount(): Flow<Int> = MutableStateFlow(0)

    override fun observePendingSyncError(): Flow<String?> = MutableStateFlow(null)

    override fun observeSyncInProgress(): Flow<Boolean> = MutableStateFlow(false)

    override fun observeCloudSyncPreference(): Flow<CloudSyncPreference> = cloudSyncPreference

    override suspend fun delete(localId: String): Result<Unit> = Result.success(Unit)

    override suspend fun fetchAndSyncAll(): Result<Unit> {
        fetchCalled = true
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
}
