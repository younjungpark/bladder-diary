package com.bladderdiary.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bladderdiary.app.domain.usecase.GetMonthlyCountsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

data class CalendarUiState(
    val currentYearMonth: LocalDate,
    val dailyCounts: Map<LocalDate, Int> = emptyMap()
)

class CalendarViewModel(private val getMonthlyCountsUseCase: GetMonthlyCountsUseCase) :
    ViewModel() {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Day는 1일로 고정하여 연월 관리
    private val currentYearMonthState = MutableStateFlow(LocalDate(today.year, today.month, 1))

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = currentYearMonthState.flatMapLatest { yearMonth ->
        val yearMonthString = buildString {
            append(yearMonth.year)
            append('-')
            append(yearMonth.monthNumber.toString().padStart(2, '0'))
        }
        getMonthlyCountsUseCase(yearMonthString).flatMapLatest { countsMap ->
            MutableStateFlow(CalendarUiState(yearMonth, countsMap))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(currentYearMonthState.value)
    )

    fun goPreviousMonth() {
        currentYearMonthState.value = currentYearMonthState.value.plus(-1, DateTimeUnit.MONTH)
    }

    fun goNextMonth() {
        currentYearMonthState.value = currentYearMonthState.value.plus(1, DateTimeUnit.MONTH)
    }

    companion object {
        fun factory(getMonthlyCountsUseCase: GetMonthlyCountsUseCase): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CalendarViewModel(getMonthlyCountsUseCase) as T
            }
    }
}
