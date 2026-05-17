package com.bladderdiary.app.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bladderdiary.app.data.backup.BackupRestoreMode
import com.bladderdiary.app.domain.model.BackupRepository
import com.bladderdiary.app.domain.model.BackupRestorePreview
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class MainUiState(
    val selectedDate: kotlinx.datetime.LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date,
    val dailyCount: Int = 0,
    val dailyVolumeMl: Int? = null,
    val events: List<VoidingEvent> = emptyList(),
    val pendingSyncCount: Int = 0,
    val pendingSyncError: String? = null,
    val isSyncing: Boolean = false,
    val isCloudSyncEnabled: Boolean = false,
    val hasCloudSyncChoice: Boolean = false,
    val isCloudSyncChanging: Boolean = false,
    val isDriveBackupConnected: Boolean = false,
    val isAutoBackupEnabled: Boolean = false,
    val lastBackupSuccessEpochMs: Long? = null,
    val lastBackupFailureEpochMs: Long? = null,
    val lastBackupErrorMessage: String? = null,
    val isBackupRunning: Boolean = false,
    val pendingManualBackupContent: String? = null,
    val pendingRestorePreview: BackupRestorePreview? = null,
    val shouldShowRestoreCloudUploadNotice: Boolean = false,
    val isAdding: Boolean = false,
    val isExportingPdf: Boolean = false,
    val pendingPdfShareFile: VoidingPdfShareFile? = null,
    val confirmDeleteEventId: String? = null,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val addVoidingEventUseCase: AddVoidingEventUseCase,
    private val getDailyEventsUseCase: GetDailyEventsUseCase,
    private val getDailyCountUseCase: GetDailyCountUseCase,
    private val deleteVoidingEventUseCase: DeleteVoidingEventUseCase,
    private val updateVoidingEventUseCase: UpdateVoidingEventUseCase,
    private val voidingPdfExporter: VoidingPdfExporter,
    private val voidingRepository: VoidingRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {
    private val selectedDate = MutableStateFlow(
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    )
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val syncStateFlow = combine(
                voidingRepository.observePendingSyncCount(),
                voidingRepository.observePendingSyncError(),
                voidingRepository.observeSyncInProgress(),
                voidingRepository.observeCloudSyncPreference()
            ) { pending, pendingError, isSyncing, cloudSyncPreference ->
                SyncUiSnapshot(
                    pendingSyncCount = if (cloudSyncPreference.isEnabled) pending else 0,
                    pendingSyncError = if (cloudSyncPreference.isEnabled) pendingError else null,
                    isSyncing = isSyncing,
                    isCloudSyncEnabled = cloudSyncPreference.isEnabled,
                    hasCloudSyncChoice = cloudSyncPreference.hasUserChoice
                )
            }
            combine(
                selectedDate,
                selectedDate.flatMapLatest { date -> getDailyEventsUseCase(date) },
                selectedDate.flatMapLatest { date -> getDailyCountUseCase(date) },
                syncStateFlow,
                backupRepository.observeState()
            ) { date, events, count, syncState, backupState ->
                val currentState = _uiState.value
                val shouldShowRestoreCloudUploadNotice =
                    currentState.shouldShowRestoreCloudUploadNotice &&
                        (
                            !syncState.isCloudSyncEnabled ||
                                syncState.pendingSyncCount > 0 ||
                                syncState.isSyncing
                            )
                MainUiState(
                    selectedDate = date,
                    events = events,
                    dailyCount = count,
                    dailyVolumeMl = events.mapNotNull { it.volumeMl }
                        .takeIf { it.isNotEmpty() }
                        ?.sum(),
                    pendingSyncCount = syncState.pendingSyncCount,
                    pendingSyncError = syncState.pendingSyncError,
                    isSyncing = syncState.isSyncing,
                    isCloudSyncEnabled = syncState.isCloudSyncEnabled,
                    hasCloudSyncChoice = syncState.hasCloudSyncChoice,
                    isCloudSyncChanging = currentState.isCloudSyncChanging,
                    isDriveBackupConnected = backupState.isDriveBackupConnected,
                    isAutoBackupEnabled = backupState.isAutoBackupEnabled,
                    lastBackupSuccessEpochMs = backupState.lastBackupSuccessEpochMs,
                    lastBackupFailureEpochMs = backupState.lastBackupFailureEpochMs,
                    lastBackupErrorMessage = backupState.lastBackupErrorMessage,
                    isBackupRunning = backupState.isBackupRunning,
                    pendingManualBackupContent = currentState.pendingManualBackupContent,
                    pendingRestorePreview = currentState.pendingRestorePreview,
                    shouldShowRestoreCloudUploadNotice = shouldShowRestoreCloudUploadNotice,
                    isAdding = currentState.isAdding,
                    isExportingPdf = currentState.isExportingPdf,
                    pendingPdfShareFile = currentState.pendingPdfShareFile,
                    confirmDeleteEventId = currentState.confirmDeleteEventId,
                    message = currentState.message
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addNow(
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String? = null,
        volumeMl: Int? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true, message = null) }
            val result = addVoidingEventUseCase(
                urgency,
                hasIncontinence,
                isNocturia,
                memo,
                volumeMl
            )
            _uiState.update {
                it.copy(
                    isAdding = false,
                    message = if (result.isSuccess) {
                        "배뇨 기록이 저장되었습니다."
                    } else {
                        result.exceptionOrNull()?.message
                    }
                )
            }
        }
    }

    fun addAtSelectedTime(
        hour: Int,
        minute: Int,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String? = null,
        volumeMl: Int? = null
    ) {
        viewModelScope.launch {
            val date = _uiState.value.selectedDate
            _uiState.update { it.copy(isAdding = true, message = null) }
            val result = addVoidingEventUseCase(
                date,
                hour,
                minute,
                urgency,
                hasIncontinence,
                isNocturia,
                memo,
                volumeMl
            )
            _uiState.update {
                it.copy(
                    isAdding = false,
                    message = if (result.isSuccess) {
                        "지정한 시간으로 저장되었습니다."
                    } else {
                        result.exceptionOrNull()?.message
                    }
                )
            }
        }
    }

    fun updateEvent(
        localId: String,
        hour: Int,
        minute: Int,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ) {
        viewModelScope.launch {
            val result = updateVoidingEventUseCase(
                localId = localId,
                hour = hour,
                minute = minute,
                urgency = urgency,
                hasIncontinence = hasIncontinence,
                isNocturia = isNocturia,
                memo = memo,
                volumeMl = volumeMl
            )
            _uiState.update {
                it.copy(
                    message = if (result.isSuccess) {
                        "기록이 업데이트되었습니다."
                    } else {
                        result.exceptionOrNull()?.message
                    }
                )
            }
        }
    }

    fun exportPdf(
        startDate: kotlinx.datetime.LocalDate,
        endDate: kotlinx.datetime.LocalDate,
        includeMemo: Boolean
    ) {
        viewModelScope.launch {
            if (startDate > endDate) {
                _uiState.update { it.copy(message = "기간 범위를 다시 확인해 주세요.") }
                return@launch
            }

            _uiState.update {
                it.copy(isExportingPdf = true, message = null, pendingPdfShareFile = null)
            }
            val eventsResult = voidingRepository.getByDateRange(startDate, endDate)
            val events = eventsResult.getOrNull()

            if (eventsResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isExportingPdf = false,
                        message = eventsResult.exceptionOrNull()?.message ?: "PDF 내보내기에 실패했습니다."
                    )
                }
                return@launch
            }

            if (events.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        isExportingPdf = false,
                        message = "선택한 기간에 내보낼 기록이 없습니다."
                    )
                }
                return@launch
            }

            val exportResult = voidingPdfExporter.export(
                params = VoidingPdfExportParams(
                    startDate = startDate,
                    endDate = endDate,
                    includeMemo = includeMemo
                ),
                events = events
            )
            _uiState.update {
                it.copy(
                    isExportingPdf = false,
                    pendingPdfShareFile = exportResult.getOrNull(),
                    message = exportResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun consumePendingPdfShareFile() {
        _uiState.update { it.copy(pendingPdfShareFile = null) }
    }

    fun goPreviousDay() {
        selectedDate.value = selectedDate.value.plus(-1, DateTimeUnit.DAY)
    }

    fun goNextDay() {
        selectedDate.value = selectedDate.value.plus(1, DateTimeUnit.DAY)
    }

    fun setDate(date: kotlinx.datetime.LocalDate) {
        selectedDate.value = date
    }

    fun askDelete(localId: String) {
        if (_uiState.value.confirmDeleteEventId != null) return
        _uiState.update { it.copy(confirmDeleteEventId = localId) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(confirmDeleteEventId = null) }
    }

    fun confirmDelete() {
        val targetId = _uiState.value.confirmDeleteEventId ?: return
        _uiState.update { it.copy(confirmDeleteEventId = null) }
        viewModelScope.launch {
            val result = deleteVoidingEventUseCase(targetId)
            _uiState.update {
                it.copy(
                    message = if (result.isSuccess) {
                        "기록을 삭제했습니다."
                    } else {
                        result.exceptionOrNull()?.message
                    }
                )
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun setCloudSyncEnabled(isEnabled: Boolean) {
        if (_uiState.value.isCloudSyncChanging) return

        val shouldShowRestoreCloudUploadNotice =
            _uiState.value.shouldShowRestoreCloudUploadNotice
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudSyncChanging = true, message = null) }
            val result = voidingRepository.setCloudSyncEnabled(isEnabled)
            _uiState.update {
                it.copy(
                    isCloudSyncChanging = false,
                    message = if (result.isSuccess) {
                        if (isEnabled) {
                            if (shouldShowRestoreCloudUploadNotice) {
                                "클라우드 동기화를 켰습니다. " +
                                    "복원 기록 암호화 업로드는 처음 한 번 오래 걸릴 수 있습니다."
                            } else {
                                "클라우드 동기화를 켰습니다."
                            }
                        } else {
                            "클라우드 동기화를 껐습니다. 기존 클라우드 데이터는 삭제되지 않습니다."
                        }
                    } else {
                        result.exceptionOrNull()?.message ?: "동기화 설정을 변경하지 못했습니다."
                    }
                )
            }
        }
    }

    fun backupToDrive(accessToken: String, passphrase: String, enableAutoBackup: Boolean = false) {
        viewModelScope.launch {
            val chars = passphrase.toCharArray()
            _uiState.update { it.copy(message = null) }
            val backupResult = try {
                backupRepository.backupToDrive(accessToken, chars)
            } finally {
                chars.fill('\u0000')
            }
            val finalResult = if (backupResult.isSuccess && enableAutoBackup) {
                backupRepository.setAutoBackupEnabled(true)
            } else {
                backupResult
            }
            _uiState.update {
                it.copy(
                    message = if (finalResult.isSuccess) {
                        if (enableAutoBackup) {
                            "Google Drive 자동 백업을 켰습니다. 기록 변경 후 약 30분 뒤 백업됩니다."
                        } else {
                            "Google Drive 백업을 저장했습니다."
                        }
                    } else {
                        finalResult.exceptionOrNull()?.message ?: "Google Drive 백업에 실패했습니다."
                    }
                )
            }
        }
    }

    fun setAutoBackupEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(message = null) }
            val result = backupRepository.setAutoBackupEnabled(isEnabled)
            _uiState.update {
                it.copy(
                    message = if (result.isSuccess) {
                        if (isEnabled) {
                            "Google Drive 자동 백업을 켰습니다. 기록 변경 후 약 30분 뒤 백업됩니다."
                        } else {
                            "Google Drive 자동 백업을 껐습니다."
                        }
                    } else {
                        result.exceptionOrNull()?.message ?: "자동 백업 설정을 변경하지 못했습니다."
                    }
                )
            }
        }
    }

    fun createManualBackup(passphrase: String) {
        viewModelScope.launch {
            val chars = passphrase.toCharArray()
            _uiState.update { it.copy(message = null, pendingManualBackupContent = null) }
            val result = try {
                backupRepository.createManualBackup(chars)
            } finally {
                chars.fill('\u0000')
            }
            _uiState.update {
                it.copy(
                    pendingManualBackupContent = result.getOrNull(),
                    message = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun consumePendingManualBackupContent(isSaved: Boolean) {
        _uiState.update {
            it.copy(
                pendingManualBackupContent = null,
                message = if (isSaved) {
                    "암호화 백업 파일을 저장했습니다."
                } else {
                    "백업 파일 저장을 취소했습니다."
                }
            )
        }
    }

    fun prepareDriveRestore(accessToken: String, passphrase: String) {
        viewModelScope.launch {
            val chars = passphrase.toCharArray()
            _uiState.update { it.copy(message = null, pendingRestorePreview = null) }
            val result = try {
                backupRepository.prepareDriveRestore(accessToken, chars)
            } finally {
                chars.fill('\u0000')
            }
            logBackupPreviewResult(source = "Drive", result = result)
            _uiState.update {
                it.copy(
                    pendingRestorePreview = result.getOrNull(),
                    message = result.exceptionOrNull().toBackupOperationMessage(
                        "Google Drive 백업 복원 미리보기를 준비하지 못했습니다."
                    )
                )
            }
        }
    }

    fun prepareManualRestore(envelopeJson: String, passphrase: String) {
        viewModelScope.launch {
            val chars = passphrase.toCharArray()
            _uiState.update { it.copy(message = null, pendingRestorePreview = null) }
            val result = try {
                backupRepository.prepareManualRestore(envelopeJson, chars)
            } finally {
                chars.fill('\u0000')
            }
            logBackupPreviewResult(source = "Manual", result = result)
            _uiState.update {
                it.copy(
                    pendingRestorePreview = result.getOrNull(),
                    message = result.exceptionOrNull().toBackupOperationMessage(
                        "백업 파일 복원 미리보기를 준비하지 못했습니다."
                    )
                )
            }
        }
    }

    fun confirmRestore(mode: BackupRestoreMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(message = null) }
            val result = backupRepository.confirmPendingRestore(mode)
            val report = result.getOrNull()
            _uiState.update {
                it.copy(
                    pendingRestorePreview = if (result.isSuccess) {
                        null
                    } else {
                        it.pendingRestorePreview
                    },
                    shouldShowRestoreCloudUploadNotice = if (
                        report != null &&
                        report.restoredCount > 0
                    ) {
                        true
                    } else {
                        it.shouldShowRestoreCloudUploadNotice
                    },
                    message = if (result.isSuccess) {
                        report?.toRestoreMessage()
                    } else {
                        result.exceptionOrNull()?.message ?: "백업 복원에 실패했습니다."
                    }
                )
            }
        }
    }

    fun cancelRestorePreview() {
        viewModelScope.launch {
            backupRepository.cancelPendingRestore()
            _uiState.update { it.copy(pendingRestorePreview = null) }
        }
    }

    companion object {
        fun factory(
            addVoidingEventUseCase: AddVoidingEventUseCase,
            getDailyEventsUseCase: GetDailyEventsUseCase,
            getDailyCountUseCase: GetDailyCountUseCase,
            deleteVoidingEventUseCase: DeleteVoidingEventUseCase,
            updateVoidingEventUseCase: UpdateVoidingEventUseCase,
            voidingPdfExporter: VoidingPdfExporter,
            voidingRepository: VoidingRepository,
            backupRepository: BackupRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(
                addVoidingEventUseCase = addVoidingEventUseCase,
                getDailyEventsUseCase = getDailyEventsUseCase,
                getDailyCountUseCase = getDailyCountUseCase,
                deleteVoidingEventUseCase = deleteVoidingEventUseCase,
                updateVoidingEventUseCase = updateVoidingEventUseCase,
                voidingPdfExporter = voidingPdfExporter,
                voidingRepository = voidingRepository,
                backupRepository = backupRepository
            ) as T
        }
    }
}

private data class SyncUiSnapshot(
    val pendingSyncCount: Int,
    val pendingSyncError: String?,
    val isSyncing: Boolean,
    val isCloudSyncEnabled: Boolean,
    val hasCloudSyncChoice: Boolean
)

private fun logBackupPreviewResult(source: String, result: Result<BackupRestorePreview>) {
    val preview = result.getOrNull()
    if (preview != null) {
        logBackupDebug(
            BACKUP_VIEW_MODEL_TAG,
            "$source restore preview ready records=${preview.recordCount} " +
                "deleted=${preview.deletedRecordCount}"
        )
        return
    }
    val error = result.exceptionOrNull()
    logBackupError(
        BACKUP_VIEW_MODEL_TAG,
        "$source restore preview failed: ${error?.javaClass?.name}: ${error?.message}",
        error
    )
}

private fun Throwable?.toBackupOperationMessage(defaultMessage: String): String? {
    if (this == null) return null
    return message?.takeIf { it.isNotBlank() } ?: defaultMessage
}

private fun logBackupDebug(tag: String, message: String) {
    runCatching {
        Log.d(tag, message)
    }
}

private fun logBackupError(tag: String, message: String, error: Throwable?) {
    runCatching {
        Log.e(tag, message, error)
    }
}

private fun com.bladderdiary.app.data.backup.BackupRestoreReport.toRestoreMessage(): String =
    when (mode) {
        BackupRestoreMode.MERGE -> "백업과 합치기를 완료했습니다. ${restoredCount}건을 반영했습니다."
        BackupRestoreMode.REPLACE -> "백업으로 교체했습니다. ${restoredCount}건을 반영했습니다."
    }

private const val BACKUP_VIEW_MODEL_TAG = "BackupViewModel"
