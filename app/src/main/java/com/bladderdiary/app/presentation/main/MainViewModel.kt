package com.bladderdiary.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val isAdding: Boolean = false,
    val isExportingPdf: Boolean = false,
    val pendingPdfShareFile: VoidingPdfShareFile? = null,
    val confirmDeleteEventId: String? = null,
    val message: String? = null
)

class MainViewModel(
    private val addVoidingEventUseCase: AddVoidingEventUseCase,
    private val getDailyEventsUseCase: GetDailyEventsUseCase,
    private val getDailyCountUseCase: GetDailyCountUseCase,
    private val deleteVoidingEventUseCase: DeleteVoidingEventUseCase,
    private val updateVoidingEventUseCase: UpdateVoidingEventUseCase,
    private val voidingPdfExporter: VoidingPdfExporter,
    private val voidingRepository: VoidingRepository
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
                voidingRepository.observeSyncInProgress()
            ) { pending, pendingError, isSyncing ->
                Triple(pending, pendingError, isSyncing)
            }
            combine(
                selectedDate,
                selectedDate.flatMapLatest { date -> getDailyEventsUseCase(date) },
                selectedDate.flatMapLatest { date -> getDailyCountUseCase(date) },
                syncStateFlow
            ) { date, events, count, syncState ->
                val (pending, pendingError, isSyncing) = syncState
                MainUiState(
                    selectedDate = date,
                    events = events,
                    dailyCount = count,
                    dailyVolumeMl = events.mapNotNull { it.volumeMl }
                        .takeIf { it.isNotEmpty() }
                        ?.sum(),
                    pendingSyncCount = pending,
                    pendingSyncError = pendingError,
                    isSyncing = isSyncing,
                    isAdding = _uiState.value.isAdding,
                    isExportingPdf = _uiState.value.isExportingPdf,
                    pendingPdfShareFile = _uiState.value.pendingPdfShareFile,
                    confirmDeleteEventId = _uiState.value.confirmDeleteEventId,
                    message = _uiState.value.message
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
        _uiState.update { it.copy(confirmDeleteEventId = localId) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(confirmDeleteEventId = null) }
    }

    fun confirmDelete() {
        val targetId = _uiState.value.confirmDeleteEventId ?: return
        viewModelScope.launch {
            val result = deleteVoidingEventUseCase(targetId)
            _uiState.update {
                it.copy(
                    confirmDeleteEventId = null,
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

    companion object {
        fun factory(
            addVoidingEventUseCase: AddVoidingEventUseCase,
            getDailyEventsUseCase: GetDailyEventsUseCase,
            getDailyCountUseCase: GetDailyCountUseCase,
            deleteVoidingEventUseCase: DeleteVoidingEventUseCase,
            updateVoidingEventUseCase: UpdateVoidingEventUseCase,
            voidingPdfExporter: VoidingPdfExporter,
            voidingRepository: VoidingRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(
                addVoidingEventUseCase = addVoidingEventUseCase,
                getDailyEventsUseCase = getDailyEventsUseCase,
                getDailyCountUseCase = getDailyCountUseCase,
                deleteVoidingEventUseCase = deleteVoidingEventUseCase,
                updateVoidingEventUseCase = updateVoidingEventUseCase,
                voidingPdfExporter = voidingPdfExporter,
                voidingRepository = voidingRepository
            ) as T
        }
    }
}
