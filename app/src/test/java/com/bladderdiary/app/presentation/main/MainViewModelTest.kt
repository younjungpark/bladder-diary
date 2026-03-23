package com.bladderdiary.app.presentation.main

import com.bladderdiary.app.MainDispatcherRule
import com.bladderdiary.app.domain.model.SyncReport
import com.bladderdiary.app.domain.model.SyncState
import com.bladderdiary.app.domain.model.VoidingEvent
import com.bladderdiary.app.domain.model.VoidingRepository
import com.bladderdiary.app.domain.usecase.AddVoidingEventUseCase
import com.bladderdiary.app.domain.usecase.DeleteVoidingEventUseCase
import com.bladderdiary.app.domain.usecase.GetDailyCountUseCase
import com.bladderdiary.app.domain.usecase.GetDailyEventsUseCase
import com.bladderdiary.app.domain.usecase.UpdateVoidingEventMemoUseCase
import com.bladderdiary.app.domain.usecase.UpdateVoidingEventVolumeUseCase
import com.bladderdiary.app.export.VoidingPdfExportParams
import com.bladderdiary.app.export.VoidingPdfExporter
import com.bladderdiary.app.export.VoidingPdfShareFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

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
    fun `지금 기록 시 절박감이 저장소로 전달된다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.addNow(urgency = 4)
        advanceUntilIdle()

        assertEquals(4, repository.lastAddNowUrgency)
        assertEquals("배뇨 기록이 저장되었습니다.", viewModel.uiState.value.message)
    }

    @Test
    fun `시간 지정 기록 시 절박감이 저장소로 전달된다`() = runTest {
        val repository = FakeVoidingRepository()
        val viewModel = createViewModel(repository, FakeVoidingPdfExporter())

        viewModel.addAtSelectedTime(hour = 7, minute = 45, urgency = 2)
        advanceUntilIdle()

        assertEquals(7, repository.lastAddAtHour)
        assertEquals(45, repository.lastAddAtMinute)
        assertEquals(2, repository.lastAddAtUrgency)
        assertEquals("지정한 시간으로 저장되었습니다.", viewModel.uiState.value.message)
    }

    private fun createViewModel(
        repository: FakeVoidingRepository,
        exporter: FakeVoidingPdfExporter
    ): MainViewModel {
        return MainViewModel(
            addVoidingEventUseCase = AddVoidingEventUseCase(repository),
            getDailyEventsUseCase = GetDailyEventsUseCase(repository),
            getDailyCountUseCase = GetDailyCountUseCase(repository),
            deleteVoidingEventUseCase = DeleteVoidingEventUseCase(repository),
            updateVoidingEventMemoUseCase = UpdateVoidingEventMemoUseCase(repository),
            updateVoidingEventVolumeUseCase = UpdateVoidingEventVolumeUseCase(repository),
            voidingPdfExporter = exporter,
            voidingRepository = repository
        )
    }

    private fun sampleEvent(): VoidingEvent {
        return VoidingEvent(
            localId = "event-1",
            userId = "user-1",
            voidedAtEpochMs = 1_000L,
            localDate = "2026-03-01",
            isDeleted = false,
            syncState = SyncState.SYNCED,
            updatedAtEpochMs = 1_000L,
            memo = "메모",
            volumeMl = 250,
            urgency = 3
        )
    }
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

private class FakeVoidingRepository : VoidingRepository {
    var rangeResult: Result<List<VoidingEvent>> = Result.success(emptyList())
    var lastAddNowUrgency: Int? = null
    var lastAddAtHour: Int? = null
    var lastAddAtMinute: Int? = null
    var lastAddAtUrgency: Int? = null
    private val events = MutableStateFlow<List<VoidingEvent>>(emptyList())
    private val count = MutableStateFlow(0)
    private val pendingCount = MutableStateFlow(0)
    private val pendingError = MutableStateFlow<String?>(null)
    private val isSyncing = MutableStateFlow(false)

    override suspend fun addNow(urgency: Int, memo: String?, volumeMl: Int?): Result<Unit> {
        lastAddNowUrgency = urgency
        return Result.success(Unit)
    }

    override suspend fun addAt(
        date: LocalDate,
        hour: Int,
        minute: Int,
        urgency: Int,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> {
        lastAddAtHour = hour
        lastAddAtMinute = minute
        lastAddAtUrgency = urgency
        return Result.success(Unit)
    }

    override suspend fun updateMemo(localId: String, memo: String?): Result<Unit> = Result.success(Unit)

    override suspend fun updateVolume(localId: String, volumeMl: Int?): Result<Unit> = Result.success(Unit)

    override suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<VoidingEvent>> {
        return rangeResult
    }

    override fun observeByDate(date: LocalDate): Flow<List<VoidingEvent>> = events

    override fun observeDailyCount(date: LocalDate): Flow<Int> = count

    override fun observeMonthlyCounts(yearMonth: String): Flow<Map<LocalDate, Int>> {
        return MutableStateFlow(emptyMap())
    }

    override fun observePendingSyncCount(): Flow<Int> = pendingCount

    override fun observePendingSyncError(): Flow<String?> = pendingError

    override fun observeSyncInProgress(): Flow<Boolean> = isSyncing

    override suspend fun delete(localId: String): Result<Unit> = Result.success(Unit)

    override suspend fun fetchAndSyncAll(): Result<Unit> = Result.success(Unit)

    override suspend fun syncPending(): Result<SyncReport> = Result.success(SyncReport(0, 0))

    override suspend fun requeueAllForUpload(): Result<Unit> = Result.success(Unit)
}
