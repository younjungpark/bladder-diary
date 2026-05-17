package com.bladderdiary.app.presentation.main

import com.bladderdiary.app.MainDispatcherRule
import com.bladderdiary.app.data.backup.BackupRestoreMode
import com.bladderdiary.app.data.backup.BackupRestoreReport
import com.bladderdiary.app.domain.model.BackupRepository
import com.bladderdiary.app.domain.model.BackupRestorePreview
import com.bladderdiary.app.domain.model.BackupSettingsState
import com.bladderdiary.app.domain.model.CloudSyncPreference
import com.bladderdiary.app.domain.model.SyncReport
import com.bladderdiary.app.domain.model.SyncState
import com.bladderdiary.app.domain.model.VoidingEvent
import com.bladderdiary.app.domain.model.VoidingRepository
import com.bladderdiary.app.domain.usecase.AddVoidingEventUseCase
import com.bladderdiary.app.domain.usecase.DeleteVoidingEventUseCase
import com.bladderdiary.app.domain.usecase.GetDailyCountUseCase
import com.bladderdiary.app.domain.usecase.GetDailyEventsUseCase
import com.bladderdiary.app.domain.usecase.UpdateVoidingEventUseCase
import com.bladderdiary.app.export.VoidingPdfExportParams
import com.bladderdiary.app.export.VoidingPdfExporter
import com.bladderdiary.app.export.VoidingPdfShareFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `PDF 내보내기 성공 시 공유 파일이 준비된다`() = runTest {
        val repository = FakeVoidingRepository()
        repository.rangeResult = Result.success(listOf(sampleEvent()))
        val exporter = FakeVoidingPdfExporter()
        exporter.result = Result.success(
            VoidingPdfShareFile(
                uriString = "content://test/report.pdf",
                fileName = "report.pdf"
            )
        )
        val viewModel = createViewModel(repository, exporter)

        viewModel.exportPdf(
            startDate = LocalDate(2026, 3, 1),
            endDate = LocalDate(2026, 3, 3),
            includeMemo = false
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isExportingPdf)
        assertNotNull(viewModel.uiState.value.pendingPdfShareFile)
        assertEquals(false, exporter.lastParams?.includeMemo)
        assertEquals(1, exporter.lastEvents.size)
    }

    @Test
    fun `기간 내 기록이 없으면 공유 파일을 만들지 않는다`() = runTest {
        val repository = FakeVoidingRepository()
        repository.rangeResult = Result.success(emptyList())
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.exportPdf(
            startDate = LocalDate(2026, 3, 1),
            endDate = LocalDate(2026, 3, 3),
            includeMemo = false
        )
        advanceUntilIdle()

        assertEquals("선택한 기간에 내보낼 기록이 없습니다.", viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.pendingPdfShareFile)
    }

    @Test
    fun `공유 파일 소비 후 상태가 초기화된다`() = runTest {
        val repository = FakeVoidingRepository()
        repository.rangeResult = Result.success(listOf(sampleEvent()))
        val exporter = FakeVoidingPdfExporter()
        exporter.result = Result.success(
            VoidingPdfShareFile(
                uriString = "content://test/report.pdf",
                fileName = "report.pdf"
            )
        )
        val viewModel = createViewModel(repository, exporter)

        viewModel.exportPdf(
            startDate = LocalDate(2026, 3, 1),
            endDate = LocalDate(2026, 3, 3),
            includeMemo = true
        )
        advanceUntilIdle()
        viewModel.consumePendingPdfShareFile()

        assertNull(viewModel.uiState.value.pendingPdfShareFile)
    }

    @Test
    fun `지금 기록 시 절박감과 요실금 여부가 저장소로 전달된다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.addNow(
            urgency = 4,
            hasIncontinence = true,
            isNocturia = true,
            memo = "메모",
            volumeMl = 180
        )
        advanceUntilIdle()

        assertEquals(4, repository.lastAddNowUrgency)
        assertEquals(true, repository.lastAddNowHasIncontinence)
        assertEquals(true, repository.lastAddNowIsNocturia)
        assertEquals("메모", repository.lastAddNowMemo)
        assertEquals(180, repository.lastAddNowVolumeMl)
        assertEquals("배뇨 기록이 저장되었습니다.", viewModel.uiState.value.message)
    }

    @Test
    fun `시간 지정 기록 시 모든 입력값이 저장소로 전달된다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.addAtSelectedTime(
            hour = 7,
            minute = 45,
            urgency = 2,
            hasIncontinence = false,
            isNocturia = true,
            memo = "지정 메모",
            volumeMl = 250
        )
        advanceUntilIdle()

        assertEquals(7, repository.lastAddAtHour)
        assertEquals(45, repository.lastAddAtMinute)
        assertEquals(2, repository.lastAddAtUrgency)
        assertEquals(false, repository.lastAddAtHasIncontinence)
        assertEquals(true, repository.lastAddAtIsNocturia)
        assertEquals("지정 메모", repository.lastAddAtMemo)
        assertEquals(250, repository.lastAddAtVolumeMl)
        assertEquals("지정한 시간으로 저장되었습니다.", viewModel.uiState.value.message)
    }

    @Test
    fun `기록 수정 시 단일 업데이트로 전체 필드가 갱신된다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.updateEvent(
            localId = "event-1",
            hour = 21,
            minute = 5,
            urgency = 5,
            hasIncontinence = true,
            isNocturia = true,
            memo = "수정 메모",
            volumeMl = 320
        )
        advanceUntilIdle()

        assertEquals("event-1", repository.lastUpdatedLocalId)
        assertEquals(21, repository.lastUpdatedHour)
        assertEquals(5, repository.lastUpdatedMinute)
        assertEquals(5, repository.lastUpdatedUrgency)
        assertEquals(true, repository.lastUpdatedHasIncontinence)
        assertEquals(true, repository.lastUpdatedIsNocturia)
        assertEquals("수정 메모", repository.lastUpdatedMemo)
        assertEquals(320, repository.lastUpdatedVolumeMl)
        assertEquals("기록이 업데이트되었습니다.", viewModel.uiState.value.message)
    }

    @Test
    fun `삭제 요청 시 확인 대상 기록을 저장한다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.askDelete("event-1")

        assertEquals("event-1", viewModel.uiState.value.confirmDeleteEventId)
    }

    @Test
    fun `삭제 확인 취소 시 대상 기록이 초기화된다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.askDelete("event-1")
        viewModel.dismissDeleteDialog()

        assertNull(viewModel.uiState.value.confirmDeleteEventId)
    }

    @Test
    fun `기록 삭제 확정 시 저장소 호출 후 삭제 메시지를 노출한다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.askDelete("event-1")
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertEquals("event-1", repository.lastDeletedLocalId)
        assertNull(viewModel.uiState.value.confirmDeleteEventId)
        assertEquals("기록을 삭제했습니다.", viewModel.uiState.value.message)
    }

    @Test
    fun `기록 삭제 확정 시 확인 창을 즉시 닫는다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.askDelete("event-1")
        viewModel.confirmDelete()

        assertNull(viewModel.uiState.value.confirmDeleteEventId)
    }

    @Test
    fun `기록 삭제 실패 시 확인 상태를 해제하고 오류 메시지를 유지한다`() = runTest {
        val repository = FakeVoidingRepository()
        repository.deleteResult = Result.failure(IllegalStateException("삭제 실패"))
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.askDelete("event-1")
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertEquals("event-1", repository.lastDeletedLocalId)
        assertNull(viewModel.uiState.value.confirmDeleteEventId)
        assertEquals("삭제 실패", viewModel.uiState.value.message)
    }

    @Test
    fun `삭제 확인이 열려 있으면 다른 삭제 요청을 무시한다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.askDelete("event-1")
        viewModel.askDelete("event-2")

        assertEquals("event-1", viewModel.uiState.value.confirmDeleteEventId)
        assertTrue(repository.deletedLocalIds.isEmpty())
    }

    @Test
    fun `클라우드 동기화 설정 변경 시 저장소를 호출하고 상태를 갱신한다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.setCloudSyncEnabled(true)
        advanceUntilIdle()

        assertEquals(true, repository.cloudSyncPreference.value.isEnabled)
        assertEquals(true, repository.cloudSyncPreference.value.hasUserChoice)
        assertEquals(1, repository.fetchAndSyncAllCallCount)
        assertEquals("클라우드 동기화를 켰습니다.", viewModel.uiState.value.message)
    }

    @Test
    fun `백업 상태 변경 시 UI 상태에 반영된다`() = runTest {
        val backupRepository = FakeBackupRepository()
        val viewModel = createViewModel(
            repository = FakeVoidingRepository(),
            exporter = FakeVoidingPdfExporter(),
            backupRepository = backupRepository
        )

        backupRepository.state.value = BackupSettingsState(
            isDriveBackupConnected = true,
            isAutoBackupEnabled = true,
            lastBackupSuccessEpochMs = 1234L
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDriveBackupConnected)
        assertTrue(viewModel.uiState.value.isAutoBackupEnabled)
        assertEquals(1234L, viewModel.uiState.value.lastBackupSuccessEpochMs)
    }

    @Test
    fun `Google Drive 백업 성공 시 완료 메시지를 노출한다`() = runTest {
        val backupRepository = FakeBackupRepository()
        val viewModel = createViewModel(
            repository = FakeVoidingRepository(),
            exporter = FakeVoidingPdfExporter(),
            backupRepository = backupRepository
        )

        viewModel.backupToDrive("token", "backup-password")
        advanceUntilIdle()

        assertEquals("token", backupRepository.lastBackupAccessToken)
        assertEquals("backup-password", backupRepository.lastBackupPassphrase)
        assertEquals("Google Drive 백업을 저장했습니다.", viewModel.uiState.value.message)
    }

    @Test
    fun `수동 백업 생성 시 내보낼 envelope를 보관한다`() = runTest {
        val backupRepository = FakeBackupRepository()
        backupRepository.manualBackupResult = Result.success("{\"backup\":true}")
        val viewModel = createViewModel(
            repository = FakeVoidingRepository(),
            exporter = FakeVoidingPdfExporter(),
            backupRepository = backupRepository
        )

        viewModel.createManualBackup("manual-password")
        advanceUntilIdle()

        assertEquals("manual-password", backupRepository.lastManualBackupPassphrase)
        assertEquals("{\"backup\":true}", viewModel.uiState.value.pendingManualBackupContent)
    }

    @Test
    fun `복원 미리보기 후 병합 복원을 확정한다`() = runTest {
        val backupRepository = FakeBackupRepository()
        val preview = BackupRestorePreview(
            previewId = "preview-1",
            createdAtEpochMs = 1000L,
            sourceAppVersionName = "1.0.4",
            sourceAppVersionCode = 14,
            sourceDatabaseVersion = 1,
            recordCount = 3,
            deletedRecordCount = 0
        )
        backupRepository.prepareDriveRestoreResult = Result.success(preview)
        val viewModel = createViewModel(
            repository = FakeVoidingRepository(),
            exporter = FakeVoidingPdfExporter(),
            backupRepository = backupRepository
        )

        viewModel.prepareDriveRestore("token", "restore-password")
        advanceUntilIdle()
        assertEquals(preview, viewModel.uiState.value.pendingRestorePreview)
        viewModel.confirmRestore(BackupRestoreMode.MERGE)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingRestorePreview)
        assertEquals(BackupRestoreMode.MERGE, backupRepository.lastRestoreMode)
        assertEquals("백업과 합치기를 완료했습니다. 3건을 반영했습니다.", viewModel.uiState.value.message)
    }

    private fun createViewModel(
        repository: FakeVoidingRepository,
        exporter: FakeVoidingPdfExporter,
        backupRepository: FakeBackupRepository = FakeBackupRepository()
    ): MainViewModel = MainViewModel(
        addVoidingEventUseCase = AddVoidingEventUseCase(repository),
        getDailyEventsUseCase = GetDailyEventsUseCase(repository),
        getDailyCountUseCase = GetDailyCountUseCase(repository),
        deleteVoidingEventUseCase = DeleteVoidingEventUseCase(repository),
        updateVoidingEventUseCase = UpdateVoidingEventUseCase(repository),
        voidingPdfExporter = exporter,
        voidingRepository = repository,
        backupRepository = backupRepository
    )

    private fun sampleEvent(): VoidingEvent = VoidingEvent(
        localId = "event-1",
        userId = "user-1",
        voidedAtEpochMs = 1_000L,
        localDate = "2026-03-01",
        isDeleted = false,
        syncState = SyncState.SYNCED,
        updatedAtEpochMs = 1_000L,
        memo = "메모",
        volumeMl = 250,
        urgency = 3,
        hasIncontinence = true,
        isNocturia = true
    )
}

private class FakeVoidingPdfExporter : VoidingPdfExporter {
    var result: Result<VoidingPdfShareFile> = Result.failure(IllegalStateException("not set"))
    var lastParams: VoidingPdfExportParams? = null
    var lastEvents: List<VoidingEvent> = emptyList()

    override suspend fun export(
        params: VoidingPdfExportParams,
        events: List<VoidingEvent>
    ): Result<VoidingPdfShareFile> {
        lastParams = params
        lastEvents = events
        return result
    }
}

private class FakeBackupRepository : BackupRepository {
    val state = MutableStateFlow(BackupSettingsState())
    var driveBackupResult: Result<Unit> = Result.success(Unit)
    var manualBackupResult: Result<String> = Result.success("backup-envelope")
    var prepareDriveRestoreResult: Result<BackupRestorePreview> = Result.success(
        BackupRestorePreview(
            previewId = "preview",
            createdAtEpochMs = 1000L,
            sourceAppVersionName = "1.0.4",
            sourceAppVersionCode = 14,
            sourceDatabaseVersion = 1,
            recordCount = 0,
            deletedRecordCount = 0
        )
    )
    var prepareManualRestoreResult: Result<BackupRestorePreview> = prepareDriveRestoreResult
    var confirmRestoreResult: Result<BackupRestoreReport> = Result.success(
        BackupRestoreReport(
            mode = BackupRestoreMode.MERGE,
            restoredCount = 3,
            replacedExistingCount = 0,
            keptLocalNewerCount = 0,
            deletedExistingCount = 0
        )
    )
    var autoBackupResult: Result<Unit> = Result.success(Unit)
    var lastBackupAccessToken: String? = null
    var lastBackupPassphrase: String? = null
    var lastManualBackupPassphrase: String? = null
    var lastDriveRestoreAccessToken: String? = null
    var lastDriveRestorePassphrase: String? = null
    var lastManualRestoreEnvelope: String? = null
    var lastManualRestorePassphrase: String? = null
    var lastRestoreMode: BackupRestoreMode? = null

    override fun observeState(): Flow<BackupSettingsState> = state

    override suspend fun backupToDrive(accessToken: String, passphrase: CharArray): Result<Unit> {
        lastBackupAccessToken = accessToken
        lastBackupPassphrase = passphrase.concatToString().trimEnd('\u0000')
        return driveBackupResult
    }

    override suspend fun backupToDriveWithStoredKey(accessToken: String): Result<Unit> {
        lastBackupAccessToken = accessToken
        return driveBackupResult
    }

    override suspend fun createManualBackup(passphrase: CharArray): Result<String> {
        lastManualBackupPassphrase = passphrase.concatToString().trimEnd('\u0000')
        return manualBackupResult
    }

    override suspend fun createManualBackupWithStoredKey(): Result<String> = manualBackupResult

    override suspend fun prepareDriveRestore(
        accessToken: String,
        passphrase: CharArray
    ): Result<BackupRestorePreview> {
        lastDriveRestoreAccessToken = accessToken
        lastDriveRestorePassphrase = passphrase.concatToString().trimEnd('\u0000')
        return prepareDriveRestoreResult
    }

    override suspend fun prepareManualRestore(
        envelopeJson: String,
        passphrase: CharArray
    ): Result<BackupRestorePreview> {
        lastManualRestoreEnvelope = envelopeJson
        lastManualRestorePassphrase = passphrase.concatToString().trimEnd('\u0000')
        return prepareManualRestoreResult
    }

    override suspend fun confirmPendingRestore(
        mode: BackupRestoreMode
    ): Result<BackupRestoreReport> {
        lastRestoreMode = mode
        confirmRestoreResult = confirmRestoreResult.map { it.copy(mode = mode) }
        return confirmRestoreResult
    }

    override suspend fun cancelPendingRestore() = Unit

    override suspend fun setAutoBackupEnabled(isEnabled: Boolean): Result<Unit> {
        state.value = state.value.copy(isAutoBackupEnabled = isEnabled)
        return autoBackupResult
    }

    override suspend fun runAutomaticBackup(): Result<Unit> = autoBackupResult
}

private class FakeVoidingRepository : VoidingRepository {
    var rangeResult: Result<List<VoidingEvent>> = Result.success(emptyList())
    var lastAddNowUrgency: Int? = null
    var lastAddNowHasIncontinence: Boolean? = null
    var lastAddNowIsNocturia: Boolean? = null
    var lastAddNowMemo: String? = null
    var lastAddNowVolumeMl: Int? = null
    var lastAddAtHour: Int? = null
    var lastAddAtMinute: Int? = null
    var lastAddAtUrgency: Int? = null
    var lastAddAtHasIncontinence: Boolean? = null
    var lastAddAtIsNocturia: Boolean? = null
    var lastAddAtMemo: String? = null
    var lastAddAtVolumeMl: Int? = null
    var lastUpdatedLocalId: String? = null
    var lastUpdatedHour: Int? = null
    var lastUpdatedMinute: Int? = null
    var lastUpdatedUrgency: Int? = null
    var lastUpdatedHasIncontinence: Boolean? = null
    var lastUpdatedIsNocturia: Boolean? = null
    var lastUpdatedMemo: String? = null
    var lastUpdatedVolumeMl: Int? = null
    var deleteResult: Result<Unit> = Result.success(Unit)
    var lastDeletedLocalId: String? = null
    val deletedLocalIds = mutableListOf<String>()
    var fetchAndSyncAllCallCount: Int = 0
    val cloudSyncPreference = MutableStateFlow(
        CloudSyncPreference(
            isEnabled = false,
            hasUserChoice = true
        )
    )
    private val events = MutableStateFlow<List<VoidingEvent>>(emptyList())
    private val count = MutableStateFlow(0)
    private val pendingCount = MutableStateFlow(0)
    private val pendingError = MutableStateFlow<String?>(null)
    private val isSyncing = MutableStateFlow(false)

    override suspend fun addNow(
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> {
        lastAddNowUrgency = urgency
        lastAddNowHasIncontinence = hasIncontinence
        lastAddNowIsNocturia = isNocturia
        lastAddNowMemo = memo
        lastAddNowVolumeMl = volumeMl
        return Result.success(Unit)
    }

    override suspend fun addAt(
        date: LocalDate,
        hour: Int,
        minute: Int,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> {
        lastAddAtHour = hour
        lastAddAtMinute = minute
        lastAddAtUrgency = urgency
        lastAddAtHasIncontinence = hasIncontinence
        lastAddAtIsNocturia = isNocturia
        lastAddAtMemo = memo
        lastAddAtVolumeMl = volumeMl
        return Result.success(Unit)
    }

    override suspend fun updateEvent(
        localId: String,
        hour: Int,
        minute: Int,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> {
        lastUpdatedLocalId = localId
        lastUpdatedHour = hour
        lastUpdatedMinute = minute
        lastUpdatedUrgency = urgency
        lastUpdatedHasIncontinence = hasIncontinence
        lastUpdatedIsNocturia = isNocturia
        lastUpdatedMemo = memo
        lastUpdatedVolumeMl = volumeMl
        return Result.success(Unit)
    }

    override suspend fun getByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<VoidingEvent>> = rangeResult

    override fun observeByDate(date: LocalDate): Flow<List<VoidingEvent>> = events

    override fun observeDailyCount(date: LocalDate): Flow<Int> = count

    override fun observeMonthlyCounts(yearMonth: String): Flow<Map<LocalDate, Int>> =
        MutableStateFlow(emptyMap())

    override fun observePendingSyncCount(): Flow<Int> = pendingCount

    override fun observePendingSyncError(): Flow<String?> = pendingError

    override fun observeSyncInProgress(): Flow<Boolean> = isSyncing

    override fun observeCloudSyncPreference(): Flow<CloudSyncPreference> = cloudSyncPreference

    override suspend fun delete(localId: String): Result<Unit> {
        lastDeletedLocalId = localId
        deletedLocalIds += localId
        return deleteResult
    }

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
}
